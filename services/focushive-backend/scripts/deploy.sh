#!/bin/bash

# FocusHive Backend Deployment Script
# Usage: ./scripts/deploy.sh [dev|staging|production]

set -e

ENVIRONMENT=${1:-dev}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
    exit 1
}

# Validate environment
case $ENVIRONMENT in
    dev|development)
        ENVIRONMENT="dev"
        COMPOSE_FILE="docker-compose.yml"
        ;;
    staging)
        ENVIRONMENT="staging"
        COMPOSE_FILE="docker-compose.yml -f docker-compose.prod.yml"
        ;;
    prod|production)
        ENVIRONMENT="production"
        COMPOSE_FILE="docker-compose.yml -f docker-compose.prod.yml"
        ;;
    *)
        error "Invalid environment: $ENVIRONMENT. Use: dev, staging, or production"
        ;;
esac

log "🚀 Starting deployment for environment: $ENVIRONMENT"

# Change to project root
cd "$PROJECT_ROOT"

# Check if .env file exists
if [ ! -f .env ]; then
    warn ".env file not found, creating from .env.example"
    cp .env.example .env
    warn "Please edit .env file with your configuration before proceeding"
    read -p "Continue? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Pre-deployment checks
log "📋 Running pre-deployment checks..."

# Check Docker is running
if ! docker info > /dev/null 2>&1; then
    error "Docker is not running. Please start Docker and try again."
fi

# Check Docker Compose is available
if ! command -v docker-compose > /dev/null 2>&1; then
    error "docker-compose is not installed or not in PATH"
fi

# Check if ports are available
check_port() {
    local port=$1
    local service=$2
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null ; then
        warn "Port $port is already in use (required for $service)"
        read -p "Continue anyway? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
}

log "🔍 Checking port availability..."
check_port 8080 "FocusHive Backend"
check_port 5432 "PostgreSQL"
check_port 6379 "Redis"

# Create necessary directories
log "📁 Creating directories..."
mkdir -p data/postgres logs ssl nginx/conf.d monitoring/grafana/{dashboards,datasources}

# Build application
log "🏗️  Building application..."
if [ "$ENVIRONMENT" = "production" ]; then
    ./gradlew clean build -x test --no-daemon
else
    ./gradlew clean build --no-daemon
fi

# Build Docker images
log "🐳 Building Docker images..."
docker-compose -f $COMPOSE_FILE build --no-cache

# Stop existing containers
log "🛑 Stopping existing containers..."
docker-compose -f $COMPOSE_FILE down --remove-orphans

# Start services
log "🚀 Starting services..."
if [ "$ENVIRONMENT" = "production" ]; then
    docker-compose -f $COMPOSE_FILE --profile monitoring up -d
else
    docker-compose -f $COMPOSE_FILE up -d
fi

# Wait for services to be healthy
log "⏳ Waiting for services to be ready..."
sleep 30

# Health checks
log "🏥 Running health checks..."
health_check() {
    local service=$1
    local url=$2
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if curl -f -s "$url" > /dev/null 2>&1; then
            log "✅ $service is healthy"
            return 0
        fi
        log "⏳ Waiting for $service to be ready (attempt $attempt/$max_attempts)..."
        sleep 10
        ((attempt++))
    done

    error "❌ $service failed health check after $max_attempts attempts"
}

health_check "FocusHive Backend" "http://localhost:8080/actuator/health"

# Show running services
log "📊 Services status:"
docker-compose -f $COMPOSE_FILE ps

# Show logs
log "📄 Recent logs:"
docker-compose -f $COMPOSE_FILE logs --tail=20 focushive-backend

# Final information
log "🎉 Deployment completed successfully!"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}Environment:${NC} $ENVIRONMENT"
echo -e "${GREEN}FocusHive Backend:${NC} http://localhost:8080"
echo -e "${GREEN}API Documentation:${NC} http://localhost:8080/swagger-ui.html"
echo -e "${GREEN}Health Check:${NC} http://localhost:8080/actuator/health"

if [ "$ENVIRONMENT" = "production" ]; then
    echo -e "${GREEN}Grafana Dashboard:${NC} http://localhost:3000"
    echo -e "${GREEN}Prometheus:${NC} http://localhost:9090"
fi

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# Monitoring commands
echo -e "${YELLOW}Useful commands:${NC}"
echo "  View logs: docker-compose -f $COMPOSE_FILE logs -f focushive-backend"
echo "  Stop services: docker-compose -f $COMPOSE_FILE down"
echo "  Restart backend: docker-compose -f $COMPOSE_FILE restart focushive-backend"
echo "  Database shell: docker-compose -f $COMPOSE_FILE exec postgres psql -U focushive -d focushive"
echo "  Redis CLI: docker-compose -f $COMPOSE_FILE exec redis redis-cli"