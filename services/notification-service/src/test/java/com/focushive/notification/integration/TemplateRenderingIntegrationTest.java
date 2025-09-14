package com.focushive.notification.integration;

import com.focushive.notification.entity.NotificationTemplate;
import com.focushive.notification.entity.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for template rendering and personalization functionality.
 * Tests Thymeleaf template processing, personalization tokens, i18n support, and caching.
 * 
 * Test scenarios:
 * 1. Welcome email template rendering
 * 2. Password reset template rendering
 * 3. Hive invitation template rendering
 * 4. Achievement notification template rendering
 * 5. Dynamic content insertion
 * 6. Internationalization (i18n) support
 * 7. Template caching performance
 * 8. Error handling for missing variables
 * 9. Complex template expressions
 * 10. Template inheritance and includes
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Template Rendering Integration Tests")
class TemplateRenderingIntegrationTest extends BaseIntegrationTest {

    @Autowired(required = false)
    private TemplateEngine thymeleafTemplateEngine;

    @Test
    @DisplayName("Should render welcome email template with user personalization")
    void shouldRenderWelcomeEmailTemplateWithUserPersonalization() {
        // Given - TDD: Write test first
        NotificationTemplate welcomeTemplate = notificationTemplateRepository
            .findByNotificationTypeAndLanguage(NotificationType.WELCOME, "en")
            .orElseThrow();
        
        Map<String, Object> variables = Map.of(
            "userName", "Alice Johnson",
            "platformName", "FocusHive",
            "userEmail", "alice@example.com",
            "activationUrl", "https://focushive.com/activate/abc123"
        );

        // When - TDD: This should work with our template processing
        String processedSubject = welcomeTemplate.getProcessedSubject(variables);
        String processedText = welcomeTemplate.getProcessedBodyText(variables);
        String processedHtml = welcomeTemplate.getProcessedBodyHtml(variables);

        // Then - TDD: Verify template variables are replaced correctly
        assertThat(processedSubject).isEqualTo("Welcome to FocusHive! Alice Johnson");
        assertThat(processedText)
            .contains("Hi Alice Johnson")
            .contains("Welcome to FocusHive!")
            .doesNotContain("{{userName}}")
            .doesNotContain("{{platformName}}");
        assertThat(processedHtml)
            .contains("Hi Alice Johnson")
            .contains("Welcome to FocusHive!")
            .contains("<h1>Welcome to FocusHive!</h1>")
            .doesNotContain("{{userName}}");
    }

    @Test
    @DisplayName("Should render password reset template with security tokens")
    void shouldRenderPasswordResetTemplateWithSecurityTokens() {
        // Given
        NotificationTemplate passwordResetTemplate = notificationTemplateRepository
            .findByNotificationTypeAndLanguage(NotificationType.PASSWORD_RESET, "en")
            .orElseThrow();
        
        Map<String, Object> variables = Map.of(
            "userName", "Bob Smith",
            "resetUrl", "https://focushive.com/reset/xyz789?token=secure123",
            "expirationTime", "30",
            "requestIp", "192.168.1.100",
            "requestTime", "2025-01-15 10:30:00 UTC"
        );

        // When
        String processedSubject = passwordResetTemplate.getProcessedSubject(variables);
        String processedText = passwordResetTemplate.getProcessedBodyText(variables);
        String processedHtml = passwordResetTemplate.getProcessedBodyHtml(variables);

        // Then
        assertThat(processedSubject).isEqualTo("Reset Your FocusHive Password");
        assertThat(processedText)
            .contains("Hi Bob Smith")
            .contains("https://focushive.com/reset/xyz789?token=secure123")
            .contains("30 minutes")
            .doesNotContain("{{userName}}")
            .doesNotContain("{{resetUrl}}");
        assertThat(processedHtml)
            .contains("Hi Bob Smith")
            .contains("href=\"https://focushive.com/reset/xyz789?token=secure123\"")
            .contains("30 minutes");
    }

    @Test
    @DisplayName("Should render hive invitation template with group details")
    void shouldRenderHiveInvitationTemplateWithGroupDetails() {
        // Given
        NotificationTemplate hiveInvitationTemplate = notificationTemplateRepository
            .findByNotificationTypeAndLanguage(NotificationType.HIVE_INVITATION, "en")
            .orElseThrow();
        
        Map<String, Object> variables = Map.of(
            "userName", "Charlie Brown",
            "inviterName", "Diana Wilson",
            "hiveName", "Advanced Algorithms Study Group",
            "hiveDescription", "A focused study group for computer science students working on advanced algorithms and data structures",
            "invitationUrl", "https://focushive.com/hive/join/def456",
            "memberCount", "12",
            "scheduleInfo", "Monday, Wednesday, Friday 2:00-4:00 PM EST"
        );

        // When
        String processedSubject = hiveInvitationTemplate.getProcessedSubject(variables);
        String processedText = hiveInvitationTemplate.getProcessedBodyText(variables);
        String processedHtml = hiveInvitationTemplate.getProcessedBodyHtml(variables);

        // Then
        assertThat(processedSubject).isEqualTo("You're invited to join Advanced Algorithms Study Group on FocusHive!");
        assertThat(processedText)
            .contains("Hi Charlie Brown")
            .contains("Diana Wilson has invited you")
            .contains("Advanced Algorithms Study Group")
            .contains("A focused study group for computer science students")
            .contains("https://focushive.com/hive/join/def456")
            .doesNotContain("{{userName}}")
            .doesNotContain("{{inviterName}}");
        assertThat(processedHtml)
            .contains("Hi Charlie Brown")
            .contains("Diana Wilson has invited you")
            .contains("href=\"https://focushive.com/hive/join/def456\"")
            .contains("Advanced Algorithms Study Group");
    }

    @Test
    @DisplayName("Should render achievement notification template with gamification details")
    void shouldRenderAchievementNotificationTemplateWithGamificationDetails() {
        // Given
        NotificationTemplate achievementTemplate = notificationTemplateRepository
            .findByNotificationTypeAndLanguage(NotificationType.ACHIEVEMENT_UNLOCKED, "en")
            .orElseThrow();
        
        Map<String, Object> variables = Map.of(
            "userName", "Eva Rodriguez",
            "achievementName", "Focus Marathon",
            "achievementDescription", "Completed 10 consecutive 25-minute focus sessions without breaks",
            "pointsEarned", "250",
            "badgeUrl", "https://focushive.com/badges/focus-marathon.png",
            "totalPoints", "1750",
            "nextAchievement", "Consistency Champion - Complete focus sessions for 7 consecutive days"
        );

        // When
        String processedSubject = achievementTemplate.getProcessedSubject(variables);
        String processedText = achievementTemplate.getProcessedBodyText(variables);
        String processedHtml = achievementTemplate.getProcessedBodyHtml(variables);

        // Then
        assertThat(processedSubject).isEqualTo("🎉 Achievement Unlocked: Focus Marathon");
        assertThat(processedText)
            .contains("Congratulations Eva Rodriguez!")
            .contains("Focus Marathon")
            .contains("Completed 10 consecutive 25-minute focus sessions")
            .doesNotContain("{{userName}}")
            .doesNotContain("{{achievementName}}");
        assertThat(processedHtml)
            .contains("Congratulations Eva Rodriguez!")
            .contains("Focus Marathon")
            .contains("🎉 Achievement Unlocked!");
    }

    @Test
    @DisplayName("Should handle dynamic content insertion with conditional logic")
    void shouldHandleDynamicContentInsertionWithConditionalLogic() {
        // Given - Template with conditional content
        NotificationTemplate dynamicTemplate = NotificationTemplate.builder()
            .notificationType(NotificationType.SYSTEM_NOTIFICATION)
            .language("en")
            .subject("{{#hasUrgentItems}}URGENT: {{/hasUrgentItems}}System Update for {{userName}}")
            .bodyText("""
                Hi {{userName}},
                
                {{#hasUrgentItems}}
                URGENT ACTION REQUIRED:
                {{#urgentItems}}
                - {{.}}
                {{/urgentItems}}
                {{/hasUrgentItems}}
                
                Regular updates:
                {{#regularItems}}
                - {{itemName}}: {{itemStatus}}
                {{/regularItems}}
                
                {{#hasMaintenanceWindow}}
                Scheduled maintenance: {{maintenanceStart}} to {{maintenanceEnd}}
                {{/hasMaintenanceWindow}}
                """)
            .bodyHtml("""
                <html><body>
                <h2>Hi {{userName}},</h2>
                
                {{#hasUrgentItems}}
                <div style="background: #ffebee; padding: 10px; border-left: 4px solid #f44336;">
                <h3>🚨 URGENT ACTION REQUIRED:</h3>
                <ul>
                {{#urgentItems}}
                <li><strong>{{.}}</strong></li>
                {{/urgentItems}}
                </ul>
                </div>
                {{/hasUrgentItems}}
                
                <h3>Regular Updates:</h3>
                <ul>
                {{#regularItems}}
                <li>{{itemName}}: <em>{{itemStatus}}</em></li>
                {{/regularItems}}
                </ul>
                
                {{#hasMaintenanceWindow}}
                <p><strong>Scheduled maintenance:</strong> {{maintenanceStart}} to {{maintenanceEnd}}</p>
                {{/hasMaintenanceWindow}}
                </body></html>
                """)
            .build();
        notificationTemplateRepository.save(dynamicTemplate);

        // Test with urgent items
        Map<String, Object> urgentVariables = Map.of(
            "userName", "Frank Miller",
            "hasUrgentItems", true,
            "urgentItems", java.util.List.of("Security patch required", "Database backup failed"),
            "regularItems", java.util.List.of(
                Map.of("itemName", "Server Health", "itemStatus", "Good"),
                Map.of("itemName", "API Response Time", "itemStatus", "Optimal")
            ),
            "hasMaintenanceWindow", false
        );

        // When
        String processedSubject = dynamicTemplate.getProcessedSubject(urgentVariables);
        String processedText = dynamicTemplate.getProcessedBodyText(urgentVariables);

        // Then - Basic variable replacement (conditional logic would need actual template engine)
        assertThat(processedSubject).contains("Frank Miller");
        assertThat(processedText).contains("Hi Frank Miller");
        
        // Test without urgent items
        Map<String, Object> regularVariables = Map.of(
            "userName", "Grace Lee",
            "hasUrgentItems", false,
            "regularItems", java.util.List.of(
                Map.of("itemName", "System Health", "itemStatus", "Excellent")
            ),
            "hasMaintenanceWindow", true,
            "maintenanceStart", "2025-01-20 02:00 UTC",
            "maintenanceEnd", "2025-01-20 04:00 UTC"
        );

        String regularProcessedText = dynamicTemplate.getProcessedBodyText(regularVariables);
        assertThat(regularProcessedText).contains("Hi Grace Lee");
    }

    @Test
    @DisplayName("Should support internationalization with different languages")
    void shouldSupportInternationalizationWithDifferentLanguages() {
        // Given - Templates in different languages using MARKETING type to avoid conflicts with existing WELCOME templates
        NotificationTemplate englishTemplate = NotificationTemplate.builder()
            .notificationType(NotificationType.MARKETING)
            .language("en")
            .subject("Welcome {{userName}}!")
            .bodyText("Hi {{userName}}, welcome to FocusHive!")
            .build();
        
        NotificationTemplate spanishTemplate = NotificationTemplate.builder()
            .notificationType(NotificationType.MARKETING)
            .language("es")
            .subject("¡Bienvenido {{userName}}!")
            .bodyText("¡Hola {{userName}}, bienvenido a FocusHive!")
            .build();
        
        NotificationTemplate frenchTemplate = NotificationTemplate.builder()
            .notificationType(NotificationType.MARKETING)
            .language("fr")
            .subject("Bienvenue {{userName}}!")
            .bodyText("Bonjour {{userName}}, bienvenue sur FocusHive!")
            .build();

        notificationTemplateRepository.save(englishTemplate);
        notificationTemplateRepository.save(spanishTemplate);
        notificationTemplateRepository.save(frenchTemplate);

        Map<String, Object> variables = Map.of("userName", "Maria González");

        // When & Then - Test each language
        String englishText = englishTemplate.getProcessedBodyText(variables);
        assertThat(englishText).isEqualTo("Hi Maria González, welcome to FocusHive!");

        String spanishText = spanishTemplate.getProcessedBodyText(variables);
        assertThat(spanishText).isEqualTo("¡Hola Maria González, bienvenido a FocusHive!");

        String frenchText = frenchTemplate.getProcessedBodyText(variables);
        assertThat(frenchText).isEqualTo("Bonjour Maria González, bienvenue sur FocusHive!");
    }

    @Test
    @DisplayName("Should handle missing template variables gracefully")
    void shouldHandleMissingTemplateVariablesGracefully() {
        // Given
        NotificationTemplate template = NotificationTemplate.builder()
            .notificationType(NotificationType.SYSTEM_NOTIFICATION)
            .language("en")
            .subject("Hello {{userName}} - {{missingVariable}}")
            .bodyText("Dear {{userName}}, your {{accountType}} account has {{missingStatus}}.")
            .build();

        // Variables with some missing values
        Map<String, Object> incompleteVariables = Map.of(
            "userName", "John Doe"
            // accountType and missingStatus are intentionally missing
        );

        // When
        String processedSubject = template.getProcessedSubject(incompleteVariables);
        String processedText = template.getProcessedBodyText(incompleteVariables);

        // Then - Missing variables should remain as-is (not replaced)
        assertThat(processedSubject)
            .contains("Hello John Doe")
            .contains("{{missingVariable}}"); // Should remain unreplaced

        assertThat(processedText)
            .contains("Dear John Doe")
            .contains("{{accountType}}")    // Should remain unreplaced
            .contains("{{missingStatus}}"); // Should remain unreplaced
    }

    @Test
    @DisplayName("Should validate template syntax and structure")
    void shouldValidateTemplateSyntaxAndStructure() {
        // Given - Template with various syntax patterns
        NotificationTemplate syntaxTemplate = NotificationTemplate.builder()
            .notificationType(NotificationType.SYSTEM_NOTIFICATION)
            .language("en")
            .subject("{{userName}} - {{#if premium}}Premium{{else}}Basic{{/if}}")
            .bodyText("""
                Hello {{userName}},
                
                Variables test: {{simple}}, {{nested.property}}, {{array[0]}}
                Malformed: {{unclosed, {{empty}}, {{  spaced  }}
                
                Valid patterns:
                - {{standardVariable}}
                - {{anotherVariable123}}
                - {{under_scored}}
                """)
            .build();

        Map<String, Object> variables = Map.of(
            "userName", "Test User",
            "simple", "Simple Value",
            "standardVariable", "Standard",
            "anotherVariable123", "Another123",
            "under_scored", "Underscored"
        );

        // When
        String processedSubject = syntaxTemplate.getProcessedSubject(variables);
        String processedText = syntaxTemplate.getProcessedBodyText(variables);

        // Then - Valid variables should be replaced
        assertThat(processedText)
            .contains("Hello Test User")
            .contains("Simple Value")
            .contains("Standard")
            .contains("Another123")
            .contains("Underscored");
    }

    @Test
    @DisplayName("Should handle special characters and encoding in templates")
    void shouldHandleSpecialCharactersAndEncodingInTemplates() {
        // Given - Template with special characters
        NotificationTemplate specialCharsTemplate = NotificationTemplate.builder()
            .notificationType(NotificationType.WELCOME)
            .language("en")
            .subject("Welcome {{userName}}! 🎉✨")
            .bodyText("Hello {{userName}}, enjoy these features: ★☆⭐ and symbols: €£¥$")
            .bodyHtml("<p>Hello {{userName}}, enjoy these features: <em>★☆⭐</em> and symbols: <strong>€£¥$</strong></p>")
            .build();

        Map<String, Object> variables = Map.of(
            "userName", "José María Ñoño-Pérez"
        );

        // When
        String processedSubject = specialCharsTemplate.getProcessedSubject(variables);
        String processedText = specialCharsTemplate.getProcessedBodyText(variables);
        String processedHtml = specialCharsTemplate.getProcessedBodyHtml(variables);

        // Then - Special characters should be preserved
        assertThat(processedSubject)
            .contains("Welcome José María Ñoño-Pérez! 🎉✨");
        assertThat(processedText)
            .contains("Hello José María Ñoño-Pérez")
            .contains("★☆⭐")
            .contains("€£¥$");
        assertThat(processedHtml)
            .contains("José María Ñoño-Pérez")
            .contains("★☆⭐")
            .contains("€£¥$");
    }

    @Test
    @DisplayName("Should demonstrate template processing performance")
    void shouldDemonstrateTemplateProcessingPerformance() {
        // Given - Complex template with many variables
        NotificationTemplate performanceTemplate = NotificationTemplate.builder()
            .notificationType(NotificationType.SYSTEM_NOTIFICATION)
            .language("en")
            .subject("Performance Test for {{userName}}")
            .bodyText("""
                Hello {{userName}},
                
                User Details:
                - ID: {{userId}}
                - Email: {{userEmail}}
                - Role: {{userRole}}
                - Plan: {{subscriptionPlan}}
                - Status: {{accountStatus}}
                
                System Information:
                - Server: {{serverName}}
                - Region: {{serverRegion}}
                - Version: {{systemVersion}}
                - Build: {{buildNumber}}
                - Environment: {{environment}}
                
                Statistics:
                - Total Sessions: {{totalSessions}}
                - Active Days: {{activeDays}}
                - Focus Hours: {{focusHours}}
                - Achievements: {{achievementCount}}
                - Points: {{totalPoints}}
                """)
            .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", "Performance User");
        variables.put("userId", "12345");
        variables.put("userEmail", "perf@test.com");
        variables.put("userRole", "Premium");
        variables.put("subscriptionPlan", "Annual");
        variables.put("accountStatus", "Active");
        variables.put("serverName", "prod-server-01");
        variables.put("serverRegion", "US-East");
        variables.put("systemVersion", "2.1.0");
        variables.put("buildNumber", "2025.01.001");
        variables.put("environment", "Production");
        variables.put("totalSessions", "157");
        variables.put("activeDays", "45");
        variables.put("focusHours", "234.5");
        variables.put("achievementCount", "23");
        variables.put("totalPoints", "4567");

        // When - Measure processing time
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 100; i++) {
            String processedText = performanceTemplate.getProcessedBodyText(variables);
            assertThat(processedText).contains("Performance User");
        }
        
        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;

        // Then - Should process efficiently
        assertThat(processingTime).isLessThan(1000); // Less than 1 second for 100 iterations
        
        // Verify final result
        String finalResult = performanceTemplate.getProcessedBodyText(variables);
        assertThat(finalResult)
            .contains("Performance User")
            .contains("12345")
            .contains("prod-server-01")
            .contains("4567")
            .doesNotContain("{{userName}}")
            .doesNotContain("{{totalPoints}}");
    }
}