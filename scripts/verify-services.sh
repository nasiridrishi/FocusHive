#!/bin/bash

# Service verification script for FocusHive microservices
# Tests health endpoints for all 8 services

echo "=== FocusHive Backend Services Health Check ==="
echo "Testing all 8 services on their designated ports..."
echo

# Define services and their ports (compatible with older bash)
services="focushive-backend:8080 identity-service:8081 music-service:8082 notification-service:8083 chat-service:8084 analytics-service:8085 forum-service:8086 buddy-service:8087"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Track results
total_services=0
running_services=0

# Function to test health endpoint
test_service() {
    local service=$1
    local port=$2
    local url="http://localhost:$port/api/v1/health"
    
    echo -n "Testing $service (port $port)... "
    
    # Test with timeout
    response=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 3 --max-time 5 "$url" 2>/dev/null)
    
    if [ "$response" = "200" ]; then
        echo -e "${GREEN}✅ HEALTHY${NC}"
        results[$service]="HEALTHY"
        ((running_services++))
    elif [ "$response" = "401" ] || [ "$response" = "403" ]; then
        echo -e "${YELLOW}⚠️  SECURITY ERROR (HTTP $response)${NC}"
        results[$service]="SECURITY_ERROR"
    elif [ -n "$response" ]; then
        echo -e "${RED}❌ ERROR (HTTP $response)${NC}"
        results[$service]="ERROR_$response"
    else
        echo -e "${RED}❌ NOT RESPONDING${NC}"
        results[$service]="NOT_RESPONDING"
    fi
    
    ((total_services++))
}

# Test all services
for service_port in $services; do
    service_name=$(echo "$service_port" | cut -d: -f1)
    port=$(echo "$service_port" | cut -d: -f2)
    test_service "$service_name" "$port"
done

echo
echo "=== Summary ==="
echo "Services running: $running_services/$total_services"
echo

echo
if [ $running_services -eq $total_services ]; then
    echo -e "${GREEN}🎉 All services are healthy!${NC}"
    exit 0
elif [ $running_services -gt 0 ]; then
    echo -e "${YELLOW}⚠️  Some services need attention${NC}"
    exit 1
else
    echo -e "${RED}❌ No services are responding${NC}"
    exit 2
fi