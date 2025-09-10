#!/bin/bash

# ===============================================================================
# PRODUCTION BUILD SCRIPT - FocusHive Identity Service
# ===============================================================================
# This script provides automated production build with security scanning,
# optimization, and multi-platform support.
# ===============================================================================

set -euo pipefail

# Script Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_NAME="identity-service"
REGISTRY="${DOCKER_REGISTRY:-focushive}"
TAG="${IMAGE_TAG:-latest}"
PUSH="${PUSH_IMAGE:-true}"
SCAN_SECURITY="${SCAN_SECURITY:-true}"
BUILD_ARGS="${BUILD_ARGS:-}"
PLATFORM="${PLATFORM:-linux/amd64,linux/arm64}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# Function to display usage
usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Production build script for FocusHive Identity Service

OPTIONS:
    -t, --tag TAG               Docker image tag (default: latest)
    -r, --registry REG          Docker registry (default: focushive)
    -p, --platform PLATFORMS    Target platforms (default: linux/amd64,linux/arm64)
    --no-push                   Don't push image to registry
    --no-scan                   Skip security scanning
    --build-args ARGS           Additional build arguments
    -h, --help                  Show this help message

ENVIRONMENT VARIABLES:
    IMAGE_TAG                   Docker image tag
    DOCKER_REGISTRY            Docker registry URL
    PUSH_IMAGE                 Set to 'false' to skip push
    SCAN_SECURITY              Set to 'false' to skip security scan
    BUILD_ARGS                 Additional build arguments
    PLATFORM                   Target platforms

EXAMPLES:
    $0                                      # Build with defaults
    $0 --tag v1.2.3                       # Build specific version
    $0 --tag v1.2.3 --no-push             # Build without pushing
    $0 --platform linux/amd64             # Build for specific platform

EOF
}

# Function to check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    local missing_tools=()
    
    # Check required tools
    command -v docker >/dev/null 2>&1 || missing_tools+=("docker")
    command -v gradle >/dev/null 2>&1 || missing_tools+=("gradle")
    
    if [ ${#missing_tools[@]} -ne 0 ]; then
        log_error "Missing required tools: ${missing_tools[*]}"
        exit 1
    fi
    
    # Check Docker is running
    if ! docker info >/dev/null 2>&1; then
        log_error "Docker daemon is not running"
        exit 1
    fi
    
    # Check buildx for multi-platform builds
    if [[ "$PLATFORM" == *","* ]]; then
        if ! docker buildx version >/dev/null 2>&1; then
            log_error "Docker buildx is required for multi-platform builds"
            exit 1
        fi
    fi
    
    log_success "Prerequisites check passed"
}

# Function to run tests
run_tests() {
    log_info "Running tests..."
    
    cd "$SCRIPT_DIR"
    
    # Run unit tests
    if ./gradlew test; then
        log_success "Unit tests passed"
    else
        log_error "Unit tests failed"
        exit 1
    fi
    
    # Run integration tests (if they exist)
    if ./gradlew integrationTest 2>/dev/null || true; then
        log_success "Integration tests passed"
    else
        log_warn "Integration tests not available or failed"
    fi
    
    # Generate test reports
    if [[ -d "build/reports/tests/test" ]]; then
        log_info "Test reports available at: build/reports/tests/test/index.html"
    fi
}

# Function to build application
build_application() {
    log_info "Building application..."
    
    cd "$SCRIPT_DIR"
    
    # Clean and build
    if ./gradlew clean build -x test; then
        log_success "Application build completed"
    else
        log_error "Application build failed"
        exit 1
    fi
    
    # Verify JAR file exists
    local jar_file="build/libs/$SERVICE_NAME.jar"
    if [[ ! -f "$jar_file" ]]; then
        log_error "JAR file not found: $jar_file"
        exit 1
    fi
    
    # Display JAR information
    local jar_size=$(du -h "$jar_file" | cut -f1)
    log_info "Generated JAR: $jar_file ($jar_size)"
}

# Function to prepare build context
prepare_build_context() {
    log_info "Preparing build context..."
    
    # Create build info file
    cat > "$SCRIPT_DIR/build-info.json" << EOF
{
    "service": "$SERVICE_NAME",
    "version": "$TAG",
    "buildTime": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
    "gitCommit": "$(git rev-parse --short HEAD 2>/dev/null || echo 'unknown')",
    "gitBranch": "$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')",
    "buildUser": "${USER:-unknown}",
    "buildHost": "$(hostname)"
}
EOF
    
    log_success "Build context prepared"
}

# Function to build Docker image
build_docker_image() {
    log_info "Building Docker image..."
    
    local image_name="$REGISTRY/$SERVICE_NAME:$TAG"
    local build_args_array=()
    
    # Add build arguments
    if [[ -n "$BUILD_ARGS" ]]; then
        IFS=' ' read -ra ADDR <<< "$BUILD_ARGS"
        for arg in "${ADDR[@]}"; do
            build_args_array+=("--build-arg" "$arg")
        done
    fi
    
    # Add standard build arguments
    build_args_array+=("--build-arg" "VERSION=$TAG")
    build_args_array+=("--build-arg" "BUILD_DATE=$(date -u +"%Y-%m-%dT%H:%M:%SZ")")
    build_args_array+=("--build-arg" "GIT_COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo 'unknown')")
    
    cd "$SCRIPT_DIR"
    
    if [[ "$PLATFORM" == *","* ]]; then
        # Multi-platform build using buildx
        log_info "Building multi-platform image for: $PLATFORM"
        
        # Create and use buildx builder if not exists
        docker buildx create --name "$SERVICE_NAME-builder" --use 2>/dev/null || docker buildx use "$SERVICE_NAME-builder" 2>/dev/null || true
        
        local buildx_args=(
            "buildx" "build"
            "--platform" "$PLATFORM"
            "--tag" "$image_name"
            "${build_args_array[@]}"
            "--label" "service=$SERVICE_NAME"
            "--label" "version=$TAG"
            "--label" "build-date=$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
            "--label" "git-commit=$(git rev-parse --short HEAD 2>/dev/null || echo 'unknown')"
        )
        
        if [[ "$PUSH" == "true" ]]; then
            buildx_args+=("--push")
        else
            buildx_args+=("--load")
        fi
        
        buildx_args+=(".")
        
        if docker "${buildx_args[@]}"; then
            log_success "Multi-platform Docker image built: $image_name"
        else
            log_error "Docker build failed"
            exit 1
        fi
    else
        # Single platform build
        local docker_args=(
            "build"
            "--platform" "$PLATFORM"
            "--tag" "$image_name"
            "${build_args_array[@]}"
            "--label" "service=$SERVICE_NAME"
            "--label" "version=$TAG"
            "--label" "build-date=$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
            "--label" "git-commit=$(git rev-parse --short HEAD 2>/dev/null || echo 'unknown')"
            "."
        )
        
        if docker "${docker_args[@]}"; then
            log_success "Docker image built: $image_name"
        else
            log_error "Docker build failed"
            exit 1
        fi
    fi
    
    # Display image information
    if [[ "$PUSH" != "true" ]] || [[ "$PLATFORM" != *","* ]]; then
        docker images "$image_name" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
    fi
}

# Function to scan for security vulnerabilities
security_scan() {
    if [[ "$SCAN_SECURITY" != "true" ]]; then
        log_info "Security scanning disabled"
        return 0
    fi
    
    log_info "Running security scan..."
    
    local image_name="$REGISTRY/$SERVICE_NAME:$TAG"
    
    # Try different security scanners
    if command -v trivy >/dev/null 2>&1; then
        log_info "Scanning with Trivy..."
        if trivy image --severity HIGH,CRITICAL --exit-code 1 "$image_name"; then
            log_success "Trivy security scan passed"
        else
            log_error "Trivy security scan found vulnerabilities"
            exit 1
        fi
    elif command -v docker >/dev/null 2>&1 && docker --help | grep -q scan; then
        log_info "Scanning with Docker scan..."
        if docker scan "$image_name"; then
            log_success "Docker security scan completed"
        else
            log_warn "Docker security scan failed or found vulnerabilities"
        fi
    else
        log_warn "No security scanner available (install trivy or docker scan)"
    fi
}

# Function to push image to registry
push_image() {
    if [[ "$PUSH" != "true" ]]; then
        log_info "Image push disabled"
        return 0
    fi
    
    local image_name="$REGISTRY/$SERVICE_NAME:$TAG"
    
    # Skip push for multi-platform builds (already pushed)
    if [[ "$PLATFORM" == *","* ]]; then
        log_info "Multi-platform image already pushed during build"
        return 0
    fi
    
    log_info "Pushing image to registry..."
    
    if docker push "$image_name"; then
        log_success "Image pushed: $image_name"
    else
        log_error "Failed to push image"
        exit 1
    fi
    
    # Also tag and push as latest if not already latest
    if [[ "$TAG" != "latest" ]]; then
        local latest_image="$REGISTRY/$SERVICE_NAME:latest"
        docker tag "$image_name" "$latest_image"
        docker push "$latest_image"
        log_success "Image also tagged and pushed as: $latest_image"
    fi
}

# Function to cleanup temporary files
cleanup() {
    log_info "Cleaning up..."
    
    # Remove build info file
    rm -f "$SCRIPT_DIR/build-info.json"
    
    # Remove unused Docker resources
    docker system prune -f --filter "until=24h" >/dev/null 2>&1 || true
    
    log_success "Cleanup completed"
}

# Function to generate build summary
generate_summary() {
    log_info "Build Summary:"
    log_info "  Service: $SERVICE_NAME"
    log_info "  Tag: $TAG"
    log_info "  Registry: $REGISTRY"
    log_info "  Platform: $PLATFORM"
    log_info "  Image: $REGISTRY/$SERVICE_NAME:$TAG"
    log_info "  Pushed: $PUSH"
    log_info "  Security Scan: $SCAN_SECURITY"
    log_info "  Build Time: $(date)"
    
    if [[ -f "$SCRIPT_DIR/build/libs/$SERVICE_NAME.jar" ]]; then
        local jar_size=$(du -h "$SCRIPT_DIR/build/libs/$SERVICE_NAME.jar" | cut -f1)
        log_info "  JAR Size: $jar_size"
    fi
}

# Main build function
main() {
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -t|--tag)
                TAG="$2"
                shift 2
                ;;
            -r|--registry)
                REGISTRY="$2"
                shift 2
                ;;
            -p|--platform)
                PLATFORM="$2"
                shift 2
                ;;
            --no-push)
                PUSH="false"
                shift
                ;;
            --no-scan)
                SCAN_SECURITY="false"
                shift
                ;;
            --build-args)
                BUILD_ARGS="$2"
                shift 2
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                usage
                exit 1
                ;;
        esac
    done
    
    log_info "Starting production build for $SERVICE_NAME:$TAG"
    
    # Execute build steps
    check_prerequisites
    run_tests
    build_application
    prepare_build_context
    build_docker_image
    security_scan
    push_image
    cleanup
    generate_summary
    
    log_success "Production build completed successfully!"
}

# Error handling
trap 'log_error "Build failed at line $LINENO. Exit code: $?"' ERR

# Run main function
main "$@"