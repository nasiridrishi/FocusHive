#!/bin/bash

# =============================================================================
# FocusHive Buddy Service - Comprehensive Endpoint Testing Script
# =============================================================================

set -e  # Exit on error

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Base URL
BASE_URL="http://localhost:8087/api/v1/buddy"

# Test data - Using UUIDs
USER1_ID="550e8400-e29b-41d4-a716-446655440001"
USER2_ID="550e8400-e29b-41d4-a716-446655440002"
USER3_ID="550e8400-e29b-41d4-a716-446655440003"
ADMIN_ID="550e8400-e29b-41d4-a716-446655440000"
PARTNERSHIP_ID=""
GOAL_ID=""
MILESTONE_ID=""
CHECKIN_ID=""

# Counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Function to test endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local description=$3
    local data=$4
    local expected_status=$5
    local user_id=${6:-$USER1_ID}

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    echo -ne "${CYAN}Testing: ${NC}$description... "

    # Generate JWT token for the user
    local token=$(./create-jwt.sh "$user_id" "USER" 2>/dev/null | tail -1)
    if [ "$user_id" == "$ADMIN_ID" ]; then
        token=$(./create-jwt.sh "$user_id" "ADMIN" 2>/dev/null | tail -1)
    fi

    local response
    local status_code

    if [ "$method" == "GET" ] || [ "$method" == "DELETE" ]; then
        response=$(curl -s -w "\n%{http_code}" -X $method \
            -H "Authorization: Bearer $token" \
            -H "X-User-ID: $user_id" \
            -H "Content-Type: application/json" \
            "$BASE_URL$endpoint")
    else
        response=$(curl -s -w "\n%{http_code}" -X $method \
            -H "Authorization: Bearer $token" \
            -H "X-User-ID: $user_id" \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$BASE_URL$endpoint")
    fi

    status_code=$(echo "$response" | tail -n 1)
    body=$(echo "$response" | sed '$d')

    if [ "$status_code" == "$expected_status" ]; then
        echo -e "${GREEN}✅ PASSED${NC} (Status: $status_code)"
        PASSED_TESTS=$((PASSED_TESTS + 1))

        # Extract IDs if needed
        if [[ "$endpoint" == "/partnerships/request" ]] && [ "$status_code" == "201" ]; then
            PARTNERSHIP_ID=$(echo "$body" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
            echo "  Partnership ID: $PARTNERSHIP_ID"
        elif [[ "$endpoint" == "/goals" ]] && [ "$status_code" == "201" ]; then
            GOAL_ID=$(echo "$body" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
            echo "  Goal ID: $GOAL_ID"
        elif [[ "$endpoint" == "/checkins/daily" ]] && [ "$status_code" == "201" ]; then
            CHECKIN_ID=$(echo "$body" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
            echo "  Checkin ID: $CHECKIN_ID"
        fi

        return 0
    else
        echo -e "${RED}❌ FAILED${NC} (Expected: $expected_status, Got: $status_code)"
        echo "  Response: $(echo "$body" | head -n 1)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
}

echo -e "${BLUE}==============================================================================${NC}"
echo -e "${BLUE}           FocusHive Buddy Service - Comprehensive Endpoint Testing           ${NC}"
echo -e "${BLUE}==============================================================================${NC}"
echo ""

# =============================================================================
# Health Check
# =============================================================================
echo -e "${YELLOW}🏥 Health Check Endpoint${NC}"
echo -e "${YELLOW}------------------------${NC}"
# Health check doesn't need auth - test directly
echo -ne "${CYAN}Testing: ${NC}Health check... "
health_response=$(curl -s -w "\n%{http_code}" "http://localhost:8087/api/v1/health")
health_status=$(echo "$health_response" | tail -n 1)
if [ "$health_status" == "200" ]; then
    echo -e "${GREEN}✅ PASSED${NC} (Status: 200)"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ FAILED${NC} (Got: $health_status)"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))
echo ""

# =============================================================================
# Matching Controller (8 endpoints)
# =============================================================================
echo -e "${YELLOW}🤝 Matching Controller Tests${NC}"
echo -e "${YELLOW}----------------------------${NC}"

# Set up matching preferences first
PREFERENCES_DATA='{
  "userId": "'$USER1_ID'",
  "interests": ["coding", "fitness", "reading"],
  "goals": ["learn new skills", "stay healthy"],
  "preferredTimezone": "UTC",
  "communicationStyle": "direct",
  "experienceLevel": "intermediate",
  "maxPartners": 3
}'

test_endpoint "PUT" "/matching/preferences" "Update matching preferences (User 1)" "$PREFERENCES_DATA" "200" "$USER1_ID"

PREFERENCES_DATA='{"userId": "'$USER2_ID'", "interests": ["coding", "music"], "goals": ["productivity"], "preferredTimezone": "UTC", "communicationStyle": "direct", "experienceLevel": "beginner", "maxPartners": 3}'
test_endpoint "PUT" "/matching/preferences" "Update matching preferences (User 2)" "$PREFERENCES_DATA" "200" "$USER2_ID"

test_endpoint "POST" "/matching/queue" "Join matching queue" "" "200"
test_endpoint "GET" "/matching/queue/status" "Get queue status" "" "200"
test_endpoint "DELETE" "/matching/queue" "Leave matching queue" "" "200"
test_endpoint "GET" "/matching/queue/size" "Get queue size (Admin)" "" "200" "$ADMIN_ID"
test_endpoint "GET" "/matching/queue/size" "Get queue size (Non-admin - should fail)" "" "403" "$USER1_ID" || true
test_endpoint "GET" "/matching/suggestions?limit=5" "Get match suggestions" "" "200"
test_endpoint "POST" "/matching/calculate" "Calculate compatibility" '{"targetUserId": "'$USER2_ID'"}' "200"
test_endpoint "GET" "/matching/preferences" "Get matching preferences" "" "200"
echo ""

# =============================================================================
# Partnership Controller (8 endpoints)
# =============================================================================
echo -e "${YELLOW}👥 Partnership Controller Tests${NC}"
echo -e "${YELLOW}-------------------------------${NC}"

PARTNERSHIP_REQUEST='{
  "requesterId": "'$USER1_ID'",
  "recipientId": "'$USER2_ID'",
  "message": "Let us be accountability buddies!",
  "durationDays": 30
}'

test_endpoint "POST" "/partnerships/request" "Create partnership request" "$PARTNERSHIP_REQUEST" "201"

if [ ! -z "$PARTNERSHIP_ID" ]; then
    test_endpoint "PUT" "/partnerships/$PARTNERSHIP_ID/approve" "Approve partnership" "" "200" "$USER2_ID"
    test_endpoint "GET" "/partnerships" "Get user partnerships" "" "200"
    test_endpoint "GET" "/partnerships/$PARTNERSHIP_ID" "Get partnership details" "" "200"
    test_endpoint "PUT" "/partnerships/$PARTNERSHIP_ID/pause" "Pause partnership" "" "200"
    test_endpoint "PUT" "/partnerships/$PARTNERSHIP_ID/resume" "Resume partnership" "" "200"
    test_endpoint "GET" "/partnerships/$PARTNERSHIP_ID/health" "Get partnership health" "" "200"

    # Create another partnership to test rejection
    PARTNERSHIP_REQUEST2='{"requesterId": "'$USER1_ID'", "recipientId": "'$USER3_ID'", "message": "Test partnership", "durationDays": 30}'
    test_endpoint "POST" "/partnerships/request" "Create second partnership" "$PARTNERSHIP_REQUEST2" "201"
    NEW_PARTNERSHIP_ID=$(echo "$PARTNERSHIP_ID" | tail -1)
    test_endpoint "PUT" "/partnerships/$NEW_PARTNERSHIP_ID/reject" "Reject partnership" "" "200" "$USER3_ID" || true
fi
echo ""

# =============================================================================
# Goal Controller (10 endpoints)
# =============================================================================
echo -e "${YELLOW}🎯 Goal Controller Tests${NC}"
echo -e "${YELLOW}------------------------${NC}"

GOAL_DATA='{
  "title": "Learn TypeScript",
  "description": "Master TypeScript for better code quality",
  "type": "LEARNING",
  "category": "SKILL_DEVELOPMENT",
  "targetValue": 100,
  "currentValue": 0,
  "unit": "hours",
  "deadline": "2024-12-31",
  "visibility": "PARTNERS",
  "tags": ["programming", "typescript", "web"]
}'

test_endpoint "POST" "/goals" "Create goal" "$GOAL_DATA" "201"

if [ ! -z "$GOAL_ID" ]; then
    test_endpoint "GET" "/goals" "Get user goals" "" "200"
    test_endpoint "GET" "/goals/$GOAL_ID" "Get goal details" "" "200"

    UPDATE_GOAL='{"currentValue": 25, "notes": "Making good progress"}'
    test_endpoint "PUT" "/goals/$GOAL_ID" "Update goal" "$UPDATE_GOAL" "200"

    MILESTONE_DATA='{
      "title": "Complete basic tutorial",
      "description": "Finish official TypeScript tutorial",
      "targetDate": "2024-11-30",
      "completed": false
    }'
    test_endpoint "POST" "/goals/$GOAL_ID/milestones" "Add milestone" "$MILESTONE_DATA" "201"

    test_endpoint "GET" "/goals/$GOAL_ID/progress" "Get goal progress" "" "200"
    test_endpoint "GET" "/goals/templates" "Get goal templates" "" "200"

    SEARCH_DATA='{"query": "typescript", "category": "SKILL_DEVELOPMENT"}'
    test_endpoint "POST" "/goals/search" "Search goals" "$SEARCH_DATA" "200"

    test_endpoint "DELETE" "/goals/$GOAL_ID" "Delete goal" "" "204"
fi
echo ""

# =============================================================================
# Checkin Controller (9 endpoints)
# =============================================================================
echo -e "${YELLOW}📝 Checkin Controller Tests${NC}"
echo -e "${YELLOW}---------------------------${NC}"

DAILY_CHECKIN='{
  "partnershipId": "'$PARTNERSHIP_ID'",
  "checkinType": "DAILY",
  "mood": 7,
  "energyLevel": 8,
  "productivity": 7,
  "stressLevel": 3,
  "notes": "Had a productive day!",
  "goalsProgress": {"fitness": 80, "learning": 60},
  "challenges": ["time management"],
  "wins": ["completed workout", "learned new concept"]
}'

if [ ! -z "$PARTNERSHIP_ID" ]; then
    test_endpoint "POST" "/checkins/daily" "Create daily check-in" "$DAILY_CHECKIN" "201"

    WEEKLY_REVIEW='{
      "partnershipId": "'$PARTNERSHIP_ID'",
      "weekStartDate": "2024-01-01",
      "weekEndDate": "2024-01-07",
      "weeklyProgress": "Good progress on all goals",
      "accomplishments": "Completed 80% of planned tasks",
      "challengesFaced": "Time management issues",
      "nextWeekGoals": "Focus on completing project milestone"
    }'
    test_endpoint "POST" "/checkins/weekly" "Create weekly review" "$WEEKLY_REVIEW" "201"

    test_endpoint "GET" "/checkins?partnershipId=$PARTNERSHIP_ID&startDate=2024-01-01&endDate=2024-12-31" "Get user check-ins" "" "200"

    if [ ! -z "$CHECKIN_ID" ]; then
        test_endpoint "GET" "/checkins/$CHECKIN_ID" "Get check-in details" "" "200"
    fi

    test_endpoint "GET" "/checkins/streaks?partnershipId=$PARTNERSHIP_ID" "Get check-in streaks" "" "200"
    test_endpoint "GET" "/checkins/accountability?partnershipId=$PARTNERSHIP_ID" "Get accountability score" "" "200"
    test_endpoint "GET" "/checkins/analytics?partnershipId=$PARTNERSHIP_ID&startDate=2024-01-01&endDate=2024-12-31" "Get check-in analytics" "" "200"
    test_endpoint "GET" "/checkins/export?partnershipId=$PARTNERSHIP_ID&format=JSON&startDate=2024-01-01&endDate=2024-12-31" "Export check-in data" "" "200"
fi
echo ""

# =============================================================================
# Edge Cases and Error Scenarios
# =============================================================================
echo -e "${YELLOW}⚠️  Edge Cases and Error Scenarios${NC}"
echo -e "${YELLOW}----------------------------------${NC}"

test_endpoint "GET" "/partnerships/invalid-id" "Invalid partnership ID" "" "404" || true
test_endpoint "GET" "/goals?page=-1" "Negative page number" "" "400" || true
test_endpoint "POST" "/partnerships/request" "Empty request body" "{}" "400" || true
test_endpoint "GET" "/checkins" "Missing required parameters" "" "400" || true
test_endpoint "POST" "/goals" "Invalid goal data" '{"title": ""}' "400" || true
echo ""

# =============================================================================
# Summary
# =============================================================================
echo -e "${BLUE}==============================================================================${NC}"
echo -e "${BLUE}                              Test Summary                                    ${NC}"
echo -e "${BLUE}==============================================================================${NC}"
echo ""
echo -e "Total Tests:    ${CYAN}$TOTAL_TESTS${NC}"
echo -e "Passed:         ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed:         ${RED}$FAILED_TESTS${NC}"
SUCCESS_RATE=$((PASSED_TESTS * 100 / TOTAL_TESTS))
echo -e "Success Rate:   ${YELLOW}$SUCCESS_RATE%${NC}"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}🎉 All tests passed successfully!${NC}"
else
    echo -e "${YELLOW}⚠️  Some tests failed. Please check the logs for details.${NC}"
fi

echo ""
echo -e "${BLUE}==============================================================================${NC}"