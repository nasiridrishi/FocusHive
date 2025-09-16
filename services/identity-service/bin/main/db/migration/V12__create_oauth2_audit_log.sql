-- V12__create_oauth2_audit_log.sql
-- Create OAuth2 audit log table for comprehensive security logging

CREATE TABLE oauth2_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Event information
    event_type VARCHAR(50) NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Actor information
    user_id UUID,
    username VARCHAR(100),
    client_id VARCHAR(100),
    client_name VARCHAR(255),
    
    -- Request information
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    request_id VARCHAR(100),
    session_id VARCHAR(100),
    
    -- OAuth2 specific
    grant_type VARCHAR(50),
    scope VARCHAR(500),
    redirect_uri VARCHAR(500),
    response_type VARCHAR(50),
    token_id VARCHAR(100),
    authorization_code VARCHAR(255),
    
    -- Result information
    success BOOLEAN NOT NULL,
    error_code VARCHAR(100),
    error_description VARCHAR(500),
    http_status INTEGER,
    
    -- Additional context (JSONB for flexibility)
    additional_data JSONB,
    
    -- Compliance and security
    geolocation VARCHAR(255),
    suspicious_activity BOOLEAN DEFAULT FALSE,
    threat_indicators VARCHAR(500),
    
    -- Performance metrics
    processing_time_ms BIGINT
);

-- Indexes for efficient querying
CREATE INDEX idx_audit_timestamp ON oauth2_audit_log(timestamp DESC);
CREATE INDEX idx_audit_event_type ON oauth2_audit_log(event_type);
CREATE INDEX idx_audit_client_id ON oauth2_audit_log(client_id);
CREATE INDEX idx_audit_user_id ON oauth2_audit_log(user_id);
CREATE INDEX idx_audit_risk_level ON oauth2_audit_log(risk_level);
CREATE INDEX idx_audit_success ON oauth2_audit_log(success);
CREATE INDEX idx_audit_suspicious ON oauth2_audit_log(suspicious_activity);
CREATE INDEX idx_audit_ip_address ON oauth2_audit_log(ip_address);

-- Composite indexes for common queries
CREATE INDEX idx_audit_client_timestamp ON oauth2_audit_log(client_id, timestamp DESC);
CREATE INDEX idx_audit_user_timestamp ON oauth2_audit_log(user_id, timestamp DESC);
CREATE INDEX idx_audit_suspicious_timestamp ON oauth2_audit_log(suspicious_activity, timestamp DESC) 
    WHERE suspicious_activity = TRUE;
CREATE INDEX idx_audit_failed_attempts ON oauth2_audit_log(success, timestamp DESC) 
    WHERE success = FALSE;

-- JSONB index for additional_data queries
CREATE INDEX idx_audit_additional_data ON oauth2_audit_log USING GIN(additional_data);

-- Table for audit log retention policy
CREATE TABLE audit_retention_policy (
    id SERIAL PRIMARY KEY,
    retention_days INTEGER NOT NULL DEFAULT 90,
    last_cleanup TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insert default retention policy
INSERT INTO audit_retention_policy (retention_days) VALUES (90);

-- Function to automatically cleanup old audit logs
CREATE OR REPLACE FUNCTION cleanup_old_audit_logs() RETURNS void AS $$
DECLARE
    retention_days INTEGER;
    cutoff_date TIMESTAMP WITH TIME ZONE;
BEGIN
    -- Get retention policy
    SELECT retention_days INTO retention_days FROM audit_retention_policy LIMIT 1;
    
    -- Calculate cutoff date
    cutoff_date := CURRENT_TIMESTAMP - (retention_days || ' days')::INTERVAL;
    
    -- Delete old records
    DELETE FROM oauth2_audit_log WHERE timestamp < cutoff_date;
    
    -- Update last cleanup timestamp
    UPDATE audit_retention_policy SET last_cleanup = CURRENT_TIMESTAMP;
    
    -- Log the cleanup
    RAISE NOTICE 'Cleaned up audit logs older than % days', retention_days;
END;
$$ LANGUAGE plpgsql;

-- Comment on table and important columns
COMMENT ON TABLE oauth2_audit_log IS 'OAuth2 security audit log for tracking all authentication and authorization events';
COMMENT ON COLUMN oauth2_audit_log.event_type IS 'Type of OAuth2 event (AUTHORIZATION_REQUEST, TOKEN_ISSUED, etc.)';
COMMENT ON COLUMN oauth2_audit_log.risk_level IS 'Security risk level: LOW, MEDIUM, HIGH, CRITICAL';
COMMENT ON COLUMN oauth2_audit_log.additional_data IS 'JSONB field for flexible additional context data';
COMMENT ON COLUMN oauth2_audit_log.suspicious_activity IS 'Flag indicating potential security threat';
COMMENT ON COLUMN oauth2_audit_log.processing_time_ms IS 'Request processing time in milliseconds for performance monitoring';