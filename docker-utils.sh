#!/bin/bash

# FocusHive Docker Utilities Script

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to display usage
usage() {
    echo "Usage: $0 {up|down|restart|logs|shell|db-shell|clean}"
    echo ""
    echo "Commands:"
    echo "  up        - Start all services"
    echo "  down      - Stop all services"
    echo "  restart   - Restart all services"
    echo "  logs      - Show logs (optionally specify service: logs backend)"
    echo "  shell     - Open shell in a service (e.g., shell backend)"
    echo "  db-shell  - Open database shell (postgres/mongodb/redis)"
    echo "  clean     - Stop services and remove volumes"
    exit 1
}

# Check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        echo -e "${RED}Docker is not running. Please start Docker first.${NC}"
        exit 1
    fi
}

# Start services
up() {
    echo -e "${GREEN}Starting FocusHive services...${NC}"
    docker-compose up -d
    echo -e "${GREEN}Services started! Access at:${NC}"
    echo "  Frontend: http://localhost:5173"
    echo "  Backend: Internal only (accessible via Docker network)"
    echo "  Databases: Internal only (PostgreSQL, MongoDB, Redis)"
}

# Stop services
down() {
    echo -e "${YELLOW}Stopping FocusHive services...${NC}"
    docker-compose down
    echo -e "${GREEN}Services stopped.${NC}"
}

# Restart services
restart() {
    down
    up
}

# Show logs
logs() {
    if [ -z "$1" ]; then
        docker-compose logs -f
    else
        docker-compose logs -f "$1"
    fi
}

# Open shell in service
shell() {
    if [ -z "$1" ]; then
        echo -e "${RED}Please specify a service (e.g., shell backend)${NC}"
        exit 1
    fi
    docker-compose exec "$1" sh
}

# Open database shell
db_shell() {
    case "$1" in
        postgres)
            docker-compose exec postgres psql -U focushive_user -d focushive
            ;;
        mongodb|mongo)
            docker-compose exec mongodb mongosh -u focushive_user -p focushive_pass --authenticationDatabase admin focushive
            ;;
        redis)
            docker-compose exec redis redis-cli -a focushive_pass
            ;;
        *)
            echo -e "${RED}Please specify a database: postgres, mongodb, or redis${NC}"
            exit 1
            ;;
    esac
}

# Clean up everything
clean() {
    echo -e "${YELLOW}Stopping services and removing volumes...${NC}"
    docker-compose down -v
    echo -e "${GREEN}Cleanup complete.${NC}"
}

# Main script logic
check_docker

case "$1" in
    up)
        up
        ;;
    down)
        down
        ;;
    restart)
        restart
        ;;
    logs)
        logs "$2"
        ;;
    shell)
        shell "$2"
        ;;
    db-shell)
        db_shell "$2"
        ;;
    clean)
        clean
        ;;
    *)
        usage
        ;;
esac