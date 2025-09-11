#!/bin/bash

# FocusHive Local Development Startup Script (No NGINX, Local PostgreSQL)
# This script starts all services locally without Docker/NGINX, using local PostgreSQL

set -e  # Exit on error

# Set JAVA_HOME for the script
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"

# PostgreSQL Configuration (adjust these to match your local setup)
DB_HOST="localhost"
DB_PORT="5432"
DB_USER="${DB_USER:-focushive_user}"
DB_PASSWORD="${DB_PASSWORD:-focushive_pass}"
DB_NAME="${DB_NAME:-focushive}"
IDENTITY_DB_NAME="${IDENTITY_DB_NAME:-identity_db}"
IDENTITY_DB_USER="${IDENTITY_DB_USER:-identity_user}"
IDENTITY_DB_PASSWORD="${IDENTITY_DB_PASSWORD:-identity_pass}"

# Using Docker for PostgreSQL, no need for local psql

# Redis Configuration
REDIS_PORT="6379"
IDENTITY_REDIS_PORT="6380"

# Service Ports
BACKEND_PORT="8080"
IDENTITY_PORT="8081"
MUSIC_PORT="8082"
NOTIFICATION_PORT="8083"
CHAT_PORT="8084"
ANALYTICS_PORT="8085"
FORUM_PORT="8086"
BUDDY_PORT="8087"
FRONTEND_PORT="5173"

# Process tracking
PIDS_FILE="$PROJECT_ROOT/.local-services.pids"

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}   FocusHive Local Development Startup${NC}"
echo -e "${BLUE}   (No NGINX, Local PostgreSQL)${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

# Function to check if port is in use
check_port() {
    local port=$1
    # Try netstat first, fall back to lsof if needed
    if netstat -tln 2>/dev/null | grep -q ":$port "; then
        return 0
    elif lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Function to wait for service
wait_for_service() {
    local service=$1
    local port=$2
    local max_attempts=30
    local attempt=1
    
    echo -n "Waiting for $service on port $port..."
    while ! check_port $port; do
        if [ $attempt -eq $max_attempts ]; then
            echo -e " ${RED}Failed!${NC}"
            return 1
        fi
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    echo -e " ${GREEN}Ready!${NC}"
    return 0
}

# Function to wait for Redis specifically
wait_for_redis() {
    local service=$1
    local port=$2
    local password=$3
    local container=$4
    local max_attempts=30
    local attempt=1
    
    echo -n "Waiting for $service on port $port..."
    while true; do
        if [ $attempt -eq $max_attempts ]; then
            echo -e " ${RED}Failed!${NC}"
            return 1
        fi
        
        # Check if container is running and Redis responds to ping
        if docker exec "$container" redis-cli -a "$password" ping >/dev/null 2>&1; then
            echo -e " ${GREEN}Ready!${NC}"
            return 0
        fi
        
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
}

# Function to add colored prefix to output
add_prefix() {
    local prefix=$1
    local color=$2
    while IFS= read -r line; do
        echo -e "${color}[$prefix]${NC} $line"
    done
}

# Function to start a service
start_service() {
    local service_name=$1
    local service_dir=$2
    local port=$3
    local prefix_name=""
    local prefix_color=""
    
    # Set prefix and color based on service
    case "$service_name" in
        "backend")
            prefix_name="BACKEND"
            prefix_color="${GREEN}"
            ;;
        "identity-service")
            prefix_name="IDENTITY"
            prefix_color="${BLUE}"
            ;;
        "frontend")
            prefix_name="FRONTEND"
            prefix_color="${YELLOW}"
            ;;
        *)
            prefix_name=$(echo "$service_name" | tr '[:lower:]' '[:upper:]' | cut -c1-8)
            prefix_color="${NC}"
            ;;
    esac
    
    echo -e "${YELLOW}Starting $service_name...${NC}"
    cd "$service_dir"
    
    # Start the service in background with colored prefix output
    if [[ "$service_name" == "frontend" ]]; then
        npm run dev -- --host 0.0.0.0 2>&1 | add_prefix "$prefix_name" "$prefix_color" &
    elif [[ "$service_name" == "backend" ]]; then
        # Backend needs special handling for database config
        SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/$DB_NAME" \
        SPRING_DATASOURCE_USERNAME="$DB_USER" \
        SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD" \
        SPRING_DATASOURCE_DRIVER_CLASS_NAME="org.postgresql.Driver" \
        SPRING_JPA_HIBERNATE_DDL_AUTO="update" \
        SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT="org.hibernate.dialect.PostgreSQLDialect" \
        SPRING_DATA_REDIS_HOST="localhost" \
        SPRING_DATA_REDIS_PORT="$REDIS_PORT" \
        SPRING_DATA_REDIS_PASSWORD="focushive_pass" \
        ./gradlew bootRun 2>&1 | add_prefix "$prefix_name" "$prefix_color" &
    elif [[ "$service_name" == "identity-service" ]]; then
        # Identity service with its own database on port 5433
        DB_URL="jdbc:postgresql://localhost:5433/$IDENTITY_DB_NAME" \
        DB_USERNAME="$IDENTITY_DB_USER" \
        DB_PASSWORD="$IDENTITY_DB_PASSWORD" \
        JWT_SECRET="your-512-bit-secret-key-for-jwt-token-generation-that-is-exactly-64-bytes-long!!!" \
        REDIS_HOST="localhost" \
        REDIS_PORT="$IDENTITY_REDIS_PORT" \
        REDIS_PASSWORD="identity_redis_pass" \
        SERVER_PORT="$IDENTITY_PORT" \
        SPRING_PROFILES_ACTIVE="dev" \
        SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5433/$IDENTITY_DB_NAME" \
        SPRING_DATASOURCE_USERNAME="$IDENTITY_DB_USER" \
        SPRING_DATASOURCE_PASSWORD="$IDENTITY_DB_PASSWORD" \
        ./gradlew bootRun 2>&1 | add_prefix "$prefix_name" "$prefix_color" &
    else
        # Other services use main database
        SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/$DB_NAME" \
        SPRING_DATASOURCE_USERNAME="$DB_USER" \
        SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD" \
        SPRING_DATASOURCE_DRIVER_CLASS_NAME="org.postgresql.Driver" \
        SPRING_JPA_HIBERNATE_DDL_AUTO="update" \
        SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT="org.hibernate.dialect.PostgreSQLDialect" \
        SPRING_DATA_REDIS_HOST="localhost" \
        SPRING_DATA_REDIS_PORT="$REDIS_PORT" \
        SPRING_DATA_REDIS_PASSWORD="focushive_pass" \
        ./gradlew bootRun 2>&1 | add_prefix "$prefix_name" "$prefix_color" &
    fi
    
    local pid=$!
    echo "$service_name:$pid" >> "$PIDS_FILE"
    
    # Wait for service to be ready
    wait_for_service "$service_name" "$port"
}

# Cleanup function
cleanup() {
    echo ""
    echo -e "${YELLOW}Shutting down services...${NC}"
    
    if [ -f "$PIDS_FILE" ]; then
        while IFS=':' read -r service pid; do
            if kill -0 "$pid" 2>/dev/null; then
                echo "Stopping $service (PID: $pid)"
                kill "$pid" 2>/dev/null || true
            fi
        done < "$PIDS_FILE"
        rm "$PIDS_FILE"
    fi
    
    # Stop Docker containers if we started them
    docker stop focushive-postgres 2>/dev/null || true
    docker stop focushive-identity-postgres 2>/dev/null || true
    docker stop focushive-redis 2>/dev/null || true
    docker stop focushive-identity-redis 2>/dev/null || true
    
    echo -e "${GREEN}All services stopped.${NC}"
}

# Set up trap for cleanup on exit
trap cleanup EXIT INT TERM

# Clear previous PIDs file
rm -f "$PIDS_FILE"

# Step 1: Start PostgreSQL with Docker
echo -e "${BLUE}Step 1: Starting PostgreSQL databases with Docker...${NC}"

# Main PostgreSQL
if ! check_port 5432; then
    echo "Starting main PostgreSQL..."
    docker run -d --name focushive-postgres \
        -p 5432:5432 \
        -e POSTGRES_USER=$DB_USER \
        -e POSTGRES_PASSWORD=$DB_PASSWORD \
        -e POSTGRES_DB=$DB_NAME \
        --rm \
        postgres:16-alpine
    
    # Wait for PostgreSQL to be ready
    echo -n "Waiting for main PostgreSQL..."
    while ! docker exec focushive-postgres pg_isready -U $DB_USER >/dev/null 2>&1; do
        echo -n "."
        sleep 2
    done
    echo -e " ${GREEN}Ready!${NC}"
else
    echo -e "${YELLOW}Main PostgreSQL already running on port 5432${NC}"
fi

# Identity PostgreSQL (on different port)
if ! check_port 5433; then
    echo "Starting identity PostgreSQL..."
    docker run -d --name focushive-identity-postgres \
        -p 5433:5432 \
        -e POSTGRES_USER=$IDENTITY_DB_USER \
        -e POSTGRES_PASSWORD=$IDENTITY_DB_PASSWORD \
        -e POSTGRES_DB=$IDENTITY_DB_NAME \
        --rm \
        postgres:16-alpine
    
    # Wait for PostgreSQL to be ready
    echo -n "Waiting for identity PostgreSQL..."
    while ! docker exec focushive-identity-postgres pg_isready -U $IDENTITY_DB_USER >/dev/null 2>&1; do
        echo -n "."
        sleep 2
    done
    echo -e " ${GREEN}Ready!${NC}"
else
    echo -e "${YELLOW}Identity PostgreSQL already running on port 5433${NC}"
fi

echo -e "${GREEN}PostgreSQL databases ready${NC}"

# Step 3: Start Redis using Docker (easier than installing locally)
echo -e "${BLUE}Step 3: Starting Redis instances...${NC}"

# Main Redis
if ! check_port $REDIS_PORT; then
    docker run -d --name focushive-redis \
        -p $REDIS_PORT:6379 \
        --rm \
        redis:7-alpine \
        redis-server --requirepass focushive_pass
    wait_for_redis "Redis (Main)" $REDIS_PORT "focushive_pass" "focushive-redis"
else
    echo -e "${YELLOW}Redis (Main) already running on port $REDIS_PORT${NC}"
fi

# Identity Redis
if ! check_port $IDENTITY_REDIS_PORT; then
    docker run -d --name focushive-identity-redis \
        -p $IDENTITY_REDIS_PORT:6379 \
        --rm \
        redis:7-alpine \
        redis-server --requirepass identity_redis_pass
    wait_for_redis "Redis (Identity)" $IDENTITY_REDIS_PORT "identity_redis_pass" "focushive-identity-redis"
else
    echo -e "${YELLOW}Redis (Identity) already running on port $IDENTITY_REDIS_PORT${NC}"
fi

# Step 4: Services will be configured via environment variables
echo -e "${BLUE}Step 4: Services will use environment variables for configuration${NC}"
echo -e "${GREEN}No property files needed - using environment variables${NC}"

# Step 5: Update frontend environment
echo -e "${BLUE}Step 5: Configuring frontend...${NC}"
cat > "$PROJECT_ROOT/frontend/.env.local" <<EOF
# API endpoints (direct access without NGINX)
VITE_API_BASE_URL=http://localhost:$BACKEND_PORT
VITE_IDENTITY_API_URL=http://localhost:$IDENTITY_PORT
VITE_MUSIC_API_URL=http://localhost:$MUSIC_PORT
VITE_NOTIFICATION_API_URL=http://localhost:$NOTIFICATION_PORT
VITE_CHAT_API_URL=http://localhost:$CHAT_PORT
VITE_ANALYTICS_API_URL=http://localhost:$ANALYTICS_PORT
VITE_FORUM_API_URL=http://localhost:$FORUM_PORT
VITE_BUDDY_API_URL=http://localhost:$BUDDY_PORT

# WebSocket Configuration (Required)
VITE_WEBSOCKET_URL=ws://localhost:$BACKEND_PORT
VITE_WS_URL=ws://localhost:$BACKEND_PORT/ws

# Optional WebSocket Settings
VITE_WEBSOCKET_RECONNECT_ATTEMPTS=10
VITE_WEBSOCKET_RECONNECT_DELAY=1000
VITE_WEBSOCKET_HEARTBEAT_INTERVAL=30000

# Other configurations
VITE_APP_NAME=FocusHive
VITE_APP_ENV=local
EOF

echo -e "${GREEN}Frontend configured${NC}"

# Step 6: Install dependencies
echo -e "${BLUE}Step 6: Installing dependencies...${NC}"

# Frontend dependencies
if [ ! -d "$PROJECT_ROOT/frontend/node_modules" ]; then
    echo "Installing frontend dependencies..."
    cd "$PROJECT_ROOT/frontend"
    npm install
else
    echo "Frontend dependencies already installed"
fi

# Step 7: Start services
echo -e "${BLUE}Step 7: Starting services...${NC}"

# Start backend services

# Start main backend
start_service "backend" "$PROJECT_ROOT/services/focushive-backend" $BACKEND_PORT

# Start identity service
start_service "identity-service" "$PROJECT_ROOT/services/identity-service" $IDENTITY_PORT

# Start other services if they exist
for service_dir in "$PROJECT_ROOT/services"/*-service; do
    if [ -d "$service_dir" ] && [ "$(basename $service_dir)" != "identity-service" ]; then
        service_name=$(basename "$service_dir")
        service_upper=$(echo $service_name | tr '[:lower:]' '[:upper:]' | tr '-' '_')
        eval "port=\$${service_upper//-SERVICE/}_PORT"
        
        if [ ! -z "$port" ]; then
            start_service "$service_name" "$service_dir" "$port" || true
        fi
    fi
done

# Start frontend last
start_service "frontend" "$PROJECT_ROOT/frontend" $FRONTEND_PORT

# Final status
echo ""
echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}   All services started successfully!${NC}"
echo -e "${GREEN}================================================${NC}"
echo ""
echo "Services available at:"
echo -e "  ${BLUE}Frontend:${NC} http://localhost:$FRONTEND_PORT"
echo -e "  ${BLUE}Backend API:${NC} http://localhost:$BACKEND_PORT"
echo -e "  ${BLUE}Identity API:${NC} http://localhost:$IDENTITY_PORT"
echo -e "  ${BLUE}Music API:${NC} http://localhost:$MUSIC_PORT"
echo ""
echo -e "${CYAN}Network Access:${NC}"
echo -e "  Frontend is accessible from other devices on your network at:"
echo -e "  ${CYAN}http://$(ipconfig getifaddr en0 2>/dev/null || hostname -I 2>/dev/null | awk '{print $1}' || echo "YOUR_IP"):$FRONTEND_PORT${NC}"
echo ""
echo "Logs are shown below with colored prefixes:"
echo -e "  ${GREEN}[BACKEND]${NC} - Backend service logs"
echo -e "  ${YELLOW}[FRONTEND]${NC} - Frontend service logs (with HMR)"
echo -e "  ${BLUE}[IDENTITY]${NC} - Identity service logs"
echo ""
echo -e "${YELLOW}Press Ctrl+C to stop all services${NC}"
echo ""

# Keep script running
wait