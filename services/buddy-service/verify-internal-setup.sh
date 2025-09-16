#!/bin/bash

echo "================================"
echo "Verifying Internal-Only Setup"
echo "================================"
echo ""

PASSED=0
FAILED=0

# 1. Test external ports are blocked
echo -n "1. PostgreSQL blocked externally: "
if ! nc -zv localhost 5437 2>/dev/null; then
    echo "✅ PASSED (port not accessible)"
    PASSED=$((PASSED + 1))
else
    echo "❌ FAILED (port should not be accessible)"
    FAILED=$((FAILED + 1))
fi

echo -n "2. Redis blocked externally: "
if ! nc -zv localhost 6387 2>/dev/null; then
    echo "✅ PASSED (port not accessible)"
    PASSED=$((PASSED + 1))
else
    echo "❌ FAILED (port should not be accessible)"
    FAILED=$((FAILED + 1))
fi

# 2. Test app port is accessible
echo -n "3. App port accessible: "
if nc -zv localhost 8087 2>/dev/null; then
    echo "✅ PASSED"
    PASSED=$((PASSED + 1))
else
    echo "❌ FAILED"
    FAILED=$((FAILED + 1))
fi

# 3. Test internal connectivity
echo -n "4. Internal DB connectivity: "
if docker exec focushive_buddy_service_app nc -zv focushive_buddy_service_postgres 5432 2>/dev/null; then
    echo "✅ PASSED"
    PASSED=$((PASSED + 1))
else
    echo "❌ FAILED"
    FAILED=$((FAILED + 1))
fi

echo -n "5. Internal Redis connectivity: "
if docker exec focushive_buddy_service_app nc -zv focushive_buddy_service_redis 6379 2>/dev/null; then
    echo "✅ PASSED"
    PASSED=$((PASSED + 1))
else
    echo "❌ FAILED"
    FAILED=$((FAILED + 1))
fi

# 4. Test API functionality
echo -n "6. Health check API: "
status=$(curl -s http://localhost:8087/api/v1/health | jq -r '.status' 2>/dev/null)
if [ "$status" = "UP" ]; then
    echo "✅ PASSED"
    PASSED=$((PASSED + 1))
else
    echo "❌ FAILED"
    FAILED=$((FAILED + 1))
fi

echo -n "7. Database component health: "
db_status=$(curl -s http://localhost:8087/api/v1/health | jq -r '.components.database' 2>/dev/null)
if [ "$db_status" = "UP" ]; then
    echo "✅ PASSED"
    PASSED=$((PASSED + 1))
else
    echo "❌ FAILED"
    FAILED=$((FAILED + 1))
fi

echo -n "8. Swagger UI: "
swagger_code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8087/swagger-ui/index.html)
if [ "$swagger_code" = "200" ]; then
    echo "✅ PASSED"
    PASSED=$((PASSED + 1))
else
    echo "❌ FAILED"
    FAILED=$((FAILED + 1))
fi

echo ""
echo "================================"
echo "Test Summary"
echo "================================"
echo "Passed: $PASSED/8"
echo "Failed: $FAILED/8"

if [ $FAILED -eq 0 ]; then
    echo ""
    echo "✅ All tests passed! PostgreSQL and Redis are internal-only."
    echo "✅ App is fully functional with secure internal connectivity."
    exit 0
else
    echo ""
    echo "❌ Some tests failed. Please check the configuration."
    exit 1
fi
