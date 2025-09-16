package com.focushive.websocket.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.websocket.dto.PresenceUpdate;
import com.focushive.websocket.dto.WebSocketMessage;
import com.focushive.websocket.service.PresenceTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for WebSocket presence functionality.
 * Tests real-time presence tracking, heartbeat, and status updates.
 * THIS WILL FAIL initially as enhanced features don't exist yet.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WebSocketPresenceIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private PresenceTrackingService presenceTrackingService;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketStompClient stompClient;
    private String websocketUrl;

    @BeforeEach
    void setUp() {
        websocketUrl = "ws://localhost:" + port + "/ws";

        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        stompClient.setMessageConverter(converter);
    }

    @Test
    @Timeout(10)
    void shouldEstablishWebSocketConnectionAndSubscribeToPresence() throws Exception {
        // Given
        BlockingQueue<WebSocketMessage<?>> receivedMessages = new LinkedBlockingQueue<>();
        CountDownLatch connectLatch = new CountDownLatch(1);
        CountDownLatch subscribeLatch = new CountDownLatch(1);

        StompSessionHandler sessionHandler = new TestSessionHandler(connectLatch, subscribeLatch, receivedMessages);

        // When
        StompSession session = stompClient.connect(websocketUrl, sessionHandler).get(5, TimeUnit.SECONDS);

        // Then
        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect to WebSocket");
        assertTrue(subscribeLatch.await(5, TimeUnit.SECONDS), "Should subscribe to presence topic");
        assertThat(session.isConnected()).isTrue();
    }

    @Test
    @Timeout(10)
    void shouldSendAndReceiveHeartbeat() throws Exception {
        // Given
        BlockingQueue<Map<String, Object>> heartbeatResponses = new LinkedBlockingQueue<>();
        CountDownLatch responseLatch = new CountDownLatch(1);
        Long userId = 12345L;

        StompSession session = connectWithUserId(userId);

        // Subscribe to heartbeat acknowledgment
        session.subscribe("/user/queue/presence/ack", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                heartbeatResponses.offer((Map<String, Object>) payload);
                responseLatch.countDown();
            }
        });

        // When - Send heartbeat
        session.send("/app/presence/ws-heartbeat", null);

        // Then
        assertTrue(responseLatch.await(5, TimeUnit.SECONDS), "Should receive heartbeat response");

        Map<String, Object> response = heartbeatResponses.poll();
        assertThat(response).isNotNull();
        assertThat(response.get("status")).isEqualTo("ok");
        assertThat(response.get("userId")).isEqualTo(userId);
    }

    @Test
    @Timeout(10)
    void shouldBroadcastPresenceUpdateToAllSubscribers() throws Exception {
        // Given - Two users connected
        Long user1Id = 1001L;
        Long user2Id = 1002L;
        BlockingQueue<WebSocketMessage<PresenceUpdate>> user2Messages = new LinkedBlockingQueue<>();
        CountDownLatch messageLatch = new CountDownLatch(1);

        StompSession user1Session = connectWithUserId(user1Id);
        StompSession user2Session = connectWithUserId(user2Id);

        // User2 subscribes to presence updates
        user2Session.subscribe("/topic/presence", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return new WebSocketMessageType();
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                user2Messages.offer((WebSocketMessage<PresenceUpdate>) payload);
                messageLatch.countDown();
            }
        });

        // When - User1 updates status
        Map<String, Object> statusUpdate = Map.of(
            "status", "IN_FOCUS_SESSION",
            "hiveId", "100",
            "activity", "Deep Work"
        );
        user1Session.send("/app/presence/status", statusUpdate);

        // Then - User2 receives the update
        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "User2 should receive presence update");

        WebSocketMessage<PresenceUpdate> message = user2Messages.poll();
        assertThat(message).isNotNull();
        assertThat(message.getEvent()).isEqualTo("presence.status.updated");
        assertThat(message.getPayload().getUserId()).isEqualTo(user1Id);
        assertThat(message.getPayload().getStatus()).isEqualTo(PresenceUpdate.PresenceStatus.IN_FOCUS_SESSION);
    }

    @Test
    @Timeout(10)
    void shouldTrackPresenceInSpecificHive() throws Exception {
        // Given
        Long userId = 2001L;
        Long hiveId = 300L;
        BlockingQueue<WebSocketMessage<?>> hiveMessages = new LinkedBlockingQueue<>();
        CountDownLatch messageLatch = new CountDownLatch(1);

        StompSession session = connectWithUserId(userId);

        // Subscribe to hive-specific presence
        session.subscribe("/topic/hive/" + hiveId + "/presence", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return WebSocketMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                hiveMessages.offer((WebSocketMessage<?>) payload);
                messageLatch.countDown();
            }
        });

        // When - User joins hive
        Map<String, Object> joinHive = Map.of(
            "status", "ONLINE",
            "hiveId", hiveId.toString(),
            "activity", "Working in Hive"
        );
        session.send("/app/presence/status", joinHive);

        // Then
        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Should receive hive presence update");

        WebSocketMessage<?> message = hiveMessages.poll();
        assertThat(message).isNotNull();
        assertThat(message.getEvent()).contains("presence");
    }

    @Test
    @Timeout(10)
    void shouldHandleConnectionLifecycle() throws Exception {
        // Given
        Long userId = 3001L;
        CountDownLatch connectLatch = new CountDownLatch(1);
        CountDownLatch disconnectLatch = new CountDownLatch(1);

        // Custom handler to track connection events
        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                connectLatch.countDown();
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                disconnectLatch.countDown();
            }
        };

        // When - Connect
        StompSession session = stompClient.connect(websocketUrl, sessionHandler).get(5, TimeUnit.SECONDS);
        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect");

        // Verify user is tracked as online
        PresenceUpdate presence = presenceTrackingService.getUserPresence(userId);
        assertThat(presence).isNotNull();

        // When - Disconnect
        session.disconnect();

        // Then - User should be marked offline
        Thread.sleep(1000); // Allow time for disconnect processing
        presence = presenceTrackingService.getUserPresence(userId);
        assertThat(presence.getStatus()).isEqualTo(PresenceUpdate.PresenceStatus.OFFLINE);
    }

    @Test
    @Timeout(10)
    void shouldHandleTypingIndicator() throws Exception {
        // Given
        Long userId = 4001L;
        String location = "hive:500";
        BlockingQueue<Map<String, Object>> typingMessages = new LinkedBlockingQueue<>();
        CountDownLatch messageLatch = new CountDownLatch(2);

        StompSession session = connectWithUserId(userId);

        // Subscribe to typing updates
        session.subscribe("/topic/presence/typing", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                typingMessages.offer((Map<String, Object>) payload);
                messageLatch.countDown();
            }
        });

        // When - Send typing start
        Map<String, Object> typingStart = Map.of(
            "location", location,
            "isTyping", true
        );
        session.send("/app/presence/typing", typingStart);

        // And - Send typing stop
        Thread.sleep(500);
        Map<String, Object> typingStop = Map.of(
            "location", location,
            "isTyping", false
        );
        session.send("/app/presence/typing", typingStop);

        // Then
        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Should receive typing updates");

        List<Map<String, Object>> messages = new ArrayList<>();
        typingMessages.drainTo(messages);

        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).get("isTyping")).isEqualTo(true);
        assertThat(messages.get(1).get("isTyping")).isEqualTo(false);
    }

    @Test
    @Timeout(10)
    void shouldStartAndTrackFocusSession() throws Exception {
        // Given
        Long userId = 5001L;
        Long hiveId = 600L;
        Integer focusMinutes = 25;

        StompSession session = connectWithUserId(userId);

        // When - Start focus session
        Map<String, Object> focusData = Map.of(
            "hiveId", hiveId.toString(),
            "minutes", focusMinutes.toString()
        );
        session.send("/app/presence/focus/start", focusData);

        // Then - Verify focus session is tracked
        Thread.sleep(500); // Allow processing time
        PresenceUpdate presence = presenceTrackingService.getUserPresence(userId);

        assertThat(presence).isNotNull();
        assertThat(presence.getStatus()).isEqualTo(PresenceUpdate.PresenceStatus.IN_FOCUS_SESSION);
        assertThat(presence.getFocusMinutesRemaining()).isEqualTo(focusMinutes);
        assertThat(presence.getHiveId()).isEqualTo(hiveId);
    }

    @Test
    @Timeout(10)
    void shouldGetHivePresenceList() throws Exception {
        // Given
        Long hiveId = 700L;
        Long user1Id = 6001L;
        Long user2Id = 6002L;
        BlockingQueue<WebSocketMessage<List<PresenceUpdate>>> presenceListMessages = new LinkedBlockingQueue<>();
        CountDownLatch messageLatch = new CountDownLatch(1);

        // Connect two users to the same hive
        StompSession user1Session = connectWithUserId(user1Id);
        StompSession user2Session = connectWithUserId(user2Id);

        // Update both users to be in the hive
        Map<String, Object> joinHive = Map.of(
            "status", "ONLINE",
            "hiveId", hiveId.toString()
        );
        user1Session.send("/app/presence/status", joinHive);
        user2Session.send("/app/presence/status", joinHive);

        Thread.sleep(500); // Allow updates to process

        // Subscribe to hive presence list response
        user1Session.subscribe("/topic/hive/" + hiveId + "/presence", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return new HivePresenceListType();
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                presenceListMessages.offer((WebSocketMessage<List<PresenceUpdate>>) payload);
                messageLatch.countDown();
            }
        });

        // When - Request hive presence list
        user1Session.send("/app/presence/hive/" + hiveId, null);

        // Then
        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Should receive hive presence list");

        WebSocketMessage<List<PresenceUpdate>> message = presenceListMessages.poll();
        assertThat(message).isNotNull();
        assertThat(message.getEvent()).isEqualTo("presence.hive.list");
        assertThat(message.getPayload()).hasSize(2);

        List<Long> userIds = message.getPayload().stream()
            .map(PresenceUpdate::getUserId)
            .toList();
        assertThat(userIds).containsExactlyInAnyOrder(user1Id, user2Id);
    }

    @Test
    @Timeout(10)
    void shouldHandleReconnectionWithStateRecovery() throws Exception {
        // Given
        Long userId = 7001L;
        Long hiveId = 800L;

        // First connection
        StompSession session1 = connectWithUserId(userId);
        Map<String, Object> statusUpdate = Map.of(
            "status", "IN_FOCUS_SESSION",
            "hiveId", hiveId.toString(),
            "activity", "Working"
        );
        session1.send("/app/presence/status", statusUpdate);
        Thread.sleep(500);

        // Disconnect
        session1.disconnect();
        Thread.sleep(500);

        // When - Reconnect
        StompSession session2 = connectWithUserId(userId);

        // Send recovery request
        session2.send("/app/presence/recover", Map.of("userId", userId));

        // Then - Previous state should be recovered
        Thread.sleep(500);
        PresenceUpdate presence = presenceTrackingService.getUserPresence(userId);
        assertThat(presence).isNotNull();
        assertThat(presence.getStatus()).isEqualTo(PresenceUpdate.PresenceStatus.IN_FOCUS_SESSION);
        assertThat(presence.getHiveId()).isEqualTo(hiveId);
    }

    @Test
    @Timeout(10)
    void shouldHandleMultipleHivePresenceUpdates() throws Exception {
        // Given
        Long userId = 8001L;
        Long hive1Id = 900L;
        Long hive2Id = 901L;

        StompSession session = connectWithUserId(userId);

        // When - User switches between hives
        Map<String, Object> joinHive1 = Map.of(
            "status", "ONLINE",
            "hiveId", hive1Id.toString()
        );
        session.send("/app/presence/status", joinHive1);
        Thread.sleep(500);

        // Switch to hive 2
        Map<String, Object> joinHive2 = Map.of(
            "status", "ONLINE",
            "hiveId", hive2Id.toString()
        );
        session.send("/app/presence/status", joinHive2);
        Thread.sleep(500);

        // Then - User should only be in hive 2
        List<PresenceUpdate> hive1Presence = presenceTrackingService.getHivePresence(hive1Id);
        List<PresenceUpdate> hive2Presence = presenceTrackingService.getHivePresence(hive2Id);

        assertThat(hive1Presence).noneMatch(p -> p.getUserId().equals(userId));
        assertThat(hive2Presence).anyMatch(p -> p.getUserId().equals(userId));
    }

    // Helper method to connect with a specific user ID
    private StompSession connectWithUserId(Long userId) throws Exception {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("User-ID", userId.toString());

        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {};
        return stompClient.connect(websocketUrl, headers, sessionHandler).get(5, TimeUnit.SECONDS);
    }

    // Test session handler
    private static class TestSessionHandler extends StompSessionHandlerAdapter {
        private final CountDownLatch connectLatch;
        private final CountDownLatch subscribeLatch;
        private final BlockingQueue<WebSocketMessage<?>> messages;

        public TestSessionHandler(CountDownLatch connectLatch, CountDownLatch subscribeLatch,
                                  BlockingQueue<WebSocketMessage<?>> messages) {
            this.connectLatch = connectLatch;
            this.subscribeLatch = subscribeLatch;
            this.messages = messages;
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            connectLatch.countDown();

            // Subscribe to presence topic
            session.subscribe("/topic/presence", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return WebSocketMessage.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    messages.offer((WebSocketMessage<?>) payload);
                    subscribeLatch.countDown();
                }
            });
        }

        @Override
        public void handleException(StompSession session, StompCommand command,
                                     StompHeaders headers, byte[] payload, Throwable exception) {
            exception.printStackTrace();
        }
    }

    // Custom type for WebSocketMessage<PresenceUpdate>
    private static class WebSocketMessageType implements java.lang.reflect.ParameterizedType {
        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{PresenceUpdate.class};
        }

        @Override
        public Type getRawType() {
            return WebSocketMessage.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }

    // Custom type for WebSocketMessage<List<PresenceUpdate>>
    private static class HivePresenceListType implements java.lang.reflect.ParameterizedType {
        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{new ListPresenceType()};
        }

        @Override
        public Type getRawType() {
            return WebSocketMessage.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }

    private static class ListPresenceType implements java.lang.reflect.ParameterizedType {
        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{PresenceUpdate.class};
        }

        @Override
        public Type getRawType() {
            return List.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }
}