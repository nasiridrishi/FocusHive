#!/bin/bash

# FocusHive Cross-Service Integration Tests Runner
# Comprehensive test execution script with TDD validation

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/../docker-compose.yml"
TEST_PROFILE="integration-test"
COVERAGE_THRESHOLD=80
MAX_HEAP_SIZE="4g"

echo -e "${BLUE}🚀 FocusHive Cross-Service Integration Tests${NC}"
echo -e "${BLUE}============================================${NC}"
echo

# Function to check prerequisites
check_prerequisites() {
    echo -e "${YELLOW}📋 Checking prerequisites...${NC}"
    
    # Check Java version
    if ! command -v java &> /dev/null; then
        echo -e "${RED}❌ Java not found. Please install Java 21+${NC}"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 21 ]; then
        echo -e "${RED}❌ Java 21+ required. Found version: $JAVA_VERSION${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ Java version: $JAVA_VERSION${NC}"
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}❌ Docker not found. Please install Docker Desktop${NC}"
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        echo -e "${RED}❌ Docker daemon not running. Please start Docker Desktop${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ Docker is running${NC}"
    
    # Check Gradle wrapper
    if [ ! -f "$SCRIPT_DIR/gradlew" ]; then
        echo -e "${RED}❌ Gradle wrapper not found${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ Gradle wrapper found${NC}"
    
    echo
}

# Function to clean up previous test runs
cleanup_previous_runs() {
    echo -e "${YELLOW}🧹 Cleaning up previous test runs...${NC}"
    
    # Clean Gradle build directory
    if [ -d "$SCRIPT_DIR/build" ]; then
        rm -rf "$SCRIPT_DIR/build"
        echo -e "${GREEN}✅ Cleaned Gradle build directory${NC}"
    fi
    
    # Stop any running TestContainers
    echo -e "${YELLOW}🐳 Stopping any running TestContainers...${NC}"
    docker ps -q --filter "label=org.testcontainers" | xargs -r docker stop
    docker ps -a -q --filter "label=org.testcontainers" | xargs -r docker rm
    echo -e "${GREEN}✅ TestContainers cleaned up${NC}"
    
    # Clean up test networks
    docker network ls --filter "name=*test*" -q | xargs -r docker network rm 2>/dev/null || true
    echo -e "${GREEN}✅ Test networks cleaned up${NC}"
    
    echo
}

# Function to set up test environment
setup_test_environment() {
    echo -e "${YELLOW}⚙️  Setting up test environment...${NC}"
    
    # Export environment variables
    export SPRING_PROFILES_ACTIVE="$TEST_PROFILE"
    export TESTCONTAINERS_REUSE_ENABLE=false
    export GRADLE_OPTS="-Xmx$MAX_HEAP_SIZE -XX:MaxMetaspaceSize=1g"
    export JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport"
    
    echo -e "${GREEN}✅ Environment variables set${NC}"
    echo -e "   SPRING_PROFILES_ACTIVE: $SPRING_PROFILES_ACTIVE"
    echo -e "   GRADLE_OPTS: $GRADLE_OPTS"
    echo
}

# Function to run specific test category
run_test_category() {
    local category=$1
    local display_name=$2
    
    echo -e "${BLUE}🧪 Running $display_name Tests${NC}"
    echo -e "${BLUE}$(printf '=%.0s' {1..50})${NC}"
    
    cd "$SCRIPT_DIR"
    
    case $category in
        "all")
            ./gradlew test --info --continue
            ;;
        "cross-service")
            ./gradlew crossServiceIntegrationTest --info --continue
            ;;
        "performance")
            ./gradlew performanceIntegrationTest --info --continue
            ;;
        "data-flow")
            ./gradlew test --tests "*DataFlow*" --info --continue
            ;;
        "tdd")
            ./gradlew test --tests "*_ShouldFail" --info --continue
            ;;
        *)
            ./gradlew test --tests "*$category*" --info --continue
            ;;
    esac
}

# Function to generate test reports
generate_reports() {
    echo -e "${YELLOW}📊 Generating test reports...${NC}"
    
    cd "$SCRIPT_DIR"
    
    # Generate JaCoCo coverage report
    ./gradlew jacocoTestReport
    
    # Generate test report
    ./gradlew test --continue || true
    
    echo -e "${GREEN}✅ Reports generated${NC}"
    
    # Display report locations
    if [ -f "$SCRIPT_DIR/build/reports/tests/test/index.html" ]; then
        echo -e "${BLUE}📄 Test Report: file://$SCRIPT_DIR/build/reports/tests/test/index.html${NC}"
    fi
    
    if [ -f "$SCRIPT_DIR/build/reports/jacoco/test/html/index.html" ]; then
        echo -e "${BLUE}📄 Coverage Report: file://$SCRIPT_DIR/build/reports/jacoco/test/html/index.html${NC}"
    fi
    
    echo
}

# Function to validate TDD approach
validate_tdd_approach() {
    echo -e "${YELLOW}🔍 Validating TDD approach...${NC}"
    
    cd "$SCRIPT_DIR"
    
    # Find failing tests (should have _ShouldFail suffix)
    FAILING_TESTS=$(find src/test/java -name "*.java" -exec grep -l "_ShouldFail" {} \; | wc -l)
    
    if [ "$FAILING_TESTS" -gt 0 ]; then
        echo -e "${GREEN}✅ Found $FAILING_TESTS TDD failing tests${NC}"
    else
        echo -e "${YELLOW}⚠️  No TDD failing tests found${NC}"
    fi
    
    # Check for TDD comments in tests
    TDD_COMMENTS=$(find src/test/java -name "*.java" -exec grep -l "STEP 1.*Write failing test first" {} \; | wc -l)
    
    if [ "$TDD_COMMENTS" -gt 0 ]; then
        echo -e "${GREEN}✅ Found TDD approach documentation in $TDD_COMMENTS files${NC}"
    else
        echo -e "${YELLOW}⚠️  TDD documentation could be improved${NC}"
    fi
    
    echo
}

# Function to check test coverage
check_coverage() {
    echo -e "${YELLOW}📈 Checking test coverage...${NC}"
    
    cd "$SCRIPT_DIR"
    
    if [ -f "build/reports/jacoco/test/jacocoTestReport.xml" ]; then
        # Parse coverage from XML report
        COVERAGE=$(grep -oP 'covered="\K[^"]*' build/reports/jacoco/test/jacocoTestReport.xml | head -1)
        TOTAL=$(grep -oP 'missed="\K[^"]*' build/reports/jacoco/test/jacocoTestReport.xml | head -1)
        
        if [ -n "$COVERAGE" ] && [ -n "$TOTAL" ]; then
            PERCENTAGE=$((COVERAGE * 100 / (COVERAGE + TOTAL)))
            
            if [ "$PERCENTAGE" -ge "$COVERAGE_THRESHOLD" ]; then
                echo -e "${GREEN}✅ Test coverage: $PERCENTAGE% (threshold: $COVERAGE_THRESHOLD%)${NC}"
            else
                echo -e "${RED}❌ Test coverage: $PERCENTAGE% below threshold: $COVERAGE_THRESHOLD%${NC}"
            fi
        else
            echo -e "${YELLOW}⚠️  Coverage data not available${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️  Coverage report not found${NC}"
    fi
    
    echo
}

# Function to display test statistics
display_statistics() {
    echo -e "${BLUE}📊 Test Statistics${NC}"
    echo -e "${BLUE}=================${NC}"
    
    cd "$SCRIPT_DIR"
    
    # Count test files
    TEST_FILES=$(find src/test/java -name "*Test.java" | wc -l)
    echo -e "${GREEN}Test Files: $TEST_FILES${NC}"
    
    # Count test methods
    TEST_METHODS=$(find src/test/java -name "*.java" -exec grep -c "@Test" {} \; | awk '{sum += $1} END {print sum}')
    echo -e "${GREEN}Test Methods: $TEST_METHODS${NC}"
    
    # Count TDD tests
    TDD_TESTS=$(find src/test/java -name "*.java" -exec grep -c "_ShouldFail" {} \; | awk '{sum += $1} END {print sum}')
    echo -e "${GREEN}TDD Failing Tests: $TDD_TESTS${NC}"
    
    # Count integration test classes
    INTEGRATION_TESTS=$(find src/test/java -name "*IntegrationTest.java" | wc -l)
    echo -e "${GREEN}Integration Test Classes: $INTEGRATION_TESTS${NC}"
    
    # Count utility classes
    UTIL_FILES=$(find src/test/java -name "*Factory.java" -o -name "*Utils.java" -o -name "Abstract*.java" | wc -l)
    echo -e "${GREEN}Utility Classes: $UTIL_FILES${NC}"
    
    echo
}

# Function to run performance benchmarks
run_performance_benchmarks() {
    echo -e "${YELLOW}⚡ Running performance benchmarks...${NC}"
    
    cd "$SCRIPT_DIR"
    
    # Run performance tests with timing
    start_time=$(date +%s)
    ./gradlew performanceIntegrationTest --info
    end_time=$(date +%s)
    
    duration=$((end_time - start_time))
    echo -e "${GREEN}✅ Performance tests completed in ${duration}s${NC}"
    
    # Check if performance results are available
    if [ -f "build/test-results/test/TEST-com.focushive.integration.EventOrderingIntegrationTest.xml" ]; then
        echo -e "${GREEN}✅ Performance metrics collected${NC}"
    fi
    
    echo
}

# Main execution function
main() {
    echo -e "${GREEN}Starting FocusHive Integration Tests...${NC}"
    echo -e "${GREEN}Time: $(date)${NC}"
    echo
    
    # Check command line arguments
    TEST_CATEGORY=${1:-"all"}
    SKIP_CLEANUP=${2:-false}
    
    # Execute test pipeline
    check_prerequisites
    
    if [ "$SKIP_CLEANUP" != "true" ]; then
        cleanup_previous_runs
    fi
    
    setup_test_environment
    validate_tdd_approach
    display_statistics
    
    # Run tests based on category
    case $TEST_CATEGORY in
        "all")
            run_test_category "all" "All Integration"
            ;;
        "tdd")
            run_test_category "tdd" "TDD Failing"
            ;;
        "performance")
            run_performance_benchmarks
            ;;
        "analytics")
            run_test_category "UserAnalyticsFlowIntegrationTest" "User Analytics Flow"
            ;;
        "notifications")
            run_test_category "HiveNotificationFlowIntegrationTest" "Hive Notification Flow"
            ;;
        "buddy")
            run_test_category "BuddyAnalyticsFlowIntegrationTest" "Buddy Analytics Flow"
            ;;
        "consistency")
            run_test_category "DataConsistencyIntegrationTest" "Data Consistency"
            ;;
        "events")
            run_test_category "EventOrderingIntegrationTest" "Event Ordering"
            ;;
        *)
            echo -e "${RED}❌ Unknown test category: $TEST_CATEGORY${NC}"
            echo -e "${YELLOW}Available categories: all, tdd, performance, analytics, notifications, buddy, consistency, events${NC}"
            exit 1
            ;;
    esac
    
    # Generate reports
    generate_reports
    check_coverage
    
    echo -e "${GREEN}🎉 Integration tests completed successfully!${NC}"
    echo -e "${GREEN}Time: $(date)${NC}"
}

# Help function
show_help() {
    echo -e "${BLUE}FocusHive Cross-Service Integration Tests Runner${NC}"
    echo
    echo -e "${YELLOW}Usage:${NC}"
    echo "  $0 [CATEGORY] [SKIP_CLEANUP]"
    echo
    echo -e "${YELLOW}Categories:${NC}"
    echo "  all           - Run all integration tests (default)"
    echo "  tdd           - Run TDD failing tests only"
    echo "  performance   - Run performance benchmarks"
    echo "  analytics     - Run user analytics flow tests"
    echo "  notifications - Run notification flow tests"
    echo "  buddy         - Run buddy system tests"
    echo "  consistency   - Run data consistency tests"
    echo "  events        - Run event ordering tests"
    echo
    echo -e "${YELLOW}Options:${NC}"
    echo "  SKIP_CLEANUP  - Set to 'true' to skip cleanup (default: false)"
    echo
    echo -e "${YELLOW}Examples:${NC}"
    echo "  $0                    # Run all tests"
    echo "  $0 tdd                # Run TDD failing tests"
    echo "  $0 performance        # Run performance tests"
    echo "  $0 analytics true     # Run analytics tests without cleanup"
}

# Check for help flag
if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    show_help
    exit 0
fi

# Execute main function
main "$@"