#!/bin/bash

# Deploy FocusHive to remote Docker at 192.168.2.3

echo "🚀 Deploying FocusHive to remote Docker at 192.168.2.3..."

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Use remote docker context
docker context use remote-docker

# Create network
echo -e "${YELLOW}Creating Docker network...${NC}"
docker network create focushive-network 2>/dev/null || echo "Network already exists"

# Start databases
echo -e "${YELLOW}Starting databases...${NC}"

# Main PostgreSQL
docker run -d \
  --name focushive-db \
  --network focushive-network \
  -e POSTGRES_DB=focushive \
  -e POSTGRES_USER=focushive_user \
  -e POSTGRES_PASSWORD=focushive_pass \
  -p 5432:5432 \
  -v postgres_data:/var/lib/postgresql/data \
  --health-cmd="pg_isready -U focushive_user -d focushive" \
  --health-interval=10s \
  --health-timeout=5s \
  --health-retries=5 \
  postgres:16-alpine 2>/dev/null || echo "focushive-db already exists"

# Identity PostgreSQL
docker run -d \
  --name identity-db \
  --network focushive-network \
  -e POSTGRES_DB=identity_db \
  -e POSTGRES_USER=identity_user \
  -e POSTGRES_PASSWORD=identity_pass \
  -p 5433:5432 \
  -v identity_db_data:/var/lib/postgresql/data \
  --health-cmd="pg_isready -U identity_user -d identity_db" \
  --health-interval=10s \
  --health-timeout=5s \
  --health-retries=5 \
  postgres:16-alpine 2>/dev/null || echo "identity-db already exists"

# Start Redis instances
echo -e "${YELLOW}Starting Redis instances...${NC}"

# Main Redis
docker run -d \
  --name focushive-redis \
  --network focushive-network \
  -p 6379:6379 \
  -v redis_data:/data \
  --health-cmd="redis-cli -a focushive_pass ping" \
  --health-interval=10s \
  --health-timeout=5s \
  --health-retries=5 \
  redis:7-alpine redis-server --requirepass focushive_pass 2>/dev/null || echo "focushive-redis already exists"

# Identity Redis
docker run -d \
  --name identity-redis \
  --network focushive-network \
  -p 6380:6379 \
  -v identity_redis_data:/data \
  --health-cmd="redis-cli -a identity_redis_pass ping" \
  --health-interval=10s \
  --health-timeout=5s \
  --health-retries=5 \
  redis:7-alpine redis-server --requirepass identity_redis_pass 2>/dev/null || echo "identity-redis already exists"

# Wait for databases to be ready
echo -e "${YELLOW}Waiting for databases to be ready...${NC}"
sleep 15

# Build and run Identity Service
echo -e "${YELLOW}Building and running Identity Service...${NC}"
cd identity-service
docker build -t focushive/identity-service .
docker run -d \
  --name identity-service \
  --network focushive-network \
  -p 8081:8081 \
  -e DB_HOST=identity-db \
  -e DB_PORT=5432 \
  -e DB_NAME=identity_db \
  -e DB_USER=identity_user \
  -e DB_PASSWORD=identity_pass \
  -e REDIS_HOST=identity-redis \
  -e REDIS_PORT=6379 \
  -e REDIS_PASSWORD=identity_redis_pass \
  -e KEY_STORE_PASSWORD=changeme \
  -e PRIVATE_KEY_PASSWORD=changeme \
  -e FOCUSHIVE_CLIENT_SECRET=secret \
  -e CORS_ORIGINS="http://192.168.2.3:3000,http://192.168.2.3:5173,http://192.168.2.3:8080,http://localhost:3000,http://localhost:5173,http://localhost:8080" \
  -e SERVER_PORT=8081 \
  -e LOG_LEVEL=INFO \
  -e SHOW_SQL=false \
  focushive/identity-service 2>/dev/null || echo "identity-service already exists"
cd ..

# Build and run Main Backend
echo -e "${YELLOW}Building and running Main Backend...${NC}"
cd backend
docker build -t focushive/backend .
docker run -d \
  --name focushive-backend \
  --network focushive-network \
  -p 8080:8080 \
  -e DB_HOST=focushive-db \
  -e DB_PORT=5432 \
  -e DB_NAME=focushive \
  -e DB_USER=focushive_user \
  -e DB_PASSWORD=focushive_pass \
  -e REDIS_HOST=focushive-redis \
  -e REDIS_PORT=6379 \
  -e REDIS_PASSWORD=focushive_pass \
  -e IDENTITY_SERVICE_URL=http://identity-service:8081 \
  -e SERVER_PORT=8080 \
  -e JWT_SECRET=your-super-secret-jwt-key-change-in-production \
  -e JWT_EXPIRATION=86400000 \
  -e LOG_LEVEL=INFO \
  -e SHOW_SQL=false \
  focushive/backend 2>/dev/null || echo "focushive-backend already exists"
cd ..

echo -e "\n${GREEN}✅ Deployment complete!${NC}"
echo -e "\n📊 Running containers:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo -e "\n${GREEN}🎉 FocusHive is now running on remote Docker!${NC}"
echo -e "\nAccess points:"
echo -e "  - Backend API: ${GREEN}http://192.168.2.3:8080${NC}"
echo -e "  - Identity Service: ${GREEN}http://192.168.2.3:8081${NC}"
echo -e "  - PostgreSQL (Main): ${GREEN}192.168.2.3:5432${NC}"
echo -e "  - PostgreSQL (Identity): ${GREEN}192.168.2.3:5433${NC}"
echo -e "  - Redis (Main): ${GREEN}192.168.2.3:6379${NC}"
echo -e "  - Redis (Identity): ${GREEN}192.168.2.3:6380${NC}"

echo -e "\nUseful commands:"
echo -e "  - View logs: ${YELLOW}docker logs -f [container-name]${NC}"
echo -e "  - Stop all: ${YELLOW}docker stop focushive-backend identity-service focushive-redis identity-redis focushive-db identity-db${NC}"
echo -e "  - Remove all: ${YELLOW}docker rm focushive-backend identity-service focushive-redis identity-redis focushive-db identity-db${NC}"