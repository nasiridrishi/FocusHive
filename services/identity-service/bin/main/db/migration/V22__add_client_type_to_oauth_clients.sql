-- Add missing client_type column to oauth_clients table
-- This column defines whether the OAuth client is confidential or public

ALTER TABLE oauth_clients 
ADD COLUMN client_type VARCHAR(20) NOT NULL DEFAULT 'CONFIDENTIAL';

-- Add check constraint to ensure only valid client types
ALTER TABLE oauth_clients
ADD CONSTRAINT chk_oauth_client_type 
CHECK (client_type IN ('CONFIDENTIAL', 'PUBLIC'));

-- Add index for performance if querying by client type
CREATE INDEX idx_oauth_clients_type ON oauth_clients(client_type);

-- Add comment for documentation
COMMENT ON COLUMN oauth_clients.client_type IS 'OAuth2 client type: CONFIDENTIAL for server-side apps, PUBLIC for SPAs/mobile apps';