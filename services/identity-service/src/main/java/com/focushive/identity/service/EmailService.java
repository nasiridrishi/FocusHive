package com.focushive.identity.service;

import com.focushive.identity.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for sending emails.
 * This is a stub implementation - in production, integrate with an email provider.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    
    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;
    
    @Value("${app.email.from:noreply@focushive.com}")
    private String fromEmail;
    
    /**
     * Send email verification link to user.
     */
    public void sendVerificationEmail(User user) {
        String verificationLink = baseUrl + "/verify-email?token=" + user.getEmailVerificationToken();
        
        // TODO: Implement actual email sending via SMTP/SendGrid/AWS SES
        log.info("Email verification initiated for user ID: {}", user.getId());
        
        // In development, log the link at DEBUG level only (never in production)
        if (log.isDebugEnabled()) {
            log.debug("=== EMAIL VERIFICATION LINK (DEV ONLY) ===");
            log.debug("User ID: {}", user.getId());
            log.debug("Link: {}", verificationLink);
            log.debug("==========================================");
        }
    }
    
    /**
     * Send password reset email to user.
     */
    public void sendPasswordResetEmail(User user, String resetToken) {
        String resetLink = baseUrl + "/reset-password?token=" + resetToken;
        
        // TODO: Implement actual email sending
        log.info("Password reset email initiated for user ID: {}", user.getId());
        
        // In development, log the link at DEBUG level only (never in production)
        if (log.isDebugEnabled()) {
            log.debug("=== PASSWORD RESET LINK (DEV ONLY) ===");
            log.debug("User ID: {}", user.getId());
            log.debug("Link: {}", resetLink);
            log.debug("=====================================");
        }
    }
    
    /**
     * Send welcome email after successful registration.
     */
    public void sendWelcomeEmail(User user) {
        // TODO: Implement actual email sending
        log.info("Welcome email sent to user ID: {}", user.getId());
    }
    
    /**
     * Send notification when a new login from unknown device.
     */
    public void sendNewDeviceLoginAlert(User user, String deviceInfo, String ipAddress) {
        // TODO: Implement actual email sending
        log.info("New device login alert sent to user ID: {} from IP: {}", user.getId(), 
                 ipAddress != null ? ipAddress.substring(0, Math.min(ipAddress.length(), 7)) + "..." : "unknown");
    }
}