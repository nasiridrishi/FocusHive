-- Add missing columns to users table that are required by User entity
-- This migration adds account deletion tracking, phone number, and token invalidation fields

-- Add account deletion and recovery fields
ALTER TABLE users
ADD COLUMN IF NOT EXISTS account_deleted_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS deletion_token VARCHAR(255);

-- Add phone number fields with encryption support
ALTER TABLE users
ADD COLUMN IF NOT EXISTS phone_number VARCHAR(500),  -- Encrypted phone number
ADD COLUMN IF NOT EXISTS phone_number_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Add token invalidation timestamp for session management
ALTER TABLE users
ADD COLUMN IF NOT EXISTS token_invalidated_at TIMESTAMP;

-- Create indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_users_deletion_token ON users(deletion_token) WHERE deletion_token IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_account_deleted_at ON users(account_deleted_at) WHERE account_deleted_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_token_invalidated_at ON users(token_invalidated_at) WHERE token_invalidated_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_phone_number_verified ON users(phone_number_verified) WHERE phone_number_verified = TRUE;

-- Add comments for documentation
COMMENT ON COLUMN users.account_deleted_at IS 'Timestamp when account deletion was initiated';
COMMENT ON COLUMN users.deletion_token IS 'Token for account recovery within grace period';
COMMENT ON COLUMN users.phone_number IS 'Encrypted phone number (AES-256-GCM)';
COMMENT ON COLUMN users.phone_number_verified IS 'Whether phone number has been verified';
COMMENT ON COLUMN users.token_invalidated_at IS 'Timestamp after which all tokens issued before this time are invalid';