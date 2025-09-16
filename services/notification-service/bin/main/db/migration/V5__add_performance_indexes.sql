-- V5__add_performance_indexes.sql
-- Performance indexes for notification service as per TODO.md requirements
-- Target: Query response time <100ms for 90% of queries

-- Notification table indexes
CREATE INDEX IF NOT EXISTS idx_notifications_user_id_created_at
ON notifications(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_read_status_user
ON notifications(is_read, user_id);

CREATE INDEX IF NOT EXISTS idx_notifications_type_user
ON notifications(type, user_id);

-- Compound index for common query pattern
CREATE INDEX IF NOT EXISTS idx_notifications_user_read_created
ON notifications(user_id, is_read, created_at DESC);

-- Notification preferences indexes
CREATE INDEX IF NOT EXISTS idx_preferences_user_id
ON notification_preferences(user_id);

-- Template indexes
CREATE INDEX IF NOT EXISTS idx_templates_type
ON notification_templates(notification_type);

CREATE INDEX IF NOT EXISTS idx_templates_language
ON notification_templates(language);

-- Note: dead_letter_messages table and indexes are created in V6__create_dead_letter_messages_table.sql

-- Security audit log indexes (if table exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_name = 'security_audit_log') THEN
        CREATE INDEX IF NOT EXISTS idx_audit_timestamp
        ON security_audit_log(timestamp DESC);

        CREATE INDEX IF NOT EXISTS idx_audit_action
        ON security_audit_log(action);

        CREATE INDEX IF NOT EXISTS idx_audit_username
        ON security_audit_log(username);

        CREATE INDEX IF NOT EXISTS idx_audit_ip_address
        ON security_audit_log(ip_address);

        CREATE INDEX IF NOT EXISTS idx_audit_session_id
        ON security_audit_log(session_id);
    END IF;
END $$;

-- Email delivery tracking indexes
CREATE TABLE IF NOT EXISTS email_delivery_tracking (
    id BIGSERIAL PRIMARY KEY,
    tracking_id VARCHAR(255) UNIQUE NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    processing_time_ms BIGINT,
    sent_at TIMESTAMP,
    opened_at TIMESTAMP,
    clicked_at TIMESTAMP,
    bounced_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_email_tracking_id
ON email_delivery_tracking(tracking_id);

CREATE INDEX IF NOT EXISTS idx_email_recipient
ON email_delivery_tracking(recipient);

CREATE INDEX IF NOT EXISTS idx_email_status
ON email_delivery_tracking(status);

CREATE INDEX IF NOT EXISTS idx_email_sent_at
ON email_delivery_tracking(sent_at DESC);

-- Partial indexes for performance optimization
CREATE INDEX IF NOT EXISTS idx_notifications_unread
ON notifications(user_id, created_at DESC)
WHERE is_read = false;

-- Note: dead letter messages indexes are created in V6__create_dead_letter_messages_table.sql

-- Function-based index for email domain analysis
CREATE INDEX IF NOT EXISTS idx_email_domain
ON email_delivery_tracking(SUBSTRING(recipient FROM '@(.*)$'));

-- Add comment for documentation
COMMENT ON INDEX idx_notifications_user_id_created_at IS 'Primary query pattern for user notifications timeline';
COMMENT ON INDEX idx_notifications_unread IS 'Optimized for unread notification queries';
COMMENT ON INDEX idx_email_domain IS 'Email domain analysis for deliverability tracking';

-- Update statistics for query planner
ANALYZE notifications;
ANALYZE notification_preferences;
ANALYZE notification_templates;
ANALYZE email_delivery_tracking;
ANALYZE security_audit_log;