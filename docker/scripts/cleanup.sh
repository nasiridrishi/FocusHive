#!/bin/bash

# FocusHive Docker Cleanup Script
# Removes containers, networks, and optionally volumes

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

# Function to confirm action
confirm() {
    read -p "$1 [y/N]: " -n 1 -r
    echo
    [[ $REPLY =~ ^[Yy]$ ]]
}

# Function to stop and remove containers
remove_containers() {
    print_message "Removing containers..." "$YELLOW"
    docker-compose down
    print_message "Containers removed!" "$GREEN"
}

# Function to remove volumes
remove_volumes() {
    if confirm "Do you want to remove data volumes? (This will delete all data!)"; then
        print_message "Removing volumes..." "$RED"
        docker-compose down -v
        print_message "Volumes removed!" "$GREEN"
    else
        print_message "Volumes preserved." "$YELLOW"
    fi
}

# Function to remove images
remove_images() {
    if confirm "Do you want to remove Docker images?"; then
        print_message "Removing images..." "$YELLOW"
        docker-compose down --rmi local
        print_message "Images removed!" "$GREEN"
    else
        print_message "Images preserved." "$YELLOW"
    fi
}

# Function to clean build cache
clean_cache() {
    if confirm "Do you want to clean Docker build cache?"; then
        print_message "Cleaning build cache..." "$YELLOW"
        docker builder prune -f
        print_message "Build cache cleaned!" "$GREEN"
    fi
}

# Function to show disk usage
show_disk_usage() {
    print_message "\nDocker Disk Usage:" "$YELLOW"
    docker system df
}

# Main execution
main() {
    print_message "🧹 FocusHive Docker Cleanup" "$YELLOW"
    print_message "================================" "$YELLOW"
    
    if ! confirm "This will stop and remove FocusHive containers. Continue?"; then
        print_message "Cleanup cancelled." "$YELLOW"
        exit 0
    fi
    
    remove_containers
    remove_volumes
    remove_images
    clean_cache
    show_disk_usage
    
    print_message "\n✅ Cleanup completed!" "$GREEN"
    print_message "To start fresh, run: ./docker/scripts/start.sh" "$YELLOW"
}

# Run main function
main