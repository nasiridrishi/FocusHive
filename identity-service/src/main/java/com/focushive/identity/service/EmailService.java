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
        log.info("Sending verification email to {} with link: {}", user.getEmail(), verificationLink);
        
        // In development, just log the link
        log.info("=== EMAIL VERIFICATION LINK ===");
        log.info("User: {} ({})", user.getDisplayName(), user.getEmail());
        log.info("Link: {}", verificationLink);
        log.info("===============================");
    }
    
    /**
     * Send password reset email to user.
     */
    public void sendPasswordResetEmail(User user, String resetToken) {
        String resetLink = baseUrl + "/reset-password?token=" + resetToken;
        
        // TODO: Implement actual email sending
        log.info("Sending password reset email to {} with link: {}", user.getEmail(), resetLink);
        
        // In development, just log the link
        log.info("=== PASSWORD RESET LINK ===");
        log.info("User: {} ({})", user.getDisplayName(), user.getEmail());
        log.info("Link: {}", resetLink);
        log.info("===========================");
    }
    
    /**
     * Send welcome email after successful registration.
     */
    public void sendWelcomeEmail(User user) {
        // TODO: Implement actual email sending
        log.info("Sending welcome email to {}", user.getEmail());
    }
    
    /**
     * Send notification when a new login from unknown device.
     */
    public void sendNewDeviceLoginAlert(User user, String deviceInfo, String ipAddress) {
        // TODO: Implement actual email sending
        log.info("Sending new device login alert to {} from IP: {}", user.getEmail(), ipAddress);
    }
}