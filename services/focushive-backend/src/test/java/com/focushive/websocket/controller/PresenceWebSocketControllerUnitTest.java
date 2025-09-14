package com.focushive.websocket.controller;

import com.focushive.websocket.dto.PresenceUpdate;
import com.focushive.websocket.service.PresenceTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for PresenceWebSocketController using mocks
 * This avoids the WebSocket integration issues by testing the controller logic in isolation
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PresenceWebSocketControllerUnitTest {

    @Mock
    private PresenceTrackingService presenceTrackingService;

    @Mock
    private Principal principal;

    @InjectMocks
    private PresenceWebSocketController controller;

    @BeforeEach
    void setUp() {
        // Setup is now done per test to avoid unnecessary stubbing warnings
    }

    @Test
    void shouldHandleHeartbeat() {
        // GIVEN: A user sending a heartbeat
        when(principal.getName()).thenReturn("1");
        Long expectedUserId = 1L;

        // WHEN: Heartbeat is processed
        Map<String, Object> result = controller.heartbeat(principal);

        // THEN: User activity should be updated and acknowledgment returned
        verify(presenceTrackingService).updateUserActivity(expectedUserId);

        assertNotNull(result);
        assertEquals("ok", result.get("status"));
        assertEquals(expectedUserId, result.get("userId"));
        assertNotNull(result.get("timestamp"));
    }

    @Test
    void shouldUpdateUserStatus() {
        // GIVEN: Status update data
        when(principal.getName()).thenReturn("1");
        Map<String, Object> statusData = new HashMap<>();
        statusData.put("status", "ONLINE");
        statusData.put("hiveId", "123");
        statusData.put("activity", "Working on project");

        Long userId = 1L;
        Long hiveId = 123L;

        PresenceUpdate mockPresence = PresenceUpdate.builder()
            .userId(userId)
            .status(PresenceUpdate.PresenceStatus.ONLINE)
            .hiveId(hiveId)
            .currentActivity("Working on project")
            .lastSeen(LocalDateTime.now())
            .build();

        when(presenceTrackingService.getUserPresence(userId)).thenReturn(mockPresence);

        // WHEN: Status is updated
        var result = controller.updateStatus(statusData, principal);

        // THEN: Presence should be updated and message returned
        verify(presenceTrackingService).updateUserPresence(
            eq(userId),
            eq(PresenceUpdate.PresenceStatus.ONLINE),
            eq(hiveId),
            eq("Working on project")
        );
        verify(presenceTrackingService).getUserPresence(userId);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("presence.status.updated", result.getEvent());
        assertEquals(mockPresence, result.getPayload());
    }

    @Test
    void shouldStartFocusSession() {
        // GIVEN: Focus session data
        when(principal.getName()).thenReturn("1");
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("hiveId", "456");
        sessionData.put("minutes", "25");

        Long userId = 1L;
        Long hiveId = 456L;
        Integer focusMinutes = 25;

        // WHEN: Focus session is started
        controller.startFocusSession(sessionData, principal);

        // THEN: Focus session should be started
        verify(presenceTrackingService).startFocusSession(userId, hiveId, focusMinutes);
    }

    @Test
    void shouldStartBuddySession() {
        // GIVEN: Buddy session data
        when(principal.getName()).thenReturn("1");
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("buddyId", "789");

        Long userId = 1L;
        Long buddyId = 789L;

        // WHEN: Buddy session is started
        controller.startBuddySession(sessionData, principal);

        // THEN: Buddy session should be started
        verify(presenceTrackingService).startBuddySession(userId, buddyId);
    }

    @Test
    void shouldGetUserPresence() {
        // GIVEN: A user ID to lookup
        // No principal mocking needed for this test since it doesn't use principal
        Long targetUserId = 123L;

        PresenceUpdate mockPresence = PresenceUpdate.builder()
            .userId(targetUserId)
            .status(PresenceUpdate.PresenceStatus.AWAY)
            .lastSeen(LocalDateTime.now().minusMinutes(5))
            .build();

        when(presenceTrackingService.getUserPresence(targetUserId)).thenReturn(mockPresence);

        // WHEN: Getting user presence
        var result = controller.getUserPresence(targetUserId);

        // THEN: Presence information should be returned
        verify(presenceTrackingService).getUserPresence(targetUserId);

        assertNotNull(result);
        assertEquals("presence.user.info", result.getEvent());
        assertEquals(mockPresence, result.getPayload());
    }

    @Test
    void shouldDisconnectUser() {
        // GIVEN: A user requesting disconnection
        when(principal.getName()).thenReturn("1");
        Long userId = 1L;

        // WHEN: User disconnects
        controller.disconnect(principal);

        // THEN: User presence should be removed
        verify(presenceTrackingService).removeUserPresence(userId);
    }

    @Test
    void shouldHandleTypingIndicator() {
        // GIVEN: Typing indicator data
        when(principal.getName()).thenReturn("1"); // Mock user ID as string
        Map<String, Object> typingData = new HashMap<>();
        typingData.put("location", "hive:123");
        typingData.put("isTyping", "true");

        Long userId = 1L;
        String location = "hive:123";
        boolean isTyping = true;

        // WHEN: Typing indicator is sent
        Map<String, Object> result = controller.userTyping(typingData, principal);

        // THEN: Typing information should be returned
        assertNotNull(result);
        assertEquals(userId, result.get("userId"));
        assertEquals("1", result.get("username")); // Username is the principal name
        assertEquals(location, result.get("location"));
        assertEquals(isTyping, result.get("isTyping"));
        assertNotNull(result.get("timestamp"));
    }
}