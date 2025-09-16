package com.focushive.notification.messaging;

import com.focushive.notification.messaging.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for handling dead letter queue messages and providing DLQ statistics.
 * 
 * This service processes failed notification messages and provides monitoring
 * capabilities for dead letter queue operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterQueueHandler {

    private final RabbitTemplate rabbitTemplate;
    private final NotificationMessageProducer messageProducer;

    // Statistics tracking
    private final AtomicLong totalDeadLetterMessages = new AtomicLong(0);
    private final Map<String, AtomicLong> deadLetterReasonCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> deadLetterQueueCounts = new ConcurrentHashMap<>();

    /**
     * Handle messages from the main dead letter queue.
     *
     * @param message the failed notification message
     * @param rawMessage the raw RabbitMQ message with headers
     */
    @RabbitListener(queues = "#{deadLetterQueue.name}")
    public void handleDeadLetterMessage(NotificationMessage message, Message rawMessage) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Extract metadata from headers
            DeadLetterMessageMetadata metadata = extractMetadata(rawMessage);
            
            // Update statistics
            updateStatistics(metadata);
            
            log.warn("Processing dead letter message - ID: {}, User: {}, Reason: {}, Original Queue: {}, Failed At: {}", 
                    message.getNotificationId(), 
                    message.getUserId(),
                    metadata.getFailureReason(),
                    metadata.getOriginalQueue(),
                    metadata.getFailedAt());
            
            // Analyze if message can be recovered
            if (canAttemptRecovery(message, metadata)) {
                attemptMessageRecovery(message, metadata);
            } else {
                // Store for manual investigation
                storeForInvestigation(message, metadata);
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("Dead letter message processed in {}ms", processingTime);
            
        } catch (Exception e) {
            log.error("Failed to process dead letter message: {}", message.getNotificationId(), e);
        }
    }

    /**
     * Handle messages from the email dead letter queue.
     *
     * @param message the failed email notification message
     * @param rawMessage the raw RabbitMQ message with headers
     */
    @RabbitListener(queues = "#{emailDeadLetterQueue.name}")
    public void handleEmailDeadLetterMessage(NotificationMessage message, Message rawMessage) {
        DeadLetterMessageMetadata metadata = extractMetadata(rawMessage);
        updateStatistics(metadata);
        
        log.warn("Email notification failed - ID: {}, Recipient: {}, Reason: {}", 
                message.getNotificationId(), 
                message.getEmailTo(), 
                metadata.getFailureReason());
        
        // For email failures, we might want to try alternative delivery channels
        if (message.getDeliveryChannels() != null && message.getDeliveryChannels().length > 1) {
            log.info("Attempting delivery via alternative channels for notification: {}", 
                    message.getNotificationId());
            // Remove EMAIL channel and retry
            String[] alternativeChannels = removeEmailChannel(message.getDeliveryChannels());
            if (alternativeChannels.length > 0) {
                message.setDeliveryChannels(alternativeChannels);
                messageProducer.sendNotification(message);
            }
        } else {
            storeForInvestigation(message, metadata);
        }
    }

    /**
     * Handle messages from the priority dead letter queue.
     *
     * @param message the failed priority notification message
     * @param rawMessage the raw RabbitMQ message with headers
     */
    @RabbitListener(queues = "#{priorityDeadLetterQueue.name}")
    public void handlePriorityDeadLetterMessage(NotificationMessage message, Message rawMessage) {
        DeadLetterMessageMetadata metadata = extractMetadata(rawMessage);
        updateStatistics(metadata);
        
        log.error("CRITICAL: Priority notification failed - ID: {}, User: {}, Reason: {}", 
                message.getNotificationId(), 
                message.getUserId(), 
                metadata.getFailureReason());
        
        // Priority messages get special handling - notify administrators immediately
        notifyAdministrators(message, metadata);
        
        // Store for immediate investigation
        storeForInvestigation(message, metadata);
    }

    /**
     * Get dead letter queue statistics.
     *
     * @return map of DLQ statistics
     */
    public Map<String, Object> getDeadLetterStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDeadLetterMessages", totalDeadLetterMessages.get());
        stats.put("reasonCounts", new HashMap<>(deadLetterReasonCounts));
        stats.put("queueCounts", new HashMap<>(deadLetterQueueCounts));
        stats.put("timestamp", LocalDateTime.now());
        return stats;
    }

    /**
     * Reset dead letter statistics (for testing or maintenance).
     */
    public void resetStatistics() {
        totalDeadLetterMessages.set(0);
        deadLetterReasonCounts.clear();
        deadLetterQueueCounts.clear();
        log.info("Dead letter queue statistics have been reset");
    }

    /**
     * Check if a message can potentially be recovered.
     *
     * @param message the failed message
     * @param metadata the failure metadata
     * @return true if recovery should be attempted
     */
    private boolean canAttemptRecovery(NotificationMessage message, DeadLetterMessageMetadata metadata) {
        // Don't attempt recovery for certain types of failures
        if (metadata.getFailureReason() != null) {
            String reason = metadata.getFailureReason().toLowerCase();
            if (reason.contains("validation") || 
                reason.contains("malformed") || 
                reason.contains("invalid user") ||
                reason.contains("permanent")) {
                return false;
            }
        }
        
        // Don't attempt recovery if too much time has passed
        if (metadata.getFailedAt() != null) {
            try {
                LocalDateTime failedTime = LocalDateTime.parse(metadata.getFailedAt());
                if (failedTime.isBefore(LocalDateTime.now().minusHours(24))) {
                    return false;
                }
            } catch (Exception e) {
                log.warn("Could not parse failed timestamp: {}", metadata.getFailedAt());
            }
        }
        
        return true;
    }

    /**
     * Attempt to recover a failed message by resending it.
     *
     * @param message the failed message
     * @param metadata the failure metadata
     */
    private void attemptMessageRecovery(NotificationMessage message, DeadLetterMessageMetadata metadata) {
        log.info("Attempting recovery for message: {} (reason: {})", 
                message.getNotificationId(), metadata.getFailureReason());
        
        try {
            // Reset retry count to allow one more attempt
            message.setRetryCount(0);
            message.setTimestamp(LocalDateTime.now());
            
            // Add recovery marker to track this attempt
            if (message.getData() == null) {
                message.setData(new HashMap<>());
            }
            message.getData().put("dlq_recovery_attempt", LocalDateTime.now().toString());
            message.getData().put("original_failure_reason", metadata.getFailureReason());
            
            // Send back to original queue for processing
            messageProducer.sendNotification(message);
            
            log.info("Recovery attempt initiated for message: {}", message.getNotificationId());
            
        } catch (Exception e) {
            log.error("Failed to attempt recovery for message: {}", message.getNotificationId(), e);
            storeForInvestigation(message, metadata);
        }
    }

    /**
     * Store failed message for manual investigation.
     *
     * @param message the failed message
     * @param metadata the failure metadata
     */
    private void storeForInvestigation(NotificationMessage message, DeadLetterMessageMetadata metadata) {
        // In a real implementation, this would store to a database or file system
        // For now, we'll log it with structured information
        
        Map<String, Object> investigationData = new HashMap<>();
        investigationData.put("notificationId", message.getNotificationId());
        investigationData.put("userId", message.getUserId());
        investigationData.put("type", message.getType());
        investigationData.put("title", message.getTitle());
        investigationData.put("failureReason", metadata.getFailureReason());
        investigationData.put("originalQueue", metadata.getOriginalQueue());
        investigationData.put("failedAt", metadata.getFailedAt());
        investigationData.put("storedForInvestigationAt", LocalDateTime.now());
        
        log.error("STORED_FOR_INVESTIGATION: {}", investigationData);
        
        // TODO: Implement actual persistence layer for failed messages
        // This could be:
        // - Database table for failed messages
        // - File-based storage
        // - External monitoring system
    }

    /**
     * Notify administrators about critical failures.
     *
     * @param message the failed message
     * @param metadata the failure metadata
     */
    private void notifyAdministrators(NotificationMessage message, DeadLetterMessageMetadata metadata) {
        log.error("ADMIN_NOTIFICATION: Critical notification failure - ID: {}, Reason: {}", 
                message.getNotificationId(), metadata.getFailureReason());
        
        // TODO: Implement actual admin notification
        // This could be:
        // - Send email to administrators
        // - Create incident in monitoring system
        // - Send alert to Slack/Teams
        // - Create ticket in issue tracking system
    }

    /**
     * Extract metadata from RabbitMQ message headers.
     *
     * @param message the RabbitMQ message
     * @return the extracted metadata
     */
    private DeadLetterMessageMetadata extractMetadata(Message message) {
        MessageProperties properties = message.getMessageProperties();
        Map<String, Object> headers = properties.getHeaders();
        
        return DeadLetterMessageMetadata.builder()
                .failureReason(getHeaderValue(headers, "x-failure-reason", "Unknown"))
                .originalQueue(getHeaderValue(headers, "x-original-queue", "unknown"))
                .failedAt(getHeaderValue(headers, "x-failed-at", LocalDateTime.now().toString()))
                .retryCount(getHeaderValue(headers, "x-retry-count", "0"))
                .firstFailedAt(getHeaderValue(headers, "x-first-failed-at", 
                        getHeaderValue(headers, "x-failed-at", LocalDateTime.now().toString())))
                .build();
    }

    /**
     * Get header value with default.
     *
     * @param headers the message headers
     * @param key the header key
     * @param defaultValue the default value
     * @return the header value or default
     */
    private String getHeaderValue(Map<String, Object> headers, String key, String defaultValue) {
        Object value = headers.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Update statistics for dead letter message processing.
     *
     * @param metadata the message metadata
     */
    private void updateStatistics(DeadLetterMessageMetadata metadata) {
        totalDeadLetterMessages.incrementAndGet();
        
        // Count by failure reason
        deadLetterReasonCounts.computeIfAbsent(metadata.getFailureReason(), k -> new AtomicLong(0))
                .incrementAndGet();
        
        // Count by original queue
        deadLetterQueueCounts.computeIfAbsent(metadata.getOriginalQueue(), k -> new AtomicLong(0))
                .incrementAndGet();
    }

    /**
     * Remove EMAIL channel from delivery channels array.
     *
     * @param channels the original channels
     * @return channels without EMAIL
     */
    private String[] removeEmailChannel(String[] channels) {
        return java.util.Arrays.stream(channels)
                .filter(channel -> !"EMAIL".equalsIgnoreCase(channel))
                .toArray(String[]::new);
    }

    /**
     * Metadata extracted from dead letter message headers.
     */
    @lombok.Data
    @lombok.Builder
    private static class DeadLetterMessageMetadata {
        private String failureReason;
        private String originalQueue;
        private String failedAt;
        private String retryCount;
        private String firstFailedAt;
    }
}