-- Create focus_sessions table
CREATE TABLE focus_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    hive_id UUID,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE,
    target_duration_minutes INTEGER NOT NULL CHECK (target_duration_minutes > 0),
    actual_duration_minutes INTEGER CHECK (actual_duration_minutes >= 0),
    type VARCHAR(20) DEFAULT 'FOCUS' CHECK (type IN ('FOCUS', 'BREAK', 'MEDITATION', 'PLANNING')),
    completed BOOLEAN DEFAULT FALSE,
    breaks_taken INTEGER DEFAULT 0,
    distractions_logged INTEGER DEFAULT 0,
    productivity_score INTEGER CHECK (productivity_score >= 0 AND productivity_score <= 100),
    notes TEXT,
    tags TEXT[],
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for focus_sessions
CREATE INDEX idx_sessions_user ON focus_sessions(user_id);
CREATE INDEX idx_sessions_hive ON focus_sessions(hive_id) WHERE hive_id IS NOT NULL;
CREATE INDEX idx_sessions_user_time ON focus_sessions(user_id, start_time DESC);
CREATE INDEX idx_sessions_completed ON focus_sessions(user_id, completed) WHERE completed = TRUE;
CREATE INDEX idx_sessions_type ON focus_sessions(type);
CREATE INDEX idx_sessions_date ON focus_sessions(start_time);

-- Create function to update updated_at column
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_focus_sessions_updated_at BEFORE UPDATE ON focus_sessions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create session_breaks table
CREATE TABLE session_breaks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES focus_sessions(id) ON DELETE CASCADE,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE,
    duration_minutes INTEGER,
    reason VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_breaks_session ON session_breaks(session_id);

-- Create daily_summaries table
CREATE TABLE daily_summaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    date DATE NOT NULL,
    total_minutes INTEGER DEFAULT 0,
    focus_minutes INTEGER DEFAULT 0,
    break_minutes INTEGER DEFAULT 0,
    sessions_count INTEGER DEFAULT 0,
    completed_sessions INTEGER DEFAULT 0,
    average_session_length INTEGER,
    productivity_score INTEGER,
    most_productive_hour INTEGER CHECK (most_productive_hour >= 0 AND most_productive_hour <= 23),
    breaks_taken INTEGER DEFAULT 0,
    distractions_count INTEGER DEFAULT 0,
    goals_achieved INTEGER DEFAULT 0,
    streak_days INTEGER DEFAULT 0,
    hives_visited JSONB DEFAULT '[]'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, date)
);

-- Indexes for daily_summaries
CREATE INDEX idx_daily_summaries_user_date ON daily_summaries(user_id, date DESC);
CREATE INDEX idx_daily_summaries_date ON daily_summaries(date);

CREATE TRIGGER update_daily_summaries_updated_at BEFORE UPDATE ON daily_summaries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create user_achievements table
CREATE TABLE user_achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    achievement_type VARCHAR(50) NOT NULL,
    achievement_key VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    icon_url VARCHAR(500),
    points INTEGER DEFAULT 0,
    level INTEGER DEFAULT 1,
    progress JSONB DEFAULT '{}'::jsonb,
    unlocked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, achievement_key)
);

-- Indexes for user_achievements
CREATE INDEX idx_achievements_user ON user_achievements(user_id);
CREATE INDEX idx_achievements_type ON user_achievements(achievement_type);
CREATE INDEX idx_achievements_unlocked ON user_achievements(user_id, unlocked_at DESC);

-- Create productivity_goals table
CREATE TABLE productivity_goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL CHECK (type IN ('DAILY', 'WEEKLY', 'MONTHLY', 'CUSTOM')),
    target_minutes INTEGER,
    target_sessions INTEGER,
    start_date DATE NOT NULL,
    end_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    progress JSONB DEFAULT '{}'::jsonb,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for productivity_goals
CREATE INDEX idx_goals_user ON productivity_goals(user_id);
CREATE INDEX idx_goals_active ON productivity_goals(user_id, is_active) WHERE is_active = TRUE;
CREATE INDEX idx_goals_dates ON productivity_goals(start_date, end_date);

CREATE TRIGGER update_productivity_goals_updated_at BEFORE UPDATE ON productivity_goals
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create weekly_summaries table for aggregated weekly data
CREATE TABLE weekly_summaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    week_start_date DATE NOT NULL,
    week_end_date DATE NOT NULL,
    total_minutes INTEGER DEFAULT 0,
    focus_minutes INTEGER DEFAULT 0,
    break_minutes INTEGER DEFAULT 0,
    sessions_count INTEGER DEFAULT 0,
    completed_sessions INTEGER DEFAULT 0,
    average_daily_minutes INTEGER,
    productivity_trend DECIMAL(3,2), -- e.g., +0.15 for 15% improvement
    most_productive_day INTEGER CHECK (most_productive_day >= 0 AND most_productive_day <= 6), -- 0=Sunday
    goals_achieved INTEGER DEFAULT 0,
    streak_days INTEGER DEFAULT 0,
    hives_activity JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, week_start_date)
);

-- Indexes for weekly_summaries
CREATE INDEX idx_weekly_summaries_user_date ON weekly_summaries(user_id, week_start_date DESC);
CREATE INDEX idx_weekly_summaries_date ON weekly_summaries(week_start_date);

CREATE TRIGGER update_weekly_summaries_updated_at BEFORE UPDATE ON weekly_summaries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create hive_analytics table for hive-level statistics
CREATE TABLE hive_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hive_id UUID NOT NULL,
    date DATE NOT NULL,
    active_users INTEGER DEFAULT 0,
    total_sessions INTEGER DEFAULT 0,
    total_minutes INTEGER DEFAULT 0,
    average_session_length INTEGER,
    peak_activity_hour INTEGER CHECK (peak_activity_hour >= 0 AND peak_activity_hour <= 23),
    engagement_score DECIMAL(3,2), -- 0.00 to 1.00
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(hive_id, date)
);

-- Indexes for hive_analytics
CREATE INDEX idx_hive_analytics_hive_date ON hive_analytics(hive_id, date DESC);
CREATE INDEX idx_hive_analytics_date ON hive_analytics(date);

CREATE TRIGGER update_hive_analytics_updated_at BEFORE UPDATE ON hive_analytics
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Function to update daily summary on session end
CREATE OR REPLACE FUNCTION update_daily_summary_on_session_end()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.end_time IS NOT NULL AND OLD.end_time IS NULL THEN
        -- Update or insert daily summary
        INSERT INTO daily_summaries (user_id, date, total_minutes, sessions_count, completed_sessions)
        VALUES (
            NEW.user_id,
            (NEW.start_time AT TIME ZONE 'UTC')::date,
            COALESCE(NEW.actual_duration_minutes, 0),
            1,
            CASE WHEN NEW.completed THEN 1 ELSE 0 END
        )
        ON CONFLICT (user_id, date) DO UPDATE SET
            total_minutes = daily_summaries.total_minutes + COALESCE(NEW.actual_duration_minutes, 0),
            sessions_count = daily_summaries.sessions_count + 1,
            completed_sessions = daily_summaries.completed_sessions + CASE WHEN NEW.completed THEN 1 ELSE 0 END,
            updated_at = CURRENT_TIMESTAMP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_daily_summary_trigger
AFTER UPDATE ON focus_sessions
FOR EACH ROW EXECUTE FUNCTION update_daily_summary_on_session_end();

-- Comments for documentation
COMMENT ON TABLE focus_sessions IS 'Individual focus/work sessions with detailed metrics';
COMMENT ON TABLE session_breaks IS 'Breaks taken during focus sessions';
COMMENT ON TABLE daily_summaries IS 'Daily aggregated productivity metrics per user';
COMMENT ON TABLE weekly_summaries IS 'Weekly aggregated productivity metrics and trends';
COMMENT ON TABLE user_achievements IS 'Unlocked achievements and badges for gamification';
COMMENT ON TABLE productivity_goals IS 'User-defined productivity goals and targets';
COMMENT ON TABLE hive_analytics IS 'Hive-level analytics and engagement metrics';

COMMENT ON COLUMN focus_sessions.productivity_score IS 'Self-reported productivity score from 0-100';
COMMENT ON COLUMN daily_summaries.streak_days IS 'Current consecutive days of productivity goals met';
COMMENT ON COLUMN weekly_summaries.productivity_trend IS 'Week-over-week productivity change as decimal';
COMMENT ON COLUMN hive_analytics.engagement_score IS 'Calculated engagement score from 0.00 to 1.00';