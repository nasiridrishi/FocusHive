-- Add account lockout tracking fields to users table
-- This implements OWASP A04:2021 Insecure Design best practices

-- Add failed login attempt tracking
ALTER TABLE users
ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0,
ADD COLUMN last_failed_login_at TIMESTAMP,
ADD COLUMN account_locked_at TIMESTAMP,
ADD COLUMN account_locked_until TIMESTAMP;

-- Add indexes for lockout queries
CREATE INDEX idx_users_failed_attempts ON users(failed_login_attempts) WHERE failed_login_attempts > 0;
CREATE INDEX idx_users_locked_until ON users(account_locked_until) WHERE account_locked_until IS NOT NULL;
CREATE INDEX idx_users_lockout_status ON users(account_non_locked, account_locked_until);

-- Add comments for documentation
COMMENT ON COLUMN users.failed_login_attempts IS 'Number of consecutive failed login attempts';
COMMENT ON COLUMN users.last_failed_login_at IS 'Timestamp of the last failed login attempt';
COMMENT ON COLUMN users.account_locked_at IS 'Timestamp when the account was locked due to failed attempts';
COMMENT ON COLUMN users.account_locked_until IS 'Timestamp until which the account remains locked (NULL for indefinite)';