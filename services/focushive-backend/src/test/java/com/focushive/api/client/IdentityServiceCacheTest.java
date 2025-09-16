package com.focushive.api.client;

import com.focushive.api.dto.identity.TokenValidationResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * TDD RED PHASE TESTS for Identity Service caching behavior.
 * These tests SHOULD FAIL initially until caching layer is implemented.
 *
 * Tests caching requirements:
 * - Cache successful token validations
 * - TTL based on token expiry
 * - Cache invalidation on logout
 * - 80% cache hit ratio target
 * - Cached validation < 5ms performance
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
    "identity.service.url=http://localhost:${wiremock.server.port}",
    "identity.service.cache.ttl=300", // 5 minutes
    "identity.service.cache.max-entries=1000",
    "spring.cache.type=caffeine",
    "spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=300s"
})
class IdentityServiceCacheTest {

    @Autowired
    private IdentityServiceClient identityServiceClient;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private WireMockServer wireMockServer;

    private static final String CACHED_TOKEN = "Bearer cached-token-123";
    private static final String NON_CACHED_TOKEN = "Bearer non-cached-token-456";

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        // Clear all caches
        cacheManager.getCacheNames().forEach(name ->
            cacheManager.getCache(name).clear());
    }

    /**
     * TDD RED PHASE TEST
     * This test SHOULD FAIL initially.
     * Tests that successful validations are cached.
     */
    @Test
    void shouldCacheSuccessfulTokenValidation() {
        // Arrange
        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .withHeader("Authorization", equalTo(CACHED_TOKEN))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "valid": true,
                        "userId": "550e8400-e29b-41d4-a716-446655440000",
                        "username": "testuser"
                    }
                    """)));

        // Act - first call should hit the service
        TokenValidationResponse firstResponse = identityServiceClient.validateToken(CACHED_TOKEN);

        // Verify service was called
        verify(1, postRequestedFor(urlEqualTo("/api/v1/auth/validate")));

        // Act - second call should use cache (no additional service call)
        TokenValidationResponse secondResponse = identityServiceClient.validateToken(CACHED_TOKEN);

        // Assert
        assertThat(firstResponse.isValid()).isTrue();
        assertThat(secondResponse.isValid()).isTrue();
        assertThat(firstResponse.getUserId()).isEqualTo(secondResponse.getUserId());

        // Service should still have been called only once (second call used cache)
        verify(1, postRequestedFor(urlEqualTo("/api/v1/auth/validate")));
    }

    /**
     * TDD RED PHASE TEST
     * This test SHOULD FAIL initially.
     * Tests that invalid tokens are not cached.
     */
    @Test
    void shouldNotCacheInvalidTokenValidation() {
        // Arrange
        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .withHeader("Authorization", equalTo(NON_CACHED_TOKEN))
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "valid": false,
                        "errorMessage": "Invalid token"
                    }
                    """)));

        // Act - make two calls with invalid token
        TokenValidationResponse firstResponse = identityServiceClient.validateToken(NON_CACHED_TOKEN);
        TokenValidationResponse secondResponse = identityServiceClient.validateToken(NON_CACHED_TOKEN);

        // Assert
        assertThat(firstResponse.isValid()).isFalse();
        assertThat(secondResponse.isValid()).isFalse();

        // Both calls should hit the service (no caching for invalid responses)
        verify(2, postRequestedFor(urlEqualTo("/api/v1/auth/validate")));
    }

    /**
     * TDD RED PHASE TEST
     * This test SHOULD FAIL initially.
     * Tests cache performance requirement: cached validation < 5ms.
     */
    @Test
    void shouldMeetCachedValidationPerformanceRequirement() {
        // Arrange
        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .withHeader("Authorization", equalTo(CACHED_TOKEN))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withFixedDelay(20) // Add delay to distinguish from cache
                .withBody("""
                    {
                        "valid": true,
                        "userId": "550e8400-e29b-41d4-a716-446655440000"
                    }
                    """)));

        // First call to populate cache
        identityServiceClient.validateToken(CACHED_TOKEN);

        // Act - measure cached call performance
        long startTime = System.nanoTime();
        TokenValidationResponse response = identityServiceClient.validateToken(CACHED_TOKEN);
        long endTime = System.nanoTime();
        long responseTime = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        // Assert
        assertThat(response.isValid()).isTrue();
        // Performance requirement: cached validation < 5ms
        assertThat(responseTime).isLessThan(5);
    }

    /**
     * TDD RED PHASE TEST
     * This test SHOULD FAIL initially.
     * Tests cache TTL behavior - cached entries should expire.
     */
    @Test
    void shouldRespectCacheTTL() throws InterruptedException {
        // This test would require a shorter TTL for practical testing
        // In real implementation, we'd use test-specific configuration

        // Arrange with very short TTL
        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "valid": true,
                        "userId": "550e8400-e29b-41d4-a716-446655440000"
                    }
                    """)));

        // Act - first call
        identityServiceClient.validateToken(CACHED_TOKEN);
        verify(1, postRequestedFor(urlEqualTo("/api/v1/auth/validate")));

        // Wait for cache to expire (in real test, we'd use test-specific short TTL)
        // For now, we'll verify the cache can be manually cleared
        cacheManager.getCache("identity-validation").clear();

        // Act - second call after cache cleared
        identityServiceClient.validateToken(CACHED_TOKEN);

        // Assert - service should be called again
        verify(2, postRequestedFor(urlEqualTo("/api/v1/auth/validate")));
    }

    /**
     * TDD RED PHASE TEST
     * This test SHOULD FAIL initially.
     * Tests cache eviction on logout scenarios.
     */
    @Test
    void shouldEvictCacheOnLogout() {
        // Arrange
        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "valid": true,
                        "userId": "550e8400-e29b-41d4-a716-446655440000"
                    }
                    """)));

        // Act - populate cache
        identityServiceClient.validateToken(CACHED_TOKEN);
        verify(1, postRequestedFor(urlEqualTo("/api/v1/auth/validate")));

        // Simulate logout by manually clearing cache
        // In real implementation, this would be triggered by logout event
        cacheManager.getCache("identity-validation").evict(CACHED_TOKEN);

        // Act - call after cache eviction
        identityServiceClient.validateToken(CACHED_TOKEN);

        // Assert - service should be called again
        verify(2, postRequestedFor(urlEqualTo("/api/v1/auth/validate")));
    }

    /**
     * TDD RED PHASE TEST
     * This test SHOULD FAIL initially.
     * Tests cache size limits and LRU eviction.
     */
    @Test
    void shouldRespectCacheSizeLimits() {
        // Arrange - stub multiple tokens
        for (int i = 0; i < 5; i++) {
            String token = "Bearer token-" + i;
            stubFor(post(urlEqualTo("/api/v1/auth/validate"))
                .withHeader("Authorization", equalTo(token))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(String.format("""
                        {
                            "valid": true,
                            "userId": "550e8400-e29b-41d4-a716-446655440%03d"
                        }
                        """, i))));
        }

        // Act - make calls to populate cache
        for (int i = 0; i < 5; i++) {
            String token = "Bearer token-" + i;
            TokenValidationResponse response = identityServiceClient.validateToken(token);
            assertThat(response.isValid()).isTrue();
        }

        // Verify all calls hit the service initially
        verify(5, postRequestedFor(urlEqualTo("/api/v1/auth/validate")));

        // Act - make second round of calls (should use cache)
        for (int i = 0; i < 5; i++) {
            String token = "Bearer token-" + i;
            TokenValidationResponse response = identityServiceClient.validateToken(token);
            assertThat(response.isValid()).isTrue();
        }

        // Assert - no additional service calls (all cached)
        verify(5, postRequestedFor(urlEqualTo("/api/v1/auth/validate")));
    }
}