#!/bin/bash

# ===================================================================
# COMPREHENSIVE E2E CLEANUP SCRIPT
# 
# Safely stops and cleans up E2E testing environment
# Includes volume management, network cleanup, and resource monitoring
# ===================================================================

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Configuration
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.e2e.yml}"
PROJECT_NAME="focushive-e2e"
FORCE_CLEANUP=false
KEEP_VOLUMES=false
KEEP_IMAGES=false
KEEP_NETWORKS=false

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_section() {
    echo -e "\n${PURPLE}=== $1 ===${NC}"
}

log_step() {
    echo -e "${CYAN}[STEP]${NC} $1"
}

# Show current resource usage
show_current_usage() {
    log_section "Current Resource Usage"
    
    # Show running containers
    local running_containers=$(docker compose -f "$COMPOSE_FILE" ps -q 2>/dev/null | wc -l)
    if [ $running_containers -gt 0 ]; then
        log_info "Running containers: $running_containers"
        docker compose -f "$COMPOSE_FILE" ps --format table
        
        echo -e "\n${CYAN}Resource Stats:${NC}"
        docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}" $(docker compose -f "$COMPOSE_FILE" ps -q) 2>/dev/null || echo "Could not retrieve container stats"
    else
        log_info "No containers are currently running"
    fi
    
    # Show volumes
    local volumes=$(docker volume ls -q | grep -E "(focushive|e2e)" 2>/dev/null | wc -l)
    if [ $volumes -gt 0 ]; then
        log_info "E2E-related volumes: $volumes"
        docker volume ls | grep -E "(focushive|e2e)" || true
    fi
    
    # Show networks
    local networks=$(docker network ls -q | xargs docker network inspect 2>/dev/null | jq -r '.[] | select(.Name | contains("focushive") or contains("e2e")) | .Name' | wc -l)
    if [ $networks -gt 0 ]; then
        log_info "E2E-related networks: $networks"
        docker network ls | grep -E "(focushive|e2e)" || true
    fi
}

# Stop containers gracefully
stop_containers() {
    log_section "Stopping Containers"
    
    if [ ! -f "$COMPOSE_FILE" ]; then
        log_warning "Docker Compose file not found: $COMPOSE_FILE"
        return 0
    fi
    
    local running_containers=$(docker compose -f "$COMPOSE_FILE" ps -q 2>/dev/null)
    
    if [ -z "$running_containers" ]; then
        log_info "No containers are running"
        return 0
    fi
    
    log_step "Gracefully stopping containers..."
    
    # Stop services in reverse dependency order for cleaner shutdown
    local services_reverse=(
        "test-data-seeder"
        "frontend-e2e"
        "buddy-service"
        "forum-service"
        "analytics-service"
        "chat-service"
        "notification-service"
        "music-service"
        "focushive-backend"
        "identity-service"
        "email-mock"
        "spotify-mock"
        "test-redis"
        "test-db"
    )
    
    for service in "${services_reverse[@]}"; do
        local container_id=$(docker compose -f "$COMPOSE_FILE" ps -q "$service" 2>/dev/null)
        if [ -n "$container_id" ]; then
            log_step "Stopping $service..."
            docker compose -f "$COMPOSE_FILE" stop "$service" --timeout=30 2>/dev/null || true
        fi
    done
    
    # Force stop any remaining containers
    local remaining_containers=$(docker compose -f "$COMPOSE_FILE" ps -q 2>/dev/null)
    if [ -n "$remaining_containers" ]; then
        log_step "Force stopping remaining containers..."
        docker compose -f "$COMPOSE_FILE" kill 2>/dev/null || true
    fi
    
    log_success "All containers stopped"
}

# Remove containers
remove_containers() {
    log_section "Removing Containers"
    
    if [ ! -f "$COMPOSE_FILE" ]; then
        log_warning "Docker Compose file not found: $COMPOSE_FILE"
        return 0
    fi
    
    log_step "Removing containers..."
    docker compose -f "$COMPOSE_FILE" rm -f -v 2>/dev/null || true
    
    # Remove any orphaned containers
    log_step "Removing orphaned containers..."
    docker compose -f "$COMPOSE_FILE" down --remove-orphans 2>/dev/null || true
    
    # Clean up any remaining E2E containers
    local orphaned_containers=$(docker ps -aq --filter "label=com.docker.compose.project=$PROJECT_NAME" 2>/dev/null)
    if [ -n "$orphaned_containers" ]; then
        log_step "Removing orphaned E2E containers..."
        echo "$orphaned_containers" | xargs docker rm -f 2>/dev/null || true
    fi
    
    log_success "Containers removed"
}

# Clean up volumes
cleanup_volumes() {
    log_section "Cleaning Up Volumes"
    
    if [ "$KEEP_VOLUMES" = true ]; then
        log_info "Keeping volumes (--keep-volumes flag set)"
        return 0
    fi
    
    # List E2E volumes
    local e2e_volumes=$(docker volume ls -q | grep -E "(focushive.*e2e|.*test.*)" 2>/dev/null || true)
    
    if [ -z "$e2e_volumes" ]; then
        log_info "No E2E volumes found"
        return 0
    fi
    
    if [ "$FORCE_CLEANUP" = false ]; then
        echo -e "\n${YELLOW}The following volumes will be removed:${NC}"
        echo "$e2e_volumes" | sed 's/^/  - /'
        echo ""
        echo -e "${YELLOW}This will permanently delete all test data, including:${NC}"
        echo "  - Test database data"
        echo "  - Redis cache data"
        echo "  - Application logs"
        echo "  - Any uploaded files or generated content"
        echo ""
        echo -e "${RED}Are you sure you want to continue? (y/N)${NC}"
        read -r confirmation
        
        if [[ ! $confirmation =~ ^[Yy]$ ]]; then
            log_info "Volume cleanup cancelled"
            return 0
        fi
    fi
    
    log_step "Removing E2E volumes..."
    
    # Remove volumes one by one to avoid errors
    while IFS= read -r volume; do
        if [ -n "$volume" ]; then
            log_step "Removing volume: $volume"
            docker volume rm "$volume" 2>/dev/null || log_warning "Could not remove volume: $volume"
        fi
    done <<< "$e2e_volumes"
    
    # Remove any remaining project volumes
    docker compose -f "$COMPOSE_FILE" down --volumes 2>/dev/null || true
    
    log_success "Volumes cleaned up"
}

# Clean up Docker images
cleanup_images() {
    log_section "Cleaning Up Images"
    
    if [ "$KEEP_IMAGES" = true ]; then
        log_info "Keeping images (--keep-images flag set)"
        return 0
    fi
    
    # Find E2E-related images
    local e2e_images=$(docker images --format "{{.Repository}}:{{.Tag}}" | grep -E "(focushive|test)" | grep -v "<none>" || true)
    
    if [ -z "$e2e_images" ]; then
        log_info "No E2E images found"
        return 0
    fi
    
    if [ "$FORCE_CLEANUP" = false ]; then
        echo -e "\n${YELLOW}The following images will be removed:${NC}"
        echo "$e2e_images" | sed 's/^/  - /'
        echo ""
        echo -e "${YELLOW}This will require rebuilding images for next E2E run.${NC}"
        echo -e "${RED}Continue? (y/N)${NC}"
        read -r confirmation
        
        if [[ ! $confirmation =~ ^[Yy]$ ]]; then
            log_info "Image cleanup cancelled"
            return 0
        fi
    fi
    
    log_step "Removing E2E images..."
    
    # Remove images one by one
    while IFS= read -r image; do
        if [ -n "$image" ]; then
            log_step "Removing image: $image"
            docker rmi "$image" 2>/dev/null || log_warning "Could not remove image: $image"
        fi
    done <<< "$e2e_images"
    
    log_success "Images cleaned up"
}

# Clean up networks
cleanup_networks() {
    log_section "Cleaning Up Networks"
    
    if [ "$KEEP_NETWORKS" = true ]; then
        log_info "Keeping networks (--keep-networks flag set)"
        return 0
    fi
    
    # Remove compose networks
    if [ -f "$COMPOSE_FILE" ]; then
        docker compose -f "$COMPOSE_FILE" down 2>/dev/null || true
    fi
    
    # Find and remove E2E networks
    local e2e_networks=$(docker network ls --filter "name=focushive" --filter "name=e2e" -q 2>/dev/null || true)
    
    if [ -n "$e2e_networks" ]; then
        log_step "Removing E2E networks..."
        echo "$e2e_networks" | xargs docker network rm 2>/dev/null || log_warning "Some networks could not be removed"
        log_success "Networks cleaned up"
    else
        log_info "No E2E networks found"
    fi
}

# Clean up temporary files and logs
cleanup_files() {
    log_section "Cleaning Up Files"
    
    local cleanup_paths=(
        "logs/e2e"
        "test-reports"
        "frontend/e2e/results"
        "frontend/playwright-report"
        "frontend/test-results"
        "/tmp/seed-*.sql"
    )
    
    for path in "${cleanup_paths[@]}"; do
        if [ -e "$path" ]; then
            log_step "Cleaning up: $path"
            if [ -d "$path" ]; then
                rm -rf "$path"/* 2>/dev/null || true
            else
                rm -f "$path" 2>/dev/null || true
            fi
        fi
    done
    
    # Clean up any leftover environment files if not in use
    if [ "$FORCE_CLEANUP" = true ] && [ -f ".env.e2e" ]; then
        log_step "Removing .env.e2e file"
        rm -f .env.e2e
    fi
    
    log_success "Temporary files cleaned up"
}

# Docker system cleanup
docker_system_cleanup() {
    log_section "Docker System Cleanup"
    
    if [ "$FORCE_CLEANUP" = false ]; then
        echo -e "${YELLOW}Perform Docker system cleanup? This will:${NC}"
        echo "  - Remove all dangling images"
        echo "  - Remove unused networks"
        echo "  - Remove unused volumes (not associated with containers)"
        echo "  - Remove build cache"
        echo ""
        echo -e "${YELLOW}Continue? (y/N)${NC}"
        read -r confirmation
        
        if [[ ! $confirmation =~ ^[Yy]$ ]]; then
            log_info "Docker system cleanup cancelled"
            return 0
        fi
    fi
    
    log_step "Running Docker system cleanup..."
    
    # Clean up dangling images
    docker image prune -f 2>/dev/null || true
    
    # Clean up unused networks
    docker network prune -f 2>/dev/null || true
    
    # Clean up unused volumes (be careful with this)
    if [ "$FORCE_CLEANUP" = true ]; then
        docker volume prune -f 2>/dev/null || true
    fi
    
    # Clean up build cache
    docker builder prune -f 2>/dev/null || true
    
    log_success "Docker system cleanup completed"
}

# Show cleanup results
show_cleanup_results() {
    log_section "Cleanup Results"
    
    # Check remaining resources
    local remaining_containers=$(docker ps -aq --filter "label=com.docker.compose.project=$PROJECT_NAME" 2>/dev/null | wc -l)
    local remaining_volumes=$(docker volume ls -q | grep -E "(focushive.*e2e|.*test.*)" 2>/dev/null | wc -l)
    local remaining_images=$(docker images --format "{{.Repository}}:{{.Tag}}" | grep -E "(focushive|test)" | wc -l)
    local remaining_networks=$(docker network ls --filter "name=focushive" --filter "name=e2e" -q 2>/dev/null | wc -l)
    
    echo -e "${CYAN}Cleanup Summary:${NC}"
    echo "  Containers: $remaining_containers remaining"
    echo "  Volumes: $remaining_volumes remaining"
    echo "  Images: $remaining_images remaining"
    echo "  Networks: $remaining_networks remaining"
    
    if [ $remaining_containers -eq 0 ] && [ $remaining_networks -eq 0 ]; then
        log_success "✅ E2E environment completely cleaned up"
    elif [ $remaining_containers -eq 0 ]; then
        log_success "✅ E2E containers and networks cleaned up"
        log_info "Some volumes or images remain (use --force for complete cleanup)"
    else
        log_warning "⚠️ Some resources may still remain"
        echo "Run 'docker ps -a' and 'docker volume ls' to check"
    fi
    
    # Show disk space freed (approximate)
    log_info "To see disk space freed, run: docker system df"
}

# Handle script interruption
cleanup_on_interrupt() {
    echo -e "\n\n${YELLOW}Cleanup interrupted!${NC}"
    log_warning "Some resources may not have been cleaned up properly"
    log_info "Re-run this script to complete cleanup"
    exit 130
}

# Parse command line arguments
parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --force|-f)
                FORCE_CLEANUP=true
                shift
                ;;
            --keep-volumes)
                KEEP_VOLUMES=true
                shift
                ;;
            --keep-images)
                KEEP_IMAGES=true
                shift
                ;;
            --keep-networks)
                KEEP_NETWORKS=true
                shift
                ;;
            --compose-file)
                COMPOSE_FILE="$2"
                shift 2
                ;;
            --help|-h)
                show_help
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done
}

# Show help
show_help() {
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -f, --force           Force cleanup without confirmation prompts"
    echo "  --keep-volumes        Keep Docker volumes (preserve test data)"
    echo "  --keep-images         Keep Docker images (faster next startup)"
    echo "  --keep-networks       Keep Docker networks"
    echo "  --compose-file FILE   Use specific Docker Compose file"
    echo "  -h, --help           Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                    # Interactive cleanup (asks for confirmation)"
    echo "  $0 --force            # Force complete cleanup"
    echo "  $0 --keep-volumes     # Clean up but preserve test data"
    echo "  $0 --keep-images      # Clean up but keep images for faster restart"
    echo ""
    echo "Environment Variables:"
    echo "  COMPOSE_FILE          Docker Compose file (default: docker-compose.e2e.yml)"
}

# Main cleanup function
main() {
    parse_arguments "$@"
    
    echo "═════════════════════════════════════════"
    echo "🧹 FocusHive E2E Environment Cleanup"
    echo "═════════════════════════════════════════"
    echo ""
    
    # Set up signal handlers
    trap cleanup_on_interrupt INT TERM
    
    if [ "$FORCE_CLEANUP" = false ]; then
        show_current_usage
        
        echo ""
        echo -e "${YELLOW}This will clean up the E2E testing environment.${NC}"
        echo -e "${YELLOW}Continue? (y/N)${NC}"
        read -r confirmation
        
        if [[ ! $confirmation =~ ^[Yy]$ ]]; then
            log_info "Cleanup cancelled"
            exit 0
        fi
    fi
    
    stop_containers
    remove_containers
    cleanup_volumes
    cleanup_images
    cleanup_networks
    cleanup_files
    
    if [ "$FORCE_CLEANUP" = true ]; then
        docker_system_cleanup
    fi
    
    show_cleanup_results
    
    log_success "🎉 E2E environment cleanup completed!"
    echo ""
    echo "📋 Next steps:"
    echo "  Start new environment: ./scripts/setup-e2e-env.sh"
    echo "  Check Docker resources: docker system df"
    echo "  Full Docker cleanup: docker system prune -a --volumes"
}

# Run main function
main "$@"