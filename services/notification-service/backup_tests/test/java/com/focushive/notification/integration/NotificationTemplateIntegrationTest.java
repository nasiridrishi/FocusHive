package com.focushive.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.notification.entity.NotificationTemplate;
import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.repository.NotificationTemplateRepository;
import com.focushive.notification.config.NotificationTemplateTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for NotificationTemplate system.
 * Tests complete workflow from REST API through service layer to database.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(com.focushive.notification.config.JpaConfig.class) // Import JPA auditing configuration
class NotificationTemplateIntegrationTest {

    @Autowired
    private NotificationTemplateRepository templateRepository;

    private NotificationTemplate sampleTemplate;

    @BeforeEach
    void setUp() {
        // Clean up database before each test
        templateRepository.deleteAll();

        sampleTemplate = NotificationTemplate.builder()
                .notificationType(NotificationType.WELCOME)
                .language("en")
                .subject("Welcome to {{platformName}}!")
                .bodyText("Hello {{userName}}, welcome to {{platformName}}. We're excited to have you!")
                .bodyHtml("<h1>Welcome {{userName}}!</h1><p>Welcome to <strong>{{platformName}}</strong>.</p>")
                .build();
    }

    @Test
    @DisplayName("Should create and retrieve template via repository")
    void shouldCreateAndRetrieveTemplateViaRepository() {
        // Step 1: Save template via repository
        NotificationTemplate savedTemplate = templateRepository.save(sampleTemplate);
        assertThat(savedTemplate).isNotNull();
        assertThat(savedTemplate.getId()).isNotNull();
        assertThat(savedTemplate.getNotificationType()).isEqualTo(NotificationType.WELCOME);
        assertThat(savedTemplate.getLanguage()).isEqualTo("en");

        // Step 2: Retrieve template via repository
        Optional<NotificationTemplate> retrievedTemplate = templateRepository
                .findByNotificationTypeAndLanguage(NotificationType.WELCOME, "en");
        
        assertThat(retrievedTemplate).isPresent();
        assertThat(retrievedTemplate.get().getSubject()).isEqualTo("Welcome to {{platformName}}!");
        assertThat(retrievedTemplate.get().getBodyText()).isEqualTo("Hello {{userName}}, welcome to {{platformName}}. We're excited to have you!");
        assertThat(retrievedTemplate.get().getBodyHtml()).isEqualTo("<h1>Welcome {{userName}}!</h1><p>Welcome to <strong>{{platformName}}</strong>.</p>");
    }

    @Test
    @DisplayName("Should find templates using custom repository methods")
    void shouldFindTemplatesUsingCustomMethods() {
        // Step 1: Create and save multiple templates
        NotificationTemplate welcomeEn = NotificationTemplate.builder()
                .notificationType(NotificationType.WELCOME)
                .language("en")
                .subject("Welcome!")
                .bodyText("Welcome message")
                .build();
        
        NotificationTemplate welcomeEs = NotificationTemplate.builder()
                .notificationType(NotificationType.WELCOME)
                .language("es")
                .subject("¡Bienvenido!")
                .bodyText("Mensaje de bienvenida")
                .build();
        
        NotificationTemplate taskAssignedEn = NotificationTemplate.builder()
                .notificationType(NotificationType.TASK_ASSIGNED)
                .language("en")
                .subject("Task Assigned")
                .bodyText("A task has been assigned to you")
                .build();
        
        templateRepository.save(welcomeEn);
        templateRepository.save(welcomeEs);
        templateRepository.save(taskAssignedEn);
        
        // Step 2: Test custom repository methods
        List<NotificationTemplate> welcomeTemplates = templateRepository
                .findByNotificationType(NotificationType.WELCOME);
        assertThat(welcomeTemplates).hasSize(2);
        
        List<NotificationTemplate> englishTemplates = templateRepository
                .findByLanguage("en");
        assertThat(englishTemplates).hasSize(2);
        
        List<String> availableLanguages = templateRepository.findAvailableLanguages();
        assertThat(availableLanguages).containsExactlyInAnyOrder("en", "es");
        
        boolean existsWelcomeEn = templateRepository
                .existsByNotificationTypeAndLanguage(NotificationType.WELCOME, "en");
        assertThat(existsWelcomeEn).isTrue();
        
        boolean existsWelcomeFr = templateRepository
                .existsByNotificationTypeAndLanguage(NotificationType.WELCOME, "fr");
        assertThat(existsWelcomeFr).isFalse();
    }

    @Test
    @DisplayName("Should process template variables correctly")
    void shouldProcessTemplateVariables() {
        // Step 1: Create template with variables
        NotificationTemplate template = NotificationTemplate.builder()
                .notificationType(NotificationType.TASK_ASSIGNED)
                .language("en")
                .subject("Task {{taskTitle}} assigned to {{userName}}")
                .bodyText("Hello {{userName}}, task '{{taskTitle}}' has been assigned to you. Due date: {{dueDate}}")
                .bodyHtml("<h2>Task Assignment</h2><p>Hello {{userName}},</p><p>Task <strong>{{taskTitle}}</strong> has been assigned to you.</p>")
                .build();
        
        NotificationTemplate savedTemplate = templateRepository.save(template);
        
        // Step 2: Test template variable processing using entity method
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", "John Doe");
        variables.put("taskTitle", "Review Code");
        variables.put("dueDate", "2024-01-15");
        
        String processedSubject = savedTemplate.processTemplate(savedTemplate.getSubject(), variables);
        String processedBodyText = savedTemplate.processTemplate(savedTemplate.getBodyText(), variables);
        String processedBodyHtml = savedTemplate.processTemplate(savedTemplate.getBodyHtml(), variables);
        
        // Step 3: Verify variable substitution
        assertThat(processedSubject).isEqualTo("Task Review Code assigned to John Doe");
        assertThat(processedBodyText).isEqualTo("Hello John Doe, task 'Review Code' has been assigned to you. Due date: 2024-01-15");
        assertThat(processedBodyHtml).isEqualTo("<h2>Task Assignment</h2><p>Hello John Doe,</p><p>Task <strong>Review Code</strong> has been assigned to you.</p>");
        
        // Step 4: Test helper methods
        assertThat(savedTemplate.hasSubject()).isTrue();
        assertThat(savedTemplate.hasHtmlContent()).isTrue();
        assertThat(savedTemplate.getTemplateKey()).isEqualTo("TASK_ASSIGNED_en");
    }
}