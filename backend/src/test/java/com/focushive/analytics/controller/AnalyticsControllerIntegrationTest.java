package com.focushive.analytics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.analytics.dto.SessionRequest;
import com.focushive.timer.entity.FocusSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(locations = "classpath:application-test.yml")
@Transactional
public class AnalyticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testStartFocusSession() throws Exception {
        SessionRequest request = new SessionRequest();
        request.setTargetDurationMinutes(25);
        request.setType(FocusSession.SessionType.WORK);
        request.setNotes("Test work session");

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
        mockMvc.perform(get("/api/v1/analytics/user/stats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value("testuser"))
                .andExpect(jsonPath("$.totalSessions").exists())
                .andExpect(jsonPath("$.completedSessions").exists())
                .andExpect(jsonPath("$.completionRate").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testGetLeaderboard() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/leaderboard")
                .param("hiveId", "test-hive-id"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
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
        // First start a session to get a session ID
        SessionRequest startRequest = new SessionRequest();
        startRequest.setTargetDurationMinutes(25);
        startRequest.setType(FocusSession.SessionType.WORK);

        String startResponse = mockMvc.perform(post("/api/v1/analytics/session/start")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(startRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Parse session ID from response
        String sessionId = objectMapper.readTree(startResponse).get("id").asText();

        // End the session
        AnalyticsController.EndFocusSessionRequest endRequest = 
            new AnalyticsController.EndFocusSessionRequest(
                sessionId, 
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
                .andExpect(jsonPath("$.id").value(sessionId))
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.actualDurationMinutes").value(23));
    }
}