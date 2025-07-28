-- Create chat messages table
CREATE TABLE chat_messages (
    id VARCHAR(36) PRIMARY KEY,
    hive_id VARCHAR(36) NOT NULL,
    sender_id VARCHAR(36) NOT NULL,
    sender_username VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    edited BOOLEAN DEFAULT FALSE,
    edited_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_message_hive FOREIGN KEY (hive_id) REFERENCES hives(id) ON DELETE CASCADE
);

-- Create index for efficient message retrieval
CREATE INDEX idx_chat_messages_hive_created ON chat_messages(hive_id, created_at DESC);
CREATE INDEX idx_chat_messages_sender ON chat_messages(sender_id);

-- Create message reactions table (optional enhancement)
CREATE TABLE message_reactions (
    id VARCHAR(36) PRIMARY KEY,
    message_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    reaction VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reaction_message FOREIGN KEY (message_id) REFERENCES chat_messages(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_message_reaction (message_id, user_id, reaction)
);

-- Create read receipts table
CREATE TABLE message_read_receipts (
    id VARCHAR(36) PRIMARY KEY,
    message_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_receipt_message FOREIGN KEY (message_id) REFERENCES chat_messages(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_message_read (message_id, user_id)
);