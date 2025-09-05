#!/bin/bash
# Deploy FocusHive Backend Services with Internal-Only Networking

set -e

echo "Starting FocusHive Backend Deployment..."

# Change to docker directory
cd "$(dirname "$0")"

# Function to check if a service is healthy
check_service_health() {
    local service_name="$1"
    local timeout="${2:-60}"
    local count=0
    
    echo "Waiting for $service_name to be healthy..."
    while [ $count -lt $timeout ]; do
        if docker inspect --format='{{.State.Health.Status}}' "$service_name" 2>/dev/null | grep -q "healthy"; then
            echo "✅ $service_name is healthy"
            return 0
        fi
        sleep 2
        count=$((count + 2))
        echo "  Waiting... (${count}s/${timeout}s)"
    done
    echo "❌ $service_name failed to become healthy within ${timeout}s"
    return 1
}

# Function to check if a service is running
check_service_running() {
    local service_name="$1"
    if docker ps --format '{{.Names}}' | grep -q "^${service_name}$"; then
        echo "✅ $service_name is running"
        return 0
    else
        echo "❌ $service_name is not running"
        return 1
    fi
}

# Function to show service logs
show_service_logs() {
    local service_name="$1"
    echo "Recent logs for $service_name:"
    docker logs "$service_name" --tail 10 2>/dev/null || echo "  No logs available"
    echo ""
}

# Stop any existing containers
echo "Stopping existing backend services..."
docker compose -f docker-compose.backend-internal.yml down 2>/dev/null || true

# Clean up any orphaned containers
echo "Cleaning up orphaned containers..."
docker system prune -f >/dev/null 2>&1 || true

# Build fresh images
echo "Building Docker images..."
docker compose -f docker-compose.backend-internal.yml build --no-cache

# Start infrastructure services first (databases and Redis)
echo "Starting infrastructure services..."
docker compose -f docker-compose.backend-internal.yml up -d db redis identity-db identity-redis

# Wait for databases to be healthy
echo "Verifying database health..."
check_service_health "focushive-db" 60 || exit 1
check_service_health "focushive-redis" 30 || exit 1
check_service_health "identity-db" 60 || exit 1
check_service_health "identity-redis" 30 || exit 1

# Start identity service
echo "Starting Identity Service..."
docker compose -f docker-compose.backend-internal.yml up -d identity-service

# Wait for identity service to be healthy
check_service_health "identity-service" 90 || exit 1

# Start main backend service
echo "Starting FocusHive Backend..."
docker compose -f docker-compose.backend-internal.yml up -d backend

# Wait for backend to be healthy
check_service_health "backend" 120 || exit 1

# Verify all services are running
echo ""
echo "Verifying all services..."
check_service_running "focushive-db" || exit 1
check_service_running "focushive-redis" || exit 1
check_service_running "identity-db" || exit 1
check_service_running "identity-redis" || exit 1
check_service_running "identity-service" || exit 1
check_service_running "backend" || exit 1

# Show container status
echo ""
echo "Container Status:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "(focushive|identity|backend)"

# Show network information
echo ""
echo "Network Information:"
docker network ls | grep focushive-network
echo ""
echo "Services in focushive-network:"
docker network inspect focushive-network --format '{{range .Containers}}{{.Name}}: {{.IPv4Address}}{{println}}{{end}}' 2>/dev/null || echo "Network not found or empty"

# Test internal connectivity
echo ""
echo "Testing Internal Connectivity:"
echo "Testing backend health endpoint..."
if docker exec backend wget -q -O - http://localhost:8080/actuator/health 2>/dev/null; then
    echo "✅ Backend health endpoint accessible internally"
else
    echo "❌ Backend health endpoint not accessible"
fi

echo "Testing identity service health endpoint..."
if docker exec identity-service wget -q -O - http://localhost:8081/actuator/health 2>/dev/null; then
    echo "✅ Identity service health endpoint accessible internally"
else
    echo "❌ Identity service health endpoint not accessible"
fi

# Show recent logs for troubleshooting
echo ""
echo "Recent Service Logs:"
show_service_logs "backend"
show_service_logs "identity-service"

echo "========================================="
echo "✅ Backend Deployment Complete!"
echo "========================================="
echo ""
echo "Internal Service URLs (Docker network only):"
echo "  Backend:          http://backend:8080"
echo "  Identity Service: http://identity-service:8081"
echo "  PostgreSQL:       postgresql://focushive-db:5432/focushive"
echo "  Redis:            redis://focushive-redis:6379"
echo ""
echo "⚠️  ZERO-TRUST ARCHITECTURE ENABLED:"
echo "   - No ports exposed to host system"
echo "   - All communication via internal Docker network"
echo "   - External access only via Cloudflare tunnel"
echo ""
echo "Cloudflare Tunnel Routing:"
echo "  dev.focushive.app/api → backend:8080"
echo ""
echo "Management Commands:"
echo "  View logs:    docker logs <service-name>"
echo "  Stop all:     docker compose -f docker-compose.backend-internal.yml down"
echo "  Restart:      docker compose -f docker-compose.backend-internal.yml restart <service>"
echo "  Shell access: docker exec -it <service-name> /bin/sh"
echo ""
echo "Health Check:"
echo "  docker exec backend wget -q -O - http://localhost:8080/actuator/health"
echo "  docker exec identity-service wget -q -O - http://localhost:8081/actuator/health"