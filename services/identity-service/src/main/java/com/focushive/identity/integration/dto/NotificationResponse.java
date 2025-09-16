package com.focushive.identity.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO from the notification service.
 * This DTO matches the notification-service API response contract.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationResponse {

    /**
     * Unique identifier of the notification
     */
    private String id;

    /**
     * ID of the user who received the notification
     */
    private String userId;

    /**
     * Type of notification
     */
    private String type;

    /**
     * Title of the notification
     */
    private String title;

    /**
     * Content of the notification
     */
    private String content;

    /**
     * Status of the notification (PENDING, SENT, DELIVERED, READ, FAILED)
     */
    private String status;

    /**
     * Priority level
     */
    private String priority;

    /**
     * When the notification was created
     */
    private LocalDateTime createdAt;

    /**
     * When the notification was read (if applicable)
     */
    private LocalDateTime readAt;

    /**
     * Additional metadata
     */
    private Map<String, Object> metadata;
}