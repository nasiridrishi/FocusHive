#!/bin/bash

# FocusHive Shared Network Setup Script
# This script ensures the shared network exists before deploying services

set -e

NETWORK_NAME="focushive-shared-network"

echo "🔍 Checking if network '$NETWORK_NAME' exists..."

# Check if network already exists
if docker network ls --format "table {{.Name}}" | grep -q "^$NETWORK_NAME$"; then
    echo "✅ Network '$NETWORK_NAME' already exists"
else
    echo "🔧 Creating network '$NETWORK_NAME'..."
    docker network create $NETWORK_NAME
    echo "✅ Network '$NETWORK_NAME' created successfully"
fi

echo ""
echo "🚀 Network setup complete! You can now deploy services:"
echo "   cd services/identity-service && docker-compose up -d"
echo "   cd services/notification-service && docker-compose up -d" 
echo "   cd services/buddy-service && docker-compose up -d"
echo "   cd services/focushive-backend && docker-compose up -d"
echo ""
echo "📡 All services will be able to communicate with each other."