#!/bin/bash

# FocusHive Database Backup Script
# Creates timestamped backups of PostgreSQL database

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
BACKUP_DIR="./backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DB_CONTAINER="focushive-postgres"
DB_NAME="focushive"
DB_USER="focushive"

# Function to print colored output
print_message() {
    echo -e "${2}${1}${NC}"
}

# Function to check if container is running
check_container() {
    if ! docker ps | grep -q "$DB_CONTAINER"; then
        print_message "Database container is not running!" "$RED"
        print_message "Start services first: ./docker/scripts/start.sh" "$YELLOW"
        exit 1
    fi
}

# Function to create backup directory
create_backup_dir() {
    if [ ! -d "$BACKUP_DIR" ]; then
        print_message "Creating backup directory..." "$YELLOW"
        mkdir -p "$BACKUP_DIR"
    fi
}

# Function to backup database
backup_database() {
    local backup_file="$BACKUP_DIR/focushive_${TIMESTAMP}.sql"
    
    print_message "Creating database backup..." "$YELLOW"
    print_message "Backup file: $backup_file" "$YELLOW"
    
    # Create backup using pg_dump
    docker exec -t "$DB_CONTAINER" pg_dump \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        --verbose \
        --clean \
        --no-owner \
        --no-privileges \
        --if-exists \
        > "$backup_file"
    
    # Compress the backup
    print_message "Compressing backup..." "$YELLOW"
    gzip "$backup_file"
    
    local compressed_file="${backup_file}.gz"
    local file_size=$(ls -lh "$compressed_file" | awk '{print $5}')
    
    print_message "Backup created successfully!" "$GREEN"
    print_message "File: $compressed_file" "$GREEN"
    print_message "Size: $file_size" "$GREEN"
}

# Function to backup Redis
backup_redis() {
    local redis_container="focushive-redis"
    
    if docker ps | grep -q "$redis_container"; then
        print_message "Creating Redis backup..." "$YELLOW"
        
        local redis_backup="$BACKUP_DIR/redis_${TIMESTAMP}.rdb"
        
        # Trigger Redis save and copy dump file
        docker exec -t "$redis_container" redis-cli BGSAVE
        sleep 2
        docker cp "$redis_container:/data/dump.rdb" "$redis_backup"
        
        # Compress Redis backup
        gzip "$redis_backup"
        
        print_message "Redis backup created: ${redis_backup}.gz" "$GREEN"
    fi
}

# Function to clean old backups
clean_old_backups() {
    local keep_days=${1:-7}
    
    if [ -d "$BACKUP_DIR" ]; then
        print_message "Cleaning backups older than $keep_days days..." "$YELLOW"
        
        find "$BACKUP_DIR" -name "*.gz" -type f -mtime +$keep_days -delete
        
        print_message "Old backups cleaned!" "$GREEN"
    fi
}

# Function to list existing backups
list_backups() {
    print_message "\nExisting backups:" "$YELLOW"
    
    if [ -d "$BACKUP_DIR" ] && [ "$(ls -A $BACKUP_DIR)" ]; then
        ls -lh "$BACKUP_DIR"/*.gz 2>/dev/null | tail -10
    else
        print_message "No backups found." "$YELLOW"
    fi
}

# Function to restore database (optional)
restore_database() {
    local backup_file="$1"
    
    if [ -z "$backup_file" ]; then
        print_message "Usage: $0 restore <backup_file>" "$RED"
        list_backups
        exit 1
    fi
    
    if [ ! -f "$backup_file" ]; then
        print_message "Backup file not found: $backup_file" "$RED"
        exit 1
    fi
    
    print_message "⚠️  WARNING: This will replace all current data!" "$RED"
    read -p "Are you sure you want to restore from $backup_file? [y/N]: " -n 1 -r
    echo
    
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_message "Restore cancelled." "$YELLOW"
        exit 0
    fi
    
    print_message "Restoring database..." "$YELLOW"
    
    # Decompress if needed
    if [[ "$backup_file" == *.gz ]]; then
        gunzip -c "$backup_file" | docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME"
    else
        docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" < "$backup_file"
    fi
    
    print_message "Database restored successfully!" "$GREEN"
}

# Main execution
main() {
    local action=${1:-backup}
    
    case "$action" in
        backup)
            print_message "📦 FocusHive Backup" "$GREEN"
            print_message "================================" "$GREEN"
            
            check_container
            create_backup_dir
            backup_database
            backup_redis
            clean_old_backups 7
            list_backups
            
            print_message "\n✅ Backup completed!" "$GREEN"
            ;;
            
        restore)
            print_message "📥 FocusHive Restore" "$YELLOW"
            print_message "================================" "$YELLOW"
            
            check_container
            restore_database "$2"
            ;;
            
        list)
            list_backups
            ;;
            
        *)
            print_message "Usage: $0 [backup|restore|list]" "$YELLOW"
            print_message "  backup  - Create a new backup (default)" "$YELLOW"
            print_message "  restore <file> - Restore from backup file" "$YELLOW"
            print_message "  list    - List existing backups" "$YELLOW"
            exit 1
            ;;
    esac
}

# Run main function
main "$@"