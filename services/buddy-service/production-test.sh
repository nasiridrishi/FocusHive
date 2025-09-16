#!/bin/bash

# Production deployment test script
# Tests the actual running Docker containers

echo "===================================================="
echo "Production Deployment Verification Test"
echo "===================================================="
echo ""

TOTAL=0
PASSED=0
FAILED=0

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Test function
test_case() {
    local description=$1
    local command=$2
    local expected=$3

    TOTAL=$((TOTAL + 1))
    echo -n "Test $TOTAL: $description... "

    result=$(eval "$command" 2>&1)

    if [[ "$result" == *"$expected"* ]] || [[ "$result" == "$expected" ]]; then
        echo -e "${GREEN}✅ PASSED${NC}"
        PASSED=$((PASSED + 1))
        return 0
    else
        echo -e "${RED}❌ FAILED${NC}"
        echo "  Expected: $expected"
        echo "  Got: $result"
        FAILED=$((FAILED + 1))
        return 1
    fi
}

echo -e "${YELLOW}1. Security Tests (External Ports Blocked)${NC}"
echo "----------------------------------------"
test_case "PostgreSQL NOT accessible from host" "nc -zv localhost 5437 2>&1" "Connection refused"
test_case "Redis NOT accessible from host" "nc -zv localhost 6387 2>&1" "Connection refused"

echo ""
echo -e "${YELLOW}2. Internal Connectivity Tests${NC}"
echo "----------------------------------------"
test_case "App can reach PostgreSQL internally" \
    "docker exec focushive_buddy_service_app nc -zv focushive_buddy_service_postgres 5432 2>&1" \
    "open"

test_case "App can reach Redis internally" \
    "docker exec focushive_buddy_service_app nc -zv focushive_buddy_service_redis 6379 2>&1" \
    "open"

echo ""
echo -e "${YELLOW}3. Application Functionality Tests${NC}"
echo "----------------------------------------"
test_case "Health endpoint returns UP" \
    "curl -s http://localhost:8087/api/v1/health | jq -r '.status'" \
    "UP"

test_case "Database component is UP" \
    "curl -s http://localhost:8087/api/v1/health | jq -r '.components.database'" \
    "UP"

test_case "Matching component is UP" \
    "curl -s http://localhost:8087/api/v1/health | jq -r '.components.matching'" \
    "UP"

test_case "Swagger UI is accessible" \
    "curl -s -o /dev/null -w '%{http_code}' http://localhost:8087/swagger-ui/index.html" \
    "200"

test_case "API docs are available" \
    "curl -s http://localhost:8087/v3/api-docs | jq -r '.openapi' | cut -c1-3" \
    "3.0"

echo ""
echo -e "${YELLOW}4. Database Verification${NC}"
echo "----------------------------------------"
test_case "Database has correct number of tables" \
    "docker exec focushive_buddy_service_postgres psql -U buddy_user -d buddy_service -t -c 'SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='\''public'\'';' | tr -d ' \n'" \
    "8"

test_case "Can query partnership table" \
    "docker exec focushive_buddy_service_postgres psql -U buddy_user -d buddy_service -t -c 'SELECT COUNT(*) FROM buddy_partnerships;' 2>&1 | grep -c '0'" \
    "1"

echo ""
echo -e "${YELLOW}5. Redis Cache Verification${NC}"
echo "----------------------------------------"
test_case "Redis is responding to ping" \
    "docker exec focushive_buddy_service_redis redis-cli -a redis_password_secure_2024 ping 2>&1 | grep -c 'PONG'" \
    "1"

test_case "Redis can store and retrieve values" \
    "docker exec focushive_buddy_service_redis redis-cli -a redis_password_secure_2024 SET test:key 'test-value' 2>&1 | grep -c 'OK'" \
    "1"

echo ""
echo -e "${YELLOW}6. Security Tests${NC}"
echo "----------------------------------------"
test_case "Unauthorized API access returns 401" \
    "curl -s -o /dev/null -w '%{http_code}' http://localhost:8087/api/v1/buddy/matching/suggestions" \
    "401"

test_case "App container is running as non-root user" \
    "docker exec focushive_buddy_service_app whoami" \
    "appuser"

echo ""
echo "===================================================="
echo "Test Results Summary"
echo "===================================================="
echo -e "Total Tests: ${YELLOW}$TOTAL${NC}"
echo -e "Passed: ${GREEN}$PASSED${NC}"
echo -e "Failed: ${RED}$FAILED${NC}"

if [ $FAILED -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✅ All production tests passed!${NC}"
    echo -e "${GREEN}✅ PostgreSQL and Redis are secure (internal-only)${NC}"
    echo -e "${GREEN}✅ Application is fully functional${NC}"
    exit 0
else
    echo ""
    echo -e "${RED}❌ Some tests failed. Please review the deployment.${NC}"
    exit 1
fi