package com.focushive.notification.dto;

import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.entity.Notification;
import com.focushive.notification.validation.XSSSafe;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for creating a new notification.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNotificationRequest {

    /**
     * ID of the user who should receive the notification
     */
    @NotNull(message = "User ID is required")
    @Size(min = 1, max = 50, message = "User ID must be between 1 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9-_]+$", message = "User ID contains invalid characters")
    private String userId;

    /**
     * Type of notification being created
     */
    @NotNull(message = "Notification type is required")
    private NotificationType type;

    /**
     * Title of the notification
     */
    @NotNull(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    @XSSSafe(message = "Title contains potentially dangerous content")
    private String title;

    /**
     * Content/body of the notification (optional)
     */
    @Size(max = 5000, message = "Content must not exceed 5000 characters")
    @XSSSafe(allowBasicHtml = true, message = "Content contains potentially dangerous content")
    private String content;

    /**
     * URL the user should be directed to when clicking the notification (optional)
     */
    @Size(max = 500, message = "Action URL must not exceed 500 characters")
    private String actionUrl;

    /**
     * Priority level of the notification
     */
    @Builder.Default
    private Notification.NotificationPriority priority = Notification.NotificationPriority.NORMAL;

    /**
     * Additional data to be stored with the notification as JSON (optional)
     */
    private Map<String, Object> data;

    /**
     * Variables for template processing (optional)
     * Used to replace placeholders in notification templates
     */
    private Map<String, Object> variables;

    /**
     * Language preference for the notification template (optional)
     * Defaults to "en" if not specified
     */
    @Builder.Default
    private String language = "en";

    /**
     * Whether to force delivery even during quiet hours (optional)
     * Defaults to false
     */
    @Builder.Default
    private Boolean forceDelivery = false;

    /**
     * Additional metadata for the notification (optional)
     * Can be either a NotificationMetadata object or a Map<String, Object>
     */
    @Valid
    private NotificationMetadata metadata;
    
    /**
     * Raw metadata as Map (alternative to structured metadata)
     * Used when metadata comes as a simple Map from other services
     */
    private Map<String, Object> metadataMap;
}