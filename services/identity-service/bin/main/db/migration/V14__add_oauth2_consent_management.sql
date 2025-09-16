-- V14__add_oauth2_consent_management.sql
-- Add OAuth2 consent management tables for user authorization

-- Create OAuth2 consent table
CREATE TABLE oauth2_consents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES oauth_clients(id) ON DELETE CASCADE,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP,
    revocation_reason VARCHAR(255),
    granted_ip VARCHAR(45),
    user_agent VARCHAR(500),
    session_id VARCHAR(100),
    remember_consent BOOLEAN NOT NULL DEFAULT FALSE,
    metadata TEXT,

    -- Ensure unique consent per user-client combination
    CONSTRAINT uk_oauth2_consent_user_client UNIQUE (user_id, client_id)
);

-- Create indexes for OAuth2 consents
CREATE INDEX idx_oauth2_consent_user ON oauth2_consents(user_id);
CREATE INDEX idx_oauth2_consent_client ON oauth2_consents(client_id);
CREATE INDEX idx_oauth2_consent_expires ON oauth2_consents(expires_at) WHERE expires_at IS NOT NULL;
CREATE INDEX idx_oauth2_consent_revoked ON oauth2_consents(revoked) WHERE revoked = FALSE;

-- Create table for granted scopes
CREATE TABLE oauth2_consent_scopes (
    consent_id UUID NOT NULL REFERENCES oauth2_consents(id) ON DELETE CASCADE,
    scope VARCHAR(100) NOT NULL,
    PRIMARY KEY (consent_id, scope)
);

-- Create index for scope lookups
CREATE INDEX idx_oauth2_consent_scopes_consent ON oauth2_consent_scopes(consent_id);

-- Create table for denied scopes
CREATE TABLE oauth2_consent_denied_scopes (
    consent_id UUID NOT NULL REFERENCES oauth2_consents(id) ON DELETE CASCADE,
    scope VARCHAR(100) NOT NULL,
    PRIMARY KEY (consent_id, scope)
);

-- Create index for denied scope lookups
CREATE INDEX idx_oauth2_consent_denied_scopes_consent ON oauth2_consent_denied_scopes(consent_id);

-- Add trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_oauth2_consent_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_oauth2_consent_updated_at
    BEFORE UPDATE ON oauth2_consents
    FOR EACH ROW
    EXECUTE FUNCTION update_oauth2_consent_updated_at();

-- Add consent tracking to OAuth2 audit log indexes
CREATE INDEX IF NOT EXISTS idx_audit_consent_events
    ON oauth2_audit_log(event_type)
    WHERE event_type IN ('USER_CONSENT_GRANTED', 'USER_CONSENT_REVOKED');

-- Add comment for documentation
COMMENT ON TABLE oauth2_consents IS 'Stores user consent decisions for OAuth2 client access';
COMMENT ON TABLE oauth2_consent_scopes IS 'Stores the scopes that users have granted to OAuth2 clients';
COMMENT ON TABLE oauth2_consent_denied_scopes IS 'Stores the scopes that users have explicitly denied to OAuth2 clients';
COMMENT ON COLUMN oauth2_consents.remember_consent IS 'If true, skip consent prompt for future authorization requests';
COMMENT ON COLUMN oauth2_consents.metadata IS 'Additional JSON metadata about the consent decision';