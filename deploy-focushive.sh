#!/bin/bash

# FocusHive Master Deployment Script
# Handles network setup and deploys all services with proper inter-service communication

set -e

NETWORK_NAME="focushive-shared-network"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"

echo "🚀 FocusHive Deployment Starting..."
echo ""

# Step 1: Ensure shared network exists
echo "🔍 Checking if network '$NETWORK_NAME' exists..."
if docker network ls --format "table {{.Name}}" | grep -q "^$NETWORK_NAME$"; then
    echo "✅ Network '$NETWORK_NAME' already exists"
else
    echo "🔧 Creating network '$NETWORK_NAME'..."
    docker network create $NETWORK_NAME
    echo "✅ Network '$NETWORK_NAME' created successfully"
fi
echo ""

# Step 2: Deploy services in dependency order
SERVICES=("notification-service" "identity-service" "buddy-service" "focushive-backend")

for service in "${SERVICES[@]}"; do
    service_path="$SCRIPT_DIR/services/$service"
    if [ -d "$service_path" ]; then
        echo "📦 Deploying $service..."
        cd "$service_path"
        docker-compose up -d
        echo "✅ $service deployed"
        echo ""
    else
        echo "⚠️  Service directory not found: $service_path"
    fi
done

echo "🎉 FocusHive Deployment Complete!"
echo ""
echo "📊 Service Status:"
docker ps --filter "name=focushive" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | head -20
echo ""
echo "🌐 Network Status:"
docker network inspect $NETWORK_NAME --format "Connected containers: {{len .Containers}}"
echo ""
echo "✨ All services are now running with inter-service communication enabled!"
echo "   - Identity Service: http://localhost:8081"
echo "   - Notification Service: http://localhost:8083"  
echo "   - Buddy Service: http://localhost:8087"
echo "   - Backend Service: http://localhost:8080"