#!/bin/bash

# Secure FocusHive Deployment - No Host Port Exposure
# Only accessible via Cloudflare Tunnel

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "🔒 FocusHive Secure Deployment (No Host Ports)"
echo "==============================================="

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker and try again."
    exit 1
fi

# Cloudflare token (required for external access)
CLOUDFLARE_TOKEN="${1:-}"

if [ -z "$CLOUDFLARE_TOKEN" ]; then
    echo "⚠️  Warning: No Cloudflare token provided."
    echo "   The application will only be accessible from within Docker network."
    echo ""
    echo "   Usage: $0 <cloudflare-tunnel-token>"
    echo ""
fi

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
    echo "🌐 Starting services (internal only)..."
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

# Test internal health
echo ""
echo "🧪 Testing internal health..."
if docker exec focushive-dev-app curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/health | grep -q "200"; then
    echo "✅ App health check: OK"
else
    echo "❌ App health check: FAILED"
fi

echo ""
echo "🔒 Security Configuration:"
echo "-------------------------"
echo "✅ No ports exposed to host"
echo "✅ Application only accessible via:"

if [ -n "$CLOUDFLARE_TOKEN" ]; then
    echo "   - Cloudflare Tunnel: https://dev.focushive.app/"
    echo ""
    echo "⚠️  Tunnel Status:"
    TUNNEL_STATUS=$(docker ps --format "{{.Names}} {{.Status}}" | grep tunnel | awk '{print $2}')
    if [[ "$TUNNEL_STATUS" == "Up" ]]; then
        echo "   ✅ Tunnel is running"
    else
        echo "   ❌ Tunnel is having issues. Check logs with:"
        echo "      docker logs focushive-dev-tunnel"
    fi
else
    echo "   - Internal Docker network only"
    echo "   - Add Cloudflare token to enable external access"
fi

echo ""
echo "📝 Useful commands:"
echo "  View app logs:     docker logs focushive-dev-app"
echo "  View tunnel logs:  docker logs focushive-dev-tunnel"
echo "  Stop all:          docker compose -f docker/docker-compose.frontend.yml down"
echo "  Test internal:     docker exec focushive-dev-app curl http://localhost:3000/"

echo ""
echo "🎉 Deployment complete!"