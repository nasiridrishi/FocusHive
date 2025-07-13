-- Create chat_messages table
CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hive_id UUID NOT NULL REFERENCES hives(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_id UUID REFERENCES chat_messages(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    type VARCHAR(20) DEFAULT 'TEXT' CHECK (type IN ('TEXT', 'IMAGE', 'FILE', 'SYSTEM', 'ANNOUNCEMENT')),
    metadata JSONB DEFAULT '{}'::jsonb,
    is_edited BOOLEAN DEFAULT FALSE,
    edited_at TIMESTAMP WITH TIME ZONE,
    is_pinned BOOLEAN DEFAULT FALSE,
    reactions JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Indexes for chat_messages
CREATE INDEX idx_messages_hive ON chat_messages(hive_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_messages_sender ON chat_messages(sender_id);
CREATE INDEX idx_messages_parent ON chat_messages(parent_id) WHERE parent_id IS NOT NULL;
CREATE INDEX idx_messages_pinned ON chat_messages(hive_id, is_pinned) WHERE is_pinned = TRUE AND deleted_at IS NULL;

-- Create notifications table
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    action_url VARCHAR(500),
    data JSONB DEFAULT '{}'::jsonb,
    priority VARCHAR(20) DEFAULT 'NORMAL' CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP WITH TIME ZONE,
    is_archived BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for notifications
CREATE INDEX idx_notifications_user ON notifications(user_id, is_read, created_at DESC);
CREATE INDEX idx_notifications_unread ON notifications(user_id, created_at DESC) WHERE is_read = FALSE;
CREATE INDEX idx_notifications_type ON notifications(user_id, type);
CREATE INDEX idx_notifications_priority ON notifications(user_id, priority) WHERE is_read = FALSE;

-- Create user_blocks table
CREATE TABLE user_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blocker_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(blocker_id, blocked_id),
    CHECK (blocker_id != blocked_id)
);

CREATE INDEX idx_blocks_blocker ON user_blocks(blocker_id);
CREATE INDEX idx_blocks_blocked ON user_blocks(blocked_id);

-- Create reports table
CREATE TABLE reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL REFERENCES users(id),
    reported_user_id UUID REFERENCES users(id),
    reported_hive_id UUID REFERENCES hives(id),
    reported_message_id UUID REFERENCES chat_messages(id),
    type VARCHAR(50) NOT NULL CHECK (type IN ('USER', 'HIVE', 'MESSAGE', 'OTHER')),
    reason VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(50) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'REVIEWING', 'RESOLVED', 'DISMISSED')),
    moderator_id UUID REFERENCES users(id),
    moderator_notes TEXT,
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (
        (reported_user_id IS NOT NULL AND reported_hive_id IS NULL AND reported_message_id IS NULL) OR
        (reported_user_id IS NULL AND reported_hive_id IS NOT NULL AND reported_message_id IS NULL) OR
        (reported_user_id IS NULL AND reported_hive_id IS NULL AND reported_message_id IS NOT NULL)
    )
);

-- Indexes for reports
CREATE INDEX idx_reports_reporter ON reports(reporter_id);
CREATE INDEX idx_reports_status ON reports(status) WHERE status != 'RESOLVED';
CREATE INDEX idx_reports_type ON reports(type);
CREATE INDEX idx_reports_created ON reports(created_at DESC);

CREATE TRIGGER update_reports_updated_at BEFORE UPDATE ON reports
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create buddy_connections table
CREATE TABLE buddy_connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user1_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user2_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'BLOCKED')),
    initiated_by UUID NOT NULL REFERENCES users(id),
    shared_goals TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user1_id, user2_id),
    CHECK (user1_id < user2_id)  -- Ensure consistent ordering
);

-- Indexes for buddy_connections
CREATE INDEX idx_buddy_user1 ON buddy_connections(user1_id);
CREATE INDEX idx_buddy_user2 ON buddy_connections(user2_id);
CREATE INDEX idx_buddy_status ON buddy_connections(status);

CREATE TRIGGER update_buddy_connections_updated_at BEFORE UPDATE ON buddy_connections
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();