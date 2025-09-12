-- V10: Add field-level encryption support for PII data
-- This migration adds support for encrypted PII fields and searchable hash columns

-- Add email_hash column for searchable encrypted email
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS email_hash VARCHAR(64);

-- Create index on email_hash for efficient searching
CREATE INDEX IF NOT EXISTS idx_user_email_hash 
ON users(email_hash);

-- Increase column sizes for encrypted data (encrypted data is larger than plaintext)
-- Note: We're being conservative with sizes to accommodate Base64 encoded encrypted data

-- Users table: Increase size of encrypted columns
ALTER TABLE users 
ALTER COLUMN email TYPE VARCHAR(500),  -- Was unique constraint, encrypted will be larger
ALTER COLUMN first_name TYPE VARCHAR(500),  -- Was 50, now encrypted
ALTER COLUMN last_name TYPE VARCHAR(500),  -- Was 50, now encrypted
ALTER COLUMN two_factor_secret TYPE VARCHAR(500),  -- Encrypted secret
ALTER COLUMN last_login_ip TYPE VARCHAR(500);  -- Was 45, now encrypted

-- Personas table: Increase size of encrypted columns
ALTER TABLE personas
ALTER COLUMN display_name TYPE VARCHAR(500),  -- Was 100, now encrypted
ALTER COLUMN bio TYPE VARCHAR(2000),  -- Was 500, now encrypted
ALTER COLUMN status_message TYPE VARCHAR(1000);  -- Was 200, now encrypted

-- Note: customAttributes and notificationPreferences are already stored as JSON/JSONB
-- and will be encrypted as complete JSON strings

-- Add comment to document encryption
COMMENT ON COLUMN users.email IS 'Encrypted email address (AES-256-GCM)';
COMMENT ON COLUMN users.email_hash IS 'SHA-256 hash of lowercase email for searching';
COMMENT ON COLUMN users.first_name IS 'Encrypted first name (AES-256-GCM)';
COMMENT ON COLUMN users.last_name IS 'Encrypted last name (AES-256-GCM)';
COMMENT ON COLUMN users.two_factor_secret IS 'Encrypted 2FA secret (AES-256-GCM)';
COMMENT ON COLUMN users.last_login_ip IS 'Encrypted IP address (AES-256-GCM)';

COMMENT ON COLUMN personas.display_name IS 'Encrypted display name (AES-256-GCM)';
COMMENT ON COLUMN personas.bio IS 'Encrypted biography (AES-256-GCM)';
COMMENT ON COLUMN personas.status_message IS 'Encrypted status message (AES-256-GCM)';

-- Create a function to generate email hash for existing data
-- This will be used in the data migration process
CREATE OR REPLACE FUNCTION generate_email_hash(email_input TEXT) 
RETURNS TEXT AS $$
BEGIN
    -- This is a placeholder - actual hashing will be done by the application
    -- during the data migration process
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Note: Existing data migration must be handled by the application layer
-- since encryption requires the application's encryption key.
-- A separate data migration tool should be run after this schema migration
-- to encrypt existing PII data and generate email hashes.

-- Migration verification query
DO $$
BEGIN
    RAISE NOTICE 'Field-level encryption support migration completed.';
    RAISE NOTICE 'IMPORTANT: Run the application data encryption migration tool to encrypt existing PII data.';
END $$;