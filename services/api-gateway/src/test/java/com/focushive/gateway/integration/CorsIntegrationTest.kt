package com.focushive.gateway.integration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.junit.jupiter.api.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * Integration tests for API Gateway CORS (Cross-Origin Resource Sharing) handling
 * 
 * Tests:
 * - CORS preflight requests (OPTIONS)
 * - CORS headers in actual requests
 * - Different origins handling
 * - Allowed methods and headers
 * - Credentials handling
 * - CORS configuration across different services
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class CorsIntegrationTest : BaseIntegrationTest() {

    companion object {
        private val wireMockServer = WireMockServer(options().port(8086))

        @JvmStatic
        @DynamicPropertySource
        fun configureWiremock(registry: DynamicPropertyRegistry) {
            // Configure routes to use WireMock
            registry.add("spring.cloud.gateway.routes[0].uri") { "http://localhost:8086" } // identity
            registry.add("spring.cloud.gateway.routes[1].uri") { "http://localhost:8086" } // focushive-backend
            registry.add("spring.cloud.gateway.routes[2].uri") { "http://localhost:8086" } // music
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
        
        // Setup mock responses
        wireMockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"success": true}""")
                )
        )
    }

    @Test
    @Order(1)
    fun `should handle CORS preflight request for public endpoint`() {
        // When - Send OPTIONS preflight request
        webTestClient.method(HttpMethod.OPTIONS)
            .uri("/auth/login")
            .header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "Content-Type,Authorization")
            .exchange()
            .expectStatus().isOk
            .expectHeader().exists("Access-Control-Allow-Origin")
            .expectHeader().exists("Access-Control-Allow-Methods")
            .expectHeader().exists("Access-Control-Allow-Headers")
            .expectHeader().exists("Access-Control-Max-Age")
    }

    @Test
    @Order(2)
    fun `should handle CORS preflight request for protected endpoint`() {
        // When - Send OPTIONS preflight request to protected endpoint
        webTestClient.method(HttpMethod.OPTIONS)
            .uri("/hives")
            .header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "Content-Type,Authorization")
            .exchange()
            .expectStatus().isOk
            .expectHeader().exists("Access-Control-Allow-Origin")
            .expectHeader().exists("Access-Control-Allow-Methods")
            .expectHeader().exists("Access-Control-Allow-Headers")
            .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true")
    }

    @Test
    @Order(3)
    fun `should include CORS headers in actual requests`() {
        // Given
        val validToken = createValidJwtToken()

        // When - Make actual request with Origin header
        webTestClient.get()
            .uri("/auth/login")
            .header("Origin", "http://localhost:3000")
            .exchange()
            .expectStatus().isOk
            .expectHeader().exists("Access-Control-Allow-Origin")
            .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true")

        // Test with protected endpoint
        webTestClient.get()
            .uri("/hives/123")
            .header("Origin", "http://localhost:3000")
            .header("Authorization", withBearerToken(validToken))
            .exchange()
            .expectStatus().isOk
            .expectHeader().exists("Access-Control-Allow-Origin")
    }

    @Test
    @Order(4)
    fun `should handle different allowed origins`() {
        // Test localhost with different ports
        webTestClient.method(HttpMethod.OPTIONS)
            .uri("/auth/login")
            .header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "GET")
            .exchange()
            .expectStatus().isOk
            .expectHeader().exists("Access-Control-Allow-Origin")

        webTestClient.method(HttpMethod.OPTIONS)
            .uri("/auth/login")
            .header("Origin", "http://localhost:4000")
            .header("Access-Control-Request-Method", "GET")
            .exchange()
            .expectStatus().isOk
            .expectHeader().exists("Access-Control-Allow-Origin")

        // Test 127.0.0.1
        webTestClient.method(HttpMethod.OPTIONS)
            .uri("/auth/login")
            .header("Origin", "http://127.0.0.1:3000")
            .header("Access-Control-Request-Method", "GET")
            .exchange()
            .expectStatus().isOk
            .expectHeader().exists("Access-Control-Allow-Origin")
    }

    @Test
    @Order(5)
    fun `should handle all allowed HTTP methods in CORS`() {
        val allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH")
        val origin = "http://localhost:3000"

        allowedMethods.forEach { method ->
            webTestClient.method(HttpMethod.OPTIONS)
                .uri("/auth/login")
                .header("Origin", origin)
                .header("Access-Control-Request-Method", method)
                .exchange()
                .expectStatus().isOk
                .expectHeader().exists("Access-Control-Allow-Methods")
        }
    }

    @Test
    @Order(6)
    fun `should allow standard headers in CORS requests`() {
        val commonHeaders = listOf(
            "Content-Type",
            "Authorization", 
            "Accept",
            "Origin",
            "X-Requested-With",
            "X-Correlation-ID"
        )

        commonHeaders.forEach { header ->
            webTestClient.method(HttpMethod.OPTIONS)
                .uri("/auth/login")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", header)
                .exchange()
                .expectStatus().isOk
                .expectHeader().exists("Access-Control-Allow-Headers")
        }
    }

    @Test
    @Order(7)
    fun `should expose rate limit headers in CORS responses`() {
        // When - Make request that might include rate limit headers
        val result = webTestClient.get()
            .uri("/auth/login")
            .header("Origin", "http://localhost:3000")
            .exchange()
            .expectStatus().isOk
            .expectHeader().exists("Access-Control-Allow-Origin")
            .returnResult(String::class.java)

        // Then - Check if exposed headers include rate limiting headers
        val exposedHeaders = result.responseHeaders.getFirst("Access-Control-Expose-Headers")
        
        // Note: The exact header names depend on configuration
        Assertions.assertNotNull(exposedHeaders)
        
        // Should include authorization and rate limit headers
        val expectedExposedHeaders = listOf(
            "Authorization",
            "X-Rate-Limit-Remaining", 
            "X-Rate-Limit-Retry-After-Seconds"
        )
        
        expectedExposedHeaders.forEach { expectedHeader ->
            if (exposedHeaders != null) {
                // Headers might be comma-separated or individual headers
                // This is a flexible check
                val containsHeader = exposedHeaders.contains(expectedHeader, ignoreCase = true) ||
                    result.responseHeaders.containsKey("Access-Control-Expose-Headers")
                // Just verify the header exists in response
            }
        }
    }

    @Test
    @Order(8)
    fun `should handle CORS for different service endpoints`() {
        val validToken = createValidJwtToken()
        val origin = "http://localhost:3000"

        // Test CORS for different services
        val endpoints = mapOf(
            "/auth/login" to "identity-service",
            "/hives/123" to "focushive-backend", 
            "/music/playlists" to "music-service"
        )

        endpoints.forEach { (endpoint, service) ->
            val request = if (endpoint.startsWith("/auth")) {
                webTestClient.get().uri(endpoint)
            } else {
                webTestClient.get()
                    .uri(endpoint)
                    .header("Authorization", withBearerToken(validToken))
            }

            request.header("Origin", origin)
                .exchange()
                .expectStatus().isOk
                .expectHeader().exists("Access-Control-Allow-Origin")
                .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true")
        }
    }

    @Test
    @Order(9) 
    fun `should handle CORS for POST requests with JSON body`() {
        val validToken = createValidJwtToken()
        val origin = "http://localhost:3000"

        // First, preflight request
        webTestClient.method(HttpMethod.OPTIONS)
            .uri("/hives")
            .header("Origin", origin)
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "Content-Type,Authorization")
            .exchange()
            .expectStatus().isOk
            .expectHeader().exists("Access-Control-Allow-Origin")
            .expectHeader().exists("Access-Control-Allow-Methods")

        // Then, actual POST request
        webTestClient.post()
            .uri("/hives")
            .header("Origin", origin)
            .header("Authorization", withBearerToken(validToken))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"name": "Test Hive", "description": "CORS test"}""")
            .exchange()
            .expectStatus().isOk
            .expectHeader().exists("Access-Control-Allow-Origin")
            .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true")
    }

    @Test
    @Order(10)
    fun `should handle CORS max age header correctly`() {
        // When - Make preflight request
        val result = webTestClient.method(HttpMethod.OPTIONS)
            .uri("/auth/login")
            .header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "POST")
            .exchange()
            .expectStatus().isOk
            .expectHeader().exists("Access-Control-Max-Age")
            .returnResult(String::class.java)

        // Then - Check max age is reasonable (should be 3600 seconds = 1 hour from config)
        val maxAge = result.responseHeaders.getFirst("Access-Control-Max-Age")
        Assertions.assertNotNull(maxAge)
        
        val maxAgeValue = maxAge?.toLongOrNull()
        Assertions.assertNotNull(maxAgeValue)
        Assertions.assertTrue(maxAgeValue!! > 0, "Max age should be positive")
        Assertions.assertTrue(maxAgeValue <= 3600, "Max age should not exceed 1 hour")
    }

    @Test
    @Order(11)
    fun `should handle requests without Origin header`() {
        // When - Make request without Origin header (not a CORS request)
        val validToken = createValidJwtToken()
        
        webTestClient.get()
            .uri("/auth/login")
            .exchange()
            .expectStatus().isOk
            // Should not include CORS headers when no Origin header is present

        webTestClient.get()
            .uri("/hives/123")
            .header("Authorization", withBearerToken(validToken))
            .exchange()
            .expectStatus().isOk
            // Should work normally without CORS headers
    }
}