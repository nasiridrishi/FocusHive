#!/bin/bash

# Network Error E2E Test Runner Script
# Manages mock backend lifecycle and runs Playwright tests

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
FRONTEND_DIR="$(cd "$PROJECT_DIR/../../.." && pwd)"
MOCK_BACKEND_DIR="$PROJECT_DIR"
MOCK_BACKEND_PORT=8080
FRONTEND_PORT=3000
PLAYWRIGHT_CONFIG="$PROJECT_DIR/network-error.config.ts"

# PID files for process management
MOCK_BACKEND_PID_FILE="/tmp/mock-backend.pid"
FRONTEND_PID_FILE="/tmp/frontend-dev.pid"

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

# Function to check if a port is in use
is_port_in_use() {
    lsof -Pi :$1 -sTCP:LISTEN -t >/dev/null 2>&1
}

# Function to wait for a service to be ready
wait_for_service() {
    local url=$1
    local service_name=$2
    local max_attempts=30
    local attempt=1

    print_status "Waiting for $service_name to be ready at $url..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -f -s "$url" >/dev/null 2>&1; then
            print_success "$service_name is ready!"
            return 0
        fi
        
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    print_error "Timeout waiting for $service_name to be ready"
    return 1
}

# Function to start mock backend
start_mock_backend() {
    print_status "Starting mock backend server..."
    
    if is_port_in_use $MOCK_BACKEND_PORT; then
        print_warning "Port $MOCK_BACKEND_PORT is already in use. Checking if it's our mock backend..."
        if curl -f -s "http://localhost:$MOCK_BACKEND_PORT/test/status" >/dev/null 2>&1; then
            print_success "Mock backend is already running"
            return 0
        else
            print_error "Port $MOCK_BACKEND_PORT is in use by another service"
            exit 1
        fi
    fi
    
    # Install dependencies if needed
    if [ ! -d "$MOCK_BACKEND_DIR/node_modules" ]; then
        print_status "Installing mock backend dependencies..."
        cd "$MOCK_BACKEND_DIR"
        npm install
    fi
    
    # Start mock backend in background
    cd "$MOCK_BACKEND_DIR"
    E2E_MOCK_BACKEND_PORT=$MOCK_BACKEND_PORT node mock-backend.js &
    echo $! > "$MOCK_BACKEND_PID_FILE"
    
    # Wait for mock backend to be ready
    wait_for_service "http://localhost:$MOCK_BACKEND_PORT/health" "Mock Backend"
}

# Function to start frontend dev server
start_frontend() {
    print_status "Starting frontend development server..."
    
    if is_port_in_use $FRONTEND_PORT; then
        print_warning "Port $FRONTEND_PORT is already in use. Assuming frontend is running..."
        if curl -f -s "http://localhost:$FRONTEND_PORT" >/dev/null 2>&1; then
            print_success "Frontend is already running"
            return 0
        else
            print_error "Port $FRONTEND_PORT is in use by another service"
            exit 1
        fi
    fi
    
    # Start frontend in background
    cd "$FRONTEND_DIR"
    npm run dev &
    echo $! > "$FRONTEND_PID_FILE"
    
    # Wait for frontend to be ready
    wait_for_service "http://localhost:$FRONTEND_PORT" "Frontend"
}

# Function to stop services
stop_services() {
    print_status "Stopping services..."
    
    # Stop mock backend
    if [ -f "$MOCK_BACKEND_PID_FILE" ]; then
        local mock_pid=$(cat "$MOCK_BACKEND_PID_FILE")
        if kill -0 "$mock_pid" 2>/dev/null; then
            print_status "Stopping mock backend (PID: $mock_pid)..."
            kill "$mock_pid" || true
            sleep 2
            # Force kill if still running
            if kill -0 "$mock_pid" 2>/dev/null; then
                kill -9 "$mock_pid" || true
            fi
        fi
        rm -f "$MOCK_BACKEND_PID_FILE"
    fi
    
    # Stop frontend (only if we started it)
    if [ -f "$FRONTEND_PID_FILE" ] && [ "$START_FRONTEND" = "true" ]; then
        local frontend_pid=$(cat "$FRONTEND_PID_FILE")
        if kill -0 "$frontend_pid" 2>/dev/null; then
            print_status "Stopping frontend (PID: $frontend_pid)..."
            kill "$frontend_pid" || true
            sleep 2
            # Force kill if still running
            if kill -0 "$frontend_pid" 2>/dev/null; then
                kill -9 "$frontend_pid" || true
            fi
        fi
        rm -f "$FRONTEND_PID_FILE"
    fi
}

# Function to run Playwright tests
run_tests() {
    print_status "Running NetworkErrorFallback E2E tests..."
    
    cd "$FRONTEND_DIR"
    
    # Set environment variables
    export E2E_MOCK_BACKEND_URL="http://localhost:$MOCK_BACKEND_PORT"
    export E2E_FRONTEND_URL="http://localhost:$FRONTEND_PORT"
    
    # Run tests with custom config
    local test_command="npx playwright test --config=$PLAYWRIGHT_CONFIG"
    
    # Add any additional arguments passed to script
    if [ $# -gt 0 ]; then
        test_command="$test_command $*"
    fi
    
    print_status "Running: $test_command"
    
    if $test_command; then
        print_success "All tests passed!"
        return 0
    else
        print_error "Some tests failed"
        return 1
    fi
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [options] [playwright-args]"
    echo ""
    echo "Options:"
    echo "  --start-frontend    Start frontend dev server (default: assume already running)"
    echo "  --keep-services     Keep services running after tests"
    echo "  --stop-only         Stop services without running tests"
    echo "  --status            Show status of services"
    echo "  --help              Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                                 # Run tests (assume frontend is running)"
    echo "  $0 --start-frontend               # Start frontend and run tests"
    echo "  $0 --keep-services                # Run tests and keep services running"
    echo "  $0 --headed --debug               # Pass args to Playwright"
    echo "  $0 network-error-e2e.spec.ts     # Run specific test file"
}

# Function to show service status
show_status() {
    echo "Service Status:"
    echo "==============="
    
    if is_port_in_use $MOCK_BACKEND_PORT; then
        if curl -f -s "http://localhost:$MOCK_BACKEND_PORT/test/status" >/dev/null 2>&1; then
            print_success "Mock Backend: Running on port $MOCK_BACKEND_PORT"
            curl -s "http://localhost:$MOCK_BACKEND_PORT/test/status" | jq '.' 2>/dev/null || echo "  (Status endpoint available but response not JSON)"
        else
            print_warning "Port $MOCK_BACKEND_PORT: In use but not our mock backend"
        fi
    else
        print_error "Mock Backend: Not running"
    fi
    
    if is_port_in_use $FRONTEND_PORT; then
        if curl -f -s "http://localhost:$FRONTEND_PORT" >/dev/null 2>&1; then
            print_success "Frontend: Running on port $FRONTEND_PORT"
        else
            print_warning "Port $FRONTEND_PORT: In use but not responding to HTTP"
        fi
    else
        print_error "Frontend: Not running"
    fi
}

# Trap to cleanup on exit
trap 'stop_services' EXIT INT TERM

# Parse command line arguments
START_FRONTEND=false
KEEP_SERVICES=false
STOP_ONLY=false
SHOW_STATUS=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --start-frontend)
            START_FRONTEND=true
            shift
            ;;
        --keep-services)
            KEEP_SERVICES=true
            shift
            ;;
        --stop-only)
            STOP_ONLY=true
            shift
            ;;
        --status)
            SHOW_STATUS=true
            shift
            ;;
        --help)
            show_usage
            exit 0
            ;;
        *)
            # Pass remaining arguments to Playwright
            break
            ;;
    esac
done

# Main execution
main() {
    print_status "NetworkErrorFallback E2E Test Runner"
    print_status "====================================="
    
    if [ "$SHOW_STATUS" = "true" ]; then
        show_status
        exit 0
    fi
    
    if [ "$STOP_ONLY" = "true" ]; then
        stop_services
        exit 0
    fi
    
    # Check if jq is available for JSON parsing (optional)
    if ! command -v jq &> /dev/null; then
        print_warning "jq not found. Status output will be less formatted."
    fi
    
    # Start services
    start_mock_backend
    
    if [ "$START_FRONTEND" = "true" ]; then
        start_frontend
    else
        # Just check if frontend is available
        if ! is_port_in_use $FRONTEND_PORT; then
            print_error "Frontend is not running. Use --start-frontend or start it manually."
            exit 1
        fi
        wait_for_service "http://localhost:$FRONTEND_PORT" "Frontend"
    fi
    
    # Run tests
    if run_tests "$@"; then
        test_exit_code=0
        print_success "Test run completed successfully!"
    else
        test_exit_code=1
        print_error "Test run failed!"
    fi
    
    # Cleanup services unless --keep-services is specified
    if [ "$KEEP_SERVICES" = "true" ]; then
        print_status "Keeping services running as requested..."
        print_status "Mock Backend: http://localhost:$MOCK_BACKEND_PORT"
        print_status "Frontend: http://localhost:$FRONTEND_PORT"
        print_status ""
        print_status "To stop services manually:"
        print_status "  $0 --stop-only"
        # Don't run cleanup on exit
        trap - EXIT INT TERM
    else
        stop_services
    fi
    
    exit $test_exit_code
}

# Check if we're being sourced or executed
if [ "${BASH_SOURCE[0]}" = "${0}" ]; then
    main "$@"
fi