package com.focushive.notification.service;

import com.focushive.notification.entity.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO representing a processed template with all variables substituted.
 * Used to return the final rendered content for notifications.
 */
@Data
@Builder
public class ProcessedTemplate {

    /**
     * The notification type this template was processed for.
     */
    private NotificationType notificationType;

    /**
     * The language code used for template processing.
     */
    private String language;

    /**
     * The processed subject line with variables substituted.
     */
    private String subject;

    /**
     * The processed plain text body with variables substituted.
     */
    private String bodyText;

    /**
     * The processed HTML body with variables substituted.
     */
    private String bodyHtml;

    /**
     * Timestamp when this template was processed.
     */
    private LocalDateTime processedAt;

    /**
     * The original template ID that was processed.
     */
    private String templateId;

    /**
     * Whether this template has HTML content.
     */
    private boolean hasHtmlContent;

    /**
     * Whether this template has a subject line.
     */
    private boolean hasSubject;

    /**
     * Number of variables that were substituted during processing.
     */
    private int variableCount;

    /**
     * Create a ProcessedTemplate from the rendered content.
     *
     * @param notificationType the notification type
     * @param language the language code
     * @param templateId the original template ID
     * @param subject processed subject
     * @param bodyText processed text body
     * @param bodyHtml processed HTML body
     * @param variableCount number of variables substituted
     * @return ProcessedTemplate instance
     */
    public static ProcessedTemplate create(NotificationType notificationType, String language, 
                                         String templateId, String subject, String bodyText, 
                                         String bodyHtml, int variableCount) {
        return ProcessedTemplate.builder()
                .notificationType(notificationType)
                .language(language)
                .templateId(templateId)
                .subject(subject)
                .bodyText(bodyText)
                .bodyHtml(bodyHtml)
                .hasHtmlContent(bodyHtml != null && !bodyHtml.trim().isEmpty())
                .hasSubject(subject != null && !subject.trim().isEmpty())
                .variableCount(variableCount)
                .processedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Check if this processed template is suitable for email delivery.
     * Email templates should have at least a subject or body content.
     *
     * @return true if suitable for email
     */
    public boolean isSuitableForEmail() {
        return hasSubject || (bodyText != null && !bodyText.trim().isEmpty());
    }

    /**
     * Check if this processed template prefers HTML content over plain text.
     *
     * @return true if HTML content should be used
     */
    public boolean prefersHtmlContent() {
        return hasHtmlContent && bodyHtml != null && bodyHtml.length() > (bodyText != null ? bodyText.length() : 0);
    }

    /**
     * Get the primary content body, preferring HTML if available.
     *
     * @return primary content body
     */
    public String getPrimaryContent() {
        if (hasHtmlContent && bodyHtml != null && !bodyHtml.trim().isEmpty()) {
            return bodyHtml;
        }
        return bodyText;
    }

    /**
     * Get a summary of the processing result.
     *
     * @return processing summary
     */
    public String getProcessingSummary() {
        return String.format("Template %s processed for %s/%s with %d variables at %s", 
                templateId, notificationType, language, variableCount, processedAt);
    }
}