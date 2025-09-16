-- PostgreSQL Initialization Script for Notification Service
-- This script runs when the container is first created

-- Create database if not exists (backup, in case POSTGRES_DB env var fails)
SELECT 'CREATE DATABASE notification_service'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notification_service')\gexec

-- Connect to the notification_service database
\c notification_service;

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS notification;

-- Set search path
SET search_path TO notification, public;

-- Grant permissions to the notification_user
GRANT ALL PRIVILEGES ON SCHEMA notification TO notification_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA notification TO notification_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA notification TO notification_user;
GRANT ALL PRIVILEGES ON DATABASE notification_service TO notification_user;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Performance optimizations
ALTER DATABASE notification_service SET shared_preload_libraries = 'pg_stat_statements';
ALTER DATABASE notification_service SET log_statement = 'all';
ALTER DATABASE notification_service SET log_duration = on;

-- Connection settings
ALTER DATABASE notification_service SET max_connections = 200;
ALTER DATABASE notification_service SET shared_buffers = '256MB';
ALTER DATABASE notification_service SET effective_cache_size = '1GB';

-- Create audit table for tracking
CREATE TABLE IF NOT EXISTS audit_log (
    id SERIAL PRIMARY KEY,
    table_name VARCHAR(100),
    operation VARCHAR(10),
    user_name VARCHAR(100),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data JSONB
);

-- Create indexes on audit table
CREATE INDEX IF NOT EXISTS idx_audit_log_timestamp ON audit_log(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_log_table_name ON audit_log(table_name);

-- Output confirmation
\echo 'Database initialization completed successfully!'