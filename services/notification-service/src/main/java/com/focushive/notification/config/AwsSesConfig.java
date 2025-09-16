package com.focushive.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * AWS SES SMTP Configuration for email delivery.
 * 
 * This configuration sets up JavaMailSender to use AWS SES SMTP with proper credentials.
 * It's active for all profiles except test to allow real email testing.
 */
@Slf4j
@Configuration
@Profile("!test") // Active for all profiles except test
public class AwsSesConfig {

    @Value("${aws.ses.smtp.username:}")
    private String smtpUsername;

    @Value("${aws.ses.smtp.password:}")
    private String smtpPassword;

    @Value("${aws.ses.from-email:no-reply@focushive.app}")
    private String fromEmail;

    /**
     * Creates the JavaMailSender configured for AWS SES SMTP.
     * 
     * @return configured JavaMailSender for sending emails via SMTP
     */
    @Bean
    public JavaMailSender mailSender() {
        log.info("Configuring AWS SES SMTP JavaMailSender");
        log.info("From email configured as: {}", fromEmail);
        
        // Validate required configuration
        if (smtpUsername == null || smtpUsername.trim().isEmpty()) {
            log.warn("AWS SES SMTP username not configured. Email sending will fail.");
        }
        
        if (smtpPassword == null || smtpPassword.trim().isEmpty()) {
            log.warn("AWS SES SMTP password not configured. Email sending will fail.");
        }

        // Create JavaMailSender for AWS SES SMTP
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("email-smtp.us-east-1.amazonaws.com"); // AWS SES SMTP endpoint
        mailSender.setPort(587); // TLS port
        mailSender.setUsername(smtpUsername);
        mailSender.setPassword(smtpPassword);
        
        // Configure SMTP properties
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.debug", "true"); // Enable for debugging
        
        return mailSender;
    }

    /**
     * Get the configured from email address.
     * 
     * @return the from email address for notifications
     */
    @Bean("fromEmailAddress")
    public String fromEmailAddress() {
        return fromEmail;
    }
}