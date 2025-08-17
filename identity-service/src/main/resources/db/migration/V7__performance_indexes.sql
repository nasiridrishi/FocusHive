-- V7__performance_indexes.sql
-- Performance optimization indexes for identity service

-- Users table indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_active ON users(active);
CREATE INDEX IF NOT EXISTS idx_users_email_verified ON users(email_verified);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

-- User personas indexes
CREATE INDEX IF NOT EXISTS idx_user_personas_user_id ON user_personas(user_id);
CREATE INDEX IF NOT EXISTS idx_user_personas_persona_name ON user_personas(persona_name);
CREATE INDEX IF NOT EXISTS idx_user_personas_active ON user_personas(active);
CREATE INDEX IF NOT EXISTS idx_user_personas_user_name ON user_personas(user_id, persona_name);

-- User profiles indexes
CREATE INDEX IF NOT EXISTS idx_user_profiles_user_id ON user_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_profiles_persona_id ON user_profiles(persona_id);
CREATE INDEX IF NOT EXISTS idx_user_profiles_profile_type ON user_profiles(profile_type);
CREATE INDEX IF NOT EXISTS idx_user_profiles_user_persona ON user_profiles(user_id, persona_id);

-- Privacy permissions indexes
CREATE INDEX IF NOT EXISTS idx_privacy_permissions_user_id ON privacy_permissions(user_id);
CREATE INDEX IF NOT EXISTS idx_privacy_permissions_resource ON privacy_permissions(resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_privacy_permissions_granted_to ON privacy_permissions(granted_to_type, granted_to_id);

-- OAuth2 authorization indexes
CREATE INDEX IF NOT EXISTS idx_oauth2_authorization_registered_client_id ON oauth2_authorization(registered_client_id);
CREATE INDEX IF NOT EXISTS idx_oauth2_authorization_principal_name ON oauth2_authorization(principal_name);
CREATE INDEX IF NOT EXISTS idx_oauth2_authorization_state ON oauth2_authorization(state);
CREATE INDEX IF NOT EXISTS idx_oauth2_authorization_authorization_code_value ON oauth2_authorization(authorization_code_value);
CREATE INDEX IF NOT EXISTS idx_oauth2_authorization_access_token_value ON oauth2_authorization(access_token_value);
CREATE INDEX IF NOT EXISTS idx_oauth2_authorization_refresh_token_value ON oauth2_authorization(refresh_token_value);

-- OAuth2 authorization consent indexes
CREATE INDEX IF NOT EXISTS idx_oauth2_authorization_consent_registered_client_id ON oauth2_authorization_consent(registered_client_id);
CREATE INDEX IF NOT EXISTS idx_oauth2_authorization_consent_principal_name ON oauth2_authorization_consent(principal_name);

-- User activity logs indexes
CREATE INDEX IF NOT EXISTS idx_user_activity_logs_user_id ON user_activity_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_user_activity_logs_activity_type ON user_activity_logs(activity_type);
CREATE INDEX IF NOT EXISTS idx_user_activity_logs_timestamp ON user_activity_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_user_activity_logs_user_timestamp ON user_activity_logs(user_id, timestamp);

-- Compliance audit indexes
CREATE INDEX IF NOT EXISTS idx_compliance_audit_user_id ON compliance_audit(user_id);
CREATE INDEX IF NOT EXISTS idx_compliance_audit_audit_type ON compliance_audit(audit_type);
CREATE INDEX IF NOT EXISTS idx_compliance_audit_timestamp ON compliance_audit(timestamp);
CREATE INDEX IF NOT EXISTS idx_compliance_audit_data_subject_id ON compliance_audit(data_subject_id);

-- Partial indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_active_true ON users(id, email, username) WHERE active = true;
CREATE INDEX IF NOT EXISTS idx_users_email_verified_true ON users(id, email) WHERE email_verified = true;
CREATE INDEX IF NOT EXISTS idx_user_personas_active_true ON user_personas(user_id, persona_name) WHERE active = true;

-- Function-based indexes for case-insensitive searches
CREATE INDEX IF NOT EXISTS idx_users_email_lower ON users(LOWER(email));
CREATE INDEX IF NOT EXISTS idx_users_username_lower ON users(LOWER(username));