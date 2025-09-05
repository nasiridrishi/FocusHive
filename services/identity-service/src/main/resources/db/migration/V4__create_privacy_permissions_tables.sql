-- Privacy & Permissions Tables Migration
-- Creates tables for advanced privacy controls and GDPR compliance

-- Create Privacy Settings table
CREATE TABLE privacy_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    persona_id UUID REFERENCES personas(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    setting_value VARCHAR(1000) NOT NULL,
    description VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    data_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
    priority_level INTEGER DEFAULT 0,
    overridable BOOLEAN DEFAULT TRUE,
    source VARCHAR(20) DEFAULT 'user',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Data Permissions table
CREATE TABLE data_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_id UUID REFERENCES oauth_clients(id) ON DELETE CASCADE,
    data_type VARCHAR(50) NOT NULL,
    purpose VARCHAR(1000) NOT NULL,
    legal_basis VARCHAR(50) DEFAULT 'consent',
    retention_period_days INTEGER,
    expires_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    revoked_at TIMESTAMP,
    revocation_reason VARCHAR(500),
    is_internal BOOLEAN NOT NULL DEFAULT FALSE,
    parent_permission_id UUID REFERENCES data_permissions(id) ON DELETE SET NULL,
    granted_ip VARCHAR(45),
    user_agent VARCHAR(500),
    last_used_at TIMESTAMP,
    usage_count BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Data Permission Grants table (many-to-many for permissions)
CREATE TABLE data_permission_grants (
    permission_id UUID NOT NULL REFERENCES data_permissions(id) ON DELETE CASCADE,
    permission VARCHAR(50) NOT NULL,
    PRIMARY KEY (permission_id, permission)
);

-- Create Data Permission Conditions table (additional constraints)
CREATE TABLE data_permission_conditions (
    permission_id UUID NOT NULL REFERENCES data_permissions(id) ON DELETE CASCADE,
    condition_key VARCHAR(100) NOT NULL,
    condition_value VARCHAR(500) NOT NULL,
    PRIMARY KEY (permission_id, condition_key)
);

-- Create Consent Records table
CREATE TABLE consent_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    consent_type VARCHAR(100) NOT NULL,
    purpose VARCHAR(1000) NOT NULL,
    legal_basis VARCHAR(50) NOT NULL,
    consent_given BOOLEAN NOT NULL,
    consent_version VARCHAR(20),
    consent_source VARCHAR(100),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    expires_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    withdrawn_at TIMESTAMP,
    withdrawal_reason VARCHAR(500),
    superseded_at TIMESTAMP,
    parent_consent_id UUID REFERENCES consent_records(id) ON DELETE SET NULL,
    geographic_location VARCHAR(100),
    consent_language VARCHAR(10),
    verification_method VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Consent Record Metadata table
CREATE TABLE consent_record_metadata (
    consent_id UUID NOT NULL REFERENCES consent_records(id) ON DELETE CASCADE,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value VARCHAR(500) NOT NULL,
    PRIMARY KEY (consent_id, metadata_key)
);

-- Create unique constraints for privacy settings
ALTER TABLE privacy_settings 
ADD CONSTRAINT uk_privacy_user_category_key 
UNIQUE (user_id, persona_id, category, setting_key);

-- Create indexes for Privacy Settings
CREATE INDEX idx_privacy_setting_user ON privacy_settings(user_id);
CREATE INDEX idx_privacy_setting_persona ON privacy_settings(persona_id);
CREATE INDEX idx_privacy_setting_category ON privacy_settings(category);
CREATE INDEX idx_privacy_setting_enabled ON privacy_settings(enabled);
CREATE INDEX idx_privacy_setting_data_type ON privacy_settings(data_type);
CREATE INDEX idx_privacy_setting_source ON privacy_settings(source);

-- Create indexes for Data Permissions
CREATE INDEX idx_data_permission_user ON data_permissions(user_id);
CREATE INDEX idx_data_permission_client ON data_permissions(client_id);
CREATE INDEX idx_data_permission_type ON data_permissions(data_type);
CREATE INDEX idx_data_permission_active ON data_permissions(active);
CREATE INDEX idx_data_permission_expires ON data_permissions(expires_at);
CREATE INDEX idx_data_permission_revoked ON data_permissions(revoked_at);
CREATE INDEX idx_data_permission_internal ON data_permissions(is_internal);
CREATE INDEX idx_data_permission_legal_basis ON data_permissions(legal_basis);
CREATE INDEX idx_data_permission_parent ON data_permissions(parent_permission_id);
CREATE INDEX idx_data_permission_created ON data_permissions(created_at);

-- Create indexes for Consent Records
CREATE INDEX idx_consent_record_user ON consent_records(user_id);
CREATE INDEX idx_consent_record_type ON consent_records(consent_type);
CREATE INDEX idx_consent_record_active ON consent_records(active);
CREATE INDEX idx_consent_record_given ON consent_records(consent_given);
CREATE INDEX idx_consent_record_expires ON consent_records(expires_at);
CREATE INDEX idx_consent_record_withdrawn ON consent_records(withdrawn_at);
CREATE INDEX idx_consent_record_basis ON consent_records(legal_basis);
CREATE INDEX idx_consent_record_version ON consent_records(consent_version);
CREATE INDEX idx_consent_record_source ON consent_records(consent_source);
CREATE INDEX idx_consent_record_parent ON consent_records(parent_consent_id);
CREATE INDEX idx_consent_record_created ON consent_records(created_at);

-- Add performance optimization indexes
CREATE INDEX idx_privacy_setting_user_enabled ON privacy_settings(user_id, enabled) 
WHERE enabled = TRUE;

CREATE INDEX idx_data_permission_user_active ON data_permissions(user_id, active, expires_at) 
WHERE active = TRUE;

CREATE INDEX idx_consent_record_user_active ON consent_records(user_id, active, consent_given) 
WHERE active = TRUE AND consent_given = TRUE;

-- Add partial indexes for common queries
CREATE INDEX idx_data_permission_client_active ON data_permissions(client_id, data_type) 
WHERE active = TRUE AND is_internal = FALSE;

CREATE INDEX idx_consent_record_type_effective ON consent_records(consent_type, user_id) 
WHERE active = TRUE AND consent_given = TRUE AND withdrawn_at IS NULL;

-- Add constraints for data validation
ALTER TABLE privacy_settings 
ADD CONSTRAINT chk_privacy_setting_data_type 
CHECK (data_type IN ('STRING', 'BOOLEAN', 'NUMBER', 'JSON'));

ALTER TABLE privacy_settings 
ADD CONSTRAINT chk_privacy_setting_source 
CHECK (source IN ('user', 'admin', 'system', 'imported'));

ALTER TABLE data_permissions 
ADD CONSTRAINT chk_data_permission_legal_basis 
CHECK (legal_basis IN ('consent', 'contract', 'legitimate_interest', 'vital_interests', 'public_task', 'legal_obligation'));

ALTER TABLE data_permissions 
ADD CONSTRAINT chk_data_permission_retention_positive 
CHECK (retention_period_days IS NULL OR retention_period_days > 0);

ALTER TABLE consent_records 
ADD CONSTRAINT chk_consent_record_legal_basis 
CHECK (legal_basis IN ('consent', 'contract', 'legitimate_interest', 'vital_interests', 'public_task', 'legal_obligation'));

-- Add constraints for logical consistency
ALTER TABLE data_permissions 
ADD CONSTRAINT chk_data_permission_revocation 
CHECK ((active = TRUE AND revoked_at IS NULL) OR (active = FALSE AND revoked_at IS NOT NULL));

ALTER TABLE consent_records 
ADD CONSTRAINT chk_consent_record_withdrawal 
CHECK ((consent_given = TRUE AND withdrawn_at IS NULL) OR (consent_given = FALSE) OR (withdrawn_at IS NOT NULL));

ALTER TABLE consent_records 
ADD CONSTRAINT chk_consent_record_superseded 
CHECK ((active = TRUE AND superseded_at IS NULL) OR (active = FALSE) OR (superseded_at IS NOT NULL));

-- Add foreign key constraints with proper cascading
ALTER TABLE privacy_settings 
ADD CONSTRAINT fk_privacy_setting_persona 
FOREIGN KEY (persona_id) REFERENCES personas(id) ON DELETE CASCADE;

-- Create triggers for updated_at timestamps
CREATE TRIGGER update_privacy_settings_updated_at BEFORE UPDATE ON privacy_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_data_permissions_updated_at BEFORE UPDATE ON data_permissions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add cleanup indexes for maintenance
CREATE INDEX idx_privacy_setting_cleanup ON privacy_settings(updated_at) 
WHERE enabled = FALSE;

CREATE INDEX idx_data_permission_cleanup ON data_permissions(revoked_at, retention_period_days, created_at) 
WHERE active = FALSE;

CREATE INDEX idx_consent_record_cleanup ON consent_records(withdrawn_at, expires_at) 
WHERE active = FALSE;

-- Add GDPR compliance indexes
CREATE INDEX idx_data_permission_gdpr_export ON data_permissions(user_id, data_type, active, created_at);

CREATE INDEX idx_consent_record_gdpr_export ON consent_records(user_id, consent_type, legal_basis, created_at);

CREATE INDEX idx_privacy_setting_gdpr_export ON privacy_settings(user_id, category, setting_key, created_at);

-- Add comments for documentation
COMMENT ON TABLE privacy_settings IS 'Granular privacy settings supporting user and persona-level controls';
COMMENT ON TABLE data_permissions IS 'GDPR-compliant data access permissions with retention policies';
COMMENT ON TABLE consent_records IS 'Complete consent audit trail for GDPR compliance';

COMMENT ON COLUMN privacy_settings.persona_id IS 'NULL for user-level settings, specific persona for persona-level settings';
COMMENT ON COLUMN data_permissions.legal_basis IS 'GDPR Article 6 legal basis for data processing';
COMMENT ON COLUMN data_permissions.retention_period_days IS 'Data retention period in days, NULL for indefinite';
COMMENT ON COLUMN consent_records.legal_basis IS 'GDPR legal basis for processing personal data';
COMMENT ON COLUMN consent_records.parent_consent_id IS 'Reference to previous consent for renewal tracking';