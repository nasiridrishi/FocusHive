package com.focushive.identity.service;

import com.focushive.identity.entity.User;
import com.focushive.identity.integration.service.NotificationServiceIntegration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Service for sending emails via the notification microservice.
 * This service delegates all email operations to the notification service
 * for centralized notification management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final NotificationServiceIntegration notificationService;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    @Value("${app.email.from:noreply@focushive.com}")
    private String fromEmail;

    /**
     * Send email verification link to user.
     */
    public void sendVerificationEmail(User user) {
        try {
            // Delegate to notification service
            notificationService.sendEmailVerification(user, user.getEmailVerificationToken());

            log.info("Email verification initiated for user ID: {}", user.getId());

            // In development, log the link at DEBUG level only (never in production)
            if (log.isDebugEnabled()) {
                String verificationLink = baseUrl + "/verify-email?token=" + user.getEmailVerificationToken();
                log.debug("=== EMAIL VERIFICATION LINK (DEV ONLY) ===");
                log.debug("User ID: {}", user.getId());
                log.debug("Link: {}", verificationLink);
                log.debug("==========================================");
            }
        } catch (Exception e) {
            log.error("Failed to send verification email for user ID: {}", user.getId(), e);
            // Don't throw - email failure shouldn't break registration flow
        }
    }

    /**
     * Send password reset email to user.
     */
    public void sendPasswordResetEmail(User user, String resetToken) {
        try {
            // Delegate to notification service
            notificationService.sendPasswordResetEmail(user, resetToken);

            log.info("Password reset email initiated for user ID: {}", user.getId());

            // In development, log the link at DEBUG level only (never in production)
            if (log.isDebugEnabled()) {
                String resetLink = baseUrl + "/reset-password?token=" + resetToken;
                log.debug("=== PASSWORD RESET LINK (DEV ONLY) ===");
                log.debug("User ID: {}", user.getId());
                log.debug("Link: {}", resetLink);
                log.debug("=====================================");
            }
        } catch (Exception e) {
            log.error("Failed to send password reset email for user ID: {}", user.getId(), e);
            // Don't throw - allow user to retry password reset
        }
    }

    /**
     * Send welcome email after successful registration.
     */
    public void sendWelcomeEmail(User user) {
        try {
            // Delegate to notification service
            notificationService.sendWelcomeEmail(user);

            log.info("Welcome email sent to user ID: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to send welcome email for user ID: {}", user.getId(), e);
            // Don't throw - welcome email is non-critical
        }
    }

    /**
     * Send notification when a new login from unknown device.
     */
    public void sendNewDeviceLoginAlert(User user, String deviceInfo, String ipAddress) {
        try {
            // TODO: Implement in NotificationServiceIntegration when needed
            // For now, just log as this is not a critical notification
            log.info("New device login alert for user ID: {} from IP: {}", user.getId(),
                     ipAddress != null ? ipAddress.substring(0, Math.min(ipAddress.length(), 7)) + "..." : "unknown");

            // In production, this would send a security alert via notification service
            // notificationService.sendSecurityAlert(user, "NEW_DEVICE_LOGIN", deviceInfo, ipAddress);
        } catch (Exception e) {
            log.error("Failed to send new device login alert for user ID: {}", user.getId(), e);
        }
    }

    /**
     * Send data export email to user.
     */
    public void sendDataExportEmail(User user, String exportUrl) {
        try {
            // TODO: Implement in NotificationServiceIntegration when GDPR export is ready
            log.info("Data export email for user ID: {} with export URL", user.getId());

            // In development, log the link at DEBUG level only (never in production)
            if (log.isDebugEnabled()) {
                log.debug("=== DATA EXPORT LINK (DEV ONLY) ===");
                log.debug("User ID: {}", user.getId());
                log.debug("Export URL: {}", exportUrl);
                log.debug("===================================");
            }

            // In production, this would send via notification service
            // notificationService.sendDataExportNotification(user, exportUrl);
        } catch (Exception e) {
            log.error("Failed to send data export email for user ID: {}", user.getId(), e);
        }
    }

    /**
     * Send account lockout notification email to user.
     *
     * @param user The user whose account was locked
     * @param lockoutDuration Duration of lockout (e.g., "30 minutes", "indefinite")
     * @param failedAttempts Number of failed attempts that triggered the lockout
     */
    public void sendAccountLockoutEmail(User user, String lockoutDuration, int failedAttempts) {
        try {
            // Calculate lockout end time based on duration string
            LocalDateTime lockedUntil;
            if ("indefinite".equalsIgnoreCase(lockoutDuration)) {
                lockedUntil = LocalDateTime.now().plusYears(100); // Effectively indefinite
            } else {
                // Parse duration like "30 minutes" - simple implementation
                int minutes = 30; // Default
                if (lockoutDuration.contains("minute")) {
                    try {
                        String numStr = lockoutDuration.replaceAll("[^0-9]", "");
                        minutes = Integer.parseInt(numStr);
                    } catch (NumberFormatException e) {
                        log.warn("Could not parse lockout duration: {}, using default 30 minutes", lockoutDuration);
                    }
                }
                lockedUntil = LocalDateTime.now().plusMinutes(minutes);
            }

            // Delegate to notification service
            notificationService.sendAccountLockedNotification(user, lockedUntil, failedAttempts);

            log.warn("Account lockout notification sent to user ID: {} - Duration: {}, Failed attempts: {}",
                    user.getId(), lockoutDuration, failedAttempts);

            // In development, log notification details at DEBUG level
            if (log.isDebugEnabled()) {
                log.debug("=== ACCOUNT LOCKOUT NOTIFICATION (DEV ONLY) ===");
                log.debug("User ID: {}", user.getId());
                log.debug("Email: {}", user.getEmail());
                log.debug("Username: {}", user.getUsername());
                log.debug("Lockout Duration: {}", lockoutDuration);
                log.debug("Failed Attempts: {}", failedAttempts);
                log.debug("Instructions: If this was you, please wait {} and try again. If this wasn't you, your account may be under attack.", lockoutDuration);
                log.debug("===============================================");
            }
        } catch (Exception e) {
            log.error("Failed to send account lockout email for user ID: {}", user.getId(), e);
            // Don't throw - critical security notification but shouldn't break lockout flow
        }
    }

    /**
     * Send two-factor authentication code to user.
     * This is a new method to support 2FA.
     */
    public void sendTwoFactorCode(User user, String code) {
        try {
            notificationService.sendTwoFactorCode(user, code);
            log.info("2FA code sent to user ID: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to send 2FA code for user ID: {}", user.getId(), e);
            throw new RuntimeException("Failed to send 2FA code", e); // 2FA is critical
        }
    }

    /**
     * Send password changed confirmation to user.
     * This is a security notification when password is successfully changed.
     */
    public void sendPasswordChangedNotification(User user) {
        try {
            notificationService.sendPasswordChangedNotification(user);
            log.info("Password changed notification sent to user ID: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to send password changed notification for user ID: {}", user.getId(), e);
            // Don't throw - this is informational only
        }
    }
}