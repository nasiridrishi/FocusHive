#!/bin/bash
# Deploy FocusHive Frontend to dev.focushive.app via Cloudflare Tunnel

set -e

echo "Starting FocusHive Frontend Deployment..."

# Change to docker directory
cd "$(dirname "$0")"

# Check if .env file exists
if [ ! -f .env ]; then
    echo "Error: .env file not found in docker directory!"
    echo "Please ensure .env file exists with CLOUDFLARE_TUNNEL_TOKEN"
    exit 1
fi

# Check if CLOUDFLARE_TUNNEL_TOKEN is set in .env
if ! grep -q "CLOUDFLARE_TUNNEL_TOKEN=" .env || [ -z "$(grep CLOUDFLARE_TUNNEL_TOKEN= .env | cut -d'=' -f2)" ]; then
    echo "Error: CLOUDFLARE_TUNNEL_TOKEN not set in .env file!"
    exit 1
fi

# Export environment variables from .env
export $(grep -v '^#' .env | xargs)

# Stop any existing containers
echo "Stopping existing containers..."
docker compose --env-file .env -f docker-compose.frontend.yml down

# Build fresh images
echo "Building Docker images..."
docker compose --env-file .env -f docker-compose.frontend.yml build

# Start services
echo "Starting services..."
docker compose --env-file .env -f docker-compose.frontend.yml up -d

# Check container status
echo "Checking container status..."
sleep 5
docker ps | grep focushive

# Check tunnel logs
echo "Checking Cloudflare tunnel status..."
docker logs focushive-dev-tunnel --tail 10

echo "Deployment complete!"
echo "Frontend should be accessible at: https://dev.focushive.app"
echo ""
echo "To check logs:"
echo "  docker logs focushive-dev-frontend"
echo "  docker logs focushive-dev-tunnel"
echo ""
echo "To stop:"
echo "  docker compose --env-file .env -f docker-compose.frontend.yml down"