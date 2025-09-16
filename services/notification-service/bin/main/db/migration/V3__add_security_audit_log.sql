-- Create security audit log table
CREATE TABLE IF NOT EXISTS security_audit_log (
    id VARCHAR(255) PRIMARY KEY,
    action VARCHAR(100) NOT NULL,
    username VARCHAR(100),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    details TEXT,
    success BOOLEAN NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    session_id VARCHAR(100),
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP
);

-- Create indexes for security audit log
CREATE INDEX IF NOT EXISTS idx_security_audit_username ON security_audit_log(username, timestamp);
CREATE INDEX IF NOT EXISTS idx_security_audit_action ON security_audit_log(action, timestamp);
CREATE INDEX IF NOT EXISTS idx_security_audit_timestamp ON security_audit_log(timestamp);