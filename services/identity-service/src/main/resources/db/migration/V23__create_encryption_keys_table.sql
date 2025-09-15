-- V23__create_encryption_keys_table.sql
-- Create table for storing encryption keys to ensure persistence across service restarts
-- This is critical for maintaining data integrity and preventing encryption/decryption failures

CREATE TABLE IF NOT EXISTS encryption_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version VARCHAR(100) NOT NULL UNIQUE,
    key_material TEXT NOT NULL, -- Base64 encoded encrypted key material
    salt VARCHAR(255) NOT NULL,
    algorithm VARCHAR(50) NOT NULL DEFAULT 'AES-256-GCM',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    active BOOLEAN NOT NULL DEFAULT true,
    rotated_from VARCHAR(100), -- Previous key version if this was created via rotation
    rotation_reason VARCHAR(255),
    created_by VARCHAR(100) DEFAULT 'system',
    metadata JSONB, -- Additional metadata for audit/compliance

    -- Constraints
    CONSTRAINT uk_encryption_key_version UNIQUE (version),
    CONSTRAINT chk_algorithm CHECK (algorithm IN ('AES-256-GCM', 'AES-256-CBC', 'AES-128-GCM'))
);

-- Indexes for performance
CREATE INDEX idx_encryption_keys_active ON encryption_keys(active) WHERE active = true;
CREATE INDEX idx_encryption_keys_version ON encryption_keys(version);
CREATE INDEX idx_encryption_keys_created_at ON encryption_keys(created_at);
CREATE INDEX idx_encryption_keys_expires_at ON encryption_keys(expires_at) WHERE expires_at IS NOT NULL;

-- Comments for documentation
COMMENT ON TABLE encryption_keys IS 'Stores encryption keys for field-level encryption with automatic rotation support';
COMMENT ON COLUMN encryption_keys.version IS 'Unique version identifier for the key (e.g., v1234567890-ABC123)';
COMMENT ON COLUMN encryption_keys.key_material IS 'Base64 encoded encrypted key material - never store plain keys';
COMMENT ON COLUMN encryption_keys.salt IS 'Salt used for key derivation';
COMMENT ON COLUMN encryption_keys.algorithm IS 'Encryption algorithm used with this key';
COMMENT ON COLUMN encryption_keys.active IS 'Whether this key is currently active for encryption (only one should be active)';
COMMENT ON COLUMN encryption_keys.rotated_from IS 'Previous key version if this key was created via rotation';
COMMENT ON COLUMN encryption_keys.metadata IS 'Additional metadata for compliance and audit purposes';

-- Function to ensure only one active key exists
CREATE OR REPLACE FUNCTION ensure_single_active_key()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.active = true THEN
        -- Deactivate all other keys when setting a new active key
        UPDATE encryption_keys
        SET active = false
        WHERE id != NEW.id AND active = true;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to maintain single active key
CREATE TRIGGER trg_ensure_single_active_key
    BEFORE INSERT OR UPDATE OF active ON encryption_keys
    FOR EACH ROW
    WHEN (NEW.active = true)
    EXECUTE FUNCTION ensure_single_active_key();

-- Audit table for key usage tracking
CREATE TABLE IF NOT EXISTS encryption_key_usage_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key_version VARCHAR(100) NOT NULL,
    operation VARCHAR(50) NOT NULL, -- 'ENCRYPT', 'DECRYPT', 'HASH', 'ROTATE'
    entity_type VARCHAR(100), -- e.g., 'User', 'PersonalData'
    entity_id UUID,
    performed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    performed_by VARCHAR(100),
    success BOOLEAN NOT NULL DEFAULT true,
    error_message TEXT,

    CONSTRAINT fk_key_version
        FOREIGN KEY (key_version)
        REFERENCES encryption_keys(version)
        ON DELETE CASCADE
);

-- Index for audit queries
CREATE INDEX idx_key_usage_audit_version ON encryption_key_usage_audit(key_version);
CREATE INDEX idx_key_usage_audit_performed_at ON encryption_key_usage_audit(performed_at);
CREATE INDEX idx_key_usage_audit_operation ON encryption_key_usage_audit(operation);

COMMENT ON TABLE encryption_key_usage_audit IS 'Audit trail for encryption key usage for compliance and debugging';