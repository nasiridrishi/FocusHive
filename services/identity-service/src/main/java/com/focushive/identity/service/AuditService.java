package com.focushive.identity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for auditing security events.
 * Provides centralized logging for security-related activities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    /**
     * Log a security event for auditing purposes.
     *
     * @param userId User ID associated with the event
     * @param eventType Type of security event
     * @param description Description of the event
     * @param ipAddress IP address (optional)
     */
    public void logSecurityEvent(UUID userId, String eventType, String description, String ipAddress) {
        // For now, use structured logging. In a production system, this would
        // typically write to a dedicated audit table or external audit system.
        log.info("SECURITY_AUDIT: userId={}, eventType={}, description={}, ipAddress={}, timestamp={}",
                userId, eventType, description, ipAddress, Instant.now());

        // TODO: In production, consider:
        // 1. Writing to audit_logs table
        // 2. Sending to external SIEM system
        // 3. Triggering alerts for critical events
    }

    /**
     * Log a security event without user ID (e.g., for anonymous attempts).
     *
     * @param eventType Type of security event
     * @param description Description of the event
     * @param ipAddress IP address (optional)
     */
    public void logSecurityEvent(String eventType, String description, String ipAddress) {
        log.info("SECURITY_AUDIT: eventType={}, description={}, ipAddress={}, timestamp={}",
                eventType, description, ipAddress, Instant.now());
    }

    /**
     * Log user profile update event.
     *
     * @param userId User ID
     * @param description Description of the update
     */
    public void logUserProfileUpdate(UUID userId, String description) {
        logSecurityEvent(userId, "USER_PROFILE_UPDATE", description, null);
    }

    /**
     * Log password change event.
     *
     * @param userId User ID
     */
    public void logPasswordChange(UUID userId) {
        logSecurityEvent(userId, "PASSWORD_CHANGE", "User password changed", null);
    }

    /**
     * Log account deletion event.
     *
     * @param userId User ID
     * @param reason Reason for deletion
     */
    public void logAccountDeletion(UUID userId, String reason) {
        logSecurityEvent(userId, "ACCOUNT_DELETION", "Account marked for deletion: " + reason, null);
    }

    /**
     * Log account recovery event.
     *
     * @param userId User ID
     */
    public void logAccountRecovery(UUID userId) {
        logSecurityEvent(userId, "ACCOUNT_RECOVERY", "Account recovered from deletion", null);
    }
}