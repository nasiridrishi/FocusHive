#!/bin/bash

# ============================================================================
# Docker Registry Management Script for FocusHive
# Handles image building, tagging, and pushing to registries
# ============================================================================

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
DOCKER_DIR="${PROJECT_ROOT}/docker"

# Default values
REGISTRY=""
REGISTRY_USERNAME=""
REGISTRY_PASSWORD=""
TAG="latest"
BUILD_ARGS=""
PLATFORM="linux/amd64"
PUSH=false
FORCE_BUILD=false
NO_CACHE=false

# Services
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

show_usage() {
    cat << EOF
Usage: $0 [OPTIONS] COMMAND

DESCRIPTION:
    Docker registry management for FocusHive services.
    Build, tag, and push container images to registries.

COMMANDS:
    build       Build Docker images locally
    tag         Tag images for registry
    push        Push images to registry
    login       Login to registry
    logout      Logout from registry
    list        List available images
    clean       Clean old images
    build-push  Build and push in one command

OPTIONS:
    -r, --registry REGISTRY     Registry URL (e.g., docker.io, ghcr.io, localhost:5000)
    -u, --username USERNAME     Registry username
    -p, --password PASSWORD     Registry password (use env REGISTRY_PASSWORD instead)
    -t, --tag TAG              Image tag [default: latest]
    -s, --services SERVICES    Comma-separated services to process [default: all]
    --platform PLATFORM        Target platform [default: linux/amd64]
    --build-arg KEY=VALUE      Build argument (can be used multiple times)
    --no-cache                 Build without cache
    --force                    Force rebuild even if image exists
    --push                     Push after building (for build command)
    -h, --help                 Show this help

EXAMPLES:
    # Build all services
    $0 build

    # Build and push to Docker Hub
    $0 -r docker.io/myuser -t v1.0.0 --push build

    # Build specific services with custom args
    $0 -s "identity-service,web" --build-arg NODE_ENV=production build

    # Push to GitHub Container Registry
    $0 -r ghcr.io/myorg/focushive -t latest push

    # Multi-platform build
    $0 --platform linux/amd64,linux/arm64 -r myregistry.com build-push

SUPPORTED REGISTRIES:
    - Docker Hub: docker.io/username
    - GitHub Container Registry: ghcr.io/username
    - Google Container Registry: gcr.io/project-id
    - Amazon ECR: account-id.dkr.ecr.region.amazonaws.com
    - Azure Container Registry: myregistry.azurecr.io
    - Local registry: localhost:5000

EOF
}

# ============================================================================
# Registry Functions
# ============================================================================

login_registry() {
    if [[ -z "$REGISTRY" ]]; then
        error "Registry not specified"
        exit 1
    fi
    
    info "Logging into registry: $REGISTRY"
    
    if [[ -n "$REGISTRY_USERNAME" ]]; then
        if [[ -n "$REGISTRY_PASSWORD" ]]; then
            echo "$REGISTRY_PASSWORD" | docker login "$REGISTRY" --username "$REGISTRY_USERNAME" --password-stdin
        else
            docker login "$REGISTRY" --username "$REGISTRY_USERNAME"
        fi
    else
        docker login "$REGISTRY"
    fi
    
    log "Successfully logged into $REGISTRY"
}

logout_registry() {
    if [[ -z "$REGISTRY" ]]; then
        error "Registry not specified"
        exit 1
    fi
    
    info "Logging out from registry: $REGISTRY"
    docker logout "$REGISTRY"
    log "Successfully logged out from $REGISTRY"
}

# ============================================================================
# Build Functions
# ============================================================================

build_service() {
    local service="$1"
    local service_path
    
    # Determine service path
    if [[ "$service" == "web" ]]; then
        service_path="${PROJECT_ROOT}/frontend"
    else
        service_path="${PROJECT_ROOT}/services/${service}"
    fi
    
    if [[ ! -d "$service_path" ]]; then
        error "Service directory not found: $service_path"
        return 1
    fi
    
    if [[ ! -f "$service_path/Dockerfile" ]]; then
        error "Dockerfile not found for service: $service"
        return 1
    fi
    
    local image_name="focushive-${service}"
    local full_image_name="${image_name}:${TAG}"
    
    if [[ -n "$REGISTRY" ]]; then
        full_image_name="${REGISTRY}/${service}:${TAG}"
    fi
    
    # Check if image exists and force build not requested
    if [[ "$FORCE_BUILD" != "true" ]] && docker image inspect "$full_image_name" &>/dev/null; then
        warn "Image $full_image_name already exists (use --force to rebuild)"
        return 0
    fi
    
    info "Building $service -> $full_image_name"
    
    local build_cmd=(docker build)
    
    # Add build arguments
    if [[ -n "$BUILD_ARGS" ]]; then
        IFS=' ' read -ra ARGS <<< "$BUILD_ARGS"
        for arg in "${ARGS[@]}"; do
            build_cmd+=(--build-arg "$arg")
        done
    fi
    
    # Add platform if specified
    if [[ -n "$PLATFORM" ]]; then
        build_cmd+=(--platform "$PLATFORM")
    fi
    
    # Add no-cache if requested
    if [[ "$NO_CACHE" == "true" ]]; then
        build_cmd+=(--no-cache)
    fi
    
    # Add progress
    build_cmd+=(--progress=plain)
    
    # Add tags
    build_cmd+=(-t "$full_image_name")
    
    # If registry specified, also tag with local name for compatibility
    if [[ -n "$REGISTRY" ]]; then
        build_cmd+=(-t "${image_name}:${TAG}")
    fi
    
    # Add context path
    build_cmd+=("$service_path")
    
    # Execute build
    "${build_cmd[@]}"
    
    log "Successfully built $full_image_name"
}

build_images() {
    local services_to_build=("${ALL_SERVICES[@]}")
    
    if [[ -n "$SERVICES" ]]; then
        IFS=',' read -ra services_to_build <<< "$SERVICES"
    fi
    
    info "Building ${#services_to_build[@]} services: ${services_to_build[*]}"
    
    local failed_builds=()
    
    for service in "${services_to_build[@]}"; do
        if ! build_service "$service"; then
            failed_builds+=("$service")
        fi
    done
    
    if [[ ${#failed_builds[@]} -gt 0 ]]; then
        error "Failed to build services: ${failed_builds[*]}"
        exit 1
    fi
    
    log "All services built successfully"
}

# ============================================================================
# Tag Functions
# ============================================================================

tag_service() {
    local service="$1"
    local source_image="focushive-${service}:${TAG}"
    local target_image="${REGISTRY}/${service}:${TAG}"
    
    if ! docker image inspect "$source_image" &>/dev/null; then
        error "Source image not found: $source_image"
        return 1
    fi
    
    info "Tagging $source_image -> $target_image"
    docker tag "$source_image" "$target_image"
    
    log "Successfully tagged $target_image"
}

tag_images() {
    if [[ -z "$REGISTRY" ]]; then
        error "Registry must be specified for tagging"
        exit 1
    fi
    
    local services_to_tag=("${ALL_SERVICES[@]}")
    
    if [[ -n "$SERVICES" ]]; then
        IFS=',' read -ra services_to_tag <<< "$SERVICES"
    fi
    
    info "Tagging ${#services_to_tag[@]} services for registry: $REGISTRY"
    
    local failed_tags=()
    
    for service in "${services_to_tag[@]}"; do
        if ! tag_service "$service"; then
            failed_tags+=("$service")
        fi
    done
    
    if [[ ${#failed_tags[@]} -gt 0 ]]; then
        error "Failed to tag services: ${failed_tags[*]}"
        exit 1
    fi
    
    log "All services tagged successfully"
}

# ============================================================================
# Push Functions
# ============================================================================

push_service() {
    local service="$1"
    local image_name="${REGISTRY}/${service}:${TAG}"
    
    if ! docker image inspect "$image_name" &>/dev/null; then
        error "Image not found: $image_name"
        error "Did you build and tag the image first?"
        return 1
    fi
    
    info "Pushing $image_name"
    docker push "$image_name"
    
    log "Successfully pushed $image_name"
}

push_images() {
    if [[ -z "$REGISTRY" ]]; then
        error "Registry must be specified for pushing"
        exit 1
    fi
    
    local services_to_push=("${ALL_SERVICES[@]}")
    
    if [[ -n "$SERVICES" ]]; then
        IFS=',' read -ra services_to_push <<< "$SERVICES"
    fi
    
    info "Pushing ${#services_to_push[@]} services to registry: $REGISTRY"
    
    # Ensure we're logged in
    if ! docker info | grep -q "Registry:"; then
        warn "Not logged into registry, attempting login"
        login_registry
    fi
    
    local failed_pushes=()
    
    for service in "${services_to_push[@]}"; do
        if ! push_service "$service"; then
            failed_pushes+=("$service")
        fi
    done
    
    if [[ ${#failed_pushes[@]} -gt 0 ]]; then
        error "Failed to push services: ${failed_pushes[*]}"
        exit 1
    fi
    
    log "All services pushed successfully"
}

# ============================================================================
# Utility Functions
# ============================================================================

list_images() {
    info "Available FocusHive images:"
    echo ""
    
    # Show local images
    echo "=== Local Images ==="
    docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}" | grep -E "(focushive|REPOSITORY)"
    
    # Show registry images if registry specified
    if [[ -n "$REGISTRY" ]]; then
        echo ""
        echo "=== Registry Images ($REGISTRY) ==="
        for service in "${ALL_SERVICES[@]}"; do
            local image_name="${REGISTRY}/${service}"
            echo "Checking $image_name..."
            docker manifest inspect "$image_name:$TAG" &>/dev/null && echo "  ✓ $image_name:$TAG exists" || echo "  ✗ $image_name:$TAG not found"
        done
    fi
}

clean_images() {
    info "Cleaning up old FocusHive images"
    
    # Remove dangling images
    local dangling=$(docker images -f "dangling=true" -q | grep -v '^$' || true)
    if [[ -n "$dangling" ]]; then
        info "Removing dangling images"
        docker rmi $dangling
    fi
    
    # Remove old tagged images (keep latest 3)
    for service in "${ALL_SERVICES[@]}"; do
        local image_pattern="focushive-${service}"
        local old_images=$(docker images "$image_pattern" --format "{{.Repository}}:{{.Tag}}" | head -n -3)
        
        if [[ -n "$old_images" ]]; then
            info "Removing old images for $service"
            echo "$old_images" | xargs -r docker rmi
        fi
    done
    
    log "Image cleanup completed"
}

# ============================================================================
# Command Processing
# ============================================================================

process_command() {
    local command="$1"
    
    case "$command" in
        build)
            build_images
            if [[ "$PUSH" == "true" ]]; then
                if [[ -z "$REGISTRY" ]]; then
                    error "Cannot push without registry specified"
                    exit 1
                fi
                tag_images
                push_images
            fi
            ;;
        tag)
            tag_images
            ;;
        push)
            push_images
            ;;
        login)
            login_registry
            ;;
        logout)
            logout_registry
            ;;
        list)
            list_images
            ;;
        clean)
            clean_images
            ;;
        build-push)
            build_images
            if [[ -z "$REGISTRY" ]]; then
                error "Registry must be specified for build-push"
                exit 1
            fi
            tag_images
            push_images
            ;;
        *)
            error "Unknown command: $command"
            show_usage
            exit 1
            ;;
    esac
}

# ============================================================================
# Main Function
# ============================================================================

main() {
    local command=""
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -r|--registry)
                REGISTRY="$2"
                shift 2
                ;;
            -u|--username)
                REGISTRY_USERNAME="$2"
                shift 2
                ;;
            -p|--password)
                REGISTRY_PASSWORD="$2"
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
            --platform)
                PLATFORM="$2"
                shift 2
                ;;
            --build-arg)
                BUILD_ARGS="$BUILD_ARGS --build-arg $2"
                shift 2
                ;;
            --no-cache)
                NO_CACHE=true
                shift
                ;;
            --force)
                FORCE_BUILD=true
                shift
                ;;
            --push)
                PUSH=true
                shift
                ;;
            -h|--help)
                show_usage
                exit 0
                ;;
            build|tag|push|login|logout|list|clean|build-push)
                command="$1"
                shift
                ;;
            *)
                error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done
    
    # Check for registry password in environment
    if [[ -z "$REGISTRY_PASSWORD" ]] && [[ -n "${DOCKER_REGISTRY_PASSWORD:-}" ]]; then
        REGISTRY_PASSWORD="$DOCKER_REGISTRY_PASSWORD"
    fi
    
    if [[ -z "$command" ]]; then
        error "No command specified"
        show_usage
        exit 1
    fi
    
    process_command "$command"
}

# Execute main function
main "$@"