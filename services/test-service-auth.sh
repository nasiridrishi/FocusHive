#!/bin/bash

# Service-to-Service Authentication Test Script
# Tests JWT authentication between all FocusHive microservices

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Service URLs (adjust for Docker or local environment)
IDENTITY_SERVICE="${IDENTITY_SERVICE_URL:-http://localhost:8081}"
NOTIFICATION_SERVICE="${NOTIFICATION_SERVICE_URL:-http://localhost:8083}"
BUDDY_SERVICE="${BUDDY_SERVICE_URL:-http://localhost:8087}"
BACKEND_SERVICE="${BACKEND_SERVICE_URL:-http://localhost:8080}"

echo "================================================"
echo "FocusHive Service-to-Service Authentication Test"
echo "================================================"
echo

# Function to print test results
print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✅ $2${NC}"
    else
        echo -e "${RED}❌ $2${NC}"
    fi
}

# Function to check if service is healthy
check_service_health() {
    local service_name=$1
    local service_url=$2

    echo "Checking $service_name health..."

    response=$(curl -s -o /dev/null -w "%{http_code}" "$service_url/actuator/health" 2>/dev/null || echo "000")

    if [ "$response" = "200" ] || [ "$response" = "503" ]; then
        print_result 0 "$service_name is reachable (HTTP $response)"
        return 0
    else
        print_result 1 "$service_name is not reachable (HTTP $response)"
        return 1
    fi
}

# Function to test JWKS endpoint
test_jwks_endpoint() {
    echo
    echo "Testing JWKS Endpoint..."

    response=$(curl -s -w "\n%{http_code}" "$IDENTITY_SERVICE/.well-known/jwks.json" 2>/dev/null)
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)

    if [ "$http_code" = "200" ]; then
        # Check if keys array exists
        if echo "$body" | grep -q '"keys"'; then
            key_count=$(echo "$body" | grep -o '"kid"' | wc -l)
            if [ "$key_count" -gt 0 ]; then
                print_result 0 "JWKS endpoint returned $key_count public key(s)"
            else
                print_result 1 "JWKS endpoint returned empty key set (RSA may not be configured)"
                echo -e "${YELLOW}⚠️  Identity service may be using HMAC instead of RSA${NC}"
            fi
        else
            print_result 1 "JWKS endpoint returned invalid response"
        fi
    else
        print_result 1 "JWKS endpoint returned HTTP $http_code"
    fi
}

# Function to test OpenID Discovery
test_openid_discovery() {
    echo
    echo "Testing OpenID Discovery..."

    response=$(curl -s -w "\n%{http_code}" "$IDENTITY_SERVICE/.well-known/openid_configuration" 2>/dev/null)
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)

    if [ "$http_code" = "200" ]; then
        if echo "$body" | grep -q '"issuer"'; then
            issuer=$(echo "$body" | grep -o '"issuer"[[:space:]]*:[[:space:]]*"[^"]*"' | sed 's/.*: *"\(.*\)"/\1/')
            print_result 0 "OpenID Discovery endpoint working (issuer: $issuer)"
        else
            print_result 1 "OpenID Discovery returned invalid response"
        fi
    else
        print_result 1 "OpenID Discovery returned HTTP $http_code"
    fi
}

# Function to create a test user and get JWT token
get_user_jwt_token() {
    echo
    echo "Creating test user and obtaining JWT token..."

    # Register a test user
    register_response=$(curl -s -X POST "$IDENTITY_SERVICE/api/v1/auth/register" \
        -H "Content-Type: application/json" \
        -d '{
            "username": "test-user-'$(date +%s)'",
            "email": "test'$(date +%s)'@example.com",
            "password": "TestPassword123!"
        }' 2>/dev/null || echo '{"error": "registration failed"}')

    if echo "$register_response" | grep -q "token"; then
        token=$(echo "$register_response" | grep -o '"token"[[:space:]]*:[[:space:]]*"[^"]*"' | sed 's/.*: *"\(.*\)"/\1/')
        if [ -n "$token" ]; then
            print_result 0 "User JWT token obtained"
            echo "$token"
            return 0
        fi
    fi

    # If registration fails, try login with existing test user
    login_response=$(curl -s -X POST "$IDENTITY_SERVICE/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{
            "username": "test-user",
            "password": "TestPassword123!"
        }' 2>/dev/null || echo '{"error": "login failed"}')

    if echo "$login_response" | grep -q "token"; then
        token=$(echo "$login_response" | grep -o '"token"[[:space:]]*:[[:space:]]*"[^"]*"' | sed 's/.*: *"\(.*\)"/\1/')
        if [ -n "$token" ]; then
            print_result 0 "User JWT token obtained (existing user)"
            echo "$token"
            return 0
        fi
    fi

    print_result 1 "Failed to obtain user JWT token"
    return 1
}

# Function to test service-to-service authentication
test_service_auth() {
    local from_service=$1
    local to_service=$2
    local to_url=$3
    local endpoint=$4

    echo
    echo "Testing $from_service → $to_service authentication..."

    # Get a user token first
    user_token=$(get_user_jwt_token)

    if [ -z "$user_token" ]; then
        print_result 1 "Cannot test without valid JWT token"
        return 1
    fi

    # Make request with service headers
    response=$(curl -s -w "\n%{http_code}" -X GET "$to_url$endpoint" \
        -H "Authorization: Bearer $user_token" \
        -H "X-Service-Name: $from_service" \
        -H "X-Correlation-ID: $(uuidgen || echo 'test-correlation-id')" \
        2>/dev/null)

    http_code=$(echo "$response" | tail -n1)

    case "$http_code" in
        200|201|204)
            print_result 0 "$from_service → $to_service: Success (HTTP $http_code)"
            ;;
        401)
            print_result 1 "$from_service → $to_service: Unauthorized (HTTP 401)"
            echo -e "${YELLOW}    Check JWT validation configuration in $to_service${NC}"
            ;;
        403)
            print_result 1 "$from_service → $to_service: Forbidden (HTTP 403)"
            echo -e "${YELLOW}    Token valid but insufficient permissions${NC}"
            ;;
        404)
            print_result 0 "$from_service → $to_service: Endpoint not found but auth likely working (HTTP 404)"
            ;;
        500|502|503)
            print_result 1 "$from_service → $to_service: Server error (HTTP $http_code)"
            ;;
        000)
            print_result 1 "$from_service → $to_service: Connection failed"
            ;;
        *)
            print_result 1 "$from_service → $to_service: Unexpected response (HTTP $http_code)"
            ;;
    esac
}

# Main test execution
echo "Step 1: Checking Service Health"
echo "================================"

all_healthy=true
check_service_health "Identity Service" "$IDENTITY_SERVICE" || all_healthy=false
check_service_health "Notification Service" "$NOTIFICATION_SERVICE" || all_healthy=false
check_service_health "Buddy Service" "$BUDDY_SERVICE" || all_healthy=false
check_service_health "Backend Service" "$BACKEND_SERVICE" || all_healthy=false

if [ "$all_healthy" = false ]; then
    echo
    echo -e "${YELLOW}⚠️  Warning: Some services are not healthy. Tests may fail.${NC}"
fi

echo
echo "Step 2: Testing Identity Service Endpoints"
echo "==========================================="

test_openid_discovery
test_jwks_endpoint

echo
echo "Step 3: Testing Service-to-Service Authentication"
echo "=================================================="

# Test all service pairs
test_service_auth "identity-service" "notification-service" "$NOTIFICATION_SERVICE" "/api/v1/notifications"
test_service_auth "identity-service" "buddy-service" "$BUDDY_SERVICE" "/api/v1/partnerships"
test_service_auth "identity-service" "backend" "$BACKEND_SERVICE" "/api/v1/hives"

test_service_auth "backend" "notification-service" "$NOTIFICATION_SERVICE" "/api/v1/notifications"
test_service_auth "backend" "buddy-service" "$BUDDY_SERVICE" "/api/v1/partnerships"
test_service_auth "backend" "identity-service" "$IDENTITY_SERVICE" "/api/v1/users/me"

test_service_auth "notification-service" "identity-service" "$IDENTITY_SERVICE" "/api/v1/users/me"
test_service_auth "buddy-service" "identity-service" "$IDENTITY_SERVICE" "/api/v1/users/me"

echo
echo "================================================"
echo "Authentication Test Summary"
echo "================================================"
echo
echo "Key Findings:"
echo "-------------"

# Check if RSA is configured
if [ "$key_count" -eq 0 ]; then
    echo -e "${YELLOW}• Identity Service may be using HMAC instead of RSA${NC}"
    echo "  To enable RSA, ensure jwt.use-rsa=true in identity service config"
fi

echo
echo "Recommendations:"
echo "----------------"
echo "1. Ensure all services have JWT_ISSUER_URI set to http://identity-service:8081"
echo "2. Verify RSA keys exist in identity-service/src/main/resources/keys/"
echo "3. Check that jwt.use-rsa=true in identity service configuration"
echo "4. Review service logs for detailed authentication errors"
echo "5. Ensure Docker services use service names, not localhost"

echo
echo "Test completed!"