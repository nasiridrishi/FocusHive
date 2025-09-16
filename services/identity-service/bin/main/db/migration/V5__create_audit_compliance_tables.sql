-- Audit & Compliance Tables Migration
-- Creates enhanced audit logging and data export request tracking

-- Drop the existing simple audit logs table to replace with enhanced version
DROP TABLE IF EXISTS identity_audit_logs CASCADE;

-- Create enhanced Audit Logs table
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    client_id UUID REFERENCES oauth_clients(id) ON DELETE SET NULL,
    event_type VARCHAR(100) NOT NULL,
    event_category VARCHAR(50) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    resource VARCHAR(200),
    action VARCHAR(20) NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    session_id VARCHAR(100),
    request_id VARCHAR(100),
    error_code VARCHAR(50),
    error_message VARCHAR(1000),
    duration_ms BIGINT,
    geographic_location VARCHAR(100),
    risk_score INTEGER,
    automated_action_triggered BOOLEAN DEFAULT FALSE,
    automated_action_details VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Audit Log Metadata table
CREATE TABLE audit_log_metadata (
    audit_log_id UUID NOT NULL REFERENCES audit_logs(id) ON DELETE CASCADE,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value VARCHAR(500) NOT NULL,
    PRIMARY KEY (audit_log_id, metadata_key)
);

-- Create Data Export Requests table
CREATE TABLE data_export_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    request_type VARCHAR(50) NOT NULL,
    format VARCHAR(10) NOT NULL DEFAULT 'JSON',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reason VARCHAR(1000),
    verification_method VARCHAR(50),
    processing_started_at TIMESTAMP,
    completed_at TIMESTAMP,
    failed_at TIMESTAMP,
    export_file_path VARCHAR(500),
    file_size_bytes BIGINT,
    record_count INTEGER,
    expires_at TIMESTAMP,
    error_message VARCHAR(1000),
    error_code VARCHAR(50),
    downloaded_at TIMESTAMP,
    download_count INTEGER DEFAULT 0,
    last_download_ip VARCHAR(45),
    retention_days INTEGER DEFAULT 30,
    file_checksum VARCHAR(128),
    encrypted BOOLEAN DEFAULT TRUE,
    encryption_key_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Data Export Categories table
CREATE TABLE data_export_categories (
    export_request_id UUID NOT NULL REFERENCES data_export_requests(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL,
    PRIMARY KEY (export_request_id, category)
);

-- Create Data Export Metadata table
CREATE TABLE data_export_metadata (
    export_request_id UUID NOT NULL REFERENCES data_export_requests(id) ON DELETE CASCADE,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value VARCHAR(500) NOT NULL,
    PRIMARY KEY (export_request_id, metadata_key)
);

-- Create indexes for Audit Logs
CREATE INDEX idx_audit_log_user ON audit_logs(user_id);
CREATE INDEX idx_audit_log_client ON audit_logs(client_id);
CREATE INDEX idx_audit_log_event_type ON audit_logs(event_type);
CREATE INDEX idx_audit_log_category ON audit_logs(event_category);
CREATE INDEX idx_audit_log_severity ON audit_logs(severity);
CREATE INDEX idx_audit_log_outcome ON audit_logs(outcome);
CREATE INDEX idx_audit_log_created ON audit_logs(created_at);
CREATE INDEX idx_audit_log_ip ON audit_logs(ip_address);
CREATE INDEX idx_audit_log_resource ON audit_logs(resource);
CREATE INDEX idx_audit_log_session ON audit_logs(session_id);
CREATE INDEX idx_audit_log_request ON audit_logs(request_id);
CREATE INDEX idx_audit_log_risk ON audit_logs(risk_score);

-- Create indexes for Data Export Requests
CREATE INDEX idx_export_request_user ON data_export_requests(user_id);
CREATE INDEX idx_export_request_status ON data_export_requests(status);
CREATE INDEX idx_export_request_type ON data_export_requests(request_type);
CREATE INDEX idx_export_request_created ON data_export_requests(created_at);
CREATE INDEX idx_export_request_expires ON data_export_requests(expires_at);
CREATE INDEX idx_export_request_completed ON data_export_requests(completed_at);
CREATE INDEX idx_export_request_failed ON data_export_requests(failed_at);
CREATE INDEX idx_export_request_downloaded ON data_export_requests(downloaded_at);

-- Create performance optimization indexes
CREATE INDEX idx_audit_log_user_time ON audit_logs(user_id, created_at);
CREATE INDEX idx_audit_log_category_time ON audit_logs(event_category, created_at);
CREATE INDEX idx_audit_log_severity_time ON audit_logs(severity, created_at) 
WHERE severity IN ('ERROR', 'CRITICAL');

CREATE INDEX idx_export_request_user_status ON data_export_requests(user_id, status);
CREATE INDEX idx_export_request_status_time ON data_export_requests(status, created_at);

-- Create partial indexes for common queries
CREATE INDEX idx_audit_log_security_events ON audit_logs(user_id, event_type, created_at) 
WHERE event_category IN ('SECURITY', 'AUTHENTICATION', 'AUTHORIZATION');

CREATE INDEX idx_audit_log_data_events ON audit_logs(user_id, event_type, created_at) 
WHERE event_category = 'DATA_PRIVACY';

CREATE INDEX idx_audit_log_failed_events ON audit_logs(event_type, created_at, ip_address) 
WHERE outcome = 'FAILURE';

CREATE INDEX idx_audit_log_high_risk ON audit_logs(risk_score, created_at, user_id) 
WHERE risk_score >= 70;

CREATE INDEX idx_export_request_pending ON data_export_requests(created_at) 
WHERE status = 'PENDING';

CREATE INDEX idx_export_request_ready ON data_export_requests(user_id, completed_at) 
WHERE status = 'COMPLETED';

-- Add constraints for audit logs
ALTER TABLE audit_logs 
ADD CONSTRAINT chk_audit_log_severity 
CHECK (severity IN ('INFO', 'WARNING', 'ERROR', 'CRITICAL'));

ALTER TABLE audit_logs 
ADD CONSTRAINT chk_audit_log_outcome 
CHECK (outcome IN ('SUCCESS', 'FAILURE', 'PARTIAL'));

ALTER TABLE audit_logs 
ADD CONSTRAINT chk_audit_log_action 
CHECK (action IS NOT NULL AND LENGTH(TRIM(action)) > 0);

ALTER TABLE audit_logs 
ADD CONSTRAINT chk_audit_log_risk_score 
CHECK (risk_score IS NULL OR (risk_score >= 0 AND risk_score <= 100));

ALTER TABLE audit_logs 
ADD CONSTRAINT chk_audit_log_duration 
CHECK (duration_ms IS NULL OR duration_ms >= 0);

-- Add constraints for export requests
ALTER TABLE data_export_requests 
ADD CONSTRAINT chk_export_request_status 
CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'EXPIRED'));

ALTER TABLE data_export_requests 
ADD CONSTRAINT chk_export_request_format 
CHECK (format IN ('JSON', 'CSV', 'XML', 'PDF'));

ALTER TABLE data_export_requests 
ADD CONSTRAINT chk_export_request_file_size 
CHECK (file_size_bytes IS NULL OR file_size_bytes >= 0);

ALTER TABLE data_export_requests 
ADD CONSTRAINT chk_export_request_record_count 
CHECK (record_count IS NULL OR record_count >= 0);

ALTER TABLE data_export_requests 
ADD CONSTRAINT chk_export_request_download_count 
CHECK (download_count >= 0);

ALTER TABLE data_export_requests 
ADD CONSTRAINT chk_export_request_retention 
CHECK (retention_days IS NULL OR retention_days > 0);

-- Add logical consistency constraints
ALTER TABLE data_export_requests 
ADD CONSTRAINT chk_export_request_completed 
CHECK ((status = 'COMPLETED' AND completed_at IS NOT NULL) OR 
       (status != 'COMPLETED'));

ALTER TABLE data_export_requests 
ADD CONSTRAINT chk_export_request_failed 
CHECK ((status = 'FAILED' AND failed_at IS NOT NULL) OR 
       (status != 'FAILED'));

ALTER TABLE data_export_requests 
ADD CONSTRAINT chk_export_request_processing 
CHECK ((status = 'PROCESSING' AND processing_started_at IS NOT NULL) OR 
       (status != 'PROCESSING' OR processing_started_at IS NOT NULL));

ALTER TABLE data_export_requests 
ADD CONSTRAINT chk_export_request_download 
CHECK ((download_count = 0 AND downloaded_at IS NULL) OR 
       (download_count > 0 AND downloaded_at IS NOT NULL));

-- Create triggers for updated_at timestamp
CREATE TRIGGER update_data_export_requests_updated_at BEFORE UPDATE ON data_export_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create indexes for compliance and reporting
CREATE INDEX idx_audit_log_compliance_export ON audit_logs(user_id, event_category, created_at, event_type);

CREATE INDEX idx_audit_log_gdpr_events ON audit_logs(user_id, created_at) 
WHERE event_type LIKE '%GDPR%' OR event_type LIKE '%DATA_%' OR event_category = 'DATA_PRIVACY';

CREATE INDEX idx_export_request_gdpr ON data_export_requests(user_id, request_type, created_at, status);

-- Create maintenance indexes for cleanup
CREATE INDEX idx_audit_log_cleanup ON audit_logs(created_at);

CREATE INDEX idx_export_request_cleanup ON data_export_requests(expires_at, status) 
WHERE expires_at IS NOT NULL;

CREATE INDEX idx_export_request_old_downloads ON data_export_requests(downloaded_at, retention_days) 
WHERE downloaded_at IS NOT NULL;

-- Add partitioning preparation indexes (for future partitioning)
CREATE INDEX idx_audit_log_partition_date ON audit_logs(DATE(created_at), event_category);

-- Create indexes for real-time monitoring
CREATE INDEX idx_audit_log_realtime_security ON audit_logs(created_at, severity, event_category) 
WHERE event_category IN ('SECURITY', 'AUTHENTICATION') AND severity IN ('ERROR', 'CRITICAL');

CREATE INDEX idx_audit_log_realtime_failures ON audit_logs(created_at, outcome, event_type) 
WHERE outcome = 'FAILURE';

-- Add comments for documentation
COMMENT ON TABLE audit_logs IS 'Comprehensive audit logging for security, compliance, and operations';
COMMENT ON TABLE data_export_requests IS 'GDPR data portability requests with complete audit trail';

COMMENT ON COLUMN audit_logs.event_category IS 'Category for grouping: AUTHENTICATION, AUTHORIZATION, SECURITY, DATA_PRIVACY, SYSTEM';
COMMENT ON COLUMN audit_logs.severity IS 'Log severity: INFO, WARNING, ERROR, CRITICAL';
COMMENT ON COLUMN audit_logs.outcome IS 'Operation outcome: SUCCESS, FAILURE, PARTIAL';
COMMENT ON COLUMN audit_logs.risk_score IS 'Automated risk assessment score (0-100)';

COMMENT ON COLUMN data_export_requests.status IS 'Request status: PENDING, PROCESSING, COMPLETED, FAILED, EXPIRED';
COMMENT ON COLUMN data_export_requests.format IS 'Export format: JSON, CSV, XML, PDF';
COMMENT ON COLUMN data_export_requests.retention_days IS 'How long to keep the export file';
COMMENT ON COLUMN data_export_requests.encrypted IS 'Whether the export file is encrypted';

-- Create view for active audit events (commonly queried)
CREATE VIEW active_security_events AS
SELECT 
    al.*,
    u.username,
    oc.client_name
FROM audit_logs al
LEFT JOIN users u ON al.user_id = u.id
LEFT JOIN oauth_clients oc ON al.client_id = oc.id
WHERE al.event_category IN ('SECURITY', 'AUTHENTICATION', 'AUTHORIZATION');

-- Create view for recent export requests
CREATE VIEW recent_export_requests AS
SELECT 
    der.*,
    u.username,
    u.email
FROM data_export_requests der
JOIN users u ON der.user_id = u.id;

COMMENT ON VIEW active_security_events IS 'Recent security-related audit events with user and client details';
COMMENT ON VIEW recent_export_requests IS 'Recent data export requests with user details for monitoring';