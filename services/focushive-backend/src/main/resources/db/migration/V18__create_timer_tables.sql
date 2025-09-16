-- Create timer templates table
CREATE TABLE IF NOT EXISTS timer_templates (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    focus_duration INTEGER NOT NULL,
    short_break_duration INTEGER NOT NULL,
    long_break_duration INTEGER NOT NULL,
    sessions_before_long_break INTEGER DEFAULT 4,
    auto_start_breaks BOOLEAN DEFAULT false,
    auto_start_focus BOOLEAN DEFAULT false,
    sound_enabled BOOLEAN DEFAULT true,
    notification_enabled BOOLEAN DEFAULT true,
    is_system BOOLEAN DEFAULT false,
    is_default BOOLEAN DEFAULT false,
    is_public BOOLEAN DEFAULT false,
    icon VARCHAR(100),
    color VARCHAR(50),
    usage_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP
);

-- Create indexes for timer templates
CREATE INDEX idx_timer_templates_user ON timer_templates(user_id);
CREATE INDEX idx_timer_templates_default ON timer_templates(is_default);
CREATE INDEX idx_timer_templates_system ON timer_templates(is_system);
CREATE INDEX idx_timer_templates_public ON timer_templates(is_public);

-- Create focus sessions table
CREATE TABLE IF NOT EXISTS focus_sessions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    hive_id VARCHAR(255),
    title VARCHAR(500),
    description TEXT,
    session_type VARCHAR(50) NOT NULL DEFAULT 'FOCUS',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    duration_minutes INTEGER NOT NULL,
    started_at TIMESTAMP NOT NULL,
    paused_at TIMESTAMP,
    resumed_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    total_paused_duration BIGINT,
    actual_duration BIGINT,

    -- Productivity metrics
    productivity_score INTEGER,
    tab_switches INTEGER DEFAULT 0,
    distraction_minutes INTEGER DEFAULT 0,
    focus_breaks INTEGER DEFAULT 0,
    notes_count INTEGER DEFAULT 0,
    tasks_completed INTEGER DEFAULT 0,

    -- Reminder settings
    reminder_enabled BOOLEAN DEFAULT false,
    reminder_minutes_before INTEGER DEFAULT 5,
    reminder_sent BOOLEAN DEFAULT false,

    -- Template reference
    template_id VARCHAR(36),
    template_name VARCHAR(255),

    -- Session notes and tags
    notes TEXT,
    tags VARCHAR(1000),

    -- Device sync
    device_id VARCHAR(255),
    sync_token VARCHAR(255),
    last_sync_time TIMESTAMP,

    -- Audit columns
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP,

    FOREIGN KEY (template_id) REFERENCES timer_templates(id) ON DELETE SET NULL
);

-- Create indexes for focus sessions
CREATE INDEX idx_focus_sessions_user ON focus_sessions(user_id);
CREATE INDEX idx_focus_sessions_hive ON focus_sessions(hive_id);
CREATE INDEX idx_focus_sessions_status ON focus_sessions(status);
CREATE INDEX idx_focus_sessions_started ON focus_sessions(started_at);
CREATE INDEX idx_focus_sessions_completed ON focus_sessions(completed_at);
CREATE INDEX idx_focus_sessions_template ON focus_sessions(template_id);
CREATE INDEX idx_focus_sessions_sync_token ON focus_sessions(sync_token);

-- Insert system timer templates
INSERT INTO timer_templates (
    id, name, description, focus_duration, short_break_duration,
    long_break_duration, sessions_before_long_break, is_system,
    icon, color, created_at, updated_at
) VALUES
(
    gen_random_uuid()::text,
    'Pomodoro',
    'Traditional Pomodoro Technique: 25 min focus, 5 min short break, 15 min long break',
    25, 5, 15, 4, true,
    'tomato', '#FF6347',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    gen_random_uuid()::text,
    'Deep Work',
    'Extended focus periods for deep concentration: 90 min focus, 20 min break',
    90, 20, 30, 2, true,
    'brain', '#4169E1',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    gen_random_uuid()::text,
    '52-17',
    'DeskTime productivity pattern: 52 min focus, 17 min break',
    52, 17, 30, 3, true,
    'clock', '#32CD32',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    gen_random_uuid()::text,
    'Quick Focus',
    'Short bursts for quick tasks: 15 min focus, 3 min break',
    15, 3, 10, 4, true,
    'lightning', '#FFD700',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

-- Add column comments for documentation
COMMENT ON TABLE focus_sessions IS 'Stores focus timer sessions with productivity metrics';
COMMENT ON TABLE timer_templates IS 'Stores reusable timer configurations';

COMMENT ON COLUMN focus_sessions.session_type IS 'Type of session: FOCUS, SHORT_BREAK, LONG_BREAK, CUSTOM';
COMMENT ON COLUMN focus_sessions.status IS 'Session status: ACTIVE, PAUSED, COMPLETED, CANCELLED, EXPIRED';
COMMENT ON COLUMN focus_sessions.total_paused_duration IS 'Total duration in nanoseconds the session was paused';
COMMENT ON COLUMN focus_sessions.actual_duration IS 'Actual duration in nanoseconds excluding paused time';
COMMENT ON COLUMN focus_sessions.productivity_score IS 'Calculated productivity score 0-100';
COMMENT ON COLUMN focus_sessions.sync_token IS 'Token for cross-device session synchronization';

COMMENT ON COLUMN timer_templates.is_system IS 'Whether this is a system-provided template';
COMMENT ON COLUMN timer_templates.is_default IS 'Whether this is the user default template';
COMMENT ON COLUMN timer_templates.is_public IS 'Whether this template can be shared with others';