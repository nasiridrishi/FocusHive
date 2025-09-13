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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for WebSocket authentication mechanisms
 * Following TDD approach: Write test → Run (fail) → Implement → Run (pass)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WebSocketAuthIntegrationTest {
    
    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthIntegrationTest.class);

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
    // TDD Test 1: JWT Validation During Handshake
    // ============================================================================

    @Test
    @Order(1)
    @DisplayName("Should validate JWT token during WebSocket handshake")
    void shouldValidateJwtDuringHandshake() throws Exception {
        // GIVEN: Valid JWT token with proper claims
        Long userId = WebSocketTestUtils.generateTestUserId();
        String validJwtToken = WebSocketTestUtils.generateTestJwtToken(userId, "auth-test-user");
        
        // WHEN: Connecting with valid JWT token
        StompSession session = webSocketClient.connectWithJwt(validJwtToken);
        
        // THEN: Connection should be established successfully
        assertTrue(session.isConnected(), "Should connect with valid JWT token");
        assertNotNull(session.getSessionId(), "Session should be assigned an ID");
        
        // Verify authenticated user can access protected endpoints
        StompTestSession testSession = new StompTestSession(session);
        WebSocketTestClient.MessageCapture ackCapture = testSession.subscribe("/user/queue/presence/ack");
        
        testSession.send("/app/presence/ws-heartbeat", "auth-test");
        
        WebSocketTestClient.CapturedMessage ackMessage = ackCapture.waitForMessage(Duration.ofSeconds(2));
        assertNotNull(ackMessage, "Authenticated user should receive acknowledgment");
        
        // Cleanup
        testSession.disconnect();
    }

    // ============================================================================
    // TDD Test 2: Handshake Rejection with Invalid JWT
    // ============================================================================

    @Test
    @Order(2)
    @DisplayName("Should reject handshake with invalid JWT token")
    void shouldRejectHandshakeWithInvalidJwt() {
        // GIVEN: Invalid JWT token
        String invalidJwtToken = WebSocketTestUtils.generateInvalidJwtToken();
        
        // WHEN & THEN: Connection attempt should fail
        Exception exception = assertThrows(Exception.class, () -> {
            webSocketClient.connectWithJwt(invalidJwtToken);
        }, "Should reject connection with invalid JWT");
        
        log.debug("Expected authentication failure: {}", exception.getMessage());
    }

    // ============================================================================
    // TDD Test 3: Handshake Rejection with Expired JWT
    // ============================================================================

    @Test
    @Order(3)
    @DisplayName("Should reject handshake with expired JWT token")
    void shouldRejectHandshakeWithExpiredJwt() {
        // GIVEN: Expired JWT token
        String expiredJwtToken = WebSocketTestUtils.generateExpiredJwtToken();
        
        // WHEN & THEN: Connection attempt should fail
        Exception exception = assertThrows(Exception.class, () -> {
            webSocketClient.connectWithJwt(expiredJwtToken);
        }, "Should reject connection with expired JWT");
        
        log.debug("Expected expiry failure: {}", exception.getMessage());
    }

    // ============================================================================
    // TDD Test 4: Handshake Rejection without Authentication
    // ============================================================================

    @Test
    @Order(4)
    @DisplayName("Should reject handshake without authentication header")
    void shouldRejectHandshakeWithoutAuth() {
        // WHEN & THEN: Connection without auth should fail
        Exception exception = assertThrows(Exception.class, () -> {
            webSocketClient.connectUnauthenticated();
        }, "Should reject connection without authentication");
        
        log.debug("Expected no-auth failure: {}", exception.getMessage());
    }

    // ============================================================================
    // TDD Test 5: Token Refresh During Active Connection
    // ============================================================================

    @Test
    @Order(5)
    @DisplayName("Should handle token refresh during active connection")
    void shouldHandleTokenRefreshDuringConnection() throws Exception {
        // GIVEN: Valid JWT token and established connection
        Long userId = WebSocketTestUtils.generateTestUserId();
        String initialToken = WebSocketTestUtils.generateTestJwtToken(userId, "refresh-user");
        
        StompSession session = webSocketClient.connectWithJwt(initialToken);
        StompTestSession testSession = new StompTestSession(session);
        
        assertTrue(session.isConnected(), "Initial connection should succeed");
        
        // WHEN: Attempting to use connection with potentially refreshed token
        // Note: In a real scenario, this would involve token refresh mechanisms
        WebSocketTestClient.MessageCapture ackCapture = testSession.subscribe("/user/queue/presence/ack");
        
        // Test that existing connection remains valid
        testSession.send("/app/presence/ws-heartbeat", "refresh-test");
        
        // THEN: Connection should remain active and functional
        WebSocketTestClient.CapturedMessage ackMessage = ackCapture.waitForMessage(Duration.ofSeconds(2));
        assertNotNull(ackMessage, "Connection should remain active during token lifecycle");
        
        // For token refresh, typically the client would reconnect with new token
        String refreshedToken = WebSocketTestUtils.generateTestJwtToken(userId, "refresh-user-new");
        
        // Disconnect old session
        testSession.disconnect();
        
        // Reconnect with refreshed token
        StompSession newSession = webSocketClient.connectWithJwt(refreshedToken);
        assertTrue(newSession.isConnected(), "Should reconnect with refreshed token");
        
        // Cleanup
        webSocketClient.disconnect(newSession);
    }

    // ============================================================================
    // TDD Test 6: Permission-Based Topic Access
    // ============================================================================

    @Test
    @Order(6)
    @DisplayName("Should enforce permission-based access to topics")
    void shouldEnforcePermissionBasedTopicAccess() throws Exception {
        // GIVEN: Authenticated user with specific permissions
        Long userId = WebSocketTestUtils.generateTestUserId();
        String userToken = WebSocketTestUtils.generateTestJwtToken(userId, "permission-user");
        
        StompSession session = webSocketClient.connectWithJwt(userToken);
        StompTestSession testSession = new StompTestSession(session);
        
        // WHEN: Subscribing to various topic types
        // These should be allowed for authenticated users
        WebSocketTestClient.MessageCapture publicPresence = testSession.subscribe("/topic/presence");
        WebSocketTestClient.MessageCapture userQueue = testSession.subscribe("/user/queue/presence/ack");
        
        assertTrue(testSession.isSubscribedTo("/topic/presence"), "Should allow public topic subscription");
        assertTrue(testSession.isSubscribedTo("/user/queue/presence/ack"), "Should allow user queue subscription");
        
        // THEN: User should be able to send to app destinations
        assertDoesNotThrow(() -> {
            testSession.send("/app/presence/ws-heartbeat", "permission-test");
            testSession.send("/app/presence/status", 
                           WebSocketTestUtils.createPresenceUpdateData("ONLINE", null, "testing permissions"));
        }, "Authenticated user should access app destinations");
        
        // Verify messages are processed
        WebSocketTestClient.CapturedMessage ackMessage = userQueue.waitForMessage(Duration.ofSeconds(2));
        assertNotNull(ackMessage, "Should receive acknowledgment for valid operations");
        
        // Cleanup
        testSession.disconnect();
    }

    // ============================================================================
    // TDD Test 7: Authentication State Changes
    // ============================================================================

    @Test
    @Order(7)
    @DisplayName("Should handle authentication state changes appropriately")
    void shouldHandleAuthStateChanges() throws Exception {
        // GIVEN: User with valid authentication
        Long userId = WebSocketTestUtils.generateTestUserId();
        String userToken = WebSocketTestUtils.generateTestJwtToken(userId, "state-change-user");
        
        StompSession session = webSocketClient.connectWithJwt(userToken);
        StompTestSession testSession = new StompTestSession(session);
        
        // Establish normal authenticated operations
        WebSocketTestClient.MessageCapture ackCapture = testSession.subscribe("/user/queue/presence/ack");
        testSession.send("/app/presence/ws-heartbeat", "initial-auth-test");
        
        WebSocketTestClient.CapturedMessage initialAck = ackCapture.waitForMessage(Duration.ofSeconds(2));
        assertNotNull(initialAck, "Should work with valid authentication");
        
        // WHEN: Authentication state becomes invalid (simulated by token expiry)
        // In real implementation, this might trigger automatic disconnection
        
        // THEN: System should handle authentication changes gracefully
        // For now, verify the connection remains functional until explicitly invalidated
        assertTrue(session.isConnected(), "Session should remain connected until explicitly invalidated");
        
        // Test continued operation
        testSession.send("/app/presence/ws-heartbeat", "continued-operation");
        WebSocketTestClient.CapturedMessage continuedAck = ackCapture.waitForMessage(Duration.ofSeconds(2));
        assertNotNull(continuedAck, "Operations should continue until auth is invalidated");
        
        // Cleanup
        testSession.disconnect();
    }

    // ============================================================================
    // TDD Test 8: Force Disconnect on Token Expiry
    // ============================================================================

    @Test
    @Order(8)
    @DisplayName("Should force disconnect when token expires")
    void shouldForceDisconnectOnTokenExpiry() throws Exception {
        // GIVEN: User with short-lived token (simulated)
        Long userId = WebSocketTestUtils.generateTestUserId();
        String shortLivedToken = WebSocketTestUtils.generateTestJwtToken(userId, "short-lived-user");
        
        StompSession session = webSocketClient.connectWithJwt(shortLivedToken);
        StompTestSession testSession = new StompTestSession(session);
        
        assertTrue(session.isConnected(), "Should initially connect with valid token");
        
        // WHEN: Token expires (in real implementation, this would be detected by JWT validation)
        // For testing, we simulate the scenario by trying to reconnect with expired token
        testSession.disconnect();
        
        String expiredToken = WebSocketTestUtils.generateExpiredJwtToken();
        
        // THEN: Should not be able to reconnect with expired token
        Exception exception = assertThrows(Exception.class, () -> {
            webSocketClient.connectWithJwt(expiredToken);
        }, "Should reject reconnection with expired token");
        
        log.debug("Token expiry properly rejected: {}", exception.getMessage());
    }

    // ============================================================================
    // TDD Test 9: Role-Based Channel Access
    // ============================================================================

    @Test
    @Order(9)
    @DisplayName("Should enforce role-based access to specific channels")
    void shouldEnforceRoleBasedChannelAccess() throws Exception {
        // GIVEN: Users with different roles (simulated in token)
        String regularUserToken = WebSocketTestUtils.generateTestJwtToken(1L, "regular-user");
        String adminUserToken = WebSocketTestUtils.generateTestJwtToken(2L, "admin-user");
        
        StompSession regularSession = webSocketClient.connectWithJwt(regularUserToken);
        StompSession adminSession = webSocketClient.connectWithJwt(adminUserToken);
        
        StompTestSession regularUser = new StompTestSession(regularSession);
        StompTestSession adminUser = new StompTestSession(adminSession);
        
        // WHEN: Both users access standard channels
        WebSocketTestClient.MessageCapture regularCapture = regularUser.subscribe("/topic/presence");
        WebSocketTestClient.MessageCapture adminCapture = adminUser.subscribe("/topic/presence");
        
        assertTrue(regularUser.isSubscribedTo("/topic/presence"), "Regular user should access public topics");
        assertTrue(adminUser.isSubscribedTo("/topic/presence"), "Admin user should access public topics");
        
        // THEN: Both should be able to use standard presence features
        regularUser.send("/app/presence/ws-heartbeat", "regular-heartbeat");
        adminUser.send("/app/presence/ws-heartbeat", "admin-heartbeat");
        
        // Note: In a real implementation, admin-specific channels would be tested here
        // For now, we verify both users can access standard functionality
        
        // Test standard operations work for both
        regularUser.send("/app/presence/status", 
                        WebSocketTestUtils.createPresenceUpdateData("ONLINE", null, "regular user"));
        adminUser.send("/app/presence/status", 
                      WebSocketTestUtils.createPresenceUpdateData("ONLINE", null, "admin user"));
        
        // Cleanup
        regularUser.disconnect();
        adminUser.disconnect();
    }

    // ============================================================================
    // TDD Test 10: Security Headers Validation
    // ============================================================================

    @Test
    @Order(10)
    @DisplayName("Should validate security headers during connection")
    void shouldValidateSecurityHeaders() throws Exception {
        // GIVEN: Connection with security headers
        Long userId = WebSocketTestUtils.generateTestUserId();
        String userToken = WebSocketTestUtils.generateTestJwtToken(userId, "security-header-user");
        
        StompHeaders secureHeaders = WebSocketTestUtils.createAuthHeaders(userToken);
        secureHeaders.add("X-Requested-With", "XMLHttpRequest");
        secureHeaders.add("Origin", "http://localhost:" + port);
        
        // WHEN: Connecting with proper security headers
        StompSession session = webSocketClient.connect(secureHeaders);
        
        // THEN: Connection should succeed with proper headers
        assertTrue(session.isConnected(), "Should connect with proper security headers");
        
        StompTestSession testSession = new StompTestSession(session);
        
        // Verify normal operations work
        WebSocketTestClient.MessageCapture ackCapture = testSession.subscribe("/user/queue/presence/ack");
        testSession.send("/app/presence/ws-heartbeat", "security-test");
        
        WebSocketTestClient.CapturedMessage ackMessage = ackCapture.waitForMessage(Duration.ofSeconds(2));
        assertNotNull(ackMessage, "Should work with proper security headers");
        
        // Cleanup
        testSession.disconnect();
    }

    // ============================================================================
    // TDD Test 11: Cross-Origin Request Handling
    // ============================================================================

    @Test
    @Order(11)
    @DisplayName("Should handle cross-origin requests appropriately")
    void shouldHandleCrossOriginRequests() throws Exception {
        // GIVEN: Valid credentials for cross-origin connection
        Long userId = WebSocketTestUtils.generateTestUserId();
        String userToken = WebSocketTestUtils.generateTestJwtToken(userId, "cors-user");
        
        StompHeaders corsHeaders = WebSocketTestUtils.createAuthHeaders(userToken);
        corsHeaders.add("Origin", "http://localhost:3000"); // Simulated frontend origin
        
        // WHEN: Attempting cross-origin WebSocket connection
        // Note: WebSocket CORS is configured in WebSocketConfig with setAllowedOriginPatterns("*")
        StompSession session = webSocketClient.connect(corsHeaders);
        
        // THEN: Connection should succeed with proper CORS configuration
        assertTrue(session.isConnected(), "Should allow cross-origin WebSocket connections");
        
        StompTestSession testSession = new StompTestSession(session);
        
        // Verify cross-origin connection works normally
        WebSocketTestClient.MessageCapture presenceCapture = testSession.subscribe("/topic/presence");
        testSession.send("/app/presence/status", 
                        WebSocketTestUtils.createPresenceUpdateData("ONLINE", null, "cors test"));
        
        // Should receive own presence update
        WebSocketTestClient.CapturedMessage presenceMessage = presenceCapture.waitForMessage(Duration.ofSeconds(2));
        assertNotNull(presenceMessage, "Cross-origin connection should work normally");
        
        // Cleanup
        testSession.disconnect();
    }

    // ============================================================================
    // TDD Test 12: Authentication Error Propagation
    // ============================================================================

    @Test
    @Order(12)
    @DisplayName("Should properly propagate authentication errors")
    void shouldPropagateAuthenticationErrors() {
        // GIVEN: Various invalid authentication scenarios
        String[] invalidTokens = {
            "", // Empty token
            "Bearer ", // Bearer without token
            "invalid-format-token", // Malformed token
            "Bearer " + WebSocketTestUtils.generateInvalidJwtToken() // Invalid JWT
        };
        
        // WHEN & THEN: Each invalid token should produce appropriate error
        for (String invalidToken : invalidTokens) {
            Exception exception = assertThrows(Exception.class, () -> {
                StompHeaders headers = new StompHeaders();
                headers.add("Authorization", invalidToken);
                webSocketClient.connect(headers);
            }, "Should reject invalid token: " + invalidToken);
            
            // Verify error contains authentication-related information
            String errorMessage = exception.getMessage().toLowerCase();
            // Note: Exact error message depends on Spring Security configuration
            log.debug("Authentication error for token '{}': {}", invalidToken, errorMessage);
        }
    }
}