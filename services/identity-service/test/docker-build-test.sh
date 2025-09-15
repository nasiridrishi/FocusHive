#!/bin/bash

# FocusHive Identity Service - Docker Build Test Script
# Following TDD principles for production-ready deployment

set -e

echo "=== Identity Service Docker Build Test Suite ==="
echo "Starting test execution at $(date)"

# Test configuration
IMAGE_NAME="focushive-identity-service"
IMAGE_TAG="test"
CONTAINER_NAME="focushive-identity-test"
TEST_PORT=8081
HEALTH_ENDPOINT="http://localhost:$TEST_PORT/api/v1/health"
MAX_WAIT_TIME=60

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test result tracking
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Cleanup function
cleanup() {
    echo -e "${YELLOW}Cleaning up test resources...${NC}"
    docker stop $CONTAINER_NAME 2>/dev/null || true
    docker rm $CONTAINER_NAME 2>/dev/null || true
    docker rmi $IMAGE_NAME:$IMAGE_TAG 2>/dev/null || true
}

# Test result function
test_result() {
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    if [ $1 -eq 0 ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        echo -e "${GREEN}✓ Test $TOTAL_TESTS: $2 - PASSED${NC}"
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo -e "${RED}✗ Test $TOTAL_TESTS: $2 - FAILED${NC}"
        echo "Error: $3"
    fi
}

# Test 1: Verify Docker is available
echo -e "\n${YELLOW}Test 1: Docker availability check${NC}"
if docker --version > /dev/null 2>&1; then
    test_result 0 "Docker is available"
else
    test_result 1 "Docker is available" "Docker command not found"
    exit 1
fi

# Test 2: Verify Dockerfile exists
echo -e "\n${YELLOW}Test 2: Dockerfile existence check${NC}"
if [ -f "Dockerfile" ]; then
    test_result 0 "Dockerfile exists"
else
    test_result 1 "Dockerfile exists" "Dockerfile not found in current directory"
    exit 1
fi

# Test 3: Verify .dockerignore exists
echo -e "\n${YELLOW}Test 3: .dockerignore existence check${NC}"
if [ -f ".dockerignore" ]; then
    test_result 0 ".dockerignore exists"
else
    test_result 1 ".dockerignore exists" ".dockerignore not found"
fi

# Test 4: Build Docker image
echo -e "\n${YELLOW}Test 4: Docker build process${NC}"
echo "Building Docker image..."
if docker build -t $IMAGE_NAME:$IMAGE_TAG . > docker-build.log 2>&1; then
    test_result 0 "Docker image build successful"
else
    test_result 1 "Docker image build successful" "Build failed - check docker-build.log"
    cat docker-build.log
    cleanup
    exit 1
fi

# Test 5: Verify image was created
echo -e "\n${YELLOW}Test 5: Image creation verification${NC}"
if docker images | grep -q "$IMAGE_NAME.*$IMAGE_TAG"; then
    IMAGE_SIZE=$(docker images --format "{{.Size}}" $IMAGE_NAME:$IMAGE_TAG)
    test_result 0 "Docker image created (Size: $IMAGE_SIZE)"
else
    test_result 1 "Docker image created" "Image not found in docker images"
    cleanup
    exit 1
fi

# Test 6: Verify image layers and optimization
echo -e "\n${YELLOW}Test 6: Image optimization check${NC}"
LAYER_COUNT=$(docker history $IMAGE_NAME:$IMAGE_TAG | wc -l)
IMAGE_SIZE_MB=$(docker images --format "{{.Size}}" $IMAGE_NAME:$IMAGE_TAG | sed 's/MB//')
echo "Image has $LAYER_COUNT layers"
if [ $LAYER_COUNT -lt 50 ]; then
    test_result 0 "Image layer optimization (Layers: $LAYER_COUNT)"
else
    test_result 1 "Image layer optimization" "Too many layers: $LAYER_COUNT (expected < 50)"
fi

# Test 7: Start container
echo -e "\n${YELLOW}Test 7: Container startup${NC}"
echo "Starting container..."

# Load environment variables from .env file
if [ -f "../../.env" ]; then
    export $(cat ../../.env | grep -v '^#' | xargs)
fi

# Start the container with required environment variables
if docker run -d \
    --name $CONTAINER_NAME \
    -p $TEST_PORT:8081 \
    -e JWT_SECRET=${JWT_SECRET:-ef90b5d6dabcf307e93ccfc6df11dc2838b45fae0f0e3c4f5db8a4c991d5b8f6} \
    -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/focushive_identity \
    -e DATABASE_USERNAME=focushive_user \
    -e DATABASE_PASSWORD=${DATABASE_PASSWORD:-focushive_pass} \
    -e REDIS_HOST=host.docker.internal \
    -e REDIS_PORT=6379 \
    -e REDIS_PASSWORD=${REDIS_PASSWORD:-redis_pass} \
    -e SPRING_PROFILES_ACTIVE=docker \
    $IMAGE_NAME:$IMAGE_TAG > /dev/null 2>&1; then
    test_result 0 "Container started successfully"
else
    test_result 1 "Container started successfully" "Failed to start container"
    cleanup
    exit 1
fi

# Test 8: Wait for container to be healthy
echo -e "\n${YELLOW}Test 8: Container health check${NC}"
echo "Waiting for container to be healthy (max ${MAX_WAIT_TIME}s)..."
WAIT_TIME=0
while [ $WAIT_TIME -lt $MAX_WAIT_TIME ]; do
    if docker inspect --format='{{.State.Health.Status}}' $CONTAINER_NAME 2>/dev/null | grep -q "healthy"; then
        test_result 0 "Container is healthy"
        break
    elif docker inspect --format='{{.State.Status}}' $CONTAINER_NAME 2>/dev/null | grep -q "exited"; then
        test_result 1 "Container is healthy" "Container exited unexpectedly"
        echo "Container logs:"
        docker logs $CONTAINER_NAME
        cleanup
        exit 1
    fi
    sleep 2
    WAIT_TIME=$((WAIT_TIME + 2))
done

if [ $WAIT_TIME -ge $MAX_WAIT_TIME ]; then
    test_result 1 "Container is healthy" "Timeout waiting for container to be healthy"
    echo "Container logs:"
    docker logs $CONTAINER_NAME
    cleanup
    exit 1
fi

# Test 9: Check container logs for errors
echo -e "\n${YELLOW}Test 9: Container log analysis${NC}"
if docker logs $CONTAINER_NAME 2>&1 | grep -i "error\|exception" | grep -v "ERROR_CODE" | head -5; then
    test_result 1 "No critical errors in logs" "Errors found in container logs"
else
    test_result 0 "No critical errors in logs"
fi

# Test 10: Verify exposed port
echo -e "\n${YELLOW}Test 10: Port exposure verification${NC}"
if docker port $CONTAINER_NAME | grep -q "8081"; then
    test_result 0 "Port 8081 is exposed"
else
    test_result 1 "Port 8081 is exposed" "Port not exposed correctly"
fi

# Test 11: Test health endpoint
echo -e "\n${YELLOW}Test 11: Health endpoint test${NC}"
echo "Testing health endpoint..."
sleep 5 # Give service time to fully start
HEALTH_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" $HEALTH_ENDPOINT 2>/dev/null || echo "000")
if [ "$HEALTH_RESPONSE" = "200" ] || [ "$HEALTH_RESPONSE" = "204" ]; then
    test_result 0 "Health endpoint responding (HTTP $HEALTH_RESPONSE)"
else
    test_result 1 "Health endpoint responding" "HTTP response: $HEALTH_RESPONSE"
fi

# Test 12: Verify non-root user
echo -e "\n${YELLOW}Test 12: Security - Non-root user verification${NC}"
USER_ID=$(docker exec $CONTAINER_NAME id -u 2>/dev/null || echo "0")
if [ "$USER_ID" != "0" ]; then
    test_result 0 "Container running as non-root user (UID: $USER_ID)"
else
    test_result 1 "Container running as non-root user" "Running as root (UID: 0)"
fi

# Test 13: Environment variables
echo -e "\n${YELLOW}Test 13: Environment variable verification${NC}"
if docker exec $CONTAINER_NAME printenv | grep -q "JWT_SECRET"; then
    test_result 0 "Required environment variables set"
else
    test_result 1 "Required environment variables set" "JWT_SECRET not found"
fi

# Test 14: JVM settings verification
echo -e "\n${YELLOW}Test 14: JVM configuration check${NC}"
if docker exec $CONTAINER_NAME sh -c 'ps aux | grep java' | grep -q "UseContainerSupport"; then
    test_result 0 "JVM container optimizations applied"
else
    test_result 1 "JVM container optimizations applied" "UseContainerSupport not found"
fi

# Final cleanup
cleanup

# Test summary
echo -e "\n${YELLOW}=== Test Summary ===${NC}"
echo "Total Tests: $TOTAL_TESTS"
echo -e "${GREEN}Passed: $PASSED_TESTS${NC}"
echo -e "${RED}Failed: $FAILED_TESTS${NC}"

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "\n${GREEN}✓ All tests passed! Docker build is production-ready.${NC}"
    exit 0
else
    echo -e "\n${RED}✗ Some tests failed. Please fix the issues before deployment.${NC}"
    exit 1
fi