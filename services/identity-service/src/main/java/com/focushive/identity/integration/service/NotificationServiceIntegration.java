package com.focushive.identity.integration.service;

import com.focushive.identity.entity.User;
import com.focushive.identity.integration.client.NotificationServiceClient;
import com.focushive.identity.integration.dto.NotificationRequest;
import com.focushive.identity.integration.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for integrating with the notification microservice.
 * Handles all notification-related operations for the identity service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceIntegration {

    private final NotificationServiceClient notificationServiceClient;

    @Value("${application.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${application.name:FocusHive Identity Service}")
    private String applicationName;

    /**
     * Send a welcome email to a new user
     */
    public void sendWelcomeEmail(User user) {
        log.info("Sending welcome email to user: {}", user.getEmail());

        NotificationRequest request = NotificationRequest.builder()
            .userId(user.getId().toString())
            .type("WELCOME")
            .title("Welcome to FocusHive!")
            .content(buildWelcomeContent(user))
            .priority("NORMAL")
            .language(user.getPreferredLanguage() != null ? user.getPreferredLanguage() : "en")
            .variables(Map.of(
                "userName", user.getUsername(),
                "userEmail", user.getEmail()
            ))
            .metadata(buildMetadata("welcome", user))
            .build();

        sendNotification(request);
    }

    /**
     * Send an email verification email to a user
     */
    public void sendEmailVerification(User user, String verificationToken) {
        log.info("Sending email verification to user: {}", user.getEmail());

        String verificationUrl = frontendUrl + "/verify-email?token=" + verificationToken;

        NotificationRequest request = NotificationRequest.builder()
            .userId(user.getId().toString())
            .type("EMAIL_VERIFICATION")
            .title("Verify Your Email Address")
            .content(buildEmailVerificationContent(user, verificationUrl))
            .actionUrl(verificationUrl)
            .priority("HIGH")
            .forceDelivery(true)
            .language(user.getPreferredLanguage() != null ? user.getPreferredLanguage() : "en")
            .variables(Map.of(
                "userName", user.getUsername(),
                "verificationUrl", verificationUrl
            ))
            .metadata(buildMetadata("email_verification", user))
            .build();

        sendNotification(request);
    }

    /**
     * Send a password reset email to a user
     */
    public void sendPasswordResetEmail(User user, String resetToken) {
        log.info("Sending password reset email to user: {}", user.getEmail());

        String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;

        NotificationRequest request = NotificationRequest.builder()
            .userId(user.getId().toString())
            .type("PASSWORD_RESET")
            .title("Password Reset Request")
            .content(buildPasswordResetContent(user, resetUrl))
            .actionUrl(resetUrl)
            .priority("HIGH")
            .forceDelivery(true)
            .language(user.getPreferredLanguage() != null ? user.getPreferredLanguage() : "en")
            .variables(Map.of(
                "userName", user.getUsername(),
                "resetUrl", resetUrl,
                "expirationMinutes", "30"
            ))
            .metadata(buildMetadata("password_reset", user))
            .build();

        sendNotification(request);
    }

    /**
     * Send an account locked notification to a user
     */
    public void sendAccountLockedNotification(User user, LocalDateTime lockedUntil, int failedAttempts) {
        log.warn("Sending account locked notification to user: {}", user.getEmail());

        NotificationRequest request = NotificationRequest.builder()
            .userId(user.getId().toString())
            .type("SYSTEM_NOTIFICATION")
            .title("Security Alert: Account Locked")
            .content(buildAccountLockedContent(user, lockedUntil, failedAttempts))
            .priority("CRITICAL")
            .forceDelivery(true)
            .language(user.getPreferredLanguage() != null ? user.getPreferredLanguage() : "en")
            .variables(Map.of(
                "userName", user.getUsername(),
                "lockedUntil", lockedUntil.toString(),
                "failedAttempts", String.valueOf(failedAttempts)
            ))
            .metadata(buildSecurityMetadata("account_locked", user, failedAttempts))
            .build();

        sendNotification(request);
    }

    /**
     * Send a two-factor authentication code to a user
     */
    public void sendTwoFactorCode(User user, String code) {
        log.info("Sending 2FA code to user: {}", user.getEmail());

        NotificationRequest request = NotificationRequest.builder()
            .userId(user.getId().toString())
            .type("TWO_FACTOR_AUTH")
            .title("Your FocusHive Security Code")
            .content(buildTwoFactorContent(user, code))
            .priority("CRITICAL")
            .forceDelivery(true)
            .language(user.getPreferredLanguage() != null ? user.getPreferredLanguage() : "en")
            .variables(Map.of(
                "userName", user.getUsername(),
                "code", code,
                "expirationMinutes", "5"
            ))
            .metadata(buildSecurityMetadata("2fa_code", user, null))
            .build();

        sendNotification(request);
    }

    /**
     * Send a notification about successful password change
     */
    public void sendPasswordChangedNotification(User user) {
        log.info("Sending password changed notification to user: {}", user.getEmail());

        NotificationRequest request = NotificationRequest.builder()
            .userId(user.getId().toString())
            .type("SYSTEM_NOTIFICATION")
            .title("Password Changed Successfully")
            .content(buildPasswordChangedContent(user))
            .priority("HIGH")
            .language(user.getPreferredLanguage() != null ? user.getPreferredLanguage() : "en")
            .variables(Map.of(
                "userName", user.getUsername(),
                "changedAt", LocalDateTime.now().toString()
            ))
            .metadata(buildSecurityMetadata("password_changed", user, null))
            .build();

        sendNotification(request);
    }

    // Private helper methods

    private void sendNotification(NotificationRequest request) {
        try {
            NotificationResponse response = notificationServiceClient.sendNotification(request);

            if (response != null) {
                log.debug("Notification sent successfully: id={}, status={}",
                    response.getId(), response.getStatus());
            } else {
                log.error("Failed to send notification: Notification service returned null response for user {}",
                    request.getUserId());
            }
        } catch (Exception e) {
            log.error("Error sending notification to user {}: {}",
                request.getUserId(), e.getMessage(), e);

            // In production, could implement retry logic or dead letter queue here
            if ("CRITICAL".equals(request.getPriority())) {
                log.error("CRITICAL notification failed for user {}: {}",
                    request.getUserId(), request.getTitle());
                // Could trigger alert to operations team
            }
        }
    }

    private Map<String, Object> buildMetadata(String notificationType, User user) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "identity-service");
        metadata.put("notificationType", notificationType);
        metadata.put("userId", user.getId());
        metadata.put("userEmail", user.getEmail());
        metadata.put("timestamp", LocalDateTime.now().toString());
        metadata.put("applicationName", applicationName);
        return metadata;
    }

    private Map<String, Object> buildSecurityMetadata(String securityEvent, User user, Integer failedAttempts) {
        Map<String, Object> metadata = buildMetadata(securityEvent, user);
        metadata.put("securityEvent", true);
        metadata.put("ipAddress", user.getLastLoginIp());
        if (failedAttempts != null) {
            metadata.put("failedAttempts", failedAttempts);
        }
        return metadata;
    }

    // Content builders

    private String buildWelcomeContent(User user) {
        return String.format(
            "Hello %s,\n\n" +
            "Welcome to FocusHive! We're excited to have you as part of our community.\n\n" +
            "FocusHive is your digital co-working space where you can stay focused, " +
            "productive, and connected with others working towards their goals.\n\n" +
            "Get started by:\n" +
            "1. Completing your profile\n" +
            "2. Creating or joining your first hive\n" +
            "3. Setting your focus goals\n\n" +
            "If you have any questions, feel free to reach out to our support team.\n\n" +
            "Best regards,\n" +
            "The FocusHive Team",
            user.getUsername()
        );
    }

    private String buildEmailVerificationContent(User user, String verificationUrl) {
        return String.format(
            "Hello %s,\n\n" +
            "Please verify your email address to complete your FocusHive registration.\n\n" +
            "Click the link below to verify your email:\n" +
            "%s\n\n" +
            "This link will expire in 24 hours.\n\n" +
            "If you didn't create an account with FocusHive, please ignore this email.\n\n" +
            "Best regards,\n" +
            "The FocusHive Team",
            user.getUsername(),
            verificationUrl
        );
    }

    private String buildPasswordResetContent(User user, String resetUrl) {
        return String.format(
            "Hello %s,\n\n" +
            "We received a request to reset your FocusHive password.\n\n" +
            "Click the link below to reset your password:\n" +
            "%s\n\n" +
            "This link will expire in 30 minutes for security reasons.\n\n" +
            "If you didn't request a password reset, please ignore this email. " +
            "Your password won't be changed.\n\n" +
            "For security, we recommend:\n" +
            "- Using a strong, unique password\n" +
            "- Enabling two-factor authentication\n" +
            "- Not sharing your password with anyone\n\n" +
            "Best regards,\n" +
            "The FocusHive Team",
            user.getUsername(),
            resetUrl
        );
    }

    private String buildAccountLockedContent(User user, LocalDateTime lockedUntil, int failedAttempts) {
        return String.format(
            "Hello %s,\n\n" +
            "SECURITY ALERT: Your FocusHive account has been temporarily locked.\n\n" +
            "Reason: %d failed login attempts detected\n" +
            "Locked until: %s\n\n" +
            "If this was you:\n" +
            "- Wait until the lockout period expires\n" +
            "- Use the 'Forgot Password' option if you can't remember your password\n\n" +
            "If this wasn't you:\n" +
            "- Someone may be trying to access your account\n" +
            "- Reset your password immediately after the lockout expires\n" +
            "- Enable two-factor authentication for added security\n\n" +
            "For immediate assistance, contact our support team.\n\n" +
            "Best regards,\n" +
            "The FocusHive Security Team",
            user.getUsername(),
            failedAttempts,
            lockedUntil.toString()
        );
    }

    private String buildTwoFactorContent(User user, String code) {
        return String.format(
            "Hello %s,\n\n" +
            "Your FocusHive security code is:\n\n" +
            "%s\n\n" +
            "This code will expire in 5 minutes.\n\n" +
            "Never share this code with anyone. FocusHive staff will never ask for this code.\n\n" +
            "If you didn't request this code, please secure your account immediately.\n\n" +
            "Best regards,\n" +
            "The FocusHive Security Team",
            user.getUsername(),
            code
        );
    }

    private String buildPasswordChangedContent(User user) {
        return String.format(
            "Hello %s,\n\n" +
            "Your FocusHive password has been changed successfully.\n\n" +
            "If you made this change, no further action is needed.\n\n" +
            "If you didn't make this change:\n" +
            "1. Reset your password immediately\n" +
            "2. Review your account activity\n" +
            "3. Enable two-factor authentication\n" +
            "4. Contact our support team\n\n" +
            "Stay secure,\n" +
            "The FocusHive Security Team",
            user.getUsername()
        );
    }
}