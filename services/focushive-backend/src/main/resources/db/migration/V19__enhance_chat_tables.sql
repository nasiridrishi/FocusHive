-- V19: Enhance Chat Tables for Phase 4.1 - Advanced Chat Features
-- This migration adds threading, enhanced reactions, attachments, and search capabilities

-- Add new columns to existing chat_messages table for enhanced features
ALTER TABLE chat_messages
ADD COLUMN IF NOT EXISTS parent_message_id UUID REFERENCES chat_messages(id) ON DELETE SET NULL,
ADD COLUMN IF NOT EXISTS thread_id UUID,
ADD COLUMN IF NOT EXISTS reply_count INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS reaction_count INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS attachment_count INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS read_by_count INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS pinned_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN IF NOT EXISTS pinned_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0,
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;

-- Create chat_threads table for managing threaded conversations
CREATE TABLE IF NOT EXISTS chat_threads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hive_id UUID NOT NULL REFERENCES hives(id) ON DELETE CASCADE,
    parent_message_id UUID NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
    title VARCHAR(255),
    reply_count INTEGER DEFAULT 0,
    last_activity_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_reply_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    last_reply_username VARCHAR(255),
    is_archived BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP WITH TIME ZONE,

    -- Constraints
    CONSTRAINT chat_threads_unique_parent UNIQUE(parent_message_id)
);

-- Create message_attachments table for file sharing
CREATE TABLE IF NOT EXISTS message_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    mime_type VARCHAR(100),
    thumbnail_path VARCHAR(500),
    download_count BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Enhance existing message_reactions table
-- Drop and recreate with enhanced schema
DROP TABLE IF EXISTS message_reactions CASCADE;

CREATE TABLE message_reactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    username VARCHAR(255) NOT NULL,
    emoji VARCHAR(10) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP WITH TIME ZONE,

    -- Constraints
    CONSTRAINT message_reactions_unique_user_emoji UNIQUE(message_id, user_id, emoji)
);

-- Create comprehensive indexes for performance

-- Chat Messages indexes
CREATE INDEX IF NOT EXISTS idx_chat_messages_parent ON chat_messages(parent_message_id) WHERE parent_message_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_chat_messages_thread ON chat_messages(thread_id) WHERE thread_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_chat_messages_hive_created ON chat_messages(hive_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_messages_sender ON chat_messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_type ON chat_messages(message_type);
CREATE INDEX IF NOT EXISTS idx_chat_messages_pinned ON chat_messages(hive_id, is_pinned, pinned_at) WHERE is_pinned = TRUE;
CREATE INDEX IF NOT EXISTS idx_chat_messages_content_search ON chat_messages USING gin(to_tsvector('english', content));
CREATE INDEX IF NOT EXISTS idx_chat_messages_sender_username ON chat_messages(hive_id, sender_username);

-- Chat Threads indexes
CREATE INDEX IF NOT EXISTS idx_chat_threads_hive ON chat_threads(hive_id);
CREATE INDEX IF NOT EXISTS idx_chat_threads_parent ON chat_threads(parent_message_id);
CREATE INDEX IF NOT EXISTS idx_chat_threads_last_activity ON chat_threads(last_activity_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_threads_archived ON chat_threads(hive_id, is_archived);

-- Message Reactions indexes
CREATE INDEX IF NOT EXISTS idx_message_reactions_message ON message_reactions(message_id);
CREATE INDEX IF NOT EXISTS idx_message_reactions_user ON message_reactions(user_id);
CREATE INDEX IF NOT EXISTS idx_message_reactions_emoji ON message_reactions(emoji);

-- Message Attachments indexes
CREATE INDEX IF NOT EXISTS idx_message_attachments_message ON message_attachments(message_id);
CREATE INDEX IF NOT EXISTS idx_message_attachments_type ON message_attachments(file_type);
CREATE INDEX IF NOT EXISTS idx_message_attachments_size ON message_attachments(file_size);

-- Add foreign key constraint for thread_id after table creation
ALTER TABLE chat_messages
ADD CONSTRAINT IF NOT EXISTS fk_chat_messages_thread
FOREIGN KEY (thread_id) REFERENCES chat_threads(id) ON DELETE SET NULL;

-- Create triggers for maintaining denormalized counts

-- Function to update message reply count
CREATE OR REPLACE FUNCTION update_message_reply_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' AND NEW.parent_message_id IS NOT NULL THEN
        UPDATE chat_messages
        SET reply_count = reply_count + 1
        WHERE id = NEW.parent_message_id;
    ELSIF TG_OP = 'DELETE' AND OLD.parent_message_id IS NOT NULL THEN
        UPDATE chat_messages
        SET reply_count = GREATEST(reply_count - 1, 0)
        WHERE id = OLD.parent_message_id;
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Function to update message reaction count
CREATE OR REPLACE FUNCTION update_message_reaction_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE chat_messages
        SET reaction_count = reaction_count + 1
        WHERE id = NEW.message_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE chat_messages
        SET reaction_count = GREATEST(reaction_count - 1, 0)
        WHERE id = OLD.message_id;
        RETURN OLD;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Function to update message attachment count
CREATE OR REPLACE FUNCTION update_message_attachment_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE chat_messages
        SET attachment_count = attachment_count + 1
        WHERE id = NEW.message_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE chat_messages
        SET attachment_count = GREATEST(attachment_count - 1, 0)
        WHERE id = OLD.message_id;
        RETURN OLD;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Function to update thread activity
CREATE OR REPLACE FUNCTION update_thread_activity()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' AND NEW.thread_id IS NOT NULL THEN
        UPDATE chat_threads
        SET
            reply_count = reply_count + 1,
            last_activity_at = NEW.created_at,
            last_reply_user_id = NEW.sender_id,
            last_reply_username = NEW.sender_username
        WHERE id = NEW.thread_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers
DROP TRIGGER IF EXISTS trigger_update_message_reply_count ON chat_messages;
CREATE TRIGGER trigger_update_message_reply_count
    AFTER INSERT OR DELETE ON chat_messages
    FOR EACH ROW EXECUTE FUNCTION update_message_reply_count();

DROP TRIGGER IF EXISTS trigger_update_message_reaction_count ON message_reactions;
CREATE TRIGGER trigger_update_message_reaction_count
    AFTER INSERT OR DELETE ON message_reactions
    FOR EACH ROW EXECUTE FUNCTION update_message_reaction_count();

DROP TRIGGER IF EXISTS trigger_update_message_attachment_count ON message_attachments;
CREATE TRIGGER trigger_update_message_attachment_count
    AFTER INSERT OR DELETE ON message_attachments
    FOR EACH ROW EXECUTE FUNCTION update_message_attachment_count();

DROP TRIGGER IF EXISTS trigger_update_thread_activity ON chat_messages;
CREATE TRIGGER trigger_update_thread_activity
    AFTER INSERT ON chat_messages
    FOR EACH ROW EXECUTE FUNCTION update_thread_activity();

-- Create updated_at triggers for automatic timestamp updates
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply updated_at triggers to all enhanced tables
DROP TRIGGER IF EXISTS trigger_chat_messages_updated_at ON chat_messages;
CREATE TRIGGER trigger_chat_messages_updated_at
    BEFORE UPDATE ON chat_messages
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS trigger_chat_threads_updated_at ON chat_threads;
CREATE TRIGGER trigger_chat_threads_updated_at
    BEFORE UPDATE ON chat_threads
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS trigger_message_reactions_updated_at ON message_reactions;
CREATE TRIGGER trigger_message_reactions_updated_at
    BEFORE UPDATE ON message_reactions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS trigger_message_attachments_updated_at ON message_attachments;
CREATE TRIGGER trigger_message_attachments_updated_at
    BEFORE UPDATE ON message_attachments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add performance optimization: partial indexes for common queries
CREATE INDEX IF NOT EXISTS idx_chat_messages_active ON chat_messages(hive_id, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_chat_messages_with_attachments ON chat_messages(hive_id, created_at DESC)
    WHERE attachment_count > 0 AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_chat_messages_with_reactions ON chat_messages(hive_id, reaction_count DESC)
    WHERE reaction_count > 0 AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_chat_threads_active ON chat_threads(hive_id, last_activity_at DESC)
    WHERE is_archived = FALSE AND deleted_at IS NULL;

-- Add statistics for query optimization
ANALYZE chat_messages;
ANALYZE chat_threads;
ANALYZE message_reactions;
ANALYZE message_attachments;

-- Create view for message statistics
CREATE OR REPLACE VIEW chat_message_stats AS
SELECT
    hive_id,
    COUNT(*) as total_messages,
    COUNT(DISTINCT sender_id) as unique_senders,
    AVG(reaction_count) as avg_reactions,
    AVG(reply_count) as avg_replies,
    COUNT(*) FILTER (WHERE attachment_count > 0) as messages_with_attachments,
    COUNT(*) FILTER (WHERE is_pinned = TRUE) as pinned_messages,
    MIN(created_at) as oldest_message,
    MAX(created_at) as newest_message
FROM chat_messages
WHERE deleted_at IS NULL
GROUP BY hive_id;

-- Create view for thread statistics
CREATE OR REPLACE VIEW chat_thread_stats AS
SELECT
    hive_id,
    COUNT(*) as total_threads,
    COUNT(*) FILTER (WHERE is_archived = FALSE) as active_threads,
    AVG(reply_count) as avg_replies_per_thread,
    MAX(reply_count) as max_replies_in_thread,
    MIN(last_activity_at) as oldest_activity,
    MAX(last_activity_at) as newest_activity
FROM chat_threads
WHERE deleted_at IS NULL
GROUP BY hive_id;

-- Grant appropriate permissions (adjust as needed for your security model)
-- These would be customized based on your actual user roles
-- GRANT SELECT, INSERT, UPDATE, DELETE ON chat_messages TO chat_service_role;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON chat_threads TO chat_service_role;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON message_reactions TO chat_service_role;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON message_attachments TO chat_service_role;

-- Add comments for documentation
COMMENT ON TABLE chat_messages IS 'Enhanced chat messages with threading, reactions, and attachment support';
COMMENT ON TABLE chat_threads IS 'Thread metadata for organizing reply chains';
COMMENT ON TABLE message_reactions IS 'Emoji reactions to messages';
COMMENT ON TABLE message_attachments IS 'File attachments associated with messages';

COMMENT ON COLUMN chat_messages.parent_message_id IS 'Reference to parent message for threading';
COMMENT ON COLUMN chat_messages.thread_id IS 'Reference to thread for organizing conversations';
COMMENT ON COLUMN chat_messages.reply_count IS 'Denormalized count of replies to this message';
COMMENT ON COLUMN chat_messages.reaction_count IS 'Denormalized count of reactions to this message';
COMMENT ON COLUMN chat_messages.attachment_count IS 'Denormalized count of attachments on this message';
COMMENT ON COLUMN chat_messages.is_pinned IS 'Whether this message is pinned by moderators';

-- Migration completion marker
INSERT INTO schema_migrations (version, description, applied_at)
VALUES ('V19', 'Enhanced chat tables for Phase 4.1 - Threading, Reactions, Attachments', CURRENT_TIMESTAMP)
ON CONFLICT (version) DO UPDATE SET applied_at = CURRENT_TIMESTAMP;

-- Refresh materialized views if any exist
-- REFRESH MATERIALIZED VIEW IF EXISTS chat_message_summary;