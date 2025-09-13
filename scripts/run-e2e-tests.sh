#!/bin/bash

# ===================================================================
# COMPREHENSIVE E2E TEST ORCHESTRATION SCRIPT
# 
# Master script for E2E testing environment management and test execution
# Integrates all components: setup, health checks, data seeding, and testing
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
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.e2e.yml}"

# Test configuration
E2E_SUITE="${E2E_SUITE:-all}"
E2E_BROWSER="${E2E_BROWSER:-chromium}"
E2E_HEADLESS="${E2E_HEADLESS:-true}"
E2E_WORKERS="${E2E_WORKERS:-4}"
E2E_RETRIES="${E2E_RETRIES:-2}"
E2E_TIMEOUT="${E2E_TIMEOUT:-30000}"

# Flags
SKIP_SETUP=false
SKIP_HEALTH_CHECK=false
SKIP_DATA_SEEDING=false
FORCE_REBUILD=false
KEEP_ENV_RUNNING=false
VERBOSE=false
DRY_RUN=false

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

log_verbose() {
    if [ "$VERBOSE" = true ]; then
        echo -e "${CYAN}[VERBOSE]${NC} $1"
    fi
}

# Show help
show_help() {
    cat << 'EOF'
Usage: ./scripts/run-e2e-tests.sh [command] [options]

Commands:
  setup               Set up E2E environment only
  start              Start E2E environment (quick start)
  test               Run E2E tests (default)
  stop               Stop E2E environment
  clean              Clean up E2E environment
  status             Show environment status
  logs [service]     Show logs for service
  shell [service]    Open shell in service container
  help               Show this help

Options:
  --suite SUITE          Test suite to run (all, critical, smoke, integration)
  --browser BROWSER      Browser to use (chromium, firefox, webkit)
  --headless            Run tests in headless mode (default)
  --headed              Run tests in headed mode
  --workers N           Number of parallel workers (default: 4)
  --retries N           Number of retries per test (default: 2)
  --timeout MS          Test timeout in milliseconds (default: 30000)
  
  --skip-setup          Skip environment setup
  --skip-health-check   Skip health checks
  --skip-data-seeding   Skip test data seeding
  --force-rebuild       Force rebuild of all images
  --keep-running        Keep environment running after tests
  --verbose, -v         Enable verbose logging
  --dry-run            Show what would be done without executing

Environment Variables:
  COMPOSE_FILE          Docker Compose file (default: docker-compose.e2e.yml)
  E2E_SUITE            Default test suite
  E2E_BROWSER          Default browser
  E2E_HEADLESS         Default headless mode
  E2E_WORKERS          Default number of workers
  E2E_RETRIES          Default number of retries

Examples:
  ./scripts/run-e2e-tests.sh                           # Run all tests
  ./scripts/run-e2e-tests.sh test --suite smoke        # Run smoke tests only
  ./scripts/run-e2e-tests.sh setup                     # Set up environment only
  ./scripts/run-e2e-tests.sh test --headed --workers 1 # Run tests with UI
  ./scripts/run-e2e-tests.sh logs focushive-backend    # Show backend logs
  ./scripts/run-e2e-tests.sh clean --force             # Force clean environment

EOF
}

# Parse command line arguments
parse_arguments() {
    local command=""
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            setup|start|test|stop|clean|status|logs|shell|help)
                if [ -z "$command" ]; then
                    command=$1
                    shift
                else
                    log_error "Multiple commands specified: $command and $1"
                    exit 1
                fi
                ;;
            --suite)
                E2E_SUITE="$2"
                shift 2
                ;;
            --browser)
                E2E_BROWSER="$2"
                shift 2
                ;;
            --headless)
                E2E_HEADLESS=true
                shift
                ;;
            --headed)
                E2E_HEADLESS=false
                shift
                ;;
            --workers)
                E2E_WORKERS="$2"
                shift 2
                ;;
            --retries)
                E2E_RETRIES="$2"
                shift 2
                ;;
            --timeout)
                E2E_TIMEOUT="$2"
                shift 2
                ;;
            --skip-setup)
                SKIP_SETUP=true
                shift
                ;;
            --skip-health-check)
                SKIP_HEALTH_CHECK=true
                shift
                ;;
            --skip-data-seeding)
                SKIP_DATA_SEEDING=true
                shift
                ;;
            --force-rebuild)
                FORCE_REBUILD=true
                shift
                ;;
            --keep-running)
                KEEP_ENV_RUNNING=true
                shift
                ;;
            --verbose|-v)
                VERBOSE=true
                shift
                ;;
            --dry-run)
                DRY_RUN=true
                shift
                ;;
            --help|-h)
                show_help
                exit 0
                ;;
            *)
                if [ -z "$command" ] && [[ ! $1 =~ ^-- ]]; then
                    # First non-flag argument is command
                    command=$1
                    shift
                else
                    log_error "Unknown option: $1"
                    show_help
                    exit 1
                fi
                ;;
        esac
    done
    
    # Default command is test
    COMMAND=${command:-test}
    
    # Validate suite
    case $E2E_SUITE in
        all|critical|smoke|integration|auth|hive|chat|music|analytics|forum|buddy) ;;
        *) log_error "Invalid test suite: $E2E_SUITE"; exit 1 ;;
    esac
    
    # Validate browser
    case $E2E_BROWSER in
        chromium|firefox|webkit) ;;
        *) log_error "Invalid browser: $E2E_BROWSER"; exit 1 ;;
    esac
}

# Check if scripts exist
check_scripts() {
    local required_scripts=(
        "setup-e2e-env.sh"
        "health-check.sh"
        "seed-test-data.sh"
        "cleanup-e2e.sh"
    )
    
    for script in "${required_scripts[@]}"; do
        if [ ! -f "$SCRIPT_DIR/$script" ]; then
            log_error "Required script not found: $SCRIPT_DIR/$script"
            exit 1
        fi
        
        if [ ! -x "$SCRIPT_DIR/$script" ]; then
            log_warning "Making script executable: $script"
            chmod +x "$SCRIPT_DIR/$script"
        fi
    done
}

# Set up E2E environment
setup_environment() {
    log_section "Setting Up E2E Environment"
    
    if [ "$SKIP_SETUP" = true ]; then
        log_info "Skipping environment setup (--skip-setup flag)"
        return 0
    fi
    
    local setup_args=()
    
    if [ "$FORCE_REBUILD" = true ]; then
        setup_args+=("rebuild")
    fi
    
    if [ "$VERBOSE" = true ]; then
        log_verbose "Running: $SCRIPT_DIR/setup-e2e-env.sh ${setup_args[*]}"
    fi
    
    if [ "$DRY_RUN" = true ]; then
        log_info "[DRY RUN] Would run: setup-e2e-env.sh ${setup_args[*]}"
        return 0
    fi
    
    if "$SCRIPT_DIR/setup-e2e-env.sh" "${setup_args[@]}"; then
        log_success "Environment setup completed"
    else
        log_error "Environment setup failed"
        return 1
    fi
}

# Perform health check
check_health() {
    log_section "Performing Health Check"
    
    if [ "$SKIP_HEALTH_CHECK" = true ]; then
        log_info "Skipping health check (--skip-health-check flag)"
        return 0
    fi
    
    if [ "$DRY_RUN" = true ]; then
        log_info "[DRY RUN] Would run health check"
        return 0
    fi
    
    log_step "Waiting for all services to be healthy..."
    
    if "$SCRIPT_DIR/health-check.sh" wait; then
        log_success "All services are healthy"
    else
        log_error "Health check failed"
        
        # Show failing services for debugging
        log_info "Running quick health check to identify issues..."
        "$SCRIPT_DIR/health-check.sh" quick || true
        
        return 1
    fi
}

# Seed test data
seed_data() {
    log_section "Seeding Test Data"
    
    if [ "$SKIP_DATA_SEEDING" = true ]; then
        log_info "Skipping test data seeding (--skip-data-seeding flag)"
        return 0
    fi
    
    if [ "$DRY_RUN" = true ]; then
        log_info "[DRY RUN] Would seed test data"
        return 0
    fi
    
    if "$SCRIPT_DIR/seed-test-data.sh"; then
        log_success "Test data seeded successfully"
    else
        log_warning "Test data seeding had issues, but continuing with tests"
        # Don't fail here as tests might still work with partial data
    fi
}

# Run E2E tests
run_tests() {
    log_section "Running E2E Tests"
    
    if [ "$DRY_RUN" = true ]; then
        log_info "[DRY RUN] Would run E2E tests with suite: $E2E_SUITE, browser: $E2E_BROWSER"
        return 0
    fi
    
    # Check if frontend directory exists
    if [ ! -d "$PROJECT_ROOT/frontend" ]; then
        log_error "Frontend directory not found: $PROJECT_ROOT/frontend"
        return 1
    fi
    
    cd "$PROJECT_ROOT/frontend"
    
    # Install dependencies if needed
    if [ ! -d "node_modules" ] || [ "package.json" -nt "node_modules/.package-lock.json" ]; then
        log_step "Installing frontend dependencies..."
        npm install
    fi
    
    # Set environment variables for tests
    export E2E_BASE_URL="http://localhost:3000"
    export E2E_API_URL="http://localhost:8080"
    export E2E_BROWSER="$E2E_BROWSER"
    export E2E_HEADLESS="$E2E_HEADLESS"
    export E2E_WORKERS="$E2E_WORKERS"
    export E2E_RETRIES="$E2E_RETRIES"
    export E2E_TIMEOUT="$E2E_TIMEOUT"
    
    # Build test command
    local test_cmd="npx playwright test"
    
    # Add suite-specific tests
    case $E2E_SUITE in
        critical)
            test_cmd+=" --grep @critical"
            ;;
        smoke)
            test_cmd+=" --grep @smoke"
            ;;
        integration)
            test_cmd+=" --grep @integration"
            ;;
        auth)
            test_cmd+=" tests/auth/"
            ;;
        hive)
            test_cmd+=" tests/hive/"
            ;;
        chat)
            test_cmd+=" tests/chat/"
            ;;
        music)
            test_cmd+=" tests/music/"
            ;;
        analytics)
            test_cmd+=" tests/analytics/"
            ;;
        forum)
            test_cmd+=" tests/forum/"
            ;;
        buddy)
            test_cmd+=" tests/buddy/"
            ;;
        all)
            # Run all tests
            ;;
    esac
    
    # Add common options
    test_cmd+=" --workers=$E2E_WORKERS"
    test_cmd+=" --retries=$E2E_RETRIES"
    test_cmd+=" --timeout=$E2E_TIMEOUT"
    test_cmd+=" --project=$E2E_BROWSER"
    
    if [ "$E2E_HEADLESS" = true ]; then
        test_cmd+=" --headed=false"
    else
        test_cmd+=" --headed=true"
    fi
    
    if [ "$VERBOSE" = true ]; then
        test_cmd+=" --reporter=list"
        log_verbose "Running: $test_cmd"
    else
        test_cmd+=" --reporter=html"
    fi
    
    log_step "Executing E2E tests..."
    log_info "Command: $test_cmd"
    
    # Run tests with proper error handling
    local test_exit_code=0
    if ! eval "$test_cmd"; then
        test_exit_code=$?
        log_error "E2E tests failed with exit code: $test_exit_code"
        
        # Show test results location
        if [ -d "playwright-report" ]; then
            log_info "Test results available at: $PROJECT_ROOT/frontend/playwright-report/index.html"
        fi
        
        if [ -d "test-results" ]; then
            log_info "Test artifacts available at: $PROJECT_ROOT/frontend/test-results/"
        fi
        
        return $test_exit_code
    fi
    
    log_success "E2E tests completed successfully"
    
    # Show test results
    if [ -d "playwright-report" ]; then
        log_success "Test report generated: $PROJECT_ROOT/frontend/playwright-report/index.html"
    fi
    
    return 0
}

# Stop environment
stop_environment() {
    log_section "Stopping E2E Environment"
    
    if [ "$DRY_RUN" = true ]; then
        log_info "[DRY RUN] Would stop E2E environment"
        return 0
    fi
    
    cd "$PROJECT_ROOT"
    
    log_step "Stopping all services..."
    docker compose -f "$COMPOSE_FILE" stop --timeout=30
    
    log_success "E2E environment stopped"
}

# Clean up environment
cleanup_environment() {
    log_section "Cleaning Up E2E Environment"
    
    if [ "$DRY_RUN" = true ]; then
        log_info "[DRY RUN] Would clean up E2E environment"
        return 0
    fi
    
    local cleanup_args=()
    
    if [ "$FORCE_REBUILD" = true ]; then
        cleanup_args+=("--force")
    fi
    
    if "$SCRIPT_DIR/cleanup-e2e.sh" "${cleanup_args[@]}"; then
        log_success "Environment cleanup completed"
    else
        log_error "Environment cleanup failed"
        return 1
    fi
}

# Show environment status
show_status() {
    log_section "E2E Environment Status"
    
    cd "$PROJECT_ROOT"
    
    # Show running containers
    echo -e "${CYAN}Running Containers:${NC}"
    docker compose -f "$COMPOSE_FILE" ps --format table
    
    # Quick health check
    log_step "Quick health check..."
    if "$SCRIPT_DIR/health-check.sh" quick; then
        log_success "Core services are healthy"
    else
        log_warning "Some services may have issues"
    fi
}

# Show logs for service
show_logs() {
    local service=${1:-""}
    
    if [ -z "$service" ]; then
        log_info "Available services:"
        docker compose -f "$COMPOSE_FILE" config --services | sed 's/^/  - /'
        return 1
    fi
    
    cd "$PROJECT_ROOT"
    
    log_info "Showing logs for service: $service"
    docker compose -f "$COMPOSE_FILE" logs -f "$service"
}

# Open shell in service
open_shell() {
    local service=${1:-""}
    
    if [ -z "$service" ]; then
        log_info "Available services:"
        docker compose -f "$COMPOSE_FILE" config --services | sed 's/^/  - /'
        return 1
    fi
    
    cd "$PROJECT_ROOT"
    
    log_info "Opening shell in service: $service"
    docker compose -f "$COMPOSE_FILE" exec "$service" /bin/sh
}

# Handle script interruption
handle_interrupt() {
    echo -e "\n\n${YELLOW}Script interrupted!${NC}"
    
    if [ "$KEEP_ENV_RUNNING" = false ] && [ "$COMMAND" = "test" ]; then
        log_info "Cleaning up environment..."
        cleanup_environment || true
    fi
    
    exit 130
}

# Main function
main() {
    echo "═════════════════════════════════════════"
    echo "🧪 FocusHive E2E Test Orchestration"
    echo "═════════════════════════════════════════"
    echo ""
    
    # Change to project root
    cd "$PROJECT_ROOT"
    
    # Set up signal handlers
    trap handle_interrupt INT TERM
    
    # Check prerequisites
    check_scripts
    
    # Parse arguments first
    parse_arguments "$@"
    
    # Show configuration
    if [ "$VERBOSE" = true ]; then
        log_section "Configuration"
        log_verbose "Command: $COMMAND"
        log_verbose "Project Root: $PROJECT_ROOT"
        log_verbose "Compose File: $COMPOSE_FILE"
        log_verbose "Test Suite: $E2E_SUITE"
        log_verbose "Browser: $E2E_BROWSER"
        log_verbose "Headless: $E2E_HEADLESS"
        log_verbose "Workers: $E2E_WORKERS"
        log_verbose "Retries: $E2E_RETRIES"
        log_verbose "Timeout: $E2E_TIMEOUT"
    fi
    
    # Execute command
    case $COMMAND in
        setup)
            setup_environment
            ;;
        start)
            setup_environment
            check_health
            seed_data
            show_status
            ;;
        test)
            setup_environment
            check_health
            seed_data
            
            # Run tests and capture exit code
            local test_result=0
            if ! run_tests; then
                test_result=$?
            fi
            
            # Clean up unless keeping environment
            if [ "$KEEP_ENV_RUNNING" = false ]; then
                cleanup_environment
            else
                log_info "Keeping environment running (--keep-running flag)"
            fi
            
            exit $test_result
            ;;
        stop)
            stop_environment
            ;;
        clean)
            cleanup_environment
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs "$2"
            ;;
        shell)
            open_shell "$2"
            ;;
        help)
            show_help
            ;;
        *)
            log_error "Unknown command: $COMMAND"
            show_help
            exit 1
            ;;
    esac
}

# Run main function
main "$@"