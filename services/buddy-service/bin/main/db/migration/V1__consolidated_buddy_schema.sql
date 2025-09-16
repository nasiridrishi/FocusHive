-- Consolidated Buddy Service Schema Migration
-- This migration creates the complete schema matching all entity definitions
-- Supersedes all previous migrations for a clean schema

-- Drop existing tables if they exist (for clean setup)
DROP TABLE IF EXISTS buddy_interactions CASCADE;
DROP TABLE IF EXISTS partnership_health CASCADE;
DROP TABLE IF EXISTS success_stories CASCADE;
DROP TABLE IF EXISTS matching_queue CASCADE;
DROP TABLE IF EXISTS accountability_scores CASCADE;
DROP TABLE IF EXISTS buddy_checkins CASCADE;
DROP TABLE IF EXISTS goal_milestones CASCADE;
DROP TABLE IF EXISTS buddy_goals CASCADE;
DROP TABLE IF EXISTS shared_goals CASCADE;
DROP TABLE IF EXISTS buddy_requests CASCADE;
DROP TABLE IF EXISTS buddy_partnerships CASCADE;
DROP TABLE IF EXISTS buddy_preferences CASCADE;
DROP TABLE IF EXISTS matching_preferences CASCADE;
DROP TABLE IF EXISTS buddy_users CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- ==============================================================================
-- BUDDY USERS TABLE (lightweight user representation)
-- ==============================================================================
CREATE TABLE buddy_users (
    id VARCHAR(255) PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    timezone VARCHAR(50),
    interests TEXT[],
    preferred_focus_times TEXT[],
    experience_level VARCHAR(20),
    communication_style VARCHAR(50),
    personality_type VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT true,
    last_seen_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- BUDDY PREFERENCES TABLE
-- Matches BuddyPreferences.java entity
-- ==============================================================================
CREATE TABLE buddy_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    preferred_timezone VARCHAR(50),
    preferred_work_hours JSONB DEFAULT '{}',
    focus_areas TEXT[],
    goals TEXT[],
    communication_style VARCHAR(50),
    matching_enabled BOOLEAN NOT NULL DEFAULT true,
    timezone_flexibility INTEGER DEFAULT 2,
    min_commitment_hours INTEGER DEFAULT 10,
    max_partners INTEGER DEFAULT 3,
    language VARCHAR(10) DEFAULT 'en',
    personality_type VARCHAR(50),
    experience_level VARCHAR(20),
    last_active_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- BUDDY PARTNERSHIPS TABLE
-- Matches BuddyPartnership.java entity
-- ==============================================================================
CREATE TABLE buddy_partnerships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user1_id UUID NOT NULL,
    user2_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    end_reason TEXT,
    agreement_text TEXT,
    duration_days INTEGER,
    compatibility_score DECIMAL(5,4),
    health_score DECIMAL(5,4),
    last_interaction_at TIMESTAMPTZ,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,

    -- Constraints
    CONSTRAINT chk_partnership_status CHECK (status IN ('PENDING', 'ACTIVE', 'PAUSED', 'ENDED')),
    CONSTRAINT chk_different_users CHECK (user1_id != user2_id),
    CONSTRAINT chk_compatibility_score CHECK (compatibility_score BETWEEN 0.0000 AND 1.0000),
    CONSTRAINT chk_health_score CHECK (health_score BETWEEN 0.0000 AND 1.0000),
    CONSTRAINT uk_user_pair UNIQUE (user1_id, user2_id)
);

-- ==============================================================================
-- SHARED GOALS TABLE (buddy_goals in code, but shared_goals in DB)
-- Matches BuddyGoal.java entity
-- ==============================================================================
CREATE TABLE shared_goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partnership_id UUID NOT NULL REFERENCES buddy_partnerships(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    target_date DATE,
    progress_percentage INTEGER DEFAULT 0,
    created_by UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,

    -- Constraints
    CONSTRAINT chk_goal_status CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'PAUSED', 'CANCELLED', 'OVERDUE')),
    CONSTRAINT chk_progress_percentage CHECK (progress_percentage BETWEEN 0 AND 100)
);

-- ==============================================================================
-- GOAL MILESTONES TABLE
-- Matches GoalMilestone.java entity
-- ==============================================================================
CREATE TABLE goal_milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id UUID NOT NULL REFERENCES shared_goals(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    target_date DATE,
    completed_at TIMESTAMPTZ,
    completed_by UUID,
    order_index INTEGER DEFAULT 0,
    celebration_sent BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- ==============================================================================
-- BUDDY CHECKINS TABLE
-- Matches BuddyCheckin.java entity
-- ==============================================================================
CREATE TABLE buddy_checkins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partnership_id UUID NOT NULL REFERENCES buddy_partnerships(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    checkin_type VARCHAR(20),
    content TEXT,
    mood VARCHAR(20),
    productivity_rating INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- ==============================================================================
-- ACCOUNTABILITY SCORES TABLE
-- Matches AccountabilityScore.java entity
-- ==============================================================================
CREATE TABLE accountability_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    partnership_id UUID REFERENCES buddy_partnerships(id) ON DELETE CASCADE,
    score DECIMAL(3,2) DEFAULT 0.00,
    checkins_completed INTEGER DEFAULT 0,
    goals_achieved INTEGER DEFAULT 0,
    response_rate DECIMAL(3,2) DEFAULT 0.00,
    streak_days INTEGER DEFAULT 0,
    calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,

    -- Constraints
    CONSTRAINT chk_score CHECK (score BETWEEN 0.00 AND 1.00),
    CONSTRAINT chk_response_rate CHECK (response_rate BETWEEN 0.00 AND 1.00),
    CONSTRAINT uk_user_partnership UNIQUE (user_id, partnership_id)
);


-- ==============================================================================
-- INDEXES FOR PERFORMANCE
-- ==============================================================================

-- Buddy Preferences indexes
CREATE INDEX idx_buddy_preferences_user_id ON buddy_preferences(user_id);
CREATE INDEX idx_buddy_preferences_matching_enabled ON buddy_preferences(matching_enabled);
CREATE INDEX idx_buddy_preferences_last_active ON buddy_preferences(last_active_at);

-- Buddy Partnerships indexes
CREATE INDEX idx_partnerships_user1 ON buddy_partnerships(user1_id);
CREATE INDEX idx_partnerships_user2 ON buddy_partnerships(user2_id);
CREATE INDEX idx_partnerships_status ON buddy_partnerships(status);
CREATE INDEX idx_partnerships_health_score ON buddy_partnerships(health_score);
CREATE INDEX idx_partnerships_started_at ON buddy_partnerships(started_at);

-- Shared Goals indexes
CREATE INDEX idx_goals_partnership ON shared_goals(partnership_id);
CREATE INDEX idx_goals_created_by ON shared_goals(created_by);
CREATE INDEX idx_goals_status ON shared_goals(status);
CREATE INDEX idx_goals_target_date ON shared_goals(target_date);

-- Goal Milestones indexes
CREATE INDEX idx_milestones_goal ON goal_milestones(goal_id);
CREATE INDEX idx_milestones_target_date ON goal_milestones(target_date);
CREATE INDEX idx_completed_by ON goal_milestones(completed_by);
CREATE INDEX idx_celebration_sent ON goal_milestones(celebration_sent);

-- Buddy Checkins indexes
CREATE INDEX idx_checkins_partnership ON buddy_checkins(partnership_id);
CREATE INDEX idx_checkins_user ON buddy_checkins(user_id);
CREATE INDEX idx_checkins_created_at ON buddy_checkins(created_at);

-- Accountability Scores indexes
CREATE INDEX idx_accountability_user ON accountability_scores(user_id);
CREATE INDEX idx_accountability_partnership ON accountability_scores(partnership_id);
CREATE INDEX idx_accountability_score ON accountability_scores(score);

-- ==============================================================================
-- COMMENTS ON TABLES AND COLUMNS
-- ==============================================================================

COMMENT ON TABLE buddy_preferences IS 'User preferences for buddy matching algorithm';
COMMENT ON TABLE buddy_partnerships IS 'Active and historical buddy partnerships';
COMMENT ON TABLE shared_goals IS 'Goals created within buddy partnerships';
COMMENT ON TABLE goal_milestones IS 'Milestones for buddy goals';
COMMENT ON TABLE buddy_checkins IS 'Regular check-ins between buddy partners';
COMMENT ON TABLE accountability_scores IS 'Accountability metrics for users and partnerships';

COMMENT ON COLUMN buddy_preferences.preferred_work_hours IS 'JSONB map of day to work hours';
COMMENT ON COLUMN buddy_preferences.focus_areas IS 'Array of focus areas like CODING, STUDYING, etc';
COMMENT ON COLUMN buddy_partnerships.compatibility_score IS 'Algorithm-calculated compatibility (0-1)';
COMMENT ON COLUMN buddy_partnerships.health_score IS 'Partnership health score (0-1)';
COMMENT ON COLUMN accountability_scores.response_rate IS 'Rate of responding to buddy interactions';
