package com.focushive.notification.entity;

import com.focushive.notification.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Entity representing notification templates for different types and languages.
 * Templates support variable substitution using {{variableName}} syntax.
 */
@Entity
@Table(name = "notification_templates",
       uniqueConstraints = @UniqueConstraint(columnNames = {"notification_type", "language"}),
       indexes = {
           @Index(name = "idx_notification_templates_type", columnList = "notification_type"),
           @Index(name = "idx_notification_templates_lang", columnList = "language")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate extends BaseEntity {

    /**
     * Pattern for matching template variables in format {{variableName}}
     */
    private static final Pattern TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    /**
     * Type of notification this template is for
     */
    @NotNull(message = "Notification type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    /**
     * Language code for this template (e.g., "en", "es", "fr")
     */
    @NotBlank(message = "Language is required")
    @Size(min = 2, max = 5, message = "Language code must be 2-5 characters")
    @Column(name = "language", nullable = false, length = 5)
    private String language;

    /**
     * Subject line for email notifications (supports template variables)
     */
    @Size(max = 200, message = "Subject must not exceed 200 characters")
    @Column(name = "subject", length = 200)
    private String subject;

    /**
     * Plain text body for notifications (supports template variables)
     */
    @NotBlank(message = "Body text is required")
    @Column(name = "body_text", columnDefinition = "TEXT")
    private String bodyText;

    /**
     * HTML body for email notifications (supports template variables)
     */
    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;

    /**
     * Processes a template string by replacing variables with actual values.
     * Variables should be in the format {{variableName}}.
     * 
     * @param template the template string to process
     * @param variables map of variable names to their values
     * @return processed string with variables replaced
     */
    public String processTemplate(String template, Map<String, Object> variables) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        if (variables == null || variables.isEmpty()) {
            return template;
        }

        Matcher matcher = TEMPLATE_VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);
            
            String replacement = value != null ? value.toString() : matcher.group(0);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Gets the processed subject with variables replaced.
     * 
     * @param variables map of variable names to their values
     * @return processed subject
     */
    public String getProcessedSubject(Map<String, Object> variables) {
        return processTemplate(subject, variables);
    }

    /**
     * Gets the processed body text with variables replaced.
     * 
     * @param variables map of variable names to their values
     * @return processed body text
     */
    public String getProcessedBodyText(Map<String, Object> variables) {
        return processTemplate(bodyText, variables);
    }

    /**
     * Gets the processed HTML body with variables replaced.
     * 
     * @param variables map of variable names to their values
     * @return processed HTML body
     */
    public String getProcessedBodyHtml(Map<String, Object> variables) {
        return processTemplate(bodyHtml, variables);
    }

    /**
     * Gets a unique key for this template based on type and language.
     * 
     * @return unique template key
     */
    public String getTemplateKey() {
        return notificationType.name() + "_" + language;
    }

    /**
     * Checks if this template has HTML content.
     * 
     * @return true if HTML body is not null and not empty
     */
    public boolean hasHtmlContent() {
        return bodyHtml != null && !bodyHtml.trim().isEmpty();
    }

    /**
     * Checks if this template has a subject line.
     * 
     * @return true if subject is not null and not empty
     */
    public boolean hasSubject() {
        return subject != null && !subject.trim().isEmpty();
    }
}