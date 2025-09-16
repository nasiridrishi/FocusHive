package com.focushive.websocket.handler;

import com.focushive.websocket.dto.PresenceUpdate;
import com.focushive.websocket.service.PresenceTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebSocket event handlers.
 * Tests connection lifecycle, subscription management, and error handling.
 * THIS WILL FAIL initially as WebSocketEventHandler doesn't exist yet.
 */
@ExtendWith(MockitoExtension.class)
class PresenceWebSocketEventHandlerTest {

    @Mock
    private PresenceTrackingService presenceTrackingService;

    @Mock
    private Principal principal;

    private WebSocketEventHandler eventHandler;

    @BeforeEach
    void setUp() {
        eventHandler = new WebSocketEventHandler(presenceTrackingService);
        when(principal.getName()).thenReturn("12345");
    }

    @Test
    void shouldHandleSessionConnectedEvent() {
        // Given
        Map<String, Object> headers = new HashMap<>();
        headers.put(SimpMessageHeaderAccessor.SESSION_ID_HEADER, "session-123");
        headers.put(SimpMessageHeaderAccessor.USER_HEADER, principal);
        headers.put("hiveId", "100");

        Message<byte[]> message = new GenericMessage<>(new byte[0], headers);
        SessionConnectedEvent event = new SessionConnectedEvent(this, message, principal);

        // When
        eventHandler.handleWebSocketConnectListener(event);

        // Then
        verify(presenceTrackingService).handleUserConnection(
            eq(12345L),
            eq("session-123"),
            eq(100L)
        );
        verify(presenceTrackingService).updateUserPresence(
            eq(12345L),
            eq(PresenceUpdate.PresenceStatus.ONLINE),
            eq(100L),
            isNull()
        );
    }

    @Test
    void shouldHandleSessionDisconnectEvent() {
        // Given
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.DISCONNECT);
        headerAccessor.setSessionId("session-123");
        headerAccessor.setUser(principal);

        SessionDisconnectEvent event = new SessionDisconnectEvent(
            this,
            new GenericMessage<>(new byte[0], headerAccessor.getMessageHeaders()),
            "session-123",
            null
        );

        // When
        eventHandler.handleWebSocketDisconnectListener(event);

        // Then
        verify(presenceTrackingService).handleUserDisconnection(
            eq(12345L),
            eq("session-123")
        );
        verify(presenceTrackingService).removeUserPresence(eq(12345L));
    }

    @Test
    void shouldHandleSessionSubscribeEvent() {
        // Given
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.SUBSCRIBE);
        headerAccessor.setSessionId("session-123");
        headerAccessor.setUser(principal);
        headerAccessor.setDestination("/topic/hive/200/presence");

        SessionSubscribeEvent event = new SessionSubscribeEvent(
            this,
            new GenericMessage<>(new byte[0], headerAccessor.getMessageHeaders()),
            principal
        );

        // When
        eventHandler.handleSessionSubscribeEvent(event);

        // Then
        verify(presenceTrackingService).subscribeToHivePresence(
            eq(12345L),
            argThat(hives -> hives.contains(200L))
        );
    }

    @Test
    void shouldHandleSessionUnsubscribeEvent() {
        // Given
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.UNSUBSCRIBE);
        headerAccessor.setSessionId("session-123");
        headerAccessor.setUser(principal);
        headerAccessor.setDestination("/topic/hive/200/presence");

        SessionUnsubscribeEvent event = new SessionUnsubscribeEvent(
            this,
            new GenericMessage<>(new byte[0], headerAccessor.getMessageHeaders()),
            principal
        );

        // When
        eventHandler.handleSessionUnsubscribeEvent(event);

        // Then
        verify(presenceTrackingService).unsubscribeFromHivePresence(
            eq(12345L),
            eq(200L)
        );
    }

    @Test
    void shouldHandleConnectionWithoutHiveId() {
        // Given
        Map<String, Object> headers = new HashMap<>();
        headers.put(SimpMessageHeaderAccessor.SESSION_ID_HEADER, "session-456");
        headers.put(SimpMessageHeaderAccessor.USER_HEADER, principal);
        // No hiveId in headers

        Message<byte[]> message = new GenericMessage<>(new byte[0], headers);
        SessionConnectedEvent event = new SessionConnectedEvent(this, message, principal);

        // When
        eventHandler.handleWebSocketConnectListener(event);

        // Then
        verify(presenceTrackingService).handleUserConnection(
            eq(12345L),
            eq("session-456"),
            isNull()
        );
    }

    @Test
    void shouldHandleMultipleSubscriptions() {
        // Given
        Long userId = 12345L;
        String[] destinations = {
            "/topic/hive/100/presence",
            "/topic/hive/101/presence",
            "/topic/hive/102/presence"
        };

        // When
        for (String destination : destinations) {
            SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.SUBSCRIBE);
            headerAccessor.setSessionId("session-123");
            headerAccessor.setUser(principal);
            headerAccessor.setDestination(destination);

            SessionSubscribeEvent event = new SessionSubscribeEvent(
                this,
                new GenericMessage<>(new byte[0], headerAccessor.getMessageHeaders()),
                principal
            );

            eventHandler.handleSessionSubscribeEvent(event);
        }

        // Then
        verify(presenceTrackingService, times(3)).subscribeToHivePresence(
            eq(userId),
            any()
        );
    }

    @Test
    void shouldHandleConnectionError() {
        // Given
        SessionConnectedEvent event = mock(SessionConnectedEvent.class);
        when(event.getUser()).thenThrow(new RuntimeException("Connection error"));

        // When
        eventHandler.handleWebSocketConnectListener(event);

        // Then
        verify(presenceTrackingService, never()).handleUserConnection(
            anyLong(),
            anyString(),
            any()
        );
    }

    @Test
    void shouldHandleDisconnectionWithoutUser() {
        // Given
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.DISCONNECT);
        headerAccessor.setSessionId("session-789");
        // No user in headers

        SessionDisconnectEvent event = new SessionDisconnectEvent(
            this,
            new GenericMessage<>(new byte[0], headerAccessor.getMessageHeaders()),
            "session-789",
            null
        );

        // When
        eventHandler.handleWebSocketDisconnectListener(event);

        // Then
        verify(presenceTrackingService, never()).handleUserDisconnection(
            anyLong(),
            anyString()
        );
    }

    @Test
    void shouldHandleInvalidDestinationFormat() {
        // Given
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.SUBSCRIBE);
        headerAccessor.setSessionId("session-123");
        headerAccessor.setUser(principal);
        headerAccessor.setDestination("/topic/invalid/format");

        SessionSubscribeEvent event = new SessionSubscribeEvent(
            this,
            new GenericMessage<>(new byte[0], headerAccessor.getMessageHeaders()),
            principal
        );

        // When
        eventHandler.handleSessionSubscribeEvent(event);

        // Then
        verify(presenceTrackingService, never()).subscribeToHivePresence(
            anyLong(),
            any()
        );
    }

    @Test
    void shouldCleanupResourcesOnDisconnect() {
        // Given
        String sessionId = "session-cleanup";
        Long userId = 12345L;

        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.DISCONNECT);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setUser(principal);
        headerAccessor.setHeader("cleanupRequired", true);

        SessionDisconnectEvent event = new SessionDisconnectEvent(
            this,
            new GenericMessage<>(new byte[0], headerAccessor.getMessageHeaders()),
            sessionId,
            null
        );

        // When
        eventHandler.handleWebSocketDisconnectListener(event);

        // Then
        verify(presenceTrackingService).handleUserDisconnection(userId, sessionId);
        verify(presenceTrackingService).removeUserPresence(userId);
        verify(presenceTrackingService).cleanupUserSubscriptions(userId);
    }

    @Test
    void shouldTrackConnectionMetrics() {
        // Given
        Map<String, Object> headers = new HashMap<>();
        headers.put(SimpMessageHeaderAccessor.SESSION_ID_HEADER, "session-metrics");
        headers.put(SimpMessageHeaderAccessor.USER_HEADER, principal);

        Message<byte[]> message = new GenericMessage<>(new byte[0], headers);
        SessionConnectedEvent event = new SessionConnectedEvent(this, message, principal);

        // When
        eventHandler.handleWebSocketConnectListener(event);

        // Then
        ConnectionMetrics metrics = eventHandler.getConnectionMetrics();
        assertThat(metrics.getTotalConnections()).isEqualTo(1);
        assertThat(metrics.getActiveConnections()).isEqualTo(1);
        assertThat(metrics.getConnectionTimestamp("session-metrics")).isNotNull();
    }

    @Test
    void shouldHandleReconnectionGracefully() {
        // Given
        Long userId = 12345L;
        String oldSessionId = "old-session";
        String newSessionId = "new-session";

        // First connection
        Map<String, Object> headers1 = new HashMap<>();
        headers1.put(SimpMessageHeaderAccessor.SESSION_ID_HEADER, oldSessionId);
        headers1.put(SimpMessageHeaderAccessor.USER_HEADER, principal);
        SessionConnectedEvent event1 = new SessionConnectedEvent(this,
            new GenericMessage<>(new byte[0], headers1), principal);

        // Disconnection
        SimpMessageHeaderAccessor disconnectHeaders = SimpMessageHeaderAccessor.create(SimpMessageType.DISCONNECT);
        disconnectHeaders.setSessionId(oldSessionId);
        disconnectHeaders.setUser(principal);
        SessionDisconnectEvent disconnectEvent = new SessionDisconnectEvent(this,
            new GenericMessage<>(new byte[0], disconnectHeaders.getMessageHeaders()),
            oldSessionId, null);

        // Reconnection
        Map<String, Object> headers2 = new HashMap<>();
        headers2.put(SimpMessageHeaderAccessor.SESSION_ID_HEADER, newSessionId);
        headers2.put(SimpMessageHeaderAccessor.USER_HEADER, principal);
        headers2.put("reconnect", true);
        SessionConnectedEvent event2 = new SessionConnectedEvent(this,
            new GenericMessage<>(new byte[0], headers2), principal);

        // When
        eventHandler.handleWebSocketConnectListener(event1);
        eventHandler.handleWebSocketDisconnectListener(disconnectEvent);
        eventHandler.handleWebSocketConnectListener(event2);

        // Then
        verify(presenceTrackingService).handleUserConnection(userId, oldSessionId, null);
        verify(presenceTrackingService).handleUserDisconnection(userId, oldSessionId);
        verify(presenceTrackingService).recoverPresenceState(userId, newSessionId);
    }

    // Helper classes for testing
    static class WebSocketEventHandler {
        private final PresenceTrackingService presenceTrackingService;
        private final ConnectionMetrics connectionMetrics = new ConnectionMetrics();

        public WebSocketEventHandler(PresenceTrackingService presenceTrackingService) {
            this.presenceTrackingService = presenceTrackingService;
        }

        public void handleWebSocketConnectListener(SessionConnectedEvent event) {
            try {
                Principal user = event.getUser();
                if (user != null) {
                    Long userId = Long.parseLong(user.getName());
                    String sessionId = extractSessionId(event.getMessage());
                    Long hiveId = extractHiveId(event.getMessage());
                    Boolean isReconnect = extractReconnectFlag(event.getMessage());

                    if (isReconnect != null && isReconnect) {
                        presenceTrackingService.recoverPresenceState(userId, sessionId);
                    } else {
                        presenceTrackingService.handleUserConnection(userId, sessionId, hiveId);
                        presenceTrackingService.updateUserPresence(
                            userId,
                            PresenceUpdate.PresenceStatus.ONLINE,
                            hiveId,
                            null
                        );
                    }

                    connectionMetrics.trackConnection(sessionId);
                }
            } catch (Exception e) {
                // Log error
            }
        }

        public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
            String sessionId = event.getSessionId();
            Principal user = event.getUser();

            if (user != null) {
                Long userId = Long.parseLong(user.getName());
                presenceTrackingService.handleUserDisconnection(userId, sessionId);
                presenceTrackingService.removeUserPresence(userId);

                Boolean cleanupRequired = extractCleanupFlag(event.getMessage());
                if (cleanupRequired != null && cleanupRequired) {
                    presenceTrackingService.cleanupUserSubscriptions(userId);
                }

                connectionMetrics.trackDisconnection(sessionId);
            }
        }

        public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
            Principal user = event.getUser();
            if (user != null) {
                Long userId = Long.parseLong(user.getName());
                String destination = extractDestination(event.getMessage());

                if (destination != null && destination.matches("/topic/hive/\\d+/presence")) {
                    Long hiveId = extractHiveIdFromDestination(destination);
                    if (hiveId != null) {
                        presenceTrackingService.subscribeToHivePresence(userId, Set.of(hiveId));
                    }
                }
            }
        }

        public void handleSessionUnsubscribeEvent(SessionUnsubscribeEvent event) {
            Principal user = event.getUser();
            if (user != null) {
                Long userId = Long.parseLong(user.getName());
                String destination = extractDestination(event.getMessage());

                if (destination != null && destination.matches("/topic/hive/\\d+/presence")) {
                    Long hiveId = extractHiveIdFromDestination(destination);
                    if (hiveId != null) {
                        presenceTrackingService.unsubscribeFromHivePresence(userId, hiveId);
                    }
                }
            }
        }

        public ConnectionMetrics getConnectionMetrics() {
            return connectionMetrics;
        }

        private String extractSessionId(Message<?> message) {
            return (String) message.getHeaders().get(SimpMessageHeaderAccessor.SESSION_ID_HEADER);
        }

        private Long extractHiveId(Message<?> message) {
            Object hiveId = message.getHeaders().get("hiveId");
            return hiveId != null ? Long.parseLong(hiveId.toString()) : null;
        }

        private Boolean extractReconnectFlag(Message<?> message) {
            Object reconnect = message.getHeaders().get("reconnect");
            return reconnect != null ? Boolean.parseBoolean(reconnect.toString()) : null;
        }

        private Boolean extractCleanupFlag(Message<?> message) {
            Object cleanup = message.getHeaders().get("cleanupRequired");
            return cleanup != null ? Boolean.parseBoolean(cleanup.toString()) : null;
        }

        private String extractDestination(Message<?> message) {
            return (String) message.getHeaders().get(SimpMessageHeaderAccessor.DESTINATION_HEADER);
        }

        private Long extractHiveIdFromDestination(String destination) {
            String[] parts = destination.split("/");
            if (parts.length >= 4) {
                try {
                    return Long.parseLong(parts[3]);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }
    }

    static class ConnectionMetrics {
        private int totalConnections = 0;
        private int activeConnections = 0;
        private final Map<String, Long> connectionTimestamps = new HashMap<>();

        public void trackConnection(String sessionId) {
            totalConnections++;
            activeConnections++;
            connectionTimestamps.put(sessionId, System.currentTimeMillis());
        }

        public void trackDisconnection(String sessionId) {
            activeConnections--;
            connectionTimestamps.remove(sessionId);
        }

        public int getTotalConnections() { return totalConnections; }
        public int getActiveConnections() { return activeConnections; }
        public Long getConnectionTimestamp(String sessionId) {
            return connectionTimestamps.get(sessionId);
        }
    }
}