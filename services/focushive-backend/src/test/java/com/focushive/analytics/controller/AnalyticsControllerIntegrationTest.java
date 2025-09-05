package com.focushive.analytics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.analytics.dto.SessionRequest;
import com.focushive.analytics.dto.SessionResponse;
import com.focushive.analytics.dto.UserStats;
import com.focushive.analytics.service.AnalyticsService;
import com.focushive.config.TestWebMvcSecurityConfig;
import com.focushive.timer.entity.FocusSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {AnalyticsController.class, TestWebMvcSecurityConfig.class})
@ImportAutoConfiguration({
    org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class,
    org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration.class,
    org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class
})
@AutoConfigureMockMvc
@ActiveProfiles("webmvc-test")
@TestPropertySource(properties = {
    "app.features.analytics.enabled=true"
})
public class AnalyticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private AnalyticsService analyticsService;

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testStartFocusSession() throws Exception {
        SessionRequest request = new SessionRequest();
        request.setTargetDurationMinutes(25);
        request.setType(FocusSession.SessionType.WORK);
        request.setNotes("Test work session");

        // Mock service response
        SessionResponse mockResponse = new SessionResponse();
        mockResponse.setId("session-123");
        mockResponse.setUserId("testuser");
        mockResponse.setTargetDurationMinutes(25);
        mockResponse.setType(FocusSession.SessionType.WORK);
        mockResponse.setNotes("Test work session");
        mockResponse.setCompleted(false);
        
        when(analyticsService.startSession(any(SessionRequest.class), eq("testuser")))
            .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/analytics/session/start")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value("testuser"))
                .andExpect(jsonPath("$.targetDurationMinutes").value(25))
                .andExpect(jsonPath("$.type").value("WORK"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testGetCurrentUserStats() throws Exception {
        // Mock service response
        UserStats mockStats = new UserStats();
        mockStats.setUserId("testuser");
        mockStats.setTotalSessions(10);
        mockStats.setCompletedSessions(8);
        mockStats.setCompletionRate(80.0);
        mockStats.setTotalMinutes(200);
        mockStats.setAverageSessionLength(25.0);
        mockStats.setCurrentStreak(3);
        mockStats.setLongestStreak(5);
        mockStats.setStartDate(LocalDate.now().minusDays(30));
        mockStats.setEndDate(LocalDate.now());
        mockStats.setSessionsByType(Map.of("WORK", 6, "STUDY", 2));
        mockStats.setMinutesByHive(Map.of("hive1", 120, "hive2", 80));
        
        when(analyticsService.getUserStats(eq("testuser"), any(), any()))
            .thenReturn(mockStats);

        mockMvc.perform(get("/api/v1/analytics/user/stats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value("testuser"))
                .andExpect(jsonPath("$.totalSessions").value(10))
                .andExpect(jsonPath("$.completedSessions").value(8))
                .andExpect(jsonPath("$.completionRate").value(80.0));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testGetLeaderboard() throws Exception {
        // Mock service response
        AnalyticsController.LeaderboardEntry entry1 = new AnalyticsController.LeaderboardEntry(
            "user1", "User One", 120, 5, 90.0, 1);
        AnalyticsController.LeaderboardEntry entry2 = new AnalyticsController.LeaderboardEntry(
            "user2", "User Two", 100, 4, 85.0, 2);
        List<AnalyticsController.LeaderboardEntry> mockLeaderboard = List.of(entry1, entry2);
        
        when(analyticsService.getHiveLeaderboard(eq("test-hive-id"), any()))
            .thenReturn(mockLeaderboard);

        mockMvc.perform(get("/api/v1/analytics/leaderboard")
                .param("hiveId", "test-hive-id"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value("user1"))
                .andExpect(jsonPath("$[0].rank").value(1));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testGetLeaderboardWithoutHiveId() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/leaderboard"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testEndFocusSession() throws Exception {
        // Mock end session response
        SessionResponse endResponse = new SessionResponse();
        endResponse.setId("session-123");
        endResponse.setUserId("testuser");
        endResponse.setTargetDurationMinutes(25);
        endResponse.setActualDurationMinutes(23);
        endResponse.setType(FocusSession.SessionType.WORK);
        endResponse.setCompleted(true);
        
        when(analyticsService.endSession(eq("session-123"), any(), eq("testuser")))
            .thenReturn(endResponse);

        // End the session
        AnalyticsController.EndFocusSessionRequest endRequest = 
            new AnalyticsController.EndFocusSessionRequest(
                "session-123", 
                23, // actual duration
                true, // completed
                1, // breaks taken
                2, // distractions logged
                "Completed work session"
            );

        mockMvc.perform(post("/api/v1/analytics/session/end")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(endRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("session-123"))
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.actualDurationMinutes").value(23));
    }
}