#!/bin/bash

# ===================================================================
# E2E ENVIRONMENT SETUP SCRIPT
# 
# Quick setup script to ensure all required directories and 
# configurations are in place for E2E testing
# ===================================================================

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

print_status() {
    echo -e "${BLUE}[SETUP]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Create required directories
create_directories() {
    print_status "Creating required directories..."
    
    directories=(
        "docker/postgres"
        "docker/test-data"
        "docker/mocks/spotify/mappings"
        "test-reports"
        "frontend/e2e/results"
        "frontend/playwright-report"
        "scripts"
    )
    
    for dir in "${directories[@]}"; do
        if [ ! -d "$dir" ]; then
            mkdir -p "$dir"
            print_success "Created directory: $dir"
        else
            print_status "Directory already exists: $dir"
        fi
    done
}

# Check that all required files exist
check_files() {
    print_status "Checking required files..."
    
    required_files=(
        "docker-compose.e2e.yml"
        "docker/postgres/init-multiple-databases.sh"
        "docker/test-data/seed-data.sql"
        "docker/test-data/seed-users.sql"
        "docker/test-data/seed-hives.sql"
        "docker/test-data/seed-oauth-clients.sql"
        "docker/mocks/spotify/mappings/auth.json"
        "docker/mocks/spotify/mappings/playlists.json"
        "docker/mocks/spotify/mappings/playback.json"
        "scripts/run-e2e-tests.sh"
    )
    
    missing_files=()
    for file in "${required_files[@]}"; do
        if [ ! -f "$file" ]; then
            missing_files+=("$file")
        fi
    done
    
    if [ ${#missing_files[@]} -eq 0 ]; then
        print_success "All required files are present"
    else
        print_error "Missing files:"
        for file in "${missing_files[@]}"; do
            echo "  - $file"
        done
        return 1
    fi
}

# Set proper permissions
set_permissions() {
    print_status "Setting file permissions..."
    
    # Make scripts executable
    chmod +x scripts/*.sh 2>/dev/null || true
    chmod +x docker/postgres/init-multiple-databases.sh 2>/dev/null || true
    
    print_success "File permissions set"
}

# Validate Docker Compose file
validate_compose() {
    print_status "Validating Docker Compose configuration..."
    
    if docker compose -f docker-compose.e2e.yml config >/dev/null 2>&1; then
        print_success "Docker Compose configuration is valid"
    else
        print_error "Docker Compose configuration has errors"
        docker compose -f docker-compose.e2e.yml config
        return 1
    fi
}

# Create .env template for E2E testing
create_env_template() {
    print_status "Creating E2E environment template..."
    
    cat > .env.e2e.template << EOF
# ===================================================================
# FOCUSHIVE E2E TEST ENVIRONMENT VARIABLES
# Copy to .env.e2e and customize as needed
# ===================================================================

# JWT Secret (TEST ONLY - NOT FOR PRODUCTION)
JWT_SECRET=test_jwt_secret_key_for_e2e_testing_only_do_not_use_in_production

# Database Configuration
POSTGRES_DB=focushive_test
POSTGRES_USER=test_user
POSTGRES_PASSWORD=test_pass

# Redis Configuration
REDIS_PASSWORD=test_redis_pass

# Service URLs (for internal testing)
IDENTITY_SERVICE_URL=http://localhost:8081
MUSIC_SERVICE_URL=http://localhost:8082
NOTIFICATION_SERVICE_URL=http://localhost:8083
CHAT_SERVICE_URL=http://localhost:8084
ANALYTICS_SERVICE_URL=http://localhost:8085
FORUM_SERVICE_URL=http://localhost:8086
BUDDY_SERVICE_URL=http://localhost:8087

# Frontend URLs
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WEBSOCKET_URL=ws://localhost:8080/ws
VITE_IDENTITY_SERVICE_URL=http://localhost:8081
VITE_ENVIRONMENT=e2e

# Mock Service URLs
SPOTIFY_API_URL=http://localhost:8090
EMAIL_SMTP_HOST=localhost
EMAIL_SMTP_PORT=1025

# Test Configuration
E2E_HEADLESS=true
E2E_TIMEOUT=30000
E2E_RETRY_COUNT=2
E2E_PARALLEL_TESTS=4

# Debug Options
LOG_LEVEL=DEBUG
SHOW_SQL=true
ENABLE_TEST_DATA_SEEDING=true
EOF

    if [ ! -f ".env.e2e" ]; then
        cp .env.e2e.template .env.e2e
        print_success "Created .env.e2e from template"
    else
        print_status ".env.e2e already exists, template updated"
    fi
}

# Create package.json scripts if they don't exist
update_package_scripts() {
    if [ -f "frontend/package.json" ]; then
        print_status "Checking frontend package.json scripts..."
        
        # Check if E2E scripts exist
        if ! grep -q "test:e2e" frontend/package.json; then
            print_status "Consider adding these E2E scripts to frontend/package.json:"
            echo ""
            echo "\"scripts\": {"
            echo "  \"test:e2e\": \"playwright test\","
            echo "  \"test:e2e:headed\": \"playwright test --headed\","
            echo "  \"test:e2e:ui\": \"playwright test --ui\","
            echo "  \"test:e2e:report\": \"playwright show-report\","
            echo "  \"test:cypress\": \"cypress run\","
            echo "  \"test:cypress:open\": \"cypress open\""
            echo "}"
        fi
    fi
}

# Main setup function
main() {
    echo "═════════════════════════════════════════"
    echo "🧪 FocusHive E2E Environment Setup"
    echo "═════════════════════════════════════════"
    echo ""
    
    create_directories
    echo ""
    
    check_files
    echo ""
    
    set_permissions
    echo ""
    
    validate_compose
    echo ""
    
    create_env_template
    echo ""
    
    update_package_scripts
    echo ""
    
    print_success "E2E environment setup completed!"
    echo ""
    echo "📋 Next steps:"
    echo "1. Review and customize .env.e2e if needed"
    echo "2. Run './scripts/run-e2e-tests.sh start' to start the environment"
    echo "3. Run './scripts/run-e2e-tests.sh' to execute all 558 E2E tests"
    echo ""
    echo "🔧 Quick commands:"
    echo "  Start E2E env:  ./scripts/run-e2e-tests.sh start"
    echo "  Run tests:      ./scripts/run-e2e-tests.sh"
    echo "  Stop env:       ./scripts/run-e2e-tests.sh stop"
    echo "  View logs:      ./scripts/run-e2e-tests.sh logs"
    echo "  Check status:   ./scripts/run-e2e-tests.sh status"
}

# Run main function
main "$@"