-- Add missing refresh_token_id column to oauth_access_tokens table
-- This column tracks which refresh token was used to generate this access token

ALTER TABLE oauth_access_tokens 
ADD COLUMN refresh_token_id UUID;

-- Add foreign key constraint to oauth_refresh_tokens
ALTER TABLE oauth_access_tokens 
ADD CONSTRAINT fk_oauth_access_token_refresh_token 
FOREIGN KEY (refresh_token_id) REFERENCES oauth_refresh_tokens(id) ON DELETE SET NULL;

-- Add index for performance
CREATE INDEX idx_oauth_access_token_refresh ON oauth_access_tokens(refresh_token_id);

-- Add comment for documentation
COMMENT ON COLUMN oauth_access_tokens.refresh_token_id IS 'Reference to the refresh token used to generate this access token (if applicable)';