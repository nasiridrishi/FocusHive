package com.focushive.notification.service;

import com.focushive.notification.entity.NotificationTemplate;
import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * TDD tests for NotificationTemplateService.
 * Tests template CRUD operations, caching, variable substitution, and validation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationTemplateService Tests")
class NotificationTemplateServiceTest {

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private RedisTemplate<String, NotificationTemplate> redisTemplate;

    @Mock
    private ValueOperations<String, NotificationTemplate> valueOperations;

    private NotificationTemplateService templateService;

    private static final String DEFAULT_LANGUAGE = "en";
    private static final String CACHE_KEY_PREFIX = "template:";

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        templateService = new NotificationTemplateService(templateRepository, redisTemplate);
    }

    @Test
    @DisplayName("Should create a new notification template")
    void shouldCreateNotificationTemplate() {
        // Given
        NotificationTemplate template = createSampleTemplate();
        when(templateRepository.save(any(NotificationTemplate.class))).thenReturn(template);

        // When
        NotificationTemplate result = templateService.createTemplate(template);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getNotificationType()).isEqualTo(NotificationType.WELCOME);
        assertThat(result.getLanguage()).isEqualTo(DEFAULT_LANGUAGE);
        verify(templateRepository).save(template);
        verify(valueOperations).set(eq(CACHE_KEY_PREFIX + template.getTemplateKey()), eq(template), any());
    }

    @Test
    @DisplayName("Should find template by notification type and language")
    void shouldFindTemplateByTypeAndLanguage() {
        // Given
        NotificationTemplate template = createSampleTemplate();
        String cacheKey = CACHE_KEY_PREFIX + template.getTemplateKey();
        
        when(valueOperations.get(cacheKey)).thenReturn(null);
        when(templateRepository.findByNotificationTypeAndLanguage(NotificationType.WELCOME, DEFAULT_LANGUAGE))
                .thenReturn(Optional.of(template));

        // When
        Optional<NotificationTemplate> result = templateService.findTemplate(NotificationType.WELCOME, DEFAULT_LANGUAGE);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getNotificationType()).isEqualTo(NotificationType.WELCOME);
        verify(valueOperations).get(cacheKey);
        verify(templateRepository).findByNotificationTypeAndLanguage(NotificationType.WELCOME, DEFAULT_LANGUAGE);
        verify(valueOperations).set(eq(cacheKey), eq(template), any());
    }

    @Test
    @DisplayName("Should return cached template when available")
    void shouldReturnCachedTemplate() {
        // Given
        NotificationTemplate template = createSampleTemplate();
        String cacheKey = CACHE_KEY_PREFIX + template.getTemplateKey();
        
        when(valueOperations.get(cacheKey)).thenReturn(template);

        // When
        Optional<NotificationTemplate> result = templateService.findTemplate(NotificationType.WELCOME, DEFAULT_LANGUAGE);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getNotificationType()).isEqualTo(NotificationType.WELCOME);
        verify(valueOperations).get(cacheKey);
        verify(templateRepository, never()).findByNotificationTypeAndLanguage(any(), any());
    }

    @Test
    @DisplayName("Should process template with variables")
    void shouldProcessTemplateWithVariables() {
        // Given
        NotificationTemplate template = createTemplateWithVariables();
        Map<String, Object> variables = Map.of(
                "userName", "John Doe",
                "taskTitle", "Complete Project",
                "dueDate", "2025-09-20"
        );

        when(valueOperations.get(anyString())).thenReturn(null);
        when(templateRepository.findByNotificationTypeAndLanguage(NotificationType.TASK_ASSIGNED, DEFAULT_LANGUAGE))
                .thenReturn(Optional.of(template));

        // When
        ProcessedTemplate result = templateService.processTemplate(NotificationType.TASK_ASSIGNED, DEFAULT_LANGUAGE, variables);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSubject()).isEqualTo("New Task: Complete Project");
        assertThat(result.getBodyText()).contains("Hello John Doe");
        assertThat(result.getBodyText()).contains("Complete Project");
        assertThat(result.getBodyText()).contains("2025-09-20");
        assertThat(result.getBodyHtml()).contains("<h1>Hello John Doe</h1>");
    }

    @Test
    @DisplayName("Should validate template variables")
    void shouldValidateTemplateVariables() {
        // Given
        NotificationTemplate template = createTemplateWithVariables();
        Map<String, Object> incompleteVariables = Map.of(
                "userName", "John Doe"
                // Missing taskTitle and dueDate
        );

        // When/Then
        assertThatThrownBy(() -> 
                templateService.validateTemplateVariables(template, incompleteVariables))
                .isInstanceOf(TemplateValidationException.class)
                .hasMessageContaining("Missing required variables: [dueDate, taskTitle]");
    }

    @Test
    @DisplayName("Should update existing template")
    void shouldUpdateExistingTemplate() {
        // Given
        String templateId = "template-1";
        NotificationTemplate existingTemplate = createSampleTemplate();
        existingTemplate.setId(templateId);
        
        NotificationTemplate updatedTemplate = createSampleTemplate();
        updatedTemplate.setId(templateId);
        updatedTemplate.setSubject("Updated Subject");

        when(templateRepository.findById(templateId)).thenReturn(Optional.of(existingTemplate));
        when(templateRepository.save(any(NotificationTemplate.class))).thenReturn(updatedTemplate);

        // When
        NotificationTemplate result = templateService.updateTemplate(templateId, updatedTemplate);

        // Then
        assertThat(result.getSubject()).isEqualTo("Updated Subject");
        verify(templateRepository).save(updatedTemplate);
        verify(redisTemplate).delete(CACHE_KEY_PREFIX + existingTemplate.getTemplateKey());
    }

    @Test
    @DisplayName("Should delete template and clear cache")
    void shouldDeleteTemplateAndClearCache() {
        // Given
        String templateId = "template-1";
        NotificationTemplate template = createSampleTemplate();
        template.setId(templateId);

        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));

        // When
        templateService.deleteTemplate(templateId);

        // Then
        verify(templateRepository).deleteById(templateId);
        verify(redisTemplate).delete(CACHE_KEY_PREFIX + template.getTemplateKey());
    }

    @Test
    @DisplayName("Should find template with fallback to default language")
    void shouldFindTemplateWithFallback() {
        // Given
        NotificationTemplate englishTemplate = createSampleTemplate();
        String requestedLanguage = "es"; // Spanish
        
        when(valueOperations.get(CACHE_KEY_PREFIX + "WELCOME_es")).thenReturn(null);
        when(templateRepository.findTemplateWithFallback(NotificationType.WELCOME, requestedLanguage))
                .thenReturn(List.of(englishTemplate));

        // When
        Optional<NotificationTemplate> result = templateService.findTemplateWithFallback(NotificationType.WELCOME, requestedLanguage);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getLanguage()).isEqualTo(DEFAULT_LANGUAGE);
        verify(templateRepository).findTemplateWithFallback(NotificationType.WELCOME, requestedLanguage);
    }

    @Test
    @DisplayName("Should get all available languages")
    void shouldGetAllAvailableLanguages() {
        // Given
        List<String> languages = Arrays.asList("en", "es", "fr", "de");
        when(templateRepository.findAvailableLanguages()).thenReturn(languages);

        // When
        List<String> result = templateService.getAvailableLanguages();

        // Then
        assertThat(result).hasSize(4);
        assertThat(result).containsExactly("en", "es", "fr", "de");
        verify(templateRepository).findAvailableLanguages();
    }

    @Test
    @DisplayName("Should get template statistics")
    void shouldGetTemplateStatistics() {
        // Given
        when(templateRepository.getTemplateStatistics()).thenReturn(Arrays.asList(
                new Object[]{NotificationType.WELCOME, 3L, 3L},
                new Object[]{NotificationType.TASK_ASSIGNED, 2L, 2L}
        ));

        // When
        Map<NotificationType, TemplateStatistics> result = templateService.getTemplateStatistics();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(NotificationType.WELCOME).getLanguageCount()).isEqualTo(3);
        assertThat(result.get(NotificationType.TASK_ASSIGNED).getLanguageCount()).isEqualTo(2);
        verify(templateRepository).getTemplateStatistics();
    }

    @Test
    @DisplayName("Should extract template variables from content")
    void shouldExtractTemplateVariables() {
        // Given
        NotificationTemplate template = createTemplateWithVariables();

        // When
        Set<String> variables = templateService.extractTemplateVariables(template);

        // Then
        assertThat(variables).hasSize(3);
        assertThat(variables).containsExactlyInAnyOrder("userName", "taskTitle", "dueDate");
    }

    @Test
    @DisplayName("Should bulk create templates")
    void shouldBulkCreateTemplates() {
        // Given
        List<NotificationTemplate> templates = Arrays.asList(
                createSampleTemplate(),
                createTemplateWithVariables()
        );

        when(templateRepository.saveAll(templates)).thenReturn(templates);

        // When
        List<NotificationTemplate> result = templateService.bulkCreateTemplates(templates);

        // Then
        assertThat(result).hasSize(2);
        verify(templateRepository).saveAll(templates);
        verify(valueOperations, times(2)).set(anyString(), any(NotificationTemplate.class), any());
    }

    @Test
    @DisplayName("Should throw exception when template not found")
    void shouldThrowExceptionWhenTemplateNotFound() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(null);
        when(templateRepository.findByNotificationTypeAndLanguage(NotificationType.WELCOME, DEFAULT_LANGUAGE))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> 
                templateService.processTemplate(NotificationType.WELCOME, DEFAULT_LANGUAGE, Map.of()))
                .isInstanceOf(TemplateNotFoundException.class)
                .hasMessageContaining("No template found for type WELCOME and language en");
    }

    // Helper methods
    private NotificationTemplate createSampleTemplate() {
        return NotificationTemplate.builder()
                .notificationType(NotificationType.WELCOME)
                .language(DEFAULT_LANGUAGE)
                .subject("Welcome to FocusHive!")
                .bodyText("Welcome to our platform. We're excited to have you!")
                .bodyHtml("<h1>Welcome!</h1><p>Welcome to our platform. We're excited to have you!</p>")
                .build();
    }

    private NotificationTemplate createTemplateWithVariables() {
        return NotificationTemplate.builder()
                .notificationType(NotificationType.TASK_ASSIGNED)
                .language(DEFAULT_LANGUAGE)
                .subject("New Task: {{taskTitle}}")
                .bodyText("Hello {{userName}}, you have a new task '{{taskTitle}}' due on {{dueDate}}.")
                .bodyHtml("<h1>Hello {{userName}}</h1><p>You have a new task '<strong>{{taskTitle}}</strong>' due on {{dueDate}}.</p>")
                .build();
    }
}