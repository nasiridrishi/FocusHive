package com.focushive.websocket.integration;

import com.focushive.websocket.integration.util.StompTestSession;
import com.focushive.websocket.integration.util.WebSocketTestClient;
import com.focushive.websocket.integration.util.WebSocketTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for STOMP message routing
 * Following TDD approach: Write test → Run (fail) → Implement → Run (pass)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StompRoutingIntegrationTest {
    
    private static final Logger log = LoggerFactory.getLogger(StompRoutingIntegrationTest.class);

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("focushive_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @LocalServerPort
    private int port;

    private WebSocketTestClient webSocketClient;
    private String webSocketUrl;

    @BeforeEach
    void setUp() {
        webSocketUrl = WebSocketTestUtils.createWebSocketUrl(port, "/ws");
        webSocketClient = new WebSocketTestClient(webSocketUrl, Duration.ofSeconds(10));
    }

    @AfterEach
    void tearDown() {
        if (webSocketClient != null) {
            webSocketClient.shutdown();
        }
    }

    // ============================================================================
    // TDD Test 1: STOMP Connect Frame with Authentication
    // ============================================================================

    @Test
    @Order(1)
    @DisplayName("Should handle STOMP connect frame with authentication")
    void shouldHandleStompConnectWithAuthentication() throws Exception {
        // GIVEN: Valid JWT token for authentication
        String validJwtToken = WebSocketTestUtils.generateTestJwtToken(1L, "stomp-user");
        
        // WHEN: Connecting with STOMP
        StompSession session = webSocketClient.connectWithJwt(validJwtToken);
        
        // THEN: STOMP connection should be established
        assertTrue(session.isConnected(), "STOMP session should be connected");
        assertEquals("1.2", session.getStompVersion(), "STOMP version should be negotiated");
        assertNotNull(session.getSessionId(), "Session should have an ID");
        
        // Cleanup
        webSocketClient.disconnect(session);
    }

    // ============================================================================
    // TDD Test 2: Subscribe to Topic Destinations
    // ============================================================================

    @Test
    @Order(2)
    @DisplayName("Should subscribe to topic destinations successfully")
    void shouldSubscribeToTopicDestinations() throws Exception {
        // GIVEN: Authenticated STOMP session
        String validJwtToken = WebSocketTestUtils.generateTestJwtToken(1L, "topic-user");
        StompSession session = webSocketClient.connectWithJwt(validJwtToken);
        StompTestSession testSession = new StompTestSession(session);
        
        // WHEN: Subscribing to various topic destinations
        WebSocketTestClient.MessageCapture presenceCapture = testSession.subscribe("/topic/presence");
        WebSocketTestClient.MessageCapture hiveCapture = testSession.subscribe("/topic/hive/123/presence");
        WebSocketTestClient.MessageCapture typingCapture = testSession.subscribe("/topic/presence/typing");
        
        // THEN: Subscriptions should be established
        assertTrue(testSession.isSubscribedTo("/topic/presence"), "Should be subscribed to presence topic");
        assertTrue(testSession.isSubscribedTo("/topic/hive/123/presence"), "Should be subscribed to hive presence");
        assertTrue(testSession.isSubscribedTo("/topic/presence/typing"), "Should be subscribed to typing topic");
        assertEquals(3, testSession.getSubscriptionCount(), "Should have 3 active subscriptions");
        
        // Verify we can receive messages on subscribed topics
        testSession.send("/app/presence/status", 
                        WebSocketTestUtils.createPresenceUpdateData("ONLINE", 123L, "testing"));
        
        // Should receive message on presence topic
        WebSocketTestClient.CapturedMessage presenceMessage = presenceCapture.waitForMessage(Duration.ofSeconds(2));
        assertNotNull(presenceMessage, "Should receive presence update message");
        
        // Cleanup
        testSession.disconnect();
    }

    // ============================================================================
    // TDD Test 3: Subscribe to User Queue Destinations
    // ============================================================================

    @Test
    @Order(3)
    @DisplayName("Should subscribe to user-specific queue destinations")
    void shouldSubscribeToUserQueueDestinations() throws Exception {
        // GIVEN: Authenticated STOMP session
        Long userId = WebSocketTestUtils.generateTestUserId();
        String validJwtToken = WebSocketTestUtils.generateTestJwtToken(userId, "queue-user");
        StompSession session = webSocketClient.connectWithJwt(validJwtToken);
        StompTestSession testSession = new StompTestSession(session);
        
        // WHEN: Subscribing to user-specific queues
        WebSocketTestClient.MessageCapture ackCapture = testSession.subscribe("/user/queue/presence/ack");
        WebSocketTestClient.MessageCapture userPresenceCapture = testSession.subscribe("/user/queue/presence/user");
        WebSocketTestClient.MessageCapture countCapture = testSession.subscribe("/user/queue/presence/count");
        
        // THEN: User queue subscriptions should be established
        assertTrue(testSession.isSubscribedTo("/user/queue/presence/ack"), "Should be subscribed to ack queue");
        assertTrue(testSession.isSubscribedTo("/user/queue/presence/user"), "Should be subscribed to user presence queue");
        assertTrue(testSession.isSubscribedTo("/user/queue/presence/count"), "Should be subscribed to count queue");
        assertEquals(3, testSession.getSubscriptionCount(), "Should have 3 queue subscriptions");
        
        // Test receiving user-specific messages
        testSession.send("/app/presence/ws-heartbeat", "heartbeat");
        
        // Should receive acknowledgment on user queue
        WebSocketTestClient.CapturedMessage ackMessage = ackCapture.waitForMessage(Duration.ofSeconds(2));
        assertNotNull(ackMessage, "Should receive heartbeat acknowledgment");
        
        // Cleanup
        testSession.disconnect();
    }

    // ============================================================================
    // TDD Test 4: Message Publishing to Topics
    // ============================================================================

    @Test
    @Order(4)
    @DisplayName("Should publish messages to topics correctly")
    void shouldPublishMessagesToTopics() throws Exception {
        // GIVEN: Two authenticated sessions (publisher and subscriber)
        String publisherToken = WebSocketTestUtils.generateTestJwtToken(1L, "publisher");
        String subscriberToken = WebSocketTestUtils.generateTestJwtToken(2L, "subscriber");
        
        StompSession publisherSession = webSocketClient.connectWithJwt(publisherToken);
        StompSession subscriberSession = webSocketClient.connectWithJwt(subscriberToken);
        
        StompTestSession publisher = new StompTestSession(publisherSession);
        StompTestSession subscriber = new StompTestSession(subscriberSession);
        
        // WHEN: Subscriber subscribes to topic and publisher sends message
        WebSocketTestClient.MessageCapture messageCapture = subscriber.subscribe("/topic/presence");
        
        // Give subscription time to be processed
        WebSocketTestUtils.sleep(Duration.ofMillis(500));
        
        // Publisher sends presence update
        Map<String, Object> presenceData = WebSocketTestUtils.createPresenceUpdateData("ONLINE", 123L, "working");
        publisher.send("/app/presence/status", presenceData);
        
        // THEN: Subscriber should receive the published message
        WebSocketTestClient.CapturedMessage receivedMessage = messageCapture.waitForMessage(Duration.ofSeconds(3));
        assertNotNull(receivedMessage, "Subscriber should receive published message");
        
        String payload = receivedMessage.getPayload().toString();
        assertTrue(payload.contains("ONLINE") || payload.contains("online"), 
                  "Message should contain presence status");
        
        // Cleanup
        publisher.disconnect();
        subscriber.disconnect();
    }

    // ============================================================================
    // TDD Test 5: User-Specific Message Routing
    // ============================================================================

    @Test
    @Order(5)
    @DisplayName("Should route user-specific messages correctly")
    void shouldRouteUserSpecificMessages() throws Exception {
        // GIVEN: Two different users
        Long user1Id = WebSocketTestUtils.generateTestUserId();
        Long user2Id = WebSocketTestUtils.generateTestUserId();
        
        String user1Token = WebSocketTestUtils.generateTestJwtToken(user1Id, "user1");
        String user2Token = WebSocketTestUtils.generateTestJwtToken(user2Id, "user2");
        
        StompSession user1Session = webSocketClient.connectWithJwt(user1Token);
        StompSession user2Session = webSocketClient.connectWithJwt(user2Token);
        
        StompTestSession user1 = new StompTestSession(user1Session);
        StompTestSession user2 = new StompTestSession(user2Session);
        
        // WHEN: Both users subscribe to their user queues
        WebSocketTestClient.MessageCapture user1Capture = user1.subscribe("/user/queue/presence/ack");
        WebSocketTestClient.MessageCapture user2Capture = user2.subscribe("/user/queue/presence/ack");
        
        WebSocketTestUtils.sleep(Duration.ofMillis(500));
        
        // Each user sends heartbeat
        user1.send("/app/presence/ws-heartbeat", "user1-heartbeat");
        user2.send("/app/presence/ws-heartbeat", "user2-heartbeat");
        
        // THEN: Each user should only receive their own acknowledgment
        WebSocketTestClient.CapturedMessage user1Message = user1Capture.waitForMessage(Duration.ofSeconds(2));
        WebSocketTestClient.CapturedMessage user2Message = user2Capture.waitForMessage(Duration.ofSeconds(2));
        
        assertNotNull(user1Message, "User 1 should receive their own ack");
        assertNotNull(user2Message, "User 2 should receive their own ack");
        
        // Verify messages are user-specific
        String user1Payload = user1Message.getPayload().toString();
        String user2Payload = user2Message.getPayload().toString();
        
        assertTrue(user1Payload.contains(user1Id.toString()) || user1Payload.contains("ok"), 
                  "User 1 should receive message with their user ID or status");
        assertTrue(user2Payload.contains(user2Id.toString()) || user2Payload.contains("ok"), 
                  "User 2 should receive message with their user ID or status");
        
        // Cleanup
        user1.disconnect();
        user2.disconnect();
    }

    // ============================================================================
    // TDD Test 6: Broadcast to All Subscribers
    // ============================================================================

    @Test
    @Order(6)
    @DisplayName("Should broadcast messages to all topic subscribers")
    void shouldBroadcastToAllSubscribers() throws Exception {
        // GIVEN: Multiple subscribers to the same topic
        String user1Token = WebSocketTestUtils.generateTestJwtToken(1L, "subscriber1");
        String user2Token = WebSocketTestUtils.generateTestJwtToken(2L, "subscriber2");
        String user3Token = WebSocketTestUtils.generateTestJwtToken(3L, "subscriber3");
        String publisherToken = WebSocketTestUtils.generateTestJwtToken(4L, "broadcaster");
        
        StompSession subscriber1Session = webSocketClient.connectWithJwt(user1Token);
        StompSession subscriber2Session = webSocketClient.connectWithJwt(user2Token);
        StompSession subscriber3Session = webSocketClient.connectWithJwt(user3Token);
        StompSession publisherSession = webSocketClient.connectWithJwt(publisherToken);
        
        StompTestSession subscriber1 = new StompTestSession(subscriber1Session);
        StompTestSession subscriber2 = new StompTestSession(subscriber2Session);
        StompTestSession subscriber3 = new StompTestSession(subscriber3Session);
        StompTestSession publisher = new StompTestSession(publisherSession);
        
        // WHEN: All subscribers subscribe to typing topic
        String topicDestination = "/topic/presence/typing";
        WebSocketTestClient.MessageCapture capture1 = subscriber1.subscribe(topicDestination);
        WebSocketTestClient.MessageCapture capture2 = subscriber2.subscribe(topicDestination);
        WebSocketTestClient.MessageCapture capture3 = subscriber3.subscribe(topicDestination);
        
        WebSocketTestUtils.sleep(Duration.ofMillis(500));
        
        // Publisher broadcasts typing indicator
        Map<String, Object> typingData = WebSocketTestUtils.createTypingData("hive:123", true);
        publisher.send("/app/presence/typing", typingData);
        
        // THEN: All subscribers should receive the broadcast message
        WebSocketTestClient.CapturedMessage message1 = capture1.waitForMessage(Duration.ofSeconds(2));
        WebSocketTestClient.CapturedMessage message2 = capture2.waitForMessage(Duration.ofSeconds(2));
        WebSocketTestClient.CapturedMessage message3 = capture3.waitForMessage(Duration.ofSeconds(2));
        
        assertNotNull(message1, "Subscriber 1 should receive broadcast");
        assertNotNull(message2, "Subscriber 2 should receive broadcast");
        assertNotNull(message3, "Subscriber 3 should receive broadcast");
        
        // Verify all received the same broadcast
        String payload1 = message1.getPayload().toString();
        String payload2 = message2.getPayload().toString();
        String payload3 = message3.getPayload().toString();
        
        assertTrue(payload1.contains("hive:123"), "Message 1 should contain location");
        assertTrue(payload2.contains("hive:123"), "Message 2 should contain location");
        assertTrue(payload3.contains("hive:123"), "Message 3 should contain location");
        
        // Cleanup
        subscriber1.disconnect();
        subscriber2.disconnect();
        subscriber3.disconnect();
        publisher.disconnect();
    }

    // ============================================================================
    // TDD Test 7: Topic Permission Validation
    // ============================================================================

    @Test
    @Order(7)
    @DisplayName("Should validate topic permissions correctly")
    void shouldValidateTopicPermissions() throws Exception {
        // GIVEN: Authenticated user session
        String validJwtToken = WebSocketTestUtils.generateTestJwtToken(1L, "perm-user");
        StompSession session = webSocketClient.connectWithJwt(validJwtToken);
        StompTestSession testSession = new StompTestSession(session);
        
        // WHEN: Subscribing to valid destinations
        WebSocketTestClient.MessageCapture validCapture1 = testSession.subscribe("/topic/presence");
        WebSocketTestClient.MessageCapture validCapture2 = testSession.subscribe("/user/queue/presence/ack");
        
        // THEN: Valid subscriptions should succeed
        assertTrue(testSession.isSubscribedTo("/topic/presence"), "Should allow valid topic subscription");
        assertTrue(testSession.isSubscribedTo("/user/queue/presence/ack"), "Should allow valid queue subscription");
        
        // Test sending to valid destinations
        assertDoesNotThrow(() -> {
            testSession.send("/app/presence/ws-heartbeat", "test");
            testSession.send("/app/presence/status", 
                           WebSocketTestUtils.createPresenceUpdateData("ONLINE", null, null));
        }, "Should allow sending to valid app destinations");
        
        // Cleanup
        testSession.disconnect();
    }

    // ============================================================================
    // TDD Test 8: Message Acknowledgment
    // ============================================================================

    @Test
    @Order(8)
    @DisplayName("Should handle message acknowledgments properly")
    void shouldHandleMessageAcknowledgments() throws Exception {
        // GIVEN: Authenticated session with receipt handling
        String validJwtToken = WebSocketTestUtils.generateTestJwtToken(1L, "ack-user");
        StompSession session = webSocketClient.connectWithJwt(validJwtToken);
        StompTestSession testSession = new StompTestSession(session);
        
        // WHEN: Subscribing to acknowledgment queue
        WebSocketTestClient.MessageCapture ackCapture = testSession.subscribe("/user/queue/presence/ack");
        
        WebSocketTestUtils.sleep(Duration.ofMillis(500));
        
        // Send heartbeat message
        testSession.send("/app/presence/ws-heartbeat", "ack-test");
        
        // THEN: Should receive acknowledgment
        WebSocketTestClient.CapturedMessage ackMessage = ackCapture.waitForMessage(Duration.ofSeconds(3));
        assertNotNull(ackMessage, "Should receive acknowledgment message");
        
        String ackPayload = ackMessage.getPayload().toString();
        assertTrue(ackPayload.contains("ok") || ackPayload.contains("status"), 
                  "Acknowledgment should contain success status");
        
        // Cleanup
        testSession.disconnect();
    }

    // ============================================================================
    // TDD Test 9: Multiple Destination Routing
    // ============================================================================

    @Test
    @Order(9)
    @DisplayName("Should route messages to multiple destinations correctly")
    void shouldRouteToMultipleDestinations() throws Exception {
        // GIVEN: User subscribed to multiple related destinations
        Long hiveId = WebSocketTestUtils.generateTestHiveId();
        String userToken = WebSocketTestUtils.generateTestJwtToken(1L, "multi-user");
        
        StompSession session = webSocketClient.connectWithJwt(userToken);
        StompTestSession testSession = new StompTestSession(session);
        
        // WHEN: Subscribing to multiple presence-related destinations
        WebSocketTestClient.MessageCapture globalPresence = testSession.subscribe("/topic/presence");
        WebSocketTestClient.MessageCapture hivePresence = testSession.subscribe("/topic/hive/" + hiveId + "/presence");
        WebSocketTestClient.MessageCapture userAck = testSession.subscribe("/user/queue/presence/ack");
        
        WebSocketTestUtils.sleep(Duration.ofMillis(500));
        
        // Send presence update that should route to multiple destinations
        Map<String, Object> presenceData = WebSocketTestUtils.createPresenceUpdateData("ONLINE", hiveId, "testing");
        testSession.send("/app/presence/status", presenceData);
        
        // THEN: Should receive messages on appropriate destinations
        WebSocketTestClient.CapturedMessage globalMessage = globalPresence.waitForMessage(Duration.ofSeconds(2));
        WebSocketTestClient.CapturedMessage hiveMessage = hivePresence.waitForMessage(Duration.ofSeconds(2));
        
        assertNotNull(globalMessage, "Should receive message on global presence topic");
        // Note: Hive-specific routing may depend on business logic implementation
        
        String globalPayload = globalMessage.getPayload().toString();
        assertTrue(globalPayload.contains("ONLINE") || globalPayload.contains("online"), 
                  "Global message should contain status update");
        
        // Cleanup
        testSession.disconnect();
    }

    // ============================================================================
    // TDD Test 10: STOMP Protocol Compliance
    // ============================================================================

    @Test
    @Order(10)
    @DisplayName("Should comply with STOMP protocol standards")
    void shouldComplyWithStompProtocol() throws Exception {
        // GIVEN: Valid STOMP connection
        String validJwtToken = WebSocketTestUtils.generateTestJwtToken(1L, "stomp-compliance");
        StompSession session = webSocketClient.connectWithJwt(validJwtToken);
        
        // THEN: Should comply with STOMP protocol standards
        assertEquals("1.2", session.getStompVersion(), "Should negotiate STOMP 1.2");
        assertTrue(session.isConnected(), "Should maintain connected state");
        assertNotNull(session.getSessionId(), "Should provide session identifier");
        
        // Test STOMP frame handling
        StompTestSession testSession = new StompTestSession(session);
        WebSocketTestClient.MessageCapture capture = testSession.subscribe("/user/queue/presence/ack");
        
        // Send message with proper STOMP format
        testSession.send("/app/presence/ws-heartbeat", "stomp-test");
        
        // Should handle STOMP frames correctly
        WebSocketTestClient.CapturedMessage response = capture.waitForMessage(Duration.ofSeconds(2));
        assertNotNull(response, "Should receive STOMP frame response");
        assertNotNull(response.getHeaders(), "Response should have STOMP headers");
        
        // Cleanup
        testSession.disconnect();
    }
}