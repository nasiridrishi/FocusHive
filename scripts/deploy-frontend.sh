#!/bin/bash

# Deploy FocusHive Frontend with Docker
# Usage: ./scripts/deploy-frontend.sh [cloudflare-token]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "🚀 FocusHive Frontend Deployment Script"
echo "======================================="

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker and try again."
    exit 1
fi

# Cloudflare token (optional)
CLOUDFLARE_TOKEN="${1:-}"

# Navigate to project root
cd "$PROJECT_ROOT"

# Stop existing containers
echo "🛑 Stopping existing containers..."
docker compose -f docker/docker-compose.frontend.yml down 2>/dev/null || true

# Build frontend image
echo "🔨 Building frontend Docker image..."
docker build -f docker/frontend/Dockerfile -t docker-frontend .

# Start services
if [ -n "$CLOUDFLARE_TOKEN" ]; then
    echo "🌐 Starting services with Cloudflare tunnel..."
    CLOUDFLARE_TUNNEL_TOKEN="$CLOUDFLARE_TOKEN" docker compose -f docker/docker-compose.frontend.yml up -d
else
    echo "🌐 Starting services (local only, no tunnel)..."
    docker compose -f docker/docker-compose.frontend.yml up -d
fi

# Wait for services to be ready
echo "⏳ Waiting for services to start..."
sleep 5

# Check service status
echo ""
echo "📊 Service Status:"
echo "------------------"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep focushive || true

# Test local access
echo ""
echo "🧪 Testing local access..."
if curl -s -o /dev/null -w "%{http_code}" http://localhost/health | grep -q "200"; then
    echo "✅ Nginx health check: OK"
else
    echo "❌ Nginx health check: FAILED"
fi

if curl -s http://localhost/ | grep -q "FocusHive"; then
    echo "✅ Frontend serving: OK"
else
    echo "❌ Frontend serving: FAILED"
fi

echo ""
echo "📌 Access Points:"
echo "-----------------"
echo "Local: http://localhost/"

if [ -n "$CLOUDFLARE_TOKEN" ]; then
    echo "Cloudflare: https://dev.focushive.app/ (if tunnel is connected)"
    echo ""
    echo "⚠️  Note: If the tunnel is not connecting, check:"
    echo "   1. The token is valid and not expired"
    echo "   2. The tunnel is not already running elsewhere"
    echo "   3. The tunnel configuration in Cloudflare dashboard"
fi

echo ""
echo "🎉 Deployment complete!"
echo ""
echo "📝 Useful commands:"
echo "  View logs:    docker logs focushive-dev-frontend"
echo "  Stop services: docker compose -f docker/docker-compose.frontend.yml down"
echo "  Restart:      docker compose -f docker/docker-compose.frontend.yml restart"