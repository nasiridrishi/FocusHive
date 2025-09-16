-- V6__create_dead_letter_messages_table.sql
-- Dead letter messages table for storing failed notifications

-- Drop the existing table if it exists (created incorrectly by V5 with wrong structure)
DROP TABLE IF EXISTS dead_letter_messages CASCADE;

CREATE TABLE dead_letter_messages (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(255),
    recipient VARCHAR(255) NOT NULL,
    subject TEXT,
    content TEXT,
    error_message VARCHAR(1000),
    retry_count INTEGER NOT NULL DEFAULT 0,
    original_queue VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retried_at TIMESTAMP,
    resolved_at TIMESTAMP,
    resolution VARCHAR(500),
    notification_type VARCHAR(100),
    user_id BIGINT,
    priority VARCHAR(50),

    CONSTRAINT chk_status CHECK (status IN (
        'PENDING', 'RETRIED', 'RETRY_FAILED', 'RESOLVED',
        'MAX_RETRIES_EXCEEDED', 'EXPIRED', 'PROCESSING'
    )),

    CONSTRAINT chk_retry_count CHECK (retry_count >= 0)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_dlq_status ON dead_letter_messages(status);
CREATE INDEX IF NOT EXISTS idx_dlq_created_at ON dead_letter_messages(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_dlq_recipient ON dead_letter_messages(recipient);
CREATE INDEX IF NOT EXISTS idx_dlq_user_id ON dead_letter_messages(user_id);
CREATE INDEX IF NOT EXISTS idx_dlq_priority_status ON dead_letter_messages(priority, status);
CREATE INDEX IF NOT EXISTS idx_dlq_message_id ON dead_letter_messages(message_id);
CREATE INDEX IF NOT EXISTS idx_dlq_notification_type ON dead_letter_messages(notification_type);

-- Partial index for retriable messages
CREATE INDEX IF NOT EXISTS idx_dlq_retriable ON dead_letter_messages(created_at DESC)
WHERE status IN ('PENDING', 'RETRY_FAILED') AND retry_count < 3;

-- Partial index for critical pending messages
CREATE INDEX IF NOT EXISTS idx_dlq_critical_pending ON dead_letter_messages(created_at DESC)
WHERE priority = 'CRITICAL' AND status = 'PENDING';

-- Comment for documentation
COMMENT ON TABLE dead_letter_messages IS 'Stores failed notification messages for manual review and retry';
COMMENT ON COLUMN dead_letter_messages.status IS 'Current status of the dead letter message processing';
COMMENT ON COLUMN dead_letter_messages.retry_count IS 'Number of retry attempts made';
COMMENT ON COLUMN dead_letter_messages.priority IS 'Priority level: LOW, NORMAL, HIGH, CRITICAL';