#!/bin/bash

# Start Identity Service with PostgreSQL configuration for N+1 query testing
# This script configures and starts the Identity Service with proper environment variables

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# Load environment from .env file
if [ -f ".env" ]; then
    log_info "Loading environment from .env file"
    set -a
    source .env
    set +a
fi

# Ensure we're in the project root
if [ ! -f "services/identity-service/build.gradle.kts" ]; then
    log_error "This script must be run from the project root directory"
    exit 1
fi

# Check if PostgreSQL is running
log_info "Checking PostgreSQL connection..."
if ! nc -z localhost 5433 2>/dev/null; then
    log_error "PostgreSQL is not running on port 5433"
    log_info "Starting PostgreSQL container..."
    
    docker run -d \
      --name identity-postgres \
      -e POSTGRES_USER=identity_user \
      -e POSTGRES_PASSWORD=${DB_PASSWORD:-identity_pass} \
      -e POSTGRES_DB=identity_db \
      -p 5433:5432 \
      postgres:15-alpine || {
        log_warning "Container may already exist, trying to start it..."
        docker start identity-postgres || {
            log_error "Failed to start PostgreSQL container"
            exit 1
        }
    }
    
    log_info "Waiting for PostgreSQL to be ready..."
    sleep 10
fi

# SECURITY WARNING: Set these via environment variables in production
# Set required environment variables for Identity Service
export DB_HOST=localhost
export DB_PORT=5433
export DB_NAME=identity_db
export DB_USER=identity_user
export DB_PASSWORD=${DB_PASSWORD:-identity_pass}  # SECURITY: Set via env var

# JWT Configuration - SECURITY: Must be set via environment variable
if [ -z "${JWT_SECRET}" ]; then
    log_error "JWT_SECRET environment variable must be set"
    log_info "Generate with: openssl rand -base64 64"
    exit 1
fi
export JWT_SECRET=${JWT_SECRET}
export JWT_ACCESS_TOKEN_EXPIRATION=3600000
export JWT_REFRESH_TOKEN_EXPIRATION=2592000000

# OAuth2 Configuration - SECURITY: Must be set via environment variable
if [ -z "${FOCUSHIVE_CLIENT_SECRET}" ]; then
    log_error "FOCUSHIVE_CLIENT_SECRET environment variable must be set"
    exit 1
fi
export FOCUSHIVE_CLIENT_SECRET=${FOCUSHIVE_CLIENT_SECRET}

# Security Configuration - SECURITY: Set via environment variables
export KEY_STORE_PASSWORD=${KEY_STORE_PASSWORD:-"password"}
export PRIVATE_KEY_PASSWORD=${PRIVATE_KEY_PASSWORD:-"password"}

# Redis Configuration - SECURITY: Set via environment variable
export REDIS_HOST=localhost
export REDIS_PORT=6380
if [ -z "${REDIS_PASSWORD}" ]; then
    log_error "REDIS_PASSWORD environment variable must be set"
    exit 1
fi
export REDIS_PASSWORD=${REDIS_PASSWORD}

# Other configuration
export CORS_ORIGINS="http://localhost:3000,http://localhost:5173,http://localhost:8080"
export APP_BASE_URL="http://localhost:3000"
export EMAIL_FROM="noreply@focushive.com"

# Logging for development
export LOG_LEVEL=INFO
export SQL_LOG_LEVEL=DEBUG
export SQL_BINDER_LOG_LEVEL=TRACE

log_success "Environment variables configured"
echo "Database: postgresql://identity_user@localhost:5433/identity_db"
echo "JWT Secret: $(echo $JWT_SECRET | head -c 10)..."
echo "CORS Origins: $CORS_ORIGINS"

# Check if we need to create a simple keystore for development
KEYSTORE_PATH="services/identity-service/src/main/resources/keystore.p12"
if [ ! -f "$KEYSTORE_PATH" ]; then
    log_warning "Keystore not found, creating development keystore..."
    mkdir -p "$(dirname "$KEYSTORE_PATH")"
    
    # Create a simple keystore for development
    keytool -genkeypair \
        -alias identity-service \
        -keyalg RSA \
        -keysize 2048 \
        -keystore "$KEYSTORE_PATH" \
        -storepass password \
        -keypass password \
        -dname "CN=identity-service,OU=Development,O=FocusHive,C=US" \
        -validity 3650 || {
        log_warning "Could not create keystore with keytool, trying alternative approach..."
        
        # Alternative: modify configuration to use JWK instead of keystore
        log_info "Will use JWT secret instead of keystore for development"
        export USE_JWT_SECRET=true
    }
fi

# Navigate to identity service directory
cd services/identity-service

log_info "Building Identity Service..."
./gradlew build -x test

log_info "Starting Identity Service on port 8081..."
log_info "This will run in the foreground. Use Ctrl+C to stop."
echo ""

# Start the service
./gradlew bootRun --args='--spring.profiles.active=local'