package com.focushive.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import com.focushive.notification.config.ControllerTestConfiguration;
import com.focushive.notification.entity.NotificationFrequency;
import com.focushive.notification.entity.NotificationPreference;
import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.exception.ResourceNotFoundException;
import com.focushive.notification.service.NotificationPreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for NotificationPreferenceController following TDD approach.
 */
@WebMvcTest(controllers = NotificationPreferenceController.class)
@Import(ControllerTestConfiguration.class)
@DisplayName("NotificationPreferenceController Tests")
class NotificationPreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationPreferenceService preferenceService;

    @MockBean
    private com.focushive.notification.service.NotificationDigestService digestService;

    @Autowired
    private ObjectMapper objectMapper;

    private NotificationPreference testPreference;
    private static final String USER_ID = "user-123";
    private static final String PREF_ID = "pref-456";

    @BeforeEach
    void setUp() {
        testPreference = NotificationPreference.builder()
                .userId(USER_ID)
                .notificationType(NotificationType.WELCOME)
                .inAppEnabled(true)
                .emailEnabled(true)
                .pushEnabled(false)
                .frequency(NotificationFrequency.IMMEDIATE)
                .quietStartTime(LocalTime.of(22, 0))
                .quietEndTime(LocalTime.of(8, 0))
                .build();
        testPreference.setId(PREF_ID);
    }

    @Test
    @DisplayName("Should get all user preferences")
    void shouldGetAllUserPreferences() throws Exception {
        // Given
        List<NotificationPreference> preferences = Arrays.asList(testPreference);
        given(preferenceService.getUserPreferences(USER_ID)).willReturn(preferences);

        // When & Then
        mockMvc.perform(get("/api/preferences/user/{userId}", USER_ID)
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].userId").value(USER_ID))
                .andExpect(jsonPath("$[0].notificationType").value("WELCOME"))
                .andExpect(jsonPath("$[0].inAppEnabled").value(true))
                .andExpect(jsonPath("$[0].emailEnabled").value(true))
                .andExpect(jsonPath("$[0].pushEnabled").value(false));

        verify(preferenceService).getUserPreferences(USER_ID);
    }

    @Test
    @DisplayName("Should get specific user preference by type")
    void shouldGetSpecificUserPreferenceByType() throws Exception {
        // Given
        given(preferenceService.getUserPreference(USER_ID, NotificationType.WELCOME))
                .willReturn(Optional.of(testPreference));

        // When & Then
        mockMvc.perform(get("/api/preferences/user/{userId}/type/{notificationType}", 
                        USER_ID, "WELCOME")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.notificationType").value("WELCOME"));

        verify(preferenceService).getUserPreference(USER_ID, NotificationType.WELCOME);
    }

    @Test
    @DisplayName("Should return 404 when preference not found by type")
    void shouldReturn404WhenPreferenceNotFoundByType() throws Exception {
        // Given
        given(preferenceService.getUserPreference(USER_ID, NotificationType.WELCOME))
                .willReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/preferences/user/{userId}/type/{notificationType}", 
                        USER_ID, "WELCOME")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser"))))
                .andExpect(status().isNotFound());

        verify(preferenceService).getUserPreference(USER_ID, NotificationType.WELCOME);
    }

    @Test
    @DisplayName("Should create new preference")
    void shouldCreateNewPreference() throws Exception {
        // Given
        given(preferenceService.createPreference(eq(USER_ID), eq(NotificationType.WELCOME),
                eq(true), eq(true), eq(false), eq(NotificationFrequency.IMMEDIATE),
                any(LocalTime.class), any(LocalTime.class)))
                .willReturn(testPreference);

        String requestBody = """
                {
                    "notificationType": "WELCOME",
                    "inAppEnabled": true,
                    "emailEnabled": true,
                    "pushEnabled": false,
                    "frequency": "IMMEDIATE",
                    "quietStartTime": "22:00",
                    "quietEndTime": "08:00"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/preferences/user/{userId}", USER_ID)
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.notificationType").value("WELCOME"));

        verify(preferenceService).createPreference(eq(USER_ID), eq(NotificationType.WELCOME),
                eq(true), eq(true), eq(false), eq(NotificationFrequency.IMMEDIATE),
                any(LocalTime.class), any(LocalTime.class));
    }

    @Test
    @DisplayName("Should return 400 for invalid request")
    void shouldReturn400ForInvalidRequest() throws Exception {
        // Given - invalid request body (missing required fields)
        String requestBody = """
                {
                    "inAppEnabled": true
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/preferences/user/{userId}", USER_ID)
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(preferenceService, never()).createPreference(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should update existing preference")
    void shouldUpdateExistingPreference() throws Exception {
        // Given
        NotificationPreference updatedPreference = NotificationPreference.builder()
                .userId(USER_ID)
                .notificationType(NotificationType.WELCOME)
                .inAppEnabled(false)
                .emailEnabled(true)
                .pushEnabled(true)
                .frequency(NotificationFrequency.DAILY)
                .build();
        updatedPreference.setId(PREF_ID);

        given(preferenceService.updatePreference(eq(PREF_ID), eq(false), eq(true), eq(true),
                eq(NotificationFrequency.DAILY), isNull(), isNull()))
                .willReturn(updatedPreference);

        String requestBody = """
                {
                    "inAppEnabled": false,
                    "emailEnabled": true,
                    "pushEnabled": true,
                    "frequency": "DAILY"
                }
                """;

        // When & Then
        mockMvc.perform(put("/api/preferences/{preferenceId}", PREF_ID)
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inAppEnabled").value(false))
                .andExpect(jsonPath("$.emailEnabled").value(true))
                .andExpect(jsonPath("$.pushEnabled").value(true))
                .andExpect(jsonPath("$.frequency").value("DAILY"));

        verify(preferenceService).updatePreference(eq(PREF_ID), eq(false), eq(true), eq(true),
                eq(NotificationFrequency.DAILY), isNull(), isNull());
    }

    @Test
    @DisplayName("Should create or update preference")
    void shouldCreateOrUpdatePreference() throws Exception {
        // Given
        given(preferenceService.createOrUpdatePreference(eq(USER_ID), eq(NotificationType.WELCOME),
                eq(true), eq(false), eq(true), eq(NotificationFrequency.HOURLY),
                any(LocalTime.class), any(LocalTime.class)))
                .willReturn(testPreference);

        String requestBody = """
                {
                    "notificationType": "WELCOME",
                    "inAppEnabled": true,
                    "emailEnabled": false,
                    "pushEnabled": true,
                    "frequency": "HOURLY",
                    "quietStartTime": "23:00",
                    "quietEndTime": "07:00"
                }
                """;

        // When & Then
        mockMvc.perform(put("/api/preferences/user/{userId}", USER_ID)
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID));

        verify(preferenceService).createOrUpdatePreference(eq(USER_ID), eq(NotificationType.WELCOME),
                eq(true), eq(false), eq(true), eq(NotificationFrequency.HOURLY),
                any(LocalTime.class), any(LocalTime.class));
    }

    @Test
    @DisplayName("Should delete preference")
    void shouldDeletePreference() throws Exception {
        // Given
        willDoNothing().given(preferenceService).deletePreference(PREF_ID);

        // When & Then
        mockMvc.perform(delete("/api/preferences/{preferenceId}", PREF_ID)
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser")))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(preferenceService).deletePreference(PREF_ID);
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent preference")
    void shouldReturn404WhenDeletingNonExistentPreference() throws Exception {
        // Given
        willThrow(new ResourceNotFoundException("Preference not found")).given(preferenceService)
                .deletePreference(PREF_ID);

        // When & Then
        mockMvc.perform(delete("/api/preferences/{preferenceId}", PREF_ID)
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser")))
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(preferenceService).deletePreference(PREF_ID);
    }

    @Test
    @DisplayName("Should get enabled preferences for user")
    void shouldGetEnabledPreferencesForUser() throws Exception {
        // Given
        List<NotificationPreference> enabledPrefs = Arrays.asList(testPreference);
        given(preferenceService.getEnabledPreferencesForUser(USER_ID)).willReturn(enabledPrefs);

        // When & Then
        mockMvc.perform(get("/api/preferences/user/{userId}/enabled", USER_ID)
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].userId").value(USER_ID));

        verify(preferenceService).getEnabledPreferencesForUser(USER_ID);
    }

    @Test
    @DisplayName("Should create default preferences for user")
    void shouldCreateDefaultPreferencesForUser() throws Exception {
        // Given
        List<NotificationPreference> defaultPrefs = Arrays.asList(testPreference);
        given(preferenceService.createDefaultPreferencesForUser(USER_ID)).willReturn(defaultPrefs);

        // When & Then
        mockMvc.perform(post("/api/preferences/user/{userId}/defaults", USER_ID)
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser")))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].userId").value(USER_ID));

        verify(preferenceService).createDefaultPreferencesForUser(USER_ID);
    }

    @Test
    @DisplayName("Should check if notification is enabled")
    void shouldCheckIfNotificationIsEnabled() throws Exception {
        // Given
        given(preferenceService.isNotificationEnabled(USER_ID, NotificationType.WELCOME))
                .willReturn(true);

        // When & Then
        mockMvc.perform(get("/api/preferences/user/{userId}/type/{notificationType}/enabled", 
                        USER_ID, "WELCOME")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        verify(preferenceService).isNotificationEnabled(USER_ID, NotificationType.WELCOME);
    }

    @Test
    @DisplayName("Should check quiet hours")
    void shouldCheckQuietHours() throws Exception {
        // Given
        given(preferenceService.isInQuietHours(eq(USER_ID), eq(NotificationType.WELCOME), any(LocalTime.class)))
                .willReturn(true);

        // When & Then
        mockMvc.perform(get("/api/preferences/user/{userId}/type/{notificationType}/quiet-hours", 
                        USER_ID, "WELCOME")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser")))
                        .param("time", "23:30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inQuietHours").value(true));

        verify(preferenceService).isInQuietHours(eq(USER_ID), eq(NotificationType.WELCOME), any(LocalTime.class));
    }

    @Test
    @DisplayName("Should return 400 for invalid time format")
    void shouldReturn400ForInvalidTimeFormat() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/preferences/user/{userId}/type/{notificationType}/quiet-hours", 
                        USER_ID, "WELCOME")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser")))
                        .param("time", "invalid-time"))
                .andExpect(status().isBadRequest());

        verify(preferenceService, never()).isInQuietHours(any(), any(), any());
    }

    @Test
    @DisplayName("Should return 400 for invalid notification type")
    void shouldReturn400ForInvalidNotificationType() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/preferences/user/{userId}/type/{notificationType}", 
                        USER_ID, "INVALID_TYPE")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "testuser"))))
                .andExpect(status().isBadRequest());

        verify(preferenceService, never()).getUserPreference(any(), any());
    }
}