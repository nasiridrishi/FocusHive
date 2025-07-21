#!/bin/bash

# Simple script to test GitHub Actions locally using act

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}FocusHive GitHub Actions Local Test${NC}"
echo "===================================="
echo ""

# Check if act is installed
if ! command -v act &> /dev/null; then
    echo -e "${RED}Error: 'act' is not installed${NC}"
    echo "Please install act: brew install act"
    exit 1
fi

# Default values
WORKFLOW=".github/workflows/ci.yml"
JOB=""
VERBOSE=""

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -w|--workflow)
            WORKFLOW=".github/workflows/$2"
            shift 2
            ;;
        -j|--job)
            JOB="-j $2"
            shift 2
            ;;
        -v|--verbose)
            VERBOSE="-v"
            shift
            ;;
        -l|--list)
            echo -e "${GREEN}Available workflows and jobs:${NC}"
            act -l
            exit 0
            ;;
        -h|--help)
            echo "Usage: $0 [options]"
            echo ""
            echo "Options:"
            echo "  -w, --workflow <file>  Workflow file name (default: ci.yml)"
            echo "  -j, --job <name>       Run specific job"
            echo "  -v, --verbose          Verbose output"
            echo "  -l, --list             List all workflows and jobs"
            echo "  -h, --help             Show this help"
            echo ""
            echo "Examples:"
            echo "  $0                     # Run all jobs in ci.yml"
            echo "  $0 -j backend-test     # Run only backend tests"
            echo "  $0 -j identity-service-test -v  # Run identity service tests with verbose"
            echo ""
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Check if workflow exists
if [ ! -f "$WORKFLOW" ]; then
    echo -e "${RED}Workflow not found: $WORKFLOW${NC}"
    echo ""
    echo "Available workflows:"
    ls -1 .github/workflows/
    exit 1
fi

echo -e "${YELLOW}Running workflow: $WORKFLOW${NC}"
if [ -n "$JOB" ]; then
    echo -e "${YELLOW}Running specific job: ${JOB#-j }${NC}"
fi
echo ""

# Create act environment file with required variables
cat > .act.env << EOF
SPRING_PROFILES_ACTIVE=test
NODE_ENV=test
CI=true
EOF

# Run act with appropriate settings
echo -e "${GREEN}Starting GitHub Actions runner...${NC}"
echo ""

act \
    -W "$WORKFLOW" \
    $JOB \
    $VERBOSE \
    --env-file .act.env \
    --container-architecture linux/amd64 \
    -P ubuntu-latest=catthehacker/ubuntu:act-latest \
    --pull=false

# Cleanup
rm -f .act.env

echo ""
echo -e "${GREEN}Test completed!${NC}"