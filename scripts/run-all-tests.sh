#!/bin/bash

# FocusHive - Test All Services
# This script runs tests for all 8 backend services to verify TDD setup

set -e  # Exit on any error

echo "🧪 Running comprehensive test suite for all FocusHive services..."
echo "=================================================================="

# Services configuration - simple arrays for compatibility
SERVICES=(
    "Identity Service:identity-service:8081"
    "FocusHive Backend:focushive-backend:8080"
    "Music Service:music-service:8082" 
    "Notification Service:notification-service:8083"
    "Chat Service:chat-service:8084"
    "Analytics Service:analytics-service:8085"
    "Forum Service:forum-service:8086"
    "Buddy Service:buddy-service:8087"
)

PROJECT_ROOT="/Users/nasir/uol/focushive"
SUCCESS_COUNT=0
TOTAL_SERVICES=8

# Function to run tests for a service
run_service_tests() {
    local service_name="$1"
    local service_dir="$2"
    local port="$3"
    
    echo "📋 Testing: $service_name (Port: $port)"
    echo "   Directory: $service_dir"
    
    cd "$PROJECT_ROOT/services/$service_dir"
    
    if ./gradlew test --no-daemon --quiet; then
        echo "✅ $service_name - Tests PASSED"
        ((SUCCESS_COUNT++))
    else
        echo "❌ $service_name - Tests FAILED"
        echo "   Check: $PROJECT_ROOT/services/$service_dir/build/reports/tests/test/index.html"
    fi
    
    echo ""
}

# Run tests for all services
for service_info in "${SERVICES[@]}"; do
    # Parse service_info: "Name:directory:port"
    IFS=':' read -r service_name service_dir port <<< "$service_info"
    
    run_service_tests "$service_name" "$service_dir" "$port"
done

# Summary
echo "=================================================================="
echo "📊 TEST SUMMARY:"
echo "   Services Tested: $TOTAL_SERVICES"
echo "   Tests Passed: $SUCCESS_COUNT"
echo "   Tests Failed: $((TOTAL_SERVICES - SUCCESS_COUNT))"

if [ $SUCCESS_COUNT -eq $TOTAL_SERVICES ]; then
    echo "🎉 ALL SERVICES HAVE PASSING TESTS - TDD SETUP COMPLETE!"
    echo "   Ready to start all services with start-all-services.sh"
else
    echo "⚠️  Some services have failing tests. Review and fix before proceeding."
    exit 1
fi