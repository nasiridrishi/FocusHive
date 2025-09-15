package com.focushive.notification.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for checking SMTP email service health.
 * Provides detailed information about email configuration and connectivity.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailHealthService {

    private final JavaMailSender javaMailSender;

    /**
     * Check email service health and return detailed status information.
     *
     * @return Map containing health status and details
     */
    public Map<String, Object> checkEmailHealth() {
        try {
            return performHealthCheck();
        } catch (Exception e) {
            log.warn("Email health check failed", e);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "DOWN");
            result.put("error", e.getMessage());
            result.put("type", e.getClass().getSimpleName());
            return result;
        }
    }

    private Map<String, Object> performHealthCheck() {
        Map<String, Object> details = new HashMap<>();
        
        try {
            // Get the underlying JavaMail Session and Transport
            jakarta.mail.Session session = javaMailSender.createMimeMessage().getSession();
            
            // Get SMTP transport
            Transport transport = session.getTransport("smtp");
            
            // Extract connection details from session properties
            String host = session.getProperty("mail.smtp.host");
            String port = session.getProperty("mail.smtp.port");
            String auth = session.getProperty("mail.smtp.auth");
            String startTls = session.getProperty("mail.smtp.starttls.enable");
            
            details.put("host", host != null ? host : "unknown");
            details.put("port", port != null ? port : "unknown");
            details.put("authentication", "true".equals(auth) ? "enabled" : "disabled");
            details.put("starttls", "true".equals(startTls) ? "enabled" : "disabled");
            
            // Try to connect to SMTP server
            if (host != null && port != null) {
                try {
                    transport.connect();
                    details.put("connection", "successful");
                    details.put("status", "UP");
                    details.put("message", "Connected to SMTP server");
                    transport.close();
                    
                    return details;
                            
                } catch (MessagingException e) {
                    log.warn("Failed to connect to SMTP server: {}", e.getMessage());
                    details.put("connection", "failed");
                    details.put("status", "DOWN");
                    details.put("error", e.getMessage());
                    
                    return details;
                }
            } else {
                details.put("connection", "not configured");
                details.put("status", "DOWN");
                details.put("message", "SMTP host or port not configured");
                
                return details;
            }
            
        } catch (Exception e) {
            log.error("Email health check failed with unexpected error", e);
            details.put("status", "DOWN");
            details.put("error", "Health check failed: " + e.getMessage());
            
            return details;
        }
    }
}