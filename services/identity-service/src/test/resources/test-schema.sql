-- Test schema for H2 database
-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_verification_token VARCHAR(255),
    password_reset_token VARCHAR(255),
    password_reset_token_expiry TIMESTAMP,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    two_factor_secret VARCHAR(255),
    preferred_language VARCHAR(10) DEFAULT 'en',
    timezone VARCHAR(50) DEFAULT 'UTC',
    notification_preferences TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(45),
    deleted_at TIMESTAMP
);

-- Create personas table
CREATE TABLE IF NOT EXISTS personas (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL,
    bio TEXT,
    avatar_url VARCHAR(255),
    display_name VARCHAR(100),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    language_preference VARCHAR(10),
    theme_preference VARCHAR(20),
    notification_preferences TEXT,
    allow_direct_messages BOOLEAN DEFAULT TRUE,
    searchable BOOLEAN DEFAULT TRUE,
    share_achievements BOOLEAN DEFAULT TRUE,
    share_focus_sessions BOOLEAN DEFAULT TRUE,
    show_activity BOOLEAN DEFAULT TRUE,
    show_email BOOLEAN DEFAULT FALSE,
    show_online_status BOOLEAN DEFAULT TRUE,
    show_real_name BOOLEAN DEFAULT FALSE,
    visibility_level VARCHAR(20) DEFAULT 'PUBLIC',
    status_message VARCHAR(255),
    last_active_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create persona_attributes table for custom attributes
CREATE TABLE IF NOT EXISTS persona_attributes (
    persona_id UUID NOT NULL,
    attribute_key VARCHAR(255) NOT NULL,
    attribute_value TEXT,
    PRIMARY KEY (persona_id, attribute_key),
    FOREIGN KEY (persona_id) REFERENCES personas(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_persona_user ON personas(user_id);
CREATE INDEX IF NOT EXISTS idx_persona_active ON personas(user_id, is_active);