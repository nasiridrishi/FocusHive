-- TestContainers PostgreSQL Initialization Script
-- This script sets up the test database with basic configuration

-- Create extensions if available (ignore errors if not available in test environment)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Set up basic configuration
SET timezone = 'UTC';

-- Create a simple validation table for testing
CREATE TABLE IF NOT EXISTS test_validation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insert a test record to validate container is working
INSERT INTO test_validation (name) VALUES ('TestContainers Init Success');

-- Log successful initialization
DO $$
BEGIN
    RAISE NOTICE 'TestContainers PostgreSQL initialization completed successfully';
END
$$;