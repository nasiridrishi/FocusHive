#!/bin/bash
# Quick test script to verify deployment configurations

set -e

echo "Testing FocusHive Deployment Configurations"
echo "=========================================="
echo ""

# Test frontend deployment
echo "1. Testing Frontend Deployment..."
if docker compose --env-file docker/.env -f docker/docker-compose.frontend.yml config > /dev/null 2>&1; then
    echo "   ✅ Frontend configuration is valid"
else
    echo "   ❌ Frontend configuration has errors"
fi

# Test backend deployment
echo "2. Testing Backend Deployment..."
if docker compose --env-file docker/.env -f docker/docker-compose.backend-internal.yml config > /dev/null 2>&1; then
    echo "   ✅ Backend configuration is valid"
else
    echo "   ❌ Backend configuration has errors"
fi

# Test full stack deployment
echo "3. Testing Full Stack Deployment..."
if docker compose --env-file docker/.env -f docker/docker-compose.full.yml config > /dev/null 2>&1; then
    echo "   ✅ Full stack configuration is valid"
else
    echo "   ❌ Full stack configuration has errors"
fi

# Check Cloudflare token
echo ""
echo "4. Checking Cloudflare Token..."
if grep -q "CLOUDFLARE_TUNNEL_TOKEN=" docker/.env && [ -n "$(grep CLOUDFLARE_TUNNEL_TOKEN= docker/.env | cut -d'=' -f2)" ]; then
    TOKEN_LENGTH=$(grep CLOUDFLARE_TUNNEL_TOKEN= docker/.env | cut -d'=' -f2 | wc -c)
    echo "   ✅ Cloudflare token is set (length: $TOKEN_LENGTH)"
else
    echo "   ❌ Cloudflare token is missing or empty"
fi

# Check if frontend is currently deployed
echo ""
echo "5. Current Deployment Status:"
if docker ps | grep -q "focushive-dev-frontend"; then
    echo "   ✅ Frontend is currently running"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep focushive
else
    echo "   ℹ️  No FocusHive containers are currently running"
fi

# Show network status
echo ""
echo "6. Docker Networks:"
if docker network ls | grep -q "focushive-network"; then
    echo "   ✅ FocusHive network exists"
else
    echo "   ℹ️  FocusHive network not created yet"
fi

echo ""
echo "=========================================="
echo "Deployment configuration test complete!"
echo ""
echo "To deploy:"
echo "  Frontend only:  ./scripts/deploy/deploy-local-docker.sh frontend"
echo "  Backend only:   ./scripts/deploy/deploy-local-docker.sh backend"
echo "  Full stack:     ./scripts/deploy/deploy-local-docker.sh full"