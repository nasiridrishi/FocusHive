#!/bin/bash
set -e

# PostgreSQL Database Initialization Script for Identity Service
# This script runs when the Identity Service PostgreSQL container is first created

echo "Starting Identity Service database initialization..."

# Create additional databases and configure Identity Service specific settings
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Create additional schemas for Identity Service
    CREATE SCHEMA IF NOT EXISTS oauth2;
    CREATE SCHEMA IF NOT EXISTS persona;
    CREATE SCHEMA IF NOT EXISTS privacy;
    CREATE SCHEMA IF NOT EXISTS audit;
    
    -- Grant permissions
    GRANT ALL PRIVILEGES ON SCHEMA oauth2 TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON SCHEMA persona TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON SCHEMA privacy TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON SCHEMA audit TO $POSTGRES_USER;
    
    -- Create extensions needed for Identity Service
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";
    CREATE EXTENSION IF NOT EXISTS "pg_trgm";
    CREATE EXTENSION IF NOT EXISTS "pgcrypto";
    CREATE EXTENSION IF NOT EXISTS "btree_gin";
    
    -- Set timezone
    SET timezone = 'UTC';
    
    -- Create monitoring user for health checks
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'identity_healthcheck') THEN
            CREATE ROLE identity_healthcheck WITH LOGIN PASSWORD 'identity_healthcheck';
        END IF;
    END
    \$\$;
    
    GRANT CONNECT ON DATABASE $POSTGRES_DB TO identity_healthcheck;
    GRANT USAGE ON SCHEMA public TO identity_healthcheck;
    
    -- Create read-only user for reporting
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'identity_readonly') THEN
            CREATE ROLE identity_readonly WITH LOGIN PASSWORD 'identity_readonly';
        END IF;
    END
    \$\$;
    
    GRANT CONNECT ON DATABASE $POSTGRES_DB TO identity_readonly;
    GRANT USAGE ON SCHEMA public, oauth2, persona, privacy, audit TO identity_readonly;
    
    -- Configure settings for Identity Service performance
    ALTER SYSTEM SET max_connections = 100;
    ALTER SYSTEM SET shared_buffers = '128MB';
    ALTER SYSTEM SET effective_cache_size = '512MB';
    ALTER SYSTEM SET work_mem = '2MB';
    ALTER SYSTEM SET maintenance_work_mem = '32MB';
    ALTER SYSTEM SET random_page_cost = 1.1;
    ALTER SYSTEM SET effective_io_concurrency = 200;
    ALTER SYSTEM SET checkpoint_completion_target = 0.9;
    ALTER SYSTEM SET wal_buffers = '8MB';
    ALTER SYSTEM SET default_statistics_target = 100;
    
    -- Log initialization completion
    INSERT INTO pg_stat_statements_info (dealloc) VALUES (0) ON CONFLICT DO NOTHING;
EOSQL

echo "Identity Service database initialization completed."