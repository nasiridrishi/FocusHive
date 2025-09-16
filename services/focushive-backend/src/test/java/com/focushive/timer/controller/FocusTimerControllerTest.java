package com.focushive.timer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.timer.dto.*;
import com.focushive.timer.entity.FocusSession;
import com.focushive.timer.service.FocusTimerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for FocusTimerController REST endpoints.
 * Tests timer management, templates, and statistics endpoints.
 * THIS WILL FAIL initially as FocusTimerController doesn't exist yet.
 */
@WebMvcTest(FocusTimerController.class)
class FocusTimerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FocusTimerService focusTimerService;

    private String userId;
    private String hiveId;
    private String sessionId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        hiveId = UUID.randomUUID().toString();
        sessionId = UUID.randomUUID().toString();
    }

    @Test
    @WithMockUser
    void shouldStartFocusSession() throws Exception {
        // Given
        StartTimerRequest request = StartTimerRequest.builder()
            .userId(userId)
            .hiveId(hiveId)
            .durationMinutes(25)
            .sessionType(FocusSession.SessionType.FOCUS)
            .title("Deep Work")
            .build();

        FocusSessionResponse response = FocusSessionResponse.builder()
            .id(sessionId)
            .userId(userId)
            .hiveId(hiveId)
            .status(FocusSession.SessionStatus.ACTIVE)
            .durationMinutes(25)
            .title("Deep Work")
            .startedAt(LocalDateTime.now())
            .build();

        when(focusTimerService.startSession(any(StartTimerRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/timer/start")
                .with(csrf())
                .header("User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(sessionId))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.durationMinutes").value(25))
            .andExpect(jsonPath("$.title").value("Deep Work"));

        verify(focusTimerService).startSession(any(StartTimerRequest.class));
    }

    @Test
    @WithMockUser
    void shouldPauseFocusSession() throws Exception {
        // Given
        FocusSessionResponse response = FocusSessionResponse.builder()
            .id(sessionId)
            .userId(userId)
            .status(FocusSession.SessionStatus.PAUSED)
            .pausedAt(LocalDateTime.now())
            .build();

        when(focusTimerService.pauseSession(sessionId, userId)).thenReturn(response);

        // When & Then
        mockMvc.perform(put("/api/timer/{sessionId}/pause", sessionId)
                .with(csrf())
                .header("User-ID", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(sessionId))
            .andExpect(jsonPath("$.status").value("PAUSED"))
            .andExpect(jsonPath("$.pausedAt").exists());

        verify(focusTimerService).pauseSession(sessionId, userId);
    }

    @Test
    @WithMockUser
    void shouldResumeFocusSession() throws Exception {
        // Given
        FocusSessionResponse response = FocusSessionResponse.builder()
            .id(sessionId)
            .userId(userId)
            .status(FocusSession.SessionStatus.ACTIVE)
            .resumedAt(LocalDateTime.now())
            .totalPausedMinutes(5)
            .build();

        when(focusTimerService.resumeSession(sessionId, userId)).thenReturn(response);

        // When & Then
        mockMvc.perform(put("/api/timer/{sessionId}/resume", sessionId)
                .with(csrf())
                .header("User-ID", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(sessionId))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.totalPausedMinutes").value(5));

        verify(focusTimerService).resumeSession(sessionId, userId);
    }

    @Test
    @WithMockUser
    void shouldCompleteFocusSession() throws Exception {
        // Given
        FocusSessionResponse response = FocusSessionResponse.builder()
            .id(sessionId)
            .userId(userId)
            .status(FocusSession.SessionStatus.COMPLETED)
            .completedAt(LocalDateTime.now())
            .productivityScore(85)
            .build();

        when(focusTimerService.completeSession(sessionId, userId)).thenReturn(response);

        // When & Then
        mockMvc.perform(put("/api/timer/{sessionId}/complete", sessionId)
                .with(csrf())
                .header("User-ID", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(sessionId))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.productivityScore").value(85));

        verify(focusTimerService).completeSession(sessionId, userId);
    }

    @Test
    @WithMockUser
    void shouldCancelFocusSession() throws Exception {
        // Given
        doNothing().when(focusTimerService).cancelSession(sessionId, userId);

        // When & Then
        mockMvc.perform(delete("/api/timer/{sessionId}/cancel", sessionId)
                .with(csrf())
                .header("User-ID", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Session cancelled successfully"));

        verify(focusTimerService).cancelSession(sessionId, userId);
    }

    @Test
    @WithMockUser
    void shouldGetCurrentSession() throws Exception {
        // Given
        FocusSessionResponse response = FocusSessionResponse.builder()
            .id(sessionId)
            .userId(userId)
            .status(FocusSession.SessionStatus.ACTIVE)
            .durationMinutes(25)
            .elapsedMinutes(10)
            .remainingMinutes(15)
            .build();

        when(focusTimerService.getCurrentSession(userId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/timer/current")
                .header("User-ID", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(sessionId))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.remainingMinutes").value(15));

        verify(focusTimerService).getCurrentSession(userId);
    }

    @Test
    @WithMockUser
    void shouldSynchronizeTimer() throws Exception {
        // Given
        TimerSyncResponse response = TimerSyncResponse.builder()
            .sessionId(sessionId)
            .status(FocusSession.SessionStatus.ACTIVE)
            .remainingMinutes(15)
            .elapsedMinutes(10)
            .lastSyncTime(LocalDateTime.now())
            .build();

        when(focusTimerService.synchronizeTimer(userId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/timer/sync")
                .header("User-ID", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value(sessionId))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.remainingMinutes").value(15));

        verify(focusTimerService).synchronizeTimer(userId);
    }

    @Test
    @WithMockUser
    void shouldGetActiveSessionsForHive() throws Exception {
        // Given
        List<FocusSessionResponse> activeSessions = Arrays.asList(
            FocusSessionResponse.builder()
                .id("session1")
                .userId("user1")
                .status(FocusSession.SessionStatus.ACTIVE)
                .build(),
            FocusSessionResponse.builder()
                .id("session2")
                .userId("user2")
                .status(FocusSession.SessionStatus.ACTIVE)
                .build()
        );

        when(focusTimerService.getActiveSessionsForHive(hiveId)).thenReturn(activeSessions);

        // When & Then
        mockMvc.perform(get("/api/timer/hive/{hiveId}/active", hiveId)
                .header("User-ID", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value("session1"))
            .andExpect(jsonPath("$[1].id").value("session2"));

        verify(focusTimerService).getActiveSessionsForHive(hiveId);
    }

    @Test
    @WithMockUser
    void shouldUpdateProductivityData() throws Exception {
        // Given
        ProductivityData data = ProductivityData.builder()
            .tabSwitches(5)
            .distractionMinutes(3)
            .focusBreaks(1)
            .notesCount(10)
            .build();

        doNothing().when(focusTimerService).updateProductivityData(eq(sessionId), any(ProductivityData.class));

        // When & Then
        mockMvc.perform(put("/api/timer/{sessionId}/productivity", sessionId)
                .with(csrf())
                .header("User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(data)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Productivity data updated"));

        verify(focusTimerService).updateProductivityData(eq(sessionId), any(ProductivityData.class));
    }

    @Test
    @WithMockUser
    void shouldCreateTimerTemplate() throws Exception {
        // Given
        TimerTemplateRequest request = TimerTemplateRequest.builder()
            .name("Custom Pomodoro")
            .focusDuration(30)
            .shortBreakDuration(5)
            .longBreakDuration(20)
            .sessionsBeforeLongBreak(4)
            .build();

        TimerTemplateResponse response = TimerTemplateResponse.builder()
            .id(UUID.randomUUID().toString())
            .userId(userId)
            .name("Custom Pomodoro")
            .focusDuration(30)
            .shortBreakDuration(5)
            .longBreakDuration(20)
            .sessionsBeforeLongBreak(4)
            .createdAt(LocalDateTime.now())
            .build();

        when(focusTimerService.createTemplate(eq(userId), any(TimerTemplateRequest.class)))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/timer/templates")
                .with(csrf())
                .header("User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Custom Pomodoro"))
            .andExpect(jsonPath("$.focusDuration").value(30));

        verify(focusTimerService).createTemplate(eq(userId), any(TimerTemplateRequest.class));
    }

    @Test
    @WithMockUser
    void shouldGetUserTemplates() throws Exception {
        // Given
        List<TimerTemplateResponse> templates = Arrays.asList(
            TimerTemplateResponse.builder()
                .id("template1")
                .name("Pomodoro")
                .focusDuration(25)
                .build(),
            TimerTemplateResponse.builder()
                .id("template2")
                .name("Deep Work")
                .focusDuration(90)
                .build()
        );

        when(focusTimerService.getUserTemplates(userId)).thenReturn(templates);

        // When & Then
        mockMvc.perform(get("/api/timer/templates")
                .header("User-ID", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].name").value("Pomodoro"))
            .andExpect(jsonPath("$[1].name").value("Deep Work"));

        verify(focusTimerService).getUserTemplates(userId);
    }

    @Test
    @WithMockUser
    void shouldGetUserStatistics() throws Exception {
        // Given
        UserTimerStatistics stats = UserTimerStatistics.builder()
            .userId(userId)
            .totalSessions(50)
            .totalFocusMinutes(1250)
            .averageSessionDuration(25)
            .averageProductivityScore(85.5)
            .completionRate(92.0)
            .mostProductiveHour(14)
            .streakDays(7)
            .build();

        when(focusTimerService.getUserStatistics(eq(userId), any(), any())).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/timer/statistics")
                .header("User-ID", userId)
                .param("startDate", "2024-01-01T00:00:00")
                .param("endDate", "2024-01-31T23:59:59"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalSessions").value(50))
            .andExpect(jsonPath("$.totalFocusMinutes").value(1250))
            .andExpect(jsonPath("$.averageProductivityScore").value(85.5))
            .andExpect(jsonPath("$.streakDays").value(7));

        verify(focusTimerService).getUserStatistics(eq(userId), any(), any());
    }

    @Test
    @WithMockUser
    void shouldGetSessionHistory() throws Exception {
        // Given
        List<FocusSessionResponse> sessions = Arrays.asList(
            FocusSessionResponse.builder()
                .id("session1")
                .status(FocusSession.SessionStatus.COMPLETED)
                .completedAt(LocalDateTime.now().minusDays(1))
                .productivityScore(90)
                .build(),
            FocusSessionResponse.builder()
                .id("session2")
                .status(FocusSession.SessionStatus.COMPLETED)
                .completedAt(LocalDateTime.now().minusDays(2))
                .productivityScore(85)
                .build()
        );

        Page<FocusSessionResponse> sessionPage = new PageImpl<>(sessions);
        when(focusTimerService.getUserSessionHistory(eq(userId), any())).thenReturn(sessionPage);

        // When & Then
        mockMvc.perform(get("/api/timer/history")
                .header("User-ID", userId)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].id").value("session1"))
            .andExpect(jsonPath("$.content[0].productivityScore").value(90));

        verify(focusTimerService).getUserSessionHistory(eq(userId), any());
    }

    @Test
    @WithMockUser
    void shouldReturn400WhenStartingSessionWithInvalidData() throws Exception {
        // Given
        StartTimerRequest request = StartTimerRequest.builder()
            .userId(userId)
            // Missing required fields
            .build();

        // When & Then
        mockMvc.perform(post("/api/timer/start")
                .with(csrf())
                .header("User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(focusTimerService, never()).startSession(any());
    }

    @Test
    @WithMockUser
    void shouldReturn409WhenUserAlreadyHasActiveSession() throws Exception {
        // Given
        StartTimerRequest request = StartTimerRequest.builder()
            .userId(userId)
            .hiveId(hiveId)
            .durationMinutes(25)
            .build();

        when(focusTimerService.startSession(any(StartTimerRequest.class)))
            .thenThrow(new IllegalStateException("User already has an active session"));

        // When & Then
        mockMvc.perform(post("/api/timer/start")
                .with(csrf())
                .header("User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("User already has an active session"));
    }
}