-- PostgreSQL initialization script for FocusHive Backend
-- This runs when the PostgreSQL container is first created

-- Create extensions if they don't exist
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Ensure the database exists and user has proper permissions
-- Note: Database and user are created by environment variables in docker-compose
-- This script ensures proper permissions and extensions

-- Grant all privileges on the database to the focushive user
GRANT ALL PRIVILEGES ON DATABASE focushive TO focushive;

-- Create a function for updated_at triggers if it doesn't exist
CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Ensure the user can create schemas and objects
ALTER USER focushive CREATEDB;

-- Log successful initialization
DO $$
BEGIN
  RAISE NOTICE 'FocusHive database initialization completed successfully';
END $$;