#!/bin/bash
# FocusHive Gatling Load Test Runner
# Comprehensive script for executing Gatling load tests

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../" && pwd)"
GATLING_DIR="$PROJECT_ROOT/load-tests/gatling"
RESULTS_DIR="$GATLING_DIR/results"
REPORTS_DIR="$GATLING_DIR/reports"

# Test configuration
DEFAULT_USERS=10
DEFAULT_DURATION=300
DEFAULT_RAMP_DURATION=60

# Create directories
mkdir -p "$RESULTS_DIR" "$REPORTS_DIR"

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Logging functions
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

success() {
    echo -e "${GREEN}[SUCCESS] $1${NC}"
}

warning() {
    echo -e "${YELLOW}[WARNING] $1${NC}"
}

error() {
    echo -e "${RED}[ERROR] $1${NC}"
}

info() {
    echo -e "${PURPLE}[INFO] $1${NC}"
}

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
    # Check if SBT is installed
    if ! command -v sbt &> /dev/null; then
        error "SBT is not installed. Please install SBT to run Gatling tests."
        info "Install SBT from: https://www.scala-sbt.org/download.html"
        exit 1
    fi
    
    # Check if Java is installed
    if ! command -v java &> /dev/null; then
        error "Java is not installed. Please install Java 8 or later."
        exit 1
    fi
    
    # Check Java version
    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1-2)
    log "Using Java version: $java_version"
    
    # Check if project structure exists
    if [[ ! -f "$GATLING_DIR/build.sbt" ]]; then
        error "Gatling project not found. Please ensure the Gatling test structure exists."
        exit 1
    fi
    
    success "Prerequisites check passed"
}

# Wait for FocusHive services
wait_for_service() {
    local url=$1
    local service_name=$2
    local max_attempts=20
    local attempt=1
    
    log "Waiting for $service_name to be ready at $url"
    
    while [[ $attempt -le $max_attempts ]]; do
        if curl -s -f "$url/actuator/health" > /dev/null 2>&1 || \
           curl -s -f "$url/health" > /dev/null 2>&1 || \
           curl -s -f "$url" > /dev/null 2>&1; then
            success "$service_name is ready"
            return 0
        fi
        
        log "Attempt $attempt/$max_attempts: $service_name not ready, waiting..."
        sleep 3
        ((attempt++))
    done
    
    warning "$service_name not ready after $max_attempts attempts"
    return 1
}

# Check service availability
check_services() {
    log "Checking FocusHive services availability..."
    
    local services=(
        "http://localhost:8080:FocusHive Backend"
        "http://localhost:8081:Identity Service"
        "http://localhost:8082:Music Service"
        "http://localhost:8083:Notification Service"
        "http://localhost:8084:Chat Service"
        "http://localhost:8085:Analytics Service"
        "http://localhost:8086:Forum Service"
        "http://localhost:8087:Buddy Service"
    )
    
    local ready_services=0
    for service in "${services[@]}"; do
        IFS=':' read -r url name <<< "$service"
        if wait_for_service "$url" "$name"; then
            ((ready_services++))
        fi
    done
    
    log "$ready_services out of ${#services[@]} services are ready"
    
    if [[ $ready_services -eq 0 ]]; then
        error "No services are ready. Please start the FocusHive services first."
        exit 1
    fi
}

# Run Gatling test
run_gatling_test() {
    local test_class=$1
    local test_name=$2
    local users=${3:-$DEFAULT_USERS}
    local duration=${4:-$DEFAULT_DURATION}
    local ramp_duration=${5:-$DEFAULT_RAMP_DURATION}
    
    log "Running Gatling test: $test_name"
    info "Configuration: $users users, ${duration}s duration, ${ramp_duration}s ramp-up"
    
    cd "$GATLING_DIR"
    
    # Set system properties for the test
    export JAVA_OPTS="-Dfocushive.baseUrl=http://localhost:8080 \
                      -Dusers=$users \
                      -Dtest.duration=$duration \
                      -Dramp.duration=$ramp_duration \
                      -Dfocushive.wsUrl=ws://localhost:8080 \
                      -Dfile.encoding=UTF-8 \
                      -Xms1G -Xmx2G"
    
    # Run the test
    if sbt "Gatling/testOnly $test_class"; then
        success "$test_name completed successfully"
        
        # Find the latest results
        local latest_result=$(find "$GATLING_DIR/target/gatling" -name "simulation.log" -type f -printf '%T@ %p\n' | sort -rn | head -1 | cut -d' ' -f2-)
        if [[ -n "$latest_result" ]]; then
            local result_dir=$(dirname "$latest_result")
            local report_dir="$REPORTS_DIR/$(basename "$result_dir")"
            
            # Copy results to reports directory
            if [[ -d "$result_dir" ]]; then
                cp -r "$result_dir" "$report_dir"
                success "Test report saved to: $report_dir"
                
                # Display summary if available
                local index_file="$report_dir/index.html"
                if [[ -f "$index_file" ]]; then
                    info "HTML report available at: file://$index_file"
                fi
            fi
        fi
        
        return 0
    else
        error "$test_name failed"
        return 1
    fi
}

# API Load Test
run_api_test() {
    local users=${1:-10}
    local duration=${2:-300}
    run_gatling_test "focushive.FocusHiveApiLoadTest" "API Load Test" "$users" "$duration" 60
}

# Stress Test
run_stress_test() {
    local users=${1:-50}
    local duration=${2:-600}
    run_gatling_test "focushive.FocusHiveStressTest" "Stress Test" "$users" "$duration" 120
}

# WebSocket Test
run_websocket_test() {
    local users=${1:-15}
    local duration=${2:-300}
    run_gatling_test "focushive.FocusHiveWebSocketTest" "WebSocket Test" "$users" "$duration" 45
}

# Run all tests
run_all_tests() {
    log "Running complete Gatling test suite..."
    
    local failed_tests=()
    
    info "=== Starting API Load Test ==="
    if ! run_api_test 10 300; then
        failed_tests+=("API Load Test")
    fi
    
    sleep 30 # Cooldown between tests
    
    info "=== Starting WebSocket Test ==="
    if ! run_websocket_test 15 300; then
        failed_tests+=("WebSocket Test")  
    fi
    
    sleep 30 # Cooldown between tests
    
    info "=== Starting Stress Test ==="
    if ! run_stress_test 30 400; then
        failed_tests+=("Stress Test")
    fi
    
    log "Gatling test suite completed"
    
    if [[ ${#failed_tests[@]} -eq 0 ]]; then
        success "All Gatling tests passed!"
    else
        warning "Failed tests: ${failed_tests[*]}"
        return 1
    fi
}

# Generate consolidated report
generate_consolidated_report() {
    log "Generating consolidated Gatling report..."
    
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local consolidated_report="$REPORTS_DIR/consolidated_gatling_report_$timestamp.html"
    
    cat > "$consolidated_report" << 'EOF'
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FocusHive Gatling Test Results</title>
    <style>
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }
        .container { max-width: 1200px; margin: 0 auto; }
        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 10px; text-align: center; margin-bottom: 30px; }
        .test-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(350px, 1fr)); gap: 20px; }
        .test-card { background: white; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); padding: 25px; }
        .test-header { border-bottom: 2px solid #eee; padding-bottom: 15px; margin-bottom: 20px; }
        .test-title { color: #333; font-size: 1.4em; font-weight: bold; }
        .metric-row { display: flex; justify-content: space-between; margin: 10px 0; padding: 8px; background: #f9f9f9; border-radius: 4px; }
        .metric-label { font-weight: bold; color: #555; }
        .metric-value { color: #333; }
        .success { color: #27ae60; font-weight: bold; }
        .warning { color: #f39c12; font-weight: bold; }
        .error { color: #e74c3c; font-weight: bold; }
        .footer { text-align: center; margin-top: 40px; padding: 20px; color: #666; }
        .links { margin-top: 15px; }
        .links a { color: #667eea; text-decoration: none; margin: 0 10px; padding: 8px 16px; background: #eef1ff; border-radius: 4px; display: inline-block; }
        .links a:hover { background: #dde4ff; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🚀 FocusHive Gatling Load Test Results</h1>
            <p>Generated on: $(date)</p>
            <p>Test Environment: Local Development</p>
        </div>
        
        <div class="test-grid">
EOF

    # Process each test report
    for report_dir in "$REPORTS_DIR"/focushivea*/; do
        if [[ -d "$report_dir" ]]; then
            local test_name=$(basename "$report_dir" | sed 's/focushive//' | sed 's/[0-9-]*$//' | tr '[:lower:]' '[:upper:]')
            local simulation_log="$report_dir/simulation.log"
            
            echo "<div class='test-card'>" >> "$consolidated_report"
            echo "<div class='test-header'><div class='test-title'>$test_name Test</div></div>" >> "$consolidated_report"
            
            if [[ -f "$simulation_log" ]]; then
                # Parse simulation.log and extract metrics
                echo "<div class='metric-row'><span class='metric-label'>Status:</span><span class='metric-value success'>Completed</span></div>" >> "$consolidated_report"
                
                # Add link to detailed report
                local index_file="$report_dir/index.html"
                if [[ -f "$index_file" ]]; then
                    echo "<div class='links'><a href='file://$index_file' target='_blank'>View Detailed Report</a></div>" >> "$consolidated_report"
                fi
            else
                echo "<div class='metric-row'><span class='metric-label'>Status:</span><span class='metric-value error'>No Results Found</span></div>" >> "$consolidated_report"
            fi
            
            echo "</div>" >> "$consolidated_report"
        fi
    done
    
    cat >> "$consolidated_report" << 'EOF'
        </div>
        
        <div class="footer">
            <p>FocusHive Load Testing Suite - Powered by Gatling</p>
            <p>For more information, visit the individual test reports</p>
        </div>
    </div>
</body>
</html>
EOF
    
    success "Consolidated report generated: $consolidated_report"
    info "Open in browser: file://$consolidated_report"
}

# Clean old results
cleanup() {
    log "Cleaning up old test results..."
    
    # Keep only last 5 results
    find "$RESULTS_DIR" -maxdepth 1 -type d -name "*" -printf '%T@ %p\n' 2>/dev/null | \
        sort -rn | tail -n +6 | cut -d' ' -f2- | xargs -r rm -rf
    
    find "$REPORTS_DIR" -maxdepth 1 -type d -name "*" -printf '%T@ %p\n' 2>/dev/null | \
        sort -rn | tail -n +6 | cut -d' ' -f2- | xargs -r rm -rf
    
    # Clean Gatling target directory
    if [[ -d "$GATLING_DIR/target/gatling" ]]; then
        find "$GATLING_DIR/target/gatling" -maxdepth 1 -type d -name "*" -printf '%T@ %p\n' 2>/dev/null | \
            sort -rn | tail -n +6 | cut -d' ' -f2- | xargs -r rm -rf
    fi
    
    success "Cleanup completed"
}

# Main execution
main() {
    log "FocusHive Gatling Load Test Runner"
    log "=================================="
    
    check_prerequisites
    
    case "${1:-help}" in
        api)
            check_services
            run_api_test "${2:-10}" "${3:-300}"
            ;;
        stress)
            check_services  
            run_stress_test "${2:-30}" "${3:-400}"
            ;;
        websocket|ws)
            check_services
            run_websocket_test "${2:-15}" "${3:-300}"
            ;;
        all)
            check_services
            run_all_tests
            generate_consolidated_report
            ;;
        compile)
            log "Compiling Gatling tests..."
            cd "$GATLING_DIR"
            sbt compile
            ;;
        cleanup)
            cleanup
            ;;
        help|*)
            echo "Usage: $0 {api|stress|websocket|all|compile|cleanup} [users] [duration]"
            echo ""
            echo "Test Types:"
            echo "  api       - API load test (default: 10 users, 300s)"
            echo "  stress    - High-load stress test (default: 30 users, 400s)"  
            echo "  websocket - WebSocket test (default: 15 users, 300s)"
            echo "  all       - Run all tests sequentially"
            echo "  compile   - Compile Gatling tests only"
            echo "  cleanup   - Clean old test results"
            echo ""
            echo "Parameters:"
            echo "  users     - Number of concurrent users (default varies by test)"
            echo "  duration  - Test duration in seconds (default varies by test)"
            echo ""
            echo "Examples:"
            echo "  $0 api 20 600          # API test with 20 users for 10 minutes"
            echo "  $0 stress 50 300       # Stress test with 50 users for 5 minutes"
            echo "  $0 websocket 25        # WebSocket test with 25 users (default duration)"
            echo "  $0 all                 # Run complete test suite"
            echo ""
            echo "Environment:"
            echo "  Requires SBT and Java 8+"
            echo "  FocusHive services should be running on localhost"
            exit 0
            ;;
    esac
}

# Execute main function
main "$@"