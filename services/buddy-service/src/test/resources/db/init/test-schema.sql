-- TestContainers initialization script for buddy-service integration tests
-- This script prepares the database for TestContainers integration testing

-- Create any necessary extensions (safe to run multiple times)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Set timezone for consistent test results
SET TIME ZONE 'UTC';

-- Log successful initialization
DO $$
BEGIN
    RAISE NOTICE 'TestContainers database initialization completed successfully';
    RAISE NOTICE 'Database: %', current_database();
    RAISE NOTICE 'User: %', current_user;
    RAISE NOTICE 'Timestamp: %', now();
END $$;