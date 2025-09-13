#!/bin/bash
# FocusHive JMeter Load Test Runner Script
# Executes various load test scenarios and generates reports

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../" && pwd)"
JMETER_HOME="${JMETER_HOME:-/opt/apache-jmeter}"
JMETER_BIN="$JMETER_HOME/bin/jmeter"

# Test configuration
TEST_PLANS_DIR="$PROJECT_ROOT/load-tests/jmeter/test-plans"
CONFIG_DIR="$PROJECT_ROOT/load-tests/jmeter/config"
REPORTS_DIR="$PROJECT_ROOT/load-tests/jmeter/reports"
RESULTS_DIR="$PROJECT_ROOT/load-tests/jmeter/results"

# Create directories
mkdir -p "$REPORTS_DIR" "$RESULTS_DIR"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
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

# Check if JMeter is installed
check_jmeter() {
    if [[ ! -f "$JMETER_BIN" ]]; then
        error "JMeter not found at $JMETER_BIN"
        error "Please install JMeter or set JMETER_HOME environment variable"
        exit 1
    fi
    log "Using JMeter at: $JMETER_BIN"
}

# Wait for services to be ready
wait_for_service() {
    local url=$1
    local service_name=$2
    local max_attempts=30
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
        sleep 5
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

# Run a specific test plan
run_test() {
    local test_plan=$1
    local test_name=$2
    local threads=${3:-10}
    local duration=${4:-300}
    local ramp_time=${5:-60}
    
    log "Running $test_name test..."
    log "Configuration: $threads threads, ${duration}s duration, ${ramp_time}s ramp-up"
    
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local results_file="$RESULTS_DIR/${test_name}_${timestamp}.jtl"
    local report_dir="$REPORTS_DIR/${test_name}_${timestamp}"
    
    # JMeter command arguments
    local jmeter_args=(
        -n  # Non-GUI mode
        -t "$test_plan"  # Test plan file
        -l "$results_file"  # Results file
        -e  # Generate HTML report
        -o "$report_dir"  # HTML report output directory
        -J "load.test.threads=$threads"
        -J "default.test.duration=$duration"
        -J "default.ramp.time=$ramp_time"
        -J "test.data.cleanup=true"
        -q "$CONFIG_DIR/test-variables.properties"
    )
    
    # Run the test
    if "$JMETER_BIN" "${jmeter_args[@]}"; then
        success "$test_name test completed successfully"
        log "Results saved to: $results_file"
        log "HTML report generated at: $report_dir"
        
        # Display summary statistics
        if [[ -f "$results_file" ]]; then
            log "Test Summary:"
            awk -F',' '
                NR > 1 {
                    if ($8 == "true") success++; else error++;
                    total++;
                    response_time += $2;
                }
                END {
                    if (total > 0) {
                        printf "  Total Requests: %d\n", total;
                        printf "  Successful: %d (%.2f%%)\n", success, (success/total)*100;
                        printf "  Errors: %d (%.2f%%)\n", error, (error/total)*100;
                        printf "  Average Response Time: %.2f ms\n", response_time/total;
                    }
                }
            ' "$results_file"
        fi
        
        return 0
    else
        error "$test_name test failed"
        return 1
    fi
}

# Run smoke test
run_smoke_test() {
    log "Starting Smoke Test (basic functionality verification)"
    run_test "$TEST_PLANS_DIR/focushive-api-load-test.jmx" "smoke" 1 60 10
}

# Run load test
run_load_test() {
    log "Starting Load Test (normal expected load)"
    run_test "$TEST_PLANS_DIR/focushive-api-load-test.jmx" "load" 10 300 60
}

# Run stress test
run_stress_test() {
    log "Starting Stress Test (high load conditions)"
    run_test "$TEST_PLANS_DIR/focushive-api-load-test.jmx" "stress" 50 600 120
}

# Run spike test
run_spike_test() {
    log "Starting Spike Test (sudden load increase)"
    run_test "$TEST_PLANS_DIR/focushive-api-load-test.jmx" "spike" 100 180 10
}

# Run all tests
run_all_tests() {
    log "Running complete FocusHive load test suite..."
    
    local failed_tests=()
    
    if ! run_smoke_test; then
        failed_tests+=("Smoke")
    fi
    
    if ! run_load_test; then
        failed_tests+=("Load")
    fi
    
    if ! run_stress_test; then
        failed_tests+=("Stress")
    fi
    
    if ! run_spike_test; then
        failed_tests+=("Spike")
    fi
    
    log "Load test suite completed"
    
    if [[ ${#failed_tests[@]} -eq 0 ]]; then
        success "All tests passed!"
    else
        warning "Failed tests: ${failed_tests[*]}"
        return 1
    fi
}

# Generate consolidated report
generate_consolidated_report() {
    log "Generating consolidated test report..."
    
    local report_file="$REPORTS_DIR/consolidated_report_$(date +%Y%m%d_%H%M%S).html"
    
    cat > "$report_file" << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>FocusHive Load Test Results</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #f0f0f0; padding: 20px; border-radius: 5px; }
        .test-section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }
        .success { color: green; } .error { color: red; } .warning { color: orange; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
    </style>
</head>
<body>
    <div class="header">
        <h1>FocusHive Load Test Results</h1>
        <p>Generated on: $(date)</p>
        <p>Test Environment: Local Development</p>
    </div>
EOF
    
    # Add individual test results
    for result_file in "$RESULTS_DIR"/*.jtl; do
        if [[ -f "$result_file" ]]; then
            local test_name=$(basename "$result_file" .jtl)
            echo "<div class='test-section'>" >> "$report_file"
            echo "<h2>$test_name Test Results</h2>" >> "$report_file"
            
            # Process JTL file and add summary
            awk -F',' -v test_name="$test_name" '
                BEGIN { print "<table><tr><th>Metric</th><th>Value</th></tr>" }
                NR > 1 {
                    if ($8 == "true") success++; else error++;
                    total++;
                    response_time += $2;
                    if ($2 > max_time || max_time == 0) max_time = $2;
                    if ($2 < min_time || min_time == 0) min_time = $2;
                }
                END {
                    if (total > 0) {
                        printf "<tr><td>Total Requests</td><td>%d</td></tr>", total;
                        printf "<tr><td>Success Rate</td><td class=\"%s\">%.2f%%</td></tr>", 
                               (success/total > 0.95) ? "success" : "error", (success/total)*100;
                        printf "<tr><td>Error Rate</td><td class=\"%s\">%.2f%%</td></tr>", 
                               (error/total < 0.05) ? "success" : "error", (error/total)*100;
                        printf "<tr><td>Average Response Time</td><td>%.2f ms</td></tr>", response_time/total;
                        printf "<tr><td>Min Response Time</td><td>%.2f ms</td></tr>", min_time;
                        printf "<tr><td>Max Response Time</td><td>%.2f ms</td></tr>", max_time;
                    }
                    print "</table>";
                }
            ' "$result_file" >> "$report_file"
            
            echo "</div>" >> "$report_file"
        fi
    done
    
    echo "</body></html>" >> "$report_file"
    
    success "Consolidated report generated: $report_file"
}

# Cleanup old results
cleanup() {
    log "Cleaning up old test results..."
    
    # Keep only last 5 test results
    find "$RESULTS_DIR" -name "*.jtl" -type f -printf '%T@ %p\n' | \
        sort -rn | tail -n +6 | cut -d' ' -f2- | xargs -r rm -f
    
    # Keep only last 5 HTML reports
    find "$REPORTS_DIR" -maxdepth 1 -type d -name "*_20*" -printf '%T@ %p\n' | \
        sort -rn | tail -n +6 | cut -d' ' -f2- | xargs -r rm -rf
    
    log "Cleanup completed"
}

# Main execution
main() {
    log "FocusHive JMeter Load Test Runner"
    log "=================================="
    
    check_jmeter
    
    case "${1:-all}" in
        smoke)
            check_services
            run_smoke_test
            ;;
        load)
            check_services
            run_load_test
            ;;
        stress)
            check_services
            run_stress_test
            ;;
        spike)
            check_services
            run_spike_test
            ;;
        all)
            check_services
            run_all_tests
            generate_consolidated_report
            ;;
        cleanup)
            cleanup
            ;;
        *)
            echo "Usage: $0 {smoke|load|stress|spike|all|cleanup}"
            echo ""
            echo "Test Types:"
            echo "  smoke  - Basic functionality test (1 user, 1 minute)"
            echo "  load   - Normal load test (10 users, 5 minutes)"
            echo "  stress - High load test (50 users, 10 minutes)"
            echo "  spike  - Spike test (100 users, 3 minutes)"
            echo "  all    - Run all tests sequentially"
            echo "  cleanup- Clean old test results"
            echo ""
            echo "Environment Variables:"
            echo "  JMETER_HOME - Path to JMeter installation"
            echo ""
            echo "Example:"
            echo "  $0 load"
            echo "  JMETER_HOME=/opt/jmeter $0 all"
            exit 1
            ;;
    esac
}

# Execute main function with all arguments
main "$@"