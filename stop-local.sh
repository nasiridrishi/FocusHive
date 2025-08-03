#!/bin/bash

# Stop FocusHive services

echo "🛑 Stopping FocusHive services..."

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Stop services using saved PIDs
if [ -f .identity.pid ]; then
    IDENTITY_PID=$(cat .identity.pid)
    if ps -p $IDENTITY_PID > /dev/null; then
        echo -e "${YELLOW}Stopping Identity Service (PID: $IDENTITY_PID)...${NC}"
        kill $IDENTITY_PID
        rm .identity.pid
    else
        echo -e "${RED}Identity Service not running${NC}"
        rm .identity.pid
    fi
else
    echo -e "${RED}Identity Service PID file not found${NC}"
fi

if [ -f .backend.pid ]; then
    BACKEND_PID=$(cat .backend.pid)
    if ps -p $BACKEND_PID > /dev/null; then
        echo -e "${YELLOW}Stopping Backend Service (PID: $BACKEND_PID)...${NC}"
        kill $BACKEND_PID
        rm .backend.pid
    else
        echo -e "${RED}Backend Service not running${NC}"
        rm .backend.pid
    fi
else
    echo -e "${RED}Backend Service PID file not found${NC}"
fi

# Optionally stop Redis
read -p "Stop Redis server? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Stopping Redis...${NC}"
    redis-cli -a focushive_pass shutdown
fi

echo -e "${GREEN}✅ Services stopped${NC}"