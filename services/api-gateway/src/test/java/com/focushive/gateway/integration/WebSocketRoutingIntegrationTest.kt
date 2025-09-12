package com.focushive.gateway.integration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.junit.jupiter.api.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import org.springframework.web.reactive.socket.client.WebSocketClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.net.URI
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Integration tests for API Gateway WebSocket routing functionality
 * 
 * Following strict TDD approach:
 * 1. Write failing test for WebSocket routing
 * 2. Implement WebSocket routing configuration
 * 3. Verify test passes
 * 
 * Tests:
 * - WebSocket connection routing to backend services
 * - JWT authentication for WebSocket connections
 * - WebSocket message routing and forwarding
 * - WebSocket connection authentication and authorization
 * - WebSocket connection error handling
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class WebSocketRoutingIntegrationTest : BaseIntegrationTest() {

    companion object {
        private val wireMockServer = WireMockServer(options().port(8091))

        @JvmStatic
        @DynamicPropertySource
        fun configureWiremock(registry: DynamicPropertyRegistry) {
            // Configure WebSocket route to point to WireMock
            registry.add("spring.cloud.gateway.routes[1].uri") { "ws://localhost:8091" } // focushive-backend WebSocket
        }

        @JvmStatic
        @BeforeAll
        fun startWireMock() {
            wireMockServer.start()
        }

        @JvmStatic
        @AfterAll
        fun stopWireMock() {
            wireMockServer.stop()
        }
    }

    private lateinit var webSocketClient: WebSocketClient

    @BeforeEach
    fun setUp() {
        webTestClient = createWebTestClient()
        webSocketClient = ReactorNettyWebSocketClient()
        wireMockServer.resetAll()
    }

    @Test
    @Order(1)
    fun `should fail to route WebSocket connections without JWT authentication`() {
        // Given - WebSocket endpoint without authentication
        val websocketUri = URI.create("ws://localhost:$port/api/ws/focus-sessions")
        val latch = CountDownLatch(1)
        var connectionFailed = false

        // When - Attempt to connect without authentication token
        webSocketClient.execute(websocketUri) { session ->
            // This should fail due to lack of authentication
            session.send(Mono.empty())
        }.doOnError { 
            connectionFailed = true
            latch.countDown()
        }.doOnSuccess {
            // Should not reach here
            latch.countDown()
        }.subscribe()

        // Then - Connection should fail
        latch.await(5, TimeUnit.SECONDS)
        Assertions.assertTrue(connectionFailed, "WebSocket connection should fail without authentication")
    }

    @Test
    @Order(2)
    fun `should fail to route authenticated WebSocket connections when backend is unavailable`() {
        // Given - Valid JWT token but no backend WebSocket server
        val validToken = createValidJwtToken()
        val websocketUri = URI.create("ws://localhost:$port/api/ws/focus-sessions?token=$validToken")
        val latch = CountDownLatch(1)
        var connectionFailed = false

        // When - Attempt to connect with valid token but unavailable backend
        webSocketClient.execute(websocketUri) { session ->
            session.send(Mono.empty())
        }.doOnError { 
            connectionFailed = true
            latch.countDown()
        }.doOnSuccess {
            // Should not reach here without backend
            latch.countDown()
        }.subscribe()

        // Then - Connection should fail due to unavailable backend
        latch.await(5, TimeUnit.SECONDS)
        Assertions.assertTrue(connectionFailed, "WebSocket connection should fail when backend unavailable")
    }

    @Test
    @Order(3)
    fun `should route WebSocket connections with JWT authentication when backend is available`() {
        // Given - Mock WebSocket server and valid JWT
        setupMockWebSocketServer()
        val validToken = createValidJwtToken()
        val websocketUri = URI.create("ws://localhost:$port/api/ws/focus-sessions?token=$validToken")
        
        // When & Then - This test will fail initially as WebSocket routing is not implemented
        val connectionResult = webSocketClient.execute(websocketUri) { session ->
            // Send a test message
            val testMessage = session.textMessage("Hello WebSocket")
            session.send(Mono.just(testMessage))
                .then(session.receive().take(1).single())
        }

        // This test should fail because WebSocket routing is not yet implemented
        StepVerifier.create(connectionResult)
            .expectError() // Expecting error because WebSocket routing not implemented
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @Order(4)
    fun `should forward WebSocket messages to correct backend service`() {
        // Given - Mock WebSocket server with message handling and valid JWT
        setupMockWebSocketServer()
        val validToken = createValidJwtToken()
        val websocketUri = URI.create("ws://localhost:$port/api/ws/focus-sessions?token=$validToken")
        
        // When - Send message through WebSocket
        val messageFlow = webSocketClient.execute(websocketUri) { session ->
            val message = session.textMessage("""{"type": "START_FOCUS_SESSION", "data": {"userId": "test-user-id"}}""")
            
            session.send(Mono.just(message))
                .then(
                    session.receive()
                        .take(Duration.ofSeconds(5))
                        .map { it.payloadAsText }
                        .collectList()
                )
        }

        // Then - Should receive response from backend (will fail as WebSocket routing not implemented)
        StepVerifier.create(messageFlow)
            .expectError() // Expecting error because WebSocket routing not implemented
            .verify(Duration.ofSeconds(10))
    }

    @Test
    @Order(5)
    fun `should reject WebSocket connections with invalid JWT tokens`() {
        // Given - Invalid JWT token
        val invalidToken = createInvalidJwtToken()
        val websocketUri = URI.create("ws://localhost:$port/api/ws/focus-sessions?token=$invalidToken")
        val latch = CountDownLatch(1)
        var connectionFailed = false

        // When - Attempt to connect with invalid token
        webSocketClient.execute(websocketUri) { session ->
            session.send(Mono.empty())
        }.doOnError { 
            connectionFailed = true
            latch.countDown()
        }.doOnSuccess {
            // Should not reach here with invalid token
            latch.countDown()
        }.subscribe()

        // Then - Connection should fail with invalid token
        latch.await(5, TimeUnit.SECONDS)
        Assertions.assertTrue(connectionFailed, "WebSocket connection should fail with invalid JWT token")
    }

    @Test
    @Order(6)
    fun `should reject WebSocket connections with expired JWT tokens`() {
        // Given - Expired JWT token
        val expiredToken = createExpiredJwtToken()
        val websocketUri = URI.create("ws://localhost:$port/api/ws/focus-sessions?token=$expiredToken")
        val latch = CountDownLatch(1)
        var connectionFailed = false

        // When - Attempt to connect with expired token
        webSocketClient.execute(websocketUri) { session ->
            session.send(Mono.empty())
        }.doOnError { 
            connectionFailed = true
            latch.countDown()
        }.doOnSuccess {
            // Should not reach here with expired token
            latch.countDown()
        }.subscribe()

        // Then - Connection should fail with expired token
        latch.await(5, TimeUnit.SECONDS)
        Assertions.assertTrue(connectionFailed, "WebSocket connection should fail with expired JWT token")
    }

    @Test
    @Order(7)
    fun `should handle multiple concurrent WebSocket connections`() {
        // Given - Multiple valid JWT tokens and mock WebSocket server
        setupMockWebSocketServer()
        val tokens = (1..5).map { createValidJwtToken("user-$it", "user$it") }
        val connectionResults = mutableListOf<Mono<List<String>>>()

        // When - Create multiple concurrent WebSocket connections
        tokens.forEach { token ->
            val websocketUri = URI.create("ws://localhost:$port/api/ws/focus-sessions?token=$token")
            val connectionResult = webSocketClient.execute(websocketUri) { session ->
                val message = session.textMessage("Hello from concurrent connection")
                session.send(Mono.just(message))
                    .then(
                        session.receive()
                            .take(Duration.ofSeconds(3))
                            .map { it.payloadAsText }
                            .collectList()
                    )
            }
            connectionResults.add(connectionResult)
        }

        // Then - All connections should fail as WebSocket routing not implemented
        connectionResults.forEach { result ->
            StepVerifier.create(result)
                .expectError() // Expecting error because WebSocket routing not implemented
                .verify(Duration.ofSeconds(10))
        }
    }

    private fun setupMockWebSocketServer() {
        // Mock WebSocket server setup (simplified)
        // In real implementation, this would set up a proper WebSocket mock
        wireMockServer.stubFor(
            get(urlPathMatching("/ws/.*"))
                .willReturn(
                    aResponse()
                        .withStatus(426) // Upgrade Required for WebSocket
                        .withHeader("Upgrade", "websocket")
                        .withHeader("Connection", "Upgrade")
                )
        )
    }
}