#!/bin/bash
set -e

# PostgreSQL Development Identity Service Database Initialization Script
# Extended configuration for development environment

echo "Starting Identity Service development database initialization..."

# Create development-specific configuration for Identity Service
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Development-specific schemas for Identity Service
    CREATE SCHEMA IF NOT EXISTS oauth2;
    CREATE SCHEMA IF NOT EXISTS persona;
    CREATE SCHEMA IF NOT EXISTS privacy;
    CREATE SCHEMA IF NOT EXISTS audit;
    CREATE SCHEMA IF NOT EXISTS dev_tools;
    
    -- Grant permissions
    GRANT ALL PRIVILEGES ON SCHEMA oauth2 TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON SCHEMA persona TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON SCHEMA privacy TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON SCHEMA audit TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON SCHEMA dev_tools TO $POSTGRES_USER;
    
    -- Create extensions for Identity Service
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";
    CREATE EXTENSION IF NOT EXISTS "pg_trgm";
    CREATE EXTENSION IF NOT EXISTS "pgcrypto";
    CREATE EXTENSION IF NOT EXISTS "btree_gin";
    CREATE EXTENSION IF NOT EXISTS "pg_buffercache";
    
    -- Set timezone
    SET timezone = 'UTC';
    
    -- Create development users for Identity Service
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'identity_dev_user') THEN
            CREATE ROLE identity_dev_user WITH LOGIN PASSWORD 'identity_dev_password';
        END IF;
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'identity_test_user') THEN
            CREATE ROLE identity_test_user WITH LOGIN PASSWORD 'identity_test_password';
        END IF;
    END
    \$\$;
    
    GRANT ALL PRIVILEGES ON DATABASE $POSTGRES_DB TO identity_dev_user;
    GRANT ALL PRIVILEGES ON DATABASE $POSTGRES_DB TO identity_test_user;
    
    -- Development configuration for Identity Service (more verbose logging)
    ALTER SYSTEM SET log_statement = 'all';
    ALTER SYSTEM SET log_min_duration_statement = 0;
    ALTER SYSTEM SET log_line_prefix = '%t [%p]: [%l-1] identity_user=%u,db=%d,app=%a,client=%h ';
    ALTER SYSTEM SET log_checkpoints = on;
    ALTER SYSTEM SET log_connections = on;
    ALTER SYSTEM SET log_disconnections = on;
    ALTER SYSTEM SET log_lock_waits = on;
    ALTER SYSTEM SET log_temp_files = 0;
    
    -- Performance settings for development
    ALTER SYSTEM SET shared_preload_libraries = 'pg_stat_statements';
    ALTER SYSTEM SET track_activity_query_size = 2048;
    ALTER SYSTEM SET track_io_timing = on;
    
    -- Create Identity Service specific development helper functions
    CREATE OR REPLACE FUNCTION dev_tools.reset_oauth_tokens()
    RETURNS void AS \$reset_tokens\$
    BEGIN
        -- This will be implemented after tables are created by Flyway
        RAISE NOTICE 'OAuth token reset function created - will be functional after schema migration';
    END;
    \$reset_tokens\$ LANGUAGE plpgsql;
    
    CREATE OR REPLACE FUNCTION dev_tools.create_test_users(count integer DEFAULT 10)
    RETURNS void AS \$test_users\$
    BEGIN
        -- This will be implemented after tables are created by Flyway
        RAISE NOTICE 'Test user creation function created - will be functional after schema migration';
    END;
    \$test_users\$ LANGUAGE plpgsql;
    
    CREATE OR REPLACE FUNCTION dev_tools.cleanup_expired_tokens()
    RETURNS integer AS \$cleanup\$
    BEGIN
        -- This will be implemented after tables are created by Flyway
        RAISE NOTICE 'Token cleanup function created - will be functional after schema migration';
        RETURN 0;
    END;
    \$cleanup\$ LANGUAGE plpgsql;
    
    -- Grant usage to development users
    GRANT USAGE ON SCHEMA dev_tools TO identity_dev_user, identity_test_user;
    GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA dev_tools TO identity_dev_user, identity_test_user;
    GRANT USAGE ON ALL SCHEMAS TO identity_dev_user, identity_test_user;
EOSQL

echo "Identity Service development database initialization completed."