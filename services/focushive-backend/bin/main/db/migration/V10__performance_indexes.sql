-- V10__performance_indexes.sql
-- Performance optimization indexes for frequently queried columns

-- User table indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_enabled ON users(enabled);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

-- Hive table indexes
CREATE INDEX IF NOT EXISTS idx_hives_owner_id ON hives(owner_id);
-- CREATE INDEX IF NOT EXISTS idx_hives_active ON hives(active); -- Column doesn't exist
CREATE INDEX IF NOT EXISTS idx_hives_created_at ON hives(created_at);
CREATE INDEX IF NOT EXISTS idx_hives_type ON hives(type);

-- Hive members table indexes
CREATE INDEX IF NOT EXISTS idx_hive_members_hive_id ON hive_members(hive_id);
CREATE INDEX IF NOT EXISTS idx_hive_members_user_id ON hive_members(user_id);
CREATE INDEX IF NOT EXISTS idx_hive_members_role ON hive_members(role);
CREATE INDEX IF NOT EXISTS idx_hive_members_joined_at ON hive_members(joined_at);
-- CREATE INDEX IF NOT EXISTS idx_hive_members_active ON hive_members(active); -- Column doesn't exist
-- Composite index for common query pattern
CREATE INDEX IF NOT EXISTS idx_hive_members_hive_user ON hive_members(hive_id, user_id);
-- CREATE INDEX IF NOT EXISTS idx_hive_members_user_active ON hive_members(user_id, active); -- Column doesn't exist

-- Focus sessions table indexes
CREATE INDEX IF NOT EXISTS idx_focus_sessions_user_id ON focus_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_focus_sessions_hive_id ON focus_sessions(hive_id);
CREATE INDEX IF NOT EXISTS idx_focus_sessions_start_time ON focus_sessions(start_time);
CREATE INDEX IF NOT EXISTS idx_focus_sessions_completed ON focus_sessions(completed);
CREATE INDEX IF NOT EXISTS idx_focus_sessions_user_completed ON focus_sessions(user_id, completed);
-- Date range queries optimization
CREATE INDEX IF NOT EXISTS idx_focus_sessions_user_date_range ON focus_sessions(user_id, start_time, end_time);

-- Daily summaries table indexes
CREATE INDEX IF NOT EXISTS idx_daily_summaries_user_id ON daily_summaries(user_id);
CREATE INDEX IF NOT EXISTS idx_daily_summaries_date ON daily_summaries(date);
CREATE INDEX IF NOT EXISTS idx_daily_summaries_user_date ON daily_summaries(user_id, date);

-- Chat messages table indexes
CREATE INDEX IF NOT EXISTS idx_chat_messages_hive_id ON chat_messages(hive_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_sender_id ON chat_messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_created_at ON chat_messages(created_at);
CREATE INDEX IF NOT EXISTS idx_chat_messages_hive_created ON chat_messages(hive_id, created_at);

-- Buddy system indexes
CREATE INDEX IF NOT EXISTS idx_buddy_relationships_user1 ON buddy_relationships(user1_id);
CREATE INDEX IF NOT EXISTS idx_buddy_relationships_user2 ON buddy_relationships(user2_id);
CREATE INDEX IF NOT EXISTS idx_buddy_relationships_status ON buddy_relationships(status);
CREATE INDEX IF NOT EXISTS idx_buddy_sessions_relationship ON buddy_sessions(relationship_id);
CREATE INDEX IF NOT EXISTS idx_buddy_sessions_start_time ON buddy_sessions(start_time);

-- Forum indexes (tables don't exist yet, skipping)

-- Notification indexes (table doesn't exist yet, skipping)

-- Audit log indexes (table doesn't exist yet, skipping)

-- Partial indexes for better performance on commonly filtered data
-- CREATE INDEX IF NOT EXISTS idx_hives_active_true ON hives(id, name, created_at) WHERE active = true; -- Column doesn't exist
CREATE INDEX IF NOT EXISTS idx_users_enabled_true ON users(id, username, email) WHERE enabled = true;
CREATE INDEX IF NOT EXISTS idx_focus_sessions_incomplete ON focus_sessions(user_id, hive_id, start_time) WHERE completed = false;

-- Function-based indexes for case-insensitive searches
CREATE INDEX IF NOT EXISTS idx_users_email_lower ON users(LOWER(email));
CREATE INDEX IF NOT EXISTS idx_users_username_lower ON users(LOWER(username));
CREATE INDEX IF NOT EXISTS idx_hives_name_lower ON hives(LOWER(name));