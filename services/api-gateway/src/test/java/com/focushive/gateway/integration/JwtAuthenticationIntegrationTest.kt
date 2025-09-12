package com.focushive.gateway.integration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.junit.jupiter.api.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * Integration tests for API Gateway JWT authentication enforcement
 * 
 * Tests:
 * - Valid JWT token acceptance
 * - Invalid/expired JWT token rejection
 * - Missing JWT token rejection for protected routes
 * - Public routes accessibility without JWT
 * - JWT token validation and user context extraction
 * - Error responses for authentication failures
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class JwtAuthenticationIntegrationTest : BaseIntegrationTest() {

    companion object {
        private val wireMockServer = WireMockServer(options().port(8088))

        @JvmStatic
        @DynamicPropertySource
        fun configureWiremock(registry: DynamicPropertyRegistry) {
            // Override route URIs to point to WireMock server
            registry.add("spring.cloud.gateway.routes[1].uri") { "http://localhost:8088" } // focushive-backend
            registry.add("spring.cloud.gateway.routes[2].uri") { "http://localhost:8088" } // music-service
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

    @BeforeEach
    fun setUp() {
        webTestClient = createWebTestClient()
        wireMockServer.resetAll()
    }

    @Test
    @Order(1)
    fun `should allow access to public routes without JWT token`() {
        // Given
        wireMockServer.stubFor(
            get(urlPathMatching("/auth/.*"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"service": "identity", "endpoint": "login"}""")
                )
        )

        // When & Then - Public auth endpoints should be accessible without token
        webTestClient.get()
            .uri("/auth/login")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)

        // Verify request reached backend without authentication headers
        wireMockServer.verify(
            getRequestedFor(urlEqualTo("/auth/login"))
                .withoutHeader("X-User-Id")
                .withoutHeader("X-Username")
        )
    }

    @Test
    @Order(2)
    fun `should allow access to health endpoints without JWT token`() {
        // Given
        wireMockServer.stubFor(
            get(urlPathMatching("/health/.*"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"status": "UP"}""")
                )
        )

        // When & Then - Health endpoints should be accessible without token
        webTestClient.get()
            .uri("/health/gateway")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)

        wireMockServer.verify(getRequestedFor(urlEqualTo("/health/gateway")))
    }

    @Test
    @Order(3)
    fun `should reject protected routes without JWT token`() {
        // When & Then - Protected endpoint without token should return 401
        webTestClient.get()
            .uri("/hives/123")
            .exchange()
            .expectStatus().isUnauthorized
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.error").isEqualTo("Unauthorized")
            .jsonPath("$.message").isEqualTo("Valid JWT token required")
            .jsonPath("$.status").isEqualTo(401)

        // Verify request was not forwarded to backend
        wireMockServer.verify(0, getRequestedFor(urlEqualTo("/hives/123")))
    }

    @Test
    @Order(4)
    fun `should reject protected routes with invalid JWT token`() {
        // Given
        val invalidToken = createInvalidJwtToken()

        // When & Then - Invalid token should return 401
        webTestClient.get()
            .uri("/hives/123")
            .header("Authorization", withBearerToken(invalidToken))
            .exchange()
            .expectStatus().isUnauthorized
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.error").isEqualTo("Unauthorized")
            .jsonPath("$.message").isEqualTo("Valid JWT token required")

        // Verify request was not forwarded to backend
        wireMockServer.verify(0, getRequestedFor(urlEqualTo("/hives/123")))
    }

    @Test
    @Order(5)
    fun `should reject protected routes with expired JWT token`() {
        // Given
        val expiredToken = createExpiredJwtToken()

        // When & Then - Expired token should return 401
        webTestClient.get()
            .uri("/hives/123")
            .header("Authorization", withBearerToken(expiredToken))
            .exchange()
            .expectStatus().isUnauthorized
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.error").isEqualTo("Unauthorized")
            .jsonPath("$.message").isEqualTo("Valid JWT token required")

        // Verify request was not forwarded to backend
        wireMockServer.verify(0, getRequestedFor(urlEqualTo("/hives/123")))
    }

    @Test
    @Order(6)
    fun `should accept valid JWT token for protected routes`() {
        // Given
        val validToken = createValidJwtToken(
            userId = "user-123",
            username = "testuser",
            roles = listOf("USER", "PREMIUM"),
            personaId = "persona-456"
        )

        wireMockServer.stubFor(
            get(urlPathMatching("/hives/.*"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"id": "123", "name": "Test Hive"}""")
                )
        )

        // When & Then - Valid token should allow access
        webTestClient.get()
            .uri("/hives/123")
            .header("Authorization", withBearerToken(validToken))
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id").isEqualTo("123")
            .jsonPath("$.name").isEqualTo("Test Hive")

        // Verify request was forwarded with correct user context headers
        wireMockServer.verify(
            getRequestedFor(urlEqualTo("/hives/123"))
                .withHeader("X-User-Id", equalTo("user-123"))
                .withHeader("X-Username", equalTo("testuser"))
                .withHeader("X-User-Roles", equalTo("USER,PREMIUM"))
                .withHeader("X-Persona-Id", equalTo("persona-456"))
                .withHeader("X-Auth-Provider", equalTo("focushive-identity-service"))
        )
    }

    @Test
    @Order(7)
    fun `should handle malformed authorization header`() {
        // When & Then - Malformed authorization header should return 401
        webTestClient.get()
            .uri("/hives/123")
            .header("Authorization", "InvalidFormat token123")
            .exchange()
            .expectStatus().isUnauthorized

        webTestClient.get()
            .uri("/hives/123")
            .header("Authorization", "Bearer") // Missing token
            .exchange()
            .expectStatus().isUnauthorized

        webTestClient.get()
            .uri("/hives/123")
            .header("Authorization", "Bearer ") // Empty token
            .exchange()
            .expectStatus().isUnauthorized

        // Verify no requests were forwarded
        wireMockServer.verify(0, getRequestedFor(urlPathMatching("/hives/.*")))
    }

    @Test
    @Order(8)
    fun `should extract user context from JWT claims correctly`() {
        // Given
        val validToken = createValidJwtToken(
            userId = "user-789",
            username = "contextuser",
            roles = listOf("ADMIN", "USER"),
            personaId = "persona-admin"
        )

        wireMockServer.stubFor(
            post(urlPathMatching("/hives"))
                .willReturn(
                    aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"id": "new-hive", "created": true}""")
                )
        )

        // When & Then
        webTestClient.post()
            .uri("/hives")
            .header("Authorization", withBearerToken(validToken))
            .header("Content-Type", "application/json")
            .bodyValue("""{"name": "New Hive", "description": "Test"}""")
            .exchange()
            .expectStatus().isCreated

        // Verify all user context headers are properly extracted and forwarded
        wireMockServer.verify(
            postRequestedFor(urlEqualTo("/hives"))
                .withHeader("X-User-Id", equalTo("user-789"))
                .withHeader("X-Username", equalTo("contextuser"))
                .withHeader("X-User-Roles", equalTo("ADMIN,USER"))
                .withHeader("X-Persona-Id", equalTo("persona-admin"))
                .withHeader("X-Auth-Provider", equalTo("focushive-identity-service"))
        )
    }

    @Test
    @Order(9)
    fun `should handle JWT token with missing optional claims`() {
        // Given - Token with minimal claims (no persona_id)
        val minimalToken = createValidJwtToken(
            userId = "user-minimal",
            username = "minimaluser",
            roles = listOf("USER"),
            personaId = ""  // Empty persona ID
        )

        wireMockServer.stubFor(
            get(urlPathMatching("/hives/.*"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("""{"id": "123"}""")
                )
        )

        // When & Then - Should still work with minimal claims
        webTestClient.get()
            .uri("/hives/123")
            .header("Authorization", withBearerToken(minimalToken))
            .exchange()
            .expectStatus().isOk

        // Verify headers include empty values for missing claims
        wireMockServer.verify(
            getRequestedFor(urlEqualTo("/hives/123"))
                .withHeader("X-User-Id", equalTo("user-minimal"))
                .withHeader("X-Username", equalTo("minimaluser"))
                .withHeader("X-User-Roles", equalTo("USER"))
                .withHeader("X-Persona-Id", equalTo(""))
        )
    }

    @Test
    @Order(10)
    fun `should authenticate multiple protected service routes`() {
        // Given
        val validToken = createValidJwtToken()

        wireMockServer.stubFor(
            get(urlPathMatching("/music/.*"))
                .willReturn(aResponse().withStatus(200).withBody("""{"tracks": []}"""))
        )

        wireMockServer.stubFor(
            get(urlPathMatching("/hives/.*"))
                .willReturn(aResponse().withStatus(200).withBody("""{"hives": []}"""))
        )

        // Test music service authentication
        webTestClient.get()
            .uri("/music/playlists")
            .header("Authorization", withBearerToken(validToken))
            .exchange()
            .expectStatus().isOk

        // Test focushive backend authentication
        webTestClient.get()
            .uri("/hives/active")
            .header("Authorization", withBearerToken(validToken))
            .exchange()
            .expectStatus().isOk

        // Verify both requests were authenticated and forwarded
        wireMockServer.verify(
            getRequestedFor(urlEqualTo("/music/playlists"))
                .withHeader("X-User-Id", equalTo("test-user-id"))
        )
        
        wireMockServer.verify(
            getRequestedFor(urlEqualTo("/hives/active"))
                .withHeader("X-User-Id", equalTo("test-user-id"))
        )
    }

    @Test
    @Order(11)
    fun `should handle concurrent authentication requests correctly`() {
        // Given
        val validToken = createValidJwtToken()
        
        wireMockServer.stubFor(
            get(urlPathMatching("/hives/.*"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withFixedDelay(100) // Small delay to test concurrency
                        .withBody("""{"success": true}""")
                )
        )

        // When - Make multiple concurrent requests
        val responses = (1..5).map { id ->
            webTestClient.get()
                .uri("/hives/$id")
                .header("Authorization", withBearerToken(validToken))
                .exchange()
        }

        // Then - All should succeed
        responses.forEach { response ->
            response.expectStatus().isOk
        }

        // Verify all requests were forwarded
        wireMockServer.verify(5, getRequestedFor(urlPathMatching("/hives/.*")))
    }
}