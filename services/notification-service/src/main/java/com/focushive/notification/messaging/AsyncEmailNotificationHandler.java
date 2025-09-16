package com.focushive.notification.messaging;

import com.focushive.notification.dto.EmailRequest;
import com.focushive.notification.messaging.dto.NotificationMessage;
import com.focushive.notification.service.AsyncEmailService;
import com.focushive.notification.service.EmailMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced email notification handler using AsyncEmailService.
 * Ensures all email notifications are processed asynchronously with proper metrics.
 *
 * Performance targets:
 * - Queue acceptance: <50ms
 * - Throughput: >100 emails/second
 * - Error rate: <0.1%
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncEmailNotificationHandler {

    private final AsyncEmailService asyncEmailService;
    private final EmailMetricsService metricsService;

    /**
     * Process email notifications asynchronously.
     * Ensures <50ms queue acceptance time as per TODO.md requirements.
     */
    @RabbitListener(queues = "#{emailNotificationQueue.name}")
    public void handleAsyncEmailNotification(NotificationMessage message) {
        Instant startTime = Instant.now();
        
        try {
            // Validate message
            validateEmailMessage(message);
            
            // Convert to EmailRequest
            EmailRequest emailRequest = convertToEmailRequest(message);
            
            // Send asynchronously (this should return immediately)
            CompletableFuture<String> futureTrackingId = asyncEmailService.sendEmailAsync(emailRequest);
            
            // Record queue acceptance time
            long acceptanceTimeMs = Instant.now().toEpochMilli() - startTime.toEpochMilli();
            metricsService.recordQueueAcceptanceTime(acceptanceTimeMs);
            
            // Handle completion asynchronously
            futureTrackingId.whenComplete((trackingId, throwable) -> {
                if (throwable != null) {
                    log.error("Async email failed for notification {}: {}", 
                            message.getNotificationId(), throwable.getMessage(), throwable);
                } else {
                    log.info("Async email queued successfully - notification: {}, tracking: {}, acceptance time: {}ms",
                            message.getNotificationId(), trackingId, acceptanceTimeMs);
                }
            });
            
            log.debug("Email notification queued for async processing: {} ({}ms)", 
                    message.getNotificationId(), acceptanceTimeMs);
            
        } catch (Exception e) {
            // Record queue acceptance time even on failure
            long acceptanceTimeMs = Instant.now().toEpochMilli() - startTime.toEpochMilli();
            metricsService.recordQueueAcceptanceTime(acceptanceTimeMs);
            
            log.error("Failed to queue email notification {}: {}", 
                    message.getNotificationId(), e.getMessage(), e);
            throw new EmailQueueException("Failed to queue email notification", e);
        }
    }

    /**
     * Process high-priority emails with elevated processing.
     */
    @RabbitListener(queues = "#{priorityNotificationQueue.name}")
    public void handlePriorityAsyncEmail(NotificationMessage message) {
        if (!isEmailNotification(message)) {
            return; // Not an email notification, ignore
        }
        
        log.info("Processing high-priority email notification: {}", message.getNotificationId());
        
        try {
            EmailRequest emailRequest = convertToEmailRequest(message);
            // Set priority to HIGH for faster processing
            emailRequest.setPriority(EmailRequest.EmailPriority.HIGH);
            
            CompletableFuture<String> futureTrackingId = asyncEmailService.sendEmailAsync(emailRequest);
            
            futureTrackingId.whenComplete((trackingId, throwable) -> {
                if (throwable != null) {
                    log.error("Priority email failed: {}", message.getNotificationId(), throwable);
                } else {
                    log.info("Priority email queued: {} -> {}", message.getNotificationId(), trackingId);
                }
            });
            
        } catch (Exception e) {
            log.error("Failed to process priority email: {}", message.getNotificationId(), e);
            throw new EmailQueueException("Failed to process priority email", e);
        }
    }

    /**
     * Process batch email notifications for improved throughput.
     */
    public void handleBatchEmailNotifications(NotificationMessage... messages) {
        log.info("Processing batch of {} email notifications", messages.length);
        
        try {
            EmailRequest[] emailRequests = new EmailRequest[messages.length];
            for (int i = 0; i < messages.length; i++) {
                emailRequests[i] = convertToEmailRequest(messages[i]);
            }
            
            // Use batch processing for better throughput
            CompletableFuture<java.util.Map<String, String>> batchResults = 
                asyncEmailService.sendBatchEmailsAsync(java.util.Arrays.asList(emailRequests));
            
            batchResults.whenComplete((results, throwable) -> {
                if (throwable != null) {
                    log.error("Batch email processing failed", throwable);
                } else {
                    log.info("Batch email processing completed: {} results", results.size());
                }
            });
            
        } catch (Exception e) {
            log.error("Failed to process batch emails", e);
        }
    }

    /**
     * Convert NotificationMessage to EmailRequest.
     */
    private EmailRequest convertToEmailRequest(NotificationMessage message) {
        // Convert Map<String, String> to Map<String, Object>
        Map<String, Object> variables = message.getTemplateVariables() != null ? 
                new HashMap<>(message.getTemplateVariables()) : null;
        
        return EmailRequest.builder()
                .to(message.getEmailTo())
                .subject(message.getEmailSubject() != null ? message.getEmailSubject() : message.getTitle())
                .htmlContent(message.getMessage())
                .templateName(message.getTemplateId() != null ? message.getTemplateId().toString() : null)
                .variables(variables)
                .priority(determinePriority(message))
                .userId(Long.parseLong(message.getUserId())) // Convert string to Long
                .notificationType(message.getType() != null ? message.getType().name() : "SYSTEM_NOTIFICATION")
                .build();
    }

    /**
     * Determine email priority based on notification message.
     */
    private EmailRequest.EmailPriority determinePriority(NotificationMessage message) {
        if (message.isUrgent()) {
            return EmailRequest.EmailPriority.CRITICAL;
        }
        
        // Map notification types to email priorities
        if (message.getType() != null) {
            switch (message.getType()) {
                case SYSTEM_ANNOUNCEMENT:
                case PASSWORD_RESET:
                case EMAIL_VERIFICATION:
                    return EmailRequest.EmailPriority.HIGH;
                case MARKETING:
                case WEEKLY_SUMMARY:
                    return EmailRequest.EmailPriority.LOW;
                default:
                    return EmailRequest.EmailPriority.NORMAL;
            }
        }
        return EmailRequest.EmailPriority.NORMAL;
    }

    /**
     * Validate email message has required fields.
     */
    private void validateEmailMessage(NotificationMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("Notification message is null");
        }
        
        if (message.getEmailTo() == null || message.getEmailTo().trim().isEmpty()) {
            throw new IllegalArgumentException("Email recipient is required");
        }
        
        if (message.getTitle() == null && message.getEmailSubject() == null) {
            throw new IllegalArgumentException("Email subject is required");
        }
        
        if (message.getMessage() == null && message.getTemplateId() == null) {
            throw new IllegalArgumentException("Email content or template is required");
        }
    }

    /**
     * Check if notification message is for email delivery.
     */
    private boolean isEmailNotification(NotificationMessage message) {
        return message.getEmailTo() != null && !message.getEmailTo().trim().isEmpty();
    }

    /**
     * Exception for email queue processing failures.
     */
    public static class EmailQueueException extends RuntimeException {
        public EmailQueueException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}