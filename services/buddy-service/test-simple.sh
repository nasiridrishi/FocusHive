#!/bin/bash

# Simple test script for Buddy Service endpoints

BASE_URL="http://localhost:8087"

echo "==========================================="
echo "Buddy Service Endpoint Tests"
echo "==========================================="

# Test Health Check
echo -n "1. Health check: "
status=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL/api/v1/health)
if [ "$status" = "200" ]; then
    echo "✅ PASSED (200)"
else
    echo "❌ FAILED ($status)"
fi

# Test Swagger UI
echo -n "2. Swagger UI: "
status=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL/swagger-ui/index.html)
if [ "$status" = "200" ]; then
    echo "✅ PASSED (200)"
else
    echo "❌ FAILED ($status)"
fi

# Test API Docs
echo -n "3. API Docs: "
status=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL/v3/api-docs)
if [ "$status" = "200" ]; then
    echo "✅ PASSED (200)"
else
    echo "❌ FAILED ($status)"
fi

# Test public endpoints (without auth)
echo -n "4. Unauthorized request (should fail): "
status=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL/api/v1/buddy/matching/suggestions)
if [ "$status" = "401" ] || [ "$status" = "403" ]; then
    echo "✅ PASSED (Auth required)"
else
    echo "❌ FAILED (Expected 401/403, got $status)"
fi

# Check database connectivity
echo -n "5. Database connectivity: "
db_result=$(docker exec focushive_buddy_service_postgres psql -U buddy_user -d buddy_service -c "SELECT 1" 2>&1 | grep -c "(1 row)")
if [ "$db_result" = "1" ]; then
    echo "✅ PASSED"
else
    echo "❌ FAILED"
fi

# Check Redis connectivity
echo -n "6. Redis connectivity: "
redis_result=$(docker exec focushive_buddy_service_redis redis-cli -a redis_password_secure_2024 ping 2>&1 | grep -c "PONG")
if [ "$redis_result" = "1" ]; then
    echo "✅ PASSED"
else
    echo "❌ FAILED"
fi

echo ""
echo "==========================================="
echo "Service URLs:"
echo "- API: $BASE_URL/api/v1/buddy"
echo "- Swagger UI: $BASE_URL/swagger-ui/index.html"
echo "- Health: $BASE_URL/api/v1/health"
echo "==========================================="
