package com.focushive.notification.dto;

import com.focushive.notification.entity.NotificationType;
import com.focushive.user.entity.Notification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotBlank(message = "User ID is required")
    private String userId;

    /**
     * Type of notification being created
     */
    @NotNull(message = "Notification type is required")
    private NotificationType type;

    /**
     * Title of the notification
     */
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    /**
     * Content/body of the notification (optional)
     */
    @Size(max = 2000, message = "Content must not exceed 2000 characters")
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
}