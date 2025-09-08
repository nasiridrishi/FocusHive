-- OAuth2 Server Tables Migration
-- Creates tables for comprehensive OAuth2 authorization server functionality

-- Create OAuth Access Tokens table
CREATE TABLE oauth_access_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES oauth_clients(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP,
    revocation_reason VARCHAR(500),
    issued_ip VARCHAR(45),
    user_agent VARCHAR(500),
    last_used_at TIMESTAMP,
    usage_count BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create OAuth Access Token Scopes table
CREATE TABLE oauth_access_token_scopes (
    token_id UUID NOT NULL REFERENCES oauth_access_tokens(id) ON DELETE CASCADE,
    scope VARCHAR(100) NOT NULL,
    PRIMARY KEY (token_id, scope)
);

-- Create OAuth Refresh Tokens table
CREATE TABLE oauth_refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES oauth_clients(id) ON DELETE CASCADE,
    access_token_id UUID REFERENCES oauth_access_tokens(id) ON DELETE SET NULL,
    expires_at TIMESTAMP,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP,
    revocation_reason VARCHAR(500),
    issued_ip VARCHAR(45),
    user_agent VARCHAR(500),
    last_used_at TIMESTAMP,
    usage_count BIGINT DEFAULT 0,
    replaced_token_id UUID REFERENCES oauth_refresh_tokens(id) ON DELETE SET NULL,
    session_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create OAuth Refresh Token Scopes table
CREATE TABLE oauth_refresh_token_scopes (
    token_id UUID NOT NULL REFERENCES oauth_refresh_tokens(id) ON DELETE CASCADE,
    scope VARCHAR(100) NOT NULL,
    PRIMARY KEY (token_id, scope)
);

-- Create OAuth Authorization Codes table
CREATE TABLE oauth_authorization_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES oauth_clients(id) ON DELETE CASCADE,
    redirect_uri VARCHAR(500) NOT NULL,
    state VARCHAR(255),
    code_challenge VARCHAR(128),
    code_challenge_method VARCHAR(10),
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP,
    issued_ip VARCHAR(45),
    user_agent VARCHAR(500),
    session_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create OAuth Authorization Code Scopes table
CREATE TABLE oauth_authorization_code_scopes (
    auth_code_id UUID NOT NULL REFERENCES oauth_authorization_codes(id) ON DELETE CASCADE,
    scope VARCHAR(100) NOT NULL,
    PRIMARY KEY (auth_code_id, scope)
);

-- Create indexes for OAuth Access Tokens
CREATE INDEX idx_oauth_access_token_hash ON oauth_access_tokens(token_hash);
CREATE INDEX idx_oauth_access_token_user ON oauth_access_tokens(user_id);
CREATE INDEX idx_oauth_access_token_client ON oauth_access_tokens(client_id);
CREATE INDEX idx_oauth_access_token_expires ON oauth_access_tokens(expires_at);
CREATE INDEX idx_oauth_access_token_revoked ON oauth_access_tokens(revoked);
CREATE INDEX idx_oauth_access_token_created ON oauth_access_tokens(created_at);

-- Create indexes for OAuth Refresh Tokens
CREATE INDEX idx_oauth_refresh_token_hash ON oauth_refresh_tokens(token_hash);
CREATE INDEX idx_oauth_refresh_token_user ON oauth_refresh_tokens(user_id);
CREATE INDEX idx_oauth_refresh_token_client ON oauth_refresh_tokens(client_id);
CREATE INDEX idx_oauth_refresh_token_expires ON oauth_refresh_tokens(expires_at);
CREATE INDEX idx_oauth_refresh_token_revoked ON oauth_refresh_tokens(revoked);
CREATE INDEX idx_oauth_refresh_token_access ON oauth_refresh_tokens(access_token_id);
CREATE INDEX idx_oauth_refresh_token_session ON oauth_refresh_tokens(session_id);
CREATE INDEX idx_oauth_refresh_token_created ON oauth_refresh_tokens(created_at);

-- Create indexes for OAuth Authorization Codes
CREATE INDEX idx_oauth_auth_code ON oauth_authorization_codes(code);
CREATE INDEX idx_oauth_auth_code_user ON oauth_authorization_codes(user_id);
CREATE INDEX idx_oauth_auth_code_client ON oauth_authorization_codes(client_id);
CREATE INDEX idx_oauth_auth_code_expires ON oauth_authorization_codes(expires_at);
CREATE INDEX idx_oauth_auth_code_used ON oauth_authorization_codes(used);
CREATE INDEX idx_oauth_auth_code_session ON oauth_authorization_codes(session_id);
CREATE INDEX idx_oauth_auth_code_created ON oauth_authorization_codes(created_at);

-- Add constraints for OAuth Authorization Codes
ALTER TABLE oauth_authorization_codes 
ADD CONSTRAINT chk_oauth_auth_code_challenge_method 
CHECK (code_challenge_method IS NULL OR code_challenge_method IN ('plain', 'S256'));

-- Add constraints for token expiry times
ALTER TABLE oauth_access_tokens 
ADD CONSTRAINT chk_oauth_access_token_expires_future 
CHECK (expires_at > created_at);

ALTER TABLE oauth_authorization_codes 
ADD CONSTRAINT chk_oauth_auth_code_expires_future 
CHECK (expires_at > created_at);

-- Add constraint to ensure revoked tokens have revocation timestamp
ALTER TABLE oauth_access_tokens 
ADD CONSTRAINT chk_oauth_access_token_revocation 
CHECK ((revoked = FALSE AND revoked_at IS NULL) OR (revoked = TRUE AND revoked_at IS NOT NULL));

ALTER TABLE oauth_refresh_tokens 
ADD CONSTRAINT chk_oauth_refresh_token_revocation 
CHECK ((revoked = FALSE AND revoked_at IS NULL) OR (revoked = TRUE AND revoked_at IS NOT NULL));

-- Add constraint to ensure used codes have used timestamp
ALTER TABLE oauth_authorization_codes 
ADD CONSTRAINT chk_oauth_auth_code_used 
CHECK ((used = FALSE AND used_at IS NULL) OR (used = TRUE AND used_at IS NOT NULL));

-- Add constraint for PKCE: if code_challenge is present, method must be present
ALTER TABLE oauth_authorization_codes 
ADD CONSTRAINT chk_oauth_auth_code_pkce 
CHECK ((code_challenge IS NULL AND code_challenge_method IS NULL) OR 
       (code_challenge IS NOT NULL AND code_challenge_method IS NOT NULL));

-- Add performance optimization indexes
CREATE INDEX idx_oauth_access_token_user_active ON oauth_access_tokens(user_id, revoked, expires_at) 
WHERE revoked = FALSE;

CREATE INDEX idx_oauth_refresh_token_user_active ON oauth_refresh_tokens(user_id, revoked, expires_at) 
WHERE revoked = FALSE;

CREATE INDEX idx_oauth_auth_code_client_valid ON oauth_authorization_codes(client_id, used, expires_at) 
WHERE used = FALSE;

-- Add partial indexes for performance
CREATE INDEX idx_oauth_access_token_valid ON oauth_access_tokens(user_id, client_id) 
WHERE revoked = FALSE;

CREATE INDEX idx_oauth_refresh_token_valid ON oauth_refresh_tokens(user_id, client_id) 
WHERE revoked = FALSE;

-- Create cleanup indexes for expired tokens
CREATE INDEX idx_oauth_access_token_cleanup ON oauth_access_tokens(expires_at) 
WHERE revoked = FALSE;

CREATE INDEX idx_oauth_refresh_token_cleanup ON oauth_refresh_tokens(expires_at) 
WHERE revoked = FALSE AND expires_at IS NOT NULL;

CREATE INDEX idx_oauth_auth_code_cleanup ON oauth_authorization_codes(expires_at) 
WHERE used = FALSE;

-- Add comments for documentation
COMMENT ON TABLE oauth_access_tokens IS 'OAuth2 access tokens with security features and audit trail';
COMMENT ON TABLE oauth_refresh_tokens IS 'OAuth2 refresh tokens supporting token rotation and long-lived sessions';
COMMENT ON TABLE oauth_authorization_codes IS 'OAuth2 authorization codes with PKCE support for secure code exchange';

COMMENT ON COLUMN oauth_access_tokens.token_hash IS 'SHA-256 hash of the actual token value for security';
COMMENT ON COLUMN oauth_refresh_tokens.replaced_token_id IS 'Reference to previous token in rotation chain';
COMMENT ON COLUMN oauth_authorization_codes.code_challenge IS 'PKCE code challenge for enhanced security';
COMMENT ON COLUMN oauth_authorization_codes.code_challenge_method IS 'PKCE challenge method: plain or S256';