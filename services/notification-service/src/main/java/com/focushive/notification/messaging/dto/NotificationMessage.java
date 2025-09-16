package com.focushive.notification.messaging.dto;

import com.focushive.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Message DTO for notification events sent through RabbitMQ.
 * 
 * This class represents the payload of notification messages
 * that are published to and consumed from the message queue.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Unique identifier for the notification
     */
    private String notificationId;
    
    /**
     * User ID who will receive the notification
     */
    private String userId;
    
    /**
     * Type of notification
     */
    private NotificationType type;
    
    /**
     * Notification title
     */
    private String title;
    
    /**
     * Notification message content
     */
    private String message;
    
    /**
     * Additional data as key-value pairs
     */
    private Map<String, Object> data;
    
    /**
     * Priority level (1-10, where 10 is highest)
     */
    @Builder.Default
    private Integer priority = 5;
    
    /**
     * Template ID if using notification templates
     */
    private Long templateId;
    
    /**
     * Template variables for substitution
     */
    private Map<String, String> templateVariables;
    
    /**
     * Timestamp when the message was created
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * Number of retry attempts
     */
    @Builder.Default
    private Integer retryCount = 0;
    
    /**
     * Maximum number of retries allowed
     */
    @Builder.Default
    private Integer maxRetries = 3;
    
    /**
     * Channels to deliver the notification (EMAIL, IN_APP, PUSH)
     */
    private String[] deliveryChannels;
    
    /**
     * Email-specific fields
     */
    private String emailTo;
    private String emailSubject;
    private String emailFrom;
    
    /**
     * Correlation ID for tracking across services
     */
    private String correlationId;
    
    /**
     * Whether this is an urgent notification
     */
    @Builder.Default
    private boolean urgent = false;
    
    /**
     * Action URL for clickable notifications
     */
    private String actionUrl;
    
    /**
     * Category for grouping notifications
     */
    private String category;
}