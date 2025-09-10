#!/bin/bash

# FocusHive - Stop All Services
# This script stops all running backend services

echo "🛑 Stopping all FocusHive services..."
echo "====================================="

# Find all gradle daemon processes and Spring Boot applications
echo "🔍 Finding running services..."

# Stop Gradle daemons
echo "🔄 Stopping Gradle daemons..."
./gradlew --stop 2>/dev/null || true

# Find and kill Spring Boot processes
SPRING_PIDS=$(ps aux | grep -E "(bootRun|spring-boot)" | grep -v grep | awk '{print $2}')

if [ -n "$SPRING_PIDS" ]; then
    echo "🎯 Found Spring Boot processes: $SPRING_PIDS"
    echo "   Stopping processes..."
    
    for pid in $SPRING_PIDS; do
        echo "   Killing PID: $pid"
        kill -15 "$pid" 2>/dev/null || true  # SIGTERM first
        sleep 2
        
        # Force kill if still running
        if kill -0 "$pid" 2>/dev/null; then
            echo "   Force killing PID: $pid"
            kill -9 "$pid" 2>/dev/null || true
        fi
    done
else
    echo "ℹ️  No Spring Boot processes found running"
fi

# Clean up any remaining Java processes on our ports
PORTS=(8080 8081 8082 8083 8084 8085 8086 8087)

for port in "${PORTS[@]}"; do
    PID=$(lsof -ti:$port 2>/dev/null)
    if [ -n "$PID" ]; then
        echo "🔌 Freeing port $port (PID: $PID)"
        kill -15 "$PID" 2>/dev/null || true
        sleep 1
        
        # Force kill if needed
        if kill -0 "$PID" 2>/dev/null; then
            kill -9 "$PID" 2>/dev/null || true
        fi
    fi
done

echo ""
echo "✅ All FocusHive services stopped!"
echo ""
echo "🔍 Verify no services are running:"
echo "   ./scripts/check-services.sh"
echo ""
echo "🚀 Restart all services:"
echo "   ./scripts/start-all-services.sh"