-- ===================================================================
-- USER AND HIVE TEST DATA FOR FOCUSHIVE BACKEND
-- ===================================================================

\c focushive_test;

-- Create test hives for E2E testing
CREATE TABLE IF NOT EXISTS hives (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_by UUID NOT NULL,
    max_participants INTEGER DEFAULT 10,
    is_public BOOLEAN DEFAULT true,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create test hive data
INSERT INTO hives (id, name, description, created_by, max_participants, is_public) VALUES
('h1111111-1111-1111-1111-111111111111', 'Study Group Alpha', 'Computer Science study group', '11111111-1111-1111-1111-111111111111', 8, true),
('h2222222-2222-2222-2222-222222222222', 'Work Focus Hive', 'Professional work environment', '22222222-2222-2222-2222-222222222222', 12, true),
('h3333333-3333-3333-3333-333333333333', 'Creative Writing', 'Writers and creative minds', '33333333-3333-3333-3333-333333333333', 6, true),
('h4444444-4444-4444-4444-444444444444', 'Private Team Hive', 'Closed team workspace', '44444444-4444-4444-4444-444444444444', 5, false),
('h5555555-5555-5555-5555-555555555555', 'Large Community', 'Big public hive for testing', '55555555-5555-5555-5555-555555555555', 50, true)
ON CONFLICT (id) DO NOTHING;

-- Create hive memberships
CREATE TABLE IF NOT EXISTS hive_members (
    hive_id UUID REFERENCES hives(id),
    user_id UUID NOT NULL,
    role VARCHAR(50) DEFAULT 'member',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    PRIMARY KEY (hive_id, user_id)
);

-- Seed hive memberships
INSERT INTO hive_members (hive_id, user_id, role) VALUES
-- Study Group Alpha members
('h1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'owner'),
('h1111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'member'),
('h1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 'member'),
('h1111111-1111-1111-1111-111111111111', '66666666-6666-6666-6666-666666666666', 'member'),

-- Work Focus Hive members  
('h2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'owner'),
('h2222222-2222-2222-2222-222222222222', '44444444-4444-4444-4444-444444444444', 'moderator'),
('h2222222-2222-2222-2222-222222222222', '55555555-5555-5555-5555-555555555555', 'member'),
('h2222222-2222-2222-2222-222222222222', '77777777-7777-7777-7777-777777777777', 'member'),

-- Creative Writing members
('h3333333-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', 'owner'),
('h3333333-3333-3333-3333-333333333333', '88888888-8888-8888-8888-888888888888', 'member'),
('h3333333-3333-3333-3333-333333333333', '99999999-9999-9999-9999-999999999999', 'member'),

-- Private Team Hive
('h4444444-4444-4444-4444-444444444444', '44444444-4444-4444-4444-444444444444', 'owner'),
('h4444444-4444-4444-4444-444444444444', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'member'),

-- Large Community (many members for scale testing)
('h5555555-5555-5555-5555-555555555555', '55555555-5555-5555-5555-555555555555', 'owner'),
('h5555555-5555-5555-5555-555555555555', '11111111-1111-1111-1111-111111111111', 'moderator'),
('h5555555-5555-5555-5555-555555555555', '22222222-2222-2222-2222-222222222222', 'member'),
('h5555555-5555-5555-5555-555555555555', '33333333-3333-3333-3333-333333333333', 'member'),
('h5555555-5555-5555-5555-555555555555', '66666666-6666-6666-6666-666666666666', 'member'),
('h5555555-5555-5555-5555-555555555555', '77777777-7777-7777-7777-777777777777', 'member'),
('h5555555-5555-5555-5555-555555555555', '88888888-8888-8888-8888-888888888888', 'member'),
('h5555555-5555-5555-5555-555555555555', '99999999-9999-9999-9999-999999999999', 'member')
ON CONFLICT (hive_id, user_id) DO NOTHING;

-- Create presence data for real-time testing
CREATE TABLE IF NOT EXISTS user_presence (
    user_id UUID PRIMARY KEY,
    hive_id UUID REFERENCES hives(id),
    status VARCHAR(50) DEFAULT 'offline',
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    activity_type VARCHAR(100),
    focus_score INTEGER DEFAULT 0
);

-- Seed some presence data
INSERT INTO user_presence (user_id, hive_id, status, activity_type, focus_score) VALUES
('11111111-1111-1111-1111-111111111111', 'h1111111-1111-1111-1111-111111111111', 'online', 'studying', 85),
('22222222-2222-2222-2222-222222222222', 'h2222222-2222-2222-2222-222222222222', 'focused', 'coding', 92),
('33333333-3333-3333-3333-333333333333', 'h3333333-3333-3333-3333-333333333333', 'break', 'writing', 76),
('44444444-4444-4444-4444-444444444444', 'h4444444-4444-4444-4444-444444444444', 'online', 'meeting', 68)
ON CONFLICT (user_id) DO UPDATE SET
    hive_id = EXCLUDED.hive_id,
    status = EXCLUDED.status,
    last_seen = CURRENT_TIMESTAMP,
    activity_type = EXCLUDED.activity_type,
    focus_score = EXCLUDED.focus_score;

-- Create timer sessions for testing
CREATE TABLE IF NOT EXISTS timer_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    hive_id UUID REFERENCES hives(id),
    session_type VARCHAR(50) DEFAULT 'pomodoro',
    duration_minutes INTEGER NOT NULL,
    status VARCHAR(50) DEFAULT 'completed',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP - INTERVAL '1 hour',
    completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed timer data
INSERT INTO timer_sessions (user_id, hive_id, session_type, duration_minutes, status) VALUES
('11111111-1111-1111-1111-111111111111', 'h1111111-1111-1111-1111-111111111111', 'pomodoro', 25, 'completed'),
('11111111-1111-1111-1111-111111111111', 'h1111111-1111-1111-1111-111111111111', 'short_break', 5, 'completed'),
('22222222-2222-2222-2222-222222222222', 'h2222222-2222-2222-2222-222222222222', 'focus', 90, 'completed'),
('33333333-3333-3333-3333-333333333333', 'h3333333-3333-3333-3333-333333333333', 'pomodoro', 25, 'active'),
('44444444-4444-4444-4444-444444444444', 'h4444444-4444-4444-4444-444444444444', 'custom', 45, 'paused')
ON CONFLICT (id) DO NOTHING;

-- Verify data
SELECT 'Hives created: ' || COUNT(*) FROM hives;
SELECT 'Hive memberships: ' || COUNT(*) FROM hive_members;
SELECT 'Presence records: ' || COUNT(*) FROM user_presence;
SELECT 'Timer sessions: ' || COUNT(*) FROM timer_sessions;