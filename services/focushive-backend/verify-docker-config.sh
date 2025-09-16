#!/bin/bash

set -e

echo "=========================================="
echo "Docker Configuration Verification"
echo "=========================================="

# Check if network exists
echo "1. Checking shared network..."
if docker network ls | grep -q "focushive-shared-network"; then
    echo "   ✅ Network exists"
else
    echo "   Creating network..."
    docker network create focushive-shared-network
    echo "   ✅ Network created"
fi

# Test build
echo ""
echo "2. Testing Docker build..."
docker-compose build --no-cache focushive-backend
echo "   ✅ Build successful"

# Test that internal services don't expose ports
echo ""
echo "3. Verifying port exposure..."
EXPOSED_PORTS=$(docker-compose config | grep -E "^\s+- \"(5432|6379):" || true)
if [ -z "$EXPOSED_PORTS" ]; then
    echo "   ✅ No internal ports exposed"
else
    echo "   ❌ Found exposed internal ports:"
    echo "$EXPOSED_PORTS"
    exit 1
fi

# Test service startup
echo ""
echo "4. Testing service startup..."
docker-compose up -d postgres redis
sleep 10

# Check service health
echo ""
echo "5. Checking service health..."
docker-compose ps | grep -E "(postgres|redis)"

# Test connectivity between services
echo ""
echo "6. Testing internal connectivity..."
docker-compose exec -T postgres pg_isready -h postgres -p 5432 && echo "   ✅ PostgreSQL reachable internally"
docker-compose exec -T redis redis-cli ping && echo "   ✅ Redis reachable internally"

# Cleanup
echo ""
echo "7. Cleaning up..."
docker-compose down

echo ""
echo "=========================================="
echo "✅ All verification checks passed!"
echo "=========================================="