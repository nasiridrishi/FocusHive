#!/bin/bash

# ===============================================================================
# PRODUCTION DEPLOYMENT SCRIPT - FocusHive Identity Service
# ===============================================================================
# This script provides automated production deployment with safety checks,
# zero-downtime rolling updates, and rollback capabilities.
# ===============================================================================

set -euo pipefail

# Script Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_NAME="identity-service"
NAMESPACE="focushive"
IMAGE_TAG="${IMAGE_TAG:-latest}"
REGISTRY="${DOCKER_REGISTRY:-focushive}"
TIMEOUT="${DEPLOY_TIMEOUT:-600}"
DRY_RUN="${DRY_RUN:-false}"

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

Production deployment script for FocusHive Identity Service

OPTIONS:
    -t, --tag TAG           Docker image tag (default: latest)
    -n, --namespace NS      Kubernetes namespace (default: focushive)
    -r, --registry REG      Docker registry (default: focushive)
    -d, --dry-run          Perform dry run without actual deployment
    -h, --help             Show this help message
    --rollback             Rollback to previous version
    --health-check         Perform health check only
    --pre-deploy           Run pre-deployment checks only

ENVIRONMENT VARIABLES:
    IMAGE_TAG               Docker image tag
    DOCKER_REGISTRY         Docker registry URL
    DEPLOY_TIMEOUT          Deployment timeout in seconds (default: 600)
    DRY_RUN                 Set to 'true' for dry run mode

EXAMPLES:
    $0 --tag v1.2.3                    # Deploy specific version
    $0 --dry-run --tag v1.2.3         # Dry run deployment
    $0 --rollback                      # Rollback to previous version
    $0 --health-check                  # Check service health
    DRY_RUN=true $0 --tag v1.2.3      # Dry run via environment variable

EOF
}

# Function to check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    local missing_tools=()
    
    # Check required tools
    command -v kubectl >/dev/null 2>&1 || missing_tools+=("kubectl")
    command -v docker >/dev/null 2>&1 || missing_tools+=("docker")
    command -v envsubst >/dev/null 2>&1 || missing_tools+=("envsubst")
    command -v jq >/dev/null 2>&1 || missing_tools+=("jq")
    
    if [ ${#missing_tools[@]} -ne 0 ]; then
        log_error "Missing required tools: ${missing_tools[*]}"
        log_error "Please install the missing tools and try again."
        exit 1
    fi
    
    # Check Kubernetes connectivity
    if ! kubectl cluster-info >/dev/null 2>&1; then
        log_error "Unable to connect to Kubernetes cluster"
        log_error "Please check your kubeconfig and cluster connectivity."
        exit 1
    fi
    
    # Check namespace exists
    if ! kubectl get namespace "$NAMESPACE" >/dev/null 2>&1; then
        log_error "Namespace '$NAMESPACE' does not exist"
        log_error "Please create the namespace first: kubectl create namespace $NAMESPACE"
        exit 1
    fi
    
    log_success "Prerequisites check passed"
}

# Function to validate configuration files
validate_config() {
    log_info "Validating Kubernetes configuration files..."
    
    local config_dir="$SCRIPT_DIR/k8s"
    local required_files=("deployment.yml" "service.yml" "configmap.yml" "secrets.yml" "ingress.yml" "hpa.yml")
    
    for file in "${required_files[@]}"; do
        if [[ ! -f "$config_dir/$file" ]]; then
            log_error "Required configuration file missing: $config_dir/$file"
            exit 1
        fi
        
        # Validate YAML syntax
        if ! kubectl --dry-run=client apply -f "$config_dir/$file" >/dev/null 2>&1; then
            log_error "Invalid YAML syntax in: $config_dir/$file"
            exit 1
        fi
    done
    
    log_success "Configuration files validated"
}

# Function to check image availability
check_image() {
    log_info "Checking Docker image availability..."
    
    local image_name="$REGISTRY/$SERVICE_NAME:$IMAGE_TAG"
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would check image: $image_name"
        return 0
    fi
    
    if ! docker pull "$image_name" >/dev/null 2>&1; then
        log_error "Docker image not found: $image_name"
        log_error "Please build and push the image first."
        exit 1
    fi
    
    log_success "Docker image verified: $image_name"
}

# Function to backup current deployment
backup_current_deployment() {
    log_info "Backing up current deployment..."
    
    local backup_dir="$SCRIPT_DIR/backups/$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$backup_dir"
    
    # Backup current deployment
    kubectl get deployment "$SERVICE_NAME" -n "$NAMESPACE" -o yaml > "$backup_dir/deployment.yml" 2>/dev/null || true
    kubectl get service "$SERVICE_NAME" -n "$NAMESPACE" -o yaml > "$backup_dir/service.yml" 2>/dev/null || true
    kubectl get configmap "$SERVICE_NAME-config" -n "$NAMESPACE" -o yaml > "$backup_dir/configmap.yml" 2>/dev/null || true
    kubectl get secret "$SERVICE_NAME-secrets" -n "$NAMESPACE" -o yaml > "$backup_dir/secrets.yml" 2>/dev/null || true
    kubectl get ingress "$SERVICE_NAME-ingress" -n "$NAMESPACE" -o yaml > "$backup_dir/ingress.yml" 2>/dev/null || true
    kubectl get hpa "$SERVICE_NAME-hpa" -n "$NAMESPACE" -o yaml > "$backup_dir/hpa.yml" 2>/dev/null || true
    
    echo "$backup_dir" > "$SCRIPT_DIR/.last-backup"
    log_success "Backup created: $backup_dir"
}

# Function to apply configuration with environment substitution
apply_config() {
    local config_file="$1"
    local temp_file="/tmp/$SERVICE_NAME-$(basename "$config_file")"
    
    # Substitute environment variables
    export IMAGE_TAG NAMESPACE REGISTRY SERVICE_NAME
    export CONFIG_HASH=$(sha256sum "$SCRIPT_DIR/k8s/configmap.yml" | cut -d' ' -f1)
    
    envsubst < "$config_file" > "$temp_file"
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would apply: $config_file"
        kubectl --dry-run=client apply -f "$temp_file"
    else
        kubectl apply -f "$temp_file"
    fi
    
    rm -f "$temp_file"
}

# Function to deploy to Kubernetes
deploy() {
    log_info "Starting deployment of $SERVICE_NAME:$IMAGE_TAG to namespace $NAMESPACE..."
    
    local config_dir="$SCRIPT_DIR/k8s"
    
    # Apply configurations in correct order
    log_info "Applying ConfigMap..."
    apply_config "$config_dir/configmap.yml"
    
    log_info "Applying Secrets..."
    apply_config "$config_dir/secrets.yml"
    
    log_info "Applying Service..."
    apply_config "$config_dir/service.yml"
    
    log_info "Applying Deployment..."
    apply_config "$config_dir/deployment.yml"
    
    log_info "Applying HPA..."
    apply_config "$config_dir/hpa.yml"
    
    log_info "Applying Ingress..."
    apply_config "$config_dir/ingress.yml"
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log_success "[DRY RUN] Deployment configurations validated successfully"
        return 0
    fi
    
    log_success "Deployment configurations applied"
}

# Function to wait for rollout completion
wait_for_rollout() {
    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would wait for rollout completion"
        return 0
    fi
    
    log_info "Waiting for rollout to complete (timeout: ${TIMEOUT}s)..."
    
    if kubectl rollout status deployment/"$SERVICE_NAME" -n "$NAMESPACE" --timeout="${TIMEOUT}s"; then
        log_success "Rollout completed successfully"
    else
        log_error "Rollout failed or timed out"
        log_error "Checking pod status..."
        kubectl get pods -n "$NAMESPACE" -l app="$SERVICE_NAME"
        kubectl describe deployment "$SERVICE_NAME" -n "$NAMESPACE"
        exit 1
    fi
}

# Function to perform health checks
health_check() {
    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would perform health checks"
        return 0
    fi
    
    log_info "Performing health checks..."
    
    # Get service endpoint
    local service_ip
    service_ip=$(kubectl get service "$SERVICE_NAME" -n "$NAMESPACE" -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "")
    
    if [[ -z "$service_ip" ]]; then
        # Try to get NodePort or ClusterIP
        service_ip=$(kubectl get service "$SERVICE_NAME" -n "$NAMESPACE" -o jsonpath='{.spec.clusterIP}')
    fi
    
    if [[ -z "$service_ip" ]]; then
        log_warn "Unable to determine service IP, checking pod health instead"
        # Check pod readiness
        local ready_pods
        ready_pods=$(kubectl get pods -n "$NAMESPACE" -l app="$SERVICE_NAME" -o jsonpath='{.items[*].status.containerStatuses[0].ready}')
        
        if [[ "$ready_pods" == *"true"* ]]; then
            log_success "Pods are ready and healthy"
        else
            log_error "Pods are not ready"
            kubectl get pods -n "$NAMESPACE" -l app="$SERVICE_NAME"
            return 1
        fi
        return 0
    fi
    
    # Test health endpoint
    local health_url="http://$service_ip:8081/actuator/health"
    local max_attempts=30
    local attempt=1
    
    while [[ $attempt -le $max_attempts ]]; do
        log_info "Health check attempt $attempt/$max_attempts..."
        
        if curl -f -s "$health_url" >/dev/null 2>&1; then
            log_success "Health check passed"
            return 0
        fi
        
        sleep 10
        ((attempt++))
    done
    
    log_error "Health check failed after $max_attempts attempts"
    return 1
}

# Function to rollback deployment
rollback() {
    log_info "Rolling back deployment..."
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would rollback deployment"
        return 0
    fi
    
    if kubectl rollout undo deployment/"$SERVICE_NAME" -n "$NAMESPACE"; then
        log_success "Rollback initiated"
        wait_for_rollout
    else
        log_error "Rollback failed"
        exit 1
    fi
}

# Function to cleanup old resources
cleanup() {
    log_info "Cleaning up old resources..."
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would cleanup old resources"
        return 0
    fi
    
    # Keep only the last 3 replica sets
    kubectl get replicasets -n "$NAMESPACE" -l app="$SERVICE_NAME" --sort-by=.metadata.creationTimestamp -o name | head -n -3 | xargs -r kubectl delete -n "$NAMESPACE"
    
    # Clean up old backups (keep last 10)
    if [[ -d "$SCRIPT_DIR/backups" ]]; then
        find "$SCRIPT_DIR/backups" -maxdepth 1 -type d -name "20*" | sort -r | tail -n +11 | xargs -r rm -rf
    fi
    
    log_success "Cleanup completed"
}

# Function to run pre-deployment checks
pre_deploy_checks() {
    log_info "Running pre-deployment checks..."
    
    check_prerequisites
    validate_config
    check_image
    
    # Check if deployment already exists
    if kubectl get deployment "$SERVICE_NAME" -n "$NAMESPACE" >/dev/null 2>&1; then
        log_info "Existing deployment found, will perform rolling update"
    else
        log_info "No existing deployment found, will create new deployment"
    fi
    
    log_success "Pre-deployment checks completed"
}

# Main deployment function
main() {
    local operation="deploy"
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -t|--tag)
                IMAGE_TAG="$2"
                shift 2
                ;;
            -n|--namespace)
                NAMESPACE="$2"
                shift 2
                ;;
            -r|--registry)
                REGISTRY="$2"
                shift 2
                ;;
            -d|--dry-run)
                DRY_RUN="true"
                shift
                ;;
            --rollback)
                operation="rollback"
                shift
                ;;
            --health-check)
                operation="health-check"
                shift
                ;;
            --pre-deploy)
                operation="pre-deploy"
                shift
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
    
    # Print configuration
    log_info "Deployment Configuration:"
    log_info "  Service: $SERVICE_NAME"
    log_info "  Namespace: $NAMESPACE"
    log_info "  Image: $REGISTRY/$SERVICE_NAME:$IMAGE_TAG"
    log_info "  Operation: $operation"
    log_info "  Dry Run: $DRY_RUN"
    log_info ""
    
    # Execute operation
    case $operation in
        "deploy")
            pre_deploy_checks
            backup_current_deployment
            deploy
            wait_for_rollout
            health_check
            cleanup
            log_success "Deployment completed successfully!"
            ;;
        "rollback")
            check_prerequisites
            rollback
            health_check
            log_success "Rollback completed successfully!"
            ;;
        "health-check")
            health_check
            ;;
        "pre-deploy")
            pre_deploy_checks
            ;;
        *)
            log_error "Unknown operation: $operation"
            exit 1
            ;;
    esac
}

# Error handling
trap 'log_error "Deployment failed at line $LINENO. Exit code: $?"' ERR

# Run main function
main "$@"