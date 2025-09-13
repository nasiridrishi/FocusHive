#!/bin/bash
set -e
set -u

# ===================================================================
# POSTGRESQL MULTIPLE DATABASE INITIALIZATION SCRIPT
# 
# Creates multiple test databases for FocusHive microservices
# Used during E2E testing to isolate service data
# ===================================================================

function create_user_and_database() {
    local database=$1
    local owner=${2:-$POSTGRES_USER}
    echo "Creating database '$database' with owner '$owner'"
    
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
        SELECT 'CREATE DATABASE $database OWNER $owner ENCODING ''UTF8''' 
        WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$database');
        \gexec
        
        GRANT ALL PRIVILEGES ON DATABASE $database TO $owner;
        
        \c $database;
        
        -- Enable required extensions for each database
        CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
        CREATE EXTENSION IF NOT EXISTS "pgcrypto";
        
        -- Create necessary schemas
        CREATE SCHEMA IF NOT EXISTS audit;
        CREATE SCHEMA IF NOT EXISTS config;
        
        -- Grant permissions on schemas
        GRANT ALL ON SCHEMA public TO $owner;
        GRANT ALL ON SCHEMA audit TO $owner;
        GRANT ALL ON SCHEMA config TO $owner;
        
        -- Set default privileges for future tables
        ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $owner;
        ALTER DEFAULT PRIVILEGES IN SCHEMA audit GRANT ALL ON TABLES TO $owner;
        ALTER DEFAULT PRIVILEGES IN SCHEMA config GRANT ALL ON TABLES TO $owner;
        
        ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO $owner;
        ALTER DEFAULT PRIVILEGES IN SCHEMA audit GRANT ALL ON SEQUENCES TO $owner;
        ALTER DEFAULT PRIVILEGES IN SCHEMA config GRANT ALL ON SEQUENCES TO $owner;
EOSQL
}

# Main database should already exist via POSTGRES_DB
echo "Main test database '$POSTGRES_DB' should already exist"

# Create additional databases if POSTGRES_MULTIPLE_DATABASES is set
if [ -n "${POSTGRES_MULTIPLE_DATABASES:-}" ]; then
    echo "Creating additional databases: $POSTGRES_MULTIPLE_DATABASES"
    for db in $(echo $POSTGRES_MULTIPLE_DATABASES | tr ',' ' '); do
        create_user_and_database $db $POSTGRES_USER
        echo "Database '$db' created successfully"
    done
    echo "Multiple database creation completed!"
else
    echo "No additional databases specified in POSTGRES_MULTIPLE_DATABASES"
fi

# Set up connection pooling settings for test environment
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Optimize for test workloads
    ALTER SYSTEM SET shared_preload_libraries = 'pg_stat_statements';
    ALTER SYSTEM SET max_connections = 200;
    ALTER SYSTEM SET shared_buffers = '256MB';
    ALTER SYSTEM SET effective_cache_size = '1GB';
    ALTER SYSTEM SET maintenance_work_mem = '64MB';
    ALTER SYSTEM SET checkpoint_completion_target = 0.9;
    ALTER SYSTEM SET wal_buffers = '16MB';
    ALTER SYSTEM SET default_statistics_target = 100;
    ALTER SYSTEM SET random_page_cost = 1.1;
    ALTER SYSTEM SET effective_io_concurrency = 200;
    
    -- Enable query logging for debugging (will be verbose!)
    ALTER SYSTEM SET log_statement = 'all';
    ALTER SYSTEM SET log_min_duration_statement = 1000;
    ALTER SYSTEM SET log_min_messages = 'warning';
    
    SELECT pg_reload_conf();
EOSQL

echo "PostgreSQL multiple database initialization completed!"