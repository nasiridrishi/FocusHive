package com.focushive.timer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.config.TestWebMvcSecurityConfig;
import com.focushive.timer.dto.*;
import com.focushive.timer.entity.FocusSession;
import com.focushive.timer.service.TimerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {TimerController.class, TestWebMvcSecurityConfig.class})
@ImportAutoConfiguration({
    org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class,
    org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration.class,
    org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class
})
@AutoConfigureMockMvc
@ActiveProfiles("webmvc-test")
@WithMockUser(username = "testuser", roles = "USER")
class TimerControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private TimerService timerService;
    
    @MockBean
    private com.focushive.backend.service.IdentityIntegrationService identityIntegrationService;
    
    private String userId;
    private String sessionId;
    
    @BeforeEach
    void setUp() {
        userId = "testuser";
        sessionId = UUID.randomUUID().toString();
    }
    
    @Test
    void startSession_Success() throws Exception {
        // Given
        StartSessionRequest request = StartSessionRequest.builder()
                .sessionType(FocusSession.SessionType.WORK)
                .durationMinutes(25)
                .notes("Working on timer tests")
                .build();
        
        FocusSessionDto response = FocusSessionDto.builder()
                .id(sessionId)
                .userId(userId)
                .sessionType(FocusSession.SessionType.WORK)
                .durationMinutes(25)
                .startTime(LocalDateTime.now())
                .completed(false)
                .build();
        
        when(timerService.startSession(userId, request)).thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/v1/timer/sessions/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(sessionId))
                .andExpect(jsonPath("$.sessionType").value("WORK"))
                .andExpect(jsonPath("$.durationMinutes").value(25))
                .andExpect(jsonPath("$.completed").value(false));
    }
    
    @Test
    void endSession_Success() throws Exception {
        // Given
        FocusSessionDto response = FocusSessionDto.builder()
                .id(sessionId)
                .userId(userId)
                .sessionType(FocusSession.SessionType.WORK)
                .durationMinutes(25)
                .actualDurationMinutes(23)
                .completed(true)
                .build();
        
        when(timerService.endSession(userId, sessionId)).thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/v1/timer/sessions/{sessionId}/end", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId))
                .andExpect(jsonPath("$.actualDurationMinutes").value(23))
                .andExpect(jsonPath("$.completed").value(true));
    }
    
    @Test
    void getCurrentSession_Found() throws Exception {
        // Given
        FocusSessionDto session = FocusSessionDto.builder()
                .id(sessionId)
                .userId(userId)
                .sessionType(FocusSession.SessionType.STUDY)
                .durationMinutes(45)
                .completed(false)
                .build();
        
        when(timerService.getCurrentSession(userId)).thenReturn(session);
        
        // When & Then
        mockMvc.perform(get("/api/v1/timer/sessions/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId))
                .andExpect(jsonPath("$.sessionType").value("STUDY"));
    }
    
    @Test
    void getCurrentSession_NotFound() throws Exception {
        // Given
        when(timerService.getCurrentSession(userId)).thenReturn(null);
        
        // When & Then
        mockMvc.perform(get("/api/v1/timer/sessions/current"))
                .andExpect(status().isNoContent());
    }
    
    @Test
    void getSessionHistory_Success() throws Exception {
        // Given
        List<FocusSessionDto> sessions = Arrays.asList(
                FocusSessionDto.builder()
                        .id(UUID.randomUUID().toString())
                        .sessionType(FocusSession.SessionType.WORK)
                        .durationMinutes(25)
                        .build(),
                FocusSessionDto.builder()
                        .id(UUID.randomUUID().toString())
                        .sessionType(FocusSession.SessionType.BREAK)
                        .durationMinutes(5)
                        .build()
        );
        
        when(timerService.getSessionHistory(userId, 0, 20)).thenReturn(sessions);
        
        // When & Then
        mockMvc.perform(get("/api/v1/timer/sessions/history")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sessionType").value("WORK"))
                .andExpect(jsonPath("$[1].sessionType").value("BREAK"));
    }
    
    @Test
    void getDailyStats_Success() throws Exception {
        // Given
        LocalDate date = LocalDate.now();
        ProductivityStatsDto stats = ProductivityStatsDto.builder()
                .userId(userId)
                .date(date)
                .totalFocusMinutes(240)
                .totalBreakMinutes(60)
                .sessionsCompleted(8)
                .focusRatio(80.0)
                .build();
        
        when(timerService.getDailyStats(userId, date)).thenReturn(stats);
        
        // When & Then
        mockMvc.perform(get("/api/v1/timer/stats/daily")
                .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFocusMinutes").value(240))
                .andExpect(jsonPath("$.totalBreakMinutes").value(60))
                .andExpect(jsonPath("$.sessionsCompleted").value(8))
                .andExpect(jsonPath("$.focusRatio").value(80.0));
    }
    
    @Test
    void getWeeklyStats_Success() throws Exception {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(6);
        List<ProductivityStatsDto> weeklyStats = Arrays.asList(
                ProductivityStatsDto.builder()
                        .date(startDate)
                        .totalFocusMinutes(180)
                        .build(),
                ProductivityStatsDto.builder()
                        .date(startDate.plusDays(1))
                        .totalFocusMinutes(240)
                        .build()
        );
        
        when(timerService.getWeeklyStats(userId, startDate)).thenReturn(weeklyStats);
        
        // When & Then
        mockMvc.perform(get("/api/v1/timer/stats/weekly")
                .param("startDate", startDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }
    
    @Test
    void getMonthlyStats_Success() throws Exception {
        // Given
        int year = 2025;
        int month = 7;
        List<ProductivityStatsDto> monthlyStats = Arrays.asList(
                ProductivityStatsDto.builder()
                        .date(LocalDate.of(year, month, 1))
                        .totalFocusMinutes(480)
                        .build()
        );
        
        when(timerService.getMonthlyStats(userId, year, month)).thenReturn(monthlyStats);
        
        // When & Then
        mockMvc.perform(get("/api/v1/timer/stats/monthly")
                .param("year", String.valueOf(year))
                .param("month", String.valueOf(month)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
    
    @Test
    void getCurrentStreak_Success() throws Exception {
        // Given
        when(timerService.getCurrentStreak(userId)).thenReturn(7);
        
        // When & Then
        mockMvc.perform(get("/api/v1/timer/stats/streak"))
                .andExpect(status().isOk())
                .andExpect(content().string("7"));
    }
    
    @Test
    void getPomodoroSettings_Success() throws Exception {
        // Given
        PomodoroSettingsDto settings = PomodoroSettingsDto.builder()
                .userId(userId)
                .workDurationMinutes(25)
                .shortBreakMinutes(5)
                .longBreakMinutes(15)
                .sessionsUntilLongBreak(4)
                .autoStartBreaks(false)
                .build();
        
        when(timerService.getPomodoroSettings(userId)).thenReturn(settings);
        
        // When & Then
        mockMvc.perform(get("/api/v1/timer/pomodoro/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workDurationMinutes").value(25))
                .andExpect(jsonPath("$.shortBreakMinutes").value(5))
                .andExpect(jsonPath("$.longBreakMinutes").value(15));
    }
    
    @Test
    void updatePomodoroSettings_Success() throws Exception {
        // Given
        PomodoroSettingsDto settings = PomodoroSettingsDto.builder()
                .workDurationMinutes(30)
                .shortBreakMinutes(10)
                .longBreakMinutes(20)
                .sessionsUntilLongBreak(3)
                .autoStartBreaks(true)
                .build();
        
        when(timerService.updatePomodoroSettings(userId, settings)).thenReturn(settings);
        
        // When & Then
        mockMvc.perform(put("/api/v1/timer/pomodoro/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(settings)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workDurationMinutes").value(30))
                .andExpect(jsonPath("$.autoStartBreaks").value(true));
    }
}