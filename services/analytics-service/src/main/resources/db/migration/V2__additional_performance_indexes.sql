-- V2__additional_performance_indexes.sql
-- Additional performance optimization indexes for Analytics Service
-- Enhances productivity tracking, reporting, and analytics query performance

-- Focus sessions advanced analytics indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_focus_sessions_productivity_analysis 
ON focus_sessions(user_id, type, completed, productivity_score DESC, start_time DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_focus_sessions_duration_analysis 
ON focus_sessions(user_id, target_duration_minutes, actual_duration_minutes, completed) 
WHERE completed = TRUE;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_focus_sessions_hive_analytics 
ON focus_sessions(hive_id, start_time DESC, completed, actual_duration_minutes) 
WHERE hive_id IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_focus_sessions_weekly_reports 
ON focus_sessions(user_id, start_time, end_time, actual_duration_minutes) 
WHERE start_time >= DATE_TRUNC('week', CURRENT_TIMESTAMP);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_focus_sessions_breaks_analysis 
ON focus_sessions(user_id, breaks_taken, distractions_logged, productivity_score);

-- Session breaks performance indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_session_breaks_duration_analysis 
ON session_breaks(session_id, start_time, duration_minutes, reason);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_session_breaks_patterns 
ON session_breaks(start_time, duration_minutes, reason) 
WHERE duration_minutes IS NOT NULL;

-- Daily summaries enhanced indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_daily_summaries_trends 
ON daily_summaries(user_id, date DESC, total_minutes, productivity_score);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_daily_summaries_streaks 
ON daily_summaries(user_id, streak_days DESC, date DESC, goals_achieved);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_daily_summaries_productivity_ranking 
ON daily_summaries(date, productivity_score DESC, total_minutes DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_daily_summaries_hive_activity 
ON daily_summaries(user_id, date DESC) 
WHERE hives_visited != '[]'::jsonb;

-- Weekly summaries analytics indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_weekly_summaries_trend_analysis 
ON weekly_summaries(user_id, week_start_date DESC, productivity_trend, total_minutes);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_weekly_summaries_performance 
ON weekly_summaries(week_start_date, average_daily_minutes DESC, goals_achieved DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_weekly_summaries_patterns 
ON weekly_summaries(user_id, most_productive_day, productivity_trend);

-- User achievements gamification indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_achievements_leaderboard 
ON user_achievements(achievement_type, points DESC, unlocked_at DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_achievements_user_progress 
ON user_achievements(user_id, unlocked_at DESC, points DESC, level DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_achievements_recent 
ON user_achievements(unlocked_at DESC, achievement_type, user_id) 
WHERE unlocked_at > CURRENT_TIMESTAMP - INTERVAL '30 days';

-- Productivity goals tracking indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_productivity_goals_active_tracking 
ON productivity_goals(user_id, is_active, start_date, end_date) 
WHERE is_active = TRUE;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_productivity_goals_completion_analysis 
ON productivity_goals(user_id, type, completed_at DESC, start_date);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_productivity_goals_current 
ON productivity_goals(user_id, type, is_active) 
WHERE is_active = TRUE AND (end_date IS NULL OR end_date >= CURRENT_DATE);

-- Hive analytics performance indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_hive_analytics_engagement 
ON hive_analytics(hive_id, date DESC, engagement_score DESC, active_users);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_hive_analytics_activity_trends 
ON hive_analytics(date, total_sessions DESC, total_minutes DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_hive_analytics_peak_times 
ON hive_analytics(hive_id, peak_activity_hour, date DESC);

-- Time-based composite indexes for reporting
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_focus_sessions_monthly_reports 
ON focus_sessions(user_id, DATE_TRUNC('month', start_time), completed, actual_duration_minutes);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_daily_summaries_monthly_aggregates 
ON daily_summaries(user_id, DATE_TRUNC('month', date), total_minutes, completed_sessions);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_weekly_summaries_quarterly 
ON weekly_summaries(user_id, DATE_TRUNC('quarter', week_start_date), total_minutes);

-- Cross-table analytics indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_focus_sessions_goal_correlation 
ON focus_sessions(user_id, start_time, actual_duration_minutes, productivity_score) 
WHERE completed = TRUE;

-- Performance optimization for real-time dashboard queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_focus_sessions_realtime_dashboard 
ON focus_sessions(user_id, start_time DESC, end_time, type) 
WHERE start_time > CURRENT_TIMESTAMP - INTERVAL '24 hours';

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_daily_summaries_current_week 
ON daily_summaries(user_id, date DESC, total_minutes, completed_sessions) 
WHERE date >= DATE_TRUNC('week', CURRENT_TIMESTAMP);

-- Partial indexes for filtered analytics
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_focus_sessions_successful 
ON focus_sessions(user_id, start_time DESC, actual_duration_minutes, productivity_score) 
WHERE completed = TRUE AND productivity_score >= 70;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_achievements_high_value 
ON user_achievements(user_id, unlocked_at DESC, points) 
WHERE points >= 100;

-- Indexes for data export and backup
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_focus_sessions_export 
ON focus_sessions(user_id, created_at, start_time, end_time);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_daily_summaries_export 
ON daily_summaries(user_id, date, created_at, updated_at);

-- Function-based indexes for advanced analytics
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_focus_sessions_efficiency 
ON focus_sessions((actual_duration_minutes::decimal / target_duration_minutes), user_id) 
WHERE completed = TRUE AND target_duration_minutes > 0;

-- JSON-based indexes for metadata and flexible analytics
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_focus_sessions_metadata_gin 
ON focus_sessions USING GIN(metadata) 
WHERE metadata != '{}'::jsonb;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_weekly_summaries_hive_activity_gin 
ON weekly_summaries USING GIN(hives_activity) 
WHERE hives_activity != '{}'::jsonb;

-- Cleanup and maintenance indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_focus_sessions_old_incomplete 
ON focus_sessions(start_time, user_id) 
WHERE completed = FALSE AND start_time < CURRENT_TIMESTAMP - INTERVAL '24 hours';

-- Analyze tables after creating indexes
ANALYZE focus_sessions;
ANALYZE session_breaks;
ANALYZE daily_summaries;
ANALYZE weekly_summaries;
ANALYZE user_achievements;
ANALYZE productivity_goals;
ANALYZE hive_analytics;

-- Comments for index purposes
COMMENT ON INDEX idx_focus_sessions_productivity_analysis IS 'Optimizes productivity analysis and user performance queries';
COMMENT ON INDEX idx_daily_summaries_trends IS 'Supports trend analysis and progress tracking dashboards';
COMMENT ON INDEX idx_user_achievements_leaderboard IS 'Accelerates leaderboard and gamification queries';
COMMENT ON INDEX idx_hive_analytics_engagement IS 'Optimizes hive engagement metrics and community analytics';
COMMENT ON INDEX idx_focus_sessions_monthly_reports IS 'Supports monthly productivity reporting and analysis';
COMMENT ON INDEX idx_productivity_goals_active_tracking IS 'Optimizes active goal tracking and progress monitoring';
COMMENT ON INDEX idx_focus_sessions_efficiency IS 'Enables efficiency ratio analysis and productivity insights';