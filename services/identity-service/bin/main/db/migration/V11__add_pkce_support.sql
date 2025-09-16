-- Add PKCE support column to OAuth clients table
ALTER TABLE oauth_clients
ADD COLUMN require_pkce BOOLEAN NOT NULL DEFAULT FALSE;

-- Update existing clients to not require PKCE by default
UPDATE oauth_clients SET require_pkce = FALSE WHERE require_pkce IS NULL;

-- Add comment for documentation
COMMENT ON COLUMN oauth_clients.require_pkce IS 'Whether this client requires PKCE (Proof Key for Code Exchange) for authorization code flow';