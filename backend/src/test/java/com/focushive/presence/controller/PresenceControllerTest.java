package com.focushive.presence.controller;

import com.focushive.presence.dto.*;
import com.focushive.presence.service.PresenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresenceControllerTest {

    @Mock
    private PresenceService presenceService;
    
    @Mock
    private Principal principal;
    
    @InjectMocks
    private PresenceController presenceController;
    
    private static final String USER_ID = "user123";
    
    @BeforeEach
    void setUp() {
        when(principal.getName()).thenReturn(USER_ID);
    }
    
    @Test
    void updatePresence_shouldCallServiceAndReturnResult() {
        // Given
        PresenceUpdate update = new PresenceUpdate(
            PresenceStatus.ONLINE,
            "hive123",
            "Working on task"
        );
        
        UserPresence expectedPresence = UserPresence.builder()
            .userId(USER_ID)
            .status(PresenceStatus.ONLINE)
            .currentHiveId("hive123")
            .activity("Working on task")
            .lastSeen(Instant.now())
            .build();
        
        when(presenceService.updateUserPresence(USER_ID, update)).thenReturn(expectedPresence);
        
        // When
        UserPresence result = presenceController.updatePresence(update, principal);
        
        // Then
        assertThat(result).isEqualTo(expectedPresence);
        verify(presenceService).updateUserPresence(USER_ID, update);
    }
    
    @Test
    void heartbeat_shouldCallService() {
        // When
        presenceController.heartbeat(principal);
        
        // Then
        verify(presenceService).recordHeartbeat(USER_ID);
    }
    
    @Test
    void joinHive_shouldCallServiceAndReturnPresenceInfo() {
        // Given
        String hiveId = "hive123";
        HivePresenceInfo expectedInfo = HivePresenceInfo.builder()
            .hiveId(hiveId)
            .activeUsers(5)
            .focusingSessions(2)
            .lastActivity(System.currentTimeMillis())
            .build();
        
        when(presenceService.joinHivePresence(hiveId, USER_ID)).thenReturn(expectedInfo);
        
        // When
        HivePresenceInfo result = presenceController.joinHive(hiveId, principal);
        
        // Then
        assertThat(result).isEqualTo(expectedInfo);
        verify(presenceService).joinHivePresence(hiveId, USER_ID);
    }
    
    @Test
    void leaveHive_shouldCallServiceAndReturnPresenceInfo() {
        // Given
        String hiveId = "hive123";
        HivePresenceInfo expectedInfo = HivePresenceInfo.builder()
            .hiveId(hiveId)
            .activeUsers(4)
            .focusingSessions(2)
            .lastActivity(System.currentTimeMillis())
            .build();
        
        when(presenceService.leaveHivePresence(hiveId, USER_ID)).thenReturn(expectedInfo);
        
        // When
        HivePresenceInfo result = presenceController.leaveHive(hiveId, principal);
        
        // Then
        assertThat(result).isEqualTo(expectedInfo);
        verify(presenceService).leaveHivePresence(hiveId, USER_ID);
    }
    
    @Test
    void subscribeToHivePresence_shouldReturnCurrentPresenceInfo() {
        // Given
        String hiveId = "hive123";
        List<UserPresence> activeUsers = Arrays.asList(
            UserPresence.builder().userId("user1").status(PresenceStatus.ONLINE).build(),
            UserPresence.builder().userId("user2").status(PresenceStatus.BUSY).build()
        );
        
        when(presenceService.getHiveActiveUsers(hiveId)).thenReturn(activeUsers);
        
        // When
        HivePresenceInfo result = presenceController.subscribeToHivePresence(hiveId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHiveId()).isEqualTo(hiveId);
        assertThat(result.getActiveUsers()).isEqualTo(2);
        assertThat(result.getOnlineMembers()).isEqualTo(activeUsers);
    }
    
    @Test
    void getHiveMembers_shouldReturnActiveUsers() {
        // Given
        String hiveId = "hive123";
        List<UserPresence> expectedUsers = Arrays.asList(
            UserPresence.builder().userId("user1").status(PresenceStatus.ONLINE).build(),
            UserPresence.builder().userId("user2").status(PresenceStatus.AWAY).build()
        );
        
        when(presenceService.getHiveActiveUsers(hiveId)).thenReturn(expectedUsers);
        
        // When
        List<UserPresence> result = presenceController.getHiveMembers(hiveId);
        
        // Then
        assertThat(result).isEqualTo(expectedUsers);
    }
    
    @Test
    void startFocusSession_shouldCallServiceAndReturnSession() {
        // Given
        PresenceController.StartSessionRequest request = new PresenceController.StartSessionRequest(
            "hive123",
            25
        );
        
        FocusSession expectedSession = FocusSession.builder()
            .sessionId("session456")
            .userId(USER_ID)
            .hiveId("hive123")
            .plannedDurationMinutes(25)
            .status(FocusSession.SessionStatus.ACTIVE)
            .startTime(Instant.now())
            .build();
        
        when(presenceService.startFocusSession(USER_ID, "hive123", 25))
            .thenReturn(expectedSession);
        
        // When
        FocusSession result = presenceController.startFocusSession(request, principal);
        
        // Then
        assertThat(result).isEqualTo(expectedSession);
        verify(presenceService).startFocusSession(USER_ID, "hive123", 25);
    }
    
    @Test
    void endFocusSession_shouldCallServiceAndReturnSession() {
        // Given
        String sessionId = "session456";
        PresenceController.EndSessionRequest request = new PresenceController.EndSessionRequest(sessionId);
        
        FocusSession expectedSession = FocusSession.builder()
            .sessionId(sessionId)
            .userId(USER_ID)
            .status(FocusSession.SessionStatus.COMPLETED)
            .endTime(Instant.now())
            .actualDurationMinutes(23)
            .build();
        
        when(presenceService.endFocusSession(USER_ID, sessionId))
            .thenReturn(expectedSession);
        
        // When
        FocusSession result = presenceController.endFocusSession(request, principal);
        
        // Then
        assertThat(result).isEqualTo(expectedSession);
        verify(presenceService).endFocusSession(USER_ID, sessionId);
    }
    
    @Test
    void getCurrentSession_shouldReturnActiveSession() {
        // Given
        FocusSession expectedSession = FocusSession.builder()
            .sessionId("session789")
            .userId(USER_ID)
            .status(FocusSession.SessionStatus.ACTIVE)
            .build();
        
        when(presenceService.getActiveFocusSession(USER_ID))
            .thenReturn(expectedSession);
        
        // When
        FocusSession result = presenceController.getCurrentSession(principal);
        
        // Then
        assertThat(result).isEqualTo(expectedSession);
    }
    
    @Test
    void getHiveSessions_shouldReturnAllSessions() {
        // Given
        String hiveId = "hive123";
        List<FocusSession> expectedSessions = Arrays.asList(
            FocusSession.builder()
                .sessionId("session1")
                .userId("user1")
                .status(FocusSession.SessionStatus.ACTIVE)
                .build(),
            FocusSession.builder()
                .sessionId("session2")
                .userId("user2")
                .status(FocusSession.SessionStatus.ACTIVE)
                .build()
        );
        
        when(presenceService.getHiveFocusSessions(hiveId))
            .thenReturn(expectedSessions);
        
        // When
        List<FocusSession> result = presenceController.getHiveSessions(hiveId);
        
        // Then
        assertThat(result).isEqualTo(expectedSessions);
    }
}