-- V10: Add field-level encryption support for PII data
-- This migration adds support for encrypted PII fields and searchable hash columns

-- Drop views that depend on columns being altered
-- These views were created in V5 and V6 and need to be dropped before altering column types
DROP VIEW IF EXISTS recent_export_requests;
DROP VIEW IF EXISTS active_security_events;
DROP VIEW IF EXISTS public_persona_profiles;
DROP VIEW IF EXISTS persona_profile_verification_status;

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

-- Recreate views that were dropped at the beginning of this migration
-- These views need to be recreated with the new column types

-- Recreate view from V5: active_security_events
CREATE VIEW active_security_events AS
SELECT
    al.*,
    u.username,
    oc.client_name
FROM audit_logs al
LEFT JOIN users u ON al.user_id = u.id
LEFT JOIN oauth_clients oc ON al.client_id = oc.id
WHERE al.event_category IN ('SECURITY', 'AUTHENTICATION', 'AUTHORIZATION');

-- Recreate view from V5: recent_export_requests
CREATE VIEW recent_export_requests AS
SELECT
    der.*,
    u.username,
    u.email
FROM data_export_requests der
JOIN users u ON der.user_id = u.id;

-- Recreate view from V6: public_persona_profiles
CREATE VIEW public_persona_profiles AS
SELECT
    pp.*,
    p.name as persona_name,
    p.type as persona_type,
    u.username
FROM persona_profiles pp
JOIN personas p ON pp.persona_id = p.id
JOIN users u ON p.user_id = u.id
WHERE pp.visibility = 'PUBLIC'
AND pp.enabled = TRUE;

-- Recreate view from V6: persona_profile_verification_status
CREATE VIEW persona_profile_verification_status AS
SELECT
    pp.persona_id,
    COUNT(*) as total_profiles,
    COUNT(CASE WHEN pp.data_type IN ('EMAIL', 'PHONE') THEN 1 END) as verifiable_profiles,
    COUNT(CASE WHEN pp.verified_at IS NOT NULL THEN 1 END) as verified_profiles,
    COUNT(CASE WHEN pp.required_field = TRUE THEN 1 END) as required_profiles,
    COUNT(CASE WHEN pp.required_field = TRUE AND (pp.profile_value IS NULL OR pp.profile_value = '') THEN 1 END) as missing_required_profiles
FROM persona_profiles pp
WHERE pp.enabled = TRUE
GROUP BY pp.persona_id;

-- Migration verification query
DO $$
BEGIN
    RAISE NOTICE 'Field-level encryption support migration completed.';
    RAISE NOTICE 'IMPORTANT: Run the application data encryption migration tool to encrypt existing PII data.';
END $$;