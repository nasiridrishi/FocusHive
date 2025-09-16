-- V2__additional_performance_indexes.sql
-- Additional performance optimization indexes for Chat Service  
-- Enhances messaging, reactions, and real-time features performance

-- Chat messages advanced indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_messages_sender_time 
ON chat_messages(sender_id, created_at DESC, hive_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_messages_type_filter 
ON chat_messages(hive_id, message_type, created_at DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_messages_edited 
ON chat_messages(hive_id, edited, edited_at DESC) 
WHERE edited = TRUE;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_messages_pagination 
ON chat_messages(hive_id, created_at DESC, id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_messages_user_activity 
ON chat_messages(sender_id, created_at DESC) 
WHERE message_type = 'TEXT';

-- Message reactions performance indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_message_reactions_message 
ON message_reactions(message_id, reaction, user_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_message_reactions_user 
ON message_reactions(user_id, created_at DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_message_reactions_popular 
ON message_reactions(reaction, created_at DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_message_reactions_analytics 
ON message_reactions(message_id, reaction) 
WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '30 days';

-- Message read receipts optimization
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_message_read_receipts_user 
ON message_read_receipts(user_id, read_at DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_message_read_receipts_message 
ON message_read_receipts(message_id, read_at DESC, user_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_message_read_receipts_unread 
ON message_read_receipts(user_id, message_id) 
WHERE read_at > CURRENT_TIMESTAMP - INTERVAL '24 hours';

-- Typing indicators real-time performance
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_typing_indicators_active 
ON typing_indicators(hive_id, expires_at, user_id) 
WHERE expires_at > CURRENT_TIMESTAMP;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_typing_indicators_cleanup 
ON typing_indicators(expires_at) 
WHERE expires_at < CURRENT_TIMESTAMP;

-- Chat statistics and analytics indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_statistics_trends 
ON chat_statistics(hive_id, date DESC, message_count);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_statistics_activity 
ON chat_statistics(date, active_users_count DESC, message_count DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_statistics_hourly 
ON chat_statistics(hive_id, most_active_hour, date DESC);

-- Composite indexes for complex chat queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_messages_hive_sender_time 
ON chat_messages(hive_id, sender_id, created_at DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_messages_search_context 
ON chat_messages(hive_id, created_at DESC, message_type, sender_username);

-- Performance indexes for message threading (if implemented)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_messages_thread_support 
ON chat_messages(hive_id, id, created_at) 
WHERE message_type IN ('TEXT', 'IMAGE', 'FILE');

-- Time-based indexes for chat history and archival
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_messages_recent 
ON chat_messages(hive_id, created_at DESC) 
WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '7 days';

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_messages_archive 
ON chat_messages(created_at, hive_id) 
WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '90 days';

-- User activity and engagement indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_user_engagement 
ON chat_messages(sender_id, hive_id, created_at DESC) 
WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '30 days';

-- Indexes for moderation and content management
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_messages_moderation 
ON chat_messages(hive_id, message_type, sender_id, created_at DESC);

-- Function-based indexes for content search
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_messages_content_search 
ON chat_messages USING GIN(to_tsvector('english', content)) 
WHERE message_type = 'TEXT';

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_messages_sender_search 
ON chat_messages USING GIN(to_tsvector('english', sender_username));

-- Partial indexes for performance optimization
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_messages_text_only 
ON chat_messages(hive_id, created_at DESC, sender_id) 
WHERE message_type = 'TEXT';

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_messages_media 
ON chat_messages(hive_id, created_at DESC, message_type) 
WHERE message_type IN ('IMAGE', 'FILE');

-- Real-time notification support indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_messages_notifications 
ON chat_messages(hive_id, sender_id, created_at DESC) 
WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '1 hour';

-- Bulk operations and export indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_messages_bulk_export 
ON chat_messages(hive_id, created_at, sender_id, message_type);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_statistics_reporting 
ON chat_statistics(date, hive_id, message_count, active_users_count);

-- Cleanup and maintenance indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_typing_indicators_expired 
ON typing_indicators(expires_at) 
WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '1 hour';

-- Analyze tables after creating indexes
ANALYZE chat_messages;
ANALYZE message_reactions;
ANALYZE message_read_receipts;
ANALYZE typing_indicators;
ANALYZE chat_statistics;

-- Comments for index purposes
COMMENT ON INDEX idx_chat_messages_sender_time IS 'Optimizes user message history and activity queries';
COMMENT ON INDEX idx_message_reactions_message IS 'Accelerates reaction loading and counting for messages';
COMMENT ON INDEX idx_message_read_receipts_unread IS 'Supports real-time unread message tracking';
COMMENT ON INDEX idx_typing_indicators_active IS 'Optimizes real-time typing indicator retrieval';
COMMENT ON INDEX idx_chat_messages_content_search IS 'Enables full-text search across chat message content';
COMMENT ON INDEX idx_chat_statistics_trends IS 'Supports chat activity analytics and trend analysis';
COMMENT ON INDEX idx_chat_messages_pagination IS 'Optimizes message pagination for chat history loading';