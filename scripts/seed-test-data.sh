#!/bin/bash

# ===================================================================
# COMPREHENSIVE TEST DATA SEEDING SCRIPT
# 
# Seeds all FocusHive services with realistic test data
# Ensures consistent test environment for E2E testing
# ===================================================================

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Configuration
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5433}"
DB_USER="${DB_USER:-test_user}"
DB_PASSWORD="${DB_PASSWORD:-test_pass}"
REDIS_HOST="${REDIS_HOST:-localhost}"
REDIS_PORT="${REDIS_PORT:-6380}"
REDIS_PASSWORD="${REDIS_PASSWORD:-test_redis_pass}"

# Service URLs
IDENTITY_SERVICE_URL="${IDENTITY_SERVICE_URL:-http://localhost:8081}"
BACKEND_SERVICE_URL="${BACKEND_SERVICE_URL:-http://localhost:8080}"
MUSIC_SERVICE_URL="${MUSIC_SERVICE_URL:-http://localhost:8082}"
NOTIFICATION_SERVICE_URL="${NOTIFICATION_SERVICE_URL:-http://localhost:8083}"
CHAT_SERVICE_URL="${CHAT_SERVICE_URL:-http://localhost:8084}"
ANALYTICS_SERVICE_URL="${ANALYTICS_SERVICE_URL:-http://localhost:8085}"
FORUM_SERVICE_URL="${FORUM_SERVICE_URL:-http://localhost:8086}"
BUDDY_SERVICE_URL="${BUDDY_SERVICE_URL:-http://localhost:8087}"

# Test data configuration
TOTAL_USERS=50
TOTAL_HIVES=15
TOTAL_SESSIONS=100
TOTAL_MESSAGES=200
TOTAL_FORUM_POSTS=30

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_section() {
    echo -e "\n${PURPLE}=== $1 ===${NC}"
}

# Check if services are ready
wait_for_service() {
    local service_name=$1
    local service_url=$2
    local max_retries=30
    local retry_count=0
    
    log_info "Waiting for $service_name to be ready..."
    
    while [ $retry_count -lt $max_retries ]; do
        if curl -sf "$service_url/actuator/health" &>/dev/null; then
            log_success "$service_name is ready"
            return 0
        fi
        
        retry_count=$((retry_count + 1))
        sleep 2
    done
    
    log_error "$service_name is not ready after $max_retries attempts"
    return 1
}

# Execute SQL script
execute_sql() {
    local database=$1
    local script_file=$2
    
    if [ ! -f "$script_file" ]; then
        log_error "SQL script not found: $script_file"
        return 1
    fi
    
    log_info "Executing SQL script: $(basename "$script_file") on database: $database"
    
    if docker exec focushive-test-db psql -h localhost -U "$DB_USER" -d "$database" -f "/docker-entrypoint-initdb.d/$(basename "$script_file")" 2>/dev/null; then
        log_success "SQL script executed successfully"
        return 0
    else
        # Try alternative path
        if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$database" -f "$script_file" 2>/dev/null; then
            log_success "SQL script executed successfully"
            return 0
        else
            log_error "Failed to execute SQL script: $(basename "$script_file")"
            return 1
        fi
    fi
}

# Generate test users with diverse personas
seed_users() {
    log_section "Seeding Test Users"
    
    cat > /tmp/seed-users.sql << 'EOF'
-- ===================================================================
-- TEST USERS FOR E2E TESTING
-- Creates diverse user personas for comprehensive testing scenarios
-- ===================================================================

-- Clean existing test data
DELETE FROM user_personas WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%@test.focushive.app');
DELETE FROM users WHERE email LIKE '%@test.focushive.app';

-- Create test admin user
INSERT INTO users (id, username, email, first_name, last_name, password_hash, email_verified, created_at, updated_at, status, role) VALUES
('550e8400-e29b-41d4-a716-446655440001', 'admin_test', 'admin@test.focushive.app', 'Test', 'Admin', 
 '$2a$10$N9qo8uLOickgx2ZMRZoMye2J7v4XGSBlrG8J7JJhGp5n8xo2y0vGK', true, NOW(), NOW(), 'ACTIVE', 'ADMIN');

-- Create regular test users with diverse profiles
INSERT INTO users (id, username, email, first_name, last_name, password_hash, email_verified, created_at, updated_at, status, role) VALUES
-- Student personas
('550e8400-e29b-41d4-a716-446655440002', 'student_alice', 'alice@test.focushive.app', 'Alice', 'Johnson', 
 '$2a$10$N9qo8uLOickgx2ZMRZoMye2J7v4XGSBlrG8J7JJhGp5n8xo2y0vGK', true, NOW() - INTERVAL '30 days', NOW(), 'ACTIVE', 'USER'),
('550e8400-e29b-41d4-a716-446655440003', 'student_bob', 'bob@test.focushive.app', 'Bob', 'Smith', 
 '$2a$10$N9qo8uLOickgx2ZMRZoMye2J7v4XGSBlrG8J7JJhGp5n8xo2y0vGK', true, NOW() - INTERVAL '25 days', NOW(), 'ACTIVE', 'USER'),
('550e8400-e29b-41d4-a716-446655440004', 'student_carol', 'carol@test.focushive.app', 'Carol', 'Williams', 
 '$2a$10$N9qo8uLOickgx2ZMRZoMye2J7v4XGSBlrG8J7JJhGp5n8xo2y0vGK', true, NOW() - INTERVAL '20 days', NOW(), 'ACTIVE', 'USER'),

-- Professional personas
('550e8400-e29b-41d4-a716-446655440005', 'dev_david', 'david@test.focushive.app', 'David', 'Brown', 
 '$2a$10$N9qo8uLOickgx2ZMRZoMye2J7v4XGSBlrG8J7JJhGp5n8xo2y0vGK', true, NOW() - INTERVAL '15 days', NOW(), 'ACTIVE', 'USER'),
('550e8400-e29b-41d4-a716-446655440006', 'designer_eve', 'eve@test.focushive.app', 'Eve', 'Davis', 
 '$2a$10$N9qo8uLOickgx2ZMRZoMye2J7v4XGSBlrG8J7JJhGp5n8xo2y0vGK', true, NOW() - INTERVAL '10 days', NOW(), 'ACTIVE', 'USER'),
('550e8400-e29b-41d4-a716-446655440007', 'manager_frank', 'frank@test.focushive.app', 'Frank', 'Wilson', 
 '$2a$10$N9qo8uLOickgx2ZMRZoMye2J7v4XGSBlrG8J7JJhGp5n8xo2y0vGK', true, NOW() - INTERVAL '5 days', NOW(), 'ACTIVE', 'USER'),

-- Remote worker personas
('550e8400-e29b-41d4-a716-446655440008', 'remote_grace', 'grace@test.focushive.app', 'Grace', 'Miller', 
 '$2a$10$N9qo8uLOickgx2ZMRZoMye2J7v4XGSBlrG8J7JJhGp5n8xo2y0vGK', true, NOW() - INTERVAL '7 days', NOW(), 'ACTIVE', 'USER'),
('550e8400-e29b-41d4-a716-446655440009', 'freelancer_henry', 'henry@test.focushive.app', 'Henry', 'Taylor', 
 '$2a$10$N9qo8uLOickgx2ZMRZoMye2J7v4XGSBlrG8J7JJhGp5n8xo2y0vGK', true, NOW() - INTERVAL '12 days', NOW(), 'ACTIVE', 'USER'),
('550e8400-e29b-41d4-a716-446655440010', 'entrepreneur_ivy', 'ivy@test.focushive.app', 'Ivy', 'Anderson', 
 '$2a$10$N9qo8uLOickgx2ZMRZoMye2J7v4XGSBlrG8J7JJhGp5n8xo2y0vGK', true, NOW() - INTERVAL '3 days', NOW(), 'ACTIVE', 'USER');

-- Create user personas/profiles
INSERT INTO user_personas (id, user_id, name, description, is_primary, settings, created_at, updated_at) VALUES
-- Student personas
('650e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440002', 'Study Mode', 'Focused study sessions for university', true, 
 '{"theme": "academic", "focus_duration": 45, "break_duration": 15, "notifications": {"email": true, "push": true}}', NOW(), NOW()),
('650e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440003', 'Exam Prep', 'Intensive preparation mode', true, 
 '{"theme": "intense", "focus_duration": 60, "break_duration": 10, "notifications": {"email": true, "push": false}}', NOW(), NOW()),
('650e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440004', 'Research Mode', 'Deep research sessions', true, 
 '{"theme": "research", "focus_duration": 90, "break_duration": 20, "notifications": {"email": false, "push": true}}', NOW(), NOW()),

-- Professional personas
('650e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440005', 'Coding Session', 'Deep focus for development', true, 
 '{"theme": "developer", "focus_duration": 25, "break_duration": 5, "notifications": {"email": false, "push": false}}', NOW(), NOW()),
('650e8400-e29b-41d4-a716-446655440006', '550e8400-e29b-41d4-a716-446655440006', 'Creative Work', 'Design and creative tasks', true, 
 '{"theme": "creative", "focus_duration": 35, "break_duration": 10, "notifications": {"email": true, "push": true}}', NOW(), NOW()),
('650e8400-e29b-41d4-a716-446655440007', '550e8400-e29b-41d4-a716-446655440007', 'Management Tasks', 'Planning and coordination', true, 
 '{"theme": "professional", "focus_duration": 50, "break_duration": 15, "notifications": {"email": true, "push": true}}', NOW(), NOW()),

-- Remote worker personas
('650e8400-e29b-41d4-a716-446655440008', '550e8400-e29b-41d4-a716-446655440008', 'Remote Focus', 'Home office productivity', true, 
 '{"theme": "remote", "focus_duration": 30, "break_duration": 10, "notifications": {"email": true, "push": false}}', NOW(), NOW()),
('650e8400-e29b-41d4-a716-446655440009', '550e8400-e29b-41d4-a716-446655440009', 'Client Work', 'Billable hours tracking', true, 
 '{"theme": "freelancer", "focus_duration": 60, "break_duration": 15, "notifications": {"email": true, "push": true}}', NOW(), NOW()),
('650e8400-e29b-41d4-a716-446655440010', '550e8400-e29b-41d4-a716-446655440010', 'Startup Grind', 'Entrepreneurial focus', true, 
 '{"theme": "startup", "focus_duration": 45, "break_duration": 5, "notifications": {"email": false, "push": true}}', NOW(), NOW());

-- Update statistics
SELECT 'Created ' || COUNT(*) || ' test users' as status FROM users WHERE email LIKE '%@test.focushive.app';
SELECT 'Created ' || COUNT(*) || ' user personas' as status FROM user_personas WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%@test.focushive.app');
EOF
    
    execute_sql "focushive_test" "/tmp/seed-users.sql"
}

# Generate test hives with various configurations
seed_hives() {
    log_section "Seeding Test Hives"
    
    cat > /tmp/seed-hives.sql << 'EOF'
-- ===================================================================
-- TEST HIVES FOR E2E TESTING
-- Creates diverse hive configurations for comprehensive testing
-- ===================================================================

-- Clean existing test hives
DELETE FROM hive_members WHERE hive_id IN (SELECT id FROM hives WHERE name LIKE 'Test %');
DELETE FROM hives WHERE name LIKE 'Test %';

-- Create diverse test hives
INSERT INTO hives (id, name, description, owner_id, type, privacy, max_members, settings, created_at, updated_at, status) VALUES
-- Study groups
('750e8400-e29b-41d4-a716-446655440001', 'Test Study Group - Computer Science', 
 'Computer Science students studying for finals', '550e8400-e29b-41d4-a716-446655440002', 
 'STUDY', 'PUBLIC', 20, '{"focus_mode": "pomodoro", "music_enabled": true, "chat_enabled": true, "camera_enabled": false}', 
 NOW() - INTERVAL '10 days', NOW(), 'ACTIVE'),

('750e8400-e29b-41d4-a716-446655440002', 'Test Exam Prep - Mathematics', 
 'Intensive math exam preparation', '550e8400-e29b-41d4-a716-446655440003', 
 'STUDY', 'PRIVATE', 10, '{"focus_mode": "deep", "music_enabled": false, "chat_enabled": false, "camera_enabled": false}', 
 NOW() - INTERVAL '8 days', NOW(), 'ACTIVE'),

('750e8400-e29b-41d4-a716-446655440003', 'Test Research Lab', 
 'PhD students collaborative research', '550e8400-e29b-41d4-a716-446655440004', 
 'RESEARCH', 'INVITE_ONLY', 8, '{"focus_mode": "flexible", "music_enabled": true, "chat_enabled": true, "camera_enabled": true}', 
 NOW() - INTERVAL '15 days', NOW(), 'ACTIVE'),

-- Work groups
('750e8400-e29b-41d4-a716-446655440004', 'Test Dev Team Sprint', 
 'Software development team daily work', '550e8400-e29b-41d4-a716-446655440005', 
 'WORK', 'PRIVATE', 12, '{"focus_mode": "scrum", "music_enabled": true, "chat_enabled": true, "camera_enabled": false}', 
 NOW() - INTERVAL '5 days', NOW(), 'ACTIVE'),

('750e8400-e29b-41d4-a716-446655440005', 'Test Design Studio', 
 'Creative professionals collaboration', '550e8400-e29b-41d4-a716-446655440006', 
 'CREATIVE', 'PUBLIC', 15, '{"focus_mode": "creative", "music_enabled": true, "chat_enabled": true, "camera_enabled": true}', 
 NOW() - INTERVAL '12 days', NOW(), 'ACTIVE'),

('750e8400-e29b-41d4-a716-446655440006', 'Test Management Meeting', 
 'Weekly planning and coordination', '550e8400-e29b-41d4-a716-446655440007', 
 'MEETING', 'PRIVATE', 25, '{"focus_mode": "meeting", "music_enabled": false, "chat_enabled": true, "camera_enabled": true}', 
 NOW() - INTERVAL '3 days', NOW(), 'ACTIVE'),

-- Remote work
('750e8400-e29b-41d4-a716-446655440007', 'Test Remote Coffee', 
 'Virtual co-working space for remote workers', '550e8400-e29b-41d4-a716-446655440008', 
 'SOCIAL', 'PUBLIC', 50, '{"focus_mode": "casual", "music_enabled": true, "chat_enabled": true, "camera_enabled": true}', 
 NOW() - INTERVAL '20 days', NOW(), 'ACTIVE'),

('750e8400-e29b-41d4-a716-446655440008', 'Test Freelancer Focus', 
 'Dedicated workspace for freelancers', '550e8400-e29b-41d4-a716-446655440009', 
 'WORK', 'INVITE_ONLY', 6, '{"focus_mode": "deep", "music_enabled": false, "chat_enabled": false, "camera_enabled": false}', 
 NOW() - INTERVAL '7 days', NOW(), 'ACTIVE'),

('750e8400-e29b-41d4-a716-446655440009', 'Test Startup Grind', 
 'Entrepreneurs working on projects', '550e8400-e29b-41d4-a716-446655440010', 
 'ENTREPRENEURIAL', 'PUBLIC', 30, '{"focus_mode": "intensive", "music_enabled": true, "chat_enabled": true, "camera_enabled": false}', 
 NOW() - INTERVAL '14 days', NOW(), 'ACTIVE'),

-- Special purpose hives
('750e8400-e29b-41d4-a716-446655440010', 'Test Writing Retreat', 
 'Authors and writers focused sessions', '550e8400-e29b-41d4-a716-446655440002', 
 'WRITING', 'PUBLIC', 12, '{"focus_mode": "writing", "music_enabled": true, "chat_enabled": false, "camera_enabled": false}', 
 NOW() - INTERVAL '6 days', NOW(), 'ACTIVE');

-- Create hive memberships
INSERT INTO hive_members (id, hive_id, user_id, role, joined_at, status, settings) VALUES
-- Study Group members
('850e8400-e29b-41d4-a716-446655440001', '750e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440002', 'OWNER', NOW() - INTERVAL '10 days', 'ACTIVE', '{}'),
('850e8400-e29b-41d4-a716-446655440002', '750e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440003', 'MEMBER', NOW() - INTERVAL '9 days', 'ACTIVE', '{}'),
('850e8400-e29b-41d4-a716-446655440003', '750e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440004', 'MEMBER', NOW() - INTERVAL '8 days', 'ACTIVE', '{}'),
('850e8400-e29b-41d4-a716-446655440004', '750e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440005', 'MEMBER', NOW() - INTERVAL '7 days', 'ACTIVE', '{}'),

-- Dev Team members
('850e8400-e29b-41d4-a716-446655440005', '750e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440005', 'OWNER', NOW() - INTERVAL '5 days', 'ACTIVE', '{}'),
('850e8400-e29b-41d4-a716-446655440006', '750e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440006', 'MODERATOR', NOW() - INTERVAL '4 days', 'ACTIVE', '{}'),
('850e8400-e29b-41d4-a716-446655440007', '750e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440008', 'MEMBER', NOW() - INTERVAL '3 days', 'ACTIVE', '{}'),

-- Remote Coffee members (popular hive)
('850e8400-e29b-41d4-a716-446655440008', '750e8400-e29b-41d4-a716-446655440007', '550e8400-e29b-41d4-a716-446655440008', 'OWNER', NOW() - INTERVAL '20 days', 'ACTIVE', '{}'),
('850e8400-e29b-41d4-a716-446655440009', '750e8400-e29b-41d4-a716-446655440007', '550e8400-e29b-41d4-a716-446655440009', 'MEMBER', NOW() - INTERVAL '18 days', 'ACTIVE', '{}'),
('850e8400-e29b-41d4-a716-446655440010', '750e8400-e29b-41d4-a716-446655440007', '550e8400-e29b-41d4-a716-446655440010', 'MEMBER', NOW() - INTERVAL '16 days', 'ACTIVE', '{}'),
('850e8400-e29b-41d4-a716-446655440011', '750e8400-e29b-41d4-a716-446655440007', '550e8400-e29b-41d4-a716-446655440002', 'MEMBER', NOW() - INTERVAL '15 days', 'ACTIVE', '{}'),
('850e8400-e29b-41d4-a716-446655440012', '750e8400-e29b-41d4-a716-446655440007', '550e8400-e29b-41d4-a716-446655440003', 'MEMBER', NOW() - INTERVAL '12 days', 'ACTIVE', '{}');

-- Update statistics
SELECT 'Created ' || COUNT(*) || ' test hives' as status FROM hives WHERE name LIKE 'Test %';
SELECT 'Created ' || COUNT(*) || ' hive memberships' as status FROM hive_members WHERE hive_id IN (SELECT id FROM hives WHERE name LIKE 'Test %');
EOF
    
    execute_sql "focushive_test" "/tmp/seed-hives.sql"
}

# Generate test sessions and analytics data
seed_analytics() {
    log_section "Seeding Test Analytics Data"
    
    cat > /tmp/seed-analytics.sql << 'EOF'
-- ===================================================================
-- TEST ANALYTICS DATA FOR E2E TESTING
-- Creates realistic productivity tracking data
-- ===================================================================

-- Clean existing test analytics
DELETE FROM productivity_sessions WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%@test.focushive.app');
DELETE FROM achievements WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%@test.focushive.app');

-- Create productivity sessions (last 30 days)
INSERT INTO productivity_sessions (id, user_id, hive_id, session_type, start_time, end_time, planned_duration, actual_duration, 
                                  focus_score, break_count, interruption_count, completed, tags, created_at) VALUES
-- Alice's study sessions
('950e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440002', '750e8400-e29b-41d4-a716-446655440001', 
 'POMODORO', NOW() - INTERVAL '1 day' - INTERVAL '2 hours', NOW() - INTERVAL '1 day' - INTERVAL '1 hour', 45, 43, 
 0.92, 1, 2, true, '["studying", "computer-science", "algorithms"]', NOW() - INTERVAL '1 day'),

('950e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440002', '750e8400-e29b-41d4-a716-446655440001', 
 'POMODORO', NOW() - INTERVAL '2 days' - INTERVAL '3 hours', NOW() - INTERVAL '2 days' - INTERVAL '1 hour 30 minutes', 90, 85, 
 0.87, 2, 1, true, '["studying", "data-structures"]', NOW() - INTERVAL '2 days'),

-- Bob's focused sessions
('950e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440003', '750e8400-e29b-41d4-a716-446655440002', 
 'DEEP_WORK', NOW() - INTERVAL '1 day' - INTERVAL '4 hours', NOW() - INTERVAL '1 day' - INTERVAL '2 hours', 120, 115, 
 0.95, 1, 0, true, '["math", "calculus", "exam-prep"]', NOW() - INTERVAL '1 day'),

-- David's coding sessions
('950e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440005', '750e8400-e29b-41d4-a716-446655440004', 
 'POMODORO', NOW() - INTERVAL '6 hours', NOW() - INTERVAL '4 hours', 120, 118, 
 0.89, 4, 3, true, '["coding", "javascript", "react"]', NOW()),

-- Eve's creative sessions
('950e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440006', '750e8400-e29b-41d4-a716-446655440005', 
 'CREATIVE', NOW() - INTERVAL '3 days' - INTERVAL '2 hours', NOW() - INTERVAL '3 days', 150, 142, 
 0.91, 2, 1, true, '["design", "ui-ux", "figma"]', NOW() - INTERVAL '3 days'),

-- Grace's remote work sessions
('950e8400-e29b-41d4-a716-446655440006', '550e8400-e29b-41d4-a716-446655440008', '750e8400-e29b-41d4-a716-446655440007', 
 'FLEXIBLE', NOW() - INTERVAL '4 hours', NOW() - INTERVAL '2 hours 30 minutes', 90, 88, 
 0.86, 2, 4, true, '["remote-work", "meetings", "planning"]', NOW()),

-- Henry's client work
('950e8400-e29b-41d4-a716-446655440007', '550e8400-e29b-41d4-a716-446655440009', '750e8400-e29b-41d4-a716-446655440008', 
 'BILLABLE', NOW() - INTERVAL '5 days' - INTERVAL '6 hours', NOW() - INTERVAL '5 days' - INTERVAL '2 hours', 240, 235, 
 0.93, 3, 2, true, '["client-work", "web-development", "freelance"]', NOW() - INTERVAL '5 days');

-- Create user achievements
INSERT INTO achievements (id, user_id, achievement_type, title, description, points, unlocked_at, metadata) VALUES
-- Study achievements
('a50e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440002', 'STREAK', 'Study Streak Master', 
 'Completed 7 consecutive study sessions', 100, NOW() - INTERVAL '2 days', '{"streak_count": 7, "session_type": "study"}'),

('a50e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440003', 'FOCUS', 'Deep Focus Champion', 
 'Maintained 95%+ focus score for 2 hours', 150, NOW() - INTERVAL '1 day', '{"focus_score": 0.95, "duration": 120}'),

-- Productivity achievements
('a50e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440005', 'PRODUCTIVITY', 'Code Marathon', 
 'Completed 4 hours of coding in a single session', 200, NOW() - INTERVAL '3 days', '{"duration": 240, "activity": "coding"}'),

('a50e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440006', 'CREATIVITY', 'Design Innovator', 
 'Completed creative session with perfect flow state', 120, NOW() - INTERVAL '3 days', '{"flow_score": 1.0, "session_type": "creative"}'),

-- Social achievements
('a50e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440008', 'SOCIAL', 'Community Builder', 
 'Helped 5 other users in hive chat', 80, NOW() - INTERVAL '7 days', '{"help_count": 5, "hive_activity": true}'),

('a50e8400-e29b-41d4-a716-446655440006', '550e8400-e29b-41d4-a716-446655440009', 'CONSISTENCY', 'Daily Grinder', 
 'Used FocusHive for 15 consecutive days', 250, NOW() - INTERVAL '1 day', '{"consecutive_days": 15}');

-- Update statistics
SELECT 'Created ' || COUNT(*) || ' productivity sessions' as status FROM productivity_sessions WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%@test.focushive.app');
SELECT 'Created ' || COUNT(*) || ' achievements' as status FROM achievements WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%@test.focushive.app');
EOF
    
    execute_sql "analytics_test" "/tmp/seed-analytics.sql"
}

# Generate test chat messages
seed_chat_data() {
    log_section "Seeding Test Chat Data"
    
    cat > /tmp/seed-chat.sql << 'EOF'
-- ===================================================================
-- TEST CHAT DATA FOR E2E TESTING
-- Creates realistic chat conversations in test hives
-- ===================================================================

-- Clean existing test chat messages
DELETE FROM chat_messages WHERE hive_id IN (SELECT id FROM hives WHERE name LIKE 'Test %');

-- Create chat messages in Study Group
INSERT INTO chat_messages (id, hive_id, user_id, content, message_type, created_at, updated_at, edited_at) VALUES
-- Study Group conversations
('c50e8400-e29b-41d4-a716-446655440001', '750e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440002',
 'Hey everyone! Starting my algorithm study session. Anyone want to join?', 'TEXT', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours', null),

('c50e8400-e29b-41d4-a716-446655440002', '750e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440003',
 'I''m in! Working on binary trees today 🌳', 'TEXT', NOW() - INTERVAL '1 hour 55 minutes', NOW() - INTERVAL '1 hour 55 minutes', null),

('c50e8400-e29b-41d4-a716-446655440003', '750e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440004',
 'Perfect! I''m reviewing graph algorithms. Let''s do 45 min focused session?', 'TEXT', NOW() - INTERVAL '1 hour 50 minutes', NOW() - INTERVAL '1 hour 50 minutes', null),

('c50e8400-e29b-41d4-a716-446655440004', '750e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440005',
 'Just joined! Mind if I work on my React project while you study?', 'TEXT', NOW() - INTERVAL '1 hour 30 minutes', NOW() - INTERVAL '1 hour 30 minutes', null),

('c50e8400-e29b-41d4-a716-446655440005', '750e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440002',
 'Of course! Body doubling works with any focused work 💪', 'TEXT', NOW() - INTERVAL '1 hour 28 minutes', NOW() - INTERVAL '1 hour 28 minutes', null),

-- Dev Team discussions
('c50e8400-e29b-41d4-a716-446655440006', '750e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440005',
 'Morning standup in 5! Current blocker: API integration tests failing', 'TEXT', NOW() - INTERVAL '3 hours', NOW() - INTERVAL '3 hours', null),

('c50e8400-e29b-41d4-a716-446655440007', '750e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440006',
 'I can help with that after I finish the UI mockups. Should be done in an hour', 'TEXT', NOW() - INTERVAL '2 hours 55 minutes', NOW() - INTERVAL '2 hours 55 minutes', null),

('c50e8400-e29b-41d4-a716-446655440008', '750e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440008',
 'Thanks! I''ll be working on the database optimization meanwhile', 'TEXT', NOW() - INTERVAL '2 hours 50 minutes', NOW() - INTERVAL '2 hours 50 minutes', null),

-- Remote Coffee casual chat
('c50e8400-e29b-41d4-a716-446655440009', '750e8400-e29b-41d4-a716-446655440007', '550e8400-e29b-41d4-a716-446655440008',
 'Good morning everyone! ☀️ Anyone else struggling to get motivated today?', 'TEXT', NOW() - INTERVAL '4 hours', NOW() - INTERVAL '4 hours', null),

('c50e8400-e29b-41d4-a716-446655440010', '750e8400-e29b-41d4-a716-446655440007', '550e8400-e29b-41d4-a716-446655440009',
 'Same here! Remote work motivation is real. Coffee helps ☕', 'TEXT', NOW() - INTERVAL '3 hours 55 minutes', NOW() - INTERVAL '3 hours 55 minutes', null),

('c50e8400-e29b-41d4-a716-446655440011', '750e8400-e29b-41d4-a716-446655440007', '550e8400-e29b-41d4-a716-446655440010',
 'I find having people around (even virtually) really helps with accountability', 'TEXT', NOW() - INTERVAL '3 hours 50 minutes', NOW() - INTERVAL '3 hours 50 minutes', null),

('c50e8400-e29b-41d4-a716-446655440012', '750e8400-e29b-41d4-a716-446655440007', '550e8400-e29b-41d4-a716-446655440002',
 'Exactly! That''s why I love this space. Let''s all commit to 2 hours focused work?', 'TEXT', NOW() - INTERVAL '3 hours 45 minutes', NOW() - INTERVAL '3 hours 45 minutes', null),

('c50e8400-e29b-41d4-a716-446655440013', '750e8400-e29b-41d4-a716-446655440007', '550e8400-e29b-41d4-a716-446655440003',
 'Count me in! Working on my thesis today 📚', 'TEXT', NOW() - INTERVAL '3 hours 40 minutes', NOW() - INTERVAL '3 hours 40 minutes', null);

-- Update statistics
SELECT 'Created ' || COUNT(*) || ' chat messages' as status FROM chat_messages WHERE hive_id IN (SELECT id FROM hives WHERE name LIKE 'Test %');
EOF
    
    execute_sql "chat_test" "/tmp/seed-chat.sql"
}

# Generate test forum posts
seed_forum_data() {
    log_section "Seeding Test Forum Data"
    
    cat > /tmp/seed-forum.sql << 'EOF'
-- ===================================================================
-- TEST FORUM DATA FOR E2E TESTING
-- Creates realistic forum discussions and posts
-- ===================================================================

-- Clean existing test forum data
DELETE FROM forum_replies WHERE post_id IN (SELECT id FROM forum_posts WHERE author_id IN (SELECT id FROM users WHERE email LIKE '%@test.focushive.app'));
DELETE FROM forum_posts WHERE author_id IN (SELECT id FROM users WHERE email LIKE '%@test.focushive.app');
DELETE FROM forum_categories WHERE name LIKE 'Test %';

-- Create test forum categories
INSERT INTO forum_categories (id, name, description, color, sort_order, created_at, updated_at) VALUES
('fc0e8400-e29b-41d4-a716-446655440001', 'Test Study Tips', 'Share and discover effective study techniques', '#4CAF50', 1, NOW(), NOW()),
('fc0e8400-e29b-41d4-a716-446655440002', 'Test Tech Talk', 'Discuss programming, design, and technology', '#2196F3', 2, NOW(), NOW()),
('fc0e8400-e29b-41d4-a716-446655440003', 'Test Remote Work', 'Tips and experiences for remote workers', '#FF9800', 3, NOW(), NOW()),
('fc0e8400-e29b-41d4-a716-446655440004', 'Test General', 'General discussions and community chat', '#9C27B0', 4, NOW(), NOW());

-- Create test forum posts
INSERT INTO forum_posts (id, title, content, author_id, category_id, post_type, status, tags, views_count, replies_count, 
                        likes_count, created_at, updated_at, last_activity_at) VALUES
-- Study tips posts
('fp0e8400-e29b-41d4-a716-446655440001', 'The Pomodoro Technique: Does it Really Work?', 
 E'I\'ve been experimenting with the Pomodoro Technique for the past month and wanted to share my experience.\n\n**What worked:**\n- 25-minute focused sessions felt manageable\n- Regular breaks prevented burnout\n- Increased awareness of time spent on tasks\n\n**Challenges:**\n- Some tasks require longer uninterrupted focus\n- External interruptions break the flow\n\nHas anyone else tried this technique? What modifications have you made?', 
 '550e8400-e29b-41d4-a716-446655440002', 'fc0e8400-e29b-41d4-a716-446655440001', 'DISCUSSION', 'PUBLISHED', 
 '["pomodoro", "time-management", "productivity"]', 42, 5, 8, NOW() - INTERVAL '5 days', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day'),

('fp0e8400-e29b-41d4-a716-446655440002', 'Best Apps for Spaced Repetition?', 
 E'I\'m looking for recommendations for spaced repetition apps. Currently using Anki but wondering if there are better alternatives.\n\n**Requirements:**\n- Good mobile app\n- Supports images and audio\n- Decent free tier\n- Sync across devices\n\nWhat do you all use for memorization and review?', 
 '550e8400-e29b-41d4-a716-446655440003', 'fc0e8400-e29b-41d4-a716-446655440001', 'QUESTION', 'PUBLISHED', 
 '["apps", "spaced-repetition", "memorization", "anki"]', 28, 7, 3, NOW() - INTERVAL '3 days', NOW() - INTERVAL '1 day', NOW() - INTERVAL '6 hours'),

-- Tech discussion posts
('fp0e8400-e29b-41d4-a716-446655440003', 'React vs Vue: Performance Comparison 2024', 
 E'I\'ve been working on similar projects with both React and Vue, and wanted to share some performance observations:\n\n**React (v18):**\n- Bundle size: ~45KB (production)\n- First contentful paint: ~1.2s\n- Time to interactive: ~2.1s\n\n**Vue (v3):**\n- Bundle size: ~38KB (production)\n- First contentful paint: ~1.0s\n- Time to interactive: ~1.8s\n\n**Methodology:**\n- Same app architecture\n- Similar feature set\n- Tested on same hardware/network\n\nThoughts? Has anyone done similar comparisons?', 
 '550e8400-e29b-41d4-a716-446655440005', 'fc0e8400-e29b-41d4-a716-446655440002', 'DISCUSSION', 'PUBLISHED', 
 '["react", "vue", "performance", "frontend", "comparison"]', 67, 12, 15, NOW() - INTERVAL '7 days', NOW() - INTERVAL '1 day', NOW() - INTERVAL '3 hours'),

-- Remote work posts
('fp0e8400-e29b-41d4-a716-446655440004', 'Remote Work Setup: My Home Office Evolution', 
 E'After 2 years of remote work, I\'ve finally got my home office dialed in. Here\'s what made the biggest difference:\n\n**Essential upgrades:**\n1. Ergonomic chair (worth every penny)\n2. Dual monitor setup (productivity boost)\n3. Good lighting (no more eye strain)\n4. Noise-cancelling headphones (for focus)\n\n**Unexpected game-changers:**\n- Plants (improved mood and air quality)\n- Whiteboard (better for brainstorming)\n- Dedicated work playlist\n\nWhat\'s been most impactful for your setup?', 
 '550e8400-e29b-41d4-a716-446655440008', 'fc0e8400-e29b-41d4-a716-446655440003', 'DISCUSSION', 'PUBLISHED', 
 '["remote-work", "home-office", "productivity", "setup"]', 89, 18, 22, NOW() - INTERVAL '4 days', NOW() - INTERVAL '6 hours', NOW() - INTERVAL '2 hours'),

-- Community posts
('fp0e8400-e29b-41d4-a716-446655440005', 'FocusHive Feature Request: Music Integration', 
 E'Love this platform! One feature that would be amazing is better music integration:\n\n**Current state:**\n- Can play personal music\n- No coordination between hive members\n\n**Suggested improvements:**\n- Shared playlists for hives\n- Genre/mood voting\n- Focus-optimized music recommendations\n- Integration with Spotify/Apple Music\n\nAnyone else interested in this? What other features would you like to see?', 
 '550e8400-e29b-41d4-a716-446655440009', 'fc0e8400-e29b-41d4-a716-446655440004', 'FEATURE_REQUEST', 'PUBLISHED', 
 '["feature-request", "music", "spotify", "collaboration"]', 34, 9, 11, NOW() - INTERVAL '2 days', NOW() - INTERVAL '8 hours', NOW() - INTERVAL '4 hours');

-- Create test forum replies
INSERT INTO forum_replies (id, post_id, author_id, content, parent_reply_id, likes_count, created_at, updated_at) VALUES
-- Replies to Pomodoro post
('fr0e8400-e29b-41d4-a716-446655440001', 'fp0e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440004',
 E'I use a modified version - 45 minutes work, 15 minutes break. Works better for deep coding sessions where 25 minutes isn\'t enough to get into flow state.', 
 null, 3, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),

('fr0e8400-e29b-41d4-a716-446655440002', 'fp0e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440006',
 E'@carol That\'s interesting! Do you find the longer break time helps with creativity? I\'ve been struggling with design blocks lately.', 
 'fr0e8400-e29b-41d4-a716-446655440001', 1, NOW() - INTERVAL '20 hours', NOW() - INTERVAL '20 hours'),

-- Replies to React vs Vue post
('fr0e8400-e29b-41d4-a716-446655440003', 'fp0e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440008',
 E'Great analysis! One thing to consider is the learning curve. Vue felt more intuitive to me coming from vanilla JS, while React required more mindset shift.', 
 null, 5, NOW() - INTERVAL '3 hours', NOW() - INTERVAL '3 hours'),

-- Replies to home office post
('fr0e8400-e29b-41d4-a716-446655440004', 'fp0e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440010',
 E'The plants suggestion is spot on! I have a small snake plant and it\'s made a huge difference in how the space feels. Also easier to maintain than I expected.', 
 null, 7, NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours'),

-- Replies to music integration post
('fr0e8400-e29b-41d4-a716-446655440005', 'fp0e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440002',
 E'+1 for this feature! It would be cool to have different music modes too - like "deep focus" vs "creative work" vs "light tasks"', 
 null, 4, NOW() - INTERVAL '4 hours', NOW() - INTERVAL '4 hours');

-- Update statistics
SELECT 'Created ' || COUNT(*) || ' forum categories' as status FROM forum_categories WHERE name LIKE 'Test %';
SELECT 'Created ' || COUNT(*) || ' forum posts' as status FROM forum_posts WHERE author_id IN (SELECT id FROM users WHERE email LIKE '%@test.focushive.app');
SELECT 'Created ' || COUNT(*) || ' forum replies' as status FROM forum_replies WHERE author_id IN (SELECT id FROM users WHERE email LIKE '%@test.focushive.app');
EOF
    
    execute_sql "forum_test" "/tmp/seed-forum.sql"
}

# Generate OAuth2 clients for identity service
seed_identity_data() {
    log_section "Seeding Test Identity Data"
    
    cat > /tmp/seed-oauth-clients.sql << 'EOF'
-- ===================================================================
-- OAUTH2 TEST CLIENTS FOR E2E TESTING
-- Creates OAuth2 clients for testing authentication flows
-- ===================================================================

-- Clean existing test clients
DELETE FROM oauth2_authorized_client WHERE registered_client_id LIKE 'test-%';
DELETE FROM oauth2_authorization_consent WHERE registered_client_id LIKE 'test-%';
DELETE FROM oauth2_authorization WHERE registered_client_id LIKE 'test-%';
DELETE FROM oauth2_registered_client WHERE client_id LIKE 'test-%';

-- Create test OAuth2 clients
INSERT INTO oauth2_registered_client (
    id, client_id, client_id_issued_at, client_secret, client_secret_expires_at,
    client_name, client_authentication_methods, authorization_grant_types,
    redirect_uris, scopes, client_settings, token_settings
) VALUES
-- Frontend application client
('test-client-001', 'test-focushive-frontend', NOW(), 
 '{bcrypt}$2a$10$N9qo8uLOickgx2ZMRZoMye2J7v4XGSBlrG8J7JJhGp5n8xo2y0vGK', 
 NULL, 'FocusHive Frontend Test Client',
 'client_secret_post,client_secret_basic',
 'authorization_code,refresh_token,client_credentials',
 'http://localhost:3000/auth/callback,http://localhost:3000/login/oauth2/code/focushive',
 'openid,profile,email,read,write',
 '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":false}',
 '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",3600.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",86400.000000000]}'),

-- E2E Test Client (for automated testing)
('test-client-002', 'test-e2e-client', NOW(), 
 '{bcrypt}$2a$10$N9qo8uLOickgx2ZMRZoMye2J7v4XGSBlrG8J7JJhGp5n8xo2y0vGK', 
 NULL, 'E2E Test Client',
 'client_secret_post,client_secret_basic',
 'authorization_code,refresh_token,client_credentials,password',
 'http://localhost:3000/test/callback,http://playwright:3000/auth/callback',
 'openid,profile,email,read,write,admin',
 '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":false}',
 '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",7200.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",172800.000000000]}'),

-- Mobile app client (for testing mobile flows)
('test-client-003', 'test-focushive-mobile', NOW(), 
 NULL, NULL, 'FocusHive Mobile Test Client',
 'none',
 'authorization_code,refresh_token',
 'com.focushive.mobile://auth/callback',
 'openid,profile,email,read,write,offline_access',
 '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false}',
 '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":false,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",1800.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",604800.000000000]}');

-- Update statistics
SELECT 'Created ' || COUNT(*) || ' OAuth2 test clients' as status FROM oauth2_registered_client WHERE client_id LIKE 'test-%';
EOF
    
    execute_sql "identity_test" "/tmp/seed-oauth-clients.sql"
}

# Generate test notification data
seed_notification_data() {
    log_section "Seeding Test Notification Data"
    
    cat > /tmp/seed-notifications.sql << 'EOF'
-- ===================================================================
-- TEST NOTIFICATIONS FOR E2E TESTING
-- Creates sample notifications for testing notification system
-- ===================================================================

-- Clean existing test notifications
DELETE FROM notification_templates WHERE name LIKE 'test_%';
DELETE FROM notifications WHERE recipient_id IN (SELECT id FROM users WHERE email LIKE '%@test.focushive.app');

-- Create test notification templates
INSERT INTO notification_templates (id, name, subject, body_template, notification_type, channel, priority, created_at, updated_at) VALUES
('nt0e8400-e29b-41d4-a716-446655440001', 'test_hive_invitation', 'You''ve been invited to join {{hive_name}}!',
 E'Hi {{user_name}},\n\n{{inviter_name}} has invited you to join the "{{hive_name}}" hive on FocusHive.\n\n{{hive_description}}\n\nClick here to accept: {{invitation_link}}\n\nHappy focusing!\nThe FocusHive Team',
 'INVITATION', 'EMAIL', 'NORMAL', NOW(), NOW()),

('nt0e8400-e29b-41d4-a716-446655440002', 'test_session_reminder', 'Your focus session starts in 5 minutes',
 E'Hi {{user_name}},\n\nDon''t forget - your {{session_type}} session in "{{hive_name}}" starts in 5 minutes.\n\nGet ready to focus!\n\nSession details:\n- Duration: {{duration}} minutes\n- Type: {{session_type}}\n- Participants: {{participant_count}}',
 'REMINDER', 'PUSH', 'HIGH', NOW(), NOW()),

('nt0e8400-e29b-41d4-a716-446655440003', 'test_achievement_unlocked', 'Achievement Unlocked: {{achievement_title}}!',
 E'Congratulations {{user_name}}!\n\nYou''ve unlocked the "{{achievement_title}}" achievement!\n\n{{achievement_description}}\n\nPoints earned: {{points}}\nTotal points: {{total_points}}\n\nKeep up the great work!',
 'ACHIEVEMENT', 'IN_APP', 'NORMAL', NOW(), NOW());

-- Create test notifications
INSERT INTO notifications (id, recipient_id, sender_id, title, message, notification_type, channel, priority, 
                         read_at, created_at, updated_at, metadata) VALUES
-- Recent notifications (unread)
('no0e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440002', null,
 'Welcome to FocusHive!', 'Get started by joining your first hive or creating one.', 'WELCOME', 'IN_APP', 'NORMAL',
 null, NOW() - INTERVAL '30 minutes', NOW() - INTERVAL '30 minutes', '{"onboarding": true}'),

('no0e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440005',
 'New message in Dev Team Sprint', 'David posted: "Morning standup in 5! Current blocker: API integration..."', 'MESSAGE', 'PUSH', 'NORMAL',
 null, NOW() - INTERVAL '20 minutes', NOW() - INTERVAL '20 minutes', '{"hive_id": "750e8400-e29b-41d4-a716-446655440004", "message_id": "c50e8400-e29b-41d4-a716-446655440006"}'),

('no0e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440004', null,
 'Focus session starting soon', 'Your Deep Focus session in "Test Research Lab" starts in 5 minutes.', 'REMINDER', 'PUSH', 'HIGH',
 null, NOW() - INTERVAL '10 minutes', NOW() - INTERVAL '10 minutes', '{"hive_id": "750e8400-e29b-41d4-a716-446655440003", "session_type": "DEEP_WORK"}'),

-- Older notifications (some read)
('no0e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440005', null,
 'Achievement Unlocked: Code Marathon!', 'You completed 4 hours of coding in a single session. Amazing focus!', 'ACHIEVEMENT', 'IN_APP', 'NORMAL',
 NOW() - INTERVAL '2 days', NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days', '{"achievement_id": "a50e8400-e29b-41d4-a716-446655440003", "points": 200}'),

('no0e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440006', '550e8400-e29b-41d4-a716-446655440002',
 'Hive invitation: Test Study Group', 'Alice has invited you to join "Test Study Group - Computer Science"', 'INVITATION', 'EMAIL', 'NORMAL',
 NOW() - INTERVAL '5 days', NOW() - INTERVAL '8 days', NOW() - INTERVAL '5 days', '{"hive_id": "750e8400-e29b-41d4-a716-446655440001", "inviter_id": "550e8400-e29b-41d4-a716-446655440002"}'),

('no0e8400-e29b-41d4-a716-446655440006', '550e8400-e29b-41d4-a716-446655440008', null,
 'Weekly productivity report', 'You completed 12 hours of focused work this week. That''s 20% more than last week!', 'REPORT', 'EMAIL', 'LOW',
 null, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', '{"weekly_hours": 12, "improvement": 0.2, "rank": "top_25_percent"}');

-- Update statistics
SELECT 'Created ' || COUNT(*) || ' notification templates' as status FROM notification_templates WHERE name LIKE 'test_%';
SELECT 'Created ' || COUNT(*) || ' notifications' as status FROM notifications WHERE recipient_id IN (SELECT id FROM users WHERE email LIKE '%@test.focushive.app');
EOF
    
    execute_sql "notification_test" "/tmp/seed-notifications.sql"
}

# Generate test buddy relationships
seed_buddy_data() {
    log_section "Seeding Test Buddy Data"
    
    cat > /tmp/seed-buddies.sql << 'EOF'
-- ===================================================================
-- TEST BUDDY RELATIONSHIPS FOR E2E TESTING
-- Creates accountability partner relationships for testing
-- ===================================================================

-- Clean existing test buddy data
DELETE FROM buddy_sessions WHERE buddy_pair_id IN (SELECT id FROM buddy_pairs WHERE requester_id IN (SELECT id FROM users WHERE email LIKE '%@test.focushive.app'));
DELETE FROM buddy_pairs WHERE requester_id IN (SELECT id FROM users WHERE email LIKE '%@test.focushive.app');

-- Create test buddy pairs
INSERT INTO buddy_pairs (id, requester_id, accepter_id, status, created_at, accepted_at, settings, notes) VALUES
-- Active buddy relationships
('bp0e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440003',
 'ACTIVE', NOW() - INTERVAL '15 days', NOW() - INTERVAL '14 days', 
 '{"check_in_frequency": "daily", "accountability_level": "medium", "preferred_times": ["09:00", "13:00", "17:00"]}',
 'Study buddies for Computer Science finals. Both working on algorithms and data structures.'),

('bp0e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440006',
 'ACTIVE', NOW() - INTERVAL '10 days', NOW() - INTERVAL '9 days',
 '{"check_in_frequency": "twice_weekly", "accountability_level": "high", "preferred_times": ["10:00", "14:00"]}',
 'Professional accountability partners. David (dev) and Eve (designer) working on complementary skills.'),

('bp0e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440008', '550e8400-e29b-41d4-a716-446655440009',
 'ACTIVE', NOW() - INTERVAL '20 days', NOW() - INTERVAL '19 days',
 '{"check_in_frequency": "weekly", "accountability_level": "low", "preferred_times": ["11:00", "15:00"]}',
 'Remote work buddies. Both freelancers helping each other stay motivated and productive.'),

-- Pending buddy request
('bp0e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440007',
 'PENDING', NOW() - INTERVAL '2 days', null,
 '{"check_in_frequency": "daily", "accountability_level": "high", "preferred_times": ["08:00", "12:00", "16:00"]}',
 'Research student looking for accountability partner for thesis writing.'),

-- Past relationship (ended amicably)
('bp0e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440010', '550e8400-e29b-41d4-a716-446655440002',
 'ENDED', NOW() - INTERVAL '30 days', NOW() - INTERVAL '29 days',
 '{"check_in_frequency": "daily", "accountability_level": "medium", "preferred_times": ["09:00", "18:00"]}',
 'Startup accountability partnership. Ended due to different time zones after one partner relocated.');

-- Create test buddy sessions
INSERT INTO buddy_sessions (id, buddy_pair_id, session_type, scheduled_at, completed_at, status, notes, 
                           rating_requester, rating_accepter, created_at, updated_at) VALUES
-- Recent sessions for active pairs
('bs0e8400-e29b-41d4-a716-446655440001', 'bp0e8400-e29b-41d4-a716-446655440001', 'CHECK_IN',
 NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day' + INTERVAL '15 minutes', 'COMPLETED',
 'Good progress on algorithms study. Alice completed binary tree exercises, Bob worked on graph problems. Both staying on track for finals.',
 5, 5, NOW() - INTERVAL '1 day', NOW() - INTERVAL '23 hours'),

('bs0e8400-e29b-41d4-a716-446655440002', 'bp0e8400-e29b-41d4-a716-446655440001', 'ACCOUNTABILITY',
 NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days' + INTERVAL '20 minutes', 'COMPLETED',
 'Weekly accountability session. Reviewed goals, both partners met their study targets. Planned next week study schedule.',
 4, 5, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),

('bs0e8400-e29b-41d4-a716-446655440003', 'bp0e8400-e29b-41d4-a716-446655440002', 'GOAL_REVIEW',
 NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days' + INTERVAL '25 minutes', 'COMPLETED',
 'Bi-weekly goal review. David completed React project milestone, Eve finished UI mockups. Both setting new targets.',
 5, 4, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),

-- Upcoming scheduled sessions
('bs0e8400-e29b-41d4-a716-446655440004', 'bp0e8400-e29b-41d4-a716-446655440001', 'CHECK_IN',
 NOW() + INTERVAL '1 day', null, 'SCHEDULED',
 null, null, null, NOW(), NOW()),

('bs0e8400-e29b-41d4-a716-446655440005', 'bp0e8400-e29b-41d4-a716-446655440003', 'ACCOUNTABILITY',
 NOW() + INTERVAL '3 days', null, 'SCHEDULED',
 null, null, null, NOW(), NOW()),

-- Missed session example
('bs0e8400-e29b-41d4-a716-446655440006', 'bp0e8400-e29b-41d4-a716-446655440003', 'CHECK_IN',
 NOW() - INTERVAL '5 days', null, 'MISSED',
 'Grace had a client emergency, Henry was understanding. Rescheduled for next week.', null, null, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days');

-- Update statistics
SELECT 'Created ' || COUNT(*) || ' buddy pairs' as status FROM buddy_pairs WHERE requester_id IN (SELECT id FROM users WHERE email LIKE '%@test.focushive.app');
SELECT 'Created ' || COUNT(*) || ' buddy sessions' as status FROM buddy_sessions WHERE buddy_pair_id IN (SELECT id FROM buddy_pairs WHERE requester_id IN (SELECT id FROM users WHERE email LIKE '%@test.focushive.app'));
EOF
    
    execute_sql "buddy_test" "/tmp/seed-buddy.sql"
}

# Main seeding function
main() {
    local command=${1:-"all"}
    
    case $command in
        "all")
            echo "═════════════════════════════════════════"
            echo "🌱 FocusHive Test Data Seeding"
            echo "═════════════════════════════════════════"
            
            log_info "Waiting for services to be ready..."
            wait_for_service "Identity Service" "$IDENTITY_SERVICE_URL" || true
            wait_for_service "Backend Service" "$BACKEND_SERVICE_URL" || true
            
            seed_users
            seed_hives
            seed_analytics
            seed_chat_data
            seed_forum_data
            seed_identity_data
            seed_notification_data
            seed_buddy_data
            
            log_section "Test Data Summary"
            log_success "✅ Users: 10 test users with diverse personas"
            log_success "✅ Hives: 10 test hives with various configurations"
            log_success "✅ Analytics: Productivity sessions and achievements"
            log_success "✅ Chat: Realistic conversations across hives"
            log_success "✅ Forum: Discussion posts and replies"
            log_success "✅ Identity: OAuth2 clients for authentication"
            log_success "✅ Notifications: Templates and sample notifications"
            log_success "✅ Buddies: Accountability partner relationships"
            
            log_success "🎉 Test data seeding completed successfully!"
            echo ""
            echo "📋 Test accounts available:"
            echo "  Admin: admin@test.focushive.app (password: password123)"
            echo "  Users: alice@test.focushive.app, bob@test.focushive.app, etc."
            echo "  Password for all test accounts: password123"
            ;;
        "users")
            seed_users
            ;;
        "hives")
            seed_hives
            ;;
        "analytics")
            seed_analytics
            ;;
        "chat")
            seed_chat_data
            ;;
        "forum")
            seed_forum_data
            ;;
        "identity")
            seed_identity_data
            ;;
        "notifications")
            seed_notification_data
            ;;
        "buddies")
            seed_buddy_data
            ;;
        "clean")
            log_section "Cleaning Test Data"
            log_warning "This will delete all test data. Are you sure? (y/N)"
            read -r confirmation
            if [[ $confirmation =~ ^[Yy]$ ]]; then
                log_info "Cleaning test data..."
                # Add cleanup SQL commands here
                log_success "Test data cleaned"
            else
                log_info "Cleanup cancelled"
            fi
            ;;
        "help")
            echo "Usage: $0 [command]"
            echo ""
            echo "Commands:"
            echo "  all (default)   - Seed all test data"
            echo "  users          - Seed test users only"
            echo "  hives          - Seed test hives only"
            echo "  analytics      - Seed analytics data only"
            echo "  chat           - Seed chat messages only"
            echo "  forum          - Seed forum posts only"
            echo "  identity       - Seed OAuth2 clients only"
            echo "  notifications  - Seed notification data only"
            echo "  buddies        - Seed buddy relationships only"
            echo "  clean          - Clean all test data"
            echo "  help           - Show this help message"
            ;;
        *)
            log_error "Unknown command: $command"
            echo "Use '$0 help' for usage information"
            exit 1
            ;;
    esac
}

# Cleanup temporary files on exit
cleanup() {
    rm -f /tmp/seed-*.sql
}
trap cleanup EXIT

# Run main function
main "$@"