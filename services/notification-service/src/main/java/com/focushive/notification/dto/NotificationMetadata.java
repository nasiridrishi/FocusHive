package com.focushive.notification.dto;

import com.focushive.notification.validation.XSSSafe;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Metadata DTO for notification with validation.
 * Contains additional fields that can be attached to notifications.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMetadata {

    /**
     * Optional email override for this specific notification.
     */
    @Email(message = "Invalid email format")
    private String emailOverride;

    /**
     * Additional tracking ID for external systems.
     */
    @Size(max = 100, message = "Tracking ID must not exceed 100 characters")
    @XSSSafe(message = "Tracking ID contains invalid characters")
    private String trackingId;

    /**
     * Source system or component that created this notification.
     */
    @Size(max = 50, message = "Source must not exceed 50 characters")
    @XSSSafe(message = "Source contains invalid characters")
    private String source;

    /**
     * Campaign or batch ID for grouping related notifications.
     */
    @Size(max = 50, message = "Campaign ID must not exceed 50 characters")
    @XSSSafe(message = "Campaign ID contains invalid characters")
    private String campaignId;

    /**
     * Tags for categorization and filtering.
     */
    @Size(max = 10, message = "Maximum 10 tags allowed")
    private Map<String, String> tags;

    /**
     * Custom properties for extending notification functionality.
     */
    @Size(max = 20, message = "Maximum 20 custom properties allowed")
    private Map<String, Object> customProperties;
}