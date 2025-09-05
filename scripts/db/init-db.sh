#!/bin/bash
set -e

# PostgreSQL Database Initialization Script for FocusHive Main Database
# This script runs when the PostgreSQL container is first created

echo "Starting FocusHive database initialization..."

# Create additional databases if needed
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Create additional schemas if needed
    CREATE SCHEMA IF NOT EXISTS analytics;
    CREATE SCHEMA IF NOT EXISTS audit;
    CREATE SCHEMA IF NOT EXISTS cache;
    
    -- Grant permissions
    GRANT ALL PRIVILEGES ON SCHEMA analytics TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON SCHEMA audit TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON SCHEMA cache TO $POSTGRES_USER;
    
    -- Create extensions
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";
    CREATE EXTENSION IF NOT EXISTS "pg_trgm";
    
    -- Set timezone
    SET timezone = 'UTC';
    
    -- Create monitoring user for health checks
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'healthcheck') THEN
            CREATE ROLE healthcheck WITH LOGIN PASSWORD 'healthcheck';
        END IF;
    END
    \$\$;
    
    GRANT CONNECT ON DATABASE $POSTGRES_DB TO healthcheck;
    GRANT USAGE ON SCHEMA public TO healthcheck;
    
    -- Log initialization completion
    INSERT INTO pg_stat_statements_info (dealloc) VALUES (0) ON CONFLICT DO NOTHING;
EOSQL

echo "FocusHive database initialization completed."