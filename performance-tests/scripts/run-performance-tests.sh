#!/bin/bash

# FocusHive Performance Testing Suite
# Comprehensive performance testing with automated reporting

set -e

# Configuration
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RESULTS_DIR="$BASE_DIR/results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
TEST_RESULTS_DIR="$RESULTS_DIR/$TIMESTAMP"

# Default test parameters
USERS=${USERS:-100}
RAMP_TIME=${RAMP_TIME:-300}
DURATION=${DURATION:-900}
BASE_URL=${BASE_URL:-"http://localhost:8080"}
WS_URL=${WS_URL:-"ws://localhost:8080/ws"}

# Create results directory
mkdir -p "$TEST_RESULTS_DIR"

echo "=== FocusHive Performance Testing Suite ==="
echo "Timestamp: $TIMESTAMP"
echo "Users: $USERS"
echo "Ramp Time: $RAMP_TIME seconds"
echo "Duration: $DURATION seconds"
echo "Base URL: $BASE_URL"
echo "WebSocket URL: $WS_URL"
echo "Results Directory: $TEST_RESULTS_DIR"
echo "========================================"

# Function to check if service is running
check_service() {
    local url=$1
    local service_name=$2
    
    echo "Checking $service_name at $url..."
    if curl -f -s "$url/actuator/health" > /dev/null 2>&1; then
        echo "✓ $service_name is running"
        return 0
    else
        echo "✗ $service_name is not running at $url"
        return 1
    fi
}

# Function to wait for service to be ready
wait_for_service() {
    local url=$1
    local service_name=$2
    local max_attempts=30
    local attempt=1
    
    echo "Waiting for $service_name to be ready..."
    
    while [ $attempt -le $max_attempts ]; do
        if check_service "$url" "$service_name"; then
            return 0
        fi
        
        echo "Attempt $attempt/$max_attempts - waiting 5 seconds..."
        sleep 5
        ((attempt++))
    done
    
    echo "✗ $service_name failed to become ready after $max_attempts attempts"
    return 1
}

# Function to run JMeter test
run_jmeter_test() {
    local test_name=$1
    local jmx_file=$2
    local additional_props=$3
    
    echo "Running $test_name..."
    
    local jmeter_props="-Jbase.url=$BASE_URL -Jws.url=$WS_URL -Jusers=$USERS -Jramp.time=$RAMP_TIME -Jduration=$DURATION"
    if [ -n "$additional_props" ]; then
        jmeter_props="$jmeter_props $additional_props"
    fi
    
    # Check if JMeter is available
    if ! command -v jmeter &> /dev/null; then
        echo "✗ JMeter not found. Please install Apache JMeter"
        return 1
    fi
    
    jmeter -n -t "$BASE_DIR/jmeter/$jmx_file" \
           $jmeter_props \
           -l "$TEST_RESULTS_DIR/${test_name}-results.jtl" \
           -j "$TEST_RESULTS_DIR/${test_name}-jmeter.log" \
           -e -o "$TEST_RESULTS_DIR/${test_name}-dashboard"
    
    if [ $? -eq 0 ]; then
        echo "✓ $test_name completed successfully"
        return 0
    else
        echo "✗ $test_name failed"
        return 1
    fi
}

# Function to run custom performance tests
run_custom_tests() {
    echo "Running custom performance validation tests..."
    
    # Database connection pool test
    echo "Testing database connection pool..."
    curl -s "$BASE_URL/actuator/metrics/hikari.connections.active" > "$TEST_RESULTS_DIR/db-connections-active.json" || true
    curl -s "$BASE_URL/actuator/metrics/hikari.connections.max" > "$TEST_RESULTS_DIR/db-connections-max.json" || true
    
    # Memory usage
    echo "Testing memory usage..."
    curl -s "$BASE_URL/actuator/metrics/jvm.memory.used" > "$TEST_RESULTS_DIR/memory-usage.json" || true
    
    # GC metrics
    echo "Testing garbage collection metrics..."
    curl -s "$BASE_URL/actuator/metrics/jvm.gc.pause" > "$TEST_RESULTS_DIR/gc-metrics.json" || true
    
    # HTTP metrics
    echo "Testing HTTP request metrics..."
    curl -s "$BASE_URL/actuator/metrics/http.server.requests" > "$TEST_RESULTS_DIR/http-metrics.json" || true
    
    # Cache metrics (if Redis is available)
    echo "Testing cache metrics..."
    curl -s "$BASE_URL/actuator/metrics/cache.gets" > "$TEST_RESULTS_DIR/cache-gets.json" || true
    curl -s "$BASE_URL/actuator/metrics/cache.puts" > "$TEST_RESULTS_DIR/cache-puts.json" || true
    
    echo "✓ Custom tests completed"
}

# Function to run WebSocket-specific tests
run_websocket_tests() {
    echo "Running WebSocket performance tests..."
    
    # Use Node.js script for WebSocket testing if available
    if command -v node &> /dev/null && [ -f "$BASE_DIR/scripts/websocket-load-test.js" ]; then
        node "$BASE_DIR/scripts/websocket-load-test.js" \
             --url "$WS_URL" \
             --clients 50 \
             --duration 300 \
             --output "$TEST_RESULTS_DIR/websocket-results.json"
    else
        echo "WebSocket load test script not found or Node.js not available"
    fi
}

# Function to generate performance report
generate_report() {
    echo "Generating performance report..."
    
    cat > "$TEST_RESULTS_DIR/performance-summary.md" << EOF
# FocusHive Performance Test Report

**Test Run:** $TIMESTAMP
**Configuration:**
- Users: $USERS
- Ramp Time: $RAMP_TIME seconds  
- Duration: $DURATION seconds
- Base URL: $BASE_URL
- WebSocket URL: $WS_URL

## Test Results

### JMeter Load Test Results
- Results: \`FocusHive-Load-Test-results.jtl\`
- Dashboard: \`FocusHive-Load-Test-dashboard/\`
- Logs: \`FocusHive-Load-Test-jmeter.log\`

### Performance Metrics
- Database Connections: \`db-connections-*.json\`
- Memory Usage: \`memory-usage.json\`
- GC Metrics: \`gc-metrics.json\`
- HTTP Metrics: \`http-metrics.json\`
- Cache Metrics: \`cache-*.json\`

### WebSocket Performance
- Results: \`websocket-results.json\`

## Performance Thresholds

### Response Time Targets
- API Responses: < 200ms (95th percentile)
- Database Queries: < 100ms (average)
- WebSocket Messages: < 50ms (95th percentile)
- Page Load: < 2s (complete)

### Throughput Targets
- API Requests: > 1000 RPS
- WebSocket Messages: > 5000 messages/second
- Concurrent WebSocket Connections: > 1000

### Resource Usage Targets
- CPU Usage: < 70% (average)
- Memory Usage: < 80% (of allocated heap)
- Database Connections: < 80% (of pool size)
- Cache Hit Rate: > 80%

## Analysis

Review the JMeter dashboard and individual metric files for detailed analysis.
Compare results against the performance thresholds above.

**Generated:** $(date)
EOF

    echo "✓ Performance report generated: $TEST_RESULTS_DIR/performance-summary.md"
}

# Function to cleanup and optimize results
cleanup_results() {
    echo "Optimizing test results..."
    
    # Compress large log files
    find "$TEST_RESULTS_DIR" -name "*.log" -size +10M -exec gzip {} \;
    
    # Create archive of complete results
    cd "$RESULTS_DIR"
    tar -czf "performance-test-$TIMESTAMP.tar.gz" "$TIMESTAMP/"
    
    echo "✓ Results archived: performance-test-$TIMESTAMP.tar.gz"
}

# Main execution flow
main() {
    echo "Starting FocusHive Performance Test Suite..."
    
    # Pre-test checks
    echo "Performing pre-test checks..."
    
    if ! wait_for_service "$BASE_URL" "FocusHive Backend"; then
        echo "Backend service is not available. Aborting tests."
        exit 1
    fi
    
    # Optional: Check Identity Service
    if [ -n "$IDENTITY_SERVICE_URL" ]; then
        wait_for_service "$IDENTITY_SERVICE_URL" "Identity Service" || echo "Warning: Identity Service not available"
    fi
    
    # Run performance tests
    echo "Starting performance tests..."
    
    # JMeter load tests
    if [ -f "$BASE_DIR/jmeter/FocusHive-Load-Test.jmx" ]; then
        run_jmeter_test "FocusHive-Load-Test" "FocusHive-Load-Test.jmx" || echo "JMeter test failed"
    else
        echo "JMeter test file not found"
    fi
    
    # Custom validation tests
    run_custom_tests
    
    # WebSocket tests
    run_websocket_tests
    
    # Generate reports
    generate_report
    
    # Cleanup
    cleanup_results
    
    echo "=== Performance Testing Complete ==="
    echo "Results available in: $TEST_RESULTS_DIR"
    echo "Summary report: $TEST_RESULTS_DIR/performance-summary.md"
    echo "Archive: $RESULTS_DIR/performance-test-$TIMESTAMP.tar.gz"
}

# Help function
show_help() {
    cat << EOF
Usage: $0 [OPTIONS]

Options:
    -u, --users USERS           Number of concurrent users (default: 100)
    -r, --ramp-time SECONDS     Ramp-up time in seconds (default: 300)
    -d, --duration SECONDS      Test duration in seconds (default: 900)
    -b, --base-url URL          Backend base URL (default: http://localhost:8080)
    -w, --ws-url URL            WebSocket URL (default: ws://localhost:8080/ws)
    -h, --help                  Show this help message

Environment Variables:
    USERS, RAMP_TIME, DURATION, BASE_URL, WS_URL can also be set as environment variables.

Examples:
    $0                                          # Run with defaults
    $0 -u 200 -d 1800                         # 200 users for 30 minutes
    $0 --base-url http://staging.focushive.com # Test against staging
EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -u|--users)
            USERS="$2"
            shift 2
            ;;
        -r|--ramp-time)
            RAMP_TIME="$2"
            shift 2
            ;;
        -d|--duration)
            DURATION="$2"
            shift 2
            ;;
        -b|--base-url)
            BASE_URL="$2"
            shift 2
            ;;
        -w|--ws-url)
            WS_URL="$2"
            shift 2
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Run main function
main