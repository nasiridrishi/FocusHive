package com.focushive.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * Service for sending email notifications.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${notification.email.from:no-reply@focushive.app}")
    private String fromEmail;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    /**
     * Send a simple text email.
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        if (!emailEnabled) {
            log.warn("Email sending is disabled. Would have sent email to: {} with subject: {}", to, subject);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            log.info("Email sent successfully to: {} with subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to: {} with subject: {}", to, subject, e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    /**
     * Send an HTML email.
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (!emailEnabled) {
            log.warn("Email sending is disabled. Would have sent HTML email to: {} with subject: {}", to, subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("HTML email sent successfully to: {} with subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send HTML email to: {} with subject: {}", to, subject, e);
            throw new RuntimeException("Failed to send HTML email: " + e.getMessage(), e);
        }
    }
}