package com.focushive.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.notification.entity.NotificationTemplate;
import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TDD tests for NotificationTemplateController.
 * Tests REST endpoints for template management.
 */
@ExtendWith(MockitoExtension.class)
class NotificationTemplateControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationTemplateService templateService;

    private ObjectMapper objectMapper;

    private NotificationTemplate sampleTemplate;
    private ProcessedTemplate processedTemplate;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationTemplateController(templateService))
                .build();

        sampleTemplate = NotificationTemplate.builder()
                .notificationType(NotificationType.WELCOME)
                .language("en")
                .subject("Welcome to FocusHive!")
                .bodyText("Welcome to our platform. We're excited to have you!")
                .bodyHtml("<h1>Welcome!</h1><p>Welcome to our platform.</p>")
                .build();
        sampleTemplate.setId("template-1");

        processedTemplate = ProcessedTemplate.builder()
                .notificationType(NotificationType.WELCOME)
                .language("en")
                .templateId("template-1")
                .subject("Welcome to FocusHive!")
                .bodyText("Welcome to our platform. We're excited to have you!")
                .bodyHtml("<h1>Welcome!</h1><p>Welcome to our platform.</p>")
                .hasHtmlContent(true)
                .hasSubject(true)
                .variableCount(0)
                .build();
    }

    @Test
    @DisplayName("Should create a new template")
    void shouldCreateTemplate() throws Exception {
        // Given
        when(templateService.createTemplate(any(NotificationTemplate.class)))
                .thenReturn(sampleTemplate);

        // When & Then
        mockMvc.perform(post("/api/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleTemplate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("template-1"))
                .andExpect(jsonPath("$.notificationType").value("WELCOME"))
                .andExpect(jsonPath("$.language").value("en"))
                .andExpect(jsonPath("$.subject").value("Welcome to FocusHive!"));

        verify(templateService).createTemplate(any(NotificationTemplate.class));
    }

    @Test
    @DisplayName("Should get template by type and language")
    void shouldGetTemplateByTypeAndLanguage() throws Exception {
        // Given
        when(templateService.findTemplate(NotificationType.WELCOME, "en"))
                .thenReturn(Optional.of(sampleTemplate));

        // When & Then
        mockMvc.perform(get("/api/templates/{type}/{language}", "WELCOME", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("template-1"))
                .andExpect(jsonPath("$.notificationType").value("WELCOME"))
                .andExpect(jsonPath("$.language").value("en"));

        verify(templateService).findTemplate(NotificationType.WELCOME, "en");
    }

    @Test
    @DisplayName("Should return 404 when template not found")
    void shouldReturn404WhenTemplateNotFound() throws Exception {
        // Given
        when(templateService.findTemplate(NotificationType.WELCOME, "es"))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/templates/{type}/{language}", "WELCOME", "es"))
                .andExpect(status().isNotFound());

        verify(templateService).findTemplate(NotificationType.WELCOME, "es");
    }

    @Test
    @DisplayName("Should process template with variables")
    void shouldProcessTemplateWithVariables() throws Exception {
        // Given
        Map<String, Object> variables = Map.of(
                "userName", "John Doe",
                "companyName", "FocusHive"
        );

        when(templateService.processTemplate(NotificationType.WELCOME, "en", variables))
                .thenReturn(processedTemplate);

        // When & Then
        mockMvc.perform(post("/api/templates/{type}/{language}/process", "WELCOME", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(variables)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationType").value("WELCOME"))
                .andExpect(jsonPath("$.language").value("en"))
                .andExpect(jsonPath("$.subject").value("Welcome to FocusHive!"))
                .andExpect(jsonPath("$.hasHtmlContent").value(true));

        verify(templateService).processTemplate(NotificationType.WELCOME, "en", variables);
    }

    @Test
    @DisplayName("Should update existing template")
    void shouldUpdateTemplate() throws Exception {
        // Given
        NotificationTemplate updatedTemplate = NotificationTemplate.builder()
                .notificationType(NotificationType.WELCOME)
                .language("en")
                .subject("Updated Welcome!")
                .bodyText("Updated welcome message")
                .bodyHtml("<h1>Updated Welcome!</h1>")
                .build();
        updatedTemplate.setId("template-1");

        when(templateService.updateTemplate(eq("template-1"), any(NotificationTemplate.class)))
                .thenReturn(updatedTemplate);

        // When & Then
        mockMvc.perform(put("/api/templates/{id}", "template-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedTemplate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Updated Welcome!"));

        verify(templateService).updateTemplate(eq("template-1"), any(NotificationTemplate.class));
    }

    @Test
    @DisplayName("Should delete template")
    void shouldDeleteTemplate() throws Exception {
        // Given
        doNothing().when(templateService).deleteTemplate("template-1");

        // When & Then
        mockMvc.perform(delete("/api/templates/{id}", "template-1"))
                .andExpect(status().isNoContent());

        verify(templateService).deleteTemplate("template-1");
    }

    @Test
    @DisplayName("Should get available languages")
    void shouldGetAvailableLanguages() throws Exception {
        // Given
        List<String> languages = Arrays.asList("en", "es", "fr", "de");
        when(templateService.getAvailableLanguages()).thenReturn(languages);

        // When & Then
        mockMvc.perform(get("/api/templates/languages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0]").value("en"))
                .andExpect(jsonPath("$[1]").value("es"));

        verify(templateService).getAvailableLanguages();
    }

    @Test
    @DisplayName("Should get template statistics")
    void shouldGetTemplateStatistics() throws Exception {
        // Given
        Map<NotificationType, TemplateStatistics> stats = Map.of(
                NotificationType.WELCOME, TemplateStatistics.builder()
                        .languageCount(3L)
                        .templateCount(3L)
                        .build(),
                NotificationType.TASK_ASSIGNED, TemplateStatistics.builder()
                        .languageCount(2L)
                        .templateCount(2L)
                        .build()
        );

        when(templateService.getTemplateStatistics()).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/templates/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.WELCOME.languageCount").value(3))
                .andExpect(jsonPath("$.TASK_ASSIGNED.languageCount").value(2));

        verify(templateService).getTemplateStatistics();
    }

    @Test
    @DisplayName("Should extract template variables")
    void shouldExtractTemplateVariables() throws Exception {
        // Given
        Set<String> variables = Set.of("userName", "taskTitle", "dueDate");
        when(templateService.extractTemplateVariables(any(NotificationTemplate.class)))
                .thenReturn(variables);

        // When & Then
        mockMvc.perform(post("/api/templates/extract-variables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleTemplate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));

        verify(templateService).extractTemplateVariables(any(NotificationTemplate.class));
    }



    @Test
    @DisplayName("Should bulk create templates")
    void shouldBulkCreateTemplates() throws Exception {
        // Given
        List<NotificationTemplate> templates = Arrays.asList(sampleTemplate);
        when(templateService.bulkCreateTemplates(anyList())).thenReturn(templates);

        // When & Then
        mockMvc.perform(post("/api/templates/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(templates)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        verify(templateService).bulkCreateTemplates(anyList());
    }


}