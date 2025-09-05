#!/bin/bash

# Run FocusHive services locally (without Docker)

echo "🚀 Starting FocusHive services locally..."

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if PostgreSQL and Redis are installed
if ! command -v psql &> /dev/null; then
    echo -e "${RED}❌ PostgreSQL is not installed. Please install it first.${NC}"
    echo "Install with: brew install postgresql"
    exit 1
fi

if ! command -v redis-server &> /dev/null; then
    echo -e "${RED}❌ Redis is not installed. Please install it first.${NC}"
    echo "Install with: brew install redis"
    exit 1
fi

# Start Redis if not running
if ! pgrep -x "redis-server" > /dev/null; then
    echo -e "${YELLOW}Starting Redis...${NC}"
    redis-server --daemonize yes --requirepass focushive_pass
else
    echo -e "${GREEN}✅ Redis is already running${NC}"
fi

# Create databases if they don't exist
echo -e "${YELLOW}Setting up databases...${NC}"
createdb focushive 2>/dev/null || echo "Database 'focushive' already exists"
createdb identity_db 2>/dev/null || echo "Database 'identity_db' already exists"

# Export environment variables for identity service
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=identity_db
export DB_USER=$USER
export DB_PASSWORD=""
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=focushive_pass
export KEY_STORE_PASSWORD=changeme
export PRIVATE_KEY_PASSWORD=changeme
export FOCUSHIVE_CLIENT_SECRET=secret
export SERVER_PORT=8081
export LOG_LEVEL=INFO
export SHOW_SQL=false
export CORS_ORIGINS="http://localhost:3000,http://localhost:5173,http://localhost:8080"

# Start Identity Service
echo -e "${YELLOW}Starting Identity Service on port 8081...${NC}"
cd services/identity-service
nohup java -jar build/libs/identity-service.jar > ../../logs/identity-service.log 2>&1 &
IDENTITY_PID=$!
echo "Identity Service PID: $IDENTITY_PID"
cd ../..

# Wait for Identity Service to start
echo -e "${YELLOW}Waiting for Identity Service to start...${NC}"
sleep 10

# Export environment variables for main backend
export DB_NAME=focushive
export IDENTITY_SERVICE_URL=http://localhost:8081
export SERVER_PORT=8080
export JWT_SECRET=your-super-secret-jwt-key-change-in-production
export JWT_EXPIRATION=86400000

# Start Main Backend
echo -e "${YELLOW}Starting Main Backend on port 8080...${NC}"
cd services/focushive-backend
nohup java -jar build/libs/focushive-backend-0.0.1-SNAPSHOT.jar > ../../logs/backend.log 2>&1 &
BACKEND_PID=$!
echo "Backend PID: $BACKEND_PID"
cd ../..

# Save PIDs for shutdown
echo $IDENTITY_PID > .identity.pid
echo $BACKEND_PID > .backend.pid

echo -e "\n${GREEN}✅ Services started successfully!${NC}"
echo -e "\nAccess points:"
echo -e "  - Backend API: ${GREEN}http://192.168.2.3:8080${NC}"
echo -e "  - Identity Service: ${GREEN}http://192.168.2.3:8081${NC}"
echo -e "  - Swagger UI: ${GREEN}http://192.168.2.3:8080/swagger-ui.html${NC}"
echo -e "\nLogs:"
echo -e "  - Identity Service: ${YELLOW}tail -f logs/identity-service.log${NC}"
echo -e "  - Backend: ${YELLOW}tail -f logs/backend.log${NC}"
echo -e "\nTo stop services:"
echo -e "  ${YELLOW}./stop-local.sh${NC}"