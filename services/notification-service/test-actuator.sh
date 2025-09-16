#!/bin/bash

# Test script for actuator endpoints

echo "Testing Actuator Endpoints"
echo "=========================="

BASE_URL="http://localhost:8083"

test_endpoint() {
    local endpoint=$1
    local description=$2

    echo -n "Testing $endpoint - $description: "

    response=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$endpoint")

    if [ "$response" = "200" ] || [ "$response" = "401" ]; then
        echo "✓ ($response)"
    else
        echo "✗ ($response)"
    fi
}

# Test various actuator endpoints
test_endpoint "/actuator/health" "Health check"
test_endpoint "/actuator/info" "Application info"
test_endpoint "/actuator/metrics" "Metrics"
test_endpoint "/actuator/prometheus" "Prometheus metrics"
test_endpoint "/actuator/configprops" "Configuration properties"
test_endpoint "/actuator/env" "Environment"
test_endpoint "/actuator/beans" "Bean information"
test_endpoint "/actuator/loggers" "Loggers"

echo ""
echo "Note: 401 responses are expected for secured endpoints without authentication."
echo "The key is that we're not getting 404 or NoResourceFoundException anymore."