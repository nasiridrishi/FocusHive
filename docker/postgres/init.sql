-- FocusHive Database Initialization Script
-- Creates necessary schemas and extensions for all services

-- Create schemas for service isolation
CREATE SCHEMA IF NOT EXISTS focushive;
CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS analytics;

-- Set default schema
SET search_path TO focushive, public;

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";      -- For UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";       -- For encryption
CREATE EXTENSION IF NOT EXISTS "pg_trgm";        -- For text search
CREATE EXTENSION IF NOT EXISTS "btree_gin";      -- For composite indexes
CREATE EXTENSION IF NOT EXISTS "btree_gist";     -- For exclusion constraints

-- Create custom types
DO $$ BEGIN
    CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE hive_visibility AS ENUM ('PUBLIC', 'PRIVATE', 'INVITE_ONLY');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE presence_status AS ENUM ('ONLINE', 'AWAY', 'BUSY', 'OFFLINE');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Grant permissions to application user
GRANT ALL PRIVILEGES ON SCHEMA focushive TO focushive;
GRANT ALL PRIVILEGES ON SCHEMA identity TO focushive;
GRANT ALL PRIVILEGES ON SCHEMA analytics TO focushive;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA focushive TO focushive;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA focushive TO focushive;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA identity TO focushive;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA identity TO focushive;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA analytics TO focushive;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA analytics TO focushive;

-- Set default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA focushive
    GRANT ALL PRIVILEGES ON TABLES TO focushive;
ALTER DEFAULT PRIVILEGES IN SCHEMA focushive
    GRANT ALL PRIVILEGES ON SEQUENCES TO focushive;
ALTER DEFAULT PRIVILEGES IN SCHEMA identity
    GRANT ALL PRIVILEGES ON TABLES TO focushive;
ALTER DEFAULT PRIVILEGES IN SCHEMA identity
    GRANT ALL PRIVILEGES ON SEQUENCES TO focushive;
ALTER DEFAULT PRIVILEGES IN SCHEMA analytics
    GRANT ALL PRIVILEGES ON TABLES TO focushive;
ALTER DEFAULT PRIVILEGES IN SCHEMA analytics
    GRANT ALL PRIVILEGES ON SEQUENCES TO focushive;

-- Create initial indexes for common queries (Flyway will handle the rest)
CREATE INDEX IF NOT EXISTS idx_created_at ON focushive.users USING btree (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_email_lower ON focushive.users USING btree (lower(email));
CREATE INDEX IF NOT EXISTS idx_username_lower ON focushive.users USING btree (lower(username));

-- Performance tuning for Docker environment
ALTER SYSTEM SET shared_buffers = '256MB';
ALTER SYSTEM SET effective_cache_size = '1GB';
ALTER SYSTEM SET maintenance_work_mem = '64MB';
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
ALTER SYSTEM SET wal_buffers = '16MB';
ALTER SYSTEM SET default_statistics_target = 100;
ALTER SYSTEM SET random_page_cost = 1.1;
ALTER SYSTEM SET effective_io_concurrency = 200;
ALTER SYSTEM SET work_mem = '4MB';
ALTER SYSTEM SET min_wal_size = '1GB';
ALTER SYSTEM SET max_wal_size = '4GB';

-- Connection pooling settings
ALTER SYSTEM SET max_connections = 200;
ALTER SYSTEM SET superuser_reserved_connections = 3;

-- Statement timeout for safety
ALTER SYSTEM SET statement_timeout = '30s';
ALTER SYSTEM SET lock_timeout = '10s';
ALTER SYSTEM SET idle_in_transaction_session_timeout = '60s';

-- Logging for development (disable in production)
ALTER SYSTEM SET log_statement = 'all';
ALTER SYSTEM SET log_duration = on;
ALTER SYSTEM SET log_min_duration_statement = 100;

-- Apply configuration changes
SELECT pg_reload_conf();