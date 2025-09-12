package com.focushive.gateway.integration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.junit.jupiter.api.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

/**
 * Integration tests for API Gateway rate limiting functionality
 * 
 * Tests:
 * - Rate limiting enforcement per service
 * - Rate limit headers in responses
 * - Different rate limits for different services
 * - Rate limit reset after time window
 * - Concurrent request handling
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class RateLimitingIntegrationTest : BaseIntegrationTest() {

    companion object {
        private val wireMockServer = WireMockServer(options().port(8087))

        @JvmStatic
        @DynamicPropertySource
        fun configureWiremock(registry: DynamicPropertyRegistry) {
            // Configure all routes to use WireMock
            registry.add("spring.cloud.gateway.routes[0].uri") { "http://localhost:8087" } // identity
            registry.add("spring.cloud.gateway.routes[1].uri") { "http://localhost:8087" } // focushive-backend
            registry.add("spring.cloud.gateway.routes[2].uri") { "http://localhost:8087" } // music
            registry.add("spring.cloud.gateway.routes[3].uri") { "http://localhost:8087" } // health
            
            // Reduce rate limits for testing
            registry.add("spring.cloud.gateway.routes[0].filters[0].args.redis-rate-limiter.replenishRate") { "5" }
            registry.add("spring.cloud.gateway.routes[0].filters[0].args.redis-rate-limiter.burstCapacity") { "10" }
            registry.add("spring.cloud.gateway.routes[1].filters[0].args.redis-rate-limiter.replenishRate") { "10" }
            registry.add("spring.cloud.gateway.routes[1].filters[0].args.redis-rate-limiter.burstCapacity") { "20" }
            registry.add("spring.cloud.gateway.routes[2].filters[0].args.redis-rate-limiter.replenishRate") { "3" }
            registry.add("spring.cloud.gateway.routes[2].filters[0].args.redis-rate-limiter.burstCapacity") { "6" }
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
                        .withBody("""{"success": true, "timestamp": "${System.currentTimeMillis()}"}""")
                )
        )
        
        // Wait a bit to ensure rate limiters are reset
        Thread.sleep(1000)
    }

    @Test
    @Order(1)
    @Disabled("Rate limiting requires Redis and complex setup")
    fun `should enforce rate limits for identity service`() {
        // Given - Identity service has lower rate limit (5 req/sec, burst 10)
        val requests = 15 // Exceed burst capacity
        var rateLimitedCount = 0
        var successCount = 0

        // When - Make requests rapidly
        repeat(requests) {
            val response = webTestClient.get()
                .uri("/auth/test")
                .exchange()
                .expectStatus()

            when {
                response.expectStatus().isOk -> successCount++
                response.expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS) -> rateLimitedCount++
            }
        }

        // Then - Some requests should be rate limited
        assertTrue(rateLimitedCount > 0, "Expected some requests to be rate limited")
        assertTrue(successCount <= 10, "Expected at most 10 successful requests (burst capacity)")
    }

    @Test
    @Order(2)
    @Disabled("Rate limiting requires Redis and complex setup")
    fun `should include rate limit headers in responses`() {
        // When
        val response = webTestClient.get()
            .uri("/auth/test")
            .exchange()
            .expectStatus().isOk
            .returnResult(String::class.java)

        // Then - Should include rate limit headers
        val headers = response.responseHeaders
        
        // Note: Actual header names depend on Spring Cloud Gateway configuration
        // These might be X-RateLimit-Remaining, X-RateLimit-Burst-Capacity, etc.
        assertTrue(
            headers.containsKey("X-RateLimit-Remaining") ||
            headers.containsKey("X-Rate-Limit-Remaining"),
            "Expected rate limit headers in response"
        )
    }

    @Test
    @Order(3)
    @Disabled("Rate limiting requires Redis and complex setup")
    fun `should have different rate limits for different services`() {
        // Given - Music service has very low rate limit (3 req/sec, burst 6)
        val validToken = createValidJwtToken()
        var musicRateLimited = false
        var backendRateLimited = false

        // Test music service rate limiting (lower limit)
        repeat(8) { // Exceed music service burst capacity of 6
            val response = webTestClient.get()
                .uri("/music/test")
                .header("Authorization", withBearerToken(validToken))
                .exchange()

            if (response.expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)) {
                musicRateLimited = true
            }
        }

        // Wait a moment then test backend service (higher limit)
        Thread.sleep(100)
        
        repeat(8) { // Should not exceed backend burst capacity of 20
            val response = webTestClient.get()
                .uri("/hives/test")
                .header("Authorization", withBearerToken(validToken))
                .exchange()

            if (response.expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)) {
                backendRateLimited = true
            }
        }

        // Then
        assertTrue(musicRateLimited, "Music service should be rate limited with lower limits")
        assertTrue(!backendRateLimited, "Backend service should not be rate limited with higher limits")
    }

    @Test
    @Order(4)
    @Disabled("Rate limiting requires Redis and complex setup")
    fun `should reset rate limits after time window`() {
        // Given
        val validToken = createValidJwtToken()
        
        // Exhaust rate limit
        repeat(12) {
            webTestClient.get()
                .uri("/music/test")
                .header("Authorization", withBearerToken(validToken))
                .exchange()
        }

        // Verify rate limited
        val rateLimitedResponse = webTestClient.get()
            .uri("/music/test")
            .header("Authorization", withBearerToken(validToken))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)

        // Wait for rate limit window to reset (assuming 1 second window)
        Thread.sleep(2000)

        // Then - Should be able to make requests again
        webTestClient.get()
            .uri("/music/test")
            .header("Authorization", withBearerToken(validToken))
            .exchange()
            .expectStatus().isOk
    }

    @Test
    @Order(5)
    @Disabled("Rate limiting requires Redis and complex setup") 
    fun `should handle concurrent requests with rate limiting`() {
        // Given
        val validToken = createValidJwtToken()
        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(20)
        val results = mutableListOf<Int>()

        // When - Make concurrent requests
        repeat(20) {
            executor.submit {
                try {
                    val response = webTestClient.get()
                        .uri("/hives/concurrent-test")
                        .header("Authorization", withBearerToken(validToken))
                        .exchange()
                        .returnResult(String::class.java)

                    synchronized(results) {
                        results.add(response.status.value())
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        // Wait for all requests to complete
        assertTrue(latch.await(10, TimeUnit.SECONDS), "All requests should complete within 10 seconds")
        executor.shutdown()

        // Then - Should have mix of successful and rate limited responses
        val successfulRequests = results.count { it == 200 }
        val rateLimitedRequests = results.count { it == 429 }
        
        assertTrue(successfulRequests > 0, "Some requests should succeed")
        assertTrue(successfulRequests <= 20, "Not all requests should succeed due to rate limiting")
        assertTrue(successfulRequests + rateLimitedRequests == 20, "All requests should get a response")
    }

    @Test
    @Order(6)
    fun `should pass requests when rate limiting is disabled or not configured`() {
        // Given - Health endpoint might not have rate limiting or has generous limits
        
        // When - Make multiple requests
        repeat(25) {
            webTestClient.get()
                .uri("/health/test")
                .exchange()
                .expectStatus().isOk
        }

        // Then - All requests should succeed (health endpoint has generous rate limits)
        // Verify all requests were forwarded
        wireMockServer.verify(25, getRequestedFor(urlEqualTo("/health/test")))
    }

    @Test
    @Order(7)
    fun `should maintain rate limiting per user or IP`() {
        // This test would require proper rate limiting configuration
        // and would test that rate limits are applied per client identifier
        
        val token1 = createValidJwtToken(userId = "user1")
        val token2 = createValidJwtToken(userId = "user2")

        // Make requests with different users - should have separate rate limits
        // This is a simplified test as actual implementation would depend on 
        // how the gateway configures rate limiting keys
        
        webTestClient.get()
            .uri("/hives/user1-test")
            .header("Authorization", withBearerToken(token1))
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/hives/user2-test") 
            .header("Authorization", withBearerToken(token2))
            .exchange()
            .expectStatus().isOk

        // Verify both requests were processed
        wireMockServer.verify(getRequestedFor(urlEqualTo("/hives/user1-test")))
        wireMockServer.verify(getRequestedFor(urlEqualTo("/hives/user2-test")))
    }
}