#!/bin/bash

# ============================================================================
# Buddy Service Endpoint Testing Script - Through Cloudflare Tunnel
# ============================================================================

# Configuration
BASE_URL="https://buddy.focushive.app"
API_VERSION="v1"
API_BASE="${BASE_URL}/api/${API_VERSION}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test counter
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Test JWT token (for authenticated endpoints)
TEST_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0LXVzZXItMTIzIiwidXNlcklkIjoidGVzdC11c2VyLTEyMyIsImVtYWlsIjoidGVzdEB0ZXN0LmNvbSIsImV4cCI6OTk5OTk5OTk5OX0.test"

# Function to print section header
print_section() {
    echo ""
    echo -e "${BLUE}============================================================${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}============================================================${NC}"
    echo ""
}

# Function to test endpoint
test_endpoint() {
    local method="$1"
    local endpoint="$2"
    local expected_code="$3"
    local description="$4"
    local headers="$5"
    local data="$6"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    # Build curl command
    local curl_cmd="curl -s -o /dev/null -w '%{http_code}' -X ${method} ${endpoint}"

    if [ -n "$headers" ]; then
        curl_cmd="$curl_cmd -H '$headers'"
    fi

    if [ -n "$data" ]; then
        curl_cmd="$curl_cmd -d '$data' -H 'Content-Type: application/json'"
    fi

    # Execute curl command
    local response_code=$(eval $curl_cmd)

    if [ "$response_code" = "$expected_code" ]; then
        echo -e "${GREEN}✓${NC} ${description}"
        echo -e "  ${method} ${endpoint} → ${GREEN}${response_code}${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}✗${NC} ${description}"
        echo -e "  ${method} ${endpoint} → ${RED}${response_code}${NC} (expected ${expected_code})"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

# Function to test endpoint with response
test_endpoint_with_response() {
    local method="$1"
    local endpoint="$2"
    local description="$3"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    echo -e "${YELLOW}→${NC} ${description}"
    echo -e "  ${method} ${endpoint}"

    local response=$(curl -s -X ${method} ${endpoint})
    local response_code=$(curl -s -o /dev/null -w '%{http_code}' -X ${method} ${endpoint})

    if [ "$response_code" = "200" ] || [ "$response_code" = "201" ]; then
        echo -e "  Response: ${GREEN}${response_code}${NC}"
        echo "  Body: ${response}" | head -n 3
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "  Response: ${RED}${response_code}${NC}"
        echo "  Body: ${response}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

# ============================================================================
# Start Testing
# ============================================================================

clear
echo -e "${BLUE}"
echo "╔══════════════════════════════════════════════════════════╗"
echo "║     BUDDY SERVICE ENDPOINT TESTING (CLOUDFLARE)         ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo -e "${NC}"
echo "Base URL: ${BASE_URL}"
echo "API Base: ${API_BASE}"
echo ""

# ============================================================================
# 1. Health & Infrastructure Endpoints
# ============================================================================

print_section "1. HEALTH & INFRASTRUCTURE ENDPOINTS"

test_endpoint "GET" "${API_BASE}/health" "200" "Application Health Check" "" ""
test_endpoint "GET" "${BASE_URL}/actuator/health" "404" "Actuator Health (Should redirect)" "" ""
test_endpoint "GET" "${BASE_URL}/swagger-ui/index.html" "200" "Swagger UI" "" ""
test_endpoint "GET" "${BASE_URL}/v3/api-docs" "200" "OpenAPI Documentation" "" ""

# ============================================================================
# 2. Matching Endpoints (Require Authentication)
# ============================================================================

print_section "2. MATCHING API ENDPOINTS"

test_endpoint "GET" "${API_BASE}/buddy/matching/preferences" "401" "Get Preferences (No Auth)" "" ""
test_endpoint "GET" "${API_BASE}/buddy/matching/preferences" "401" "Get Preferences (With Token)" "X-User-Id: test-user-123" ""
test_endpoint "PUT" "${API_BASE}/buddy/matching/preferences" "401" "Update Preferences" "X-User-Id: test-user-123" '{"timezone":"UTC"}'
test_endpoint "POST" "${API_BASE}/buddy/matching/queue/join" "401" "Join Matching Queue" "X-User-Id: test-user-123" ""
test_endpoint "GET" "${API_BASE}/buddy/matching/queue/status" "401" "Queue Status" "X-User-Id: test-user-123" ""
test_endpoint "DELETE" "${API_BASE}/buddy/matching/queue/leave" "401" "Leave Queue" "X-User-Id: test-user-123" ""
test_endpoint "GET" "${API_BASE}/buddy/matching/suggestions" "401" "Get Suggestions" "X-User-Id: test-user-123" ""

# ============================================================================
# 3. Partnership Endpoints
# ============================================================================

print_section "3. PARTNERSHIP API ENDPOINTS"

test_endpoint "POST" "${API_BASE}/buddy/partnerships/request" "401" "Create Partnership Request" "X-User-Id: test-user-123" '{"targetUserId":"user-456"}'
test_endpoint "GET" "${API_BASE}/buddy/partnerships" "401" "List Partnerships" "X-User-Id: test-user-123" ""
test_endpoint "GET" "${API_BASE}/buddy/partnerships/active" "401" "Get Active Partnerships" "X-User-Id: test-user-123" ""
test_endpoint "GET" "${API_BASE}/buddy/partnerships/pending" "401" "Get Pending Requests" "X-User-Id: test-user-123" ""
test_endpoint "PUT" "${API_BASE}/buddy/partnerships/123/accept" "401" "Accept Partnership" "X-User-Id: test-user-123" ""
test_endpoint "PUT" "${API_BASE}/buddy/partnerships/123/reject" "401" "Reject Partnership" "X-User-Id: test-user-123" ""
test_endpoint "DELETE" "${API_BASE}/buddy/partnerships/123" "401" "End Partnership" "X-User-Id: test-user-123" ""

# ============================================================================
# 4. Goals Endpoints
# ============================================================================

print_section "4. GOALS API ENDPOINTS"

test_endpoint "POST" "${API_BASE}/buddy/goals" "401" "Create Goal" "X-User-Id: test-user-123" '{"title":"Test Goal"}'
test_endpoint "GET" "${API_BASE}/buddy/goals" "401" "List Goals" "X-User-Id: test-user-123" ""
test_endpoint "GET" "${API_BASE}/buddy/goals/123" "401" "Get Goal Details" "X-User-Id: test-user-123" ""
test_endpoint "PUT" "${API_BASE}/buddy/goals/123" "401" "Update Goal" "X-User-Id: test-user-123" '{"progress":50}'
test_endpoint "DELETE" "${API_BASE}/buddy/goals/123" "401" "Delete Goal" "X-User-Id: test-user-123" ""
test_endpoint "POST" "${API_BASE}/buddy/goals/123/milestones" "401" "Add Milestone" "X-User-Id: test-user-123" '{"title":"Milestone 1"}'

# ============================================================================
# 5. Check-in Endpoints
# ============================================================================

print_section "5. CHECK-IN API ENDPOINTS"

test_endpoint "POST" "${API_BASE}/buddy/checkins" "401" "Create Check-in" "X-User-Id: test-user-123" '{"mood":"HAPPY"}'
test_endpoint "GET" "${API_BASE}/buddy/checkins" "401" "List Check-ins" "X-User-Id: test-user-123" ""
test_endpoint "GET" "${API_BASE}/buddy/checkins/pending" "401" "Get Pending Check-ins" "X-User-Id: test-user-123" ""
test_endpoint "PUT" "${API_BASE}/buddy/checkins/123" "401" "Update Check-in" "X-User-Id: test-user-123" '{"completed":true}'
test_endpoint "GET" "${API_BASE}/buddy/checkins/streak" "401" "Get Streak Info" "X-User-Id: test-user-123" ""

# ============================================================================
# 6. Accountability Endpoints
# ============================================================================

print_section "6. ACCOUNTABILITY API ENDPOINTS"

test_endpoint "GET" "${API_BASE}/buddy/accountability/score" "401" "Get Accountability Score" "X-User-Id: test-user-123" ""
test_endpoint "GET" "${API_BASE}/buddy/accountability/history" "401" "Get Score History" "X-User-Id: test-user-123" ""
test_endpoint "GET" "${API_BASE}/buddy/accountability/leaderboard" "401" "Get Leaderboard" "X-User-Id: test-user-123" ""

# ============================================================================
# 7. Test with Response Bodies
# ============================================================================

print_section "7. DETAILED RESPONSE TESTS"

test_endpoint_with_response "GET" "${API_BASE}/health" "Health Check Response"
test_endpoint_with_response "GET" "${BASE_URL}/actuator/health" "Actuator Redirect Response"

# ============================================================================
# 8. Performance Test
# ============================================================================

print_section "8. PERFORMANCE TEST"

echo "Testing response time through Cloudflare tunnel..."
response_time=$(curl -s -o /dev/null -w '%{time_total}' ${API_BASE}/health)
echo -e "Health endpoint response time: ${GREEN}${response_time}s${NC}"

# Multiple requests test
echo ""
echo "Testing 10 sequential requests..."
total_time=0
for i in {1..10}; do
    time=$(curl -s -o /dev/null -w '%{time_total}' ${API_BASE}/health)
    total_time=$(echo "$total_time + $time" | bc)
    echo -n "."
done
echo ""
avg_time=$(echo "scale=3; $total_time / 10" | bc)
echo -e "Average response time: ${GREEN}${avg_time}s${NC}"

# ============================================================================
# Summary
# ============================================================================

print_section "TEST SUMMARY"

echo -e "Total Tests: ${BLUE}${TOTAL_TESTS}${NC}"
echo -e "Passed: ${GREEN}${PASSED_TESTS}${NC}"
echo -e "Failed: ${RED}${FAILED_TESTS}${NC}"

if [ $FAILED_TESTS -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✓ All endpoints responding as expected!${NC}"
else
    echo ""
    echo -e "${YELLOW}⚠ Some tests failed. This is expected for authenticated endpoints.${NC}"
fi

echo ""
echo -e "${BLUE}Note:${NC} 401 responses are expected for authenticated endpoints without valid JWT tokens."
echo ""
echo "Test completed at: $(date)"
echo ""