package com.focushive.websocket.controller;

import com.focushive.websocket.dto.PresenceUpdate;
import com.focushive.websocket.dto.WebSocketMessage;
import com.focushive.websocket.service.PresenceTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PresenceWebSocketControllerTest {
    
    @Mock
    private PresenceTrackingService presenceTrackingService;
    
    @Mock
    private Principal principal;
    
    @InjectMocks
    private PresenceWebSocketController controller;
    
    private Long userId;
    private String username;
    
    @BeforeEach
    void setUp() {
        userId = 12345L;
        username = "testuser";
        
        when(principal.getName()).thenReturn(String.valueOf(userId));
    }
    
    @Test
    void heartbeat_WithValidPrincipal_ShouldUpdateUserActivity() {
        // Given - principal is already mocked in setUp
        
        // When
        Map<String, Object> result = controller.heartbeat(principal);
        
        // Then
        verify(presenceTrackingService).updateUserActivity(eq(userId));
        
        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("ok");
        assertThat(result.get("userId")).isEqualTo(userId);
        assertThat(result.get("timestamp")).isNotNull();
    }
    
    @Test
    void updateStatus_WithValidStatusData_ShouldUpdatePresenceAndReturnMessage() {
        // Given
        Map<String, Object> statusData = new HashMap<>();
        statusData.put("status", "ONLINE");
        statusData.put("hiveId", "67890");
        statusData.put("activity", "Coding");
        
        PresenceUpdate expectedPresence = PresenceUpdate.builder()
            .userId(userId)
            .status(PresenceUpdate.PresenceStatus.ONLINE)
            .hiveId(67890L)
            .currentActivity("Coding")
            .lastSeen(LocalDateTime.now())
            .build();
            
        when(presenceTrackingService.getUserPresence(userId)).thenReturn(expectedPresence);
        
        // When
        WebSocketMessage<PresenceUpdate> result = controller.updateStatus(statusData, principal);
        
        // Then
        verify(presenceTrackingService).updateUserPresence(
            eq(userId), 
            eq(PresenceUpdate.PresenceStatus.ONLINE),
            eq(67890L),
            eq("Coding")
        );
        verify(presenceTrackingService).getUserPresence(eq(userId));
        
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(WebSocketMessage.MessageType.USER_ONLINE);
        assertThat(result.getEvent()).isEqualTo("presence.status.updated");
        assertThat(result.getPayload()).isEqualTo(expectedPresence);
        assertThat(result.getId()).isNotNull();
        assertThat(result.getTimestamp()).isNotNull();
    }
}