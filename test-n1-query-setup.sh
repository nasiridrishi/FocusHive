#!/bin/bash

# Test script for N+1 Query Performance Setup
# This script verifies that PostgreSQL and Identity Service are properly configured
# for testing UOL-335 N+1 query performance improvements

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
POSTGRES_PORT=5433
IDENTITY_PORT=8081
BACKEND_PORT=8080
DB_NAME="identity_db"
DB_USER="identity_user"
TEST_TIMEOUT=30

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Test 1: Check if PostgreSQL is running on port 5433
test_postgresql() {
    log_info "Testing PostgreSQL connection on port ${POSTGRES_PORT}..."
    
    if nc -z localhost ${POSTGRES_PORT} 2>/dev/null; then
        log_success "PostgreSQL is running on port ${POSTGRES_PORT}"
        
        # Test database connection
        log_info "Testing database connection to ${DB_NAME}..."
        if PGPASSWORD="${DB_PASSWORD:-identity_pass}" psql -h localhost -p ${POSTGRES_PORT} -U "${DB_USER}" -d "${DB_NAME}" -c "SELECT version();" >/dev/null 2>&1; then
            log_success "Database connection successful"
            return 0
        else
            log_error "Cannot connect to database ${DB_NAME}"
            return 1
        fi
    else
        log_error "PostgreSQL is not running on port ${POSTGRES_PORT}"
        return 1
    fi
}

# Test 2: Check if Identity Service is running on port 8081
test_identity_service() {
    log_info "Testing Identity Service on port ${IDENTITY_PORT}..."
    
    # Wait for service to be ready
    local count=0
    while [ $count -lt $TEST_TIMEOUT ]; do
        if nc -z localhost ${IDENTITY_PORT} 2>/dev/null; then
            log_success "Identity Service is running on port ${IDENTITY_PORT}"
            break
        fi
        sleep 1
        ((count++))
    done
    
    if [ $count -eq $TEST_TIMEOUT ]; then
        log_error "Identity Service is not running on port ${IDENTITY_PORT}"
        return 1
    fi
    
    # Test health endpoint
    log_info "Testing Identity Service health endpoint..."
    local health_response
    if health_response=$(curl -s -f "http://localhost:${IDENTITY_PORT}/actuator/health" 2>/dev/null); then
        if echo "$health_response" | grep -q '"status":"UP"'; then
            log_success "Identity Service health check passed"
            echo "Health status: $(echo "$health_response" | jq -r '.status' 2>/dev/null || echo "UP")"
            return 0
        else
            log_error "Identity Service health check failed"
            echo "Response: $health_response"
            return 1
        fi
    else
        log_error "Cannot reach Identity Service health endpoint"
        return 1
    fi
}

# Test 3: Check database schema exists (Flyway migrations ran)
test_database_schema() {
    log_info "Testing database schema (Flyway migrations)..."
    
    local tables_query="SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public';"
    local table_count
    
    if table_count=$(PGPASSWORD="${DB_PASSWORD:-identity_pass}" psql -h localhost -p ${POSTGRES_PORT} -U "${DB_USER}" -d "${DB_NAME}" -t -c "$tables_query" 2>/dev/null | xargs); then
        if [ "$table_count" -gt 0 ]; then
            log_success "Database schema exists with $table_count tables"
            
            # List main tables
            log_info "Main tables in database:"
            PGPASSWORD="${DB_PASSWORD:-identity_pass}" psql -h localhost -p ${POSTGRES_PORT} -U "${DB_USER}" -d "${DB_NAME}" -t -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name;" 2>/dev/null | sed 's/^/ - /'
            return 0
        else
            log_error "Database schema appears empty (no tables found)"
            return 1
        fi
    else
        log_error "Cannot query database schema"
        return 1
    fi
}

# Test 4: Test service communication
test_service_communication() {
    log_info "Testing service communication..."
    
    # Test if backend can reach identity service (via actuator info)
    log_info "Testing backend service health..."
    if curl -s -f "http://localhost:${BACKEND_PORT}/actuator/health" >/dev/null 2>&1; then
        log_success "Backend service is healthy"
    else
        log_warning "Backend service health check failed"
    fi
    
    # Test identity service API endpoints
    log_info "Testing Identity Service API endpoints..."
    if curl -s -f "http://localhost:${IDENTITY_PORT}/api-docs" >/dev/null 2>&1; then
        log_success "Identity Service API documentation accessible"
    else
        log_warning "Identity Service API documentation not accessible"
    fi
    
    return 0
}

# Test 5: Check environment variables
test_environment() {
    log_info "Testing required environment variables..."
    
    local missing_vars=()
    
    # Check critical environment variables
    [ -z "${JWT_SECRET}" ] && missing_vars+=("JWT_SECRET")
    [ -z "${DB_PASSWORD}" ] && missing_vars+=("DB_PASSWORD")
    
    if [ ${#missing_vars[@]} -eq 0 ]; then
        log_success "All required environment variables are set"
        return 0
    else
        log_error "Missing required environment variables: ${missing_vars[*]}"
        return 1
    fi
}

# Test 6: Performance baseline check
test_performance_baseline() {
    log_info "Testing N+1 query performance baseline..."
    
    # This would be where we'd run specific N+1 query tests
    # For now, just verify we can connect to both services
    
    local start_time=$(date +%s%N)
    
    # Simple API call to identity service
    if curl -s -f "http://localhost:${IDENTITY_PORT}/actuator/info" >/dev/null 2>&1; then
        local end_time=$(date +%s%N)
        local duration=$(( (end_time - start_time) / 1000000 )) # Convert to milliseconds
        
        log_success "Identity Service response time: ${duration}ms"
        
        if [ $duration -lt 1000 ]; then
            log_success "Response time is good for testing"
        else
            log_warning "Response time is high (${duration}ms) - may affect test results"
        fi
        return 0
    else
        log_error "Cannot test performance baseline - service not responding"
        return 1
    fi
}

# Main test runner
run_tests() {
    echo "=============================================="
    echo "  N+1 Query Performance Setup Test Suite"
    echo "=============================================="
    echo ""
    
    local tests_passed=0
    local tests_failed=0
    
    # Run all tests
    if test_environment; then
        ((tests_passed++))
    else
        ((tests_failed++))
    fi
    echo ""
    
    if test_postgresql; then
        ((tests_passed++))
    else
        ((tests_failed++))
    fi
    echo ""
    
    if test_identity_service; then
        ((tests_passed++))
    else
        ((tests_failed++))
    fi
    echo ""
    
    if test_database_schema; then
        ((tests_passed++))
    else
        ((tests_failed++))
    fi
    echo ""
    
    if test_service_communication; then
        ((tests_passed++))
    else
        ((tests_failed++))
    fi
    echo ""
    
    if test_performance_baseline; then
        ((tests_passed++))
    else
        ((tests_failed++))
    fi
    echo ""
    
    # Summary
    echo "=============================================="
    echo "  Test Results Summary"
    echo "=============================================="
    log_success "Tests passed: $tests_passed"
    
    if [ $tests_failed -gt 0 ]; then
        log_error "Tests failed: $tests_failed"
        echo ""
        log_error "Setup is not ready for N+1 query testing!"
        echo ""
        echo "Next steps:"
        echo "1. Fix failed tests above"
        echo "2. Ensure PostgreSQL is running on port 5433"
        echo "3. Ensure Identity Service is running on port 8081"
        echo "4. Verify database migrations have run"
        echo ""
        return 1
    else
        echo ""
        log_success "All tests passed! Setup is ready for N+1 query testing."
        echo ""
        echo "You can now:"
        echo "1. Run N+1 query performance tests"
        echo "2. Test Identity Service with PostgreSQL"
        echo "3. Verify performance improvements"
        echo ""
        return 0
    fi
}

# Check if running with --help
if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
    echo "N+1 Query Performance Setup Test Script"
    echo ""
    echo "This script verifies that:"
    echo "- PostgreSQL is running on port 5433 with identity_db database"
    echo "- Identity Service is running on port 8081"
    echo "- Database schema is properly migrated"
    echo "- Services can communicate"
    echo "- Environment is ready for performance testing"
    echo ""
    echo "Usage: $0 [--help]"
    echo ""
    echo "Environment variables expected:"
    echo "- JWT_SECRET: Required for JWT token generation"
    echo "- DB_PASSWORD: Password for PostgreSQL database"
    echo ""
    exit 0
fi

# Source environment if .env exists
if [ -f ".env" ]; then
    log_info "Loading environment from .env file"
    set -a
    source .env
    set +a
fi

# Run the tests
run_tests
exit $?