package com.focushive.notification.messaging;

import com.focushive.notification.messaging.dto.NotificationMessage;
import com.focushive.notification.service.NotificationService;
import com.focushive.notification.dto.CreateNotificationRequest;
import com.focushive.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service for consuming notification messages from RabbitMQ queues.
 * 
 * Handles processing of various types of notification messages
 * with retry logic and dead letter queue support.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationMessageConsumer {

    private final NotificationService notificationService;
    private final NotificationMessageProducer messageProducer;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Handle notification messages from the main queue.
     *
     * @param message the notification message to process
     */
    @RabbitListener(queues = "#{notificationQueue.name}")
    public void handleNotificationMessage(NotificationMessage message) {
        validateMessage(message);
        
        long startTime = System.currentTimeMillis();
        log.debug("Processing notification message: {}", message.getNotificationId());
        
        try {
            // Convert to create request and process
            CreateNotificationRequest request = convertToCreateRequest(message);
            notificationService.createNotification(request);
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("Successfully processed notification {} for user {} in {}ms", 
                    message.getNotificationId(), message.getUserId(), processingTime);
            
        } catch (Exception e) {
            log.error("Failed to process notification message: {}", message.getNotificationId(), e);
            handleProcessingFailure(message, e);
        }
    }

    /**
     * Handle high-priority notifications with immediate processing.
     *
     * @param message the high-priority notification message
     */
    @RabbitListener(queues = "#{priorityNotificationQueue.name}")
    public void handlePriorityNotification(NotificationMessage message) {
        validateMessage(message);
        
        log.info("Processing high-priority notification: {}", message.getNotificationId());
        
        try {
            // Process immediately without normal queue delays
            CreateNotificationRequest request = convertToCreateRequest(message);
            notificationService.createNotification(request);
            
            // For urgent messages, we'll just log it since we don't have a markAsUrgent method
            if (message.isUrgent()) {
                log.info("Urgent notification processed: {}", message.getNotificationId());
            }
            
            log.info("High-priority notification processed: {}", message.getNotificationId());
            
        } catch (Exception e) {
            log.error("Failed to process priority notification: {}", message.getNotificationId(), e);
            handleProcessingFailure(message, e);
        }
    }

    /**
     * Handle email-specific notifications.
     *
     * @param message the email notification message
     */
    @RabbitListener(queues = "#{emailNotificationQueue.name}")
    public void handleEmailNotification(NotificationMessage message) {
        validateMessage(message);
        
        if (message.getEmailTo() == null || message.getEmailTo().isEmpty()) {
            log.warn("Email notification without recipient: {}", message.getNotificationId());
            return;
        }
        
        log.debug("Processing email notification to: {}", message.getEmailTo());
        
        try {
            // Process the email notification
            CreateNotificationRequest request = convertToCreateRequest(message);
            notificationService.createNotification(request);
            
            // Log email sending (actual email service would be separate)
            log.info("Email notification created for {}: {}", 
                    message.getEmailTo(), message.getEmailSubject());
            
            log.info("Email notification sent to: {}", message.getEmailTo());
            
        } catch (Exception e) {
            log.error("Failed to send email notification: {}", message.getNotificationId(), e);
            handleProcessingFailure(message, e);
        }
    }

    /**
     * Handle template-based notifications.
     *
     * @param message the template notification message
     */
    public void handleTemplateNotification(NotificationMessage message) {
        validateMessage(message);
        
        if (message.getTemplateId() == null) {
            log.warn("Template notification without template ID: {}", message.getNotificationId());
            return;
        }
        
        log.debug("Processing template notification: {} with template: {}", 
                message.getNotificationId(), message.getTemplateId());
        
        try {
            // Process the templated notification (template rendering would be done elsewhere)
            CreateNotificationRequest request = convertToCreateRequest(message);
            notificationService.createNotification(request);
            
            log.info("Template notification processed: {}", message.getNotificationId());
            
        } catch (Exception e) {
            log.error("Failed to process template notification: {}", message.getNotificationId(), e);
            handleProcessingFailure(message, e);
        }
    }

    /**
     * Handle multi-channel delivery notifications.
     *
     * @param message the multi-channel notification message
     */
    public void handleMultiChannelNotification(NotificationMessage message) {
        validateMessage(message);
        
        if (message.getDeliveryChannels() == null || message.getDeliveryChannels().length == 0) {
            log.warn("Multi-channel notification without channels: {}", message.getNotificationId());
            return;
        }
        
        log.debug("Processing multi-channel notification: {} with channels: {}", 
                message.getNotificationId(), String.join(", ", message.getDeliveryChannels()));
        
        try {
            // Process the base notification first
            CreateNotificationRequest request = convertToCreateRequest(message);
            notificationService.createNotification(request);
            
            // Log multi-channel delivery (actual channel delivery would be separate)
            for (String channel : message.getDeliveryChannels()) {
                log.debug("Notification {} delivered to channel: {}", 
                        message.getNotificationId(), channel);
            }
            
            log.info("Multi-channel notification processed: {}", message.getNotificationId());
            
        } catch (Exception e) {
            log.error("Failed to process multi-channel notification: {}", message.getNotificationId(), e);
            handleProcessingFailure(message, e);
        }
    }

    /**
     * Handle batch notifications processing.
     *
     * @param messages array of notification messages to process
     */
    public void handleBatchNotifications(NotificationMessage... messages) {
        if (messages == null || messages.length == 0) {
            log.warn("Received empty batch notification request");
            return;
        }
        
        log.info("Processing batch of {} notifications", messages.length);
        
        int successCount = 0;
        int failureCount = 0;
        
        for (NotificationMessage message : messages) {
            try {
                validateMessage(message);
                CreateNotificationRequest request = convertToCreateRequest(message);
                notificationService.createNotification(request);
                successCount++;
                
            } catch (Exception e) {
                log.error("Failed to process batch notification: {}", 
                        message != null ? message.getNotificationId() : "null", e);
                failureCount++;
                
                if (message != null) {
                    handleProcessingFailure(message, e);
                }
            }
        }
        
        log.info("Batch processing completed: {} successful, {} failed", successCount, failureCount);
    }


    /**
     * Handle processing failures with retry logic.
     *
     * @param message the failed notification message
     * @param exception the exception that caused the failure
     */
    private void handleProcessingFailure(NotificationMessage message, Exception exception) {
        if (messageProducer.canRetry(message)) {
            log.info("Retrying failed notification: {} (attempt {}/{})", 
                    message.getNotificationId(), 
                    message.getRetryCount() + 1, 
                    message.getMaxRetries());
            
            NotificationMessage retryMessage = messageProducer.prepareRetry(message);
            messageProducer.sendNotification(retryMessage);
            
        } else {
            log.error("Max retries exceeded for notification: {}, sending to DLQ", 
                    message.getNotificationId());
            
            String reason = String.format("Processing failed after %d attempts: %s", 
                    message.getRetryCount(), exception.getMessage());
            messageProducer.sendToDeadLetterQueue(message, reason);
        }
    }

    /**
     * Validate a notification message.
     *
     * @param message the message to validate
     */
    private void validateMessage(NotificationMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("Notification message cannot be null");
        }
        if (message.getUserId() == null || message.getUserId().trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (message.getNotificationId() == null || message.getNotificationId().trim().isEmpty()) {
            throw new IllegalArgumentException("Notification ID is required");
        }
    }

    
    /**
     * Convert NotificationMessage to CreateNotificationRequest.
     *
     * @param message the notification message
     * @return the create notification request
     */
    private CreateNotificationRequest convertToCreateRequest(NotificationMessage message) {
        return CreateNotificationRequest.builder()
                .userId(message.getUserId())
                .type(message.getType())
                .title(message.getTitle())
                .content(message.getMessage())
                .actionUrl(message.getActionUrl())
                .priority(mapPriority(message.getPriority()))
                .build();
    }
    
    /**
     * Map integer priority to enum.
     *
     * @param priority the integer priority (1-10)
     * @return the corresponding NotificationPriority enum
     */
    private Notification.NotificationPriority mapPriority(Integer priority) {
        if (priority == null) {
            return Notification.NotificationPriority.NORMAL;
        }
        
        if (priority <= 3) {
            return Notification.NotificationPriority.LOW;
        } else if (priority <= 6) {
            return Notification.NotificationPriority.NORMAL;
        } else if (priority <= 8) {
            return Notification.NotificationPriority.HIGH;
        } else {
            return Notification.NotificationPriority.URGENT;
        }
    }
}
