-- V13__add_oauth2_rate_limiting.sql
-- Add rate limiting configuration to OAuth2 clients

-- Add rate limiting columns to oauth_clients table
ALTER TABLE oauth_clients
ADD COLUMN IF NOT EXISTS rate_limit_override INTEGER,
ADD COLUMN IF NOT EXISTS rate_limit_window_minutes INTEGER,
ADD COLUMN IF NOT EXISTS trusted BOOLEAN NOT NULL DEFAULT FALSE;

-- Create index for trusted clients for faster queries
CREATE INDEX IF NOT EXISTS idx_oauth_clients_trusted ON oauth_clients(trusted) WHERE trusted = TRUE;

-- Comment on new columns
COMMENT ON COLUMN oauth_clients.rate_limit_override IS 'Custom rate limit for this client (requests per window)';
COMMENT ON COLUMN oauth_clients.rate_limit_window_minutes IS 'Custom rate limit window duration in minutes';
COMMENT ON COLUMN oauth_clients.trusted IS 'Whether this client is trusted (gets higher rate limits)';

-- Table for tracking rate limit violations and suspensions
CREATE TABLE IF NOT EXISTS oauth2_rate_limit_violations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id VARCHAR(100) NOT NULL,
    endpoint VARCHAR(50) NOT NULL,
    violation_count BIGINT NOT NULL DEFAULT 1,
    first_violation TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_violation TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    suspended_until TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_violations_client
        FOREIGN KEY (client_id)
        REFERENCES oauth_clients(client_id)
        ON DELETE CASCADE
);

-- Indexes for violation tracking
CREATE INDEX IF NOT EXISTS idx_violations_client_id ON oauth2_rate_limit_violations(client_id);
CREATE INDEX IF NOT EXISTS idx_violations_client_endpoint ON oauth2_rate_limit_violations(client_id, endpoint);
CREATE INDEX IF NOT EXISTS idx_violations_suspended ON oauth2_rate_limit_violations(suspended_until)
    WHERE suspended_until IS NOT NULL;

-- Function to clean up old violation records
CREATE OR REPLACE FUNCTION cleanup_old_violations() RETURNS void AS $$
BEGIN
    -- Delete violation records older than 7 days
    DELETE FROM oauth2_rate_limit_violations
    WHERE last_violation < CURRENT_TIMESTAMP - INTERVAL '7 days';
END;
$$ LANGUAGE plpgsql;

-- Default rate limit configuration table
CREATE TABLE IF NOT EXISTS oauth2_rate_limit_config (
    id SERIAL PRIMARY KEY,
    endpoint VARCHAR(50) NOT NULL UNIQUE,
    default_limit INTEGER NOT NULL,
    default_window_minutes INTEGER NOT NULL,
    anonymous_limit INTEGER NOT NULL,
    trusted_multiplier DECIMAL(3,1) NOT NULL DEFAULT 2.0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insert default rate limit configurations
INSERT INTO oauth2_rate_limit_config (endpoint, default_limit, default_window_minutes, anonymous_limit)
VALUES
    ('authorize', 10, 1, 5),
    ('token', 60, 1, 20),
    ('introspect', 100, 1, 30),
    ('revoke', 30, 1, 10),
    ('userinfo', 60, 1, 20),
    ('jwks', 100, 5, 50),
    ('discovery', 100, 5, 50),
    ('client_registration', 5, 10, 2)
ON CONFLICT (endpoint) DO NOTHING;

-- Update some existing trusted clients (example - adjust based on your needs)
-- UPDATE oauth_clients SET trusted = TRUE WHERE client_id IN ('admin-client', 'internal-service');

-- Add comments
COMMENT ON TABLE oauth2_rate_limit_violations IS 'Tracks rate limit violations for OAuth2 clients';
COMMENT ON TABLE oauth2_rate_limit_config IS 'Default rate limit configuration for OAuth2 endpoints';