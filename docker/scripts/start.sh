#!/bin/bash

# FocusHive Docker Startup Script
# Usage: ./docker/scripts/start.sh [dev|prod]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default environment
ENVIRONMENT=${1:-dev}

# Function to print colored output
print_message() {
    echo -e "${2}${1}${NC}"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check prerequisites
check_prerequisites() {
    print_message "Checking prerequisites..." "$YELLOW"
    
    if ! command_exists docker; then
        print_message "Docker is not installed. Please install Docker first." "$RED"
        exit 1
    fi
    
    if ! command_exists docker-compose; then
        print_message "Docker Compose is not installed. Please install Docker Compose first." "$RED"
        exit 1
    fi
    
    if [ ! -f .env ]; then
        if [ -f .env.docker.example ]; then
            print_message "Creating .env file from .env.docker.example..." "$YELLOW"
            cp .env.docker.example .env
            print_message "Please update .env file with your configuration." "$YELLOW"
        else
            print_message ".env file not found. Please create it from .env.docker.example" "$RED"
            exit 1
        fi
    fi
    
    print_message "Prerequisites check passed!" "$GREEN"
}

# Function to build images
build_images() {
    print_message "Building Docker images..." "$YELLOW"
    
    if [ "$ENVIRONMENT" = "prod" ]; then
        docker-compose -f docker-compose.yml -f docker-compose.prod.yml build --parallel
    else
        docker-compose build --parallel
    fi
    
    print_message "Images built successfully!" "$GREEN"
}

# Function to start services
start_services() {
    print_message "Starting FocusHive services in $ENVIRONMENT mode..." "$YELLOW"
    
    if [ "$ENVIRONMENT" = "prod" ]; then
        docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
    elif [ "$ENVIRONMENT" = "dev" ]; then
        # Start with dev tools
        docker-compose --profile dev up -d
    else
        docker-compose up -d
    fi
    
    print_message "Services started!" "$GREEN"
}

# Function to wait for services to be healthy
wait_for_services() {
    print_message "Waiting for services to be healthy..." "$YELLOW"
    
    services=("postgres" "redis" "identity-service" "backend" "frontend")
    
    for service in "${services[@]}"; do
        echo -n "Waiting for $service..."
        
        max_attempts=30
        attempt=0
        
        while [ $attempt -lt $max_attempts ]; do
            if docker-compose ps | grep "focushive-$service" | grep -q "healthy\|running"; then
                echo " ✓"
                break
            fi
            
            sleep 2
            attempt=$((attempt + 1))
            echo -n "."
        done
        
        if [ $attempt -eq $max_attempts ]; then
            echo " ✗"
            print_message "Service $service failed to start!" "$RED"
            docker-compose logs $service
            exit 1
        fi
    done
    
    print_message "All services are healthy!" "$GREEN"
}

# Function to run database migrations
run_migrations() {
    print_message "Running database migrations..." "$YELLOW"
    
    # Wait a bit more for database to be fully ready
    sleep 5
    
    # Run Flyway migrations for backend
    docker-compose exec -T backend sh -c "java -jar /app/app.jar db migrate" || true
    
    # Run Flyway migrations for identity service
    docker-compose exec -T identity-service sh -c "java -jar /app/app.jar db migrate" || true
    
    print_message "Migrations completed!" "$GREEN"
}

# Function to show service URLs
show_urls() {
    print_message "\n📌 FocusHive is running!" "$GREEN"
    print_message "================================" "$GREEN"
    echo "Frontend:         http://localhost:${FRONTEND_PORT:-3000}"
    echo "Backend API:      http://localhost:${BACKEND_PORT:-8080}/api"
    echo "Identity Service: http://localhost:${IDENTITY_PORT:-8081}/auth"
    echo "WebSocket:        ws://localhost:${BACKEND_PORT:-8080}/ws"
    
    if [ "$ENVIRONMENT" = "dev" ]; then
        echo ""
        print_message "Development Tools:" "$YELLOW"
        echo "pgAdmin:          http://localhost:${PGADMIN_PORT:-5050}"
        echo "Redis Commander:  http://localhost:${REDIS_COMMANDER_PORT:-8082}"
    fi
    
    echo ""
    print_message "Useful commands:" "$YELLOW"
    echo "View logs:        docker-compose logs -f [service]"
    echo "Stop services:    ./docker/scripts/stop.sh"
    echo "Clean up:         ./docker/scripts/cleanup.sh"
    echo "Backup database:  ./docker/scripts/backup.sh"
}

# Main execution
main() {
    print_message "🚀 Starting FocusHive Platform" "$GREEN"
    print_message "================================" "$GREEN"
    
    check_prerequisites
    build_images
    start_services
    wait_for_services
    run_migrations
    show_urls
    
    print_message "\n✅ FocusHive is ready!" "$GREEN"
    print_message "Press Ctrl+C to stop following logs..." "$YELLOW"
    
    # Follow logs
    docker-compose logs -f --tail=100
}

# Run main function
main