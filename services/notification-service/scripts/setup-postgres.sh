#!/bin/bash

# PostgreSQL Setup Script for Notification Service
# This script sets up the required PostgreSQL databases for the notification service

echo "========================================="
echo "PostgreSQL Setup for Notification Service"
echo "========================================="

# Configuration
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-focushive}"
DB_PASSWORD="${DB_PASSWORD:-focushive123}"
DB_NAME_DEV="focushive_notifications_dev"
DB_NAME_TEST="focushive_notifications_test"
DB_NAME_PROD="focushive_notifications"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check if PostgreSQL is running
check_postgres() {
    echo -n "Checking if PostgreSQL is running... "
    if pg_isready -h $DB_HOST -p $DB_PORT > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC}"
        return 0
    else
        echo -e "${RED}✗${NC}"
        echo -e "${YELLOW}PostgreSQL is not running on $DB_HOST:$DB_PORT${NC}"
        echo "Please start PostgreSQL and try again."
        return 1
    fi
}

# Function to create database if it doesn't exist
create_database() {
    local db_name=$1
    echo -n "Creating database '$db_name'... "
    
    # Check if database exists
    if psql -h $DB_HOST -p $DB_PORT -U postgres -lqt | cut -d \| -f 1 | grep -qw $db_name; then
        echo -e "${YELLOW}Already exists${NC}"
    else
        # Create database
        if createdb -h $DB_HOST -p $DB_PORT -U postgres $db_name 2>/dev/null; then
            echo -e "${GREEN}✓${NC}"
        else
            echo -e "${RED}Failed${NC}"
            echo "You may need to run this script with appropriate PostgreSQL permissions."
            return 1
        fi
    fi
    return 0
}

# Function to create user if it doesn't exist
create_user() {
    echo -n "Creating user '$DB_USER'... "
    
    # Check if user exists
    if psql -h $DB_HOST -p $DB_PORT -U postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname='$DB_USER'" | grep -q 1; then
        echo -e "${YELLOW}Already exists${NC}"
    else
        # Create user
        if psql -h $DB_HOST -p $DB_PORT -U postgres -c "CREATE USER $DB_USER WITH PASSWORD '$DB_PASSWORD';" 2>/dev/null; then
            echo -e "${GREEN}✓${NC}"
        else
            echo -e "${RED}Failed${NC}"
            return 1
        fi
    fi
    return 0
}

# Function to grant privileges
grant_privileges() {
    local db_name=$1
    echo -n "Granting privileges on '$db_name' to '$DB_USER'... "
    
    if psql -h $DB_HOST -p $DB_PORT -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE $db_name TO $DB_USER;" 2>/dev/null; then
        echo -e "${GREEN}✓${NC}"
    else
        echo -e "${RED}Failed${NC}"
        return 1
    fi
    return 0
}

# Main execution
main() {
    echo ""
    
    # Check if PostgreSQL is running
    if ! check_postgres; then
        exit 1
    fi
    
    echo ""
    echo "Setting up databases..."
    echo "------------------------"
    
    # Create user
    if ! create_user; then
        echo -e "${RED}Failed to create user${NC}"
        exit 1
    fi
    
    # Create development database
    if ! create_database $DB_NAME_DEV; then
        echo -e "${RED}Failed to create development database${NC}"
        exit 1
    fi
    
    # Grant privileges on development database
    if ! grant_privileges $DB_NAME_DEV; then
        echo -e "${RED}Failed to grant privileges on development database${NC}"
        exit 1
    fi
    
    # Create test database
    if ! create_database $DB_NAME_TEST; then
        echo -e "${RED}Failed to create test database${NC}"
        exit 1
    fi
    
    # Grant privileges on test database
    if ! grant_privileges $DB_NAME_TEST; then
        echo -e "${RED}Failed to grant privileges on test database${NC}"
        exit 1
    fi
    
    # Create production database (optional)
    if ! create_database $DB_NAME_PROD; then
        echo -e "${RED}Failed to create production database${NC}"
        exit 1
    fi
    
    # Grant privileges on production database
    if ! grant_privileges $DB_NAME_PROD; then
        echo -e "${RED}Failed to grant privileges on production database${NC}"
        exit 1
    fi
    
    echo ""
    echo -e "${GREEN}=========================================${NC}"
    echo -e "${GREEN}PostgreSQL setup completed successfully!${NC}"
    echo -e "${GREEN}=========================================${NC}"
    echo ""
    echo "Databases created:"
    echo "  - $DB_NAME_DEV (Development)"
    echo "  - $DB_NAME_TEST (Test)"
    echo "  - $DB_NAME_PROD (Production)"
    echo ""
    echo "Connection details:"
    echo "  Host: $DB_HOST"
    echo "  Port: $DB_PORT"
    echo "  User: $DB_USER"
    echo "  Password: $DB_PASSWORD"
    echo ""
    echo "To run the application with local profile:"
    echo "  gradle bootRun --args='--spring.profiles.active=local'"
    echo ""
}

# Run main function
main