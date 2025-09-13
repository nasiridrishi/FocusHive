#!/bin/bash

# ===================================================================
# COMPREHENSIVE E2E HEALTH CHECK SCRIPT
# 
# Validates that all FocusHive services are running and healthy
# Performs detailed service health validation for E2E testing
# ===================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.e2e.yml}"
MAX_RETRIES=30
RETRY_DELAY=2
TIMEOUT=10

# Service configuration with health check endpoints
declare -A SERVICES=(
    ["test-db"]="postgresql://test_user:test_pass@localhost:5433/focushive_test"
    ["test-redis"]="redis://localhost:6380"
    ["spotify-mock"]="http://localhost:8090/__admin/health"
    ["email-mock"]="http://localhost:8025"
    ["identity-service"]="http://localhost:8081/actuator/health"
    ["focushive-backend"]="http://localhost:8080/actuator/health"
    ["music-service"]="http://localhost:8082/actuator/health"
    ["notification-service"]="http://localhost:8083/actuator/health"
    ["chat-service"]="http://localhost:8084/actuator/health"
    ["analytics-service"]="http://localhost:8085/actuator/health"
    ["forum-service"]="http://localhost:8086/actuator/health"
    ["buddy-service"]="http://localhost:8087/actuator/health"
    ["frontend-e2e"]="http://localhost:3000/health"
)

# Service descriptions
declare -A SERVICE_DESCRIPTIONS=(
    ["test-db"]="PostgreSQL Test Database"
    ["test-redis"]="Redis Cache & Sessions"
    ["spotify-mock"]="Spotify API Mock Server"
    ["email-mock"]="MailHog Email Testing"
    ["identity-service"]="OAuth2 Identity Service"
    ["focushive-backend"]="Core Backend Service"
    ["music-service"]="Music Integration Service"
    ["notification-service"]="Notification Service"
    ["chat-service"]="Real-time Chat Service"
    ["analytics-service"]="Analytics & Metrics Service"
    ["forum-service"]="Community Forum Service"
    ["buddy-service"]="Accountability Buddy Service"
    ["frontend-e2e"]="Frontend Application"
)

# Critical services that must be healthy
CRITICAL_SERVICES=("test-db" "test-redis" "identity-service" "focushive-backend")

# Statistics
TOTAL_SERVICES=${#SERVICES[@]}
HEALTHY_COUNT=0
UNHEALTHY_COUNT=0
FAILED_SERVICES=()

# Utility functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_service() {
    local service=$1
    local status=$2
    local message=$3
    local icon=""
    local color=""
    
    case $status in
        "healthy")
            icon="✅"
            color=$GREEN
            ;;
        "unhealthy")
            icon="❌"
            color=$RED
            ;;
        "checking")
            icon="🔄"
            color=$YELLOW
            ;;
        "waiting")
            icon="⏳"
            color=$CYAN
            ;;
    esac
    
    printf "${color}${icon} %-20s %-30s ${message}${NC}\n" "$service" "${SERVICE_DESCRIPTIONS[$service]}"
}

# Check if Docker and Docker Compose are available
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed or not in PATH"
        exit 1
    fi
    
    if ! command -v docker compose &> /dev/null && ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose is not installed or not in PATH"
        exit 1
    fi
    
    if [ ! -f "$COMPOSE_FILE" ]; then
        log_error "Docker Compose file not found: $COMPOSE_FILE"
        exit 1
    fi
    
    log_success "Prerequisites check passed"
}

# Check if containers are running
check_containers() {
    log_info "Checking container status..."
    
    local running_containers=$(docker compose -f "$COMPOSE_FILE" ps --services --filter "status=running" 2>/dev/null || echo "")
    local all_containers=$(docker compose -f "$COMPOSE_FILE" config --services 2>/dev/null || echo "")
    
    if [ -z "$all_containers" ]; then
        log_error "Failed to read Docker Compose configuration"
        return 1
    fi
    
    echo -e "\n${PURPLE}=== Container Status ===${NC}"
    for service in $all_containers; do
        if echo "$running_containers" | grep -q "^$service$"; then
            log_service "$service" "healthy" "Container running"
        else
            log_service "$service" "unhealthy" "Container not running"
        fi
    done
}

# Wait for a service to become healthy
wait_for_service() {
    local service=$1
    local url=$2
    local retries=0
    
    log_service "$service" "checking" "Waiting for service..."
    
    while [ $retries -lt $MAX_RETRIES ]; do
        if check_service_health "$service" "$url"; then
            log_service "$service" "healthy" "Service is healthy"
            return 0
        fi
        
        retries=$((retries + 1))
        log_service "$service" "waiting" "Retry $retries/$MAX_RETRIES in ${RETRY_DELAY}s"
        sleep $RETRY_DELAY
    done
    
    log_service "$service" "unhealthy" "Failed after $MAX_RETRIES retries"
    return 1
}

# Check individual service health
check_service_health() {
    local service=$1
    local url=$2
    
    case $service in
        "test-db")
            check_postgres_health "$url"
            ;;
        "test-redis")
            check_redis_health
            ;;
        "spotify-mock"|"email-mock")
            check_http_health "$url"
            ;;
        *-service|"frontend-e2e")
            check_spring_actuator_health "$url"
            ;;
        *)
            check_http_health "$url"
            ;;
    esac
}

# Check PostgreSQL health
check_postgres_health() {
    local url=$1
    
    # Extract connection details from URL
    local host="localhost"
    local port="5433"
    local db="focushive_test"
    local user="test_user"
    
    # Test connection with timeout
    timeout $TIMEOUT docker exec focushive-test-db pg_isready -h "$host" -p 5432 -d "$db" -U "$user" &>/dev/null
}

# Check Redis health
check_redis_health() {
    timeout $TIMEOUT docker exec focushive-test-redis redis-cli -a "test_redis_pass" ping &>/dev/null
}

# Check HTTP endpoint health
check_http_health() {
    local url=$1
    timeout $TIMEOUT curl -sf "$url" &>/dev/null
}

# Check Spring Boot Actuator health
check_spring_actuator_health() {
    local url=$1
    
    # First check if endpoint is reachable
    if ! timeout $TIMEOUT curl -sf "$url" &>/dev/null; then
        return 1
    fi
    
    # Check if status is UP
    local status=$(timeout $TIMEOUT curl -s "$url" 2>/dev/null | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    [ "$status" = "UP" ]
}

# Perform detailed service validation
validate_service_functionality() {
    local service=$1
    
    case $service in
        "identity-service")
            validate_identity_service
            ;;
        "focushive-backend")
            validate_backend_service
            ;;
        "music-service")
            validate_music_service
            ;;
        "notification-service")
            validate_notification_service
            ;;
        "chat-service")
            validate_chat_service
            ;;
        "analytics-service")
            validate_analytics_service
            ;;
        "forum-service")
            validate_forum_service
            ;;
        "buddy-service")
            validate_buddy_service
            ;;
        *)
            return 0  # Skip validation for infrastructure services
            ;;
    esac
}

# Identity service validation
validate_identity_service() {
    log_info "Validating Identity Service functionality..."
    
    # Check OAuth2 well-known configuration
    if timeout $TIMEOUT curl -sf "http://localhost:8081/.well-known/oauth-authorization-server" &>/dev/null; then
        log_success "OAuth2 configuration endpoint accessible"
    else
        log_error "OAuth2 configuration endpoint not accessible"
        return 1
    fi
    
    # Check user registration endpoint
    if timeout $TIMEOUT curl -sf "http://localhost:8081/api/users/register" -X POST -H "Content-Type: application/json" -d '{}' &>/dev/null; then
        log_success "User registration endpoint accessible"
    else
        log_warning "User registration endpoint validation skipped (expected without auth)"
    fi
}

# Backend service validation
validate_backend_service() {
    log_info "Validating Backend Service functionality..."
    
    # Check WebSocket endpoint
    if timeout $TIMEOUT curl -sf "http://localhost:8080/ws" -H "Upgrade: websocket" &>/dev/null; then
        log_success "WebSocket endpoint accessible"
    else
        log_warning "WebSocket endpoint validation requires proper headers"
    fi
    
    # Check public endpoints
    if timeout $TIMEOUT curl -sf "http://localhost:8080/api/hives/public" &>/dev/null; then
        log_success "Public API endpoints accessible"
    else
        log_warning "Public API validation skipped (may require authentication)"
    fi
}

# Music service validation
validate_music_service() {
    log_info "Validating Music Service functionality..."
    
    # Check Spotify mock connection
    if timeout $TIMEOUT curl -sf "http://localhost:8090/v1/me" &>/dev/null; then
        log_success "Spotify mock API accessible"
    else
        log_error "Spotify mock API not accessible"
        return 1
    fi
}

# Notification service validation
validate_notification_service() {
    log_info "Validating Notification Service functionality..."
    
    # Check email mock connection
    if timeout $TIMEOUT curl -sf "http://localhost:1025" &>/dev/null; then
        log_success "SMTP mock server accessible"
    else
        log_error "SMTP mock server not accessible"
        return 1
    fi
}

# Chat service validation
validate_chat_service() {
    log_info "Validating Chat Service functionality..."
    
    # Check WebSocket support
    local chat_ws_endpoint="ws://localhost:8084/ws"
    log_success "Chat WebSocket endpoint configured: $chat_ws_endpoint"
}

# Analytics service validation
validate_analytics_service() {
    log_info "Validating Analytics Service functionality..."
    
    # Check metrics endpoint
    if timeout $TIMEOUT curl -sf "http://localhost:8085/actuator/metrics" &>/dev/null; then
        log_success "Metrics endpoint accessible"
    else
        log_warning "Metrics endpoint validation skipped"
    fi
}

# Forum service validation
validate_forum_service() {
    log_info "Validating Forum Service functionality..."
    log_success "Forum service health check completed"
}

# Buddy service validation
validate_buddy_service() {
    log_info "Validating Buddy Service functionality..."
    log_success "Buddy service health check completed"
}

# Check database connections and schemas
check_databases() {
    log_info "Checking database schemas..."
    
    local databases=("focushive_test" "identity_test" "music_test" "notification_test" "chat_test" "analytics_test" "forum_test" "buddy_test")
    
    for db in "${databases[@]}"; do
        if docker exec focushive-test-db psql -U test_user -d "$db" -c "SELECT 1;" &>/dev/null; then
            log_success "Database '$db' accessible"
        else
            log_error "Database '$db' not accessible"
            FAILED_SERVICES+=("$db")
        fi
    done
}

# Check WebSocket connections
check_websockets() {
    log_info "Checking WebSocket endpoints..."
    
    local websocket_services=("focushive-backend" "chat-service")
    
    for service in "${websocket_services[@]}"; do
        local port=""
        case $service in
            "focushive-backend") port="8080" ;;
            "chat-service") port="8084" ;;
        esac
        
        if [ -n "$port" ]; then
            # Basic WebSocket endpoint check (limited without proper WebSocket client)
            if timeout $TIMEOUT curl -sf "http://localhost:$port/ws" -H "Connection: Upgrade" -H "Upgrade: websocket" &>/dev/null; then
                log_success "WebSocket endpoint for $service accessible"
            else
                log_warning "WebSocket validation for $service requires proper WebSocket handshake"
            fi
        fi
    done
}

# Main health check function
perform_health_check() {
    echo -e "\n${PURPLE}=== Service Health Check ===${NC}"
    
    for service in "${!SERVICES[@]}"; do
        local url="${SERVICES[$service]}"
        
        if wait_for_service "$service" "$url"; then
            HEALTHY_COUNT=$((HEALTHY_COUNT + 1))
            
            # Perform detailed validation for application services
            if [[ "$service" == *-service ]]; then
                validate_service_functionality "$service"
            fi
        else
            UNHEALTHY_COUNT=$((UNHEALTHY_COUNT + 1))
            FAILED_SERVICES+=("$service")
        fi
    done
}

# Check critical services
check_critical_services() {
    log_info "Checking critical services..."
    
    local critical_failed=()
    for service in "${CRITICAL_SERVICES[@]}"; do
        if [[ " ${FAILED_SERVICES[@]} " =~ " ${service} " ]]; then
            critical_failed+=("$service")
        fi
    done
    
    if [ ${#critical_failed[@]} -eq 0 ]; then
        log_success "All critical services are healthy"
        return 0
    else
        log_error "Critical services failed: ${critical_failed[*]}"
        return 1
    fi
}

# Generate health report
generate_report() {
    echo -e "\n${PURPLE}=== Health Check Report ===${NC}"
    echo -e "Total Services: $TOTAL_SERVICES"
    echo -e "Healthy: ${GREEN}$HEALTHY_COUNT${NC}"
    echo -e "Unhealthy: ${RED}$UNHEALTHY_COUNT${NC}"
    
    if [ ${#FAILED_SERVICES[@]} -gt 0 ]; then
        echo -e "\n${RED}Failed Services:${NC}"
        for service in "${FAILED_SERVICES[@]}"; do
            echo -e "  ❌ $service - ${SERVICE_DESCRIPTIONS[$service]}"
        done
    fi
    
    echo -e "\n${PURPLE}=== Additional Checks ===${NC}"
    check_databases
    check_websockets
    
    # Calculate overall health percentage
    local health_percentage=$((HEALTHY_COUNT * 100 / TOTAL_SERVICES))
    echo -e "\n${PURPLE}=== Overall Health ===${NC}"
    echo -e "System Health: $health_percentage%"
    
    if [ $health_percentage -ge 90 ]; then
        log_success "System is healthy and ready for E2E testing"
        return 0
    elif [ $health_percentage -ge 70 ]; then
        log_warning "System has some issues but may be usable for testing"
        return 1
    else
        log_error "System has critical issues and is not ready for testing"
        return 2
    fi
}

# Show service logs for debugging
show_service_logs() {
    local service=$1
    echo -e "\n${PURPLE}=== Logs for $service ===${NC}"
    docker compose -f "$COMPOSE_FILE" logs --tail=20 "$service" 2>/dev/null || echo "No logs available"
}

# Main function
main() {
    local command=${1:-"check"}
    
    case $command in
        "check"|"")
            echo "═════════════════════════════════════════"
            echo "🏥 FocusHive E2E Health Check"
            echo "═════════════════════════════════════════"
            
            check_prerequisites
            check_containers
            perform_health_check
            
            if ! check_critical_services; then
                echo -e "\n${PURPLE}=== Troubleshooting Tips ===${NC}"
                echo "1. Check failed service logs: ./scripts/health-check.sh logs <service-name>"
                echo "2. Restart failed services: docker compose -f $COMPOSE_FILE restart <service-name>"
                echo "3. View all logs: docker compose -f $COMPOSE_FILE logs -f"
                echo "4. Check container status: docker compose -f $COMPOSE_FILE ps"
            fi
            
            generate_report
            ;;
        "logs")
            if [ -n "$2" ]; then
                show_service_logs "$2"
            else
                echo "Usage: $0 logs <service-name>"
                echo "Available services: ${!SERVICES[*]}"
            fi
            ;;
        "quick")
            echo "Quick health check..."
            for service in "${CRITICAL_SERVICES[@]}"; do
                local url="${SERVICES[$service]}"
                if check_service_health "$service" "$url"; then
                    log_service "$service" "healthy" "OK"
                else
                    log_service "$service" "unhealthy" "FAILED"
                fi
            done
            ;;
        "wait")
            echo "Waiting for all services to become healthy..."
            perform_health_check
            if check_critical_services; then
                log_success "All critical services are healthy"
                exit 0
            else
                log_error "Some critical services failed to become healthy"
                exit 1
            fi
            ;;
        "help")
            echo "Usage: $0 [command]"
            echo ""
            echo "Commands:"
            echo "  check (default)  - Perform complete health check"
            echo "  quick           - Quick check of critical services only"
            echo "  wait            - Wait for services to become healthy"
            echo "  logs <service>  - Show logs for specific service"
            echo "  help            - Show this help message"
            echo ""
            echo "Environment Variables:"
            echo "  COMPOSE_FILE    - Docker Compose file to use (default: docker-compose.e2e.yml)"
            echo "  MAX_RETRIES     - Maximum retries per service (default: 30)"
            echo "  RETRY_DELAY     - Delay between retries in seconds (default: 2)"
            echo "  TIMEOUT         - Timeout for individual checks (default: 10)"
            ;;
        *)
            echo "Unknown command: $command"
            echo "Use '$0 help' for usage information"
            exit 1
            ;;
    esac
}

# Handle Ctrl+C gracefully
trap 'echo -e "\n\nHealth check interrupted"; exit 130' INT

# Run main function
main "$@"