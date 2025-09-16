#!/bin/bash

# Health Monitoring Script for FocusHive Notification Service
# This script monitors the health of all notification service components

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Service URLs
NOTIFICATION_SERVICE_URL="http://localhost:8083"
POSTGRES_HOST="localhost"
POSTGRES_PORT="5433"
REDIS_HOST="localhost"
REDIS_PORT="6380"
RABBITMQ_HOST="localhost"
RABBITMQ_PORT="15673"
RABBITMQ_USER="admin"
RABBITMQ_PASS="${RABBITMQ_PASSWORD:-rabbitmq_secure_password_2024}"

echo "=========================================="
echo "FocusHive Notification Service Health Check"
echo "=========================================="
echo ""

# Function to check service status
check_service() {
    local service_name=$1
    local check_command=$2

    if eval "$check_command" > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} $service_name: ${GREEN}HEALTHY${NC}"
        return 0
    else
        echo -e "${RED}✗${NC} $service_name: ${RED}UNHEALTHY${NC}"
        return 1
    fi
}

# Function to check HTTP endpoint
check_http_endpoint() {
    local endpoint_name=$1
    local url=$2
    local expected_code=${3:-200}

    response_code=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")

    if [ "$response_code" = "$expected_code" ]; then
        echo -e "${GREEN}✓${NC} $endpoint_name: ${GREEN}OK${NC} (HTTP $response_code)"
        return 0
    else
        echo -e "${RED}✗${NC} $endpoint_name: ${RED}FAILED${NC} (HTTP $response_code)"
        return 1
    fi
}

# Check Docker containers
echo "1. Checking Docker Containers:"
echo "------------------------------"

containers=("focushive-notification-service-app" "focushive-notification-service-postgres" "focushive-notification-service-redis" "focushive-notification-service-rabbitmq")
for container in "${containers[@]}"; do
    status=$(docker ps --filter "name=$container" --format "{{.Status}}" 2>/dev/null || echo "not found")
    if [[ $status == *"Up"* ]] && [[ $status == *"healthy"* ]]; then
        echo -e "${GREEN}✓${NC} $container: ${GREEN}Running (Healthy)${NC}"
    elif [[ $status == *"Up"* ]]; then
        echo -e "${YELLOW}⚠${NC} $container: ${YELLOW}Running (Health Unknown)${NC}"
    else
        echo -e "${RED}✗${NC} $container: ${RED}Not Running${NC}"
    fi
done

echo ""

# Check service endpoints
echo "2. Checking Service Endpoints:"
echo "-------------------------------"

check_http_endpoint "Health Endpoint" "${NOTIFICATION_SERVICE_URL}/actuator/health"
check_http_endpoint "Metrics Endpoint" "${NOTIFICATION_SERVICE_URL}/actuator/prometheus"
check_http_endpoint "Info Endpoint" "${NOTIFICATION_SERVICE_URL}/actuator/info"
check_http_endpoint "Test Ping" "${NOTIFICATION_SERVICE_URL}/api/test/ping"

echo ""

# Check external services connectivity
echo "3. Checking External Services:"
echo "-------------------------------"

# PostgreSQL
check_service "PostgreSQL" "nc -zv $POSTGRES_HOST $POSTGRES_PORT 2>/dev/null"

# Redis
check_service "Redis" "nc -zv $REDIS_HOST $REDIS_PORT 2>/dev/null"

# RabbitMQ
check_service "RabbitMQ AMQP" "nc -zv $RABBITMQ_HOST 5673 2>/dev/null"
check_service "RabbitMQ Management" "nc -zv $RABBITMQ_HOST $RABBITMQ_PORT 2>/dev/null"

echo ""

# Get detailed health status
echo "4. Detailed Health Status:"
echo "--------------------------"

health_json=$(curl -s "${NOTIFICATION_SERVICE_URL}/actuator/health" 2>/dev/null)

if [ ! -z "$health_json" ]; then
    # Parse health status
    overall_status=$(echo "$health_json" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)

    if [ "$overall_status" = "UP" ]; then
        echo -e "Overall Status: ${GREEN}UP${NC}"
    else
        echo -e "Overall Status: ${RED}$overall_status${NC}"
    fi

    # Check specific components
    echo ""
    echo "Component Status:"

    # Database
    db_status=$(echo "$health_json" | grep -o '"db":{[^}]*"status":"[^"]*"' | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    if [ "$db_status" = "UP" ]; then
        echo -e "  - Database: ${GREEN}UP${NC}"
    else
        echo -e "  - Database: ${RED}${db_status:-UNKNOWN}${NC}"
    fi

    # Redis
    redis_status=$(echo "$health_json" | grep -o '"redis":{[^}]*"status":"[^"]*"' | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    if [ "$redis_status" = "UP" ]; then
        echo -e "  - Redis: ${GREEN}UP${NC}"
    else
        echo -e "  - Redis: ${RED}${redis_status:-UNKNOWN}${NC}"
    fi

    # RabbitMQ
    rabbit_status=$(echo "$health_json" | grep -o '"rabbit":{[^}]*"status":"[^"]*"' | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    if [ "$rabbit_status" = "UP" ]; then
        echo -e "  - RabbitMQ: ${GREEN}UP${NC}"
    else
        echo -e "  - RabbitMQ: ${RED}${rabbit_status:-UNKNOWN}${NC}"
    fi

    # Circuit Breakers
    circuit_status=$(echo "$health_json" | grep -o '"circuitBreaker":{[^}]*"status":"[^"]*"' | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    if [ "$circuit_status" = "UP" ]; then
        echo -e "  - Circuit Breakers: ${GREEN}UP${NC}"
    else
        echo -e "  - Circuit Breakers: ${RED}${circuit_status:-UNKNOWN}${NC}"
    fi

    # Disk Space
    disk_status=$(echo "$health_json" | grep -o '"diskSpace":{[^}]*"status":"[^"]*"' | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    if [ "$disk_status" = "UP" ]; then
        echo -e "  - Disk Space: ${GREEN}UP${NC}"
    else
        echo -e "  - Disk Space: ${RED}${disk_status:-UNKNOWN}${NC}"
    fi
else
    echo -e "${RED}Failed to retrieve health information${NC}"
fi

echo ""

# Check recent logs for errors
echo "5. Recent Error Check:"
echo "----------------------"

error_count=$(docker logs focushive-notification-service-app 2>&1 --since=10m | grep -E "ERROR|Exception" | wc -l | tr -d ' ')

if [ "$error_count" = "0" ]; then
    echo -e "${GREEN}✓${NC} No errors in last 10 minutes"
else
    echo -e "${YELLOW}⚠${NC} Found $error_count error(s) in last 10 minutes"
    echo "Recent errors:"
    docker logs focushive-notification-service-app 2>&1 --since=10m | grep -E "ERROR|Exception" | tail -3
fi

echo ""

# Performance metrics
echo "6. Performance Metrics:"
echo "-----------------------"

# Get metrics from Prometheus endpoint
metrics=$(curl -s "${NOTIFICATION_SERVICE_URL}/actuator/prometheus" 2>/dev/null)

if [ ! -z "$metrics" ]; then
    # Extract key metrics
    notifications_sent=$(echo "$metrics" | grep "notifications_sent_total" | grep -v "#" | awk '{print $2}' | head -1)
    notifications_failed=$(echo "$metrics" | grep "notifications_failed_total" | grep -v "#" | awk '{print $2}' | head -1)

    echo "Notifications Sent: ${notifications_sent:-0}"
    echo "Notifications Failed: ${notifications_failed:-0}"

    # Calculate success rate if there are notifications
    if [ "${notifications_sent:-0}" != "0" ] || [ "${notifications_failed:-0}" != "0" ]; then
        total=$(echo "${notifications_sent:-0} + ${notifications_failed:-0}" | bc)
        if [ "$total" != "0" ]; then
            success_rate=$(echo "scale=2; ${notifications_sent:-0} * 100 / $total" | bc)
            echo "Success Rate: ${success_rate}%"
        fi
    fi
else
    echo -e "${YELLOW}Unable to retrieve metrics${NC}"
fi

echo ""
echo "=========================================="
echo "Health Check Complete"
echo "=========================================="

# Exit with appropriate code
if [ "$overall_status" = "UP" ]; then
    exit 0
else
    exit 1
fi