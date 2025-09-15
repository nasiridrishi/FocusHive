-- =====================================================
-- Analytics Service Database Migration
-- Version: V20__create_analytics_tables.sql
-- Description: Create comprehensive analytics tables for productivity tracking,
--              achievements, goals, streaks, and hive analytics
-- =====================================================

-- =====================================================
-- 1. PRODUCTIVITY METRICS TABLE
-- =====================================================
CREATE TABLE productivity_metrics (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    focus_minutes INTEGER NOT NULL DEFAULT 0 CHECK (focus_minutes >= 0),
    completed_sessions INTEGER NOT NULL DEFAULT 0 CHECK (completed_sessions >= 0),
    productivity_score INTEGER NOT NULL DEFAULT 0 CHECK (productivity_score >= 0 AND productivity_score <= 1000),
    total_sessions INTEGER DEFAULT 0 CHECK (total_sessions >= 0),
    break_minutes INTEGER DEFAULT 0 CHECK (break_minutes >= 0),
    distractions_count INTEGER DEFAULT 0 CHECK (distractions_count >= 0),
    average_session_length INTEGER DEFAULT 0 CHECK (average_session_length >= 0),
    peak_performance_hour INTEGER CHECK (peak_performance_hour >= 0 AND peak_performance_hour <= 23),
    goals_achieved INTEGER DEFAULT 0 CHECK (goals_achieved >= 0),
    streak_bonus INTEGER DEFAULT 0 CHECK (streak_bonus >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_productivity_user_date UNIQUE (user_id, date)
);

-- Indexes for productivity metrics
CREATE INDEX idx_productivity_user_date ON productivity_metrics (user_id, date);
CREATE INDEX idx_productivity_date ON productivity_metrics (date);
CREATE INDEX idx_productivity_score ON productivity_metrics (productivity_score);
CREATE INDEX idx_productivity_user_id ON productivity_metrics (user_id);

-- =====================================================
-- 2. HIVE ANALYTICS TABLE
-- =====================================================
CREATE TABLE hive_analytics (
    id VARCHAR(255) PRIMARY KEY,
    hive_id VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    active_users INTEGER NOT NULL DEFAULT 0 CHECK (active_users >= 0),
    total_focus_time INTEGER NOT NULL DEFAULT 0 CHECK (total_focus_time >= 0),
    total_sessions INTEGER NOT NULL DEFAULT 0 CHECK (total_sessions >= 0),
    completed_sessions INTEGER NOT NULL DEFAULT 0 CHECK (completed_sessions >= 0),
    average_productivity_score INTEGER DEFAULT 0 CHECK (average_productivity_score >= 0 AND average_productivity_score <= 1000),
    peak_concurrent_users INTEGER DEFAULT 0 CHECK (peak_concurrent_users >= 0),
    total_break_time INTEGER DEFAULT 0 CHECK (total_break_time >= 0),
    total_distractions INTEGER DEFAULT 0 CHECK (total_distractions >= 0),
    total_goals_achieved INTEGER DEFAULT 0 CHECK (total_goals_achieved >= 0),
    most_productive_hour INTEGER CHECK (most_productive_hour >= 0 AND most_productive_hour <= 23),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_hive_analytics_hive_date UNIQUE (hive_id, date)
);

-- Indexes for hive analytics
CREATE INDEX idx_hive_analytics_hive_date ON hive_analytics (hive_id, date);
CREATE INDEX idx_hive_analytics_date ON hive_analytics (date);
CREATE INDEX idx_hive_analytics_active_users ON hive_analytics (active_users);
CREATE INDEX idx_hive_analytics_hive_id ON hive_analytics (hive_id);

-- =====================================================
-- 3. USER STREAKS TABLE
-- =====================================================
CREATE TABLE user_streaks (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    current_streak INTEGER NOT NULL DEFAULT 0 CHECK (current_streak >= 0),
    longest_streak INTEGER NOT NULL DEFAULT 0 CHECK (longest_streak >= 0),
    last_active_date DATE NOT NULL,
    streak_start_date DATE NOT NULL,
    total_active_days INTEGER DEFAULT 0 CHECK (total_active_days >= 0),
    streak_freezes_used INTEGER DEFAULT 0 CHECK (streak_freezes_used >= 0),
    available_streak_freezes INTEGER DEFAULT 2 CHECK (available_streak_freezes >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for user streaks
CREATE INDEX idx_user_streaks_user_id ON user_streaks (user_id);
CREATE INDEX idx_user_streaks_current_streak ON user_streaks (current_streak);
CREATE INDEX idx_user_streaks_longest_streak ON user_streaks (longest_streak);
CREATE INDEX idx_user_streaks_last_active ON user_streaks (last_active_date);

-- =====================================================
-- 4. ACHIEVEMENT PROGRESS TABLE
-- =====================================================
CREATE TABLE achievement_progress (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    achievement_type VARCHAR(50) NOT NULL,
    progress INTEGER NOT NULL DEFAULT 0 CHECK (progress >= 0 AND progress <= 100),
    current_value INTEGER DEFAULT 0 CHECK (current_value >= 0),
    unlocked_at TIMESTAMP,
    first_progress_at TIMESTAMP,
    notification_sent BOOLEAN DEFAULT FALSE,
    notes VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_achievement_user_type UNIQUE (user_id, achievement_type)
);

-- Indexes for achievement progress
CREATE INDEX idx_achievement_user_id ON achievement_progress (user_id);
CREATE INDEX idx_achievement_type ON achievement_progress (achievement_type);
CREATE INDEX idx_achievement_unlocked ON achievement_progress (unlocked_at);
CREATE INDEX idx_achievement_progress ON achievement_progress (progress);
CREATE INDEX idx_achievement_notification_pending ON achievement_progress (unlocked_at, notification_sent)
    WHERE unlocked_at IS NOT NULL AND notification_sent = FALSE;

-- =====================================================
-- 5. DAILY GOALS TABLE
-- =====================================================
CREATE TABLE daily_goals (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    target_minutes INTEGER NOT NULL CHECK (target_minutes >= 1 AND target_minutes <= 1440),
    completed_minutes INTEGER NOT NULL DEFAULT 0 CHECK (completed_minutes >= 0),
    achieved BOOLEAN DEFAULT FALSE,
    achieved_at TIMESTAMP,
    description VARCHAR(200),
    priority VARCHAR(50) DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    reminder_sent BOOLEAN DEFAULT FALSE,
    streak_contribution BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_daily_goals_user_date UNIQUE (user_id, date)
);

-- Indexes for daily goals
CREATE INDEX idx_daily_goals_user_date ON daily_goals (user_id, date);
CREATE INDEX idx_daily_goals_date ON daily_goals (date);
CREATE INDEX idx_daily_goals_achieved ON daily_goals (achieved);
CREATE INDEX idx_daily_goals_user_id ON daily_goals (user_id);
CREATE INDEX idx_daily_goals_reminders ON daily_goals (date, achieved, reminder_sent)
    WHERE achieved = FALSE AND reminder_sent = FALSE;

-- =====================================================
-- 6. ANALYTICS AGGREGATION TABLES
-- =====================================================

-- Weekly aggregation table for performance optimization
CREATE TABLE weekly_analytics (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    week_start_date DATE NOT NULL,
    week_end_date DATE NOT NULL,
    total_focus_minutes INTEGER DEFAULT 0,
    total_completed_sessions INTEGER DEFAULT 0,
    average_productivity_score INTEGER DEFAULT 0,
    goals_achieved INTEGER DEFAULT 0,
    streak_maintained BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_weekly_analytics_user_week UNIQUE (user_id, week_start_date)
);

CREATE INDEX idx_weekly_analytics_user_week ON weekly_analytics (user_id, week_start_date);
CREATE INDEX idx_weekly_analytics_week ON weekly_analytics (week_start_date);

-- Monthly aggregation table for long-term trends
CREATE TABLE monthly_analytics (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL CHECK (month >= 1 AND month <= 12),
    total_focus_minutes INTEGER DEFAULT 0,
    total_completed_sessions INTEGER DEFAULT 0,
    average_productivity_score INTEGER DEFAULT 0,
    goals_achieved INTEGER DEFAULT 0,
    achievements_unlocked INTEGER DEFAULT 0,
    best_streak INTEGER DEFAULT 0,
    active_days INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_monthly_analytics_user_month UNIQUE (user_id, year, month)
);

CREATE INDEX idx_monthly_analytics_user_year_month ON monthly_analytics (user_id, year, month);
CREATE INDEX idx_monthly_analytics_year_month ON monthly_analytics (year, month);

-- =====================================================
-- 7. ACHIEVEMENT DEFINITIONS TABLE (for reference)
-- =====================================================
CREATE TABLE achievement_definitions (
    achievement_type VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    category VARCHAR(50) NOT NULL,
    target_value INTEGER NOT NULL,
    points INTEGER NOT NULL,
    difficulty VARCHAR(20) DEFAULT 'MEDIUM' CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD', 'LEGENDARY')),
    rarity VARCHAR(20) DEFAULT 'COMMON' CHECK (rarity IN ('COMMON', 'UNCOMMON', 'RARE', 'EPIC', 'LEGENDARY')),
    icon VARCHAR(100),
    unlock_criteria TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert achievement definitions
INSERT INTO achievement_definitions (achievement_type, name, description, category, target_value, points, difficulty, rarity, unlock_criteria) VALUES
-- Getting Started Achievements
('FIRST_FOCUS', 'First Focus', 'Complete your first focus session', 'Getting Started', 1, 10, 'EASY', 'COMMON', 'Complete any focus session'),
('EARLY_BIRD', 'Early Bird', 'Complete a session before 7 AM', 'Getting Started', 1, 10, 'EASY', 'COMMON', 'Complete a focus session starting before 7:00 AM'),
('NIGHT_OWL', 'Night Owl', 'Complete a session after 10 PM', 'Getting Started', 1, 10, 'EASY', 'COMMON', 'Complete a focus session starting after 10:00 PM'),

-- Session Milestones
('TEN_SESSIONS', 'Decade', 'Complete 10 focus sessions', 'Session Milestones', 10, 25, 'EASY', 'COMMON', 'Complete 10 total focus sessions'),
('FIFTY_SESSIONS', 'Half Century', 'Complete 50 focus sessions', 'Session Milestones', 50, 50, 'MEDIUM', 'UNCOMMON', 'Complete 50 total focus sessions'),
('HUNDRED_SESSIONS', 'Century', 'Complete 100 focus sessions', 'Session Milestones', 100, 100, 'HARD', 'RARE', 'Complete 100 total focus sessions'),

-- Consistency Achievements
('THREE_DAY_STREAK', 'Consistency Starter', 'Maintain a 3-day focus streak', 'Consistency', 3, 25, 'EASY', 'COMMON', 'Complete focus sessions for 3 consecutive days'),
('WEEK_WARRIOR', 'Week Warrior', 'Maintain a 7-day focus streak', 'Consistency', 7, 50, 'MEDIUM', 'UNCOMMON', 'Complete focus sessions for 7 consecutive days'),
('MONTH_MASTER', 'Month Master', 'Maintain a 30-day focus streak', 'Consistency', 30, 100, 'HARD', 'RARE', 'Complete focus sessions for 30 consecutive days'),
('CENTURY_STREAK', 'Century Streak', 'Maintain a 100-day focus streak', 'Consistency', 100, 200, 'LEGENDARY', 'LEGENDARY', 'Complete focus sessions for 100 consecutive days'),

-- Endurance Achievements
('MARATHON_RUNNER', 'Marathon Runner', 'Complete a 3-hour focus session', 'Endurance', 180, 50, 'MEDIUM', 'UNCOMMON', 'Complete a single focus session of 180+ minutes'),
('ULTRA_RUNNER', 'Ultra Runner', 'Complete a 5-hour focus session', 'Endurance', 300, 100, 'HARD', 'RARE', 'Complete a single focus session of 300+ minutes'),
('ENDURANCE_MASTER', 'Endurance Master', 'Complete a 8-hour focus session', 'Endurance', 480, 200, 'LEGENDARY', 'LEGENDARY', 'Complete a single focus session of 480+ minutes'),

-- Performance Achievements
('HIGH_PERFORMER', 'High Performer', 'Achieve productivity score of 90+', 'Performance', 90, 50, 'MEDIUM', 'UNCOMMON', 'Achieve a daily productivity score of 90 or higher'),
('PEAK_PERFORMER', 'Peak Performer', 'Achieve productivity score of 95+', 'Performance', 95, 100, 'HARD', 'RARE', 'Achieve a daily productivity score of 95 or higher'),
('PERFECT_SCORE', 'Perfect Score', 'Achieve productivity score of 100', 'Performance', 100, 200, 'LEGENDARY', 'LEGENDARY', 'Achieve a daily productivity score of 100'),

-- Social Achievements
('TEAM_PLAYER', 'Team Player', 'Complete 10 sessions in hives', 'Social', 10, 50, 'MEDIUM', 'UNCOMMON', 'Complete 10 focus sessions within hive environments'),
('HIVE_LEADER', 'Hive Leader', 'Lead productivity in a hive for 7 days', 'Social', 7, 100, 'HARD', 'RARE', 'Be the top performer in a hive for 7 consecutive days'),
('SOCIAL_BUTTERFLY', 'Social Butterfly', 'Be active in 5 different hives', 'Social', 5, 200, 'LEGENDARY', 'LEGENDARY', 'Complete focus sessions in 5 different hives'),

-- Special Achievements
('DISTRACTION_FREE', 'Distraction Free', 'Complete 10 sessions with zero distractions', 'Special', 10, 75, 'HARD', 'RARE', 'Complete 10 focus sessions with 0 recorded distractions'),
('GOAL_CRUSHER', 'Goal Crusher', 'Achieve daily goals for 30 consecutive days', 'Special', 30, 75, 'HARD', 'RARE', 'Meet or exceed daily goals for 30 consecutive days'),
('WEEKEND_WARRIOR', 'Weekend Warrior', 'Complete sessions on 10 weekends', 'Special', 10, 75, 'MEDIUM', 'RARE', 'Complete focus sessions on 10 different weekends');

-- =====================================================
-- 8. ANALYTICS VIEWS FOR COMMON QUERIES
-- =====================================================

-- User productivity overview view
CREATE VIEW user_productivity_overview AS
SELECT
    pm.user_id,
    COUNT(pm.id) as total_tracking_days,
    SUM(pm.focus_minutes) as total_focus_minutes,
    SUM(pm.completed_sessions) as total_completed_sessions,
    AVG(pm.productivity_score) as average_productivity_score,
    MAX(pm.productivity_score) as best_productivity_score,
    us.current_streak,
    us.longest_streak,
    (SELECT COUNT(*) FROM achievement_progress ap
     WHERE ap.user_id = pm.user_id AND ap.unlocked_at IS NOT NULL) as achievements_unlocked,
    (SELECT COUNT(*) FROM daily_goals dg
     WHERE dg.user_id = pm.user_id AND dg.achieved = TRUE) as goals_achieved
FROM productivity_metrics pm
LEFT JOIN user_streaks us ON us.user_id = pm.user_id
GROUP BY pm.user_id, us.current_streak, us.longest_streak;

-- Daily platform statistics view
CREATE VIEW daily_platform_stats AS
SELECT
    pm.date,
    COUNT(DISTINCT pm.user_id) as active_users,
    SUM(pm.focus_minutes) as total_focus_minutes,
    SUM(pm.completed_sessions) as total_completed_sessions,
    AVG(pm.productivity_score) as average_productivity_score,
    COUNT(CASE WHEN dg.achieved = TRUE THEN 1 END) as goals_achieved,
    COUNT(CASE WHEN ap.unlocked_at::date = pm.date THEN 1 END) as achievements_unlocked
FROM productivity_metrics pm
LEFT JOIN daily_goals dg ON dg.user_id = pm.user_id AND dg.date = pm.date
LEFT JOIN achievement_progress ap ON ap.user_id = pm.user_id
GROUP BY pm.date
ORDER BY pm.date DESC;

-- Hive leaderboard view
CREATE VIEW hive_leaderboard AS
SELECT
    ha.hive_id,
    ha.date,
    ha.active_users,
    ha.total_focus_time,
    ha.average_productivity_score,
    RANK() OVER (PARTITION BY ha.date ORDER BY ha.total_focus_time DESC) as focus_time_rank,
    RANK() OVER (PARTITION BY ha.date ORDER BY ha.average_productivity_score DESC) as productivity_rank,
    RANK() OVER (PARTITION BY ha.date ORDER BY ha.active_users DESC) as activity_rank
FROM hive_analytics ha
WHERE ha.date >= CURRENT_DATE - INTERVAL '30 days';

-- =====================================================
-- 9. TRIGGERS FOR AUTOMATIC UPDATES
-- =====================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply triggers to all analytics tables
CREATE TRIGGER update_productivity_metrics_updated_at BEFORE UPDATE ON productivity_metrics FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_hive_analytics_updated_at BEFORE UPDATE ON hive_analytics FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_user_streaks_updated_at BEFORE UPDATE ON user_streaks FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_achievement_progress_updated_at BEFORE UPDATE ON achievement_progress FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_daily_goals_updated_at BEFORE UPDATE ON daily_goals FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- 10. DATA RETENTION POLICIES (Comments for future implementation)
-- =====================================================

-- Note: The following are policy suggestions for data retention
-- Implement these as scheduled jobs or procedures:

-- 1. Archive productivity_metrics older than 2 years
-- 2. Archive hive_analytics older than 1 year
-- 3. Keep user_streaks indefinitely (small table)
-- 4. Keep achievement_progress indefinitely (valuable user data)
-- 5. Archive daily_goals older than 1 year
-- 6. Archive weekly_analytics older than 3 years
-- 7. Archive monthly_analytics older than 5 years

-- =====================================================
-- 11. INITIAL DATA SETUP (Optional)
-- =====================================================

-- Create sample data or default configurations if needed
-- This section can be populated based on application requirements

COMMIT;