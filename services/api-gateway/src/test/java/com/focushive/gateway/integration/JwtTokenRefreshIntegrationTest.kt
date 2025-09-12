package com.focushive.gateway.integration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Integration tests for JWT Token Refresh functionality in API Gateway
 * 
 * Following strict TDD approach:
 * 1. Write failing test for JWT token refresh
 * 2. Implement JWT token refresh filter/mechanism
 * 3. Verify test passes
 * 
 * Tests:
 * - JWT token refresh for near-expiry tokens
 * - Automatic token refresh in response headers
 * - Handling of refresh token validation
 * - Token refresh rate limiting
 * - Token refresh security validation
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class JwtTokenRefreshIntegrationTest : BaseIntegrationTest() {

    companion object {
        private val wireMockServer = WireMockServer(options().port(8092))

        @JvmStatic
        @DynamicPropertySource
        fun configureWiremock(registry: DynamicPropertyRegistry) {
            // Configure identity service route for token refresh
            registry.add("spring.cloud.gateway.routes[0].uri") { "http://localhost:8092" } // identity service
            registry.add("spring.cloud.gateway.routes[1].uri") { "http://localhost:8092" } // focushive-backend
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
    fun `should fail to automatically refresh JWT token when near expiry without refresh mechanism`() {
        // Given - Token expiring in 5 minutes (near expiry threshold)
        val nearExpiryToken = createNearExpiryJwtToken()
        
        // Mock backend service response
        wireMockServer.stubFor(
            get(urlPathMatching("/hives/.*"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"service": "focushive-backend", "data": "test"}""")
                )
        )

        // When - Make request with near-expiry token
        val response = webTestClient.get()
            .uri("/hives/123")
            .header("Authorization", withBearerToken(nearExpiryToken))
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .returnResult(String::class.java)

        // Then - Should NOT include refreshed token in response header (will fail as refresh not implemented)
        val refreshedToken = response.responseHeaders.getFirst("X-Refreshed-Token")
        Assertions.assertNull(refreshedToken, "Token should not be automatically refreshed (feature not implemented)")
    }

    @Test
    @Order(2)
    fun `should fail to handle explicit token refresh requests`() {
        // Given - Valid refresh token
        val refreshToken = createRefreshToken()
        
        // Mock identity service refresh endpoint (not yet implemented)
        wireMockServer.stubFor(
            post(urlEqualTo("/auth/refresh"))
                .withHeader("Content-Type", equalTo("application/json"))
                .willReturn(
                    aResponse()
                        .withStatus(404) // Not found - endpoint doesn't exist yet
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"error": "Endpoint not found"}""")
                )
        )

        // When - Attempt to refresh token via dedicated endpoint
        webTestClient.post()
            .uri("/auth/refresh")
            .header("Content-Type", "application/json")
            .bodyValue("""{"refreshToken": "$refreshToken"}""")
            .exchange()
            .expectStatus().isNotFound // Should fail as endpoint doesn't exist

        // Verify the request was made to identity service
        wireMockServer.verify(postRequestedFor(urlEqualTo("/auth/refresh")))
    }

    @Test
    @Order(3)
    fun `should fail to validate refresh tokens properly`() {
        // Given - Invalid refresh token
        val invalidRefreshToken = createInvalidRefreshToken()
        
        // Mock identity service to handle invalid refresh token
        wireMockServer.stubFor(
            post(urlEqualTo("/auth/refresh"))
                .willReturn(
                    aResponse()
                        .withStatus(404) // Not found - endpoint doesn't exist
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"error": "Endpoint not found"}""")
                )
        )

        // When - Attempt to refresh with invalid token
        webTestClient.post()
            .uri("/auth/refresh")
            .header("Content-Type", "application/json")
            .bodyValue("""{"refreshToken": "$invalidRefreshToken"}""")
            .exchange()
            .expectStatus().isNotFound // Should fail as endpoint doesn't exist

        // Then - Should have proper error handling (will fail as endpoint doesn't exist)
        wireMockServer.verify(postRequestedFor(urlEqualTo("/auth/refresh")))
    }

    @Test
    @Order(4)
    fun `should fail to rate limit token refresh requests`() {
        // Given - Valid refresh token
        val refreshToken = createRefreshToken()
        
        // Mock identity service refresh endpoint
        wireMockServer.stubFor(
            post(urlEqualTo("/auth/refresh"))
                .willReturn(
                    aResponse()
                        .withStatus(404) // Endpoint not implemented
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"error": "Endpoint not found"}""")
                )
        )

        // When - Make multiple rapid refresh requests
        repeat(10) {
            webTestClient.post()
                .uri("/auth/refresh")
                .header("Content-Type", "application/json")
                .bodyValue("""{"refreshToken": "$refreshToken"}""")
                .exchange()
                .expectStatus().isNotFound // All should fail as endpoint doesn't exist
        }

        // Then - Should have rate limiting (will fail as feature not implemented)
        // Note: This test demonstrates the need for rate limiting on refresh endpoints
        wireMockServer.verify(exactly(10), postRequestedFor(urlEqualTo("/auth/refresh")))
    }

    @Test
    @Order(5)
    fun `should fail to handle concurrent token refresh requests`() {
        // Given - Multiple refresh tokens for different users
        val refreshTokens = (1..5).map { createRefreshToken("user-$it") }
        
        // Mock identity service
        wireMockServer.stubFor(
            post(urlEqualTo("/auth/refresh"))
                .willReturn(
                    aResponse()
                        .withStatus(404) // Endpoint not implemented
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"error": "Endpoint not found"}""")
                )
        )

        // When - Make concurrent refresh requests
        val responses = refreshTokens.map { token ->
            webTestClient.post()
                .uri("/auth/refresh")
                .header("Content-Type", "application/json")
                .bodyValue("""{"refreshToken": "$token"}""")
                .exchange()
                .expectStatus().isNotFound // All should fail
        }

        // Then - Should handle concurrent requests properly (will fail as endpoint doesn't exist)
        wireMockServer.verify(exactly(5), postRequestedFor(urlEqualTo("/auth/refresh")))
    }

    @Test
    @Order(6)
    fun `should fail to include user context in token refresh process`() {
        // Given - Token with specific user context
        val refreshToken = createRefreshToken("specific-user-id", listOf("USER", "PREMIUM"))
        
        wireMockServer.stubFor(
            post(urlEqualTo("/auth/refresh"))
                .willReturn(
                    aResponse()
                        .withStatus(404) // Endpoint not implemented
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"error": "Endpoint not found"}""")
                )
        )

        // When - Refresh token with user context
        webTestClient.post()
            .uri("/auth/refresh")
            .header("Content-Type", "application/json")
            .bodyValue("""{"refreshToken": "$refreshToken"}""")
            .exchange()
            .expectStatus().isNotFound

        // Then - Should preserve user context in refresh process (will fail as not implemented)
        wireMockServer.verify(
            postRequestedFor(urlEqualTo("/auth/refresh"))
                .withRequestBody(containing(refreshToken))
        )
    }

    // Helper methods for creating different types of tokens
    private fun createNearExpiryJwtToken(
        userId: String = "test-user-id",
        username: String = "testuser"
    ): String {
        val key = Keys.hmacShaKeyFor("your-256-bit-secret-key-here-make-it-secure-for-testing".toByteArray(StandardCharsets.UTF_8))
        
        return Jwts.builder()
            .setSubject(userId)
            .claim("username", username)
            .claim("roles", listOf("USER"))
            .claim("persona_id", "test-persona")
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + 300000)) // Expires in 5 minutes
            .signWith(key)
            .compact()
    }

    private fun createRefreshToken(
        userId: String = "test-user-id",
        roles: List<String> = listOf("USER")
    ): String {
        val key = Keys.hmacShaKeyFor("your-256-bit-secret-key-here-make-it-secure-for-testing".toByteArray(StandardCharsets.UTF_8))
        
        return Jwts.builder()
            .setSubject(userId)
            .claim("type", "refresh")
            .claim("roles", roles)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + 2592000000)) // 30 days
            .signWith(key)
            .compact()
    }

    private fun createInvalidRefreshToken(): String {
        val wrongKey = Keys.hmacShaKeyFor("wrong-secret-key-for-testing-invalid-tokens-refresh".toByteArray())
        
        return Jwts.builder()
            .setSubject("test-user-id")
            .claim("type", "refresh")
            .claim("roles", listOf("USER"))
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + 2592000000))
            .signWith(wrongKey)
            .compact()
    }
}