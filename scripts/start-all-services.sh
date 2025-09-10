#!/bin/bash

# FocusHive - Start All Services
# This script starts all 8 backend services in the correct order

set -e  # Exit on any error

echo "🚀 Starting all FocusHive services..."
echo "======================================"

PROJECT_ROOT="/Users/nasir/uol/focushive"

# Check if postgres is running
echo "🔍 Checking PostgreSQL availability..."
if ! pg_isready -h localhost -p 5432 > /dev/null 2>&1; then
    echo "⚠️  PostgreSQL is not running on default port 5432"
    echo "   Please start PostgreSQL first or check connection"
fi

if ! pg_isready -h localhost -p 5433 > /dev/null 2>&1; then
    echo "⚠️  PostgreSQL is not running on port 5433 (identity service)"
    echo "   Please start PostgreSQL first or check connection"
fi

echo "✅ Database connections available"
echo ""

# Function to start a service
start_service() {
    local service_name="$1"
    local service_dir="$2"  
    local port="$3"
    local db_config="$4"
    
    echo "🚀 Starting: $service_name"
    echo "   Port: $port"
    echo "   Directory: services/$service_dir"
    
    cd "$PROJECT_ROOT/services/$service_dir"
    
    # Start service in background with database configuration
    if [ -n "$db_config" ]; then
        echo "   Database: $db_config"
        eval "$db_config ./gradlew bootRun > logs/$service_dir.log 2>&1 &"
    else
        ./gradlew bootRun > "logs/$service_dir.log" 2>&1 &
    fi
    
    local service_pid=$!
    echo "   PID: $service_pid"
    echo "   Logs: services/$service_dir/logs/$service_dir.log"
    echo ""
    
    # Give service time to start
    sleep 3
}

# Create logs directory for each service
echo "📁 Creating log directories..."
for service_dir in identity-service focushive-backend music-service notification-service chat-service analytics-service forum-service buddy-service; do
    mkdir -p "$PROJECT_ROOT/services/$service_dir/logs"
done

echo "🎯 Starting services in dependency order..."
echo ""

# 1. Identity Service (Port 8081) - OAuth2 Provider, needed by others
start_service "Identity Service" "identity-service" "8081" "DB_URL=jdbc:postgresql://localhost:5433/identity_db DB_USERNAME=postgres DB_PASSWORD=postgres"

# 2. Core FocusHive Backend (Port 8080) - Main application logic  
start_service "FocusHive Backend" "focushive-backend" "8080"

# 3. Supporting Services (can start in parallel)
start_service "Music Service" "music-service" "8082"
start_service "Notification Service" "notification-service" "8083" 
start_service "Chat Service" "chat-service" "8084"
start_service "Analytics Service" "analytics-service" "8085"
start_service "Forum Service" "forum-service" "8086"
start_service "Buddy Service" "buddy-service" "8087"

echo "⏳ Waiting for all services to fully start..."
sleep 15

echo ""
echo "🎉 All FocusHive services started!"
echo "=================================="
echo ""
echo "📍 Service URLs:"
echo "   Identity Service:     http://localhost:8081/api/v1/health"
echo "   FocusHive Backend:    http://localhost:8080/api/v1/health"  
echo "   Music Service:        http://localhost:8082/api/v1/health"
echo "   Notification Service: http://localhost:8083/api/v1/health"
echo "   Chat Service:         http://localhost:8084/api/v1/health"
echo "   Analytics Service:    http://localhost:8085/api/v1/health"
echo "   Forum Service:        http://localhost:8086/api/v1/health"
echo "   Buddy Service:        http://localhost:8087/api/v1/health"
echo ""
echo "📊 Check service status:"
echo "   ./scripts/check-services.sh"
echo ""
echo "🛑 Stop all services:"
echo "   ./scripts/stop-all-services.sh"