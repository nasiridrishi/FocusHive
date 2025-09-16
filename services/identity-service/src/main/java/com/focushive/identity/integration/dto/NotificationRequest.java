package com.focushive.identity.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for sending notifications via the notification service.
 * This DTO matches the notification-service API contract.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {

    /**
     * ID of the user who should receive the notification
     */
    private String userId;

    /**
     * Type of notification (e.g., WELCOME, PASSWORD_RESET, EMAIL_VERIFICATION)
     * Maps to NotificationType enum in notification-service
     */
    private String type;

    /**
     * Title of the notification
     */
    private String title;

    /**
     * Content/body of the notification
     */
    private String content;

    /**
     * URL the user should be directed to when clicking the notification
     */
    private String actionUrl;

    /**
     * Priority level of the notification (LOW, NORMAL, HIGH, CRITICAL)
     */
    @Builder.Default
    private String priority = "NORMAL";

    /**
     * Additional data/metadata for the notification
     */
    private Map<String, Object> metadata;

    /**
     * Variables for template processing
     */
    private Map<String, Object> variables;

    /**
     * Language preference for the notification
     */
    @Builder.Default
    private String language = "en";

    /**
     * Whether to force delivery even during quiet hours
     */
    @Builder.Default
    private Boolean forceDelivery = false;
}