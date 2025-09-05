-- Note: focus_sessions table already exists in V4__create_analytics_tables.sql
-- This migration only creates new productivity tracking tables that don't exist yet

-- Create productivity_stats table for daily aggregated stats
CREATE TABLE productivity_stats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
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
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
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
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hive_id UUID NOT NULL REFERENCES hives(id) ON DELETE CASCADE,
    timer_type VARCHAR(20) NOT NULL CHECK (timer_type IN ('POMODORO', 'COUNTDOWN', 'STOPWATCH')),
    duration_minutes INTEGER NOT NULL,
    remaining_seconds INTEGER NOT NULL,
    is_running BOOLEAN DEFAULT FALSE,
    started_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    started_at TIMESTAMP,
    paused_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indices for better query performance
-- Note: focus_sessions indexes already exist in V4__create_analytics_tables.sql
CREATE INDEX idx_productivity_user_date ON productivity_stats(user_id, date);
CREATE INDEX idx_hive_timers_hive ON hive_timers(hive_id);

-- Note: update_updated_at_column function already exists in V1__create_users_table.sql
-- Add triggers for new tables only

CREATE TRIGGER update_productivity_stats_updated_at BEFORE UPDATE ON productivity_stats
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_pomodoro_settings_updated_at BEFORE UPDATE ON pomodoro_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_hive_timers_updated_at BEFORE UPDATE ON hive_timers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();