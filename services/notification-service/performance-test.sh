#!/bin/bash

# Performance Test Script for Notification Service New Features
# Tests template caching and rate limiting functionality

echo "======================================="
echo "Performance Test for Notification Service"
echo "======================================="
echo ""

BASE_URL="http://localhost:8083"
ITERATIONS=100
CONCURRENT=10

# Function to test rate limiting
test_rate_limiting() {
    echo "Testing Rate Limiting..."
    echo "------------------------"

    # Test anonymous rate limit (20 requests per minute)
    echo "1. Testing anonymous user rate limit (20/min)..."

    START_TIME=$(date +%s)
    for i in $(seq 1 25); do
        RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/notifications")
        if [ $i -le 20 ]; then
            if [ "$RESPONSE" = "200" ] || [ "$RESPONSE" = "401" ]; then
                echo -n "✓"
            else
                echo -n "✗($RESPONSE)"
            fi
        else
            if [ "$RESPONSE" = "429" ]; then
                echo -n "✓"
            else
                echo -n "✗($RESPONSE)"
            fi
        fi
    done
    END_TIME=$(date +%s)
    echo ""
    echo "Completed in $((END_TIME - START_TIME)) seconds"
    echo ""
}

# Function to test template caching performance
test_template_caching() {
    echo "Testing Template Caching Performance..."
    echo "---------------------------------------"

    # First request (cache miss)
    echo "1. First request (cache miss):"
    START=$(date +%s%3N)
    curl -s -o /dev/null -w "   Status: %{http_code}, Time: %{time_total}s\n" \
         "$BASE_URL/api/notifications/templates"
    END=$(date +%s%3N)
    FIRST_TIME=$((END - START))

    # Second request (cache hit)
    echo "2. Second request (cache hit):"
    START=$(date +%s%3N)
    curl -s -o /dev/null -w "   Status: %{http_code}, Time: %{time_total}s\n" \
         "$BASE_URL/api/notifications/templates"
    END=$(date +%s%3N)
    SECOND_TIME=$((END - START))

    echo "3. Cache performance improvement: $(echo "scale=2; ($FIRST_TIME - $SECOND_TIME) * 100 / $FIRST_TIME" | bc)%"
    echo ""
}

# Function to test concurrent requests
test_concurrent_requests() {
    echo "Testing Concurrent Requests..."
    echo "------------------------------"
    echo "Sending $CONCURRENT concurrent requests..."

    START_TIME=$(date +%s)

    # Run concurrent requests
    for i in $(seq 1 $CONCURRENT); do
        {
            RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/notifications")
            echo "Request $i: $RESPONSE"
        } &
    done

    # Wait for all background jobs to complete
    wait

    END_TIME=$(date +%s)
    echo "Completed $CONCURRENT concurrent requests in $((END_TIME - START_TIME)) seconds"
    echo ""
}

# Function to test health endpoints
test_health_endpoints() {
    echo "Testing Health Endpoints..."
    echo "---------------------------"

    echo "1. Main health endpoint:"
    curl -s "$BASE_URL/health" | python3 -m json.tool | head -5
    echo ""

    echo "2. Actuator health endpoint:"
    curl -s "$BASE_URL/actuator/health" | python3 -m json.tool | grep -E '"status"|"components"' | head -10
    echo ""
}

# Function to measure response times
measure_response_times() {
    echo "Measuring Response Times ($ITERATIONS requests)..."
    echo "-------------------------------------------------"

    TOTAL_TIME=0
    MIN_TIME=999999
    MAX_TIME=0

    for i in $(seq 1 $ITERATIONS); do
        TIME=$(curl -s -o /dev/null -w "%{time_total}" "$BASE_URL/api/notifications")
        TIME_MS=$(echo "$TIME * 1000" | bc | cut -d. -f1)

        TOTAL_TIME=$(echo "$TOTAL_TIME + $TIME" | bc)

        # Update min/max
        if (( $(echo "$TIME < $MIN_TIME" | bc -l) )); then
            MIN_TIME=$TIME
        fi
        if (( $(echo "$TIME > $MAX_TIME" | bc -l) )); then
            MAX_TIME=$TIME
        fi

        # Progress indicator
        if [ $((i % 10)) -eq 0 ]; then
            echo -n "."
        fi
    done
    echo ""

    AVG_TIME=$(echo "scale=3; $TOTAL_TIME / $ITERATIONS" | bc)

    echo "Results:"
    echo "  Average: ${AVG_TIME}s"
    echo "  Min: ${MIN_TIME}s"
    echo "  Max: ${MAX_TIME}s"
    echo ""
}

# Main execution
echo "Starting performance tests..."
echo ""

# Check if service is running
if ! curl -s -f "$BASE_URL/actuator/health" > /dev/null 2>&1; then
    echo "ERROR: Service is not running at $BASE_URL"
    echo "Please start the service first with: ./gradlew bootRun"
    exit 1
fi

# Run tests
test_health_endpoints
test_rate_limiting
test_template_caching
test_concurrent_requests
measure_response_times

echo "======================================="
echo "Performance Test Complete!"
echo "======================================="