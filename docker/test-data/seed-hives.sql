-- ===================================================================
-- ADDITIONAL HIVE AND ACTIVITY TEST DATA
-- ===================================================================

\c focushive_test;

-- Create additional test tables for comprehensive E2E coverage
CREATE TABLE IF NOT EXISTS hive_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hive_id UUID NOT NULL,
    user_id UUID NOT NULL,
    activity_type VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB DEFAULT '{}'::jsonb
);

-- Seed hive activities
INSERT INTO hive_activities (hive_id, user_id, activity_type, description, metadata) VALUES
-- Study Group Alpha activities
('h1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'joined_hive', 'Alice joined the study group', '{"welcome": true}'),
('h1111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'started_timer', 'Bob started a 25-minute focus session', '{"duration": 25, "type": "pomodoro"}'),
('h1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 'shared_progress', 'Charlie shared study progress', '{"subject": "algorithms", "progress": 75}'),

-- Work Focus Hive activities
('h2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'created_hive', 'Bob created the work focus hive', '{"initial_setup": true}'),
('h2222222-2222-2222-2222-222222222222', '44444444-4444-4444-4444-444444444444', 'became_moderator', 'Diana became a moderator', '{"promoted_by": "22222222-2222-2222-2222-222222222222"}'),
('h2222222-2222-2222-2222-222222222222', '55555555-5555-5555-5555-555555555555', 'completed_session', 'Eve completed a 90-minute deep work session', '{"duration": 90, "productivity_score": 94}'),

-- Creative Writing activities
('h3333333-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', 'set_theme', 'Charlie set daily writing theme: "Sci-fi short stories"', '{"theme": "sci-fi", "word_goal": 500}'),
('h3333333-3333-3333-3333-333333333333', '88888888-8888-8888-8888-888888888888', 'word_count_update', 'User Three updated word count', '{"words_written": 247, "session_time": 45}'),
('h3333333-3333-3333-3333-333333333333', '99999999-9999-9999-9999-999999999999', 'peer_review', 'User Four provided feedback on draft', '{"feedback_type": "positive", "suggestions": 3}')
ON CONFLICT (id) DO NOTHING;

-- Create hive settings for different test scenarios
CREATE TABLE IF NOT EXISTS hive_settings (
    hive_id UUID PRIMARY KEY,
    settings JSONB NOT NULL DEFAULT '{}'::jsonb,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed hive settings
INSERT INTO hive_settings (hive_id, settings) VALUES
('h1111111-1111-1111-1111-111111111111', '{
    "timer_defaults": {"pomodoro": 25, "short_break": 5, "long_break": 15},
    "music_enabled": true,
    "notifications": {"sound": true, "desktop": true},
    "theme": "academic",
    "privacy": {"show_activity": true, "show_timer": true}
}'),
('h2222222-2222-2222-2222-222222222222', '{
    "timer_defaults": {"focus": 90, "break": 10},
    "music_enabled": false,
    "notifications": {"sound": false, "desktop": true},
    "theme": "professional",
    "privacy": {"show_activity": false, "show_timer": true},
    "work_mode": true
}'),
('h3333333-3333-3333-3333-333333333333', '{
    "timer_defaults": {"creative": 60, "reflection": 15},
    "music_enabled": true,
    "notifications": {"sound": true, "desktop": false},
    "theme": "creative",
    "privacy": {"show_activity": true, "show_timer": false},
    "word_goals": true
}'),
('h4444444-4444-4444-4444-444444444444', '{
    "timer_defaults": {"meeting": 30, "break": 5},
    "music_enabled": false,
    "notifications": {"sound": false, "desktop": false},
    "theme": "minimal",
    "privacy": {"show_activity": false, "show_timer": false},
    "private_mode": true
}'),
('h5555555-5555-5555-5555-555555555555', '{
    "timer_defaults": {"community": 45, "social_break": 10},
    "music_enabled": true,
    "notifications": {"sound": true, "desktop": true},
    "theme": "community",
    "privacy": {"show_activity": true, "show_timer": true},
    "leaderboard": true,
    "chat_enabled": true
}')
ON CONFLICT (hive_id) DO UPDATE SET
    settings = EXCLUDED.settings,
    updated_at = CURRENT_TIMESTAMP;

-- Create focus streaks for gamification testing
CREATE TABLE IF NOT EXISTS focus_streaks (
    user_id UUID PRIMARY KEY,
    current_streak INTEGER DEFAULT 0,
    longest_streak INTEGER DEFAULT 0,
    last_activity_date DATE DEFAULT CURRENT_DATE,
    total_focus_time INTEGER DEFAULT 0, -- in minutes
    streak_data JSONB DEFAULT '[]'::jsonb
);

-- Seed focus streaks
INSERT INTO focus_streaks (user_id, current_streak, longest_streak, total_focus_time, streak_data) VALUES
('11111111-1111-1111-1111-111111111111', 7, 14, 1250, '[
    {"date": "2024-09-12", "sessions": 3, "minutes": 75},
    {"date": "2024-09-11", "sessions": 2, "minutes": 50},
    {"date": "2024-09-10", "sessions": 4, "minutes": 100}
]'),
('22222222-2222-2222-2222-222222222222', 12, 18, 2100, '[
    {"date": "2024-09-12", "sessions": 2, "minutes": 180},
    {"date": "2024-09-11", "sessions": 1, "minutes": 90},
    {"date": "2024-09-10", "sessions": 3, "minutes": 135}
]'),
('33333333-3333-3333-3333-333333333333', 3, 8, 650, '[
    {"date": "2024-09-12", "sessions": 2, "minutes": 60},
    {"date": "2024-09-11", "sessions": 1, "minutes": 45}
]'),
('44444444-4444-4444-4444-444444444444', 1, 5, 320, '[
    {"date": "2024-09-12", "sessions": 1, "minutes": 30}
]'),
('55555555-5555-5555-5555-555555555555', 5, 11, 890, '[
    {"date": "2024-09-12", "sessions": 3, "minutes": 90},
    {"date": "2024-09-11", "sessions": 2, "minutes": 80}
]')
ON CONFLICT (user_id) DO UPDATE SET
    current_streak = EXCLUDED.current_streak,
    longest_streak = EXCLUDED.longest_streak,
    total_focus_time = EXCLUDED.total_focus_time,
    last_activity_date = EXCLUDED.last_activity_date,
    streak_data = EXCLUDED.streak_data;

-- Create achievements for gamification testing
CREATE TABLE IF NOT EXISTS user_achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    achievement_type VARCHAR(100) NOT NULL,
    achievement_name VARCHAR(255) NOT NULL,
    description TEXT,
    earned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB DEFAULT '{}'::jsonb
);

-- Seed achievements
INSERT INTO user_achievements (user_id, achievement_type, achievement_name, description, metadata) VALUES
('11111111-1111-1111-1111-111111111111', 'streak', 'Week Warrior', 'Completed 7 consecutive days of focus sessions', '{"streak_length": 7}'),
('11111111-1111-1111-1111-111111111111', 'session', 'Pomodoro Master', 'Completed 100 Pomodoro sessions', '{"sessions_completed": 100}'),
('22222222-2222-2222-2222-222222222222', 'focus', 'Deep Work Champion', 'Completed a 3-hour continuous focus session', '{"session_duration": 180}'),
('22222222-2222-2222-2222-222222222222', 'community', 'Hive Builder', 'Created a hive with 10+ active members', '{"hive_id": "h2222222-2222-2222-2222-222222222222", "member_count": 12}'),
('33333333-3333-3333-3333-333333333333', 'creative', 'Word Smith', 'Wrote 10,000 words during focus sessions', '{"total_words": 10247}'),
('44444444-4444-4444-4444-444444444444', 'leadership', 'Moderator Badge', 'Became a hive moderator', '{"hive_id": "h2222222-2222-2222-2222-222222222222"}'),
('55555555-5555-5555-5555-555555555555', 'social', 'Community Builder', 'Helped 5+ users achieve their focus goals', '{"users_helped": 7}')
ON CONFLICT (id) DO NOTHING;

-- Verify additional data
SELECT 'Hive activities: ' || COUNT(*) FROM hive_activities;
SELECT 'Hive settings: ' || COUNT(*) FROM hive_settings;
SELECT 'Focus streaks: ' || COUNT(*) FROM focus_streaks;
SELECT 'User achievements: ' || COUNT(*) FROM user_achievements;