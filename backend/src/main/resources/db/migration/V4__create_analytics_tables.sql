-- Create focus_sessions table
CREATE TABLE focus_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    hive_id UUID REFERENCES hives(id) ON DELETE SET NULL,
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
CREATE INDEX idx_sessions_date ON focus_sessions(DATE(start_time));

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
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
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
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
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
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
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

-- Function to update daily summary
CREATE OR REPLACE FUNCTION update_daily_summary_on_session_end()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.end_time IS NOT NULL AND OLD.end_time IS NULL THEN
        -- Update or insert daily summary
        INSERT INTO daily_summaries (user_id, date, total_minutes, sessions_count, completed_sessions)
        VALUES (
            NEW.user_id,
            DATE(NEW.start_time),
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