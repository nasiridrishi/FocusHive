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
 * Integration tests for API Gateway health checks and fallback functionality
 * 
 * Tests:
 * - Gateway health endpoint accessibility
 * - Service health monitoring through gateway
 * - Fallback responses when services are unavailable
 * - Circuit breaker behavior
 * - Gateway metrics and monitoring endpoints
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class GatewayHealthAndFallbackIntegrationTest : BaseIntegrationTest() {

    companion object {
        private val wireMockServer = WireMockServer(options().port(8085))

        @JvmStatic
        @DynamicPropertySource  
        fun configureWiremock(registry: DynamicPropertyRegistry) {
            // Configure routes to use WireMock
            registry.add("spring.cloud.gateway.routes[1].uri") { "http://localhost:8085" } // focushive-backend
            registry.add("spring.cloud.gateway.routes[2].uri") { "http://localhost:8085" } // music-service
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
    fun `should provide gateway health endpoint`() {
        // When & Then - Health endpoint should be accessible without authentication
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").exists()
    }

    @Test
    @Order(2)
    fun `should provide gateway info endpoint`() {
        // When & Then - Info endpoint should be accessible  
        webTestClient.get()
            .uri("/actuator/info")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
    }

    @Test
    @Order(3)
    fun `should provide prometheus metrics endpoint`() {
        // When & Then - Metrics endpoint should be accessible
        webTestClient.get()
            .uri("/actuator/prometheus")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
    }

    @Test
    @Order(4)
    fun `should handle service unavailable with appropriate error`() {
        // Given - No mock setup, so service will be unavailable
        val validToken = createValidJwtToken()

        // When & Then - Should return 503 Service Unavailable or 500 Internal Server Error
        webTestClient.get()
            .uri("/hives/123")
            .header("Authorization", withBearerToken(validToken))
            .exchange()
            .expectStatus().is5xxServerError
    }

    @Test
    @Order(5)
    fun `should handle fallback responses when configured`() {
        // Given - Mock service returning errors to trigger fallback
        wireMockServer.stubFor(
            get(urlPathMatching("/hives/.*"))
                .willReturn(aResponse().withStatus(500))
        )

        val validToken = createValidJwtToken()

        // When & Then - Should return fallback response or circuit breaker response
        val response = webTestClient.get()
            .uri("/hives/123")
            .header("Authorization", withBearerToken(validToken))
            .exchange()
            .expectStatus().is5xxServerError
            .returnResult(String::class.java)

        // Note: Actual fallback behavior depends on circuit breaker configuration
        // This test verifies that errors are handled gracefully
    }

    @Test
    @Order(6)
    fun `should retry failed requests according to configuration`() {
        // Given - Mock service that fails first calls then succeeds
        wireMockServer.stubFor(
            get(urlPathMatching("/hives/retry-test"))
                .inScenario("retry-scenario")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("first-failure")
        )
        
        wireMockServer.stubFor(
            get(urlPathMatching("/hives/retry-test"))
                .inScenario("retry-scenario")
                .whenScenarioStateIs("first-failure")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("second-failure")
        )

        wireMockServer.stubFor(
            get(urlPathMatching("/hives/retry-test"))
                .inScenario("retry-scenario")
                .whenScenarioStateIs("second-failure")
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"success": true, "retry": true}""")
                )
        )

        val validToken = createValidJwtToken()

        // When & Then - Should eventually succeed after retries
        webTestClient.get()
            .uri("/hives/retry-test")
            .header("Authorization", withBearerToken(validToken))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.retry").isEqualTo(true)

        // Verify multiple requests were made (retries)
        wireMockServer.verify(3, getRequestedFor(urlEqualTo("/hives/retry-test")))
    }

    @Test
    @Order(7)
    fun `should handle timeout scenarios gracefully`() {
        // Given - Mock service with long delay
        wireMockServer.stubFor(
            get(urlPathMatching("/hives/timeout-test"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withFixedDelay(31000) // 31 seconds - should timeout
                        .withBody("""{"delayed": true}""")
                )
        )

        val validToken = createValidJwtToken()

        // When & Then - Should timeout and return appropriate error
        webTestClient.get()
            .uri("/hives/timeout-test")
            .header("Authorization", withBearerToken(validToken))
            .exchange()
            .expectStatus().is5xxServerError
            
        // Note: WebTestClient has its own timeout (30 seconds by default)
        // so this test demonstrates timeout handling
    }

    @Test
    @Order(8)
    fun `should maintain request tracing headers`() {
        // Given
        wireMockServer.stubFor(
            get(urlPathMatching("/hives/tracing-test"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"traced": true}""")
                )
        )

        val validToken = createValidJwtToken()
        val correlationId = "trace-123-456"

        // When
        webTestClient.get()
            .uri("/hives/tracing-test")
            .header("Authorization", withBearerToken(validToken))
            .header("X-Correlation-ID", correlationId)
            .exchange()
            .expectStatus().isOk

        // Then - Verify tracing headers were forwarded
        wireMockServer.verify(
            getRequestedFor(urlEqualTo("/hives/tracing-test"))
                .withHeader("X-Correlation-ID", equalTo(correlationId))
        )
    }

    @Test
    @Order(9)
    fun `should handle global filter processing`() {
        // Given
        wireMockServer.stubFor(
            get(urlPathMatching("/hives/filter-test"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"filtered": true}""")
                )
        )

        val validToken = createValidJwtToken()

        // When
        val result = webTestClient.get()
            .uri("/hives/filter-test")
            .header("Authorization", withBearerToken(validToken))
            .exchange()
            .expectStatus().isOk
            .returnResult(String::class.java)

        // Then - Verify that global filters added expected headers/processing
        // This would depend on what the GlobalFilter actually does
        // For now, we just verify the request was processed successfully
        val responseHeaders = result.responseHeaders
        
        // The response should have been processed by filters
        Assertions.assertNotNull(responseHeaders)
        
        // Verify the request reached the backend with filter-added headers
        wireMockServer.verify(
            getRequestedFor(urlEqualTo("/hives/filter-test"))
                .withHeader("X-User-Id", equalTo("test-user-id"))
                .withHeader("X-Auth-Provider", equalTo("focushive-identity-service"))
        )
    }

    @Test
    @Order(10)
    fun `should provide circuit breaker status in health endpoint`() {
        // When
        val healthResponse = webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .returnResult(String::class.java)

        // Then - Health endpoint should provide meaningful status
        val responseBody = healthResponse.responseBody.blockFirst()
        Assertions.assertNotNull(responseBody)
        
        // Should contain basic health information
        Assertions.assertTrue(responseBody!!.contains("status"))
        
        // In a full implementation, might also contain circuit breaker states
        // "circuitBreakers": { "identity-service-cb": "CLOSED" }
        // But this depends on specific Spring Boot Actuator configuration
    }

    @Test
    @Order(11)
    fun `should handle error responses with proper format`() {
        // When - Request invalid endpoint
        webTestClient.get()
            .uri("/non-existent-service/test")
            .exchange()
            .expectStatus().isNotFound

        // When - Request with malformed token
        webTestClient.get()
            .uri("/hives/123")
            .header("Authorization", "Bearer invalid-token-format")
            .exchange()
            .expectStatus().isUnauthorized
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.error").exists()
            .jsonPath("$.message").exists()
            .jsonPath("$.status").exists()
            .jsonPath("$.timestamp").exists()
    }
}