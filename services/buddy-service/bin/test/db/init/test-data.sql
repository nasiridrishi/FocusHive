-- Test Data Setup for Buddy Service E2E Tests
-- This script initializes the database with test data for comprehensive E2E testing

-- ============================================================================
-- CREATE TEST TABLES (if not exists from Flyway migrations)
-- ============================================================================

-- User preferences table for matching
CREATE TABLE IF NOT EXISTS buddy_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL UNIQUE,
    interests TEXT,
    goals TEXT,
    timezone VARCHAR(100) DEFAULT 'UTC',
    communication_style VARCHAR(50) DEFAULT 'balanced',
    activity_level VARCHAR(50) DEFAULT 'medium',
    max_partners INTEGER DEFAULT 3,
    min_compatibility_score DECIMAL(3,2) DEFAULT 0.60,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Partnerships table
CREATE TABLE IF NOT EXISTS buddy_partnerships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_id VARCHAR(255) NOT NULL,
    recipient_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    message TEXT,
    duration_days INTEGER DEFAULT 30,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_partnership UNIQUE(requester_id, recipient_id)
);

-- Goals table
CREATE TABLE IF NOT EXISTS buddy_goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    partnership_id UUID REFERENCES buddy_partnerships(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50) DEFAULT 'PERSONAL',
    category VARCHAR(50) DEFAULT 'OTHER',
    status VARCHAR(50) DEFAULT 'ACTIVE',
    target_date TIMESTAMP,
    completion_date TIMESTAMP,
    is_public BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Milestones table
CREATE TABLE IF NOT EXISTS buddy_milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id UUID NOT NULL REFERENCES buddy_goals(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    target_date TIMESTAMP,
    completion_date TIMESTAMP,
    status VARCHAR(50) DEFAULT 'PENDING',
    order_index INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Check-ins table
CREATE TABLE IF NOT EXISTS buddy_checkins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    partnership_id UUID REFERENCES buddy_partnerships(id),
    mood INTEGER CHECK (mood >= 1 AND mood <= 10),
    energy_level INTEGER CHECK (energy_level >= 1 AND energy_level <= 10),
    productivity INTEGER CHECK (productivity >= 1 AND productivity <= 10),
    stress_level INTEGER CHECK (stress_level >= 1 AND stress_level <= 10),
    notes TEXT,
    frequency VARCHAR(50) DEFAULT 'DAILY',
    goals_progress JSONB,
    challenges TEXT[],
    wins TEXT[],
    weekly_reflection TEXT,
    checkin_date DATE DEFAULT CURRENT_DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Goal templates table
CREATE TABLE IF NOT EXISTS buddy_goal_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    type VARCHAR(50) DEFAULT 'PERSONAL',
    default_duration_days INTEGER DEFAULT 30,
    milestones JSONB,
    tags TEXT[],
    difficulty_level VARCHAR(50) DEFAULT 'MEDIUM',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Accountability scores table
CREATE TABLE IF NOT EXISTS buddy_accountability_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    partnership_id UUID REFERENCES buddy_partnerships(id),
    overall_score DECIMAL(5,2) DEFAULT 0.0,
    checkin_consistency DECIMAL(5,2) DEFAULT 0.0,
    goal_completion DECIMAL(5,2) DEFAULT 0.0,
    engagement_quality DECIMAL(5,2) DEFAULT 0.0,
    streak_bonus DECIMAL(5,2) DEFAULT 0.0,
    calculation_date DATE DEFAULT CURRENT_DATE,
    metrics JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Matching queue table (Redis backup)
CREATE TABLE IF NOT EXISTS buddy_matching_queue (
    user_id VARCHAR(255) PRIMARY KEY,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    preferences_hash VARCHAR(255),
    priority_score DECIMAL(5,2) DEFAULT 0.0
);

-- ============================================================================
-- INSERT TEST DATA
-- ============================================================================

-- Clear existing test data
DELETE FROM buddy_accountability_scores WHERE user_id LIKE 'e2e-%' OR user_id LIKE 'test-%';
DELETE FROM buddy_checkins WHERE user_id LIKE 'e2e-%' OR user_id LIKE 'test-%';
DELETE FROM buddy_milestones WHERE goal_id IN (SELECT id FROM buddy_goals WHERE user_id LIKE 'e2e-%' OR user_id LIKE 'test-%');
DELETE FROM buddy_goals WHERE user_id LIKE 'e2e-%' OR user_id LIKE 'test-%';
DELETE FROM buddy_partnerships WHERE requester_id LIKE 'e2e-%' OR recipient_id LIKE 'e2e-%' OR requester_id LIKE 'test-%' OR recipient_id LIKE 'test-%';
DELETE FROM buddy_preferences WHERE user_id LIKE 'e2e-%' OR user_id LIKE 'test-%';
DELETE FROM buddy_matching_queue WHERE user_id LIKE 'e2e-%' OR user_id LIKE 'test-%';

-- Insert goal templates for testing
INSERT INTO buddy_goal_templates (title, description, category, type, default_duration_days, milestones, tags, difficulty_level) VALUES
(
    'Fitness Transformation Challenge',
    'Complete a 30-day fitness transformation with daily workouts and nutrition tracking',
    'FITNESS',
    'PERSONAL',
    30,
    '[
        {"title": "Week 1: Establish Routine", "description": "Complete 5 workouts", "orderIndex": 1},
        {"title": "Week 2: Increase Intensity", "description": "Add cardio sessions", "orderIndex": 2},
        {"title": "Week 3: Strength Focus", "description": "Focus on strength training", "orderIndex": 3},
        {"title": "Week 4: Final Push", "description": "Complete transformation", "orderIndex": 4}
    ]'::jsonb,
    ARRAY['fitness', 'health', 'transformation', '30-day'],
    'MEDIUM'
),
(
    'Learn Programming Basics',
    'Master fundamental programming concepts and build your first project',
    'LEARNING',
    'PERSONAL',
    60,
    '[
        {"title": "Complete Python Basics", "description": "Finish online course", "orderIndex": 1},
        {"title": "Build Calculator App", "description": "Create first project", "orderIndex": 2},
        {"title": "Learn Git & GitHub", "description": "Version control basics", "orderIndex": 3},
        {"title": "Deploy Project", "description": "Put project online", "orderIndex": 4}
    ]'::jsonb,
    ARRAY['programming', 'coding', 'learning', 'python'],
    'BEGINNER'
),
(
    'Mindfulness & Meditation Practice',
    'Develop a consistent mindfulness practice for mental well-being',
    'WELLNESS',
    'PERSONAL',
    21,
    '[
        {"title": "Week 1: Daily 5-min Sessions", "description": "Establish habit", "orderIndex": 1},
        {"title": "Week 2: Increase to 10 minutes", "description": "Build consistency", "orderIndex": 2},
        {"title": "Week 3: Add Gratitude Practice", "description": "Enhance practice", "orderIndex": 3}
    ]'::jsonb,
    ARRAY['mindfulness', 'meditation', 'wellness', 'mental-health'],
    'EASY'
),
(
    'Career Advancement Plan',
    'Strategic plan for professional growth and skill development',
    'CAREER',
    'PERSONAL',
    90,
    '[
        {"title": "Skills Assessment", "description": "Identify skill gaps", "orderIndex": 1},
        {"title": "Create Learning Plan", "description": "Plan skill development", "orderIndex": 2},
        {"title": "Network Building", "description": "Connect with professionals", "orderIndex": 3},
        {"title": "Update Portfolio", "description": "Showcase new skills", "orderIndex": 4}
    ]'::jsonb,
    ARRAY['career', 'professional-development', 'skills', 'networking'],
    'HARD'
),
(
    'Financial Wellness Challenge',
    'Improve financial literacy and establish healthy money habits',
    'FINANCE',
    'PERSONAL',
    45,
    '[
        {"title": "Budget Creation", "description": "Create monthly budget", "orderIndex": 1},
        {"title": "Emergency Fund Start", "description": "Save first $500", "orderIndex": 2},
        {"title": "Debt Assessment", "description": "List and prioritize debts", "orderIndex": 3},
        {"title": "Investment Learning", "description": "Learn investment basics", "orderIndex": 4}
    ]'::jsonb,
    ARRAY['finance', 'budgeting', 'savings', 'financial-literacy'],
    'MEDIUM'
);

-- Insert sample preferences for testing compatibility
INSERT INTO buddy_preferences (user_id, interests, goals, timezone, communication_style, activity_level, max_partners, min_compatibility_score) VALUES
(
    'test-sample-user-1',
    ARRAY['fitness', 'coding', 'reading', 'travel'],
    ARRAY['get fit', 'learn new skills', 'improve productivity'],
    'America/New_York',
    'direct',
    'high',
    3,
    0.65
),
(
    'test-sample-user-2',
    ARRAY['fitness', 'cooking', 'music', 'photography'],
    ARRAY['get fit', 'eat healthy', 'creative expression'],
    'America/New_York',
    'supportive',
    'medium',
    2,
    0.70
),
(
    'test-sample-user-3',
    ARRAY['coding', 'gaming', 'technology', 'science'],
    ARRAY['learn programming', 'build projects', 'career growth'],
    'America/Los_Angeles',
    'analytical',
    'medium',
    4,
    0.60
),
(
    'test-sample-user-4',
    ARRAY['reading', 'writing', 'meditation', 'nature'],
    ARRAY['read more books', 'mindfulness practice', 'work-life balance'],
    'Europe/London',
    'gentle',
    'low',
    2,
    0.75
);

-- Insert sample partnerships for testing
INSERT INTO buddy_partnerships (id, requester_id, recipient_id, status, message, duration_days, start_date, end_date) VALUES
(
    '11111111-1111-1111-1111-111111111111',
    'test-sample-user-1',
    'test-sample-user-2',
    'ACTIVE',
    'Let''s achieve our fitness goals together!',
    30,
    CURRENT_TIMESTAMP - INTERVAL '5 days',
    CURRENT_TIMESTAMP + INTERVAL '25 days'
),
(
    '22222222-2222-2222-2222-222222222222',
    'test-sample-user-3',
    'test-sample-user-1',
    'PENDING',
    'Want to be coding buddies?',
    60,
    NULL,
    NULL
),
(
    '33333333-3333-3333-3333-333333333333',
    'test-sample-user-4',
    'test-sample-user-1',
    'ENDED',
    'Reading accountability partnership',
    30,
    CURRENT_TIMESTAMP - INTERVAL '35 days',
    CURRENT_TIMESTAMP - INTERVAL '5 days'
);

-- Insert sample goals for testing
INSERT INTO buddy_goals (id, user_id, partnership_id, title, description, type, category, status, target_date, is_public) VALUES
(
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'test-sample-user-1',
    '11111111-1111-1111-1111-111111111111',
    'Complete 30-Day Fitness Challenge',
    'Get back in shape with daily workouts and healthy eating',
    'SHARED',
    'FITNESS',
    'ACTIVE',
    CURRENT_TIMESTAMP + INTERVAL '25 days',
    true
),
(
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'test-sample-user-1',
    NULL,
    'Learn React.js',
    'Master React.js for front-end development',
    'PERSONAL',
    'LEARNING',
    'ACTIVE',
    CURRENT_TIMESTAMP + INTERVAL '60 days',
    false
),
(
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    'test-sample-user-2',
    '11111111-1111-1111-1111-111111111111',
    'Lose 10 Pounds',
    'Achieve target weight through exercise and diet',
    'SHARED',
    'FITNESS',
    'ACTIVE',
    CURRENT_TIMESTAMP + INTERVAL '25 days',
    true
),
(
    'dddddddd-dddd-dddd-dddd-dddddddddddd',
    'test-sample-user-4',
    NULL,
    'Read 12 Books This Year',
    'Develop a consistent reading habit',
    'PERSONAL',
    'LEARNING',
    'COMPLETED',
    CURRENT_TIMESTAMP - INTERVAL '30 days',
    true
);

-- Insert sample milestones
INSERT INTO buddy_milestones (id, goal_id, title, description, target_date, status, order_index) VALUES
(
    '11111111-1111-1111-1111-111111111111',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'Week 1: Establish Routine',
    'Complete 5 workouts in the first week',
    CURRENT_TIMESTAMP - INTERVAL '20 days',
    'COMPLETED',
    1
),
(
    '22222222-2222-2222-2222-222222222222',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'Week 2: Increase Intensity',
    'Add cardio sessions to workouts',
    CURRENT_TIMESTAMP - INTERVAL '13 days',
    'COMPLETED',
    2
),
(
    '33333333-3333-3333-3333-333333333333',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'Week 3: Strength Focus',
    'Focus on strength training exercises',
    CURRENT_TIMESTAMP - INTERVAL '6 days',
    'IN_PROGRESS',
    3
),
(
    '44444444-4444-4444-4444-444444444444',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'Week 4: Final Push',
    'Complete the fitness transformation',
    CURRENT_TIMESTAMP + INTERVAL '1 day',
    'PENDING',
    4
);

-- Insert sample check-ins
INSERT INTO buddy_checkins (id, user_id, partnership_id, mood, energy_level, productivity, stress_level, notes, frequency, goals_progress, challenges, wins, checkin_date) VALUES
(
    '55555555-5555-5555-5555-555555555555',
    'test-sample-user-1',
    '11111111-1111-1111-1111-111111111111',
    8,
    7,
    8,
    3,
    'Great workout today! Feeling motivated.',
    'DAILY',
    '{"fitness": 80, "learning": 60}'::jsonb,
    ARRAY['time management', 'motivation in evening'],
    ARRAY['completed morning workout', 'learned new React concept'],
    CURRENT_DATE
),
(
    '66666666-6666-6666-6666-666666666666',
    'test-sample-user-1',
    '11111111-1111-1111-1111-111111111111',
    7,
    6,
    7,
    4,
    'Slightly tired but stayed consistent with goals.',
    'DAILY',
    '{"fitness": 75, "learning": 65}'::jsonb,
    ARRAY['fatigue', 'work stress'],
    ARRAY['maintained workout streak', 'finished React tutorial'],
    CURRENT_DATE - INTERVAL '1 day'
),
(
    '77777777-7777-7777-7777-777777777777',
    'test-sample-user-2',
    '11111111-1111-1111-1111-111111111111',
    9,
    8,
    9,
    2,
    'Amazing day! Hit all my targets.',
    'DAILY',
    '{"fitness": 90, "nutrition": 85}'::jsonb,
    ARRAY['meal prep time'],
    ARRAY['perfect workout', 'ate healthy all day', 'encouraged my buddy'],
    CURRENT_DATE
),
(
    '88888888-8888-8888-8888-888888888888',
    'test-sample-user-1',
    '11111111-1111-1111-1111-111111111111',
    8,
    7,
    8,
    3,
    'Overall great week with consistent progress.',
    'WEEKLY',
    '{"fitness": 78, "learning": 62}'::jsonb,
    ARRAY['balancing work and fitness', 'weekend motivation'],
    ARRAY['workout streak maintained', 'React project started', 'great buddy support'],
    CURRENT_DATE - INTERVAL '2 days'
);

-- Insert sample accountability scores
INSERT INTO buddy_accountability_scores (user_id, partnership_id, overall_score, checkin_consistency, goal_completion, engagement_quality, streak_bonus, metrics) VALUES
(
    'test-sample-user-1',
    '11111111-1111-1111-1111-111111111111',
    85.5,
    90.0,
    80.0,
    88.0,
    15.0,
    '{
        "daily_checkin_rate": 0.95,
        "weekly_checkin_rate": 1.0,
        "goal_completion_rate": 0.80,
        "milestone_completion_rate": 0.67,
        "buddy_interaction_score": 88.0,
        "consistency_streak": 12,
        "longest_streak": 18,
        "total_checkins": 25,
        "missed_checkins": 1
    }'::jsonb
),
(
    'test-sample-user-2',
    '11111111-1111-1111-1111-111111111111',
    92.3,
    95.0,
    85.0,
    95.0,
    20.0,
    '{
        "daily_checkin_rate": 0.98,
        "weekly_checkin_rate": 1.0,
        "goal_completion_rate": 0.85,
        "milestone_completion_rate": 0.75,
        "buddy_interaction_score": 95.0,
        "consistency_streak": 15,
        "longest_streak": 20,
        "total_checkins": 28,
        "missed_checkins": 0
    }'::jsonb
);

-- ============================================================================
-- CREATE INDEXES FOR PERFORMANCE
-- ============================================================================

-- Indexes for better query performance during testing
CREATE INDEX IF NOT EXISTS idx_buddy_preferences_user_id ON buddy_preferences(user_id);
CREATE INDEX IF NOT EXISTS idx_buddy_partnerships_users ON buddy_partnerships(requester_id, recipient_id);
CREATE INDEX IF NOT EXISTS idx_buddy_partnerships_status ON buddy_partnerships(status);
CREATE INDEX IF NOT EXISTS idx_buddy_goals_user_id ON buddy_goals(user_id);
CREATE INDEX IF NOT EXISTS idx_buddy_goals_status ON buddy_goals(status);
CREATE INDEX IF NOT EXISTS idx_buddy_checkins_user_id ON buddy_checkins(user_id);
CREATE INDEX IF NOT EXISTS idx_buddy_checkins_date ON buddy_checkins(checkin_date);
CREATE INDEX IF NOT EXISTS idx_buddy_checkins_frequency ON buddy_checkins(frequency);
CREATE INDEX IF NOT EXISTS idx_buddy_milestones_goal_id ON buddy_milestones(goal_id);
CREATE INDEX IF NOT EXISTS idx_buddy_accountability_user_id ON buddy_accountability_scores(user_id);

-- ============================================================================
-- DATA VALIDATION
-- ============================================================================

-- Verify test data was inserted correctly
DO $$
BEGIN
    RAISE NOTICE 'Test data summary:';
    RAISE NOTICE 'Goal templates: %', (SELECT COUNT(*) FROM buddy_goal_templates);
    RAISE NOTICE 'User preferences: %', (SELECT COUNT(*) FROM buddy_preferences);
    RAISE NOTICE 'Partnerships: %', (SELECT COUNT(*) FROM buddy_partnerships);
    RAISE NOTICE 'Goals: %', (SELECT COUNT(*) FROM buddy_goals);
    RAISE NOTICE 'Milestones: %', (SELECT COUNT(*) FROM buddy_milestones);
    RAISE NOTICE 'Check-ins: %', (SELECT COUNT(*) FROM buddy_checkins);
    RAISE NOTICE 'Accountability scores: %', (SELECT COUNT(*) FROM buddy_accountability_scores);
    RAISE NOTICE 'Test data initialization completed successfully!';
END $$;