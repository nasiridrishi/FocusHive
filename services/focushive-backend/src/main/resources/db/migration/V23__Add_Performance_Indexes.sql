-- Performance optimization indexes for FocusHive Backend
-- These indexes are designed to improve query performance for frequently accessed data

-- Users table indexes
CREATE INDEX IF NOT EXISTS idx_user_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_user_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_user_created_at ON users(created_at);
CREATE INDEX IF NOT EXISTS idx_user_deleted_at ON users(deleted_at);

-- Hives table indexes
CREATE INDEX IF NOT EXISTS idx_hive_owner_id ON hives(owner_id);
CREATE INDEX IF NOT EXISTS idx_hive_created_at ON hives(created_at);
CREATE INDEX IF NOT EXISTS idx_hive_is_active ON hives(is_active);
CREATE INDEX IF NOT EXISTS idx_hive_is_public ON hives(is_public);

-- Hive members indexes for join performance
CREATE INDEX IF NOT EXISTS idx_hive_member_hive_id ON hive_members(hive_id);
CREATE INDEX IF NOT EXISTS idx_hive_member_user_id ON hive_members(user_id);
CREATE INDEX IF NOT EXISTS idx_hive_member_joined_at ON hive_members(joined_at);
CREATE INDEX IF NOT EXISTS idx_hive_member_composite ON hive_members(hive_id, user_id);

-- Chat messages indexes for real-time performance
CREATE INDEX IF NOT EXISTS idx_chat_message_hive_id ON chat_messages(hive_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_sender_id ON chat_messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_created_at ON chat_messages(created_at);

-- Focus sessions indexes for analytics
CREATE INDEX IF NOT EXISTS idx_focus_session_user_id ON focus_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_focus_session_hive_id ON focus_sessions(hive_id);
CREATE INDEX IF NOT EXISTS idx_focus_session_start_time ON focus_sessions(start_time);
CREATE INDEX IF NOT EXISTS idx_focus_session_end_time ON focus_sessions(end_time);
CREATE INDEX IF NOT EXISTS idx_focus_session_status ON focus_sessions(status);

-- Productivity stats indexes
CREATE INDEX IF NOT EXISTS idx_productivity_stats_user_id ON productivity_stats(user_id);
CREATE INDEX IF NOT EXISTS idx_productivity_stats_date ON productivity_stats(date);
CREATE INDEX IF NOT EXISTS idx_productivity_stats_composite ON productivity_stats(user_id, date);

-- Notifications indexes
CREATE INDEX IF NOT EXISTS idx_notification_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_is_read ON notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_notification_created_at ON notifications(created_at);
CREATE INDEX IF NOT EXISTS idx_notification_unread_user ON notifications(user_id, is_read) WHERE is_read = false;

-- Forum posts indexes
CREATE INDEX IF NOT EXISTS idx_forum_post_category_id ON forum_posts(category_id);
CREATE INDEX IF NOT EXISTS idx_forum_post_author_id ON forum_posts(author_id);
CREATE INDEX IF NOT EXISTS idx_forum_post_created_at ON forum_posts(created_at);
CREATE INDEX IF NOT EXISTS idx_forum_post_is_locked ON forum_posts(is_locked);
CREATE INDEX IF NOT EXISTS idx_forum_post_is_pinned ON forum_posts(is_pinned);

-- Forum replies indexes
CREATE INDEX IF NOT EXISTS idx_forum_reply_post_id ON forum_replies(post_id);
CREATE INDEX IF NOT EXISTS idx_forum_reply_author_id ON forum_replies(author_id);
CREATE INDEX IF NOT EXISTS idx_forum_reply_created_at ON forum_replies(created_at);

-- Buddy relationships indexes
CREATE INDEX IF NOT EXISTS idx_buddy_relationship_requester_id ON buddy_relationships(requester_id);
CREATE INDEX IF NOT EXISTS idx_buddy_relationship_requested_id ON buddy_relationships(requested_id);
CREATE INDEX IF NOT EXISTS idx_buddy_relationship_status ON buddy_relationships(status);
CREATE INDEX IF NOT EXISTS idx_buddy_relationship_created_at ON buddy_relationships(created_at);

-- Audit logs indexes for monitoring
CREATE INDEX IF NOT EXISTS idx_audit_log_user_id ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_resource_type ON audit_logs(resource_type);

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_hive_member_active ON hive_members(hive_id, user_id) WHERE is_muted = false;
CREATE INDEX IF NOT EXISTS idx_chat_message_hive_recent ON chat_messages(hive_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_focus_session_user_recent ON focus_sessions(user_id, start_time DESC);

-- Performance comments
COMMENT ON INDEX idx_user_email IS 'Improves login and user lookup performance';
COMMENT ON INDEX idx_hive_member_composite IS 'Optimizes hive membership queries';
COMMENT ON INDEX idx_chat_message_hive_recent IS 'Accelerates chat message retrieval';
COMMENT ON INDEX idx_focus_session_user_recent IS 'Speeds up productivity analytics queries';