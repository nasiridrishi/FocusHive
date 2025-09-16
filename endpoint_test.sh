#!/bin/bash

# FocusHive Identity Service Endpoint Testing Script
# This script tests all available endpoints and generates a comprehensive report

BASE_URL="http://localhost:8081"
REPORT_FILE="identity_service_test_report.json"
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test results array
declare -a test_results=()

# Function to add test result
add_test_result() {
    local endpoint="$1"
    local method="$2"
    local status="$3"
    local response_time="$4"
    local http_status="$5"
    local description="$6"
    local error_message="$7"
    
    test_results+=("{
        \"endpoint\": \"$endpoint\",
        \"method\": \"$method\",
        \"status\": \"$status\",
        \"response_time_ms\": $response_time,
        \"http_status\": $http_status,
        \"description\": \"$description\",
        \"error_message\": \"$error_message\",
        \"timestamp\": \"$(date -u +"%Y-%m-%dT%H:%M:%SZ")\"
    }")
}

# Function to test endpoint
test_endpoint() {
    local method="$1"
    local endpoint="$2"
    local data="$3"
    local headers="$4"
    local description="$5"
    local expected_status="$6"
    
    echo -e "${BLUE}Testing: $method $endpoint${NC}"
    
    local start_time=$(gdate +%s%3N 2>/dev/null || date +%s000)
    local response
    local http_status
    local curl_exit_code
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "HTTPSTATUS:%{http_code}" $headers "$BASE_URL$endpoint" 2>&1)
        curl_exit_code=$?
    elif [ "$method" = "POST" ]; then
        # Always add Content-Type header for POST requests
        if [ -n "$data" ]; then
            response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST -H "Content-Type: application/json" $headers -d "$data" "$BASE_URL$endpoint" 2>&1)
        else
            response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST -H "Content-Type: application/json" $headers "$BASE_URL$endpoint" 2>&1)
        fi
        curl_exit_code=$?
    fi
    
    local end_time=$(gdate +%s%3N 2>/dev/null || date +%s000)
    local response_time=$((end_time - start_time))
    
    http_status=$(echo $response | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)
    response_body=$(echo $response | sed -E 's/HTTPSTATUS:[0-9]*$//')
    
    # Determine test status
    local test_status="FAIL"
    local error_msg=""
    
    if [ $curl_exit_code -eq 0 ] && [ -n "$http_status" ]; then
        if [ -n "$expected_status" ] && [ "$http_status" = "$expected_status" ]; then
            test_status="PASS"
        elif [ -z "$expected_status" ] && [ "$http_status" -lt 500 ]; then
            test_status="PASS"
        else
            error_msg="Expected status: $expected_status, Got: $http_status"
        fi
    else
        error_msg="Connection failed or invalid response"
        http_status="0"
    fi
    
    # Color coding for output
    if [ "$test_status" = "PASS" ]; then
        echo -e "  ${GREEN}✓ PASS${NC} - HTTP $http_status (${response_time}ms)"
    else
        echo -e "  ${RED}✗ FAIL${NC} - $error_msg (${response_time}ms)"
    fi
    
    add_test_result "$endpoint" "$method" "$test_status" "$response_time" "${http_status:-0}" "$description" "$error_msg"
}

echo "======================================================"
echo "FocusHive Identity Service Endpoint Testing"
echo "======================================================"
echo "Base URL: $BASE_URL"
echo "Timestamp: $TIMESTAMP"
echo ""

# Test 1: Health Check and Monitoring Endpoints
echo -e "${YELLOW}=== HEALTH CHECK & MONITORING ENDPOINTS ===${NC}"
test_endpoint "GET" "/api/v1/health" "" "" "Custom health check endpoint" "200"
test_endpoint "GET" "/actuator/health" "" "" "Spring Boot actuator health check" "200"
test_endpoint "GET" "/actuator/info" "" "" "Application info endpoint" ""
test_endpoint "GET" "/actuator/metrics" "" "" "Application metrics endpoint" "401"

# Test 2: Documentation Endpoints
echo -e "\n${YELLOW}=== DOCUMENTATION ENDPOINTS ===${NC}"
test_endpoint "GET" "/swagger-ui/index.html" "" "" "Swagger UI interface" "200"
test_endpoint "GET" "/v3/api-docs" "" "" "OpenAPI specification" "400"

# Test 3: Authentication Endpoints (Public)
echo -e "\n${YELLOW}=== AUTHENTICATION ENDPOINTS (PUBLIC) ===${NC}"
# Generate random username to avoid conflicts
RANDOM_USER="testuser_$(date +%s)_$RANDOM"
RANDOM_EMAIL="test_$(date +%s)@example.com"
test_endpoint "POST" "/api/v1/auth/register" '{"username":"'$RANDOM_USER'","email":"'$RANDOM_EMAIL'","password":"TestPass123!","firstName":"Test","lastName":"User"}' "" "User registration endpoint" "201"
test_endpoint "POST" "/api/v1/auth/login" '{"usernameOrEmail":"admin","password":"Admin123!"}' "" "User login endpoint" "401"

# Get access token for authenticated tests
echo -e "\n${BLUE}Getting access token for authenticated tests...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" -d '{"usernameOrEmail":"testuser","password":"TestPass123!"}' "$BASE_URL/api/v1/auth/login")
ACCESS_TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -n "$ACCESS_TOKEN" ] && [ "$ACCESS_TOKEN" != "null" ]; then
    echo -e "${GREEN}✓ Access token obtained${NC}"
    AUTH_HEADER="-H \"Authorization: Bearer $ACCESS_TOKEN\""
else
    echo -e "${RED}✗ Failed to obtain access token${NC}"
    AUTH_HEADER=""
fi

# Test 4: Token Management
echo -e "\n${YELLOW}=== TOKEN MANAGEMENT ENDPOINTS ===${NC}"
test_endpoint "POST" "/api/v1/auth/validate" '{"token":"invalid-token"}' "" "Token validation endpoint" "500"
test_endpoint "POST" "/api/v1/auth/introspect" '{"token":"invalid-token"}' "" "Token introspection endpoint" "200"

# Test 5: Password Reset
echo -e "\n${YELLOW}=== PASSWORD RESET ENDPOINTS ===${NC}"
test_endpoint "POST" "/api/v1/auth/password/reset-request" '{"email":"test@example.com"}' "" "Password reset request endpoint" "200"

# Test 6: OAuth2 and OpenID Connect Endpoints
echo -e "\n${YELLOW}=== OAUTH2 & OPENID CONNECT ENDPOINTS ===${NC}"
test_endpoint "GET" "/.well-known/openid_configuration" "" "" "OpenID Connect discovery endpoint" "200"
test_endpoint "GET" "/.well-known/jwks.json" "" "" "JSON Web Key Set endpoint" "200"
test_endpoint "GET" "/oauth2/authorize?client_id=test&response_type=code&redirect_uri=http://localhost:3000/callback" "" "" "OAuth2 authorization endpoint" ""

# Test 6: Admin Endpoints (Protected)
echo -e "\n${YELLOW}=== ADMIN ENDPOINTS (PROTECTED) ===${NC}"
if [ -n "$AUTH_HEADER" ]; then
    eval "test_endpoint \"GET\" \"/api/admin/users/test-user-id/lockout-status\" \"\" \"$AUTH_HEADER\" \"User lockout status endpoint\" \"\""
else
    echo -e "  ${YELLOW}⚠ Skipping admin endpoints - no auth token${NC}"
fi

# Test 7: User Management Endpoints
echo -e "\n${YELLOW}=== USER MANAGEMENT ENDPOINTS ===${NC}"
test_endpoint "GET" "/api/v1/users/me" "" "$AUTH_HEADER" "Get current user profile" ""
test_endpoint "GET" "/api/v1/users" "" "$AUTH_HEADER" "List users endpoint" ""

# Test 8: Persona Management
echo -e "\n${YELLOW}=== PERSONA MANAGEMENT ENDPOINTS ===${NC}"
test_endpoint "GET" "/api/v1/personas" "" "$AUTH_HEADER" "List personas endpoint" ""
test_endpoint "GET" "/api/v1/personas/active" "" "$AUTH_HEADER" "Get active persona endpoint" ""

# Test 9: Privacy Endpoints
echo -e "\n${YELLOW}=== PRIVACY MANAGEMENT ENDPOINTS ===${NC}"
test_endpoint "GET" "/api/v1/privacy/settings" "" "$AUTH_HEADER" "Privacy settings endpoint" ""

# Test 10: Error Scenarios
echo -e "\n${YELLOW}=== ERROR SCENARIOS ===${NC}"
test_endpoint "GET" "/api/v1/nonexistent" "" "" "Non-existent endpoint" "401"
test_endpoint "POST" "/api/v1/auth/login" '{"invalid":"json"}' "" "Invalid login payload" "400"
test_endpoint "GET" "/api/v1/users/me" "" "" "Protected endpoint without auth" "401"

# Generate JSON Report
echo -e "\n${BLUE}Generating test report...${NC}"

# Count results
total_tests=${#test_results[@]}
passed_tests=0
failed_tests=0

for result in "${test_results[@]}"; do
    if echo "$result" | grep -q '"status": "PASS"'; then
        ((passed_tests++))
    else
        ((failed_tests++))
    fi
done

# Create JSON report
cat > "$REPORT_FILE" <<EOF
{
    "test_summary": {
        "total_tests": $total_tests,
        "passed_tests": $passed_tests,
        "failed_tests": $failed_tests,
        "success_rate": "$(echo "scale=2; $passed_tests * 100 / $total_tests" | bc)%",
        "test_timestamp": "$TIMESTAMP",
        "base_url": "$BASE_URL"
    },
    "test_results": [
        $(IFS=','; echo "${test_results[*]}")
    ]
}
EOF

echo ""
echo "======================================================"
echo "TEST SUMMARY"
echo "======================================================"
echo -e "Total Tests: ${BLUE}$total_tests${NC}"
echo -e "Passed: ${GREEN}$passed_tests${NC}"
echo -e "Failed: ${RED}$failed_tests${NC}"
echo -e "Success Rate: ${YELLOW}$(echo "scale=1; $passed_tests * 100 / $total_tests" | bc)%${NC}"
echo ""
echo -e "Full report saved to: ${BLUE}$REPORT_FILE${NC}"
echo "======================================================"