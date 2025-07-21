#!/bin/bash

# Script to run GitHub Actions locally using Docker

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"

cd "$SCRIPT_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}FocusHive GitHub Actions Local Runner${NC}"
echo "======================================"
echo ""

# Function to display usage
usage() {
    echo "Usage: $0 [command] [options]"
    echo ""
    echo "Commands:"
    echo "  build    - Build the GitHub Actions runner container"
    echo "  run      - Run a specific workflow"
    echo "  shell    - Start an interactive shell in the runner container"
    echo "  test     - Run all tests locally"
    echo "  clean    - Clean up containers and volumes"
    echo ""
    echo "Options:"
    echo "  -w, --workflow <name>    - Specify workflow file (default: ci.yml)"
    echo "  -j, --job <name>        - Run specific job from workflow"
    echo "  -h, --help              - Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 build                        # Build the runner container"
    echo "  $0 run                          # Run default CI workflow"
    echo "  $0 run -w ci.yml -j backend-test # Run specific job"
    echo "  $0 shell                        # Interactive shell"
    echo ""
}

# Parse command
COMMAND=${1:-help}
shift || true

# Default values
WORKFLOW="ci.yml"
JOB=""
ACT_OPTIONS=""

# Parse options
while [[ $# -gt 0 ]]; do
    case $1 in
        -w|--workflow)
            WORKFLOW="$2"
            shift 2
            ;;
        -j|--job)
            JOB="$2"
            ACT_OPTIONS="$ACT_OPTIONS -j $JOB"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            usage
            exit 1
            ;;
    esac
done

# Execute command
case $COMMAND in
    build)
        echo -e "${YELLOW}Building GitHub Actions runner container...${NC}"
        docker-compose build github-runner
        echo -e "${GREEN}Build complete!${NC}"
        ;;
        
    run)
        echo -e "${YELLOW}Running workflow: $WORKFLOW${NC}"
        if [ -n "$JOB" ]; then
            echo -e "${YELLOW}Running specific job: $JOB${NC}"
        fi
        
        # Check if workflow exists
        if [ ! -f "$PROJECT_ROOT/.github/workflows/$WORKFLOW" ]; then
            echo -e "${RED}Workflow file not found: $WORKFLOW${NC}"
            echo "Available workflows:"
            ls -1 "$PROJECT_ROOT/.github/workflows/"
            exit 1
        fi
        
        # Run with act
        docker-compose run --rm \
            -e WORKFLOW="$WORKFLOW" \
            -e ACT_OPTIONS="$ACT_OPTIONS" \
            github-runner
        ;;
        
    shell)
        echo -e "${YELLOW}Starting interactive shell...${NC}"
        docker-compose run --rm \
            -e WORKFLOW="" \
            github-runner
        ;;
        
    test)
        echo -e "${YELLOW}Running all tests locally...${NC}"
        
        # Run backend tests
        echo -e "\n${GREEN}Running Backend Tests...${NC}"
        docker-compose run --rm \
            -e WORKFLOW="ci.yml" \
            -e ACT_OPTIONS="-j backend-test" \
            github-runner
            
        # Run identity service tests
        echo -e "\n${GREEN}Running Identity Service Tests...${NC}"
        docker-compose run --rm \
            -e WORKFLOW="ci.yml" \
            -e ACT_OPTIONS="-j identity-service-test" \
            github-runner
            
        # Run frontend tests
        echo -e "\n${GREEN}Running Frontend Tests...${NC}"
        docker-compose run --rm \
            -e WORKFLOW="ci.yml" \
            -e ACT_OPTIONS="-j frontend-test" \
            github-runner
            
        echo -e "\n${GREEN}All tests complete!${NC}"
        ;;
        
    clean)
        echo -e "${YELLOW}Cleaning up containers and volumes...${NC}"
        docker-compose down -v
        echo -e "${GREEN}Cleanup complete!${NC}"
        ;;
        
    *)
        usage
        exit 0
        ;;
esac