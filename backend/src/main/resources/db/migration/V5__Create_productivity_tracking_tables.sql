-- Create focus_sessions table for tracking individual work/study sessions
CREATE TABLE focus_sessions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    hive_id VARCHAR(36),
    session_type VARCHAR(20) NOT NULL CHECK (session_type IN ('WORK', 'STUDY', 'BREAK', 'CUSTOM')),
    duration_minutes INTEGER NOT NULL,
    actual_duration_minutes INTEGER,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    completed BOOLEAN DEFAULT FALSE,
    interruptions INTEGER DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_session_hive FOREIGN KEY (hive_id) REFERENCES hives(id) ON DELETE SET NULL
);

-- Create productivity_stats table for daily aggregated stats
CREATE TABLE productivity_stats (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    date DATE NOT NULL,
    total_focus_minutes INTEGER DEFAULT 0,
    total_break_minutes INTEGER DEFAULT 0,
    sessions_completed INTEGER DEFAULT 0,
    sessions_started INTEGER DEFAULT 0,
    longest_streak_minutes INTEGER DEFAULT 0,
    daily_goal_minutes INTEGER DEFAULT 480, -- 8 hours default
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, date)
);

-- Create pomodoro_settings table for user preferences
CREATE TABLE pomodoro_settings (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE,
    work_duration_minutes INTEGER DEFAULT 25,
    short_break_minutes INTEGER DEFAULT 5,
    long_break_minutes INTEGER DEFAULT 15,
    sessions_until_long_break INTEGER DEFAULT 4,
    auto_start_breaks BOOLEAN DEFAULT FALSE,
    auto_start_work BOOLEAN DEFAULT FALSE,
    notification_enabled BOOLEAN DEFAULT TRUE,
    sound_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create hive_timers table for synchronized hive sessions
CREATE TABLE hive_timers (
    id VARCHAR(36) PRIMARY KEY,
    hive_id VARCHAR(36) NOT NULL,
    timer_type VARCHAR(20) NOT NULL CHECK (timer_type IN ('POMODORO', 'COUNTDOWN', 'STOPWATCH')),
    duration_minutes INTEGER NOT NULL,
    remaining_seconds INTEGER NOT NULL,
    is_running BOOLEAN DEFAULT FALSE,
    started_by VARCHAR(36) NOT NULL,
    started_at TIMESTAMP,
    paused_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hive_timer_hive FOREIGN KEY (hive_id) REFERENCES hives(id) ON DELETE CASCADE
);

-- Create indices for better query performance
CREATE INDEX idx_sessions_user_date ON focus_sessions(user_id, start_time);
CREATE INDEX idx_sessions_hive ON focus_sessions(hive_id);
CREATE INDEX idx_productivity_user_date ON productivity_stats(user_id, date);
CREATE INDEX idx_hive_timers_hive ON hive_timers(hive_id);

-- Add trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_focus_sessions_updated_at BEFORE UPDATE ON focus_sessions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_productivity_stats_updated_at BEFORE UPDATE ON productivity_stats
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_pomodoro_settings_updated_at BEFORE UPDATE ON pomodoro_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_hive_timers_updated_at BEFORE UPDATE ON hive_timers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();