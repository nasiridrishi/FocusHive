#!/bin/bash
set -e

# PostgreSQL Development Database Initialization Script
# Extended configuration for development environment

echo "Starting FocusHive development database initialization..."

# Create development-specific configuration
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Development-specific schemas
    CREATE SCHEMA IF NOT EXISTS analytics;
    CREATE SCHEMA IF NOT EXISTS audit;
    CREATE SCHEMA IF NOT EXISTS cache;
    CREATE SCHEMA IF NOT EXISTS dev_tools;
    
    -- Grant permissions
    GRANT ALL PRIVILEGES ON SCHEMA analytics TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON SCHEMA audit TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON SCHEMA cache TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON SCHEMA dev_tools TO $POSTGRES_USER;
    
    -- Create extensions
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";
    CREATE EXTENSION IF NOT EXISTS "pg_trgm";
    CREATE EXTENSION IF NOT EXISTS "pgcrypto";
    CREATE EXTENSION IF NOT EXISTS "btree_gin";
    CREATE EXTENSION IF NOT EXISTS "pg_buffercache";
    
    -- Set timezone
    SET timezone = 'UTC';
    
    -- Create development users
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'dev_user') THEN
            CREATE ROLE dev_user WITH LOGIN PASSWORD 'dev_password';
        END IF;
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'test_user') THEN
            CREATE ROLE test_user WITH LOGIN PASSWORD 'test_password';
        END IF;
    END
    \$\$;
    
    GRANT ALL PRIVILEGES ON DATABASE $POSTGRES_DB TO dev_user;
    GRANT ALL PRIVILEGES ON DATABASE $POSTGRES_DB TO test_user;
    
    -- Development configuration (more permissive)
    ALTER SYSTEM SET log_statement = 'all';
    ALTER SYSTEM SET log_min_duration_statement = 0;
    ALTER SYSTEM SET log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d,app=%a,client=%h ';
    ALTER SYSTEM SET log_checkpoints = on;
    ALTER SYSTEM SET log_connections = on;
    ALTER SYSTEM SET log_disconnections = on;
    ALTER SYSTEM SET log_lock_waits = on;
    ALTER SYSTEM SET log_temp_files = 0;
    
    -- Performance settings for development
    ALTER SYSTEM SET shared_preload_libraries = 'pg_stat_statements';
    ALTER SYSTEM SET track_activity_query_size = 2048;
    ALTER SYSTEM SET track_io_timing = on;
    
    -- Create development helper functions
    CREATE OR REPLACE FUNCTION dev_tools.reset_sequences()
    RETURNS void AS \$reset\$
    DECLARE
        rec RECORD;
    BEGIN
        FOR rec IN SELECT schemaname, sequencename FROM pg_sequences WHERE schemaname NOT IN ('information_schema', 'pg_catalog')
        LOOP
            EXECUTE 'ALTER SEQUENCE ' || quote_ident(rec.schemaname) || '.' || quote_ident(rec.sequencename) || ' RESTART WITH 1';
        END LOOP;
    END;
    \$reset\$ LANGUAGE plpgsql;
    
    CREATE OR REPLACE FUNCTION dev_tools.truncate_all_tables()
    RETURNS void AS \$truncate\$
    DECLARE
        rec RECORD;
    BEGIN
        FOR rec IN SELECT schemaname, tablename FROM pg_tables WHERE schemaname NOT IN ('information_schema', 'pg_catalog')
        LOOP
            EXECUTE 'TRUNCATE TABLE ' || quote_ident(rec.schemaname) || '.' || quote_ident(rec.tablename) || ' CASCADE';
        END LOOP;
    END;
    \$truncate\$ LANGUAGE plpgsql;
    
    -- Grant usage to development users
    GRANT USAGE ON SCHEMA dev_tools TO dev_user, test_user;
    GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA dev_tools TO dev_user, test_user;
EOSQL

echo "FocusHive development database initialization completed."