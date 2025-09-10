#!/bin/bash

# FocusHive - Check All Services Status
# This script checks the health status of all 8 backend services

echo "🏥 Checking FocusHive Services Health Status..."
echo "=============================================="

# Services configuration
declare -A SERVICES
SERVICES["Identity Service"]="8081"
SERVICES["FocusHive Backend"]="8080"
SERVICES["Music Service"]="8082"
SERVICES["Notification Service"]="8083"
SERVICES["Chat Service"]="8084"
SERVICES["Analytics Service"]="8085"
SERVICES["Forum Service"]="8086"
SERVICES["Buddy Service"]="8087"

HEALTHY_COUNT=0
TOTAL_SERVICES=8

# Function to check service health
check_service_health() {
    local service_name="$1"
    local port="$2"
    local url="http://localhost:$port/api/v1/health"
    
    printf "%-25s Port %-4s " "$service_name" "$port"
    
    # Check if service responds within 5 seconds
    if response=$(curl -s --max-time 5 "$url" 2>/dev/null); then
        if echo "$response" | grep -q '"status":"UP"'; then
            echo "✅ HEALTHY"
            ((HEALTHY_COUNT++))
        else
            echo "⚠️  UNHEALTHY (responded but status not UP)"
        fi
    else
        echo "❌ DOWN (no response)"
    fi
}

# Check all services
for service_name in "${!SERVICES[@]}"; do
    check_service_health "$service_name" "${SERVICES[$service_name]}"
done

echo ""
echo "📊 SUMMARY:"
echo "   Healthy Services: $HEALTHY_COUNT/$TOTAL_SERVICES"

if [ $HEALTHY_COUNT -eq $TOTAL_SERVICES ]; then
    echo "🎉 All services are healthy!"
    echo ""
    echo "🌐 Frontend should be available at: http://localhost:3000"
else
    echo "⚠️  Some services are not healthy. Check logs:"
    echo "   tail -f services/*/logs/*.log"
fi