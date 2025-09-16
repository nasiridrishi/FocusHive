#!/bin/bash

# JaCoCo Test Coverage Utility Script
# This script provides easy commands for running and viewing test coverage reports

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Project info
PROJECT_NAME="Buddy Service"
COVERAGE_THRESHOLD="95%"

print_header() {
    echo -e "\n${BLUE}================================"
    echo -e "$PROJECT_NAME - Coverage Utility"
    echo -e "Coverage Threshold: $COVERAGE_THRESHOLD"
    echo -e "================================${NC}\n"
}

print_usage() {
    echo -e "${YELLOW}Usage:${NC}"
    echo "  ./coverage.sh [command]"
    echo ""
    echo -e "${YELLOW}Commands:${NC}"
    echo "  test      - Run all tests and generate coverage report"
    echo "  check     - Check coverage thresholds (fails build on violation)"
    echo "  quiet     - Check coverage without failing (for development)"
    echo "  report    - Open HTML coverage report in browser"
    echo "  clean     - Clean coverage reports"
    echo "  summary   - Show coverage summary from last run"
    echo "  help      - Show this help message"
}

run_tests() {
    echo -e "${BLUE}Running tests and generating coverage report...${NC}"
    ./gradlew clean testCoverage
    echo -e "\n${GREEN}✅ Tests completed and coverage report generated!${NC}"
    show_coverage_summary
}

check_coverage() {
    echo -e "${BLUE}Checking coverage thresholds...${NC}"
    ./gradlew jacocoTestCoverageVerification
    echo -e "\n${GREEN}✅ Coverage check passed! All thresholds met.${NC}"
}

check_coverage_quiet() {
    echo -e "${BLUE}Checking coverage (development mode)...${NC}"
    ./gradlew checkCoverageQuiet
}

open_report() {
    REPORT_PATH="build/reports/jacoco/test/html/index.html"
    if [[ -f "$REPORT_PATH" ]]; then
        echo -e "${GREEN}Opening coverage report in browser...${NC}"
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS
            open "$REPORT_PATH"
        elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
            # Linux
            xdg-open "$REPORT_PATH"
        else
            echo -e "${YELLOW}Please open manually: file://$(pwd)/$REPORT_PATH${NC}"
        fi
    else
        echo -e "${RED}❌ Coverage report not found. Run './coverage.sh test' first.${NC}"
        exit 1
    fi
}

clean_reports() {
    echo -e "${BLUE}Cleaning coverage reports...${NC}"
    ./gradlew clean
    echo -e "${GREEN}✅ Coverage reports cleaned.${NC}"
}

show_coverage_summary() {
    REPORT_CSV="build/reports/jacoco/test/jacocoTestReport.csv"
    if [[ -f "$REPORT_CSV" ]]; then
        echo -e "\n${YELLOW}📊 Coverage Summary:${NC}"
        echo "----------------------------------------"
        
        # Parse CSV and show summary
        while IFS=, read -r group package class instruction_missed instruction_covered branch_missed branch_covered line_missed line_covered complexity_missed complexity_covered method_missed method_covered; do
            if [[ "$group" != "GROUP" ]]; then  # Skip header
                instruction_total=$((instruction_missed + instruction_covered))
                line_total=$((line_missed + line_covered))
                
                if [[ $instruction_total -gt 0 ]] && [[ $line_total -gt 0 ]]; then
                    instruction_percent=$(awk "BEGIN {printf \"%.1f\", ($instruction_covered/$instruction_total)*100}")
                    line_percent=$(awk "BEGIN {printf \"%.1f\", ($line_covered/$line_total)*100}")
                    
                    echo -e "Instructions: ${instruction_percent}% (${instruction_covered}/${instruction_total})"
                    echo -e "Lines: ${line_percent}% (${line_covered}/${line_total})"
                    break
                fi
            fi
        done < "$REPORT_CSV"
        
        echo "----------------------------------------"
        echo -e "📁 Full report: ${BLUE}file://$(pwd)/build/reports/jacoco/test/html/index.html${NC}"
    else
        echo -e "${YELLOW}No coverage summary available. Run tests first.${NC}"
    fi
}

# Main script logic
case "${1:-help}" in
    "test")
        print_header
        run_tests
        ;;
    "check")
        print_header
        check_coverage
        ;;
    "quiet")
        print_header
        check_coverage_quiet
        ;;
    "report")
        print_header
        open_report
        ;;
    "clean")
        print_header
        clean_reports
        ;;
    "summary")
        print_header
        show_coverage_summary
        ;;
    "help"|*)
        print_header
        print_usage
        ;;
esac