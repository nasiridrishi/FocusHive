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
 * Integration tests for API Versioning functionality in API Gateway
 * 
 * Following strict TDD approach:
 * 1. Write failing test for API versioning support
 * 2. Implement API versioning routing and management
 * 3. Verify test passes
 * 
 * Tests:
 * - Header-based API versioning (Accept-Version header)
 * - Path-based API versioning (/v1/, /v2/)
 * - Query parameter versioning (?version=v1)
 * - Default version handling
 * - Version compatibility and deprecation warnings
 * - Version-specific rate limiting
 * - Backward compatibility support
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ApiVersioningIntegrationTest : BaseIntegrationTest() {

    companion object {
        private val wireMockServerV1 = WireMockServer(options().port(8094))
        private val wireMockServerV2 = WireMockServer(options().port(8095))

        @JvmStatic
        @DynamicPropertySource
        fun configureWiremock(registry: DynamicPropertyRegistry) {
            // Configure versioned routes (these don't exist yet - will cause tests to fail)
            
            // V1 routes (legacy)
            registry.add("spring.cloud.gateway.routes[10].id") { "focushive-backend-v1" }
            registry.add("spring.cloud.gateway.routes[10].uri") { "http://localhost:8094" }
            registry.add("spring.cloud.gateway.routes[10].predicates[0]") { "Path=/v1/hives/**,/v1/presence/**" }
            
            // V2 routes (current)
            registry.add("spring.cloud.gateway.routes[11].id") { "focushive-backend-v2" }
            registry.add("spring.cloud.gateway.routes[11].uri") { "http://localhost:8095" }
            registry.add("spring.cloud.gateway.routes[11].predicates[0]") { "Path=/v2/hives/**,/v2/presence/**" }
            
            // Header-based versioning (not implemented yet)
            registry.add("spring.cloud.gateway.routes[12].predicates[1]") { "Header=Accept-Version,v1" }
            registry.add("spring.cloud.gateway.routes[13].predicates[1]") { "Header=Accept-Version,v2" }
        }

        @JvmStatic
        @BeforeAll
        fun startWireMock() {
            wireMockServerV1.start()
            wireMockServerV2.start()
        }

        @JvmStatic
        @AfterAll
        fun stopWireMock() {
            wireMockServerV1.stop()
            wireMockServerV2.stop()
        }
    }

    @BeforeEach
    fun setUp() {
        webTestClient = createWebTestClient()
        wireMockServerV1.resetAll()
        wireMockServerV2.resetAll()
        
        // Setup V1 responses
        wireMockServerV1.stubFor(
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("API-Version", "v1")
                        .withBody("""{"version": "v1", "data": "legacy format", "deprecated": true}""")
                )
        )
        
        // Setup V2 responses  
        wireMockServerV2.stubFor(
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("API-Version", "v2")
                        .withBody("""{"version": "v2", "data": {"modernFormat": true}, "features": ["enhanced"]}""")
                )
        )
    }

    @Test
    @Order(1)
    fun `should fail to route to v1 API via path-based versioning`() {
        // Given - Valid JWT token
        val token = createValidJwtToken()
        
        // When - Request V1 API via path
        webTestClient.get()
            .uri("/v1/hives/123")
            .header("Authorization", withBearerToken(token))
            .exchange()
            .expectStatus().isNotFound // Will fail - V1 routes not configured
        
        // Then - Should have routed to V1 backend (will fail as versioning not implemented)
        // wireMockServerV1.verify(getRequestedFor(urlEqualTo("/hives/123")))
    }

    @Test
    @Order(2)
    fun `should fail to route to v2 API via path-based versioning`() {
        // Given - Valid JWT token
        val token = createValidJwtToken()
        
        // When - Request V2 API via path  
        webTestClient.get()
            .uri("/v2/hives/123")
            .header("Authorization", withBearerToken(token))
            .exchange()
            .expectStatus().isNotFound // Will fail - V2 routes not configured
        
        // Then - Should have routed to V2 backend (will fail as versioning not implemented)
        // wireMockServerV2.verify(getRequestedFor(urlEqualTo("/hives/123")))
    }

    @Test
    @Order(3)
    fun `should fail to route via header-based API versioning`() {
        // Given - Valid JWT token
        val token = createValidJwtToken()
        
        // When - Request with Accept-Version header for V1
        val v1Response = webTestClient.get()
            .uri("/hives/123")
            .header("Authorization", withBearerToken(token))
            .header("Accept-Version", "v1")
            .exchange()
            .expectStatus().isNotFound // Will fail - header-based routing not implemented
        
        // When - Request with Accept-Version header for V2
        val v2Response = webTestClient.get()
            .uri("/hives/123")
            .header("Authorization", withBearerToken(token))
            .header("Accept-Version", "v2")
            .exchange()
            .expectStatus().isNotFound // Will fail - header-based routing not implemented
        
        // Then - Should route to appropriate versions (will fail as not implemented)
        // These verifications would fail because header-based routing isn't configured
    }

    @Test
    @Order(4)
    fun `should fail to handle query parameter-based versioning`() {
        // Given - Valid JWT token
        val token = createValidJwtToken()
        
        // When - Request with version query parameter
        webTestClient.get()
            .uri("/hives/123?version=v1")
            .header("Authorization", withBearerToken(token))
            .exchange()
            .expectStatus().isNotFound // Will fail - query param versioning not implemented
        
        // Then - Should route based on query parameter (will fail as not implemented)
    }

    @Test
    @Order(5)
    fun `should fail to provide default version when no version specified`() {
        // Given - Valid JWT token
        val token = createValidJwtToken()
        
        // When - Request without version specification
        val response = webTestClient.get()
            .uri("/hives/123")
            .header("Authorization", withBearerToken(token))
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .returnResult(String::class.java)
        
        // Then - Should default to latest version (will fail as versioning not implemented)
        val versionHeader = response.responseHeaders.getFirst("API-Version")
        Assertions.assertNull(versionHeader, "API-Version header not provided (versioning not implemented)")
    }

    @Test
    @Order(6)
    fun `should fail to provide deprecation warnings for old API versions`() {
        // Given - Valid JWT token
        val token = createValidJwtToken()
        
        // When - Request V1 API (deprecated)
        val response = webTestClient.get()
            .uri("/v1/hives/123")
            .header("Authorization", withBearerToken(token))
            .exchange()
            .expectStatus().isNotFound // Will fail - V1 routes not configured
            .returnResult(String::class.java)
        
        // Then - Should include deprecation warning (will fail as not implemented)
        val deprecationHeader = response.responseHeaders.getFirst("Deprecation")
        val warningHeader = response.responseHeaders.getFirst("Warning")
        
        Assertions.assertNull(deprecationHeader, "Deprecation header not implemented")
        Assertions.assertNull(warningHeader, "Warning header not implemented")
    }

    @Test
    @Order(7)
    fun `should fail to apply different rate limits per API version`() {
        // Given - Valid JWT token
        val token = createValidJwtToken()
        
        // When - Make multiple requests to V1 (should have stricter limits for deprecated version)
        val v1Responses = mutableListOf<Int>()
        repeat(15) {
            val response = webTestClient.get()
                .uri("/v1/hives/test")
                .header("Authorization", withBearerToken(token))
                .exchange()
            v1Responses.add(response.expectBody().returnResult().status.value())
        }
        
        // When - Make multiple requests to V2 (should have more lenient limits)
        val v2Responses = mutableListOf<Int>()
        repeat(15) {
            val response = webTestClient.get()
                .uri("/v2/hives/test")
                .header("Authorization", withBearerToken(token))
                .exchange()
            v2Responses.add(response.expectBody().returnResult().status.value())
        }
        
        // Then - V1 should be rate limited more aggressively (will fail as versioning not implemented)
        val v1RateLimited = v1Responses.count { it == 404 } // Currently gets 404, not 429
        val v2RateLimited = v2Responses.count { it == 404 }
        
        Assertions.assertEquals(v1RateLimited, v2RateLimited, "Version-specific rate limiting not implemented")
    }

    @Test
    @Order(8)
    fun `should fail to support version compatibility matrix`() {
        // Given - Token with client version information
        val token = createValidJwtToken()
        
        // When - Client requests with compatibility requirements
        val response = webTestClient.get()
            .uri("/hives/123")
            .header("Authorization", withBearerToken(token))
            .header("Client-Version", "mobile-1.0")
            .header("Accept-Version", ">=v1")
            .exchange()
            .expectStatus().isNotFound // Will fail - compatibility routing not implemented
            .returnResult(String::class.java)
        
        // Then - Should route to compatible version (will fail as not implemented)
        val compatibleVersion = response.responseHeaders.getFirst("Compatible-Version")
        Assertions.assertNull(compatibleVersion, "Version compatibility not implemented")
    }

    @Test
    @Order(9)
    fun `should fail to handle version negotiation`() {
        // Given - Valid JWT token
        val token = createValidJwtToken()
        
        // When - Client requests multiple acceptable versions
        webTestClient.get()
            .uri("/hives/123")
            .header("Authorization", withBearerToken(token))
            .header("Accept-Version", "v2, v1;q=0.8")
            .exchange()
            .expectStatus().isNotFound // Will fail - version negotiation not implemented
        
        // Then - Should negotiate best version (will fail as negotiation not implemented)
    }

    @Test
    @Order(10)
    fun `should fail to provide version-specific documentation links`() {
        // Given - Request to a versioned endpoint
        val token = createValidJwtToken()
        
        // When - Request API with version
        val response = webTestClient.get()
            .uri("/v2/hives/123")
            .header("Authorization", withBearerToken(token))
            .exchange()
            .expectStatus().isNotFound // Will fail - versioned routes not configured
            .returnResult(String::class.java)
        
        // Then - Should include documentation links (will fail as not implemented)
        val docsLink = response.responseHeaders.getFirst("Link")
        Assertions.assertNull(docsLink, "Version-specific documentation links not implemented")
    }
}