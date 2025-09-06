#!/bin/bash

# FocusHive Local Services Stop Script
# Stops all locally running services started by start-local-no-nginx.sh

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"
PIDS_FILE="$PROJECT_ROOT/.local-services.pids"

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}   Stopping FocusHive Local Services${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

# Stop services from PID file
if [ -f "$PIDS_FILE" ]; then
    echo -e "${YELLOW}Stopping application services...${NC}"
    while IFS=':' read -r service pid; do
        if kill -0 "$pid" 2>/dev/null; then
            echo "  Stopping $service (PID: $pid)"
            kill "$pid" 2>/dev/null || true
            
            # Wait for process to stop
            timeout=10
            while kill -0 "$pid" 2>/dev/null && [ $timeout -gt 0 ]; do
                sleep 1
                timeout=$((timeout - 1))
            done
            
            # Force kill if still running
            if kill -0 "$pid" 2>/dev/null; then
                echo "  Force stopping $service"
                kill -9 "$pid" 2>/dev/null || true
            fi
        else
            echo "  $service already stopped"
        fi
    done < "$PIDS_FILE"
    rm "$PIDS_FILE"
    echo -e "${GREEN}Application services stopped${NC}"
else
    echo -e "${YELLOW}No PID file found. Services may not be running.${NC}"
fi

# Stop Redis containers
echo -e "${YELLOW}Stopping Redis containers...${NC}"
docker stop focushive-redis 2>/dev/null && echo "  Main Redis stopped" || echo "  Main Redis not running"
docker stop focushive-identity-redis 2>/dev/null && echo "  Identity Redis stopped" || echo "  Identity Redis not running"

# Optional: Kill any remaining Java processes (be careful with this!)
echo ""
read -p "Kill all remaining Gradle/Java processes? (y/N): " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Stopping all Gradle processes...${NC}"
    pkill -f gradlew 2>/dev/null || true
    pkill -f "gradle.*bootRun" 2>/dev/null || true
    echo -e "${GREEN}Gradle processes stopped${NC}"
fi

# Clean up logs (optional)
echo ""
read -p "Clean up log files? (y/N): " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Cleaning up logs...${NC}"
    rm -rf "$PROJECT_ROOT/logs"
    echo -e "${GREEN}Logs cleaned${NC}"
fi

echo ""
echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}   All services stopped successfully!${NC}"
echo -e "${GREEN}================================================${NC}"