-- ===================================================================
-- FOCUSHIVE E2E TEST DATA SEEDING
-- 
-- This script seeds test data for all microservices during E2E testing
-- Provides consistent test data for the 558 E2E test scenarios
-- ===================================================================

-- Disable notices and info messages during seeding
SET client_min_messages TO WARNING;

-- ===================================================================
-- SHARED REFERENCE DATA
-- ===================================================================

-- Create test users table (will be replicated across services as needed)
CREATE TABLE IF NOT EXISTS test_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    email_verified BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Test user data for E2E scenarios
INSERT INTO test_users (id, email, username, password_hash, first_name, last_name) VALUES
-- Standard test users
('11111111-1111-1111-1111-111111111111', 'alice@test.com', 'alice_test', '$2a$10$N9qo8uLOickgx2ZMRZoMye1uiTzYhCGPP4pYPTBGOiGSAqEOQ2CJm', 'Alice', 'Johnson'),
('22222222-2222-2222-2222-222222222222', 'bob@test.com', 'bob_test', '$2a$10$N9qo8uLOickgx2ZMRZoMye1uiTzYhCGPP4pYPTBGOiGSAqEOQ2CJm', 'Bob', 'Smith'),
('33333333-3333-3333-3333-333333333333', 'charlie@test.com', 'charlie_test', '$2a$10$N9qo8uLOickgx2ZMRZoMye1uiTzYhCGPP4pYPTBGOiGSAqEOQ2CJm', 'Charlie', 'Brown'),
('44444444-4444-4444-4444-444444444444', 'diana@test.com', 'diana_test', '$2a$10$N9qo8uLOickgx2ZMRZoMye1uiTzYhCGPP4pYPTBGOiGSAqEOQ2CJm', 'Diana', 'Wilson'),
('55555555-5555-5555-5555-555555555555', 'eve@test.com', 'eve_test', '$2a$10$N9qo8uLOickgx2ZMRZoMye1uiTzYhCGPP4pYPTBGOiGSAqEOQ2CJm', 'Eve', 'Davis'),

-- Admin user for administrative tests
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'admin@test.com', 'admin_test', '$2a$10$N9qo8uLOickgx2ZMRZoMye1uiTzYhCGPP4pYPTBGOiGSAqEOQ2CJm', 'Admin', 'User'),

-- Users for specific test scenarios
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'newuser@test.com', 'newuser_test', '$2a$10$N9qo8uLOickgx2ZMRZoMye1uiTzYhCGPP4pYPTBGOiGSAqEOQ2CJm', 'New', 'User'),
('cccccccc-cccc-cccc-cccc-cccccccccccc', 'moderator@test.com', 'moderator_test', '$2a$10$N9qo8uLOickgx2ZMRZoMye1uiTzYhCGPP4pYPTBGOiGSAqEOQ2CJm', 'Moderator', 'User'),

-- Bulk users for load testing scenarios  
('66666666-6666-6666-6666-666666666666', 'user1@test.com', 'user1_test', '$2a$10$N9qo8uLOickgx2ZMRZoMye1uiTzYhCGPP4pYPTBGOiGSAqEOQ2CJm', 'User', 'One'),
('77777777-7777-7777-7777-777777777777', 'user2@test.com', 'user2_test', '$2a$10$N9qo8uLOickgx2ZMRZoMye1uiTzYhCGPP4pYPTBGOiGSAqEOQ2CJm', 'User', 'Two'),
('88888888-8888-8888-8888-888888888888', 'user3@test.com', 'user3_test', '$2a$10$N9qo8uLOickgx2ZMRZoMye1uiTzYhCGPP4pYPTBGOiGSAqEOQ2CJm', 'User', 'Three'),
('99999999-9999-9999-9999-999999999999', 'user4@test.com', 'user4_test', '$2a$10$N9qo8uLOickgx2ZMRZoMye1uiTzYhCGPP4pYPTBGOiGSAqEOQ2CJm', 'User', 'Four')
ON CONFLICT (email) DO NOTHING;

-- ===================================================================
-- TEST CONFIGURATION DATA
-- ===================================================================

-- Test configuration for feature flags
CREATE TABLE IF NOT EXISTS test_config (
    key VARCHAR(255) PRIMARY KEY,
    value TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO test_config (key, value, description) VALUES
('features.chat.enabled', 'true', 'Enable chat functionality in tests'),
('features.music.enabled', 'true', 'Enable music service integration'),
('features.notifications.email', 'true', 'Enable email notifications'),
('features.analytics.tracking', 'true', 'Enable analytics tracking'),
('features.buddy.matching', 'true', 'Enable buddy matching system'),
('features.forum.moderation', 'true', 'Enable forum moderation'),
('test.environment', 'e2e', 'Test environment identifier'),
('test.data.version', '1.0.0', 'Test data schema version')
ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;

-- Test rate limits for API testing
CREATE TABLE IF NOT EXISTS test_rate_limits (
    endpoint VARCHAR(255) PRIMARY KEY,
    requests_per_minute INTEGER NOT NULL,
    burst_limit INTEGER NOT NULL
);

INSERT INTO test_rate_limits (endpoint, requests_per_minute, burst_limit) VALUES
('/api/auth/login', 100, 20),
('/api/hives', 200, 50),
('/api/timer', 300, 75),
('/api/presence', 500, 100),
('/api/chat', 400, 80),
('/ws/presence', 1000, 200)
ON CONFLICT (endpoint) DO UPDATE SET 
    requests_per_minute = EXCLUDED.requests_per_minute,
    burst_limit = EXCLUDED.burst_limit;

-- ===================================================================
-- AUDIT AND MONITORING SETUP
-- ===================================================================

-- Test audit log table
CREATE TABLE IF NOT EXISTS audit.test_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    user_id UUID,
    resource_type VARCHAR(100),
    resource_id VARCHAR(255),
    details JSONB,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    test_scenario VARCHAR(255)
);

-- Indexes for efficient test queries
CREATE INDEX IF NOT EXISTS idx_test_events_user_id ON audit.test_events (user_id);
CREATE INDEX IF NOT EXISTS idx_test_events_timestamp ON audit.test_events (timestamp);
CREATE INDEX IF NOT EXISTS idx_test_events_scenario ON audit.test_events (test_scenario);

-- Test metrics table for performance monitoring
CREATE TABLE IF NOT EXISTS config.test_metrics (
    id BIGSERIAL PRIMARY KEY,
    metric_name VARCHAR(255) NOT NULL,
    metric_value NUMERIC NOT NULL,
    test_case VARCHAR(255),
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

CREATE INDEX IF NOT EXISTS idx_test_metrics_name_time ON config.test_metrics (metric_name, recorded_at);

-- ===================================================================
-- CLEANUP FUNCTIONS FOR TESTS
-- ===================================================================

-- Function to reset test data between test runs
CREATE OR REPLACE FUNCTION reset_test_data() RETURNS void AS $$
BEGIN
    -- Truncate audit data but keep test users and config
    TRUNCATE TABLE audit.test_events RESTART IDENTITY CASCADE;
    TRUNCATE TABLE config.test_metrics RESTART IDENTITY CASCADE;
    
    -- Reset user states to default
    UPDATE test_users SET 
        is_active = true,
        email_verified = true,
        updated_at = CURRENT_TIMESTAMP;
        
    RAISE NOTICE 'Test data reset completed';
END;
$$ LANGUAGE plpgsql;

-- Function to create test scenario data
CREATE OR REPLACE FUNCTION create_test_scenario(scenario_name TEXT) RETURNS void AS $$
BEGIN
    INSERT INTO audit.test_events (event_type, details, test_scenario)
    VALUES ('test_scenario_start', json_build_object('scenario', scenario_name), scenario_name);
    
    RAISE NOTICE 'Test scenario % initialized', scenario_name;
END;
$$ LANGUAGE plpgsql;

-- ===================================================================
-- INITIAL DATA VALIDATION
-- ===================================================================

-- Verify test data was inserted correctly
DO $$ 
DECLARE
    user_count INTEGER;
    config_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO user_count FROM test_users;
    SELECT COUNT(*) INTO config_count FROM test_config;
    
    RAISE NOTICE 'Test data seeding completed:';
    RAISE NOTICE '  - Test users: %', user_count;
    RAISE NOTICE '  - Config entries: %', config_count;
    
    IF user_count < 12 THEN
        RAISE EXCEPTION 'Insufficient test users created. Expected at least 12, got %', user_count;
    END IF;
    
    IF config_count < 8 THEN
        RAISE EXCEPTION 'Insufficient config entries created. Expected at least 8, got %', config_count;
    END IF;
END $$;

-- Create a test marker to verify this script ran
CREATE TABLE IF NOT EXISTS test_seed_status (
    seeded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version VARCHAR(50) DEFAULT '1.0.0',
    notes TEXT
);

INSERT INTO test_seed_status (notes) VALUES ('E2E test data seeding completed successfully');

COMMIT;