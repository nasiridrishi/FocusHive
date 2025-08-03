package com.focushive.presence.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.presence.dto.FocusSession;
import com.focushive.presence.dto.HivePresenceInfo;
import com.focushive.presence.dto.PresenceStatus;
import com.focushive.presence.dto.UserPresence;
import com.focushive.presence.service.PresenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PresenceRestController.class,
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class,
        AutoConfigureTestDatabase.class,
        JpaRepositoriesAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FeignAutoConfiguration.class,
        SpringDataWebAutoConfiguration.class
    }
)
class PresenceRestControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private PresenceService presenceService;
    
    // Mock all dependencies that could be autowired
    @MockBean(name = "identityServiceClient")
    private com.focushive.api.client.IdentityServiceClient identityServiceClient;
    
    @MockBean(name = "identityServiceAuthenticationFilter")
    private com.focushive.api.security.IdentityServiceAuthenticationFilter identityServiceAuthenticationFilter;
    
    @Test
    void getUserPresence_whenExists_shouldReturn200() throws Exception {
        // Given
        String userId = "user456";
        UserPresence expectedPresence = UserPresence.builder()
            .userId(userId)
            .status(PresenceStatus.ONLINE)
            .currentHiveId("hive123")
            .lastSeen(Instant.now())
            .build();
        
        when(presenceService.getUserPresence(userId)).thenReturn(expectedPresence);
        
        // When & Then
        mockMvc.perform(get("/api/v1/presence/users/{userId}", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(userId))
            .andExpect(jsonPath("$.status").value("ONLINE"))
            .andExpect(jsonPath("$.currentHiveId").value("hive123"));
    }
    
    @Test
    void getUserPresence_whenNotExists_shouldReturn404() throws Exception {
        // Given
        String userId = "user456";
        when(presenceService.getUserPresence(userId)).thenReturn(null);
        
        // When & Then
        mockMvc.perform(get("/api/v1/presence/users/{userId}", userId))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void getMyPresence_whenOnline_shouldReturn200() throws Exception {
        // Given
        UserPresence expectedPresence = UserPresence.builder()
            .userId("user123")
            .status(PresenceStatus.BUSY)
            .activity("In a meeting")
            .lastSeen(Instant.now())
            .build();
        
        when(presenceService.getUserPresence("user123")).thenReturn(expectedPresence);
        
        // When & Then
        mockMvc.perform(get("/api/v1/presence/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value("user123"))
            .andExpect(jsonPath("$.status").value("BUSY"))
            .andExpect(jsonPath("$.activity").value("In a meeting"));
    }
    
    @Test
    void getMyPresence_whenOffline_shouldReturn404() throws Exception {
        // Given
        when(presenceService.getUserPresence("user123")).thenReturn(null);
        
        // When & Then
        mockMvc.perform(get("/api/v1/presence/me"))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void getHivePresence_shouldReturnPresenceInfo() throws Exception {
        // Given
        String hiveId = "hive123";
        List<UserPresence> activeUsers = Arrays.asList(
            UserPresence.builder().userId("user1").status(PresenceStatus.ONLINE).build(),
            UserPresence.builder().userId("user2").status(PresenceStatus.AWAY).build()
        );
        
        List<FocusSession> sessions = Arrays.asList(
            FocusSession.builder()
                .sessionId("session1")
                .status(FocusSession.SessionStatus.ACTIVE)
                .build()
        );
        
        when(presenceService.getHiveActiveUsers(hiveId)).thenReturn(activeUsers);
        when(presenceService.getHiveFocusSessions(hiveId)).thenReturn(sessions);
        
        // When & Then
        mockMvc.perform(get("/api/v1/presence/hives/{hiveId}", hiveId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hiveId").value(hiveId))
            .andExpect(jsonPath("$.activeUsers").value(2))
            .andExpect(jsonPath("$.focusingSessions").value(1))
            .andExpect(jsonPath("$.onlineMembers").isArray())
            .andExpect(jsonPath("$.onlineMembers.length()").value(2));
    }
    
    @Test
    void getMultipleHivesPresence_shouldReturnPresenceMap() throws Exception {
        // Given
        Set<String> hiveIds = new HashSet<>(Arrays.asList("hive1", "hive2"));
        
        Map<String, HivePresenceInfo> presenceMap = new HashMap<>();
        presenceMap.put("hive1", HivePresenceInfo.builder()
            .hiveId("hive1")
            .activeUsers(3)
            .focusingSessions(1)
            .build());
        presenceMap.put("hive2", HivePresenceInfo.builder()
            .hiveId("hive2")
            .activeUsers(5)
            .focusingSessions(2)
            .build());
        
        when(presenceService.getHivesPresenceInfo(any())).thenReturn(presenceMap);
        
        // When & Then
        mockMvc.perform(post("/api/v1/presence/hives/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(hiveIds)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hive1.activeUsers").value(3))
            .andExpect(jsonPath("$.hive1.focusingSessions").value(1))
            .andExpect(jsonPath("$.hive2.activeUsers").value(5))
            .andExpect(jsonPath("$.hive2.focusingSessions").value(2));
    }
    
    @Test
    void getMyActiveSession_whenExists_shouldReturn200() throws Exception {
        // Given
        FocusSession expectedSession = FocusSession.builder()
            .sessionId("session123")
            .userId("user123")
            .hiveId("hive456")
            .status(FocusSession.SessionStatus.ACTIVE)
            .plannedDurationMinutes(25)
            .startTime(Instant.now())
            .build();
        
        when(presenceService.getActiveFocusSession("user123")).thenReturn(expectedSession);
        
        // When & Then
        mockMvc.perform(get("/api/v1/presence/sessions/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value("session123"))
            .andExpect(jsonPath("$.userId").value("user123"))
            .andExpect(jsonPath("$.hiveId").value("hive456"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.plannedDurationMinutes").value(25));
    }
    
    @Test
    void getMyActiveSession_whenNotExists_shouldReturn404() throws Exception {
        // Given
        when(presenceService.getActiveFocusSession("user123")).thenReturn(null);
        
        // When & Then
        mockMvc.perform(get("/api/v1/presence/sessions/me"))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void getHiveSessions_shouldReturnSessionsList() throws Exception {
        // Given
        String hiveId = "hive123";
        List<FocusSession> expectedSessions = Arrays.asList(
            FocusSession.builder()
                .sessionId("session1")
                .userId("user1")
                .status(FocusSession.SessionStatus.ACTIVE)
                .plannedDurationMinutes(30)
                .build(),
            FocusSession.builder()
                .sessionId("session2")
                .userId("user2")
                .status(FocusSession.SessionStatus.COMPLETED)
                .plannedDurationMinutes(25)
                .actualDurationMinutes(28)
                .build()
        );
        
        when(presenceService.getHiveFocusSessions(hiveId)).thenReturn(expectedSessions);
        
        // When & Then
        mockMvc.perform(get("/api/v1/presence/hives/{hiveId}/sessions", hiveId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].sessionId").value("session1"))
            .andExpect(jsonPath("$[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$[1].sessionId").value("session2"))
            .andExpect(jsonPath("$[1].status").value("COMPLETED"));
    }
    
    @Test
    void getUserPresence_withoutAuth_shouldStillWork() throws Exception {
        // Given - Security is disabled, so this should work
        String userId = "user123";
        UserPresence expectedPresence = UserPresence.builder()
            .userId(userId)
            .status(PresenceStatus.ONLINE)
            .currentHiveId("hive123")
            .lastSeen(Instant.now())
            .build();
        
        when(presenceService.getUserPresence(userId)).thenReturn(expectedPresence);
        
        // When & Then
        mockMvc.perform(get("/api/v1/presence/users/user123"))
            .andExpect(status().isOk());
    }
    
    @Test
    void getHivePresence_withoutAuth_shouldStillWork() throws Exception {
        // Given - Security is disabled, so this should work
        String hiveId = "hive123";
        when(presenceService.getHiveActiveUsers(hiveId)).thenReturn(Arrays.asList());
        when(presenceService.getHiveFocusSessions(hiveId)).thenReturn(Arrays.asList());
        
        // When & Then
        mockMvc.perform(get("/api/v1/presence/hives/hive123"))
            .andExpect(status().isOk());
    }
}