-- Create chat messages table
CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hive_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    sender_username VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    edited BOOLEAN DEFAULT FALSE,
    edited_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index for efficient message retrieval
CREATE INDEX idx_chat_messages_hive_created ON chat_messages(hive_id, created_at DESC);
CREATE INDEX idx_chat_messages_sender ON chat_messages(sender_id);

-- Create message reactions table (optional enhancement)
CREATE TABLE message_reactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL,
    user_id UUID NOT NULL,
    reaction VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reaction_message FOREIGN KEY (message_id) REFERENCES chat_messages(id) ON DELETE CASCADE,
    UNIQUE(message_id, user_id, reaction)
);

-- Create read receipts table
CREATE TABLE message_read_receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL,
    user_id UUID NOT NULL,
    read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_receipt_message FOREIGN KEY (message_id) REFERENCES chat_messages(id) ON DELETE CASCADE,
    UNIQUE(message_id, user_id)
);

-- Create typing indicators table (for real-time typing status)
CREATE TABLE typing_indicators (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hive_id UUID NOT NULL,
    user_id UUID NOT NULL,
    username VARCHAR(255) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE(hive_id, user_id)
);

-- Create indexes for typing indicators
CREATE INDEX idx_typing_indicators_hive ON typing_indicators(hive_id, expires_at);
CREATE INDEX idx_typing_indicators_expires ON typing_indicators(expires_at);

-- Create chat statistics table
CREATE TABLE chat_statistics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hive_id UUID NOT NULL,
    date DATE NOT NULL,
    message_count INTEGER DEFAULT 0,
    active_users_count INTEGER DEFAULT 0,
    most_active_hour INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(hive_id, date)
);

-- Create index for chat statistics
CREATE INDEX idx_chat_statistics_hive_date ON chat_statistics(hive_id, date DESC);

-- Add comments for documentation
COMMENT ON TABLE chat_messages IS 'Stores all chat messages within hives';
COMMENT ON TABLE message_reactions IS 'Stores user reactions to messages (emoji reactions)';
COMMENT ON TABLE message_read_receipts IS 'Tracks which users have read which messages';
COMMENT ON TABLE typing_indicators IS 'Temporary storage for real-time typing indicators';
COMMENT ON TABLE chat_statistics IS 'Daily statistics for chat activity per hive';

COMMENT ON COLUMN chat_messages.message_type IS 'Type of message: TEXT, IMAGE, FILE, SYSTEM';
COMMENT ON COLUMN typing_indicators.expires_at IS 'When the typing indicator should be automatically removed';
COMMENT ON COLUMN chat_statistics.most_active_hour IS 'Hour of day (0-23) with most chat activity';