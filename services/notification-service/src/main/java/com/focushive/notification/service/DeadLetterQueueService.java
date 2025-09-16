package com.focushive.notification.service;

import com.focushive.notification.dto.EmailRequest;
import com.focushive.notification.entity.DeadLetterMessage;
import com.focushive.notification.repository.DeadLetterMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dead Letter Queue service for handling failed notifications.
 *
 * Responsibilities:
 * - Process messages from dead letter queue
 * - Store failed messages for manual review
 * - Provide retry mechanism for failed messages
 * - Generate alerts for critical failures
 * - Maintain audit trail of failed messages
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterQueueService {

    private final DeadLetterMessageRepository deadLetterRepository;
    private final EmailMetricsService metricsService;
    private final AsyncEmailService emailService;

    /**
     * Listens to the main dead letter queue for general failures.
     */
    @RabbitListener(queues = "${notification.queue.dlq.name:notifications.dlq}")
    @Transactional
    public void handleDeadLetterMessage(Message message, EmailRequest emailRequest) {
        log.error("Processing dead letter message for recipient: {}", emailRequest.getTo());

        try {
            // Store in database for manual review
            DeadLetterMessage dlqMessage = DeadLetterMessage.builder()
                .messageId(message.getMessageProperties().getMessageId())
                .recipient(emailRequest.getTo())
                .subject(emailRequest.getSubject())
                .content(emailRequest.getHtmlContent())
                .errorMessage(getErrorMessage(message))
                .retryCount(getRetryCount(message))
                .originalQueue(getOriginalQueue(message))
                .createdAt(LocalDateTime.now())
                .status(DeadLetterMessage.Status.PENDING)
                .build();

            deadLetterRepository.save(dlqMessage);

            // Record metrics
            metricsService.recordEmailDeadLetter();

            // Send alert for critical emails
            if (emailRequest.getPriority() == EmailRequest.EmailPriority.CRITICAL) {
                sendCriticalFailureAlert(emailRequest, dlqMessage);
            }

            log.info("Dead letter message stored with ID: {}", dlqMessage.getId());

        } catch (Exception e) {
            log.error("Failed to process dead letter message", e);
            // At this point, we've exhausted all options
            // Log to external monitoring system would go here
        }
    }

    /**
     * Listens to the email-specific dead letter queue.
     */
    @RabbitListener(queues = "focushive.notifications.email.dlq")
    @Transactional
    public void handleEmailDeadLetterMessage(Message message, EmailRequest emailRequest) {
        log.error("Processing email dead letter for: {}", emailRequest.getTo());

        // Similar processing but with email-specific handling
        handleDeadLetterMessage(message, emailRequest);

        // Additional email-specific processing
        checkForBounce(emailRequest);
    }

    /**
     * Listens to the priority dead letter queue.
     */
    @RabbitListener(queues = "focushive.notifications.priority.dlq")
    @Transactional
    public void handlePriorityDeadLetterMessage(Message message, EmailRequest emailRequest) {
        log.error("CRITICAL: Priority message failed for: {}", emailRequest.getTo());

        // Process with higher urgency
        handleDeadLetterMessage(message, emailRequest);

        // Immediate alert for priority failures
        sendImmediateAlert(emailRequest);
    }

    /**
     * Manually retry a dead letter message.
     */
    @Transactional
    public boolean retryDeadLetterMessage(Long messageId) {
        return deadLetterRepository.findById(messageId)
            .map(dlqMessage -> {
                if (dlqMessage.getRetryCount() >= 3) {
                    log.warn("Max retries exceeded for message: {}", messageId);
                    dlqMessage.setStatus(DeadLetterMessage.Status.MAX_RETRIES_EXCEEDED);
                    deadLetterRepository.save(dlqMessage);
                    return false;
                }

                try {
                    // Rebuild email request
                    EmailRequest retryRequest = EmailRequest.builder()
                        .to(dlqMessage.getRecipient())
                        .subject(dlqMessage.getSubject())
                        .htmlContent(dlqMessage.getContent())
                        .priority(EmailRequest.EmailPriority.HIGH) // Elevate priority
                        .build();

                    // Attempt to resend
                    emailService.sendEmailAsync(retryRequest);

                    // Update status
                    dlqMessage.setStatus(DeadLetterMessage.Status.RETRIED);
                    dlqMessage.setRetryCount(dlqMessage.getRetryCount() + 1);
                    dlqMessage.setRetriedAt(LocalDateTime.now());
                    deadLetterRepository.save(dlqMessage);

                    log.info("Successfully retried dead letter message: {}", messageId);
                    return true;

                } catch (Exception e) {
                    log.error("Failed to retry dead letter message: {}", messageId, e);
                    dlqMessage.setStatus(DeadLetterMessage.Status.RETRY_FAILED);
                    dlqMessage.setErrorMessage(dlqMessage.getErrorMessage() + " | Retry failed: " + e.getMessage());
                    deadLetterRepository.save(dlqMessage);
                    return false;
                }
            })
            .orElse(false);
    }

    /**
     * Get all pending dead letter messages for review.
     */
    public List<DeadLetterMessage> getPendingMessages() {
        return deadLetterRepository.findByStatus(DeadLetterMessage.Status.PENDING);
    }

    /**
     * Mark a dead letter message as resolved.
     */
    @Transactional
    public void markAsResolved(Long messageId, String resolution) {
        deadLetterRepository.findById(messageId).ifPresent(dlqMessage -> {
            dlqMessage.setStatus(DeadLetterMessage.Status.RESOLVED);
            dlqMessage.setResolution(resolution);
            dlqMessage.setResolvedAt(LocalDateTime.now());
            deadLetterRepository.save(dlqMessage);
            log.info("Dead letter message {} marked as resolved", messageId);
        });
    }

    /**
     * Clean up old dead letter messages.
     */
    @Transactional
    public int cleanupOldMessages(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        List<DeadLetterMessage> oldMessages = deadLetterRepository.findByCreatedAtBeforeAndStatusIn(
            cutoffDate,
            List.of(DeadLetterMessage.Status.RESOLVED, DeadLetterMessage.Status.EXPIRED)
        );

        deadLetterRepository.deleteAll(oldMessages);
        log.info("Cleaned up {} old dead letter messages", oldMessages.size());
        return oldMessages.size();
    }

    /**
     * Extract error message from RabbitMQ message properties.
     */
    private String getErrorMessage(Message message) {
        Object deathReason = message.getMessageProperties()
            .getHeaders()
            .get("x-first-death-reason");

        if (deathReason != null) {
            return deathReason.toString();
        }

        Object exception = message.getMessageProperties()
            .getHeaders()
            .get("x-exception-message");

        return exception != null ? exception.toString() : "Unknown error";
    }

    /**
     * Get retry count from message headers.
     */
    private int getRetryCount(Message message) {
        Object retryCount = message.getMessageProperties()
            .getHeaders()
            .get("x-death-count");

        return retryCount != null ? ((Number) retryCount).intValue() : 0;
    }

    /**
     * Get original queue name from message headers.
     */
    private String getOriginalQueue(Message message) {
        Object queue = message.getMessageProperties()
            .getHeaders()
            .get("x-first-death-queue");

        return queue != null ? queue.toString() : "unknown";
    }

    /**
     * Send alert for critical email failures.
     */
    private void sendCriticalFailureAlert(EmailRequest emailRequest, DeadLetterMessage dlqMessage) {
        // Send alert to admin
        EmailRequest alertRequest = EmailRequest.builder()
            .to("admin@focushive.com") // Should be configurable
            .subject("CRITICAL: Email Delivery Failed")
            .htmlContent(String.format(
                "<h2>Critical Email Failure</h2>" +
                "<p><strong>Original Recipient:</strong> %s</p>" +
                "<p><strong>Subject:</strong> %s</p>" +
                "<p><strong>Error:</strong> %s</p>" +
                "<p><strong>Message ID:</strong> %d</p>" +
                "<p>Please review in the admin panel.</p>",
                emailRequest.getTo(),
                emailRequest.getSubject(),
                dlqMessage.getErrorMessage(),
                dlqMessage.getId()
            ))
            .priority(EmailRequest.EmailPriority.CRITICAL)
            .build();

        try {
            emailService.sendEmailAsync(alertRequest);
        } catch (Exception e) {
            log.error("Failed to send critical failure alert", e);
        }
    }

    /**
     * Send immediate alert for priority failures.
     */
    private void sendImmediateAlert(EmailRequest emailRequest) {
        log.error("IMMEDIATE ALERT: Priority email failed for {}", emailRequest.getTo());
        // Integration with external alerting system would go here
        // e.g., PagerDuty, Slack, SMS
    }

    /**
     * Check if email failure is due to bounce.
     */
    private void checkForBounce(EmailRequest emailRequest) {
        // Check bounce patterns in error messages
        // Update user preferences if email is bouncing
        // This would integrate with bounce handling service
        log.debug("Checking for bounce patterns for: {}", emailRequest.getTo());
    }

    /**
     * Get statistics about dead letter queue.
     */
    public DeadLetterStatistics getStatistics() {
        return DeadLetterStatistics.builder()
            .totalMessages(deadLetterRepository.count())
            .pendingMessages(deadLetterRepository.countByStatus(DeadLetterMessage.Status.PENDING))
            .resolvedMessages(deadLetterRepository.countByStatus(DeadLetterMessage.Status.RESOLVED))
            .failedRetries(deadLetterRepository.countByStatus(DeadLetterMessage.Status.RETRY_FAILED))
            .build();
    }

    /**
     * Statistics DTO for dead letter queue.
     */
    @lombok.Data
    @lombok.Builder
    public static class DeadLetterStatistics {
        private long totalMessages;
        private long pendingMessages;
        private long resolvedMessages;
        private long failedRetries;
    }
}