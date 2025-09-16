-- V16__add_oauth_client_management_fields.sql
-- Add additional fields for OAuth2 client management

-- Add updated_at column to track client modifications
ALTER TABLE oauth_clients
ADD COLUMN updated_at TIMESTAMP;

-- Add secret_rotated_at column to track secret rotation history
ALTER TABLE oauth_clients
ADD COLUMN secret_rotated_at TIMESTAMP;

-- Update existing rows to set updated_at to created_at
UPDATE oauth_clients
SET updated_at = created_at
WHERE updated_at IS NULL;

-- Create index for client listing and search queries
CREATE INDEX idx_oauth_clients_enabled ON oauth_clients(enabled);
CREATE INDEX idx_oauth_clients_name ON oauth_clients(client_name);
CREATE INDEX idx_oauth_clients_updated ON oauth_clients(updated_at);

-- Add comments for documentation
COMMENT ON COLUMN oauth_clients.updated_at IS 'Timestamp of last client configuration update';
COMMENT ON COLUMN oauth_clients.secret_rotated_at IS 'Timestamp of last client secret rotation';