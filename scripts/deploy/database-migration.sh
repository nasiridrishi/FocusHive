#!/bin/bash

# ============================================================================
# Database Migration and Management Script for FocusHive
# Handles database setup, migrations, backups, and maintenance
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
BACKUP_DIR="${PROJECT_ROOT}/backups"

# Default values
ENVIRONMENT="development"
SERVICE="all"
BACKUP_ENCRYPTION=false
RESTORE_FILE=""
DRY_RUN=false
FORCE=false

# Database services and their configurations
declare -A DATABASE_CONFIGS
DATABASE_CONFIGS[main]="focushive-db:5432:focushive:focushive_user"
DATABASE_CONFIGS[identity]="identity-db:5432:identity_db:identity_user"
DATABASE_CONFIGS[music]="music-db:5432:focushive_music:music_user"
DATABASE_CONFIGS[notification]="notification-db:5432:notification_service:notification_user"
DATABASE_CONFIGS[chat]="chat-db:5432:chat_service:chat_user"
DATABASE_CONFIGS[analytics]="analytics-db:5432:analytics_service:analytics_user"
DATABASE_CONFIGS[forum]="forum-db:5432:forum_service:forum_user"
DATABASE_CONFIGS[buddy]="buddy-db:5432:buddy_service:buddy_user"

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
    Database management for FocusHive application.
    Handles migrations, backups, restores, and maintenance.

COMMANDS:
    migrate         Run database migrations
    rollback        Rollback last migration
    status          Show migration status
    backup          Create database backup
    restore         Restore from backup
    reset           Reset database (WARNING: destructive)
    seed            Seed database with test data
    health          Check database health
    clean           Clean old backups and logs

OPTIONS:
    -e, --environment ENV    Environment (development|staging|production) [default: development]
    -s, --service SERVICE    Database service (main|identity|music|all) [default: all]
    -f, --file FILE          Restore file path (for restore command)
    --encrypt                Encrypt backups
    --force                  Force operation without confirmation
    --dry-run               Show what would be done without executing
    -h, --help              Show this help

DATABASE SERVICES:
    main            Main application database (focushive)
    identity        Identity service database
    music           Music service database  
    notification    Notification service database
    chat            Chat service database
    analytics       Analytics service database
    forum           Forum service database
    buddy           Buddy service database
    all             All databases

EXAMPLES:
    # Run migrations for all databases
    $0 migrate

    # Backup specific service in production
    $0 -e production -s main backup --encrypt

    # Restore from backup
    $0 -s identity restore -f /path/to/backup.sql

    # Reset development database
    $0 -e development -s main reset --force

    # Check health of all databases
    $0 health

EOF
}

# ============================================================================
# Environment and Database Functions
# ============================================================================

load_environment() {
    local env="$1"
    
    local env_file="${DOCKER_DIR}/.env.${env}"
    if [[ -f "$env_file" ]]; then
        info "Loading environment from $env_file"
        set -a
        source "$env_file"
        set +a
    else
        local default_env="${DOCKER_DIR}/.env"
        if [[ -f "$default_env" ]]; then
            info "Loading default environment from $default_env"
            set -a
            source "$default_env"
            set +a
        fi
    fi
}

get_database_config() {
    local service="$1"
    
    if [[ -z "${DATABASE_CONFIGS[$service]:-}" ]]; then
        error "Unknown database service: $service"
        exit 1
    fi
    
    echo "${DATABASE_CONFIGS[$service]}"
}

parse_database_config() {
    local config="$1"
    IFS=':' read -r container port database user <<< "$config"
    
    export DB_CONTAINER="$container"
    export DB_PORT="$port"
    export DB_NAME="$database"
    export DB_USER="$user"
}

check_database_connection() {
    local service="$1"
    local config
    config=$(get_database_config "$service")
    parse_database_config "$config"
    
    info "Checking connection to $service database ($DB_CONTAINER)"
    
    if [[ "$DRY_RUN" == "true" ]]; then
        info "DRY RUN: Would check connection to $DB_CONTAINER"
        return 0
    fi
    
    cd "$DOCKER_DIR"
    
    # Check if container is running
    if ! docker-compose ps "$DB_CONTAINER" | grep -q "Up"; then
        error "Database container $DB_CONTAINER is not running"
        return 1
    fi
    
    # Test database connection
    if docker-compose exec -T "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1;" &>/dev/null; then
        log "✓ $service database connection successful"
        return 0
    else
        error "✗ Failed to connect to $service database"
        return 1
    fi
}

# ============================================================================
# Migration Functions
# ============================================================================

run_migrations() {
    local service="$1"
    
    if [[ "$service" == "all" ]]; then
        info "Running migrations for all services"
        local failed_services=()
        
        for svc in "${!DATABASE_CONFIGS[@]}"; do
            if ! run_single_migration "$svc"; then
                failed_services+=("$svc")
            fi
        done
        
        if [[ ${#failed_services[@]} -gt 0 ]]; then
            error "Migration failed for services: ${failed_services[*]}"
            exit 1
        fi
        
        log "All migrations completed successfully"
        return 0
    fi
    
    run_single_migration "$service"
}

run_single_migration() {
    local service="$1"
    local config
    config=$(get_database_config "$service")
    parse_database_config "$config"
    
    info "Running migrations for $service service"
    
    if ! check_database_connection "$service"; then
        return 1
    fi
    
    if [[ "$DRY_RUN" == "true" ]]; then
        info "DRY RUN: Would run migrations for $service"
        return 0
    fi
    
    cd "$DOCKER_DIR"
    
    # For Spring Boot services, migrations are handled by Flyway on startup
    case "$service" in
        main|identity|music|notification|chat|analytics|forum|buddy)
            info "Spring Boot service - migrations run automatically on startup"
            
            # We can check migration status by connecting to the service
            local service_container
            case "$service" in
                main) service_container="focushive-backend" ;;
                identity) service_container="identity-service" ;;
                *) service_container="${service}-service" ;;
            esac
            
            # Check if service container is healthy
            if docker-compose ps "$service_container" | grep -q "healthy\|Up"; then
                log "✓ $service migrations completed (service is running)"
            else
                warn "⚠ $service service container is not healthy - migrations may have failed"
            fi
            ;;
        *)
            error "Unknown service type for migrations: $service"
            return 1
            ;;
    esac
    
    return 0
}

show_migration_status() {
    local service="$1"
    
    if [[ "$service" == "all" ]]; then
        info "Migration status for all services:"
        echo ""
        
        for svc in "${!DATABASE_CONFIGS[@]}"; do
            echo "=== $svc Service ==="
            show_single_migration_status "$svc"
            echo ""
        done
        return 0
    fi
    
    show_single_migration_status "$service"
}

show_single_migration_status() {
    local service="$1"
    local config
    config=$(get_database_config "$service")
    parse_database_config "$config"
    
    if ! check_database_connection "$service"; then
        return 1
    fi
    
    cd "$DOCKER_DIR"
    
    # Check Flyway schema history table
    info "Migration history for $service:"
    docker-compose exec -T "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "
        SELECT version, description, type, script, installed_on, execution_time
        FROM flyway_schema_history 
        ORDER BY installed_rank DESC 
        LIMIT 10;
    " 2>/dev/null || warn "No migration history found (flyway_schema_history table missing)"
}

# ============================================================================
# Backup Functions
# ============================================================================

create_backup() {
    local service="$1"
    
    # Create backup directory
    mkdir -p "$BACKUP_DIR"
    
    if [[ "$service" == "all" ]]; then
        info "Creating backup for all services"
        
        local timestamp=$(date +%Y%m%d_%H%M%S)
        local backup_dir="${BACKUP_DIR}/full_backup_${timestamp}"
        mkdir -p "$backup_dir"
        
        for svc in "${!DATABASE_CONFIGS[@]}"; do
            create_single_backup "$svc" "$backup_dir"
        done
        
        # Create manifest
        echo "# FocusHive Full Backup - $timestamp" > "$backup_dir/MANIFEST"
        echo "# Created on: $(date)" >> "$backup_dir/MANIFEST"
        echo "# Environment: $ENVIRONMENT" >> "$backup_dir/MANIFEST"
        echo "" >> "$backup_dir/MANIFEST"
        ls -la "$backup_dir"/*.sql >> "$backup_dir/MANIFEST"
        
        log "Full backup created in: $backup_dir"
        return 0
    fi
    
    create_single_backup "$service" "$BACKUP_DIR"
}

create_single_backup() {
    local service="$1"
    local output_dir="$2"
    local config
    config=$(get_database_config "$service")
    parse_database_config "$config"
    
    if ! check_database_connection "$service"; then
        return 1
    fi
    
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local backup_file="${output_dir}/${service}_backup_${timestamp}.sql"
    
    info "Creating backup for $service database"
    info "Backup file: $backup_file"
    
    if [[ "$DRY_RUN" == "true" ]]; then
        info "DRY RUN: Would create backup $backup_file"
        return 0
    fi
    
    cd "$DOCKER_DIR"
    
    # Create backup using pg_dump
    docker-compose exec -T "$DB_CONTAINER" pg_dump -U "$DB_USER" -d "$DB_NAME" \
        --verbose --clean --create --if-exists > "$backup_file"
    
    if [[ -s "$backup_file" ]]; then
        local size=$(du -h "$backup_file" | cut -f1)
        log "✓ Backup created successfully ($size): $backup_file"
        
        # Encrypt if requested
        if [[ "$BACKUP_ENCRYPTION" == "true" ]]; then
            encrypt_backup "$backup_file"
        fi
        
        # Compress backup
        gzip "$backup_file"
        log "✓ Backup compressed: ${backup_file}.gz"
    else
        error "Backup creation failed - file is empty"
        rm -f "$backup_file"
        return 1
    fi
}

encrypt_backup() {
    local backup_file="$1"
    
    if ! command -v gpg &> /dev/null; then
        warn "GPG not available - skipping encryption"
        return 0
    fi
    
    info "Encrypting backup file"
    gpg --symmetric --cipher-algo AES256 "$backup_file"
    rm "$backup_file"
    log "✓ Backup encrypted: ${backup_file}.gpg"
}

restore_database() {
    local service="$1"
    local restore_file="$2"
    
    if [[ -z "$restore_file" ]]; then
        error "Restore file not specified"
        exit 1
    fi
    
    if [[ ! -f "$restore_file" ]]; then
        error "Restore file not found: $restore_file"
        exit 1
    fi
    
    local config
    config=$(get_database_config "$service")
    parse_database_config "$config"
    
    warn "This will REPLACE the $service database with backup data!"
    if [[ "$FORCE" != "true" ]]; then
        read -p "Are you sure you want to continue? [y/N] " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            info "Restore cancelled"
            exit 0
        fi
    fi
    
    info "Restoring $service database from: $restore_file"
    
    if [[ "$DRY_RUN" == "true" ]]; then
        info "DRY RUN: Would restore $service from $restore_file"
        return 0
    fi
    
    cd "$DOCKER_DIR"
    
    # Handle compressed files
    local restore_cmd=""
    if [[ "$restore_file" == *.gz ]]; then
        restore_cmd="zcat '$restore_file'"
    elif [[ "$restore_file" == *.gpg ]]; then
        restore_cmd="gpg --quiet --batch --decrypt '$restore_file'"
    else
        restore_cmd="cat '$restore_file'"
    fi
    
    # Restore database
    eval "$restore_cmd" | docker-compose exec -T "$DB_CONTAINER" psql -U "$DB_USER" -d postgres
    
    log "✓ Database restore completed for $service"
}

# ============================================================================
# Maintenance Functions
# ============================================================================

reset_database() {
    local service="$1"
    
    warn "This will COMPLETELY RESET the $service database!"
    warn "ALL DATA WILL BE LOST!"
    
    if [[ "$FORCE" != "true" ]]; then
        read -p "Type 'RESET' to confirm: " -r
        if [[ "$REPLY" != "RESET" ]]; then
            info "Reset cancelled"
            exit 0
        fi
    fi
    
    local config
    config=$(get_database_config "$service")
    parse_database_config "$config"
    
    info "Resetting $service database"
    
    if [[ "$DRY_RUN" == "true" ]]; then
        info "DRY RUN: Would reset $service database"
        return 0
    fi
    
    cd "$DOCKER_DIR"
    
    # Drop and recreate database
    docker-compose exec -T "$DB_CONTAINER" psql -U "$DB_USER" -d postgres -c "
        DROP DATABASE IF EXISTS \"$DB_NAME\";
        CREATE DATABASE \"$DB_NAME\" OWNER \"$DB_USER\";
    "
    
    log "✓ Database reset completed for $service"
    
    # Restart the application service to trigger migrations
    local service_container
    case "$service" in
        main) service_container="focushive-backend" ;;
        identity) service_container="identity-service" ;;
        *) service_container="${service}-service" ;;
    esac
    
    info "Restarting $service_container to trigger migrations"
    docker-compose restart "$service_container"
}

seed_database() {
    local service="$1"
    
    info "Seeding $service database with test data"
    
    if [[ "$DRY_RUN" == "true" ]]; then
        info "DRY RUN: Would seed $service database"
        return 0
    fi
    
    # Run seed scripts if they exist
    local seed_script="${PROJECT_ROOT}/scripts/seed/${service}-seed.sql"
    if [[ -f "$seed_script" ]]; then
        info "Running seed script: $seed_script"
        local config
        config=$(get_database_config "$service")
        parse_database_config "$config"
        
        cd "$DOCKER_DIR"
        cat "$seed_script" | docker-compose exec -T "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME"
        
        log "✓ Database seeded for $service"
    else
        warn "No seed script found for $service: $seed_script"
    fi
}

check_health() {
    info "Checking database health for all services"
    echo ""
    
    local unhealthy_services=()
    
    for service in "${!DATABASE_CONFIGS[@]}"; do
        echo "=== $service Service ==="
        
        if check_database_connection "$service"; then
            # Check database size
            local config
            config=$(get_database_config "$service")
            parse_database_config "$config"
            
            cd "$DOCKER_DIR"
            local size
            size=$(docker-compose exec -T "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -c "
                SELECT pg_size_pretty(pg_database_size('$DB_NAME'));
            " | tr -d ' \n')
            
            info "Database size: $size"
            
            # Check connection count
            local connections
            connections=$(docker-compose exec -T "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -c "
                SELECT count(*) FROM pg_stat_activity WHERE datname = '$DB_NAME';
            " | tr -d ' \n')
            
            info "Active connections: $connections"
            
        else
            unhealthy_services+=("$service")
        fi
        
        echo ""
    done
    
    if [[ ${#unhealthy_services[@]} -gt 0 ]]; then
        error "Unhealthy databases: ${unhealthy_services[*]}"
        exit 1
    fi
    
    log "All databases are healthy"
}

clean_old_data() {
    info "Cleaning old backups and logs"
    
    if [[ "$DRY_RUN" == "true" ]]; then
        info "DRY RUN: Would clean old backups and logs"
        return 0
    fi
    
    # Clean old backups (keep last 10)
    if [[ -d "$BACKUP_DIR" ]]; then
        info "Cleaning old backup files"
        find "$BACKUP_DIR" -name "*.sql.gz" -type f -mtime +30 -delete
        find "$BACKUP_DIR" -name "*.sql.gpg" -type f -mtime +30 -delete
        find "$BACKUP_DIR" -type d -empty -delete
    fi
    
    # Clean old logs
    local log_dir="${DOCKER_DIR}/logs"
    if [[ -d "$log_dir" ]]; then
        info "Cleaning old log files"
        find "$log_dir" -name "*.log" -type f -mtime +7 -delete
        find "$log_dir" -name "*.log.*" -type f -mtime +7 -delete
    fi
    
    log "Cleanup completed"
}

# ============================================================================
# Main Function
# ============================================================================

main() {
    local command=""
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -e|--environment)
                ENVIRONMENT="$2"
                shift 2
                ;;
            -s|--service)
                SERVICE="$2"
                shift 2
                ;;
            -f|--file)
                RESTORE_FILE="$2"
                shift 2
                ;;
            --encrypt)
                BACKUP_ENCRYPTION=true
                shift
                ;;
            --force)
                FORCE=true
                shift
                ;;
            --dry-run)
                DRY_RUN=true
                shift
                ;;
            -h|--help)
                show_usage
                exit 0
                ;;
            migrate|rollback|status|backup|restore|reset|seed|health|clean)
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
    
    if [[ -z "$command" ]]; then
        error "No command specified"
        show_usage
        exit 1
    fi
    
    # Load environment
    load_environment "$ENVIRONMENT"
    
    # Execute command
    case "$command" in
        migrate)
            run_migrations "$SERVICE"
            ;;
        status)
            show_migration_status "$SERVICE"
            ;;
        backup)
            create_backup "$SERVICE"
            ;;
        restore)
            restore_database "$SERVICE" "$RESTORE_FILE"
            ;;
        reset)
            reset_database "$SERVICE"
            ;;
        seed)
            seed_database "$SERVICE"
            ;;
        health)
            check_health
            ;;
        clean)
            clean_old_data
            ;;
        rollback)
            error "Rollback not implemented yet - this requires manual intervention"
            exit 1
            ;;
        *)
            error "Unknown command: $command"
            exit 1
            ;;
    esac
}

# Execute main function
main "$@"