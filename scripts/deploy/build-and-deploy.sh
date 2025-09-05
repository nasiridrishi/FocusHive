#!/bin/bash

# ============================================================================
# FocusHive Build and Deployment Script - 2025 Edition
# Comprehensive Docker-based deployment automation with health checks
# ============================================================================

set -euo pipefail  # Exit on error, undefined vars, pipe failures

# ============================================================================
# Configuration and Constants
# ============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
DOCKER_DIR="${PROJECT_ROOT}/docker"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Default values
ENVIRONMENT="development"
REGISTRY=""
TAG="latest"
PUSH_TO_REGISTRY=false
RUN_TESTS=false
SKIP_BUILD=false
SERVICES=""
TIMEOUT=300
VERBOSE=false
DRY_RUN=false

# Services array
ALL_SERVICES=(
    "identity-service"
    "focushive-backend"
    "music-service"
    "notification-service"
    "chat-service"
    "analytics-service"
    "forum-service"
    "buddy-service"
    "web"
)

# ============================================================================
# Utility Functions
# ============================================================================

log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] $*${NC}"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING: $*${NC}"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $*${NC}"
}

info() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] INFO: $*${NC}"
}

debug() {
    if [[ "${VERBOSE}" == "true" ]]; then
        echo -e "${PURPLE}[$(date +'%Y-%m-%d %H:%M:%S')] DEBUG: $*${NC}"
    fi
}

step() {
    echo -e "${CYAN}[$(date +'%Y-%m-%d %H:%M:%S')] STEP: $*${NC}"
}

show_usage() {
    cat << EOF
Usage: $0 [OPTIONS]

DESCRIPTION:
    Comprehensive build and deployment script for FocusHive application.
    Supports multiple environments, selective service deployment, and automated testing.

OPTIONS:
    -e, --environment ENV       Target environment (development|staging|production) [default: development]
    -r, --registry REGISTRY     Docker registry URL for pushing images
    -t, --tag TAG              Docker image tag [default: latest]
    -s, --services SERVICES    Comma-separated list of services to build/deploy (all if not specified)
    -p, --push                 Push images to registry after building
    -T, --test                 Run tests before deployment
    --skip-build               Skip the build phase (useful for deployment only)
    --timeout SECONDS          Health check timeout in seconds [default: 300]
    --dry-run                  Show what would be done without executing
    -v, --verbose              Enable verbose logging
    -h, --help                 Show this help message

EXAMPLES:
    # Development deployment (default)
    $0

    # Production deployment with registry push
    $0 -e production -r docker.io/focushive -t v1.0.0 -p

    # Deploy specific services
    $0 -s "identity-service,focushive-backend" -e staging

    # Run tests and deploy
    $0 -T -e production

    # Dry run to see what would happen
    $0 --dry-run -e production -r docker.io/focushive -t v1.0.0 -p

ENVIRONMENT FILES:
    The script will look for environment files in the following order:
    1. ${DOCKER_DIR}/.env.{environment}
    2. ${DOCKER_DIR}/.env
    3. Default values

SERVICES:
    Available services: ${ALL_SERVICES[*]}

EOF
}

# ============================================================================
# Environment and Validation Functions
# ============================================================================

validate_environment() {
    local env="$1"
    case "$env" in
        development|staging|production|test)
            return 0
            ;;
        *)
            error "Invalid environment: $env"
            error "Valid environments: development, staging, production, test"
            exit 1
            ;;
    esac
}

load_environment() {
    local env="$1"
    
    # Try environment-specific file first
    local env_file="${DOCKER_DIR}/.env.${env}"
    if [[ -f "$env_file" ]]; then
        info "Loading environment from $env_file"
        set -a  # Automatically export all variables
        source "$env_file"
        set +a
    else
        # Fall back to default .env
        local default_env="${DOCKER_DIR}/.env"
        if [[ -f "$default_env" ]]; then
            info "Loading default environment from $default_env"
            set -a
            source "$default_env"
            set +a
        else
            warn "No environment file found, using defaults"
        fi
    fi
}

check_prerequisites() {
    step "Checking prerequisites"
    
    local missing_tools=()
    
    # Check for required tools
    for tool in docker docker-compose jq curl; do
        if ! command -v "$tool" &> /dev/null; then
            missing_tools+=("$tool")
        fi
    done
    
    if [[ ${#missing_tools[@]} -gt 0 ]]; then
        error "Missing required tools: ${missing_tools[*]}"
        error "Please install missing tools and try again"
        exit 1
    fi
    
    # Check Docker daemon
    if ! docker info &> /dev/null; then
        error "Docker daemon is not running"
        exit 1
    fi
    
    # Check for compose file
    local compose_file="${DOCKER_DIR}/docker-compose.yml"
    if [[ ! -f "$compose_file" ]]; then
        error "Docker Compose file not found: $compose_file"
        exit 1
    fi
    
    log "Prerequisites check passed"
}

validate_services() {
    local services="$1"
    if [[ -z "$services" ]]; then
        return 0  # Empty means all services
    fi
    
    IFS=',' read -ra SERVICE_LIST <<< "$services"
    for service in "${SERVICE_LIST[@]}"; do
        if [[ ! " ${ALL_SERVICES[*]} " =~ " ${service} " ]]; then
            error "Invalid service: $service"
            error "Available services: ${ALL_SERVICES[*]}"
            exit 1
        fi
    done
}

# ============================================================================
# Docker and Build Functions
# ============================================================================

build_services() {
    step "Building Docker images"
    
    local services_arg=""
    if [[ -n "$SERVICES" ]]; then
        services_arg="$SERVICES"
        info "Building selected services: $SERVICES"
    else
        info "Building all services"
    fi
    
    local compose_files=("-f" "${DOCKER_DIR}/docker-compose.yml")
    
    # Add environment-specific compose file if it exists
    if [[ "$ENVIRONMENT" != "development" ]]; then
        local env_compose="${DOCKER_DIR}/docker-compose.${ENVIRONMENT}.yml"
        if [[ -f "$env_compose" ]]; then
            compose_files+=("-f" "$env_compose")
            debug "Added environment compose file: $env_compose"
        fi
    fi
    
    if [[ "$DRY_RUN" == "true" ]]; then
        info "DRY RUN: Would execute: docker-compose ${compose_files[*]} build ${services_arg}"
        return 0
    fi
    
    cd "$DOCKER_DIR"
    
    # Build with progress output
    if [[ "$VERBOSE" == "true" ]]; then
        docker-compose "${compose_files[@]}" build --progress=plain $services_arg
    else
        docker-compose "${compose_files[@]}" build $services_arg
    fi
    
    log "Build completed successfully"
}

tag_images() {
    if [[ -z "$REGISTRY" ]]; then
        debug "No registry specified, skipping image tagging"
        return 0
    fi
    
    step "Tagging images for registry"
    
    local services_to_tag
    if [[ -n "$SERVICES" ]]; then
        IFS=',' read -ra services_to_tag <<< "$SERVICES"
    else
        services_to_tag=("${ALL_SERVICES[@]}")
    fi
    
    for service in "${services_to_tag[@]}"; do
        local local_image="focushive-${service}:latest"
        local registry_image="${REGISTRY}/${service}:${TAG}"
        
        if [[ "$DRY_RUN" == "true" ]]; then
            info "DRY RUN: Would tag $local_image as $registry_image"
        else
            debug "Tagging $local_image as $registry_image"
            docker tag "$local_image" "$registry_image"
        fi
    done
    
    log "Image tagging completed"
}

push_images() {
    if [[ "$PUSH_TO_REGISTRY" != "true" ]]; then
        debug "Registry push not requested, skipping"
        return 0
    fi
    
    if [[ -z "$REGISTRY" ]]; then
        error "Registry push requested but no registry specified"
        exit 1
    fi
    
    step "Pushing images to registry: $REGISTRY"
    
    local services_to_push
    if [[ -n "$SERVICES" ]]; then
        IFS=',' read -ra services_to_push <<< "$SERVICES"
    else
        services_to_push=("${ALL_SERVICES[@]}")
    fi
    
    for service in "${services_to_push[@]}"; do
        local registry_image="${REGISTRY}/${service}:${TAG}"
        
        if [[ "$DRY_RUN" == "true" ]]; then
            info "DRY RUN: Would push $registry_image"
        else
            info "Pushing $registry_image"
            docker push "$registry_image"
        fi
    done
    
    log "Registry push completed"
}

# ============================================================================
# Testing Functions
# ============================================================================

run_tests() {
    if [[ "$RUN_TESTS" != "true" ]]; then
        debug "Tests not requested, skipping"
        return 0
    fi
    
    step "Running tests"
    
    if [[ "$DRY_RUN" == "true" ]]; then
        info "DRY RUN: Would run test suite"
        return 0
    fi
    
    cd "$DOCKER_DIR"
    
    # Run unit tests first
    info "Running unit tests"
    docker-compose -f docker-compose.yml -f docker-compose.test.yml run --rm integration-tests
    
    # Run integration tests
    info "Running integration tests"
    docker-compose -f docker-compose.yml -f docker-compose.test.yml --profile integration-tests up --abort-on-container-exit
    
    log "All tests passed"
}

# ============================================================================
# Deployment Functions
# ============================================================================

deploy_services() {
    step "Deploying services to $ENVIRONMENT environment"
    
    cd "$DOCKER_DIR"
    
    local compose_files=("-f" "docker-compose.yml")
    
    # Add environment-specific compose file
    if [[ "$ENVIRONMENT" != "development" ]]; then
        local env_compose="docker-compose.${ENVIRONMENT}.yml"
        if [[ -f "$env_compose" ]]; then
            compose_files+=("-f" "$env_compose")
        fi
    fi
    
    local services_arg=""
    if [[ -n "$SERVICES" ]]; then
        services_arg="$SERVICES"
        info "Deploying selected services: $SERVICES"
    else
        info "Deploying all services"
    fi
    
    if [[ "$DRY_RUN" == "true" ]]; then
        info "DRY RUN: Would execute: docker-compose ${compose_files[*]} up -d ${services_arg}"
        return 0
    fi
    
    # Deploy with rolling updates
    docker-compose "${compose_files[@]}" up -d $services_arg
    
    log "Deployment completed"
}

wait_for_health_checks() {
    step "Waiting for services to be healthy"
    
    local services_to_check
    if [[ -n "$SERVICES" ]]; then
        IFS=',' read -ra services_to_check <<< "$SERVICES"
    else
        services_to_check=("${ALL_SERVICES[@]}")
    fi
    
    local start_time=$(date +%s)
    local healthy_services=()
    local max_attempts=$((TIMEOUT / 10))  # Check every 10 seconds
    
    if [[ "$DRY_RUN" == "true" ]]; then
        info "DRY RUN: Would wait for health checks on services: ${services_to_check[*]}"
        return 0
    fi
    
    for attempt in $(seq 1 "$max_attempts"); do
        debug "Health check attempt $attempt of $max_attempts"
        
        local all_healthy=true
        
        for service in "${services_to_check[@]}"; do
            if [[ " ${healthy_services[*]} " =~ " ${service} " ]]; then
                continue  # Already healthy
            fi
            
            local container_name="focushive-${service}"
            if [[ "$service" == "focushive-backend" ]]; then
                container_name="focushive-backend"
            elif [[ "$service" == "web" ]]; then
                container_name="focushive-web"
            fi
            
            local health_status
            health_status=$(docker inspect --format='{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "no-container")
            
            case "$health_status" in
                "healthy")
                    if [[ ! " ${healthy_services[*]} " =~ " ${service} " ]]; then
                        healthy_services+=("$service")
                        log "✓ $service is healthy"
                    fi
                    ;;
                "unhealthy")
                    warn "✗ $service is unhealthy"
                    all_healthy=false
                    ;;
                "starting")
                    debug "⏳ $service is starting"
                    all_healthy=false
                    ;;
                "no-container")
                    debug "🔍 $service container not found or no health check"
                    all_healthy=false
                    ;;
                *)
                    warn "❓ $service has unknown health status: $health_status"
                    all_healthy=false
                    ;;
            esac
        done
        
        if [[ "$all_healthy" == "true" ]]; then
            log "All services are healthy!"
            return 0
        fi
        
        sleep 10
    done
    
    error "Timeout waiting for services to become healthy after ${TIMEOUT} seconds"
    
    # Show detailed status for debugging
    error "Final health status:"
    for service in "${services_to_check[@]}"; do
        local container_name="focushive-${service}"
        if [[ "$service" == "focushive-backend" ]]; then
            container_name="focushive-backend"
        elif [[ "$service" == "web" ]]; then
            container_name="focushive-web"
        fi
        
        local health_status
        health_status=$(docker inspect --format='{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "no-container")
        error "  $service: $health_status"
        
        # Show recent logs for unhealthy services
        if [[ "$health_status" == "unhealthy" ]]; then
            error "Recent logs for $service:"
            docker logs --tail=20 "$container_name" 2>&1 | sed 's/^/    /'
        fi
    done
    
    exit 1
}

# ============================================================================
# Cleanup and Monitoring Functions
# ============================================================================

cleanup_old_images() {
    step "Cleaning up old Docker images"
    
    if [[ "$DRY_RUN" == "true" ]]; then
        info "DRY RUN: Would clean up old Docker images"
        return 0
    fi
    
    # Remove dangling images
    local dangling_images
    dangling_images=$(docker images -f "dangling=true" -q)
    if [[ -n "$dangling_images" ]]; then
        info "Removing dangling images"
        docker rmi $dangling_images
    fi
    
    # Remove old images (keep last 3 versions)
    for service in "${ALL_SERVICES[@]}"; do
        local old_images
        old_images=$(docker images "focushive-${service}" --format "table {{.Repository}}:{{.Tag}}\t{{.CreatedAt}}" | tail -n +2 | sort -k2 -r | tail -n +4 | awk '{print $1}')
        if [[ -n "$old_images" ]]; then
            info "Removing old images for $service"
            echo "$old_images" | xargs -r docker rmi
        fi
    done
    
    log "Image cleanup completed"
}

show_deployment_status() {
    step "Deployment Status Summary"
    
    cd "$DOCKER_DIR"
    
    echo ""
    echo "=== CONTAINER STATUS ==="
    docker-compose ps
    
    echo ""
    echo "=== SERVICE URLS ==="
    echo "Frontend: http://localhost:${FRONTEND_PORT:-5173}"
    echo "Backend API: http://localhost:${BACKEND_PORT:-8080}"
    echo "Identity Service: http://localhost:${IDENTITY_SERVICE_PORT:-8081}"
    echo "API Gateway: http://localhost:${NGINX_HTTP_PORT:-80}"
    
    if [[ "$ENVIRONMENT" == "production" ]]; then
        echo "Prometheus: http://localhost:9090"
        echo "Grafana: http://localhost:3000"
    fi
    
    echo ""
    echo "=== HEALTH CHECK ENDPOINTS ==="
    echo "Backend Health: http://localhost:${BACKEND_PORT:-8080}/actuator/health"
    echo "Identity Health: http://localhost:${IDENTITY_SERVICE_PORT:-8081}/api/v1/health"
    
    echo ""
    echo "=== LOGS ==="
    echo "View logs with: docker-compose logs -f [service-name]"
    echo "Available services: ${ALL_SERVICES[*]}"
}

# ============================================================================
# Main Execution Flow
# ============================================================================

main() {
    step "Starting FocusHive deployment process"
    
    # Load environment configuration
    load_environment "$ENVIRONMENT"
    
    # Run prerequisite checks
    check_prerequisites
    
    # Validate services if specified
    validate_services "$SERVICES"
    
    # Show configuration summary
    info "Configuration Summary:"
    info "  Environment: $ENVIRONMENT"
    info "  Registry: ${REGISTRY:-'(none)'}"
    info "  Tag: $TAG"
    info "  Services: ${SERVICES:-'(all)'}"
    info "  Push to Registry: $PUSH_TO_REGISTRY"
    info "  Run Tests: $RUN_TESTS"
    info "  Skip Build: $SKIP_BUILD"
    info "  Timeout: ${TIMEOUT}s"
    info "  Dry Run: $DRY_RUN"
    
    if [[ "$DRY_RUN" == "true" ]]; then
        warn "DRY RUN MODE - No actual changes will be made"
    fi
    
    # Execute deployment pipeline
    if [[ "$RUN_TESTS" == "true" ]]; then
        run_tests
    fi
    
    if [[ "$SKIP_BUILD" != "true" ]]; then
        build_services
        tag_images
        push_images
    fi
    
    deploy_services
    wait_for_health_checks
    cleanup_old_images
    
    log "🎉 Deployment completed successfully!"
    show_deployment_status
}

# ============================================================================
# Argument Parsing
# ============================================================================

while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENVIRONMENT="$2"
            validate_environment "$ENVIRONMENT"
            shift 2
            ;;
        -r|--registry)
            REGISTRY="$2"
            shift 2
            ;;
        -t|--tag)
            TAG="$2"
            shift 2
            ;;
        -s|--services)
            SERVICES="$2"
            shift 2
            ;;
        -p|--push)
            PUSH_TO_REGISTRY=true
            shift
            ;;
        -T|--test)
            RUN_TESTS=true
            shift
            ;;
        --skip-build)
            SKIP_BUILD=true
            shift
            ;;
        --timeout)
            TIMEOUT="$2"
            shift 2
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        *)
            error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# ============================================================================
# Script Entry Point
# ============================================================================

# Trap for cleanup
trap 'error "Script interrupted"; exit 130' INT TERM

# Execute main function
main "$@"