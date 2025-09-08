-- V7__performance_indexes.sql
-- Performance optimization indexes for identity service

-- Users table indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_enabled ON users(enabled);
CREATE INDEX IF NOT EXISTS idx_users_email_verified ON users(email_verified);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

-- User personas indexes
CREATE INDEX IF NOT EXISTS idx_personas_user_id_v7 ON personas(user_id);
CREATE INDEX IF NOT EXISTS idx_personas_name ON personas(name);
CREATE INDEX IF NOT EXISTS idx_personas_is_active ON personas(is_active);
CREATE INDEX IF NOT EXISTS idx_personas_user_name ON personas(user_id, name);

-- User profiles indexes (table doesn't exist yet, skipping)

-- Privacy permissions indexes (table doesn't exist yet, skipping)

-- OAuth2 authorization indexes (tables use different structure)

-- OAuth2 authorization consent indexes (tables use different structure)

-- User activity logs indexes (table doesn't exist yet, skipping)

-- Compliance audit indexes (table doesn't exist yet, skipping)

-- Partial indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_enabled_true ON users(id, email, username) WHERE enabled = true;
CREATE INDEX IF NOT EXISTS idx_users_email_verified_true ON users(id, email) WHERE email_verified = true;
CREATE INDEX IF NOT EXISTS idx_personas_is_active_true ON personas(user_id, name) WHERE is_active = true;

-- Function-based indexes for case-insensitive searches
CREATE INDEX IF NOT EXISTS idx_users_email_lower ON users(LOWER(email));
CREATE INDEX IF NOT EXISTS idx_users_username_lower ON users(LOWER(username));