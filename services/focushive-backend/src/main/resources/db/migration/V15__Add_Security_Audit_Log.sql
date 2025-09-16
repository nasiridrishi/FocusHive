-- Add security audit logging table
-- Migration: V15__Add_Security_Audit_Log.sql

-- Create security audit log table for tracking authorization attempts
CREATE TABLE security_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    username VARCHAR(255),
    operation VARCHAR(255) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(255),
    access_granted BOOLEAN NOT NULL DEFAULT false,
    ip_address VARCHAR(45),
    user_agent TEXT,
    session_id VARCHAR(255),
    failure_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX idx_security_audit_user_id ON security_audit_log(user_id);
CREATE INDEX idx_security_audit_username ON security_audit_log(username);
CREATE INDEX idx_security_audit_operation ON security_audit_log(operation);
CREATE INDEX idx_security_audit_resource_type ON security_audit_log(resource_type);
CREATE INDEX idx_security_audit_created_at ON security_audit_log(created_at);
CREATE INDEX idx_security_audit_access_granted ON security_audit_log(access_granted);

-- Create index for common query patterns
CREATE INDEX idx_security_audit_user_operation_date 
ON security_audit_log(user_id, operation, created_at DESC);

-- Create index for failed access attempts monitoring
CREATE INDEX idx_security_audit_failed_attempts 
ON security_audit_log(username, access_granted, created_at DESC) 
WHERE access_granted = false;

-- Add comments for documentation
COMMENT ON TABLE security_audit_log IS 'Audit log for security and authorization events';
COMMENT ON COLUMN security_audit_log.user_id IS 'ID of the user attempting the operation';
COMMENT ON COLUMN security_audit_log.username IS 'Username of the user (for cases where user_id is not available)';
COMMENT ON COLUMN security_audit_log.operation IS 'The operation being attempted (e.g., READ, CREATE, UPDATE, DELETE)';
COMMENT ON COLUMN security_audit_log.resource_type IS 'Type of resource being accessed (e.g., HIVE, TIMER, CHAT)';
COMMENT ON COLUMN security_audit_log.resource_id IS 'ID of the specific resource being accessed';
COMMENT ON COLUMN security_audit_log.access_granted IS 'Whether the access was granted or denied';
COMMENT ON COLUMN security_audit_log.ip_address IS 'IP address of the user making the request';
COMMENT ON COLUMN security_audit_log.user_agent IS 'User agent string from the request';
COMMENT ON COLUMN security_audit_log.session_id IS 'Session ID for tracking user sessions';
COMMENT ON COLUMN security_audit_log.failure_reason IS 'Reason for access denial (if applicable)';

-- Create a view for recent security events (last 7 days)
CREATE VIEW recent_security_events AS
SELECT 
    id,
    user_id,
    username,
    operation,
    resource_type,
    resource_id,
    access_granted,
    ip_address,
    failure_reason,
    created_at
FROM security_audit_log 
WHERE created_at >= NOW() - INTERVAL '7 days'
ORDER BY created_at DESC;

-- Create a view for failed access attempts (security monitoring)
CREATE VIEW failed_access_attempts AS
SELECT 
    username,
    operation,
    resource_type,
    ip_address,
    failure_reason,
    created_at,
    COUNT(*) OVER (
        PARTITION BY username, ip_address, operation 
        ORDER BY created_at 
        RANGE BETWEEN INTERVAL '1 hour' PRECEDING AND CURRENT ROW
    ) as attempts_last_hour
FROM security_audit_log 
WHERE access_granted = false
    AND created_at >= NOW() - INTERVAL '24 hours'
ORDER BY created_at DESC;