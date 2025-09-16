-- V15__add_oauth2_session_management.sql
-- Add OAuth2 session management tables for tracking user sessions

-- Create OAuth2 sessions table
CREATE TABLE oauth2_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES oauth_clients(id) ON DELETE CASCADE,
    session_token VARCHAR(255) NOT NULL UNIQUE,
    auth_method VARCHAR(50),
    auth_level VARCHAR(50) DEFAULT 'basic',
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    device_fingerprint VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    termination_reason VARCHAR(255),
    terminated_at TIMESTAMP,
    metadata TEXT,
    granted_scopes VARCHAR(1000),
    id_token TEXT
);

-- Create indexes for OAuth2 sessions
CREATE INDEX idx_oauth2_session_token ON oauth2_sessions(session_token);
CREATE INDEX idx_oauth2_session_user ON oauth2_sessions(user_id);
CREATE INDEX idx_oauth2_session_client ON oauth2_sessions(client_id);
CREATE INDEX idx_oauth2_session_expires ON oauth2_sessions(expires_at);
CREATE INDEX idx_oauth2_session_active ON oauth2_sessions(active) WHERE active = TRUE;

-- Add session reference to refresh tokens table
ALTER TABLE oauth_refresh_tokens
ADD COLUMN oauth2_session_id UUID REFERENCES oauth2_sessions(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_oauth_refresh_token_session ON oauth_refresh_tokens(oauth2_session_id);

-- Create session events table for audit
CREATE TABLE oauth2_session_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES oauth2_sessions(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    event_data TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_oauth2_session_events_session ON oauth2_session_events(session_id);
CREATE INDEX idx_oauth2_session_events_type ON oauth2_session_events(event_type);
CREATE INDEX idx_oauth2_session_events_occurred ON oauth2_session_events(occurred_at);

-- Add trigger to update last_accessed_at timestamp
CREATE OR REPLACE FUNCTION update_oauth2_session_last_accessed()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_accessed_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_oauth2_session_last_accessed
    BEFORE UPDATE ON oauth2_sessions
    FOR EACH ROW
    WHEN (OLD.last_accessed_at IS DISTINCT FROM NEW.last_accessed_at)
    EXECUTE FUNCTION update_oauth2_session_last_accessed();

-- Add comment for documentation
COMMENT ON TABLE oauth2_sessions IS 'Stores OAuth2 session information for user authentication state';
COMMENT ON TABLE oauth2_session_events IS 'Audit log of session events for security monitoring';
COMMENT ON COLUMN oauth2_sessions.auth_level IS 'Authentication level: basic, elevated, mfa';
COMMENT ON COLUMN oauth2_sessions.auth_method IS 'Authentication method: password, social, biometric, etc.';
COMMENT ON COLUMN oauth2_sessions.device_fingerprint IS 'Device fingerprint for enhanced security and tracking';