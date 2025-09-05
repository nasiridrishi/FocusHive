#!/bin/bash

# FocusHive LAN Deployment Script
# This script starts all services accessible on your LAN

set -e

# Navigate to the project root
cd "$(dirname "$0")/../.."

LAN_IP=$(ip addr show | grep -E "inet.*brd" | grep -v "127.0.0.1" | awk '{print $2}' | cut -d'/' -f1 | head -1)

echo "========================================="
echo "   FocusHive LAN Deployment"
echo "========================================="
echo ""
echo "🌐 Your LAN IP: $LAN_IP"
echo ""

# Start database services
echo "📦 Starting database services..."
docker compose -f docker/docker-compose.yml up -d db identity-db redis identity-redis

# Wait for databases to be ready
echo "⏳ Waiting for databases to initialize..."
sleep 10

# Start backend services (if you want to use Docker)
# echo "🚀 Starting backend services..."
# docker compose -f docker/docker-compose.yml up -d backend identity-service nginx

# For now, just start the frontend
echo "🎨 Starting frontend development server..."
cd frontend
npm run dev -- --host 0.0.0.0 &
FRONTEND_PID=$!

echo ""
echo "========================================="
echo "   Services Available on LAN"
echo "========================================="
echo ""
echo "🎨 Frontend:     http://$LAN_IP:5173"
echo "🐘 PostgreSQL:   Internal only (security)"
echo "🔴 Redis:        Internal only (security)"
echo ""
echo "========================================="
echo "   Access from other devices:"
echo "========================================="
echo ""
echo "📱 Mobile:       http://$LAN_IP:5173"
echo "💻 Other PCs:    http://$LAN_IP:5173"
echo ""
echo "⚠️  Make sure your firewall allows port 5173"
echo ""
echo "Press Ctrl+C to stop all services"
echo ""

# Keep script running
wait $FRONTEND_PID