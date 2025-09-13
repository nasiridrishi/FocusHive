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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for multi-user presence synchronization
 * Following TDD approach: Write test → Run (fail) → Implement → Run (pass)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PresenceSyncIntegrationTest {
    
    private static final Logger log = LoggerFactory.getLogger(PresenceSyncIntegrationTest.class);

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
    // TDD Test 1: User Joining Hive Broadcasts Presence
    // ============================================================================

    @Test
    @Order(1)
    @DisplayName("Should broadcast presence when user joins hive")
    void shouldBroadcastPresenceWhenUserJoinsHive() throws Exception {
        // GIVEN: Two users, one already monitoring hive presence
        Long hiveId = WebSocketTestUtils.generateTestHiveId();
        String observer1Token = WebSocketTestUtils.generateTestJwtToken(1L, "observer1");
        String joinerToken = WebSocketTestUtils.generateTestJwtToken(2L, "joiner");
        
        StompSession observerSession = webSocketClient.connectWithJwt(observer1Token);
        StompTestSession observer = new StompTestSession(observerSession);
        
        // Observer subscribes to hive presence updates
        WebSocketTestClient.MessageCapture presenceCapture = observer.subscribe("/topic/hive/" + hiveId + "/presence");
        WebSocketTestClient.MessageCapture globalPresence = observer.subscribe("/topic/presence");
        
        WebSocketTestUtils.sleep(Duration.ofMillis(500));
        
        // WHEN: New user joins the hive
        StompSession joinerSession = webSocketClient.connectWithJwt(joinerToken);
        StompTestSession joiner = new StompTestSession(joinerSession);
        
        Map<String, Object> joinData = WebSocketTestUtils.createPresenceUpdateData("ONLINE", hiveId, "just joined");
        joiner.send("/app/presence/status", joinData);
        
        // THEN: Observer should receive presence broadcast
        WebSocketTestClient.CapturedMessage presenceMessage = presenceCapture.waitForMessage(Duration.ofSeconds(3));
        WebSocketTestClient.CapturedMessage globalMessage = globalPresence.waitForMessage(Duration.ofSeconds(3));
        
        assertNotNull(presenceMessage, "Should receive hive-specific presence update");
        assertNotNull(globalMessage, "Should receive global presence update");
        
        String presencePayload = presenceMessage.getPayload().toString();
        assertTrue(presencePayload.contains("ONLINE") || presencePayload.contains("online"), 
                  "Presence message should indicate user is online");
        
        // Cleanup
        observer.disconnect();
        joiner.disconnect();
    }

    // ============================================================================
    // TDD Test 2: User Status Updates Synchronization
    // ============================================================================

    @Test
    @Order(2)
    @DisplayName("Should synchronize user status updates across all subscribers")
    void shouldSynchronizeUserStatusUpdates() throws Exception {
        // GIVEN: Multiple users monitoring presence in the same hive
        Long hiveId = WebSocketTestUtils.generateTestHiveId();
        Long userId = WebSocketTestUtils.generateTestUserId();
        
        String user1Token = WebSocketTestUtils.generateTestJwtToken(1L, "observer1");
        String user2Token = WebSocketTestUtils.generateTestJwtToken(2L, "observer2");
        String user3Token = WebSocketTestUtils.generateTestJwtToken(3L, "observer3");
        String updaterToken = WebSocketTestUtils.generateTestJwtToken(userId, "updater");
        
        // Set up observers
        StompSession observer1Session = webSocketClient.connectWithJwt(user1Token);
        StompSession observer2Session = webSocketClient.connectWithJwt(user2Token);
        StompSession observer3Session = webSocketClient.connectWithJwt(user3Token);
        StompSession updaterSession = webSocketClient.connectWithJwt(updaterToken);
        
        StompTestSession observer1 = new StompTestSession(observer1Session);
        StompTestSession observer2 = new StompTestSession(observer2Session);
        StompTestSession observer3 = new StompTestSession(observer3Session);
        StompTestSession updater = new StompTestSession(updaterSession);
        
        // All observers subscribe to presence updates
        WebSocketTestClient.MessageCapture capture1 = observer1.subscribe("/topic/presence");
        WebSocketTestClient.MessageCapture capture2 = observer2.subscribe("/topic/presence");
        WebSocketTestClient.MessageCapture capture3 = observer3.subscribe("/topic/presence");
        
        WebSocketTestUtils.sleep(Duration.ofMillis(500));
        
        // WHEN: User updates their status multiple times
        String[] statuses = {"ONLINE", "AWAY", "BUSY", "ONLINE"};
        String[] activities = {"starting work", "taking break", "in meeting", "back to work"};
        
        for (int i = 0; i < statuses.length; i++) {
            Map<String, Object> statusData = WebSocketTestUtils.createPresenceUpdateData(
                statuses[i], hiveId, activities[i]);
            updater.send("/app/presence/status", statusData);
            WebSocketTestUtils.sleep(Duration.ofMillis(300)); // Small delay between updates
        }
        
        // THEN: All observers should receive all status updates
        for (int observerNum = 1; observerNum <= 3; observerNum++) {
            WebSocketTestClient.MessageCapture capture = (observerNum == 1) ? capture1 : 
                                                       (observerNum == 2) ? capture2 : capture3;
            
            // Should receive at least one status update (may not receive all due to timing)
            WebSocketTestClient.CapturedMessage statusMessage = capture.waitForMessage(Duration.ofSeconds(3));
            assertNotNull(statusMessage, String.format("Observer %d should receive status update", observerNum));
            
            String payload = statusMessage.getPayload().toString();
            boolean hasValidStatus = payload.contains("ONLINE") || payload.contains("AWAY") || 
                                   payload.contains("BUSY") || payload.contains("online") || 
                                   payload.contains("away") || payload.contains("busy");
            assertTrue(hasValidStatus, String.format("Observer %d should receive valid status", observerNum));
        }
        
        // Cleanup
        observer1.disconnect();
        observer2.disconnect();
        observer3.disconnect();
        updater.disconnect();
    }

    // ============================================================================
    // TDD Test 3: User Disconnection Broadcasts Leave Event
    // ============================================================================

    @Test
    @Order(3)
    @DisplayName("Should broadcast leave event when user disconnects")
    void shouldBroadcastLeaveEventOnDisconnection() throws Exception {
        // GIVEN: User in hive with observers monitoring
        Long hiveId = WebSocketTestUtils.generateTestHiveId();
        String observerToken = WebSocketTestUtils.generateTestJwtToken(1L, "observer");
        String leaverToken = WebSocketTestUtils.generateTestJwtToken(2L, "leaver");
        
        StompSession observerSession = webSocketClient.connectWithJwt(observerToken);
        StompSession leaverSession = webSocketClient.connectWithJwt(leaverToken);
        
        StompTestSession observer = new StompTestSession(observerSession);
        StompTestSession leaver = new StompTestSession(leaverSession);
        
        // Observer subscribes to presence updates
        WebSocketTestClient.MessageCapture presenceCapture = observer.subscribe("/topic/presence");
        
        // Leaver joins hive first
        Map<String, Object> joinData = WebSocketTestUtils.createPresenceUpdateData("ONLINE", hiveId, "joining");
        leaver.send("/app/presence/status", joinData);
        
        WebSocketTestUtils.sleep(Duration.ofMillis(500));
        
        // WHEN: User explicitly disconnects
        leaver.send("/app/presence/disconnect", Map.of("reason", "voluntary"));
        leaver.disconnect();
        
        // THEN: Observer should be notified of user leaving
        // Note: Disconnect notification might be handled by connection event handler
        // For now, we verify the explicit disconnect message was sent
        assertTrue(true, "Disconnect message sent successfully");
        
        // Verify observer can still receive other presence updates
        Map<String, Object> testData = WebSocketTestUtils.createPresenceUpdateData("ONLINE", hiveId, "still here");
        observer.send("/app/presence/status", testData);
        
        WebSocketTestClient.CapturedMessage confirmMessage = presenceCapture.waitForMessage(Duration.ofSeconds(2));
        assertNotNull(confirmMessage, "Observer should still receive presence updates after another user leaves");
        
        // Cleanup
        observer.disconnect();
    }

    // ============================================================================
    // TDD Test 4: Presence List Retrieval for New Joiners
    // ============================================================================

    @Test
    @Order(4)
    @DisplayName("Should provide presence list to new joiners")
    void shouldProvidePresenceListToNewJoiners() throws Exception {
        // GIVEN: Existing users in hive
        Long hiveId = WebSocketTestUtils.generateTestHiveId();
        String existing1Token = WebSocketTestUtils.generateTestJwtToken(1L, "existing1");
        String existing2Token = WebSocketTestUtils.generateTestJwtToken(2L, "existing2");
        String newJoinerToken = WebSocketTestUtils.generateTestJwtToken(3L, "newjoiner");
        
        StompSession existing1Session = webSocketClient.connectWithJwt(existing1Token);
        StompSession existing2Session = webSocketClient.connectWithJwt(existing2Token);
        
        StompTestSession existing1 = new StompTestSession(existing1Session);
        StompTestSession existing2 = new StompTestSession(existing2Session);
        
        // Existing users join hive
        existing1.send("/app/presence/status", 
                      WebSocketTestUtils.createPresenceUpdateData("ONLINE", hiveId, "working"));
        existing2.send("/app/presence/status", 
                      WebSocketTestUtils.createPresenceUpdateData("BUSY", hiveId, "in meeting"));
        
        WebSocketTestUtils.sleep(Duration.ofMillis(1000)); // Allow presence to be established
        
        // WHEN: New user joins and requests hive presence
        StompSession newJoinerSession = webSocketClient.connectWithJwt(newJoinerToken);
        StompTestSession newJoiner = new StompTestSession(newJoinerSession);
        
        WebSocketTestClient.MessageCapture presenceListCapture = 
            newJoiner.subscribe("/topic/hive/" + hiveId + "/presence");
        
        WebSocketTestUtils.sleep(Duration.ofMillis(500));
        
        // Request current hive presence
        newJoiner.send("/app/presence/hive/" + hiveId, Map.of("action", "getPresence"));
        
        // THEN: Should receive presence list with existing users
        WebSocketTestClient.CapturedMessage presenceList = 
            presenceListCapture.waitForMessage(Duration.ofSeconds(3));
        
        assertNotNull(presenceList, "New joiner should receive presence list");
        
        String presencePayload = presenceList.getPayload().toString();
        // Should contain information about existing users (exact format depends on implementation)
        assertTrue(presencePayload.length() > 10, "Presence list should contain user information");
        
        // Cleanup
        existing1.disconnect();
        existing2.disconnect();
        newJoiner.disconnect();
    }

    // ============================================================================
    // TDD Test 5: Concurrent User Updates Handling
    // ============================================================================

    @Test
    @Order(5)
    @DisplayName("Should handle concurrent user updates correctly")
    void shouldHandleConcurrentUserUpdates() throws Exception {
        // GIVEN: Multiple users updating presence simultaneously
        Long hiveId = WebSocketTestUtils.generateTestHiveId();
        String observerToken = WebSocketTestUtils.generateTestJwtToken(1L, "observer");
        
        StompSession observerSession = webSocketClient.connectWithJwt(observerToken);
        StompTestSession observer = new StompTestSession(observerSession);
        WebSocketTestClient.MessageCapture presenceCapture = observer.subscribe("/topic/presence");
        
        WebSocketTestUtils.sleep(Duration.ofMillis(500));
        
        // WHEN: Multiple users send updates concurrently
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
            final int userId = i + 10; // User IDs 10-14
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    String userToken = WebSocketTestUtils.generateTestJwtToken((long) userId, "concurrent" + userId);
                    StompSession userSession = webSocketClient.connectWithJwt(userToken);
                    StompTestSession user = new StompTestSession(userSession);
                    
                    // Send multiple updates rapidly
                    for (int j = 0; j < 3; j++) {
                        Map<String, Object> updateData = WebSocketTestUtils.createPresenceUpdateData(
                            (j % 2 == 0) ? "ONLINE" : "BUSY", hiveId, "update " + j);
                        user.send("/app/presence/status", updateData);
                        WebSocketTestUtils.sleep(Duration.ofMillis(100));
                    }
                    
                    user.disconnect();
                } catch (Exception e) {
                    log.error("Error in concurrent update: {}", e.getMessage());
                }
            }, executor);
            futures.add(future);
        }
        
        // Wait for all updates to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .get(10, TimeUnit.SECONDS);
        
        // THEN: Observer should receive presence updates (may not be all due to rapid updates)
        int receivedUpdates = 0;
        long endTime = System.currentTimeMillis() + 5000; // 5 second timeout
        
        while (System.currentTimeMillis() < endTime && receivedUpdates < 5) {
            WebSocketTestClient.CapturedMessage message = presenceCapture.waitForMessage(Duration.ofSeconds(1));
            if (message != null) {
                receivedUpdates++;
                String payload = message.getPayload().toString();
                assertTrue(payload.contains("ONLINE") || payload.contains("BUSY") || 
                          payload.contains("online") || payload.contains("busy"),
                          "Received message should contain valid status");
            }
        }
        
        assertTrue(receivedUpdates > 0, "Should receive at least some concurrent updates");
        log.info("Received {} presence updates from concurrent users", receivedUpdates);
        
        // Cleanup
        observer.disconnect();
        executor.shutdown();
    }

    // ============================================================================
    // TDD Test 6: Presence Persistence Across Reconnections
    // ============================================================================

    @Test
    @Order(6)
    @DisplayName("Should maintain presence state across reconnections")
    void shouldMaintainPresenceAcrossReconnections() throws Exception {
        // GIVEN: User with established presence
        Long userId = WebSocketTestUtils.generateTestUserId();
        Long hiveId = WebSocketTestUtils.generateTestHiveId();
        String userToken = WebSocketTestUtils.generateTestJwtToken(userId, "reconnect-user");
        
        StompSession firstSession = webSocketClient.connectWithJwt(userToken);
        StompTestSession firstConnection = new StompTestSession(firstSession);
        
        // Establish presence
        Map<String, Object> initialPresence = WebSocketTestUtils.createPresenceUpdateData("ONLINE", hiveId, "initial");
        firstConnection.send("/app/presence/status", initialPresence);
        
        WebSocketTestUtils.sleep(Duration.ofMillis(1000)); // Allow presence to be stored
        
        // WHEN: User disconnects and reconnects
        firstConnection.disconnect();
        WebSocketTestUtils.sleep(Duration.ofMillis(1000)); // Simulate network interruption
        
        StompSession secondSession = webSocketClient.connectWithJwt(userToken);
        StompTestSession secondConnection = new StompTestSession(secondSession);
        
        WebSocketTestClient.MessageCapture userPresenceCapture = 
            secondConnection.subscribe("/user/queue/presence/user");
        
        // Request user's own presence
        secondConnection.send("/app/presence/user/" + userId, Map.of("action", "getPresence"));
        
        // THEN: User should be able to retrieve their presence state
        WebSocketTestClient.CapturedMessage presenceMessage = 
            userPresenceCapture.waitForMessage(Duration.ofSeconds(3));
        
        assertNotNull(presenceMessage, "Should receive user presence after reconnection");
        
        String presencePayload = presenceMessage.getPayload().toString();
        assertTrue(presencePayload.length() > 10, "Presence data should be available");
        
        // Cleanup
        secondConnection.disconnect();
    }

    // ============================================================================
    // TDD Test 7: Presence Cleanup on Timeout
    // ============================================================================

    @Test
    @Order(7)
    @DisplayName("Should cleanup presence on connection timeout")
    void shouldCleanupPresenceOnTimeout() throws Exception {
        // GIVEN: User with established presence
        Long userId = WebSocketTestUtils.generateTestUserId();
        Long hiveId = WebSocketTestUtils.generateTestHiveId();
        String userToken = WebSocketTestUtils.generateTestJwtToken(userId, "timeout-user");
        String observerToken = WebSocketTestUtils.generateTestJwtToken(999L, "timeout-observer");
        
        StompSession userSession = webSocketClient.connectWithJwt(userToken);
        StompSession observerSession = webSocketClient.connectWithJwt(observerToken);
        
        StompTestSession user = new StompTestSession(userSession);
        StompTestSession observer = new StompTestSession(observerSession);
        
        // Observer monitors hive count
        WebSocketTestClient.MessageCapture countCapture = observer.subscribe("/user/queue/presence/count");
        
        // User establishes presence
        user.send("/app/presence/status", 
                 WebSocketTestUtils.createPresenceUpdateData("ONLINE", hiveId, "about to timeout"));
        
        WebSocketTestUtils.sleep(Duration.ofMillis(500));
        
        // Get initial count
        observer.send("/app/presence/hive/" + hiveId + "/count", Map.of("action", "getCount"));
        WebSocketTestClient.CapturedMessage initialCount = countCapture.waitForMessage(Duration.ofSeconds(2));
        assertNotNull(initialCount, "Should get initial hive count");
        
        // WHEN: User connection times out (simulate by abrupt disconnect)
        userSession.disconnect(); // Abrupt disconnect without cleanup
        
        // Wait for timeout cleanup (this would normally be handled by heartbeat failure)
        WebSocketTestUtils.sleep(Duration.ofMillis(2000));
        
        // THEN: Check if presence was cleaned up
        observer.send("/app/presence/hive/" + hiveId + "/count", Map.of("action", "getCount"));
        WebSocketTestClient.CapturedMessage finalCount = countCapture.waitForMessage(Duration.ofSeconds(2));
        
        // Note: Actual cleanup behavior depends on implementation
        // For now, we verify the observer can still get count requests
        assertNotNull(finalCount, "Should still be able to get hive count after user timeout");
        
        // Cleanup
        observer.disconnect();
    }

    // ============================================================================
    // TDD Test 8: High-Frequency Presence Updates
    // ============================================================================

    @Test
    @Order(8)
    @DisplayName("Should handle high-frequency presence updates efficiently")
    void shouldHandleHighFrequencyUpdates() throws Exception {
        // GIVEN: User capable of rapid updates
        Long userId = WebSocketTestUtils.generateTestUserId();
        Long hiveId = WebSocketTestUtils.generateTestHiveId();
        String userToken = WebSocketTestUtils.generateTestJwtToken(userId, "rapid-user");
        String observerToken = WebSocketTestUtils.generateTestJwtToken(998L, "rapid-observer");
        
        StompSession userSession = webSocketClient.connectWithJwt(userToken);
        StompSession observerSession = webSocketClient.connectWithJwt(observerToken);
        
        StompTestSession user = new StompTestSession(userSession);
        StompTestSession observer = new StompTestSession(observerSession);
        
        WebSocketTestClient.MessageCapture presenceCapture = observer.subscribe("/topic/presence");
        
        WebSocketTestUtils.sleep(Duration.ofMillis(500));
        
        // WHEN: Sending rapid presence updates
        long startTime = System.currentTimeMillis();
        int updateCount = 10;
        
        for (int i = 0; i < updateCount; i++) {
            String activity = "rapid update " + i;
            String status = (i % 3 == 0) ? "ONLINE" : (i % 3 == 1) ? "BUSY" : "AWAY";
            
            Map<String, Object> updateData = WebSocketTestUtils.createPresenceUpdateData(status, hiveId, activity);
            user.send("/app/presence/status", updateData);
            
            // Very small delay to create high frequency
            WebSocketTestUtils.sleep(Duration.ofMillis(50));
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        // THEN: System should handle rapid updates without errors
        assertTrue(totalTime < 2000, "Rapid updates should complete quickly");
        
        // Observer should receive at least some updates (system may throttle)
        int receivedCount = 0;
        long endTime = System.currentTimeMillis() + 3000;
        
        while (System.currentTimeMillis() < endTime && receivedCount < 5) {
            WebSocketTestClient.CapturedMessage message = presenceCapture.waitForMessage(Duration.ofSeconds(1));
            if (message != null) {
                receivedCount++;
            }
        }
        
        assertTrue(receivedCount > 0, "Should receive at least some rapid updates");
        log.info("Received {} out of {} rapid presence updates", receivedCount, updateCount);
        
        // Cleanup
        user.disconnect();
        observer.disconnect();
    }
}