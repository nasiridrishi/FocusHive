#!/bin/bash

# ===================================================================
# COMPREHENSIVE E2E ENVIRONMENT SETUP SCRIPT
# 
# Complete setup and validation of E2E testing environment
# Includes Docker validation, service building, and health verification
# ===================================================================

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Configuration
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.e2e.yml}"
PROJECT_NAME="focushive-e2e"
TIMEOUT=300  # 5 minutes timeout for service startup
BUILD_TIMEOUT=600  # 10 minutes timeout for builds
HEALTH_CHECK_INTERVAL=5
MAX_HEALTH_RETRIES=60

# Required tools
REQUIRED_TOOLS=("docker" "docker-compose" "curl" "jq" "psql")

# Service startup order (dependencies first)
SERVICE_ORDER=(
    "test-db"
    "test-redis"
    "spotify-mock"
    "email-mock"
    "identity-service"
    "focushive-backend"
    "music-service"
    "notification-service"
    "chat-service"
    "analytics-service"
    "forum-service"
    "buddy-service"
    "frontend-e2e"
)

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

log_section() {
    echo -e "\n${PURPLE}=== $1 ===${NC}"
}

log_step() {
    echo -e "${CYAN}[STEP]${NC} $1"
}

# Show spinner while waiting
show_spinner() {
    local pid=$1
    local delay=0.1
    local spinstr='|/-\'
    while [ "$(ps a | awk '{print $1}' | grep $pid)" ]; do
        local temp=${spinstr#?}
        printf " [%c]  " "$spinstr"
        local spinstr=$temp${spinstr%"$temp"}
        sleep $delay
        printf "\b\b\b\b\b\b"
    done
    printf "    \b\b\b\b"
}

# Check prerequisites
check_prerequisites() {
    log_section "Checking Prerequisites"
    
    # Check if running as root (not recommended)
    if [ "$EUID" -eq 0 ]; then
        log_warning "Running as root is not recommended for Docker operations"
    fi
    
    # Check required tools
    for tool in "${REQUIRED_TOOLS[@]}"; do
        if command -v "$tool" &> /dev/null; then
            log_success "$tool is installed"
        else
            log_error "$tool is not installed or not in PATH"
            exit 1
        fi
    done
    
    # Check Docker daemon
    if ! docker info &> /dev/null; then
        log_error "Docker daemon is not running or not accessible"
        exit 1
    fi
    
    # Check Docker Compose version
    local compose_version=$(docker compose version --short 2>/dev/null || docker-compose --version | awk '{print $3}' | sed 's/,//')
    log_success "Docker Compose version: $compose_version"
    
    # Check available disk space (minimum 2GB)
    local available_space=$(df . | awk 'NR==2 {print $4}')
    local min_space_kb=$((2 * 1024 * 1024))  # 2GB in KB
    
    if [ "$available_space" -lt "$min_space_kb" ]; then
        log_warning "Low disk space: $(($available_space / 1024 / 1024))GB available. Minimum 2GB recommended."
    else
        log_success "Sufficient disk space available: $(($available_space / 1024 / 1024))GB"
    fi
    
    # Check memory
    local available_memory=$(free -m | awk 'NR==2{print $7}')
    if [ "$available_memory" -lt 1024 ]; then
        log_warning "Low available memory: ${available_memory}MB. Minimum 1GB recommended."
    else
        log_success "Sufficient memory available: ${available_memory}MB"
    fi
}

# Validate Docker Compose configuration
validate_compose_config() {
    log_section "Validating Docker Compose Configuration"
    
    if [ ! -f "$COMPOSE_FILE" ]; then
        log_error "Docker Compose file not found: $COMPOSE_FILE"
        exit 1
    fi
    
    log_step "Validating compose file syntax..."
    if docker compose -f "$COMPOSE_FILE" config --quiet; then
        log_success "Docker Compose configuration is valid"
    else
        log_error "Docker Compose configuration has errors"
        docker compose -f "$COMPOSE_FILE" config
        exit 1
    fi
    
    # Check for required services
    local services=$(docker compose -f "$COMPOSE_FILE" config --services)
    local missing_services=()
    
    for service in "${SERVICE_ORDER[@]}"; do
        if ! echo "$services" | grep -q "^$service$"; then
            missing_services+=("$service")
        fi
    done
    
    if [ ${#missing_services[@]} -eq 0 ]; then
        log_success "All required services are defined"
    else
        log_error "Missing services: ${missing_services[*]}"
        exit 1
    fi
}

# Create required directories and files
setup_directories() {
    log_section "Setting Up Directories and Files"
    
    local directories=(
        "docker/postgres"
        "docker/test-data" 
        "docker/mocks/spotify/mappings"
        "docker/mocks/spotify/__files"
        "test-reports"
        "frontend/e2e/results"
        "frontend/playwright-report"
        "logs/e2e"
    )
    
    for dir in "${directories[@]}"; do
        if [ ! -d "$dir" ]; then
            mkdir -p "$dir"
            log_success "Created directory: $dir"
        fi
    done
    
    # Ensure scripts are executable
    chmod +x scripts/*.sh 2>/dev/null || true
    chmod +x docker/postgres/init-multiple-databases.sh 2>/dev/null || true
    
    log_success "Directory setup completed"
}

# Create environment file if it doesn't exist
create_env_file() {
    log_section "Creating Environment Configuration"
    
    if [ ! -f ".env.e2e" ]; then
        log_step "Creating .env.e2e file..."
        cat > .env.e2e << 'EOF'
# ===================================================================
# FOCUSHIVE E2E TEST ENVIRONMENT VARIABLES
# ===================================================================

# JWT Configuration (TEST ONLY)
JWT_SECRET=test_jwt_secret_key_for_e2e_testing_only_never_use_in_production
JWT_EXPIRATION=86400000

# Database Configuration  
POSTGRES_DB=focushive_test
POSTGRES_USER=test_user
POSTGRES_PASSWORD=test_pass
POSTGRES_MULTIPLE_DATABASES=identity_test,music_test,notification_test,chat_test,analytics_test,forum_test,buddy_test

# Redis Configuration
REDIS_PASSWORD=test_redis_pass

# Service URLs (Internal Docker Network)
IDENTITY_SERVICE_URL=http://identity-service:8081
MUSIC_SERVICE_URL=http://music-service:8082
NOTIFICATION_SERVICE_URL=http://notification-service:8083
CHAT_SERVICE_URL=http://chat-service:8084
ANALYTICS_SERVICE_URL=http://analytics-service:8085
FORUM_SERVICE_URL=http://forum-service:8086
BUDDY_SERVICE_URL=http://buddy-service:8087

# Frontend URLs (External Access)
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WEBSOCKET_URL=ws://localhost:8080/ws
VITE_IDENTITY_SERVICE_URL=http://localhost:8081
VITE_ENVIRONMENT=e2e
VITE_ENABLE_ANALYTICS=false

# Mock Services
SPOTIFY_API_URL=http://spotify-mock:8080
EMAIL_SMTP_HOST=email-mock
EMAIL_SMTP_PORT=1025
EMAIL_SMTP_USER=test
EMAIL_SMTP_PASSWORD=test
EMAIL_SMTP_FROM=test@focushive.app

# OAuth2 Configuration (TEST ONLY)
KEY_STORE_PASSWORD=test_keystore_password
PRIVATE_KEY_PASSWORD=test_private_key_password
FOCUSHIVE_CLIENT_SECRET=test_client_secret_for_e2e_only

# Test Configuration
E2E_HEADLESS=true
E2E_TIMEOUT=30000
E2E_RETRY_COUNT=2
E2E_PARALLEL_TESTS=4
E2E_VIEWPORT_WIDTH=1280
E2E_VIEWPORT_HEIGHT=720

# Debug Configuration
LOG_LEVEL=DEBUG
SHOW_SQL=true
ENABLE_TEST_DATA_SEEDING=true
CORS_ORIGINS=*

# Performance Tuning
SPRING_PROFILES_ACTIVE=e2e,test
JVM_OPTS=-Xmx512m -XX:+UseG1GC
EOF
        log_success "Created .env.e2e configuration file"
    else
        log_info ".env.e2e already exists, using existing configuration"
    fi
}

# Build all Docker images
build_images() {
    log_section "Building Docker Images"
    
    log_step "Building all service images..."
    
    # Build with progress output
    docker compose -f "$COMPOSE_FILE" build --parallel --progress=plain 2>&1 | tee logs/e2e/build.log &
    local build_pid=$!
    
    # Show progress
    echo -n "Building images..."
    show_spinner $build_pid
    wait $build_pid
    local build_result=$?
    
    if [ $build_result -eq 0 ]; then
        log_success "All Docker images built successfully"
    else
        log_error "Docker image build failed. Check logs/e2e/build.log for details"
        exit 1
    fi
    
    # Show image sizes
    log_step "Docker image sizes:"
    docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}" | grep -E "(focushive|test)" || true
}

# Start infrastructure services first
start_infrastructure() {
    log_section "Starting Infrastructure Services"
    
    local infra_services=("test-db" "test-redis" "spotify-mock" "email-mock")
    
    for service in "${infra_services[@]}"; do
        log_step "Starting $service..."
        docker compose -f "$COMPOSE_FILE" up -d "$service"
        
        # Wait for service to be healthy
        wait_for_service_health "$service"
    done
    
    log_success "All infrastructure services are running"
}

# Start application services in order
start_application_services() {
    log_section "Starting Application Services"
    
    local app_services=("identity-service" "focushive-backend" "music-service" "notification-service" "chat-service" "analytics-service" "forum-service" "buddy-service" "frontend-e2e")
    
    for service in "${app_services[@]}"; do
        log_step "Starting $service..."
        docker compose -f "$COMPOSE_FILE" up -d "$service"
        
        # Wait for service to be healthy (with longer timeout for application services)
        wait_for_service_health "$service" 120
    done
    
    log_success "All application services are running"
}

# Wait for a service to become healthy
wait_for_service_health() {
    local service=$1
    local timeout=${2:-60}
    local retries=0
    local max_retries=$((timeout / HEALTH_CHECK_INTERVAL))
    
    log_step "Waiting for $service to become healthy..."
    
    while [ $retries -lt $max_retries ]; do
        local health_status=$(docker compose -f "$COMPOSE_FILE" ps --format json "$service" 2>/dev/null | jq -r '.[0].Health // "unknown"' 2>/dev/null || echo "unknown")
        
        case $health_status in
            "healthy")
                log_success "$service is healthy"
                return 0
                ;;
            "unhealthy")
                log_warning "$service is unhealthy (attempt $((retries + 1))/$max_retries)"
                ;;
            "starting")
                log_info "$service is starting (attempt $((retries + 1))/$max_retries)"
                ;;
            *)
                # Check if container is running even without health check
                local container_status=$(docker compose -f "$COMPOSE_FILE" ps --format json "$service" 2>/dev/null | jq -r '.[0].State // "unknown"' 2>/dev/null || echo "unknown")
                if [ "$container_status" = "running" ]; then
                    log_success "$service is running (no health check defined)"
                    return 0
                else
                    log_warning "$service status: $container_status (attempt $((retries + 1))/$max_retries)"
                fi
                ;;
        esac
        
        retries=$((retries + 1))
        sleep $HEALTH_CHECK_INTERVAL
    done
    
    log_error "$service failed to become healthy within $timeout seconds"
    
    # Show logs for debugging
    log_info "Last 20 lines of $service logs:"
    docker compose -f "$COMPOSE_FILE" logs --tail=20 "$service" || true
    
    return 1
}

# Perform comprehensive health check
perform_health_check() {
    log_section "Performing Health Check"
    
    if [ -f "./scripts/health-check.sh" ]; then
        log_step "Running comprehensive health check..."
        if ./scripts/health-check.sh wait; then
            log_success "All services passed health check"
        else
            log_error "Health check failed. Some services are not healthy"
            return 1
        fi
    else
        log_warning "Health check script not found, performing basic checks..."
        
        # Basic connectivity tests
        local critical_endpoints=(
            "http://localhost:8081/actuator/health"  # Identity Service
            "http://localhost:8080/actuator/health"  # Backend Service
            "http://localhost:3000"                  # Frontend
        )
        
        for endpoint in "${critical_endpoints[@]}"; do
            if curl -sf "$endpoint" &>/dev/null; then
                log_success "✅ $endpoint is accessible"
            else
                log_error "❌ $endpoint is not accessible"
                return 1
            fi
        done
    fi
}

# Seed test data
seed_test_data() {
    log_section "Seeding Test Data"
    
    if [ -f "./scripts/seed-test-data.sh" ]; then
        log_step "Running test data seeding..."
        if ./scripts/seed-test-data.sh; then
            log_success "Test data seeded successfully"
        else
            log_warning "Test data seeding had some issues, but environment may still be usable"
        fi
    else
        log_warning "Test data seeding script not found"
        log_info "You can manually seed data later using: ./scripts/seed-test-data.sh"
    fi
}

# Show environment status
show_environment_status() {
    log_section "Environment Status"
    
    # Show running containers
    echo -e "${CYAN}Running Containers:${NC}"
    docker compose -f "$COMPOSE_FILE" ps --format table
    
    echo -e "\n${CYAN}Service URLs:${NC}"
    cat << EOF
🌐 Frontend:           http://localhost:3000
🔐 Identity Service:   http://localhost:8081
⚙️  Backend API:       http://localhost:8080
🎵 Music Service:      http://localhost:8082
📧 Notification:       http://localhost:8083
💬 Chat Service:       http://localhost:8084
📊 Analytics:          http://localhost:8085
💭 Forum:              http://localhost:8086
🤝 Buddy Service:      http://localhost:8087

🧪 Test Infrastructure:
📧 MailHog (Email):    http://localhost:8025
🎵 Spotify Mock:       http://localhost:8090
🗄️  PostgreSQL:        localhost:5433
🔴 Redis:              localhost:6380
EOF
    
    echo -e "\n${CYAN}Quick Commands:${NC}"
    cat << EOF
🔍 Health Check:       ./scripts/health-check.sh
🌱 Seed Data:          ./scripts/seed-test-data.sh  
🧪 Run E2E Tests:      ./scripts/run-e2e-tests.sh
📋 View Logs:          docker compose -f $COMPOSE_FILE logs -f [service]
🛑 Stop Environment:   ./scripts/cleanup-e2e.sh
EOF
    
    # Show resource usage
    echo -e "\n${CYAN}Resource Usage:${NC}"
    docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}" $(docker compose -f "$COMPOSE_FILE" ps -q) 2>/dev/null || echo "Could not retrieve container stats"
}

# Handle cleanup on script interruption
cleanup_on_exit() {
    echo -e "\n\n${YELLOW}Setup interrupted. Cleaning up...${NC}"
    docker compose -f "$COMPOSE_FILE" down --remove-orphans &>/dev/null || true
    exit 130
}

# Main setup function
main() {
    local command=${1:-"setup"}
    
    case $command in
        "setup"|"")
            echo "═════════════════════════════════════════"
            echo "🚀 FocusHive E2E Environment Setup"
            echo "═════════════════════════════════════════"
            echo ""
            
            # Set up signal handlers
            trap cleanup_on_exit INT TERM
            
            check_prerequisites
            validate_compose_config
            setup_directories
            create_env_file
            
            # Stop any existing environment
            log_step "Stopping any existing E2E environment..."
            docker compose -f "$COMPOSE_FILE" down --remove-orphans &>/dev/null || true
            
            build_images
            start_infrastructure
            start_application_services
            perform_health_check
            seed_test_data
            
            show_environment_status
            
            log_success "🎉 E2E environment setup completed successfully!"
            echo ""
            echo "📋 Next steps:"
            echo "1. Run E2E tests: ./scripts/run-e2e-tests.sh"
            echo "2. Open frontend: http://localhost:3000"
            echo "3. Check health: ./scripts/health-check.sh"
            echo "4. View logs: docker compose -f $COMPOSE_FILE logs -f"
            echo ""
            ;;
        "quick")
            echo "🏃 Quick E2E Environment Setup (existing images)"
            
            check_prerequisites
            validate_compose_config
            
            log_step "Starting existing environment..."
            docker compose -f "$COMPOSE_FILE" up -d
            
            perform_health_check
            show_environment_status
            ;;
        "rebuild")
            echo "🔨 Rebuilding E2E Environment"
            
            log_step "Stopping existing environment..."
            docker compose -f "$COMPOSE_FILE" down --remove-orphans
            
            log_step "Removing existing images..."
            docker compose -f "$COMPOSE_FILE" down --rmi all --volumes
            
            # Run full setup
            main setup
            ;;
        "status")
            show_environment_status
            ;;
        "help")
            echo "Usage: $0 [command]"
            echo ""
            echo "Commands:"
            echo "  setup (default)  - Complete E2E environment setup"
            echo "  quick           - Quick start with existing images"
            echo "  rebuild         - Rebuild everything from scratch"
            echo "  status          - Show current environment status"
            echo "  help            - Show this help message"
            echo ""
            echo "Environment Variables:"
            echo "  COMPOSE_FILE    - Docker Compose file (default: docker-compose.e2e.yml)"
            echo "  TIMEOUT         - Service startup timeout (default: 300s)"
            echo "  BUILD_TIMEOUT   - Image build timeout (default: 600s)"
            ;;
        *)
            log_error "Unknown command: $command"
            echo "Use '$0 help' for usage information"
            exit 1
            ;;
    esac
}

# Ensure logs directory exists
mkdir -p logs/e2e

# Run main function
main "$@"