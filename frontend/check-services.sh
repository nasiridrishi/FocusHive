#!/bin/bash

# FocusHive Services Health Check Script
# Use this before running E2E tests to ensure all services are available

echo "🔍 Checking FocusHive Services Status..."
echo "=========================================="

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Track overall status
ALL_HEALTHY=true

# Function to check service health
check_service() {
    local name=$1
    local url=$2
    local port=$3

    printf "%-25s" "$name (port $port):"

    response=$(curl -s -o /dev/null -w "%{http_code}" $url 2>/dev/null)

    if [ "$response" = "200" ]; then
        echo -e " ${GREEN}✅ HEALTHY${NC}"
    else
        echo -e " ${RED}❌ UNHEALTHY${NC} (HTTP $response)"
        ALL_HEALTHY=false
    fi
}

# Check each service
check_service "FocusHive Backend" "http://localhost:8080/actuator/health" 8080
check_service "Identity Service" "http://localhost:8081/actuator/health" 8081
check_service "Notification Service" "http://localhost:8083/actuator/health" 8083
check_service "Buddy Service" "http://localhost:8087/api/v1/health" 8087

echo ""
echo "🐳 Docker Container Status:"
echo "---------------------------"

# Check Docker containers
CONTAINERS=$(docker ps --format "table {{.Names}}\t{{.Status}}" | grep focushive)

if [ -z "$CONTAINERS" ]; then
    echo -e "${RED}No FocusHive containers are running!${NC}"
    echo "Run: cd ../services/focushive-backend && docker-compose up -d"
    ALL_HEALTHY=false
else
    echo "$CONTAINERS"
fi

echo ""
echo "🔌 WebSocket Endpoint Check:"
echo "-----------------------------"

# Check WebSocket endpoint (should return 400 or be accessible)
ws_response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/ws 2>/dev/null)
# WebSocket endpoints typically return 400 or 404 for regular HTTP requests
if [ "$ws_response" = "400" ] || [ "$ws_response" = "404" ] || [ "$ws_response" = "200" ]; then
    echo -e "WebSocket endpoint:       ${GREEN}✅ AVAILABLE${NC} (ws://localhost:8080/ws)"
else
    echo -e "WebSocket endpoint:       ${RED}❌ UNAVAILABLE${NC} (HTTP $ws_response)"
    ALL_HEALTHY=false
fi

echo ""
echo "=========================================="

if [ "$ALL_HEALTHY" = true ]; then
    echo -e "${GREEN}✅ All services are healthy! Ready for E2E testing.${NC}"
    exit 0
else
    echo -e "${RED}❌ Some services are not healthy. Please fix before running E2E tests.${NC}"
    echo ""
    echo "To start services:"
    echo "  cd ../services/focushive-backend"
    echo "  docker-compose up -d"
    echo ""
    echo "To check logs:"
    echo "  docker logs focushive_backend_main"
    echo "  docker logs focushive-identity-service-app"
    exit 1
fi