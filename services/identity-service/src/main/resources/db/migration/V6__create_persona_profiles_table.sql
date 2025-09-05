-- Persona Profiles Tables Migration
-- Creates detailed profile management for personas with flexible metadata

-- Create Persona Profiles table
CREATE TABLE persona_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    persona_id UUID NOT NULL REFERENCES personas(id) ON DELETE CASCADE,
    profile_key VARCHAR(100) NOT NULL,
    profile_value VARCHAR(2000) NOT NULL,
    category VARCHAR(50),
    data_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    description VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER DEFAULT 0,
    required_field BOOLEAN DEFAULT FALSE,
    user_editable BOOLEAN DEFAULT TRUE,
    source VARCHAR(50) DEFAULT 'user_input',
    verified_at TIMESTAMP,
    verification_method VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Persona Profile Metadata table
CREATE TABLE persona_profile_metadata (
    profile_id UUID NOT NULL REFERENCES persona_profiles(id) ON DELETE CASCADE,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value VARCHAR(500) NOT NULL,
    PRIMARY KEY (profile_id, metadata_key)
);

-- Create Persona Profile Validation table
CREATE TABLE persona_profile_validation (
    profile_id UUID NOT NULL REFERENCES persona_profiles(id) ON DELETE CASCADE,
    rule_key VARCHAR(100) NOT NULL,
    rule_value VARCHAR(500) NOT NULL,
    PRIMARY KEY (profile_id, rule_key)
);

-- Create Persona Profile Source Metadata table
CREATE TABLE persona_profile_source_metadata (
    profile_id UUID NOT NULL REFERENCES persona_profiles(id) ON DELETE CASCADE,
    source_key VARCHAR(100) NOT NULL,
    source_value VARCHAR(500) NOT NULL,
    PRIMARY KEY (profile_id, source_key)
);

-- Create unique constraint for persona profile keys
ALTER TABLE persona_profiles 
ADD CONSTRAINT uk_persona_profile_key 
UNIQUE (persona_id, profile_key);

-- Create indexes for Persona Profiles
CREATE INDEX idx_persona_profile_persona ON persona_profiles(persona_id);
CREATE INDEX idx_persona_profile_category ON persona_profiles(category);
CREATE INDEX idx_persona_profile_visibility ON persona_profiles(visibility);
CREATE INDEX idx_persona_profile_enabled ON persona_profiles(enabled);
CREATE INDEX idx_persona_profile_data_type ON persona_profiles(data_type);
CREATE INDEX idx_persona_profile_source ON persona_profiles(source);
CREATE INDEX idx_persona_profile_required ON persona_profiles(required_field);
CREATE INDEX idx_persona_profile_order ON persona_profiles(display_order);
CREATE INDEX idx_persona_profile_verified ON persona_profiles(verified_at);
CREATE INDEX idx_persona_profile_created ON persona_profiles(created_at);
CREATE INDEX idx_persona_profile_updated ON persona_profiles(updated_at);

-- Create performance optimization indexes
CREATE INDEX idx_persona_profile_persona_enabled ON persona_profiles(persona_id, enabled) 
WHERE enabled = TRUE;

CREATE INDEX idx_persona_profile_public ON persona_profiles(persona_id, profile_key) 
WHERE visibility = 'PUBLIC' AND enabled = TRUE;

CREATE INDEX idx_persona_profile_category_enabled ON persona_profiles(category, enabled) 
WHERE enabled = TRUE AND category IS NOT NULL;

-- Create partial indexes for common queries
CREATE INDEX idx_persona_profile_verification_needed ON persona_profiles(persona_id, data_type, profile_key) 
WHERE data_type IN ('EMAIL', 'PHONE') AND verified_at IS NULL AND enabled = TRUE;

CREATE INDEX idx_persona_profile_required_missing ON persona_profiles(persona_id, profile_key) 
WHERE required_field = TRUE AND (profile_value IS NULL OR profile_value = '');

CREATE INDEX idx_persona_profile_user_editable ON persona_profiles(persona_id, category) 
WHERE user_editable = TRUE AND enabled = TRUE;

-- Add constraints for data validation
ALTER TABLE persona_profiles 
ADD CONSTRAINT chk_persona_profile_data_type 
CHECK (data_type IN ('STRING', 'NUMBER', 'BOOLEAN', 'DATE', 'JSON', 'EMAIL', 'PHONE', 'URL'));

ALTER TABLE persona_profiles 
ADD CONSTRAINT chk_persona_profile_visibility 
CHECK (visibility IN ('PUBLIC', 'FRIENDS', 'PRIVATE'));

ALTER TABLE persona_profiles 
ADD CONSTRAINT chk_persona_profile_source 
CHECK (source IN ('user_input', 'imported', 'calculated', 'system', 'copied'));

ALTER TABLE persona_profiles 
ADD CONSTRAINT chk_persona_profile_display_order 
CHECK (display_order >= 0);

ALTER TABLE persona_profiles 
ADD CONSTRAINT chk_persona_profile_key_not_empty 
CHECK (LENGTH(TRIM(profile_key)) > 0);

ALTER TABLE persona_profiles 
ADD CONSTRAINT chk_persona_profile_value_not_empty 
CHECK (LENGTH(TRIM(profile_value)) > 0);

-- Add constraint for verified profiles
ALTER TABLE persona_profiles 
ADD CONSTRAINT chk_persona_profile_verification 
CHECK ((verified_at IS NULL AND verification_method IS NULL) OR 
       (verified_at IS NOT NULL AND verification_method IS NOT NULL));

-- Create trigger for updated_at timestamp
CREATE TRIGGER update_persona_profiles_updated_at BEFORE UPDATE ON persona_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create indexes for metadata tables
CREATE INDEX idx_persona_profile_metadata_key ON persona_profile_metadata(metadata_key);
CREATE INDEX idx_persona_profile_validation_key ON persona_profile_validation(rule_key);
CREATE INDEX idx_persona_profile_source_key ON persona_profile_source_metadata(source_key);

-- Create specialized indexes for common profile types
CREATE INDEX idx_persona_profile_contact_info ON persona_profiles(persona_id, data_type, verified_at) 
WHERE data_type IN ('EMAIL', 'PHONE') AND enabled = TRUE;

CREATE INDEX idx_persona_profile_social_links ON persona_profiles(persona_id, profile_key) 
WHERE data_type = 'URL' AND category = 'social' AND enabled = TRUE;

CREATE INDEX idx_persona_profile_work_info ON persona_profiles(persona_id, profile_key, visibility) 
WHERE category = 'work' AND enabled = TRUE;

-- Create indexes for profile search and discovery
CREATE INDEX idx_persona_profile_searchable ON persona_profiles(profile_key, profile_value) 
WHERE visibility IN ('PUBLIC', 'FRIENDS') AND enabled = TRUE AND data_type = 'STRING';

-- Create compliance and export indexes
CREATE INDEX idx_persona_profile_gdpr_export ON persona_profiles(persona_id, category, created_at, data_type);

CREATE INDEX idx_persona_profile_data_retention ON persona_profiles(created_at, updated_at, source) 
WHERE source != 'system';

-- Create maintenance indexes
CREATE INDEX idx_persona_profile_cleanup ON persona_profiles(enabled, updated_at) 
WHERE enabled = FALSE;

CREATE INDEX idx_persona_profile_stale ON persona_profiles(updated_at, data_type) 
WHERE data_type IN ('EMAIL', 'PHONE') AND verified_at IS NULL;

-- Add check constraint for profile value validation based on data type
-- Note: This is a basic validation - more sophisticated validation would be done at application level
ALTER TABLE persona_profiles 
ADD CONSTRAINT chk_persona_profile_email_format 
CHECK (data_type != 'EMAIL' OR profile_value ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

ALTER TABLE persona_profiles 
ADD CONSTRAINT chk_persona_profile_url_format 
CHECK (data_type != 'URL' OR profile_value ~* '^https?://.*');

-- Create view for public profiles (commonly queried)
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
AND pp.enabled = TRUE 
AND p.is_active = TRUE 
AND u.enabled = TRUE 
AND u.deleted_at IS NULL;

-- Create view for profile verification status
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

-- Add comments for documentation
COMMENT ON TABLE persona_profiles IS 'Flexible profile storage for personas with validation and metadata support';
COMMENT ON TABLE persona_profile_metadata IS 'Additional metadata for persona profile fields';
COMMENT ON TABLE persona_profile_validation IS 'Validation rules for persona profile fields';
COMMENT ON TABLE persona_profile_source_metadata IS 'Metadata about how profile data was obtained';

COMMENT ON COLUMN persona_profiles.profile_key IS 'Unique key for the profile field within the persona';
COMMENT ON COLUMN persona_profiles.profile_value IS 'The actual profile value stored as text';
COMMENT ON COLUMN persona_profiles.data_type IS 'Data type for validation: STRING, NUMBER, BOOLEAN, DATE, JSON, EMAIL, PHONE, URL';
COMMENT ON COLUMN persona_profiles.visibility IS 'Who can see this profile field: PUBLIC, FRIENDS, PRIVATE';
COMMENT ON COLUMN persona_profiles.source IS 'How this data was obtained: user_input, imported, calculated, system, copied';
COMMENT ON COLUMN persona_profiles.verified_at IS 'When this profile field was verified (for EMAIL, PHONE, etc.)';

COMMENT ON VIEW public_persona_profiles IS 'Public persona profiles with persona and user context';
COMMENT ON VIEW persona_profile_verification_status IS 'Summary of profile completeness and verification status by persona';

-- Create function for profile value validation (can be extended)
CREATE OR REPLACE FUNCTION validate_persona_profile_value(p_data_type VARCHAR, p_value VARCHAR)
RETURNS BOOLEAN AS $$
BEGIN
    CASE p_data_type
        WHEN 'BOOLEAN' THEN
            RETURN p_value ~* '^(true|false)$';
        WHEN 'NUMBER' THEN
            RETURN p_value ~ '^-?[0-9]+(\.[0-9]+)?$';
        WHEN 'EMAIL' THEN
            RETURN p_value ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$';
        WHEN 'PHONE' THEN
            RETURN p_value ~ '^[+]?[0-9\s\-\(\)]+$';
        WHEN 'URL' THEN
            RETURN p_value ~* '^https?://.*';
        WHEN 'JSON' THEN
            BEGIN
                PERFORM p_value::JSON;
                RETURN TRUE;
            EXCEPTION WHEN OTHERS THEN
                RETURN FALSE;
            END;
        WHEN 'DATE' THEN
            BEGIN
                PERFORM p_value::TIMESTAMP;
                RETURN TRUE;
            EXCEPTION WHEN OTHERS THEN
                RETURN FALSE;
            END;
        ELSE
            -- STRING and other types are always valid
            RETURN TRUE;
    END CASE;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

COMMENT ON FUNCTION validate_persona_profile_value IS 'Validates profile values based on their data type';