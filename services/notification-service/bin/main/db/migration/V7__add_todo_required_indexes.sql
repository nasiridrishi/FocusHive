-- V7__add_todo_required_indexes.sql
-- Performance indexes for notification service as required by TODO.md
-- These indexes are exactly as specified in TODO.md (lines 122-148)

-- Column mappings found:
-- notifications: type (not notification_type), is_read (not read), no status column
-- notification_preferences: notification_type (correct), user_id (correct)
-- notification_templates: notification_type (correct), language (correct)
-- security_audit_log: timestamp (not event_time), username (correct)

-- Note: Since notifications table has no 'status' column, we'll create indexes
-- for the columns that do exist to match the intent of TODO.md requirements

-- 1. idx_notification_user_created
-- TODO.md: ON notifications(user_id, created_at DESC)
CREATE INDEX IF NOT EXISTS idx_notification_user_created
  ON notifications(user_id, created_at DESC);

-- 2. idx_notification_type_status  
-- TODO.md: ON notifications(notification_type, status)
-- Mapping: notification_type -> type, but no status column exists
-- Creating equivalent index for type-based queries
CREATE INDEX IF NOT EXISTS idx_notification_type_created
  ON notifications(type, created_at DESC);

-- 3. idx_notification_read_status
-- TODO.md: ON notifications(user_id, is_read, created_at DESC) 
-- Mapping: is_read exists, this matches exactly
CREATE INDEX IF NOT EXISTS idx_notification_read_status
  ON notifications(user_id, is_read, created_at DESC);

-- 4. idx_preference_user_type
-- TODO.md: ON notification_preferences(user_id, notification_type)
-- Mapping: Both columns exist exactly as specified
CREATE INDEX IF NOT EXISTS idx_preference_user_type
  ON notification_preferences(user_id, notification_type);

-- 5. idx_template_type_lang
-- TODO.md: ON notification_templates(notification_type, language)
-- Mapping: Both columns exist exactly as specified
CREATE INDEX IF NOT EXISTS idx_template_type_lang
  ON notification_templates(notification_type, language);

-- 6. idx_audit_log_timestamp
-- TODO.md: ON security_audit_logs(timestamp DESC)
-- Mapping: timestamp column exists exactly as specified
CREATE INDEX IF NOT EXISTS idx_audit_log_timestamp
  ON security_audit_log(timestamp DESC);

-- 7. idx_audit_log_user
-- TODO.md: ON security_audit_logs(username, timestamp DESC)
-- Mapping: Both columns exist exactly as specified
CREATE INDEX IF NOT EXISTS idx_audit_log_user
  ON security_audit_log(username, timestamp DESC);

-- Analyze table statistics as required by TODO.md
ANALYZE notifications;
ANALYZE notification_preferences;
ANALYZE notification_templates;
ANALYZE security_audit_log;

-- Add comments for documentation
COMMENT ON INDEX idx_notification_user_created IS 'TODO.md required: Primary query pattern for user notifications timeline';
COMMENT ON INDEX idx_notification_type_created IS 'TODO.md required: Adapted from type/status to type/created_at (no status column exists)';
COMMENT ON INDEX idx_notification_read_status IS 'TODO.md required: Optimized for user unread notification queries';
COMMENT ON INDEX idx_preference_user_type IS 'TODO.md required: User preference lookup by notification type';
COMMENT ON INDEX idx_template_type_lang IS 'TODO.md required: Template lookup by type and language';
COMMENT ON INDEX idx_audit_log_timestamp IS 'TODO.md required: Audit log chronological queries';
COMMENT ON INDEX idx_audit_log_user IS 'TODO.md required: Audit log user activity queries';