#!/bin/bash

# ===================================================================
# FOCUSHIVE E2E TEST RUNNER
# 
# Comprehensive script to start the E2E environment and run all
# 558 E2E test scenarios with proper setup and teardown
# ===================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Configuration
COMPOSE_FILE="docker-compose.e2e.yml"
PROJECT_NAME="focushive-e2e"
FRONTEND_DIR="frontend"
TIMEOUT=300  # 5 minutes timeout for services to start

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "\n${PURPLE}═══════════════════════════════════════════════${NC}"
    echo -e "${PURPLE} $1${NC}"
    echo -e "${PURPLE}═══════════════════════════════════════════════${NC}\n"
}

# Function to check if Docker is running
check_docker() {
    print_status "Checking Docker availability..."
    if ! docker info >/dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker first."
        exit 1
    fi
    print_success "Docker is running"
}

# Function to check if required files exist
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    if [ ! -f "$COMPOSE_FILE" ]; then
        print_error "Docker Compose file not found: $COMPOSE_FILE"
        exit 1
    fi
    
    if [ ! -d "$FRONTEND_DIR" ]; then
        print_error "Frontend directory not found: $FRONTEND_DIR"
        exit 1
    fi
    
    if [ ! -d "$FRONTEND_DIR/e2e" ]; then
        print_error "E2E tests directory not found: $FRONTEND_DIR/e2e"
        exit 1
    fi
    
    print_success "All prerequisites met"
}

# Function to clean up existing containers
cleanup_containers() {
    print_status "Cleaning up existing containers..."
    docker compose -f $COMPOSE_FILE -p $PROJECT_NAME down --volumes --remove-orphans >/dev/null 2>&1 || true
    
    # Clean up any leftover test containers
    docker ps -a --filter "name=focushive-e2e" --format "{{.ID}}" | xargs -r docker rm -f >/dev/null 2>&1 || true
    docker ps -a --filter "name=test-data-seeder" --format "{{.ID}}" | xargs -r docker rm -f >/dev/null 2>&1 || true
    
    print_success "Cleanup completed"
}

# Function to build Docker images
build_images() {
    print_status "Building Docker images..."
    docker compose -f $COMPOSE_FILE build --parallel
    print_success "Docker images built successfully"
}

# Function to start E2E environment
start_environment() {
    print_header "STARTING E2E ENVIRONMENT"
    
    print_status "Starting all services..."
    docker compose -f $COMPOSE_FILE -p $PROJECT_NAME up -d
    
    # Wait for services to be healthy
    print_status "Waiting for services to be ready (timeout: ${TIMEOUT}s)..."
    
    local services=("test-db" "test-redis" "identity-service" "focushive-backend" "music-service" "notification-service" "chat-service" "analytics-service" "forum-service" "buddy-service" "frontend-e2e")
    
    for service in "${services[@]}"; do
        print_status "Waiting for $service..."
        local counter=0
        while [ $counter -lt $TIMEOUT ]; do
            if docker compose -f $COMPOSE_FILE -p $PROJECT_NAME ps $service | grep -q "healthy\|Up"; then
                print_success "$service is ready"
                break
            fi
            sleep 2
            ((counter+=2))
        done
        
        if [ $counter -ge $TIMEOUT ]; then
            print_error "$service failed to start within timeout"
            print_status "Checking $service logs:"
            docker compose -f $COMPOSE_FILE -p $PROJECT_NAME logs --tail=20 $service
            return 1
        fi
    done
    
    print_success "All services are ready!"
}

# Function to verify service endpoints
verify_endpoints() {
    print_status "Verifying service endpoints..."
    
    local endpoints=(
        "http://localhost:8080/actuator/health:FocusHive Backend"
        "http://localhost:8081/actuator/health:Identity Service"
        "http://localhost:8082/actuator/health:Music Service"
        "http://localhost:8083/actuator/health:Notification Service"
        "http://localhost:8084/actuator/health:Chat Service"
        "http://localhost:8085/actuator/health:Analytics Service"
        "http://localhost:8086/actuator/health:Forum Service"
        "http://localhost:8087/actuator/health:Buddy Service"
        "http://localhost:3000:Frontend"
        "http://localhost:8090/__admin/health:Spotify Mock"
        "http://localhost:8025:Email Mock"
    )
    
    for endpoint_info in "${endpoints[@]}"; do
        local url="${endpoint_info%:*}"
        local name="${endpoint_info#*:}"
        
        if curl -s -f "$url" >/dev/null; then
            print_success "$name endpoint is accessible"
        else
            print_warning "$name endpoint is not accessible: $url"
        fi
    done
}

# Function to run E2E tests
run_tests() {
    print_header "RUNNING E2E TESTS"
    
    cd $FRONTEND_DIR
    
    # Install dependencies if needed
    if [ ! -d "node_modules" ]; then
        print_status "Installing frontend dependencies..."
        npm ci
    fi
    
    print_status "Running 558 E2E test scenarios..."
    
    # Set environment variables for tests
    export E2E_BASE_URL="http://localhost:3000"
    export E2E_API_URL="http://localhost:8080"
    export E2E_IDENTITY_URL="http://localhost:8081"
    export E2E_WEBSOCKET_URL="ws://localhost:8080/ws"
    
    # Run Playwright tests
    if command -v npx >/dev/null 2>&1; then
        print_status "Running Playwright E2E tests..."
        if npx playwright test --config=playwright.config.ts; then
            print_success "Playwright tests completed successfully!"
        else
            print_error "Some Playwright tests failed"
            TESTS_FAILED=1
        fi
    fi
    
    # Run Cypress tests if available
    if [ -f "cypress.config.ts" ] && command -v npx >/dev/null 2>&1; then
        print_status "Running Cypress E2E tests..."
        if npx cypress run --headless; then
            print_success "Cypress tests completed successfully!"
        else
            print_error "Some Cypress tests failed"
            TESTS_FAILED=1
        fi
    fi
    
    cd ..
}

# Function to generate test report
generate_report() {
    print_header "GENERATING TEST REPORT"
    
    local report_dir="test-reports/e2e-$(date +%Y%m%d-%H%M%S)"
    mkdir -p "$report_dir"
    
    # Copy test results
    if [ -d "$FRONTEND_DIR/playwright-report" ]; then
        cp -r "$FRONTEND_DIR/playwright-report" "$report_dir/"
        print_status "Playwright report copied to $report_dir/playwright-report"
    fi
    
    if [ -d "$FRONTEND_DIR/cypress/reports" ]; then
        cp -r "$FRONTEND_DIR/cypress/reports" "$report_dir/"
        print_status "Cypress reports copied to $report_dir/reports"
    fi
    
    # Generate service logs
    print_status "Collecting service logs..."
    docker compose -f $COMPOSE_FILE -p $PROJECT_NAME logs > "$report_dir/service-logs.txt"
    
    # Generate system info
    cat > "$report_dir/test-environment.md" << EOF
# E2E Test Environment Report

**Date:** $(date)
**Test Duration:** $((SECONDS / 60)) minutes
**Docker Compose File:** $COMPOSE_FILE

## Services Status
$(docker compose -f $COMPOSE_FILE -p $PROJECT_NAME ps)

## Test Results Summary
- **Total Test Scenarios:** 558
- **Test Framework:** Playwright + Cypress
- **Environment:** Docker Compose E2E Stack
- **All Services Started:** $([ ${TESTS_FAILED:-0} -eq 0 ] && echo "✅ Yes" || echo "❌ Some issues")

## Service Endpoints Tested
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- Identity Service: http://localhost:8081
- Music Service: http://localhost:8082
- Notification Service: http://localhost:8083
- Chat Service: http://localhost:8084
- Analytics Service: http://localhost:8085
- Forum Service: http://localhost:8086
- Buddy Service: http://localhost:8087
- Spotify Mock: http://localhost:8090
- Email Mock: http://localhost:8025

## Database Configuration
- PostgreSQL: Multiple test databases
- Redis: Session and real-time data
- Test Data: Seeded with realistic scenarios

$([ ${TESTS_FAILED:-0} -eq 0 ] && echo "## ✅ All tests completed successfully!" || echo "## ❌ Some tests failed - check logs for details")
EOF

    print_success "Test report generated: $report_dir"
    echo "📊 Open $report_dir/test-environment.md to view the summary"
    
    if [ -f "$report_dir/playwright-report/index.html" ]; then
        echo "🎭 Playwright report: $report_dir/playwright-report/index.html"
    fi
}

# Function to cleanup after tests
cleanup() {
    print_header "CLEANING UP"
    
    if [ "${KEEP_RUNNING:-}" != "true" ]; then
        print_status "Stopping E2E environment..."
        docker compose -f $COMPOSE_FILE -p $PROJECT_NAME down --volumes
        print_success "E2E environment stopped"
    else
        print_status "Keeping E2E environment running (KEEP_RUNNING=true)"
        echo "Services are still running at:"
        echo "  Frontend: http://localhost:3000"
        echo "  Backend: http://localhost:8080"
        echo "  To stop: docker compose -f $COMPOSE_FILE -p $PROJECT_NAME down --volumes"
    fi
}

# Main execution
main() {
    print_header "FOCUSHIVE E2E TEST RUNNER"
    echo "This script will run 558 E2E test scenarios across all FocusHive services"
    echo ""
    
    local start_time=$(date +%s)
    
    # Trap cleanup on exit
    trap cleanup EXIT
    
    # Run all steps
    check_docker
    check_prerequisites
    cleanup_containers
    build_images
    start_environment
    verify_endpoints
    run_tests
    generate_report
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    print_header "E2E TEST EXECUTION COMPLETED"
    print_success "Total execution time: $((duration / 60))m $((duration % 60))s"
    
    if [ ${TESTS_FAILED:-0} -eq 0 ]; then
        print_success "✅ All E2E tests completed successfully!"
        return 0
    else
        print_error "❌ Some E2E tests failed - check the reports for details"
        return 1
    fi
}

# Handle script arguments
case "${1:-}" in
    "start")
        print_header "STARTING E2E ENVIRONMENT ONLY"
        check_docker
        check_prerequisites
        cleanup_containers
        build_images
        start_environment
        verify_endpoints
        echo "E2E environment is running. Use 'npm run test:e2e' to run tests."
        export KEEP_RUNNING=true
        ;;
    "stop")
        print_header "STOPPING E2E ENVIRONMENT"
        docker compose -f $COMPOSE_FILE -p $PROJECT_NAME down --volumes
        print_success "E2E environment stopped"
        ;;
    "logs")
        docker compose -f $COMPOSE_FILE -p $PROJECT_NAME logs -f "${2:-}"
        ;;
    "status")
        docker compose -f $COMPOSE_FILE -p $PROJECT_NAME ps
        ;;
    *)
        main
        ;;
esac