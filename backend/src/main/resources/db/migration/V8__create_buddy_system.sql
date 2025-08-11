-- V8__create_buddy_system.sql
-- Buddy System Tables for FocusHive

-- Buddy relationships between users
CREATE TABLE buddy_relationships (
    id BIGSERIAL PRIMARY KEY,
    user1_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user2_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    initiated_by BIGINT NOT NULL REFERENCES users(id),
    matched_at TIMESTAMP,
    ended_at TIMESTAMP,
    match_score DECIMAL(3,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_buddy_pair UNIQUE(user1_id, user2_id),
    CONSTRAINT different_users CHECK (user1_id != user2_id),
    CONSTRAINT ordered_users CHECK (user1_id < user2_id),
    CONSTRAINT status_check CHECK (status IN ('PENDING', 'ACTIVE', 'ENDED', 'BLOCKED'))
);

-- Buddy goals for accountability
CREATE TABLE buddy_goals (
    id BIGSERIAL PRIMARY KEY,
    relationship_id BIGINT NOT NULL REFERENCES buddy_relationships(id) ON DELETE CASCADE,
    created_by BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    target_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    progress INTEGER DEFAULT 0 CHECK (progress >= 0 AND progress <= 100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT status_check CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED'))
);

-- Buddy check-ins for regular accountability
CREATE TABLE buddy_checkins (
    id BIGSERIAL PRIMARY KEY,
    relationship_id BIGINT NOT NULL REFERENCES buddy_relationships(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    message TEXT,
    mood VARCHAR(20),
    productivity_score INTEGER CHECK (productivity_score >= 1 AND productivity_score <= 10),
    goals_progress JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT mood_check CHECK (mood IN ('EXCELLENT', 'GOOD', 'NEUTRAL', 'STRUGGLING', 'DIFFICULT'))
);

-- Buddy preferences for matching algorithm
CREATE TABLE buddy_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    preferred_timezone VARCHAR(50),
    preferred_work_hours JSONB,
    focus_areas TEXT[],
    communication_style VARCHAR(50),
    matching_enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_user_preferences UNIQUE(user_id),
    CONSTRAINT communication_check CHECK (communication_style IN ('FREQUENT', 'MODERATE', 'MINIMAL'))
);

-- Buddy sessions for synchronized work
CREATE TABLE buddy_sessions (
    id BIGSERIAL PRIMARY KEY,
    relationship_id BIGINT NOT NULL REFERENCES buddy_relationships(id) ON DELETE CASCADE,
    initiated_by BIGINT NOT NULL REFERENCES users(id),
    session_type VARCHAR(50) NOT NULL,
    planned_duration INTEGER NOT NULL,
    actual_duration INTEGER,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT status_check CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'CANCELLED'))
);

-- Indexes for performance
CREATE INDEX idx_buddy_relationships_users ON buddy_relationships(user1_id, user2_id);
CREATE INDEX idx_buddy_relationships_status ON buddy_relationships(status);
CREATE INDEX idx_buddy_relationships_created ON buddy_relationships(created_at DESC);
CREATE INDEX idx_buddy_goals_relationship ON buddy_goals(relationship_id);
CREATE INDEX idx_buddy_goals_status ON buddy_goals(status);
CREATE INDEX idx_buddy_checkins_relationship ON buddy_checkins(relationship_id);
CREATE INDEX idx_buddy_checkins_user ON buddy_checkins(user_id);
CREATE INDEX idx_buddy_checkins_created ON buddy_checkins(created_at DESC);
CREATE INDEX idx_buddy_preferences_user ON buddy_preferences(user_id);
CREATE INDEX idx_buddy_preferences_matching ON buddy_preferences(matching_enabled) WHERE matching_enabled = true;
CREATE INDEX idx_buddy_sessions_relationship ON buddy_sessions(relationship_id);
CREATE INDEX idx_buddy_sessions_status ON buddy_sessions(status);

-- Comments for documentation
COMMENT ON TABLE buddy_relationships IS 'Stores buddy accountability partnerships between users';
COMMENT ON TABLE buddy_goals IS 'Shared goals between buddy partners for accountability';
COMMENT ON TABLE buddy_checkins IS 'Regular check-ins between buddy partners';
COMMENT ON TABLE buddy_preferences IS 'User preferences for buddy matching algorithm';
COMMENT ON TABLE buddy_sessions IS 'Synchronized work sessions between buddies';

COMMENT ON COLUMN buddy_relationships.user1_id IS 'First user ID (always lower than user2_id for consistency)';
COMMENT ON COLUMN buddy_relationships.user2_id IS 'Second user ID (always higher than user1_id for consistency)';
COMMENT ON COLUMN buddy_relationships.match_score IS 'Algorithm-calculated compatibility score (0.00 to 1.00)';
COMMENT ON COLUMN buddy_goals.progress IS 'Goal completion percentage (0-100)';
COMMENT ON COLUMN buddy_checkins.goals_progress IS 'JSON object with goal_id as key and progress as value';
COMMENT ON COLUMN buddy_preferences.preferred_work_hours IS 'JSON object with days as keys and hour ranges as values';
COMMENT ON COLUMN buddy_preferences.focus_areas IS 'Array of focus areas like coding, writing, studying, etc';