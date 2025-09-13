-- V21__additional_performance_indexes.sql
-- Additional performance optimization indexes for FocusHive Backend
-- Complements existing V10 indexes with notification, audit, and advanced analytics

-- Notification system performance indexes (V9 tables)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_notification_preferences_user_enabled 
ON notification_preferences(user_id, notification_type, in_app_enabled, email_enabled) 
WHERE in_app_enabled = TRUE OR email_enabled = TRUE OR push_enabled = TRUE;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_notification_preferences_quiet_hours 
ON notification_preferences(user_id, quiet_start_time, quiet_end_time) 
WHERE quiet_start_time IS NOT NULL AND quiet_end_time IS NOT NULL;

-- Audit system performance indexes (V13 tables)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_logs_user_entity 
ON audit_logs(user_id, entity_type, entity_id, created_at DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_logs_compliance 
ON audit_logs(entity_type, action, created_at DESC, user_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_logs_security 
ON audit_logs(ip_address, user_id, action, created_at DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_system_events_monitoring 
ON system_events(severity, event_type, created_at DESC, source);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_sessions_security 
ON user_sessions(user_id, created_at DESC, expires_at, is_active) 
WHERE is_active = TRUE;

-- Advanced hive management indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_hives_activity_status 
ON hives(owner_id, type, created_at DESC) 
WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '30 days';

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_hive_members_activity_tracking 
ON hive_members(user_id, joined_at DESC, role) 
WHERE joined_at > CURRENT_TIMESTAMP - INTERVAL '90 days';

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_hive_members_role_management 
ON hive_members(hive_id, role, joined_at DESC);

-- Enhanced user activity and engagement indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_engagement_metrics 
ON users(enabled, created_at DESC, last_login_at DESC) 
WHERE enabled = TRUE;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_security_monitoring 
ON users(failed_login_attempts, account_locked_until, last_login_at) 
WHERE failed_login_attempts > 0 OR account_locked_until IS NOT NULL;

-- Focus session advanced analytics (complementing existing indexes)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_focus_sessions_hive_performance 
ON focus_sessions(hive_id, start_time DESC, completed, actual_duration_minutes) 
WHERE hive_id IS NOT NULL AND completed = TRUE;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_focus_sessions_user_trends 
ON focus_sessions(user_id, DATE_TRUNC('week', start_time), completed, actual_duration_minutes);

-- Buddy system advanced indexes (complementing V10)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_buddy_relationships_active_lookup 
ON buddy_relationships(user1_id, status, created_at DESC) 
WHERE status = 'ACCEPTED';

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_buddy_sessions_performance 
ON buddy_sessions(relationship_id, start_time DESC, end_time, total_minutes);

-- Communication and chat advanced indexes (V12 tables)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_messages_hive_activity 
ON chat_messages(hive_id, created_at DESC, sender_id) 
WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '24 hours';

-- Daily summaries enhanced performance (complementing existing)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_daily_summaries_productivity_trends 
ON daily_summaries(user_id, date DESC, productivity_score, total_minutes) 
WHERE productivity_score IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_daily_summaries_goals_tracking 
ON daily_summaries(user_id, goals_achieved, streak_days, date DESC);

-- Notification delivery performance (if notifications table exists)
-- Note: Adding conditional indexes based on common notification patterns

-- Time-based partitioning support indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_logs_partition_maintenance 
ON audit_logs(created_at) 
WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '6 months';

-- Cross-service integration indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_external_integration 
ON users(id, email, enabled, created_at) 
WHERE enabled = TRUE;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_hives_external_analytics 
ON hives(id, owner_id, type, created_at);

-- Performance indexes for bulk operations and reporting
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_focus_sessions_bulk_reporting 
ON focus_sessions(start_time, user_id, hive_id, completed, actual_duration_minutes);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_hive_members_bulk_analytics 
ON hive_members(hive_id, user_id, joined_at, role);

-- Real-time dashboard optimization indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_active_dashboard 
ON users(enabled, last_login_at DESC) 
WHERE enabled = TRUE AND last_login_at > CURRENT_TIMESTAMP - INTERVAL '7 days';

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_hives_active_dashboard 
ON hives(created_at DESC, owner_id, type) 
WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '30 days';

-- Security and compliance advanced indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_logs_gdpr_compliance 
ON audit_logs(user_id, entity_type, created_at, action) 
WHERE entity_type IN ('user', 'persona', 'privacy_setting');

-- Function-based indexes for advanced queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_focus_sessions_efficiency_ratio 
ON focus_sessions((actual_duration_minutes::decimal / target_duration_minutes), user_id) 
WHERE completed = TRUE AND target_duration_minutes > 0;

-- JSON-based indexes for flexible metadata queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_system_events_data_gin 
ON system_events USING GIN(data) 
WHERE data IS NOT NULL;

-- Cleanup and maintenance indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_sessions_cleanup 
ON user_sessions(expires_at, is_active) 
WHERE expires_at < CURRENT_TIMESTAMP;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_focus_sessions_incomplete_cleanup 
ON focus_sessions(start_time, completed) 
WHERE completed = FALSE AND start_time < CURRENT_TIMESTAMP - INTERVAL '48 hours';

-- Analyze tables after creating indexes
ANALYZE users;
ANALYZE hives;
ANALYZE hive_members;
ANALYZE focus_sessions;
ANALYZE buddy_relationships;
ANALYZE buddy_sessions;
ANALYZE daily_summaries;
ANALYZE chat_messages;
ANALYZE notification_preferences;
ANALYZE audit_logs;
ANALYZE system_events;
ANALYZE user_sessions;

-- Comments for index purposes
COMMENT ON INDEX idx_notification_preferences_user_enabled IS 'Optimizes notification delivery and user preference queries';
COMMENT ON INDEX idx_audit_logs_user_entity IS 'Supports comprehensive audit trail and compliance reporting';
COMMENT ON INDEX idx_hives_activity_status IS 'Accelerates active hive discovery and analytics queries';
COMMENT ON INDEX idx_focus_sessions_hive_performance IS 'Optimizes hive-level productivity analytics and insights';
COMMENT ON INDEX idx_users_engagement_metrics IS 'Supports user engagement analysis and retention metrics';
COMMENT ON INDEX idx_buddy_relationships_active_lookup IS 'Optimizes buddy system queries and relationship management';
COMMENT ON INDEX idx_focus_sessions_efficiency_ratio IS 'Enables advanced productivity efficiency analysis';