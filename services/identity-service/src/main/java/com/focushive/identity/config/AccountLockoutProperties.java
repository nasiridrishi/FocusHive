package com.focushive.identity.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for account lockout settings.
 * Implements OWASP A04:2021 Insecure Design best practices.
 */
@Data
@Component
@ConfigurationProperties(prefix = "focushive.security.account-lockout")
public class AccountLockoutProperties {

    /**
     * Maximum number of consecutive failed login attempts before lockout.
     * OWASP recommendation: 3-5 attempts.
     */
    private int maxFailedAttempts = 5;

    /**
     * Duration of account lockout in minutes.
     * 0 = indefinite lockout (requires admin unlock).
     */
    private int lockoutDurationMinutes = 30;

    /**
     * Whether to enable account lockout functionality.
     */
    private boolean enabled = true;

    /**
     * Whether to automatically unlock accounts after lockout duration expires.
     */
    private boolean autoUnlockEnabled = true;

    /**
     * Reset failed attempts counter after successful login.
     */
    private boolean resetAttemptsOnSuccess = true;

    /**
     * Log all lockout events for security monitoring.
     */
    private boolean auditLockoutEvents = true;

    /**
     * Include IP address in lockout auditing.
     */
    private boolean trackIpAddress = true;
}