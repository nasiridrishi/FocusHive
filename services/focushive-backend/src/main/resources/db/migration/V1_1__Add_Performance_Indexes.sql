-- Performance optimization indexes for FocusHive Backend
-- These indexes are designed to improve query performance for frequently accessed data

-- User table indexes
CREATE INDEX IF NOT EXISTS idx_user_email ON user(email);
CREATE INDEX IF NOT EXISTS idx_user_username ON user(username);
CREATE INDEX IF NOT EXISTS idx_user_created_at ON user(created_at);
CREATE INDEX IF NOT EXISTS idx_user_deleted_at ON user(deleted_at);

-- Hive table indexes
CREATE INDEX IF NOT EXISTS idx_hive_owner_id ON hive(owner_id);
CREATE INDEX IF NOT EXISTS idx_hive_created_at ON hive(created_at);
CREATE INDEX IF NOT EXISTS idx_hive_is_active ON hive(is_active);
CREATE INDEX IF NOT EXISTS idx_hive_is_private ON hive(is_private);

-- Hive member indexes for join performance
CREATE INDEX IF NOT EXISTS idx_hive_member_hive_id ON hive_member(hive_id);
CREATE INDEX IF NOT EXISTS idx_hive_member_user_id ON hive_member(user_id);
CREATE INDEX IF NOT EXISTS idx_hive_member_joined_at ON hive_member(joined_at);
CREATE INDEX IF NOT EXISTS idx_hive_member_composite ON hive_member(hive_id, user_id);

-- Chat message indexes for real-time performance
CREATE INDEX IF NOT EXISTS idx_chat_message_hive_id ON chat_message(hive_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_sender_id ON chat_message(sender_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_created_at ON chat_message(created_at);
CREATE INDEX IF NOT EXISTS idx_chat_message_thread_id ON chat_message(thread_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_is_pinned ON chat_message(is_pinned);

-- Focus session indexes for analytics
CREATE INDEX IF NOT EXISTS idx_focus_session_user_id ON focus_session(user_id);
CREATE INDEX IF NOT EXISTS idx_focus_session_hive_id ON focus_session(hive_id);
CREATE INDEX IF NOT EXISTS idx_focus_session_start_time ON focus_session(start_time);
CREATE INDEX IF NOT EXISTS idx_focus_session_end_time ON focus_session(end_time);
CREATE INDEX IF NOT EXISTS idx_focus_session_status ON focus_session(status);

-- Productivity metrics indexes
CREATE INDEX IF NOT EXISTS idx_productivity_metric_user_id ON productivity_metric(user_id);
CREATE INDEX IF NOT EXISTS idx_productivity_metric_date ON productivity_metric(date);
CREATE INDEX IF NOT EXISTS idx_productivity_metric_composite ON productivity_metric(user_id, date);

-- Hive analytics indexes
CREATE INDEX IF NOT EXISTS idx_hive_analytics_hive_id ON hive_analytics(hive_id);
CREATE INDEX IF NOT EXISTS idx_hive_analytics_date ON hive_analytics(date);
CREATE INDEX IF NOT EXISTS idx_hive_analytics_composite ON hive_analytics(hive_id, date);

-- Notification indexes
CREATE INDEX IF NOT EXISTS idx_notification_user_id ON notification(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_is_read ON notification(is_read);
CREATE INDEX IF NOT EXISTS idx_notification_created_at ON notification(created_at);
CREATE INDEX IF NOT EXISTS idx_notification_unread_user ON notification(user_id, is_read) WHERE is_read = false;

-- Forum post indexes
CREATE INDEX IF NOT EXISTS idx_forum_post_category_id ON forum_post(category_id);
CREATE INDEX IF NOT EXISTS idx_forum_post_author_id ON forum_post(author_id);
CREATE INDEX IF NOT EXISTS idx_forum_post_created_at ON forum_post(created_at);
CREATE INDEX IF NOT EXISTS idx_forum_post_is_locked ON forum_post(is_locked);
CREATE INDEX IF NOT EXISTS idx_forum_post_is_pinned ON forum_post(is_pinned);

-- Forum reply indexes
CREATE INDEX IF NOT EXISTS idx_forum_reply_post_id ON forum_reply(post_id);
CREATE INDEX IF NOT EXISTS idx_forum_reply_author_id ON forum_reply(author_id);
CREATE INDEX IF NOT EXISTS idx_forum_reply_created_at ON forum_reply(created_at);

-- Buddy relationship indexes
CREATE INDEX IF NOT EXISTS idx_buddy_relationship_requester_id ON buddy_relationship(requester_id);
CREATE INDEX IF NOT EXISTS idx_buddy_relationship_requested_id ON buddy_relationship(requested_id);
CREATE INDEX IF NOT EXISTS idx_buddy_relationship_status ON buddy_relationship(status);
CREATE INDEX IF NOT EXISTS idx_buddy_relationship_created_at ON buddy_relationship(created_at);

-- Achievement progress indexes
CREATE INDEX IF NOT EXISTS idx_achievement_progress_user_id ON achievement_progress(user_id);
CREATE INDEX IF NOT EXISTS idx_achievement_progress_achievement_type ON achievement_progress(achievement_type);
CREATE INDEX IF NOT EXISTS idx_achievement_progress_unlocked_at ON achievement_progress(unlocked_at);

-- Audit log indexes for monitoring
CREATE INDEX IF NOT EXISTS idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_action ON audit_log(action);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_resource_type ON audit_log(resource_type);

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_hive_member_active_user ON hive_member(hive_id, user_id) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_chat_message_hive_recent ON chat_message(hive_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_focus_session_user_recent ON focus_session(user_id, start_time DESC);

-- Performance comment
COMMENT ON INDEX idx_user_email IS 'Improves login and user lookup performance';
COMMENT ON INDEX idx_hive_member_composite IS 'Optimizes hive membership queries';
COMMENT ON INDEX idx_chat_message_hive_recent IS 'Accelerates chat message retrieval';
COMMENT ON INDEX idx_focus_session_user_recent IS 'Speeds up productivity analytics queries';