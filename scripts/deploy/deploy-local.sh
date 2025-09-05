#!/bin/bash

# Deploy FocusHive to local Docker

echo "🚀 Deploying FocusHive to local Docker..."

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Navigate to the project root
cd "$(dirname "$0")/../.."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker is not running. Please start Docker and try again.${NC}"
    exit 1
fi

# Clean up old containers and volumes (optional)
echo -e "${YELLOW}🧹 Cleaning up old containers...${NC}"
docker compose -f docker/docker-compose.yml down

# Build the JAR files first
echo -e "${YELLOW}📦 Building backend services...${NC}"

# Build identity service
echo "Building Identity Service..."
cd services/identity-service
./gradlew clean build -x test
cd ../..

# Build main backend
echo "Building Main Backend..."
cd services/focushive-backend
./gradlew clean build -x test
cd ../..

# Start all services
echo -e "${YELLOW}🐳 Starting Docker containers...${NC}"
docker compose -f docker/docker-compose.yml up -d --build

# Wait for services to be healthy
echo -e "${YELLOW}⏳ Waiting for services to be healthy...${NC}"

# Function to check if a service is healthy
check_health() {
    local service=$1
    local max_attempts=30
    local attempt=0
    
    while [ $attempt -lt $max_attempts ]; do
        if docker compose -f docker/docker-compose.yml ps | grep $service | grep -q "healthy"; then
            echo -e "${GREEN}✅ $service is healthy${NC}"
            return 0
        fi
        
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    echo -e "${RED}❌ $service failed to become healthy${NC}"
    return 1
}

# Check health of critical services
check_health "focushive-db"
check_health "focushive-redis"
check_health "identity-db"
check_health "identity-redis"

# Show running services
echo -e "\n${GREEN}✅ Deployment complete!${NC}"
echo -e "\n📊 Running services:"
docker compose -f docker/docker-compose.yml ps

echo -e "\n${GREEN}🎉 FocusHive is now running!${NC}"
echo -e "\nAccess points:"
echo -e "  - Frontend: ${GREEN}http://192.168.2.3:5173${NC}"
echo -e "  - Backend API: ${GREEN}http://192.168.2.3:8080${NC}"
echo -e "  - Identity Service: ${GREEN}http://192.168.2.3:8081${NC}"
echo -e "  - PostgreSQL (Main): ${GREEN}192.168.2.3:5432${NC}"
echo -e "  - PostgreSQL (Identity): ${GREEN}192.168.2.3:5433${NC}"
echo -e "  - Redis (Main): ${GREEN}192.168.2.3:6379${NC}"
echo -e "  - Redis (Identity): ${GREEN}192.168.2.3:6380${NC}"

echo -e "\nUseful commands:"
echo -e "  - View logs: ${YELLOW}docker compose -f docker/docker-compose.yml logs -f [service-name]${NC}"
echo -e "  - Stop all: ${YELLOW}docker compose -f docker/docker-compose.yml down${NC}"
echo -e "  - Restart a service: ${YELLOW}docker compose -f docker/docker-compose.yml restart [service-name]${NC}"