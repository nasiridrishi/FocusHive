package com.focushive.notification.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTemplateTest {

    private Validator validator;
    private NotificationTemplate notificationTemplate;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        notificationTemplate = new NotificationTemplate();
        notificationTemplate.setNotificationType(NotificationType.HIVE_INVITATION);
        notificationTemplate.setLanguage("en");
        notificationTemplate.setSubject("You've been invited to join {{hiveName}}");
        notificationTemplate.setBodyText("Hi {{userName}}, you've been invited to join the hive '{{hiveName}}'.");
        notificationTemplate.setBodyHtml("<p>Hi {{userName}}, you've been invited to join the hive '<strong>{{hiveName}}</strong>'.</p>");
    }

    @Test
    void validNotificationTemplate_shouldPassValidation() {
        // When
        Set<ConstraintViolation<NotificationTemplate>> violations = validator.validate(notificationTemplate);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void notificationTemplate_withNullNotificationType_shouldFailValidation() {
        // Given
        notificationTemplate.setNotificationType(null);

        // When
        Set<ConstraintViolation<NotificationTemplate>> violations = validator.validate(notificationTemplate);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Notification type is required");
    }

    @Test
    void notificationTemplate_withBlankLanguage_shouldFailValidation() {
        // Given
        notificationTemplate.setLanguage("");

        // When
        Set<ConstraintViolation<NotificationTemplate>> violations = validator.validate(notificationTemplate);

        // Then
        assertThat(violations).hasSize(2);
        assertThat(violations.stream().map(v -> v.getMessage()))
            .containsExactlyInAnyOrder(
                "Language is required",
                "Language code must be 2-5 characters"
            );
    }

    @Test
    void notificationTemplate_withTooLongSubject_shouldFailValidation() {
        // Given
        String longSubject = "A".repeat(201); // Exceeds 200 character limit
        notificationTemplate.setSubject(longSubject);

        // When
        Set<ConstraintViolation<NotificationTemplate>> violations = validator.validate(notificationTemplate);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Subject must not exceed 200 characters");
    }

    @Test
    void notificationTemplate_withBlankBodyText_shouldFailValidation() {
        // Given
        notificationTemplate.setBodyText("");

        // When
        Set<ConstraintViolation<NotificationTemplate>> violations = validator.validate(notificationTemplate);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Body text is required");
    }

    @Test
    void notificationTemplate_shouldHaveCorrectGettersAndSetters() {
        // Given
        NotificationType type = NotificationType.TASK_COMPLETED;
        String language = "es";
        String subject = "Tarea completada";
        String bodyText = "La tarea '{{taskName}}' ha sido completada.";
        String bodyHtml = "<p>La tarea '<strong>{{taskName}}</strong>' ha sido completada.</p>";

        // When
        notificationTemplate.setNotificationType(type);
        notificationTemplate.setLanguage(language);
        notificationTemplate.setSubject(subject);
        notificationTemplate.setBodyText(bodyText);
        notificationTemplate.setBodyHtml(bodyHtml);

        // Then
        assertThat(notificationTemplate.getNotificationType()).isEqualTo(type);
        assertThat(notificationTemplate.getLanguage()).isEqualTo(language);
        assertThat(notificationTemplate.getSubject()).isEqualTo(subject);
        assertThat(notificationTemplate.getBodyText()).isEqualTo(bodyText);
        assertThat(notificationTemplate.getBodyHtml()).isEqualTo(bodyHtml);
    }

    @Test
    void processTemplate_withValidPlaceholders_shouldReplaceCorrectly() {
        // Given
        Map<String, Object> variables = Map.of(
            "userName", "John Doe",
            "hiveName", "Study Group",
            "inviterName", "Jane Smith"
        );

        // When
        String processedSubject = notificationTemplate.processTemplate(notificationTemplate.getSubject(), variables);
        String processedBodyText = notificationTemplate.processTemplate(notificationTemplate.getBodyText(), variables);

        // Then
        assertThat(processedSubject).isEqualTo("You've been invited to join Study Group");
        assertThat(processedBodyText).isEqualTo("Hi John Doe, you've been invited to join the hive 'Study Group'.");
    }

    @Test
    void processTemplate_withMissingPlaceholders_shouldKeepPlaceholders() {
        // Given
        Map<String, Object> variables = Map.of(
            "userName", "John Doe"
            // Missing hiveName
        );

        // When
        String processedSubject = notificationTemplate.processTemplate(notificationTemplate.getSubject(), variables);

        // Then
        assertThat(processedSubject).isEqualTo("You've been invited to join {{hiveName}}");
    }

    @Test
    void processTemplate_withEmptyVariables_shouldKeepAllPlaceholders() {
        // Given
        Map<String, Object> variables = Map.of();

        // When
        String processedBodyText = notificationTemplate.processTemplate(notificationTemplate.getBodyText(), variables);

        // Then
        assertThat(processedBodyText).isEqualTo("Hi {{userName}}, you've been invited to join the hive '{{hiveName}}'.");
    }

    @Test
    void processTemplate_withNullVariables_shouldKeepAllPlaceholders() {
        // When
        String processedBodyText = notificationTemplate.processTemplate(notificationTemplate.getBodyText(), null);

        // Then
        assertThat(processedBodyText).isEqualTo("Hi {{userName}}, you've been invited to join the hive '{{hiveName}}'.");
    }

    @Test
    void processTemplate_withNumberAndBooleanValues_shouldConvertToString() {
        // Given
        String template = "You have {{count}} notifications. Premium: {{isPremium}}";
        Map<String, Object> variables = Map.of(
            "count", 5,
            "isPremium", true
        );

        // When
        String processed = notificationTemplate.processTemplate(template, variables);

        // Then
        assertThat(processed).isEqualTo("You have 5 notifications. Premium: true");
    }

    @Test
    void processTemplate_withNestedBraces_shouldHandleCorrectly() {
        // Given
        String template = "{{outer}} and {{inner}} should work";
        Map<String, Object> variables = Map.of(
            "outer", "OUTER",
            "inner", "INNER"
        );

        // When
        String processed = notificationTemplate.processTemplate(template, variables);

        // Then
        assertThat(processed).isEqualTo("OUTER and INNER should work");
    }

    @Test
    void getTemplateKey_shouldReturnCorrectKey() {
        // When
        String key = notificationTemplate.getTemplateKey();

        // Then
        assertThat(key).isEqualTo("HIVE_INVITATION_en");
    }

    @Test
    void getTemplateKey_withDifferentTypeAndLanguage_shouldReturnCorrectKey() {
        // Given
        notificationTemplate.setNotificationType(NotificationType.TASK_ASSIGNED);
        notificationTemplate.setLanguage("fr");

        // When
        String key = notificationTemplate.getTemplateKey();

        // Then
        assertThat(key).isEqualTo("TASK_ASSIGNED_fr");
    }
}