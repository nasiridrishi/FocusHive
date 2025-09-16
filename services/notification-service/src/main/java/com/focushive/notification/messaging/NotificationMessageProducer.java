package com.focushive.notification.messaging;

import com.focushive.notification.messaging.dto.NotificationMessage;
import com.focushive.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for producing notification messages to RabbitMQ.
 * 
 * Handles sending notification messages to appropriate queues
 * with retry logic and dead letter queue support.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${notification.queue.exchange:focushive.notifications}")
    private String exchangeName;

    @Value("${notification.queue.dlq.exchange:focushive.notifications.dlx}")
    private String dlxExchangeName;

    /**
     * Send a notification message to the default queue.
     *
     * @param message the notification message to send
     */
    public void sendNotification(NotificationMessage message) {
        validateMessage(message);
        
        log.debug("Sending notification message: {}", message);
        rabbitTemplate.convertAndSend("notification.created", message);
        log.info("Notification message sent for user: {} with ID: {}", 
                message.getUserId(), message.getNotificationId());
    }

    /**
     * Send a high-priority notification message.
     *
     * @param message the notification message to send
     */
    public void sendPriorityNotification(NotificationMessage message) {
        validateMessage(message);
        
        log.debug("Sending priority notification: {}", message);
        
        MessagePostProcessor postProcessor = msg -> {
            msg.getMessageProperties().setPriority(message.getPriority());
            return msg;
        };
        
        rabbitTemplate.convertAndSend(
                "notification.priority.high",
                message,
                postProcessor
        );
        
        log.info("Priority notification sent with priority: {}", message.getPriority());
    }

    /**
     * Send an email notification message.
     *
     * @param message the notification message with email details
     */
    public void sendEmailNotification(NotificationMessage message) {
        validateMessage(message);
        
        if (message.getEmailTo() == null || message.getEmailTo().isEmpty()) {
            throw new IllegalArgumentException("Email recipient is required");
        }
        
        log.debug("Sending email notification to: {}", message.getEmailTo());
        rabbitTemplate.convertAndSend("notification.email.send", message);
        log.info("Email notification queued for: {}", message.getEmailTo());
    }

    /**
     * Convert a Notification entity to a NotificationMessage.
     *
     * @param notification the notification entity
     * @return the corresponding message
     */
    public NotificationMessage createMessage(Notification notification) {
        return NotificationMessage.builder()
                .notificationId(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getContent())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Send a message with correlation data for tracking.
     *
     * @param message the notification message
     */
    public void sendWithCorrelationData(NotificationMessage message) {
        validateMessage(message);
        
        if (message.getCorrelationId() == null) {
            message.setCorrelationId(UUID.randomUUID().toString());
        }
        
        log.debug("Sending message with correlation ID: {}", message.getCorrelationId());
        rabbitTemplate.convertAndSend(
                exchangeName,
                "notification.created",
                message
        );
    }

    /**
     * Check if a message can be retried.
     *
     * @param message the notification message
     * @return true if the message can be retried
     */
    public boolean canRetry(NotificationMessage message) {
        return message.getRetryCount() < message.getMaxRetries();
    }

    /**
     * Prepare a message for retry by incrementing the retry count.
     *
     * @param message the notification message
     * @return the message prepared for retry
     */
    public NotificationMessage prepareRetry(NotificationMessage message) {
        message.setRetryCount(message.getRetryCount() + 1);
        message.setTimestamp(LocalDateTime.now());
        
        log.info("Preparing retry #{} for notification: {}", 
                message.getRetryCount(), message.getNotificationId());
        
        return message;
    }

    /**
     * Send a message to the dead letter queue.
     *
     * @param message the notification message
     * @param reason the reason for sending to DLQ
     */
    public void sendToDeadLetterQueue(NotificationMessage message, String reason) {
        log.warn("Sending message to DLQ. Reason: {}, Message: {}", reason, message);
        
        MessagePostProcessor postProcessor = msg -> {
            msg.getMessageProperties().setHeader("x-failure-reason", reason);
            msg.getMessageProperties().setHeader("x-original-queue", "notifications");
            msg.getMessageProperties().setHeader("x-failed-at", LocalDateTime.now().toString());
            return msg;
        };
        
        rabbitTemplate.convertAndSend(
                dlxExchangeName,
                "notification.failed",
                message,
                postProcessor
        );
        
        log.error("Message sent to DLQ for notification: {} due to: {}", 
                message.getNotificationId(), reason);
    }

    /**
     * Send multiple notifications in batch.
     *
     * @param messages the notification messages to send
     */
    public void sendBatch(NotificationMessage... messages) {
        for (NotificationMessage message : messages) {
            try {
                sendNotification(message);
            } catch (Exception e) {
                log.error("Failed to send batch message: {}", message, e);
                if (canRetry(message)) {
                    NotificationMessage retryMessage = prepareRetry(message);
                    sendNotification(retryMessage);
                } else {
                    sendToDeadLetterQueue(message, "Batch send failed: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Send a notification asynchronously.
     *
     * @param message the notification message
     * @return a boolean indicating success
     */
    public Boolean sendAsync(NotificationMessage message) {
        validateMessage(message);
        
        try {
            if (message.getCorrelationId() == null) {
                message.setCorrelationId(UUID.randomUUID().toString());
            }
            
            rabbitTemplate.convertAndSend(
                    exchangeName,
                    "notification.created",
                    message
            );
            
            log.debug("Async message sent successfully");
            return true;
            
        } catch (Exception e) {
            log.error("Error sending async message", e);
            return false;
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
        if (message.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }
    }
}