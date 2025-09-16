package com.focushive.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Email request DTO with comprehensive validation.
 * Supports both direct HTML content and template-based rendering.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String to;

    @Size(max = 10, message = "Maximum 10 CC recipients allowed")
    private List<@Email(message = "Invalid CC email format") String> cc;

    @Size(max = 10, message = "Maximum 10 BCC recipients allowed")
    private List<@Email(message = "Invalid BCC email format") String> bcc;

    @NotBlank(message = "Subject is required")
    @Size(max = 255, message = "Subject must not exceed 255 characters")
    private String subject;

    /**
     * Direct HTML content for the email.
     * Either htmlContent or templateName must be provided.
     */
    @Size(max = 100000, message = "Email content must not exceed 100KB")
    private String htmlContent;

    /**
     * Template name for template-based rendering.
     * Either htmlContent or templateName must be provided.
     */
    @Size(max = 100, message = "Template name must not exceed 100 characters")
    private String templateName;

    /**
     * Variables to be used with template rendering.
     * Only applicable when templateName is provided.
     */
    private Map<String, Object> variables;

    /**
     * Whether the content is HTML or plain text.
     * Default is true (HTML).
     */
    @Builder.Default
    private boolean html = true;

    /**
     * Priority level for the email.
     */
    @Builder.Default
    private EmailPriority priority = EmailPriority.NORMAL;

    /**
     * Optional reply-to address.
     */
    @Email(message = "Invalid reply-to email format")
    private String replyTo;

    /**
     * Optional attachments (file paths or URLs).
     */
    @Size(max = 5, message = "Maximum 5 attachments allowed")
    private List<String> attachments;

    /**
     * User ID associated with this email request.
     * Used for tracking and rate limiting.
     */
    private Long userId;

    /**
     * Notification type for categorization.
     */
    private String notificationType;

    /**
     * Email priority levels.
     */
    public enum EmailPriority {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }
}