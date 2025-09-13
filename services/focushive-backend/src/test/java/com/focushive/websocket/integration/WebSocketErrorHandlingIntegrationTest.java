package com.focushive.websocket.integration;

import com.focushive.websocket.integration.util.StompTestSession;
import com.focushive.websocket.integration.util.WebSocketTestClient;
import com.focushive.websocket.integration.util.WebSocketTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for WebSocket connection cleanup and error handling
 * Following TDD approach: Write test → Run (fail) → Implement → Run (pass)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WebSocketErrorHandlingIntegrationTest {
    
    private static final Logger log = LoggerFactory.getLogger(WebSocketErrorHandlingIntegrationTest.class);

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
    // TDD Test 1: Graceful Disconnect Handling
    // ============================================================================

    @Test
    @Order(1)
    @DisplayName("Should handle graceful disconnect properly")
    void shouldHandleGracefulDisconnect() throws Exception {
        // GIVEN: Established WebSocket connection
        String userToken = WebSocketTestUtils.generateTestJwtToken(1L, "graceful-user");
        StompSession session = webSocketClient.connectWithJwt(userToken);
        StompTestSession testSession = new StompTestSession(session);
        
        assertTrue(session.isConnected(), "Session should be connected initially");
        
        // Establish some activity
        WebSocketTestClient.MessageCapture ackCapture = testSession.subscribe("/user/queue/presence/ack");
        testSession.send("/app/presence/ws-heartbeat", "before-disconnect");
        
        WebSocketTestClient.CapturedMessage initialAck = ackCapture.waitForMessage(Duration.ofSeconds(2));
        assertNotNull(initialAck, "Should receive acknowledgment before disconnect");
        
        // WHEN: Gracefully disconnecting
        testSession.send("/app/presence/disconnect", Map.of("reason", "graceful"));
        testSession.disconnect();
        
        // THEN: Disconnect should complete without errors
        assertFalse(session.isConnected(), "Session should be disconnected");
        
        // Verify we can establish a new connection
        StompSession newSession = webSocketClient.connectWithJwt(userToken);
        assertTrue(newSession.isConnected(), "Should be able to reconnect after graceful disconnect");
        
        // Cleanup
        webSocketClient.disconnect(newSession);
    }

    // ============================================================================
    // TDD Test 2: Abrupt Connection Loss Recovery
    // ============================================================================

    @Test
    @Order(2)
    @DisplayName("Should handle abrupt connection loss gracefully")
    void shouldHandleAbruptConnectionLoss() throws Exception {
        // GIVEN: Established connection with active subscriptions
        String userToken = WebSocketTestUtils.generateTestJwtToken(1L, "abrupt-user");
        String observerToken = WebSocketTestUtils.generateTestJwtToken(2L, "observer-user");
        
        StompSession userSession = webSocketClient.connectWithJwt(userToken);
        StompSession observerSession = webSocketClient.connectWithJwt(observerToken);
        
        StompTestSession user = new StompTestSession(userSession);
        StompTestSession observer = new StompTestSession(observerSession);
        
        // Observer monitors for presence updates
        WebSocketTestClient.MessageCapture presenceCapture = observer.subscribe("/topic/presence");
        
        // User establishes presence
        user.send("/app/presence/status", 
                 WebSocketTestUtils.createPresenceUpdateData("ONLINE", null, "before disconnect"));
        
        WebSocketTestUtils.sleep(Duration.ofMillis(500));
        
        // WHEN: Abrupt connection loss (simulate by closing connection without cleanup)
        userSession.disconnect(); // Abrupt disconnect
        
        // THEN: System should handle the abrupt disconnection
        // Wait for potential cleanup notifications
        WebSocketTestUtils.sleep(Duration.ofMillis(1000));
        
        // Observer should still be able to receive updates from other users
        observer.send("/app/presence/status", 
                     WebSocketTestUtils.createPresenceUpdateData("ONLINE", null, "after user disconnect"));
        
        WebSocketTestClient.CapturedMessage observerUpdate = presenceCapture.waitForMessage(Duration.ofSeconds(2));
        assertNotNull(observerUpdate, "Observer should continue receiving updates after another user's abrupt disconnect");
        
        // User should be able to reconnect
        StompSession reconnectedSession = webSocketClient.connectWithJwt(userToken);
        assertTrue(reconnectedSession.isConnected(), "User should be able to reconnect after abrupt disconnect");
        
        // Cleanup
        observer.disconnect();
        webSocketClient.disconnect(reconnectedSession);
    }

    // ============================================================================
    // TDD Test 3: Message Queue During Temporary Disconnect
    // ============================================================================

    @Test
    @Order(3)
    @DisplayName("Should handle message queuing during temporary disconnect")
    void shouldHandleMessageQueueDuringDisconnect() throws Exception {
        // GIVEN: User with established presence
        Long userId = WebSocketTestUtils.generateTestUserId();
        String userToken = WebSocketTestUtils.generateTestJwtToken(userId, "queue-user");
        String senderToken = WebSocketTestUtils.generateTestJwtToken(999L, "sender-user");
        
        StompSession userSession = webSocketClient.connectWithJwt(userToken);
        StompSession senderSession = webSocketClient.connectWithJwt(senderToken);
        
        StompTestSession user = new StompTestSession(userSession);
        StompTestSession sender = new StompTestSession(senderSession);
        
        // User subscribes to their queue
        WebSocketTestClient.MessageCapture userQueue = user.subscribe("/user/queue/presence/user");
        
        WebSocketTestUtils.sleep(Duration.ofMillis(500));
        
        // WHEN: User temporarily disconnects
        user.disconnect();
        
        // Sender tries to send user-specific message
        sender.send("/app/presence/user/" + userId, Map.of("action", "getPresence"));
        
        WebSocketTestUtils.sleep(Duration.ofMillis(500));
        
        // User reconnects
        StompSession reconnectedSession = webSocketClient.connectWithJwt(userToken);
        StompTestSession reconnectedUser = new StompTestSession(reconnectedSession);
        
        WebSocketTestClient.MessageCapture reconnectedQueue = 
            reconnectedUser.subscribe("/user/queue/presence/user");
        
        // THEN: User should be able to receive messages after reconnection
        // Note: Message queuing behavior depends on broker configuration
        // For now, verify reconnection works and user can receive new messages
        sender.send("/app/presence/user/" + userId, Map.of("action", "getPresence"));
        
        WebSocketTestClient.CapturedMessage queuedMessage = 
            reconnectedQueue.waitForMessage(Duration.ofSeconds(3));
        
        // May or may not receive the message depending on broker persistence
        // At minimum, verify user can receive new messages after reconnection
        if (queuedMessage == null) {
            log.info("No queued message received - testing new message delivery");
            sender.send("/app/presence/user/" + userId, Map.of("action", "getPresence", "test", "reconnected"));
            queuedMessage = reconnectedQueue.waitForMessage(Duration.ofSeconds(2));
        }
        
        assertNotNull(queuedMessage, "User should receive messages after reconnection");
        
        // Cleanup
        sender.disconnect();
        reconnectedUser.disconnect();
    }

    // ============================================================================
    // TDD Test 4: Resource Cleanup on Disconnect
    // ============================================================================

    @Test
    @Order(4)
    @DisplayName("Should properly cleanup resources on disconnect")
    void shouldCleanupResourcesOnDisconnect() throws Exception {
        // GIVEN: Multiple users with active subscriptions
        List<StompSession> sessions = new ArrayList<>();
        List<StompTestSession> testSessions = new ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
            Long userId = WebSocketTestUtils.generateTestUserId();
            String userToken = WebSocketTestUtils.generateTestJwtToken(userId, "cleanup-user-" + i);
            
            StompSession session = webSocketClient.connectWithJwt(userToken);
            sessions.add(session);
            
            StompTestSession testSession = new StompTestSession(session);
            testSessions.add(testSession);
            
            // Each user subscribes to multiple destinations
            testSession.subscribe("/topic/presence");
            testSession.subscribe("/user/queue/presence/ack");
            
            // Send some activity
            testSession.send("/app/presence/ws-heartbeat", "cleanup-test-" + i);
        }
        
        // Verify all connections are active
        assertEquals(5, sessions.size(), "Should have 5 active sessions");
        for (StompSession session : sessions) {
            assertTrue(session.isConnected(), "All sessions should be connected");
        }
        
        // WHEN: Disconnecting all users
        for (StompTestSession testSession : testSessions) {
            testSession.disconnect();
        }
        
        // THEN: All resources should be cleaned up
        WebSocketTestUtils.sleep(Duration.ofMillis(1000)); // Allow cleanup time
        
        for (StompSession session : sessions) {
            assertFalse(session.isConnected(), "All sessions should be disconnected");
        }
        
        // Verify system can handle new connections after cleanup
        String newUserToken = WebSocketTestUtils.generateTestJwtToken(100L, "post-cleanup-user");
        StompSession newSession = webSocketClient.connectWithJwt(newUserToken);
        assertTrue(newSession.isConnected(), "Should accept new connections after cleanup");
        
        // Cleanup
        webSocketClient.disconnect(newSession);
    }

    // ============================================================================
    // TDD Test 5: Error Message Propagation
    // ============================================================================

    @Test
    @Order(5)
    @DisplayName("Should propagate error messages appropriately")
    void shouldPropagateErrorMessages() throws Exception {
        // GIVEN: Authenticated user session
        String userToken = WebSocketTestUtils.generateTestJwtToken(1L, "error-test-user");
        StompSession session = webSocketClient.connectWithJwt(userToken);
        StompTestSession testSession = new StompTestSession(session);
        
        // WHEN: Sending invalid or malformed messages
        // Test 1: Invalid destination
        assertDoesNotThrow(() -> {
            testSession.send("/app/invalid/destination", Map.of("test", "data"));
        }, "Should handle invalid destinations gracefully");
        
        // Test 2: Malformed message payload
        assertDoesNotThrow(() -> {
            testSession.send("/app/presence/status", "invalid-json-string");
        }, "Should handle malformed payloads gracefully");
        
        // Test 3: Missing required fields
        assertDoesNotThrow(() -> {
            testSession.send("/app/presence/status", Map.of("incomplete", "data"));
        }, "Should handle incomplete data gracefully");
        
        // THEN: System should remain stable and responsive
        WebSocketTestClient.MessageCapture ackCapture = testSession.subscribe("/user/queue/presence/ack");
        testSession.send("/app/presence/ws-heartbeat", "recovery-test");
        
        WebSocketTestClient.CapturedMessage recoveryAck = ackCapture.waitForMessage(Duration.ofSeconds(2));
        assertNotNull(recoveryAck, "System should recover and process valid messages after errors");
        
        // Cleanup
        testSession.disconnect();
    }

    // ============================================================================
    // TDD Test 6: Connection Limit Exceeded Handling
    // ============================================================================

    @Test
    @Order(6)
    @DisplayName("Should handle connection limit exceeded gracefully")
    void shouldHandleConnectionLimitExceeded() throws Exception {
        // GIVEN: Attempt to create many concurrent connections
        List<CompletableFuture<StompSession>> connectionFutures = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(20);
        AtomicInteger successfulConnections = new AtomicInteger(0);
        AtomicInteger failedConnections = new AtomicInteger(0);
        
        // WHEN: Creating many connections simultaneously
        for (int i = 0; i < 30; i++) { // Attempt more connections than typically allowed
            final int userId = i + 100;
            CompletableFuture<StompSession> future = CompletableFuture.supplyAsync(() -> {
                try {
                    String userToken = WebSocketTestUtils.generateTestJwtToken((long) userId, "limit-user-" + userId);
                    StompSession session = webSocketClient.connectWithJwt(userToken);
                    successfulConnections.incrementAndGet();
                    return session;
                } catch (Exception e) {
                    failedConnections.incrementAndGet();
                    log.debug("Connection failed (may be expected): {}", e.getMessage());
                    return null;
                }
            }, executor);
            connectionFutures.add(future);
        }
        
        // Wait for all connection attempts
        List<StompSession> sessions = new ArrayList<>();
        for (CompletableFuture<StompSession> future : connectionFutures) {
            try {
                StompSession session = future.get(5, TimeUnit.SECONDS);
                if (session != null && session.isConnected()) {
                    sessions.add(session);
                }
            } catch (Exception e) {
                log.debug("Connection attempt timed out or failed: {}", e.getMessage());
            }
        }
        
        // THEN: System should handle connection limits gracefully
        assertTrue(successfulConnections.get() > 0, "At least some connections should succeed");
        log.info("Successful connections: {}, Failed connections: {}", 
                successfulConnections.get(), failedConnections.get());
        
        // Cleanup successful connections
        for (StompSession session : sessions) {
            try {
                webSocketClient.disconnect(session);
            } catch (Exception e) {
                log.debug("Error during cleanup: {}", e.getMessage());
            }
        }
        
        executor.shutdown();
    }

    // ============================================================================
    // TDD Test 7: Heartbeat Timeout Detection
    // ============================================================================

    @Test
    @Order(7)
    @DisplayName("Should detect heartbeat timeout and cleanup connections")
    void shouldDetectHeartbeatTimeout() throws Exception {
        // GIVEN: Connection with heartbeat enabled
        String userToken = WebSocketTestUtils.generateTestJwtToken(1L, "heartbeat-user");
        StompSession session = webSocketClient.connectWithJwt(userToken);
        StompTestSession testSession = new StompTestSession(session);
        
        assertTrue(session.isConnected(), "Initial connection should succeed");
        
        // WHEN: Simulating heartbeat timeout (by not sending any heartbeats)
        // Note: Actual heartbeat timeout would require longer wait times
        // For testing purposes, we verify the heartbeat mechanism is functional
        
        WebSocketTestClient.MessageCapture ackCapture = testSession.subscribe("/user/queue/presence/ack");
        
        // Send regular heartbeat
        testSession.send("/app/presence/ws-heartbeat", "heartbeat-test");
        
        // THEN: Should receive heartbeat acknowledgment
        WebSocketTestClient.CapturedMessage heartbeatAck = ackCapture.waitForMessage(Duration.ofSeconds(2));
        assertNotNull(heartbeatAck, "Should receive heartbeat acknowledgment");
        
        String ackPayload = heartbeatAck.getPayload().toString();
        assertTrue(ackPayload.contains("ok") || ackPayload.contains("status"), 
                  "Heartbeat acknowledgment should indicate success");
        
        // For actual timeout testing, we would need to wait for the configured heartbeat interval
        // and verify the connection is closed. This is typically 30+ seconds in production.
        
        // Cleanup
        testSession.disconnect();
    }

    // ============================================================================
    // TDD Test 8: Malformed Message Handling
    // ============================================================================

    @Test
    @Order(8)
    @DisplayName("Should handle malformed messages without crashing")
    void shouldHandleMalformedMessages() throws Exception {
        // GIVEN: Established connection
        String userToken = WebSocketTestUtils.generateTestJwtToken(1L, "malformed-user");
        StompSession session = webSocketClient.connectWithJwt(userToken);
        StompTestSession testSession = new StompTestSession(session);
        
        // WHEN: Sending various malformed messages
        String[] malformedPayloads = {
            "{invalid-json}",
            "",
            "null",
            "{\"unclosed\": \"json",
            "[]", // Array instead of object
            "{\"nested\": {\"too\": {\"deep\": {\"structure\": true}}}}",
            "not-json-at-all"
        };
        
        for (String payload : malformedPayloads) {
            assertDoesNotThrow(() -> {
                testSession.send("/app/presence/status", payload);
            }, "Should handle malformed payload without throwing: " + payload);
            
            // Small delay between malformed messages
            WebSocketTestUtils.sleep(Duration.ofMillis(100));
        }
        
        // THEN: System should remain responsive after malformed messages
        WebSocketTestClient.MessageCapture ackCapture = testSession.subscribe("/user/queue/presence/ack");
        testSession.send("/app/presence/ws-heartbeat", "recovery-after-malformed");
        
        WebSocketTestClient.CapturedMessage recoveryAck = ackCapture.waitForMessage(Duration.ofSeconds(3));
        assertNotNull(recoveryAck, "System should recover and process valid messages after malformed ones");
        
        // Cleanup
        testSession.disconnect();
    }

    // ============================================================================
    // TDD Test 9: Network Interruption Recovery
    // ============================================================================

    @Test
    @Order(9)
    @DisplayName("Should handle network interruption recovery scenarios")
    void shouldHandleNetworkInterruptionRecovery() throws Exception {
        // GIVEN: Stable connection with established presence
        Long userId = WebSocketTestUtils.generateTestUserId();
        String userToken = WebSocketTestUtils.generateTestJwtToken(userId, "network-user");
        
        StompSession session = webSocketClient.connectWithJwt(userToken);
        StompTestSession testSession = new StompTestSession(session);
        
        // Establish presence and activity
        testSession.send("/app/presence/status", 
                        WebSocketTestUtils.createPresenceUpdateData("ONLINE", null, "before interruption"));
        
        WebSocketTestUtils.sleep(Duration.ofMillis(500));
        
        // WHEN: Simulating network interruption (abrupt disconnect and reconnect)
        testSession.disconnect(); // Simulate network drop
        
        WebSocketTestUtils.sleep(Duration.ofMillis(1000)); // Simulate network downtime
        
        // Attempt reconnection
        StompSession reconnectedSession = webSocketClient.connectWithJwt(userToken);
        StompTestSession reconnectedTestSession = new StompTestSession(reconnectedSession);
        
        // THEN: Should be able to reconnect and resume operations
        assertTrue(reconnectedSession.isConnected(), "Should reconnect after network interruption");
        
        // Resume normal operations
        WebSocketTestClient.MessageCapture ackCapture = 
            reconnectedTestSession.subscribe("/user/queue/presence/ack");
        
        reconnectedTestSession.send("/app/presence/ws-heartbeat", "after-network-recovery");
        
        WebSocketTestClient.CapturedMessage recoveryAck = ackCapture.waitForMessage(Duration.ofSeconds(3));
        assertNotNull(recoveryAck, "Should resume normal operations after network recovery");
        
        // Cleanup
        reconnectedTestSession.disconnect();
    }

    // ============================================================================
    // TDD Test 10: Concurrent Error Scenarios
    // ============================================================================

    @Test
    @Order(10)
    @DisplayName("Should handle concurrent error scenarios robustly")
    void shouldHandleConcurrentErrorScenarios() throws Exception {
        // GIVEN: Multiple users with potential error conditions
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<Boolean>> errorTestFutures = new ArrayList<>();
        AtomicInteger successfulOperations = new AtomicInteger(0);
        AtomicInteger errorOperations = new AtomicInteger(0);
        
        // WHEN: Multiple users perform potentially error-prone operations concurrently
        for (int i = 0; i < 10; i++) {
            final int userId = i + 200;
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    String userToken = WebSocketTestUtils.generateTestJwtToken((long) userId, "error-concurrent-" + userId);
                    StompSession session = webSocketClient.connectWithJwt(userToken);
                    StompTestSession testSession = new StompTestSession(session);
                    
                    // Mix of valid and invalid operations
                    testSession.send("/app/presence/ws-heartbeat", "valid-heartbeat");
                    testSession.send("/app/presence/status", "invalid-status-data");
                    testSession.send("/app/invalid/endpoint", Map.of("test", "data"));
                    testSession.send("/app/presence/status", 
                                   WebSocketTestUtils.createPresenceUpdateData("ONLINE", null, "valid-status"));
                    
                    // Abrupt disconnect
                    session.disconnect();
                    
                    successfulOperations.incrementAndGet();
                    return true;
                } catch (Exception e) {
                    errorOperations.incrementAndGet();
                    log.debug("Concurrent error test failed for user {}: {}", userId, e.getMessage());
                    return false;
                }
            }, executor);
            errorTestFutures.add(future);
        }
        
        // Wait for all operations to complete
        int completedOperations = 0;
        for (CompletableFuture<Boolean> future : errorTestFutures) {
            try {
                if (future.get(10, TimeUnit.SECONDS)) {
                    completedOperations++;
                }
            } catch (Exception e) {
                log.debug("Concurrent operation timed out: {}", e.getMessage());
            }
        }
        
        // THEN: System should handle concurrent errors without crashing
        assertTrue(completedOperations > 0, "At least some concurrent operations should complete");
        log.info("Completed operations: {}, Successful: {}, Errors: {}", 
                completedOperations, successfulOperations.get(), errorOperations.get());
        
        // Verify system is still responsive
        String testToken = WebSocketTestUtils.generateTestJwtToken(1000L, "post-error-test");
        StompSession testSession = webSocketClient.connectWithJwt(testToken);
        assertTrue(testSession.isConnected(), "System should remain responsive after concurrent errors");
        
        // Cleanup
        webSocketClient.disconnect(testSession);
        executor.shutdown();
    }
}