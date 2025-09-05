#!/bin/bash

# FocusHive Docker Stop Script
# Gracefully stops all running containers

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_message() {
    echo -e "${2}${1}${NC}"
}

# Function to stop services
stop_services() {
    print_message "Stopping FocusHive services..." "$YELLOW"
    
    # Stop containers gracefully with timeout
    docker-compose stop -t 30
    
    print_message "Services stopped successfully!" "$GREEN"
}

# Function to show container status
show_status() {
    print_message "\nContainer Status:" "$YELLOW"
    docker-compose ps
}

# Main execution
main() {
    print_message "🛑 Stopping FocusHive Platform" "$RED"
    print_message "================================" "$RED"
    
    stop_services
    show_status
    
    print_message "\n✅ FocusHive has been stopped!" "$GREEN"
    print_message "To remove containers completely, run: ./docker/scripts/cleanup.sh" "$YELLOW"
    print_message "To restart services, run: ./docker/scripts/start.sh" "$YELLOW"
}

# Run main function
main