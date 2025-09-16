-- V22__create_buddy_tables.sql
-- Create Buddy System database tables

-- Buddy Preferences table
CREATE TABLE buddy_preferences (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE,
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    focus_goals TEXT, -- JSON array stored as text
    preferred_schedule TEXT, -- JSON object stored as text
    work_hours TEXT, -- JSON object stored as text (day of week -> time ranges)
    communication_style VARCHAR(20) NOT NULL DEFAULT 'BALANCED',
    session_duration_preference INTEGER DEFAULT 25, -- in minutes
    availability_buffer INTEGER DEFAULT 15, -- buffer time in minutes
    notification_preferences TEXT, -- JSON object stored as text
    matching_criteria TEXT, -- JSON object stored as text
    is_available_for_matching BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_buddy_preferences_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Buddy Relationships table
CREATE TABLE buddy_relationships (
    id VARCHAR(36) PRIMARY KEY,
    user1_id VARCHAR(36) NOT NULL,
    user2_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    initiator_id VARCHAR(36) NOT NULL,
    match_score DECIMAL(3,2) DEFAULT 0.0,
    matched_at TIMESTAMP,
    request_message TEXT,
    rejection_reason TEXT,
    termination_reason TEXT,
    relationship_rating DECIMAL(2,1),
    total_sessions INTEGER NOT NULL DEFAULT 0,
    total_goals_completed INTEGER NOT NULL DEFAULT 0,
    streak_count INTEGER NOT NULL DEFAULT 0,
    last_activity_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_buddy_relationship_user1 FOREIGN KEY (user1_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_buddy_relationship_user2 FOREIGN KEY (user2_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_buddy_relationship_initiator FOREIGN KEY (initiator_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_buddy_relationship_users UNIQUE (user1_id, user2_id),
    CONSTRAINT ck_buddy_relationship_different_users CHECK (user1_id != user2_id),
    CONSTRAINT ck_buddy_relationship_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'TERMINATED')),
    CONSTRAINT ck_buddy_relationship_rating CHECK (relationship_rating BETWEEN 1.0 AND 5.0)
);

-- Buddy Goals table
CREATE TABLE buddy_goals (
    id VARCHAR(36) PRIMARY KEY,
    buddy_relationship_id VARCHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    goal_type VARCHAR(30) NOT NULL DEFAULT 'COLLABORATIVE',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    target_value DECIMAL(10,2),
    current_progress DECIMAL(10,2) DEFAULT 0.0,
    unit_of_measurement VARCHAR(20),
    deadline TIMESTAMP,
    priority_level VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    category VARCHAR(50),
    tags TEXT, -- JSON array stored as text
    created_by_user_id VARCHAR(36) NOT NULL,
    completed_at TIMESTAMP,
    completed_by_user_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_buddy_goal_relationship FOREIGN KEY (buddy_relationship_id) REFERENCES buddy_relationships(id) ON DELETE CASCADE,
    CONSTRAINT fk_buddy_goal_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_buddy_goal_completed_by FOREIGN KEY (completed_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_buddy_goal_type CHECK (goal_type IN ('COLLABORATIVE', 'COMPETITIVE', 'SUPPORTIVE')),
    CONSTRAINT ck_buddy_goal_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED', 'ON_HOLD')),
    CONSTRAINT ck_buddy_goal_priority CHECK (priority_level IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT'))
);

-- Buddy Sessions table
CREATE TABLE buddy_sessions (
    id VARCHAR(36) PRIMARY KEY,
    buddy_relationship_id VARCHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    session_type VARCHAR(30) NOT NULL DEFAULT 'FOCUS_SESSION',
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    scheduled_start_time TIMESTAMP NOT NULL,
    scheduled_end_time TIMESTAMP NOT NULL,
    actual_start_time TIMESTAMP,
    actual_end_time TIMESTAMP,
    scheduled_by_user_id VARCHAR(36) NOT NULL,
    meeting_url VARCHAR(500),
    agenda TEXT,
    notes TEXT,
    cancellation_reason TEXT,
    user1_attendance BOOLEAN DEFAULT false,
    user2_attendance BOOLEAN DEFAULT false,
    user1_rating INTEGER,
    user2_rating INTEGER,
    user1_feedback TEXT,
    user2_feedback TEXT,
    productivity_score DECIMAL(3,2),
    goals_discussed TEXT, -- JSON array of goal IDs
    follow_up_actions TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_buddy_session_relationship FOREIGN KEY (buddy_relationship_id) REFERENCES buddy_relationships(id) ON DELETE CASCADE,
    CONSTRAINT fk_buddy_session_scheduled_by FOREIGN KEY (scheduled_by_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT ck_buddy_session_type CHECK (session_type IN ('FOCUS_SESSION', 'CHECK_IN', 'GOAL_REVIEW', 'CASUAL_CHAT', 'ACCOUNTABILITY')),
    CONSTRAINT ck_buddy_session_status CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    CONSTRAINT ck_buddy_session_times CHECK (scheduled_end_time > scheduled_start_time),
    CONSTRAINT ck_buddy_session_user1_rating CHECK (user1_rating BETWEEN 1 AND 5),
    CONSTRAINT ck_buddy_session_user2_rating CHECK (user2_rating BETWEEN 1 AND 5)
);

-- Buddy Check-ins table
CREATE TABLE buddy_checkins (
    id VARCHAR(36) PRIMARY KEY,
    buddy_relationship_id VARCHAR(36) NOT NULL,
    initiator_id VARCHAR(36) NOT NULL,
    mood VARCHAR(20) NOT NULL,
    energy_level INTEGER NOT NULL CHECK (energy_level BETWEEN 1 AND 10),
    productivity_rating INTEGER NOT NULL CHECK (productivity_rating BETWEEN 1 AND 10),
    progress_update TEXT,
    challenges TEXT,
    goals_for_next_period TEXT,
    support_needed TEXT,
    celebration_notes TEXT,
    tags TEXT, -- JSON array stored as text
    is_private BOOLEAN NOT NULL DEFAULT false,
    response_id VARCHAR(36), -- Reference to partner's response checkin
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_buddy_checkin_relationship FOREIGN KEY (buddy_relationship_id) REFERENCES buddy_relationships(id) ON DELETE CASCADE,
    CONSTRAINT fk_buddy_checkin_initiator FOREIGN KEY (initiator_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_buddy_checkin_response FOREIGN KEY (response_id) REFERENCES buddy_checkins(id) ON DELETE SET NULL,
    CONSTRAINT ck_buddy_checkin_mood CHECK (mood IN ('EXCELLENT', 'GOOD', 'NEUTRAL', 'STRUGGLING', 'DIFFICULT'))
);

-- Create indexes for better performance
CREATE INDEX idx_buddy_preferences_user ON buddy_preferences(user_id);
CREATE INDEX idx_buddy_preferences_matching ON buddy_preferences(is_available_for_matching);

CREATE INDEX idx_buddy_relationship_user1 ON buddy_relationships(user1_id);
CREATE INDEX idx_buddy_relationship_user2 ON buddy_relationships(user2_id);
CREATE INDEX idx_buddy_relationship_status ON buddy_relationships(status);
CREATE INDEX idx_buddy_relationship_activity ON buddy_relationships(last_activity_at);
CREATE INDEX idx_buddy_relationship_match_score ON buddy_relationships(match_score DESC);

CREATE INDEX idx_buddy_goal_relationship ON buddy_goals(buddy_relationship_id);
CREATE INDEX idx_buddy_goal_status ON buddy_goals(status);
CREATE INDEX idx_buddy_goal_deadline ON buddy_goals(deadline);
CREATE INDEX idx_buddy_goal_created_by ON buddy_goals(created_by_user_id);
CREATE INDEX idx_buddy_goal_priority ON buddy_goals(priority_level);

CREATE INDEX idx_buddy_session_relationship ON buddy_sessions(buddy_relationship_id);
CREATE INDEX idx_buddy_session_status ON buddy_sessions(status);
CREATE INDEX idx_buddy_session_scheduled_start ON buddy_sessions(scheduled_start_time);
CREATE INDEX idx_buddy_session_scheduled_by ON buddy_sessions(scheduled_by_user_id);

CREATE INDEX idx_buddy_checkin_relationship ON buddy_checkins(buddy_relationship_id);
CREATE INDEX idx_buddy_checkin_initiator ON buddy_checkins(initiator_id);
CREATE INDEX idx_buddy_checkin_created ON buddy_checkins(created_at DESC);
CREATE INDEX idx_buddy_checkin_mood ON buddy_checkins(mood);

-- Insert some default buddy preferences for system users
INSERT INTO buddy_preferences (
    id, user_id, timezone, communication_style,
    session_duration_preference, is_available_for_matching
) VALUES
(
    'system-buddy-prefs-1', 'system-user-1', 'UTC', 'BALANCED',
    25, false
);

-- Create a function to automatically create buddy preferences for new users
-- (This would be better implemented as a trigger in production)
COMMENT ON TABLE buddy_preferences IS 'User preferences for buddy matching and sessions';
COMMENT ON TABLE buddy_relationships IS 'Buddy partnerships and their status';
COMMENT ON TABLE buddy_goals IS 'Shared goals between buddy pairs';
COMMENT ON TABLE buddy_sessions IS 'Scheduled and completed buddy sessions';
COMMENT ON TABLE buddy_checkins IS 'Regular check-ins between buddies';