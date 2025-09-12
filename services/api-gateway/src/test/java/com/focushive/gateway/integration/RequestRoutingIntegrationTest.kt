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
 * Integration tests for API Gateway request routing functionality
 * 
 * Tests:
 * - Route matching based on path patterns
 * - Request forwarding to correct services
 * - Path rewriting and prefix stripping
 * - Headers preservation during routing
 * - Error handling for unreachable services
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class RequestRoutingIntegrationTest : BaseIntegrationTest() {

    companion object {
        private val wireMockServer = WireMockServer(options().port(8089))

        @JvmStatic
        @DynamicPropertySource
        fun configureWiremock(registry: DynamicPropertyRegistry) {
            // Override route URIs to point to WireMock server
            registry.add("spring.cloud.gateway.routes[0].uri") { "http://localhost:8089" }
            registry.add("spring.cloud.gateway.routes[1].uri") { "http://localhost:8089" }
            registry.add("spring.cloud.gateway.routes[2].uri") { "http://localhost:8089" }
            registry.add("spring.cloud.gateway.routes[3].uri") { "http://localhost:8089" }
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
    fun `should route identity service requests correctly`() {
        // Given
        wireMockServer.stubFor(
            get(urlPathMatching("/auth/.*"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"service": "identity", "path": "auth"}""")
                )
        )

        // When & Then
        webTestClient.get()
            .uri("/auth/login")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.service").isEqualTo("identity")
            .jsonPath("$.path").isEqualTo("auth")

        // Verify the call was made to the backend
        wireMockServer.verify(getRequestedFor(urlEqualTo("/auth/login")))
    }

    @Test
    @Order(2)
    fun `should route OAuth2 requests to identity service`() {
        // Given
        wireMockServer.stubFor(
            get(urlPathMatching("/oauth2/.*"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"service": "identity", "path": "oauth2"}""")
                )
        )

        // When & Then
        webTestClient.get()
            .uri("/oauth2/authorize")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.service").isEqualTo("identity")

        wireMockServer.verify(getRequestedFor(urlEqualTo("/oauth2/authorize")))
    }

    @Test
    @Order(3)
    fun `should route protected endpoints with authentication headers`() {
        // Given
        val validToken = createValidJwtToken()
        
        wireMockServer.stubFor(
            get(urlPathMatching("/hives/.*"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"service": "focushive-backend", "path": "hives"}""")
                )
        )

        // When & Then
        webTestClient.get()
            .uri("/hives/123")
            .header("Authorization", withBearerToken(validToken))
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)

        // Verify the request was forwarded with user context headers
        wireMockServer.verify(
            getRequestedFor(urlEqualTo("/hives/123"))
                .withHeader("X-User-Id", equalTo("test-user-id"))
                .withHeader("X-Username", equalTo("testuser"))
                .withHeader("X-User-Roles", equalTo("USER"))
                .withHeader("X-Persona-Id", equalTo("test-persona"))
        )
    }

    @Test
    @Order(4)
    fun `should route music service requests correctly`() {
        // Given
        val validToken = createValidJwtToken()
        
        wireMockServer.stubFor(
            get(urlPathMatching("/music/.*"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"service": "music-service", "path": "music"}""")
                )
        )

        // When & Then
        webTestClient.get()
            .uri("/music/playlists")
            .header("Authorization", withBearerToken(validToken))
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.service").isEqualTo("music-service")

        wireMockServer.verify(getRequestedFor(urlEqualTo("/music/playlists")))
    }

    @Test
    @Order(5)
    fun `should route health check requests without authentication`() {
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

        // When & Then
        webTestClient.get()
            .uri("/health/gateway")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP")

        wireMockServer.verify(getRequestedFor(urlEqualTo("/health/gateway")))
    }

    @Test
    @Order(6)
    fun `should preserve request headers during routing`() {
        // Given
        val validToken = createValidJwtToken()
        val correlationId = "test-correlation-123"
        
        wireMockServer.stubFor(
            post(urlPathMatching("/hives"))
                .willReturn(
                    aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"id": "new-hive-123"}""")
                )
        )

        // When & Then
        webTestClient.post()
            .uri("/hives")
            .header("Authorization", withBearerToken(validToken))
            .header("X-Correlation-ID", correlationId)
            .header("Content-Type", "application/json")
            .bodyValue("""{"name": "Test Hive", "description": "Test Description"}""")
            .exchange()
            .expectStatus().isCreated

        // Verify headers were preserved
        wireMockServer.verify(
            postRequestedFor(urlEqualTo("/hives"))
                .withHeader("X-Correlation-ID", equalTo(correlationId))
                .withHeader("Content-Type", equalTo("application/json"))
                .withHeader("X-User-Id", equalTo("test-user-id"))
        )
    }

    @Test
    @Order(7)
    fun `should handle different HTTP methods correctly`() {
        // Given
        val validToken = createValidJwtToken()
        
        wireMockServer.stubFor(
            put(urlPathMatching("/hives/.*"))
                .willReturn(aResponse().withStatus(200).withBody("""{"updated": true}"""))
        )
        
        wireMockServer.stubFor(
            delete(urlPathMatching("/hives/.*"))
                .willReturn(aResponse().withStatus(204))
        )

        // Test PUT request
        webTestClient.put()
            .uri("/hives/123")
            .header("Authorization", withBearerToken(validToken))
            .header("Content-Type", "application/json")
            .bodyValue("""{"name": "Updated Hive"}""")
            .exchange()
            .expectStatus().isOk

        // Test DELETE request
        webTestClient.delete()
            .uri("/hives/456")
            .header("Authorization", withBearerToken(validToken))
            .exchange()
            .expectStatus().isNoContent

        // Verify both requests were routed
        wireMockServer.verify(putRequestedFor(urlEqualTo("/hives/123")))
        wireMockServer.verify(deleteRequestedFor(urlEqualTo("/hives/456")))
    }

    @Test
    @Order(8)
    fun `should return 404 for unmatched routes`() {
        // When & Then - Request to non-existent route
        webTestClient.get()
            .uri("/non-existent-service/test")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    @Order(9)
    fun `should handle service unavailable scenarios`() {
        // Given - Stop WireMock to simulate service unavailability
        wireMockServer.stop()

        val validToken = createValidJwtToken()

        // When & Then - Should trigger circuit breaker or return error
        webTestClient.get()
            .uri("/hives/123")
            .header("Authorization", withBearerToken(validToken))
            .exchange()
            .expectStatus().is5xxServerError

        // Restart WireMock for cleanup
        wireMockServer.start()
    }

    @Test
    @Order(10)
    fun `should route requests with query parameters correctly`() {
        // Given
        val validToken = createValidJwtToken()
        
        wireMockServer.stubFor(
            get(urlPathMatching("/hives"))
                .withQueryParam("page", equalTo("1"))
                .withQueryParam("size", equalTo("10"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"content": [], "totalElements": 0}""")
                )
        )

        // When & Then
        webTestClient.get()
            .uri("/hives?page=1&size=10")
            .header("Authorization", withBearerToken(validToken))
            .exchange()
            .expectStatus().isOk

        // Verify query parameters were preserved
        wireMockServer.verify(
            getRequestedFor(urlEqualTo("/hives"))
                .withQueryParam("page", equalTo("1"))
                .withQueryParam("size", equalTo("10"))
        )
    }
}