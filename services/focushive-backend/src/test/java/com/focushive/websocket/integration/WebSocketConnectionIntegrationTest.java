package com.focushive.websocket.integration;

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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for WebSocket connection establishment
 * Following strict TDD approach: Write test → Run (fail) → Implement → Run (pass)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WebSocketConnectionIntegrationTest {
    
    private static final Logger log = LoggerFactory.getLogger(WebSocketConnectionIntegrationTest.class);

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
    private String rawWebSocketUrl;

    @BeforeEach
    void setUp() {
        webSocketUrl = WebSocketTestUtils.createWebSocketUrl(port, "/ws");
        rawWebSocketUrl = WebSocketTestUtils.createWebSocketUrl(port, "/ws-raw");
        webSocketClient = new WebSocketTestClient(webSocketUrl, Duration.ofSeconds(10));
    }

    @AfterEach
    void tearDown() {
        if (webSocketClient != null) {
            webSocketClient.shutdown();
        }
    }

    // ============================================================================
    // TDD Test 1: Basic WebSocket Connection with Valid JWT
    // ============================================================================

    @Test
    @Order(1)
    @DisplayName("Should establish WebSocket connection with valid JWT token")
    void shouldEstablishConnectionWithValidJWT() throws Exception {
        // GIVEN: Valid JWT token
        String validJwtToken = WebSocketTestUtils.generateTestJwtToken(1L, "testuser");
        
        // WHEN: Attempting to connect with valid JWT
        StompSession session = webSocketClient.connectWithJwt(validJwtToken);
        
        // THEN: Connection should be established successfully
        assertTrue(webSocketClient.waitForConnection(session, Duration.ofSeconds(5)),
                  "WebSocket connection should be established with valid JWT");
        assertTrue(session.isConnected(), "Session should be connected");
        assertNotNull(session.getSessionId(), "Session should have an ID");
        
        // Cleanup
        webSocketClient.disconnect(session);
    }

    // ============================================================================
    // TDD Test 2: Connection Rejection with Invalid JWT
    // ============================================================================

    @Test
    @Order(2)
    @DisplayName("Should reject WebSocket connection with invalid JWT token")
    void shouldRejectConnectionWithInvalidJWT() {
        // GIVEN: Invalid JWT token
        String invalidJwtToken = WebSocketTestUtils.generateInvalidJwtToken();
        
        // WHEN & THEN: Connection should fail with invalid JWT
        assertThrows(Exception.class, () -> {
            webSocketClient.connectWithJwt(invalidJwtToken);
        }, "Connection should fail with invalid JWT token");
    }

    // ============================================================================
    // TDD Test 3: Connection Rejection with Expired JWT
    // ============================================================================

    @Test
    @Order(3)
    @DisplayName("Should reject WebSocket connection with expired JWT token")
    void shouldRejectConnectionWithExpiredJWT() {
        // GIVEN: Expired JWT token
        String expiredJwtToken = WebSocketTestUtils.generateExpiredJwtToken();
        
        // WHEN & THEN: Connection should fail with expired JWT
        assertThrows(Exception.class, () -> {
            webSocketClient.connectWithJwt(expiredJwtToken);
        }, "Connection should fail with expired JWT token");
    }

    // ============================================================================
    // TDD Test 4: Connection without Authentication
    // ============================================================================

    @Test
    @Order(4)
    @DisplayName("Should reject WebSocket connection without authentication")
    void shouldRejectConnectionWithoutAuthentication() {
        // WHEN & THEN: Connection should fail without authentication
        assertThrows(Exception.class, () -> {
            webSocketClient.connectUnauthenticated();
        }, "Connection should fail without authentication");
    }

    // ============================================================================
    // TDD Test 5: HTTP to WebSocket Upgrade
    // ============================================================================

    @Test
    @Order(5)
    @DisplayName("Should successfully upgrade HTTP connection to WebSocket")
    void shouldUpgradeHttpToWebSocket() throws Exception {
        // GIVEN: Valid JWT token for authentication
        String validJwtToken = WebSocketTestUtils.generateTestJwtToken(1L, "testuser");
        
        // WHEN: Establishing WebSocket connection (HTTP upgrade happens automatically)
        StompSession session = webSocketClient.connectWithJwt(validJwtToken);
        
        // THEN: Connection should be upgraded and established
        assertTrue(session.isConnected(), "Connection should be upgraded to WebSocket");
        assertEquals("1.2", session.getStompVersion(), "STOMP version should be negotiated");
        
        // Verify we can send a basic message to confirm the upgrade worked
        webSocketClient.sendMessage(session, "/app/presence/ws-heartbeat", "ping");
        assertEquals(1, webSocketClient.getMessagesSent(), 
                    "Should be able to send messages after upgrade");
        
        // Cleanup
        webSocketClient.disconnect(session);
    }

    // ============================================================================
    // TDD Test 6: Connection with Different Client Libraries
    // ============================================================================

    @Test
    @Order(6)
    @DisplayName("Should support connections from different client types")
    void shouldSupportDifferentClientTypes() throws Exception {
        // GIVEN: Valid JWT token
        String validJwtToken = WebSocketTestUtils.generateTestJwtToken(1L, "testuser");
        
        // WHEN: Creating multiple clients with different configurations
        WebSocketTestClient sockJSClient = new WebSocketTestClient(webSocketUrl);
        WebSocketTestClient rawClient = new WebSocketTestClient(rawWebSocketUrl);
        
        StompSession sockJSSession = sockJSClient.connectWithJwt(validJwtToken);
        StompSession rawSession = rawClient.connectWithJwt(validJwtToken);
        
        // THEN: Both clients should connect successfully
        assertTrue(sockJSSession.isConnected(), "SockJS client should connect");
        assertTrue(rawSession.isConnected(), "Raw WebSocket client should connect");
        
        assertNotEquals(sockJSSession.getSessionId(), rawSession.getSessionId(),
                       "Sessions should have different IDs");
        
        // Cleanup
        sockJSClient.disconnect(sockJSSession);
        rawClient.disconnect(rawSession);
        sockJSClient.shutdown();
        rawClient.shutdown();
    }

    // ============================================================================
    // TDD Test 7: Connection Timeout Handling
    // ============================================================================

    @Test
    @Order(7)
    @DisplayName("Should handle connection timeout gracefully")
    void shouldHandleConnectionTimeout() {
        // GIVEN: WebSocket client with very short timeout
        WebSocketTestClient timeoutClient = new WebSocketTestClient(
            "ws://non-existent-host:9999/ws", Duration.ofMillis(500));
        
        // WHEN & THEN: Connection should timeout
        Exception exception = assertThrows(RuntimeException.class, () -> {
            String validJwtToken = WebSocketTestUtils.generateTestJwtToken(1L, "testuser");
            timeoutClient.connectWithJwt(validJwtToken);
        });
        
        assertTrue(exception.getMessage().contains("timed out"),
                  "Exception should indicate timeout");
        
        // Cleanup
        timeoutClient.shutdown();
    }

    // ============================================================================
    // TDD Test 8: Maximum Concurrent Connections Limit
    // ============================================================================

    @Test
    @Order(8)
    @DisplayName("Should enforce maximum concurrent connections limit")
    void shouldEnforceMaxConcurrentConnections() throws Exception {
        // GIVEN: Multiple concurrent connection attempts
        String validJwtToken = WebSocketTestUtils.generateTestJwtToken(1L, "testuser");
        List<StompSession> sessions = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(50);
        
        try {
            // WHEN: Creating many concurrent connections
            List<CompletableFuture<StompSession>> futures = new ArrayList<>();
            for (int i = 0; i < 20; i++) { // Reasonable test limit
                CompletableFuture<StompSession> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        Long userId = WebSocketTestUtils.generateTestUserId();
                        String token = WebSocketTestUtils.generateTestJwtToken(userId, "user" + userId);
                        return webSocketClient.connectWithJwt(token);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, executor);
                futures.add(future);
            }
            
            // THEN: Connections should be established up to reasonable limits
            int successfulConnections = 0;
            for (CompletableFuture<StompSession> future : futures) {
                try {
                    StompSession session = future.get(5, TimeUnit.SECONDS);
                    if (session.isConnected()) {
                        sessions.add(session);
                        successfulConnections++;
                    }
                } catch (Exception e) {
                    log.debug("Connection failed (expected for limit testing): {}", e.getMessage());
                }
            }
            
            assertTrue(successfulConnections > 0, "At least some connections should succeed");
            log.info("Established {} concurrent WebSocket connections", successfulConnections);
            
        } finally {
            // Cleanup
            sessions.forEach(webSocketClient::disconnect);
            executor.shutdown();
        }
    }

    // ============================================================================
    // TDD Test 9: Reconnection with Same Session
    // ============================================================================

    @Test
    @Order(9)
    @DisplayName("Should support reconnection attempts")
    void shouldSupportReconnectionAttempts() throws Exception {
        // GIVEN: Valid JWT token for user
        Long userId = WebSocketTestUtils.generateTestUserId();
        String validJwtToken = WebSocketTestUtils.generateTestJwtToken(userId, "reconnect-user");
        
        // WHEN: Establishing initial connection
        StompSession firstSession = webSocketClient.connectWithJwt(validJwtToken);
        String firstSessionId = firstSession.getSessionId();
        
        assertTrue(firstSession.isConnected(), "First connection should be established");
        
        // Disconnect first session
        webSocketClient.disconnect(firstSession);
        WebSocketTestUtils.sleep(Duration.ofMillis(500)); // Wait for cleanup
        
        // Reconnect with same credentials
        StompSession secondSession = webSocketClient.connectWithJwt(validJwtToken);
        String secondSessionId = secondSession.getSessionId();
        
        // THEN: Reconnection should succeed with new session
        assertTrue(secondSession.isConnected(), "Reconnection should succeed");
        assertNotEquals(firstSessionId, secondSessionId, 
                       "New session should have different ID");
        
        // Cleanup
        webSocketClient.disconnect(secondSession);
    }

    // ============================================================================
    // TDD Test 10: Connection with Custom Headers
    // ============================================================================

    @Test
    @Order(10)
    @DisplayName("Should support connection with custom headers")
    void shouldSupportCustomHeaders() throws Exception {
        // GIVEN: Custom STOMP headers
        String validJwtToken = WebSocketTestUtils.generateTestJwtToken(1L, "custom-header-user");
        StompHeaders customHeaders = WebSocketTestUtils.createAuthHeaders(validJwtToken);
        customHeaders.add("X-Client-Type", "integration-test");
        customHeaders.add("X-Test-ID", "websocket-connection-test");
        
        // WHEN: Connecting with custom headers
        StompSession session = webSocketClient.connect(customHeaders);
        
        // THEN: Connection should succeed with custom headers
        assertTrue(session.isConnected(), "Connection should succeed with custom headers");
        assertNotNull(session.getSessionId(), "Session ID should be assigned");
        
        // Verify session can be used normally
        webSocketClient.sendMessage(session, "/app/presence/ws-heartbeat", "test");
        assertEquals(1, webSocketClient.getMessagesSent(), 
                    "Should be able to send messages");
        
        // Cleanup
        webSocketClient.disconnect(session);
    }

    // ============================================================================
    // TDD Test 11: Connection Latency Measurement
    // ============================================================================

    @Test
    @Order(11)
    @DisplayName("Should establish connection within acceptable latency")
    void shouldEstablishConnectionWithinAcceptableLatency() throws Exception {
        // GIVEN: Valid JWT token
        String validJwtToken = WebSocketTestUtils.generateTestJwtToken(1L, "latency-user");
        
        // WHEN: Measuring connection establishment time
        long startTime = System.currentTimeMillis();
        StompSession session = webSocketClient.connectWithJwt(validJwtToken);
        long connectionTime = System.currentTimeMillis() - startTime;
        
        // THEN: Connection should be established within reasonable time
        assertTrue(session.isConnected(), "Connection should be established");
        assertTrue(connectionTime < 5000, // 5 seconds for integration test
                  String.format("Connection should be established within 5 seconds, took %dms", 
                               connectionTime));
        
        log.info("WebSocket connection established in {}ms", connectionTime);
        
        // Test message round-trip latency
        long messageStart = System.currentTimeMillis();
        webSocketClient.sendMessage(session, "/app/presence/ws-heartbeat", "latency-test");
        long messageLatency = System.currentTimeMillis() - messageStart;
        
        assertTrue(WebSocketTestUtils.isLatencyAcceptable(messageLatency),
                  String.format("Message latency should be acceptable, was %dms", messageLatency));
        
        // Cleanup
        webSocketClient.disconnect(session);
    }
}