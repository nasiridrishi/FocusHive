#!/bin/bash

# =========================================
# FOCUSHIVE BACKEND DEPLOYMENT SCRIPT
# =========================================
# Copy this file to deploy.sh and update the values
# Usage: ./deploy.sh [command]
# Commands: deploy, down, logs, rebuild, clearcache, health, test
# =========================================

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# =========================================
# ENVIRONMENT VARIABLES - UPDATE THESE
# =========================================

# === SPRING CORE CONFIGURATION ===
export SPRING_APPLICATION_NAME="focushive-backend"
export SPRING_PROFILES_ACTIVE="default"
export ALLOW_BEAN_OVERRIDE="true"
export SERVER_PORT="8080"
export SERVER_ERROR_INCLUDE_MESSAGE="always"
export SERVER_ERROR_INCLUDE_BINDINGS="always"

# === DATABASE CONFIGURATION (PostgreSQL) ===
export DATABASE_URL="jdbc:postgresql://focushive_backend_postgres:5432/focushive"
export DATABASE_USERNAME="focushive_user"
export DATABASE_PASSWORD="focushive_pass_CHANGE_ME"
export DATABASE_DRIVER="org.postgresql.Driver"
export DB_POOL_MAX_SIZE="20"
export DB_POOL_MIN_IDLE="5"
export DB_CONNECTION_TIMEOUT="30000"
export DB_IDLE_TIMEOUT="600000"
export DB_MAX_LIFETIME="1800000"
export DB_LEAK_DETECTION_THRESHOLD="60000"
export DB_CONNECTION_TEST_QUERY="SELECT 1"
export DB_POOL_NAME="HikariCP"

# === POSTGRES CONTAINER CONFIG ===
export POSTGRES_DB="focushive"
export POSTGRES_USER="focushive_user"
export POSTGRES_PASSWORD="focushive_pass_CHANGE_ME"

# === JPA/HIBERNATE CONFIGURATION ===
export HIBERNATE_DDL_AUTO="update"
export HIBERNATE_DIALECT="org.hibernate.dialect.PostgreSQLDialect"
export HIBERNATE_FORMAT_SQL="false"
export HIBERNATE_USE_SQL_COMMENTS="false"
export HIBERNATE_BATCH_SIZE="25"
export HIBERNATE_BATCH_VERSIONED="true"
export HIBERNATE_ORDER_INSERTS="true"
export HIBERNATE_ORDER_UPDATES="true"
export HIBERNATE_USE_2ND_LEVEL_CACHE="false"
export HIBERNATE_USE_QUERY_CACHE="false"
export HIBERNATE_LOB_NON_CONTEXTUAL="true"
export HIBERNATE_GENERATE_STATS="false"
export HIBERNATE_NAMING_PHYSICAL_STRATEGY="org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
export HIBERNATE_NAMING_IMPLICIT_STRATEGY="org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy"
export JPA_SHOW_SQL="false"

# === REDIS CONFIGURATION ===
export REDIS_HOST="focushive_backend_redis"
export REDIS_PORT="6379"
export REDIS_PASSWORD="redis_pass_CHANGE_ME"
export REDIS_TIMEOUT="2000"
export REDIS_POOL_MAX_ACTIVE="15"
export REDIS_POOL_MAX_IDLE="8"
export REDIS_POOL_MIN_IDLE="2"
export REDIS_POOL_MAX_WAIT="-1"
export REDIS_SHUTDOWN_TIMEOUT="100"

# === CACHE CONFIGURATION ===
export CACHE_TYPE="redis"
export CACHE_TTL="3600000"
export CACHE_NULL_VALUES="false"
export CACHE_ENABLE_STATISTICS="true"
export CACHE_USE_KEY_PREFIX="true"
export CACHE_KEY_PREFIX="focushive:"

# === JWT SECURITY CONFIGURATION ===
export JWT_SECRET="your-super-secret-jwt-key-change-me-in-production-make-it-64-chars"
export JWT_EXPIRATION="86400000"
export JWT_REFRESH_EXPIRATION="604800000"
export JWT_ISSUER_URI="http://localhost:8081"
export JWT_JWK_SET_URI="http://localhost:8081/.well-known/jwks.json"

# === SERVICE URLS ===
export IDENTITY_SERVICE_URL="http://focushive-identity-service-app:8081"
export NOTIFICATION_SERVICE_URL="http://focushive-notification-service-app:8083"
export BUDDY_SERVICE_URL="http://focushive_buddy_service_app:8087"

# === IDENTITY SERVICE CLIENT CONFIG ===
export IDENTITY_SERVICE_CONNECT_TIMEOUT="5000"
export IDENTITY_SERVICE_READ_TIMEOUT="10000"
export IDENTITY_SERVICE_API_KEY=""
export IDENTITY_SERVICE_LOG_LEVEL="DEBUG"

# === FLYWAY CONFIGURATION ===
export FLYWAY_ENABLED="true"
export FLYWAY_LOCATIONS="classpath:db/migration"
export FLYWAY_BASELINE_ON_MIGRATE="true"
export FLYWAY_VALIDATE_ON_MIGRATE="true"
export FLYWAY_MIXED="true"
export FLYWAY_CLEAN_DISABLED="true"

# === APPLICATION FEATURES ===
export FEATURE_FORUM_ENABLED="true"
export FEATURE_BUDDY_ENABLED="true"
export FEATURE_NOTIFICATION_ENABLED="true"
export FEATURE_ANALYTICS_ENABLED="false"
export FEATURE_AUTHENTICATION_ENABLED="true"
export FEATURE_AUTH_CONTROLLER_ENABLED="false"
export FEATURE_REDIS_ENABLED="true"
export FEATURE_HEALTH_ENABLED="true"

# === LOGGING CONFIGURATION ===
export LOG_LEVEL_ROOT="INFO"
export LOG_LEVEL_APPLICATION="INFO"
export LOG_LEVEL_SPRING_WEB="WARN"
export LOG_LEVEL_SPRING_SECURITY="WARN"
export LOG_LEVEL_SPRING_DATA="WARN"
export LOG_LEVEL_HIBERNATE_SQL="OFF"
export LOG_LEVEL_HIBERNATE_BIND="OFF"
export LOG_LEVEL_FEIGN="DEBUG"
export LOG_PATTERN_CONSOLE="%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
export LOG_PATTERN_LEVEL="%5p"

# === MANAGEMENT ENDPOINTS ===
export MANAGEMENT_ENDPOINTS_INCLUDE="health,info,metrics,prometheus"
export HEALTH_SHOW_DETAILS="always"
export HEALTH_SHOW_COMPONENTS="always"
export HEALTH_LIVENESS_INCLUDE="ping"
export HEALTH_READINESS_INCLUDE="db,redis"
export HEALTH_STARTUP_INCLUDE="db"

# === METRICS ===
export METRICS_PROMETHEUS_ENABLED="true"
export METRICS_PERCENTILES_HISTOGRAM="true"
export METRICS_PERCENTILES="0.5,0.95,0.99"
export METRICS_AUTOTIME_ENABLED="true"

# === RATE LIMITING ===
export RATE_LIMIT_ENABLED="false"
export RATE_LIMIT_USE_REDIS="false"
export RATE_LIMIT_PUBLIC="100"
export RATE_LIMIT_AUTHENTICATED="1000"
export RATE_LIMIT_ADMIN="10000"
export RATE_LIMIT_WEBSOCKET="60"
export RATE_LIMIT_BURST="20"
export RATE_LIMIT_REFILL_DURATION="PT1M"
export RATE_LIMIT_BUCKET_TTL="PT2H"
export RATE_LIMIT_METRICS_ENABLED="true"
export RATE_LIMIT_WHITELIST=""
export RATE_LIMIT_EXCLUDED_ENDPOINTS="/actuator/health,/actuator/info,/v3/api-docs/**,/swagger-ui/**"

# === SPRINGDOC (OpenAPI) ===
export SPRINGDOC_API_PATH="/api-docs"
export SPRINGDOC_SWAGGER_PATH="/swagger-ui.html"

# === TRACING ===
export TRACING_SAMPLING_PROBABILITY="1.0"
export ZIPKIN_ENDPOINT="http://localhost:9411/api/v2/spans"

# === FEIGN CONFIGURATION ===
export FEIGN_CIRCUIT_BREAKER_ENABLED="true"
export FEIGN_CONNECT_TIMEOUT="5000"
export FEIGN_READ_TIMEOUT="10000"
export FEIGN_LOG_LEVEL="BASIC"

# === CIRCUIT BREAKER - IDENTITY SERVICE ===
export CB_IDENTITY_REGISTER_HEALTH="true"
export CB_IDENTITY_WINDOW_SIZE="10"
export CB_IDENTITY_MIN_CALLS="5"
export CB_IDENTITY_HALF_OPEN_CALLS="3"
export CB_IDENTITY_AUTO_TRANSITION="true"
export CB_IDENTITY_WAIT_DURATION="5s"
export CB_IDENTITY_FAILURE_THRESHOLD="50"
export CB_IDENTITY_SLOW_THRESHOLD="80"
export CB_IDENTITY_SLOW_DURATION="3s"

# === CIRCUIT BREAKER - NOTIFICATION SERVICE ===
export CB_NOTIFICATION_REGISTER_HEALTH="true"
export CB_NOTIFICATION_WINDOW_SIZE="10"
export CB_NOTIFICATION_MIN_CALLS="5"
export CB_NOTIFICATION_HALF_OPEN_CALLS="3"
export CB_NOTIFICATION_AUTO_TRANSITION="true"
export CB_NOTIFICATION_WAIT_DURATION="5s"
export CB_NOTIFICATION_FAILURE_THRESHOLD="50"
export CB_NOTIFICATION_SLOW_THRESHOLD="80"
export CB_NOTIFICATION_SLOW_DURATION="3s"

# === CIRCUIT BREAKER - BUDDY SERVICE ===
export CB_BUDDY_REGISTER_HEALTH="true"
export CB_BUDDY_WINDOW_SIZE="10"
export CB_BUDDY_MIN_CALLS="5"
export CB_BUDDY_HALF_OPEN_CALLS="3"
export CB_BUDDY_AUTO_TRANSITION="true"
export CB_BUDDY_WAIT_DURATION="5s"
export CB_BUDDY_FAILURE_THRESHOLD="50"
export CB_BUDDY_SLOW_THRESHOLD="80"
export CB_BUDDY_SLOW_DURATION="3s"

# === RETRY - IDENTITY SERVICE ===
export RETRY_IDENTITY_MAX_ATTEMPTS="3"
export RETRY_IDENTITY_WAIT_DURATION="1s"
export RETRY_IDENTITY_EXP_BACKOFF="true"
export RETRY_IDENTITY_BACKOFF_MULT="2"

# === RETRY - NOTIFICATION SERVICE ===
export RETRY_NOTIFICATION_MAX_ATTEMPTS="3"
export RETRY_NOTIFICATION_WAIT_DURATION="1s"
export RETRY_NOTIFICATION_EXP_BACKOFF="true"
export RETRY_NOTIFICATION_BACKOFF_MULT="2"

# === RETRY - BUDDY SERVICE ===
export RETRY_BUDDY_MAX_ATTEMPTS="3"
export RETRY_BUDDY_WAIT_DURATION="1s"
export RETRY_BUDDY_EXP_BACKOFF="true"
export RETRY_BUDDY_BACKOFF_MULT="2"

# === RATE LIMITER - IDENTITY SERVICE ===
export RL_IDENTITY_LIMIT_PER_PERIOD="100"
export RL_IDENTITY_REFRESH_PERIOD="PT1M"
export RL_IDENTITY_TIMEOUT="3s"
export RL_IDENTITY_REGISTER_HEALTH="true"

# === RATE LIMITER - NOTIFICATION SERVICE ===
export RL_NOTIFICATION_LIMIT_PER_PERIOD="50"
export RL_NOTIFICATION_REFRESH_PERIOD="PT1M"
export RL_NOTIFICATION_TIMEOUT="3s"
export RL_NOTIFICATION_REGISTER_HEALTH="true"

# === RATE LIMITER - BUDDY SERVICE ===
export RL_BUDDY_LIMIT_PER_PERIOD="100"
export RL_BUDDY_REFRESH_PERIOD="PT1M"
export RL_BUDDY_TIMEOUT="3s"
export RL_BUDDY_REGISTER_HEALTH="true"

# === TIME LIMITER - IDENTITY SERVICE ===
export TL_IDENTITY_TIMEOUT="5s"
export TL_IDENTITY_CANCEL_FUTURE="true"

# === TIME LIMITER - NOTIFICATION SERVICE ===
export TL_NOTIFICATION_TIMEOUT="10s"
export TL_NOTIFICATION_CANCEL_FUTURE="true"

# === TIME LIMITER - BUDDY SERVICE ===
export TL_BUDDY_TIMEOUT="5s"
export TL_BUDDY_CANCEL_FUTURE="true"

# === BULKHEAD - IDENTITY SERVICE ===
export BH_IDENTITY_MAX_CONCURRENT="25"
export BH_IDENTITY_MAX_WAIT="0"

# === BULKHEAD - NOTIFICATION SERVICE ===
export BH_NOTIFICATION_MAX_CONCURRENT="25"
export BH_NOTIFICATION_MAX_WAIT="0"

# === BULKHEAD - BUDDY SERVICE ===
export BH_BUDDY_MAX_CONCURRENT="25"
export BH_BUDDY_MAX_WAIT="0"

# === APP VERSION ===
export APP_VERSION="1.0.0"

# =========================================
# HELPER FUNCTIONS
# =========================================

print_banner() {
    echo -e "${BLUE}"
    echo "========================================="
    echo "       FOCUSHIVE BACKEND DEPLOYMENT"
    echo "========================================="
    echo -e "${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Create .env file for docker-compose from the variables defined in this script
generate_env_file() {
    local env_file=".env"
    print_info "Creating environment file: $env_file"
    
    # Create .env file with ALL the variables required by application.yml
    cat > "$env_file" << 'EOF'
# Environment variables for FocusHive Backend
# Auto-generated by deploy.sh - DO NOT EDIT MANUALLY
# This file contains ALL configuration variables referenced in application.yml

# === SPRING CORE CONFIGURATION ===
SPRING_APPLICATION_NAME=focushive-backend
SPRING_PROFILES_ACTIVE=default
ALLOW_BEAN_OVERRIDE=true
SERVER_PORT=8080
SERVER_ERROR_INCLUDE_MESSAGE=always
SERVER_ERROR_INCLUDE_BINDINGS=always

# === DATABASE CONFIGURATION (PostgreSQL) ===
DATABASE_URL=jdbc:postgresql://focushive_backend_postgres:5432/focushive
DATABASE_USERNAME=focushive_user
DATABASE_PASSWORD=focushive_pass_CHANGE_ME
DATABASE_DRIVER=org.postgresql.Driver
DB_POOL_MAX_SIZE=20
DB_POOL_MIN_IDLE=5
DB_CONNECTION_TIMEOUT=30000
DB_IDLE_TIMEOUT=600000
DB_MAX_LIFETIME=1800000
DB_LEAK_DETECTION_THRESHOLD=60000
DB_CONNECTION_TEST_QUERY=SELECT 1
DB_POOL_NAME=HikariCP

# === POSTGRES CONTAINER CONFIG ===
POSTGRES_DB=focushive
POSTGRES_USER=focushive_user
POSTGRES_PASSWORD=focushive_pass_CHANGE_ME

# === JPA/HIBERNATE CONFIGURATION ===
HIBERNATE_DDL_AUTO=update
HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect
HIBERNATE_FORMAT_SQL=false
HIBERNATE_USE_SQL_COMMENTS=false
HIBERNATE_BATCH_SIZE=25
HIBERNATE_BATCH_VERSIONED=true
HIBERNATE_ORDER_INSERTS=true
HIBERNATE_ORDER_UPDATES=true
HIBERNATE_USE_2ND_LEVEL_CACHE=false
HIBERNATE_USE_QUERY_CACHE=false
HIBERNATE_LOB_NON_CONTEXTUAL=true
HIBERNATE_GENERATE_STATS=false
HIBERNATE_NAMING_PHYSICAL_STRATEGY=org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy
HIBERNATE_NAMING_IMPLICIT_STRATEGY=org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy
JPA_SHOW_SQL=false

# === REDIS CONFIGURATION ===
REDIS_HOST=focushive_backend_redis
REDIS_PORT=6379
REDIS_PASSWORD=redis_pass_CHANGE_ME
REDIS_TIMEOUT=2000
REDIS_POOL_MAX_ACTIVE=15
REDIS_POOL_MAX_IDLE=8
REDIS_POOL_MIN_IDLE=2
REDIS_POOL_MAX_WAIT=-1
REDIS_SHUTDOWN_TIMEOUT=100

# === CACHE CONFIGURATION ===
CACHE_TYPE=redis
CACHE_TTL=3600000
CACHE_NULL_VALUES=false
CACHE_ENABLE_STATISTICS=true
CACHE_USE_KEY_PREFIX=true
CACHE_KEY_PREFIX=focushive:

# === JWT SECURITY CONFIGURATION ===
JWT_SECRET=your-super-secret-jwt-key-change-me-in-production-make-it-64-chars
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000
JWT_ISSUER_URI=http://localhost:8081
JWT_JWK_SET_URI=http://localhost:8081/.well-known/jwks.json

# === SERVICE URLS ===
IDENTITY_SERVICE_URL=http://focushive-identity-service-app:8081
NOTIFICATION_SERVICE_URL=http://focushive-notification-service-app:8083
BUDDY_SERVICE_URL=http://focushive_buddy_service_app:8087

# === IDENTITY SERVICE CLIENT CONFIG ===
IDENTITY_SERVICE_CONNECT_TIMEOUT=5000
IDENTITY_SERVICE_READ_TIMEOUT=10000
IDENTITY_SERVICE_API_KEY=

# === FLYWAY CONFIGURATION ===
FLYWAY_ENABLED=true
FLYWAY_LOCATIONS=classpath:db/migration
FLYWAY_BASELINE_ON_MIGRATE=true
FLYWAY_VALIDATE_ON_MIGRATE=true
FLYWAY_MIXED=true
FLYWAY_CLEAN_DISABLED=true

# === APPLICATION FEATURES ===
APP_VERSION=1.0.0
FEATURE_FORUM_ENABLED=true
FEATURE_BUDDY_ENABLED=true
FEATURE_NOTIFICATION_ENABLED=true
FEATURE_ANALYTICS_ENABLED=false
FEATURE_AUTHENTICATION_ENABLED=true
FEATURE_AUTH_CONTROLLER_ENABLED=false
FEATURE_REDIS_ENABLED=true
FEATURE_HEALTH_ENABLED=true

# === LOGGING CONFIGURATION ===
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APPLICATION=INFO
LOG_LEVEL_SPRING_WEB=WARN
LOG_LEVEL_SPRING_SECURITY=WARN
LOG_LEVEL_SPRING_DATA=WARN
LOG_LEVEL_HIBERNATE_SQL=OFF
LOG_LEVEL_HIBERNATE_BIND=OFF
LOG_LEVEL_FEIGN=DEBUG
LOG_PATTERN_CONSOLE=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
LOG_PATTERN_LEVEL=%5p

# === MANAGEMENT ENDPOINTS ===
MANAGEMENT_ENDPOINTS_INCLUDE=health,info,metrics,prometheus
HEALTH_SHOW_DETAILS=always
HEALTH_SHOW_COMPONENTS=always
HEALTH_LIVENESS_INCLUDE=ping
HEALTH_READINESS_INCLUDE=db,redis
HEALTH_STARTUP_INCLUDE=db

# === METRICS ===
METRICS_PROMETHEUS_ENABLED=true
METRICS_PERCENTILES_HISTOGRAM=true
METRICS_PERCENTILES=0.5,0.95,0.99
METRICS_AUTOTIME_ENABLED=true

# === RATE LIMITING ===
RATE_LIMIT_ENABLED=false
RATE_LIMIT_USE_REDIS=false
RATE_LIMIT_PUBLIC=100
RATE_LIMIT_AUTHENTICATED=1000
RATE_LIMIT_ADMIN=10000
RATE_LIMIT_WEBSOCKET=60
RATE_LIMIT_BURST=20
RATE_LIMIT_REFILL_DURATION=PT1M
RATE_LIMIT_BUCKET_TTL=PT2H
RATE_LIMIT_METRICS_ENABLED=true
RATE_LIMIT_WHITELIST=
RATE_LIMIT_EXCLUDED_ENDPOINTS=/actuator/health,/actuator/info,/v3/api-docs/**,/swagger-ui/**

# === SPRINGDOC (OpenAPI) ===
SPRINGDOC_API_PATH=/api-docs
SPRINGDOC_SWAGGER_PATH=/swagger-ui.html

# === TRACING ===
TRACING_SAMPLING_PROBABILITY=1.0
ZIPKIN_ENDPOINT=http://localhost:9411/api/v2/spans

# === FEIGN CONFIGURATION ===
FEIGN_CIRCUIT_BREAKER_ENABLED=true
FEIGN_CONNECT_TIMEOUT=5000
FEIGN_READ_TIMEOUT=10000
FEIGN_LOG_LEVEL=BASIC

# === CIRCUIT BREAKER - IDENTITY SERVICE ===
CB_IDENTITY_REGISTER_HEALTH=true
CB_IDENTITY_WINDOW_SIZE=10
CB_IDENTITY_MIN_CALLS=5
CB_IDENTITY_HALF_OPEN_CALLS=3
CB_IDENTITY_AUTO_TRANSITION=true
CB_IDENTITY_WAIT_DURATION=5s
CB_IDENTITY_FAILURE_THRESHOLD=50
CB_IDENTITY_SLOW_THRESHOLD=80
CB_IDENTITY_SLOW_DURATION=3s

# === CIRCUIT BREAKER - NOTIFICATION SERVICE ===
CB_NOTIFICATION_REGISTER_HEALTH=true
CB_NOTIFICATION_WINDOW_SIZE=10
CB_NOTIFICATION_MIN_CALLS=5
CB_NOTIFICATION_HALF_OPEN_CALLS=3
CB_NOTIFICATION_AUTO_TRANSITION=true
CB_NOTIFICATION_WAIT_DURATION=5s
CB_NOTIFICATION_FAILURE_THRESHOLD=50
CB_NOTIFICATION_SLOW_THRESHOLD=80
CB_NOTIFICATION_SLOW_DURATION=3s

# === CIRCUIT BREAKER - BUDDY SERVICE ===
CB_BUDDY_REGISTER_HEALTH=true
CB_BUDDY_WINDOW_SIZE=10
CB_BUDDY_MIN_CALLS=5
CB_BUDDY_HALF_OPEN_CALLS=3
CB_BUDDY_AUTO_TRANSITION=true
CB_BUDDY_WAIT_DURATION=5s
CB_BUDDY_FAILURE_THRESHOLD=50
CB_BUDDY_SLOW_THRESHOLD=80
CB_BUDDY_SLOW_DURATION=3s

# === RETRY - IDENTITY SERVICE ===
RETRY_IDENTITY_MAX_ATTEMPTS=3
RETRY_IDENTITY_WAIT_DURATION=1s
RETRY_IDENTITY_EXP_BACKOFF=true
RETRY_IDENTITY_BACKOFF_MULT=2

# === RETRY - NOTIFICATION SERVICE ===
RETRY_NOTIFICATION_MAX_ATTEMPTS=3
RETRY_NOTIFICATION_WAIT_DURATION=1s
RETRY_NOTIFICATION_EXP_BACKOFF=true
RETRY_NOTIFICATION_BACKOFF_MULT=2

# === RETRY - BUDDY SERVICE ===
RETRY_BUDDY_MAX_ATTEMPTS=3
RETRY_BUDDY_WAIT_DURATION=1s
RETRY_BUDDY_EXP_BACKOFF=true
RETRY_BUDDY_BACKOFF_MULT=2

# === RATE LIMITER - IDENTITY SERVICE ===
RL_IDENTITY_LIMIT_PER_PERIOD=100
RL_IDENTITY_REFRESH_PERIOD=PT1M
RL_IDENTITY_TIMEOUT=3s
RL_IDENTITY_REGISTER_HEALTH=true

# === RATE LIMITER - NOTIFICATION SERVICE ===
RL_NOTIFICATION_LIMIT_PER_PERIOD=50
RL_NOTIFICATION_REFRESH_PERIOD=PT1M
RL_NOTIFICATION_TIMEOUT=3s
RL_NOTIFICATION_REGISTER_HEALTH=true

# === RATE LIMITER - BUDDY SERVICE ===
RL_BUDDY_LIMIT_PER_PERIOD=100
RL_BUDDY_REFRESH_PERIOD=PT1M
RL_BUDDY_TIMEOUT=3s
RL_BUDDY_REGISTER_HEALTH=true

# === TIME LIMITER - IDENTITY SERVICE ===
TL_IDENTITY_TIMEOUT=5s
TL_IDENTITY_CANCEL_FUTURE=true

# === TIME LIMITER - NOTIFICATION SERVICE ===
TL_NOTIFICATION_TIMEOUT=10s
TL_NOTIFICATION_CANCEL_FUTURE=true

# === TIME LIMITER - BUDDY SERVICE ===
TL_BUDDY_TIMEOUT=5s
TL_BUDDY_CANCEL_FUTURE=true

# === BULKHEAD - IDENTITY SERVICE ===
BH_IDENTITY_MAX_CONCURRENT=25
BH_IDENTITY_MAX_WAIT=0

# === BULKHEAD - NOTIFICATION SERVICE ===
BH_NOTIFICATION_MAX_CONCURRENT=25
BH_NOTIFICATION_MAX_WAIT=0

# === BULKHEAD - BUDDY SERVICE ===
BH_BUDDY_MAX_CONCURRENT=25
BH_BUDDY_MAX_WAIT=0
EOF
    
    print_success "Environment file created successfully"
}

check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed or not in PATH"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed or not in PATH"
        exit 1
    fi
}

wait_for_health() {
    print_info "Waiting for application to become healthy..."
    local retries=30
    local count=0
    
    while [ $count -lt $retries ]; do
        if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
            print_success "Application is healthy!"
            return 0
        fi
        count=$((count + 1))
        print_info "Health check attempt $count/$retries..."
        sleep 2
    done
    
    print_warning "Health check timeout after $((retries * 2)) seconds"
    return 1
}

# =========================================
# COMMAND FUNCTIONS
# =========================================

cmd_deploy() {
    print_banner
    print_info "Starting deployment..."
    
    check_docker
    generate_env_file
    
    print_info "Building and starting containers..."
    docker-compose --env-file .env up --build -d
    
    if [ $? -eq 0 ]; then
        print_success "Containers started successfully"
        wait_for_health
    else
        print_error "Failed to start containers"
        exit 1
    fi
}

cmd_down() {
    print_info "Stopping and removing containers..."
    docker-compose --env-file .env down 2>/dev/null || docker-compose down
    print_success "Containers stopped"
}

cmd_logs() {
    local service=${2:-"focushive_backend"}
    local lines=${3:-"50"}
    docker-compose --env-file .env logs --tail=$lines -f $service
}

cmd_rebuild() {
    print_info "Rebuilding application..."
    docker-compose --env-file .env build --no-cache focushive_backend
    print_success "Rebuild completed"
}

cmd_clearcache() {
    print_info "Clearing Redis cache..."
    docker-compose --env-file .env exec focushive_backend_redis redis-cli -a "redis_pass_CHANGE_ME" FLUSHALL
    print_success "Cache cleared"
}

cmd_health() {
    print_info "Checking application health..."
    
    if curl -sf http://localhost:8080/actuator/health; then
        echo
        print_success "Application is healthy"
    else
        echo
        print_error "Application is not healthy"
        exit 1
    fi
}

cmd_test() {
    print_info "Running health checks and connectivity tests..."
    
    # Test database connectivity
    if docker-compose --env-file .env exec focushive_backend_postgres pg_isready -U "focushive_user" -d "focushive"; then
        print_success "Database connectivity: OK"
    else
        print_error "Database connectivity: FAILED"
    fi
    
    # Test Redis connectivity
    if docker-compose --env-file .env exec focushive_backend_redis redis-cli -a "redis_pass_CHANGE_ME" ping | grep -q PONG; then
        print_success "Redis connectivity: OK"
    else
        print_error "Redis connectivity: FAILED"
    fi
    
    # Test application health
    cmd_health
}

cmd_shell() {
    local service=${2:-"focushive_backend"}
    print_info "Opening shell in $service container..."
    docker-compose --env-file .env exec $service /bin/sh
}

cmd_help() {
    print_banner
    echo "Usage: ./deploy.sh [command]"
    echo
    echo "Commands:"
    echo "  deploy     - Build and start all containers"
    echo "  down       - Stop and remove all containers"  
    echo "  logs       - Show container logs (default: focushive_backend)"
    echo "  rebuild    - Rebuild the application container"
    echo "  clearcache - Clear Redis cache"
    echo "  health     - Check application health"
    echo "  test       - Run connectivity and health tests"
    echo "  shell      - Open shell in container (default: focushive_backend)"
    echo "  help       - Show this help message"
    echo
    echo "Examples:"
    echo "  ./deploy.sh deploy"
    echo "  ./deploy.sh logs focushive_backend 100"
    echo "  ./deploy.sh shell focushive_backend_postgres"
}

# =========================================
# MAIN EXECUTION
# =========================================

case "${1:-help}" in
    deploy)
        cmd_deploy "$@"
        ;;
    down)
        cmd_down "$@"
        ;;
    logs)
        cmd_logs "$@"
        ;;
    rebuild)
        cmd_rebuild "$@"
        ;;
    clearcache)
        cmd_clearcache "$@"
        ;;
    health)
        cmd_health "$@"
        ;;
    test)
        cmd_test "$@"
        ;;
    shell)
        cmd_shell "$@"
        ;;
    help|--help|-h)
        cmd_help "$@"
        ;;
    *)
        print_error "Unknown command: $1"
        echo
        cmd_help "$@"
        exit 1
        ;;
esac