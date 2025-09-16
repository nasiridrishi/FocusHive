package com.focushive.timer.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.timer.dto.FocusSessionResponse;
import com.focushive.timer.dto.StartTimerRequest;
import com.focushive.timer.dto.TimerSyncResponse;
import com.focushive.timer.entity.FocusSession;
import com.focushive.timer.service.FocusTimerService;
import com.focushive.websocket.dto.WebSocketMessage;
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
import java.util.Map;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for Focus Timer WebSocket functionality.
 * Tests real-time timer synchronization and notifications.
 * THIS WILL FAIL initially as timer WebSocket features don't exist yet.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FocusTimerWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private FocusTimerService focusTimerService;

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
    void shouldStartTimerViaWebSocket() throws Exception {
        // Given
        String userId = "12345";
        String hiveId = "100";
        BlockingQueue<WebSocketMessage<FocusSessionResponse>> responses = new LinkedBlockingQueue<>();
        CountDownLatch responseLatch = new CountDownLatch(1);

        StompSession session = connectWithUserId(userId);

        // Subscribe to timer updates
        session.subscribe("/user/queue/timer/updates", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return new TimerMessageType();
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                responses.offer((WebSocketMessage<FocusSessionResponse>) payload);
                responseLatch.countDown();
            }
        });

        // When - Start timer
        Map<String, Object> startRequest = Map.of(
            "userId", userId,
            "hiveId", hiveId,
            "durationMinutes", 25,
            "sessionType", "FOCUS",
            "title", "WebSocket Timer Test"
        );
        session.send("/app/timer/start", startRequest);

        // Then
        assertTrue(responseLatch.await(5, TimeUnit.SECONDS), "Should receive timer started response");

        WebSocketMessage<FocusSessionResponse> message = responses.poll();
        assertThat(message).isNotNull();
        assertThat(message.getEvent()).isEqualTo("timer.started");
        assertThat(message.getPayload().getUserId()).isEqualTo(userId);
        assertThat(message.getPayload().getDurationMinutes()).isEqualTo(25);
        assertThat(message.getPayload().getStatus()).isEqualTo(FocusSession.SessionStatus.ACTIVE);
    }

    @Test
    @Timeout(10)
    void shouldBroadcastTimerUpdatesToHive() throws Exception {
        // Given
        String user1Id = "1001";
        String user2Id = "1002";
        String hiveId = "200";
        BlockingQueue<WebSocketMessage<?>> hiveMessages = new LinkedBlockingQueue<>();
        CountDownLatch messageLatch = new CountDownLatch(2);

        StompSession user1Session = connectWithUserId(user1Id);
        StompSession user2Session = connectWithUserId(user2Id);

        // User2 subscribes to hive timer updates
        user2Session.subscribe("/topic/hive/" + hiveId + "/timers", new StompFrameHandler() {
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

        // When - User1 starts and pauses timer
        Map<String, Object> startRequest = Map.of(
            "userId", user1Id,
            "hiveId", hiveId,
            "durationMinutes", 25
        );
        user1Session.send("/app/timer/start", startRequest);

        Thread.sleep(500);

        Map<String, Object> pauseRequest = Map.of(
            "sessionId", "test-session-id"
        );
        user1Session.send("/app/timer/pause", pauseRequest);

        // Then
        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Should receive timer updates in hive");

        assertThat(hiveMessages).hasSize(2);
        WebSocketMessage<?> startMessage = hiveMessages.poll();
        assertThat(startMessage.getEvent()).contains("timer");
    }

    @Test
    @Timeout(10)
    void shouldSynchronizeTimerAcrossDevices() throws Exception {
        // Given
        String userId = "2001";
        String sessionId = startTimerDirectly(userId, "300", 25);
        BlockingQueue<TimerSyncResponse> syncResponses = new LinkedBlockingQueue<>();
        CountDownLatch syncLatch = new CountDownLatch(1);

        StompSession session = connectWithUserId(userId);

        // Subscribe to sync response
        session.subscribe("/user/queue/timer/sync", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return TimerSyncResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                syncResponses.offer((TimerSyncResponse) payload);
                syncLatch.countDown();
            }
        });

        // When - Request sync
        session.send("/app/timer/sync", Map.of("userId", userId));

        // Then
        assertTrue(syncLatch.await(5, TimeUnit.SECONDS), "Should receive sync response");

        TimerSyncResponse syncResponse = syncResponses.poll();
        assertThat(syncResponse).isNotNull();
        assertThat(syncResponse.getSessionId()).isEqualTo(sessionId);
        assertThat(syncResponse.getStatus()).isEqualTo(FocusSession.SessionStatus.ACTIVE);
        assertThat(syncResponse.getRemainingMinutes()).isLessThanOrEqualTo(25);
    }

    @Test
    @Timeout(10)
    void shouldReceiveTimerReminders() throws Exception {
        // Given
        String userId = "3001";
        String hiveId = "400";
        BlockingQueue<WebSocketMessage<?>> reminderMessages = new LinkedBlockingQueue<>();
        CountDownLatch reminderLatch = new CountDownLatch(1);

        StompSession session = connectWithUserId(userId);

        // Subscribe to reminders
        session.subscribe("/user/queue/timer/reminders", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return WebSocketMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                reminderMessages.offer((WebSocketMessage<?>) payload);
                reminderLatch.countDown();
            }
        });

        // When - Start timer with reminder
        Map<String, Object> startRequest = Map.of(
            "userId", userId,
            "hiveId", hiveId,
            "durationMinutes", 1, // 1 minute for quick test
            "reminderEnabled", true,
            "reminderMinutesBefore", 0.5 // 30 seconds before end
        );
        session.send("/app/timer/start", startRequest);

        // Then - Wait for reminder
        assertTrue(reminderLatch.await(40, TimeUnit.SECONDS), "Should receive timer reminder");

        WebSocketMessage<?> reminder = reminderMessages.poll();
        assertThat(reminder).isNotNull();
        assertThat(reminder.getEvent()).isEqualTo("timer.reminder");
    }

    @Test
    @Timeout(10)
    void shouldHandleTimerCompletion() throws Exception {
        // Given
        String userId = "4001";
        String hiveId = "500";
        BlockingQueue<WebSocketMessage<?>> completionMessages = new LinkedBlockingQueue<>();
        CountDownLatch completionLatch = new CountDownLatch(1);

        StompSession session = connectWithUserId(userId);

        // Subscribe to completion events
        session.subscribe("/user/queue/timer/completion", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return WebSocketMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                completionMessages.offer((WebSocketMessage<?>) payload);
                completionLatch.countDown();
            }
        });

        // When - Start short timer and wait for completion
        Map<String, Object> startRequest = Map.of(
            "userId", userId,
            "hiveId", hiveId,
            "durationMinutes", 0.1, // 6 seconds for quick test
            "autoComplete", true
        );
        session.send("/app/timer/start", startRequest);

        // Then - Wait for auto-completion
        assertTrue(completionLatch.await(15, TimeUnit.SECONDS), "Should receive completion event");

        WebSocketMessage<?> completion = completionMessages.poll();
        assertThat(completion).isNotNull();
        assertThat(completion.getEvent()).isEqualTo("timer.completed");
    }

    @Test
    @Timeout(10)
    void shouldPauseAndResumeTimer() throws Exception {
        // Given
        String userId = "5001";
        String sessionId = startTimerDirectly(userId, "600", 25);
        BlockingQueue<WebSocketMessage<?>> timerMessages = new LinkedBlockingQueue<>();
        CountDownLatch messageLatch = new CountDownLatch(2);

        StompSession session = connectWithUserId(userId);

        // Subscribe to timer updates
        session.subscribe("/user/queue/timer/updates", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return WebSocketMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                timerMessages.offer((WebSocketMessage<?>) payload);
                messageLatch.countDown();
            }
        });

        // When - Pause and resume
        session.send("/app/timer/pause", Map.of("sessionId", sessionId));
        Thread.sleep(500);
        session.send("/app/timer/resume", Map.of("sessionId", sessionId));

        // Then
        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Should receive pause and resume events");

        assertThat(timerMessages).hasSize(2);
        WebSocketMessage<?> pauseMessage = timerMessages.poll();
        assertThat(pauseMessage.getEvent()).isEqualTo("timer.paused");

        WebSocketMessage<?> resumeMessage = timerMessages.poll();
        assertThat(resumeMessage.getEvent()).isEqualTo("timer.resumed");
    }

    @Test
    @Timeout(10)
    void shouldReceiveProductivityUpdates() throws Exception {
        // Given
        String userId = "6001";
        String sessionId = startTimerDirectly(userId, "700", 25);
        BlockingQueue<WebSocketMessage<?>> productivityMessages = new LinkedBlockingQueue<>();
        CountDownLatch messageLatch = new CountDownLatch(1);

        StompSession session = connectWithUserId(userId);

        // Subscribe to productivity updates
        session.subscribe("/user/queue/timer/productivity", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return WebSocketMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                productivityMessages.offer((WebSocketMessage<?>) payload);
                messageLatch.countDown();
            }
        });

        // When - Send productivity data
        Map<String, Object> productivityData = Map.of(
            "sessionId", sessionId,
            "tabSwitches", 3,
            "distractionMinutes", 2,
            "focusBreaks", 1
        );
        session.send("/app/timer/productivity", productivityData);

        // Then
        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Should receive productivity update");

        WebSocketMessage<?> message = productivityMessages.poll();
        assertThat(message).isNotNull();
        assertThat(message.getEvent()).isEqualTo("timer.productivity.updated");
    }

    @Test
    @Timeout(10)
    void shouldHandleMultipleTimersInSameHive() throws Exception {
        // Given
        String user1Id = "7001";
        String user2Id = "7002";
        String user3Id = "7003";
        String hiveId = "800";
        BlockingQueue<WebSocketMessage<?>> hiveTimerMessages = new LinkedBlockingQueue<>();
        CountDownLatch messageLatch = new CountDownLatch(3);

        StompSession observerSession = connectWithUserId(user3Id);

        // Observer subscribes to hive timers
        observerSession.subscribe("/topic/hive/" + hiveId + "/timers", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return WebSocketMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                hiveTimerMessages.offer((WebSocketMessage<?>) payload);
                messageLatch.countDown();
            }
        });

        // When - Multiple users start timers
        StompSession user1Session = connectWithUserId(user1Id);
        StompSession user2Session = connectWithUserId(user2Id);

        user1Session.send("/app/timer/start", Map.of(
            "userId", user1Id,
            "hiveId", hiveId,
            "durationMinutes", 25
        ));

        Thread.sleep(500);

        user2Session.send("/app/timer/start", Map.of(
            "userId", user2Id,
            "hiveId", hiveId,
            "durationMinutes", 30
        ));

        Thread.sleep(500);

        // Request active timers for hive
        observerSession.send("/app/timer/hive/" + hiveId + "/active", null);

        // Then
        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Should receive multiple timer updates");

        assertThat(hiveTimerMessages.size()).isGreaterThanOrEqualTo(3);
    }

    // Helper methods

    private StompSession connectWithUserId(String userId) throws Exception {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("User-ID", userId);

        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {};
        return stompClient.connect(websocketUrl, headers, sessionHandler).get(5, TimeUnit.SECONDS);
    }

    private String startTimerDirectly(String userId, String hiveId, int minutes) {
        StartTimerRequest request = StartTimerRequest.builder()
            .userId(userId)
            .hiveId(hiveId)
            .durationMinutes(minutes)
            .sessionType(FocusSession.SessionType.FOCUS)
            .build();

        FocusSessionResponse response = focusTimerService.startSession(request);
        return response.getId();
    }

    // Custom type for WebSocketMessage<FocusSessionResponse>
    private static class TimerMessageType implements java.lang.reflect.ParameterizedType {
        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{FocusSessionResponse.class};
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
}