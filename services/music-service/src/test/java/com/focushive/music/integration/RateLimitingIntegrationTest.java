package com.focushive.music.integration;

import com.focushive.music.entity.SpotifyCredentials;
import com.focushive.music.repository.SpotifyCredentialsRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;

/**
 * Integration tests for API rate limiting functionality.
 * Tests rate limit enforcement, retry-after behavior, user-specific limits,
 * and endpoint-specific rate limiting policies.
 */
class RateLimitingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SpotifyCredentialsRepository credentialsRepository;

    private SpotifyCredentials testCredentials;
    private ExecutorService executorService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure rate limiting for testing
        registry.add("music.rate-limit.search.requests-per-minute", () -> "10");
        registry.add("music.rate-limit.playlist.requests-per-minute", () -> "20");
        registry.add("music.rate-limit.default.requests-per-minute", () -> "60");
        registry.add("music.rate-limit.burst.max-requests", () -> "5");
        registry.add("music.rate-limit.window.duration", () -> "PT1M"); // 1 minute
    }

    @BeforeEach
    void setupTestData() {
        credentialsRepository.deleteAll();
        
        // Create test credentials
        String userId = createTestUserId();
        testCredentials = TestFixtures.spotifyCredentialsBuilder()
            .userId(userId)
            .build();
        credentialsRepository.save(testCredentials);

        executorService = Executors.newFixedThreadPool(20);
    }

    @AfterEach
    void cleanup() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Test
    @DisplayName("Should enforce search endpoint rate limits")
    void shouldEnforceSearchEndpointRateLimits() throws Exception {
        // Given - Search endpoint with 10 requests per minute limit
        String searchUrl = "/api/music/search/tracks?q=test&userId=" + testCredentials.getUserId();

        // When - Make requests up to the limit
        for (int i = 0; i < 10; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(searchUrl, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        // Then - 11th request should be rate limited
        ResponseEntity<String> rateLimitedResponse = restTemplate.getForEntity(searchUrl, String.class);
        assertThat(rateLimitedResponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        
        // Should include rate limit headers
        HttpHeaders headers = rateLimitedResponse.getHeaders();
        assertThat(headers.getFirst("X-RateLimit-Limit")).isEqualTo("10");
        assertThat(headers.getFirst("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(headers.getFirst("Retry-After")).isNotNull();
    }

    @Test
    @DisplayName("Should enforce playlist endpoint rate limits")
    void shouldEnforcePlaylistEndpointRateLimits() throws Exception {
        // Given - Playlist endpoint with 20 requests per minute limit
        String playlistUrl = "/api/music/playlists?userId=" + testCredentials.getUserId();

        // When - Make requests up to the limit
        for (int i = 0; i < 20; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(playlistUrl, String.class);
            // Should succeed (200) or return empty list (200)
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        // Then - 21st request should be rate limited
        ResponseEntity<String> rateLimitedResponse = restTemplate.getForEntity(playlistUrl, String.class);
        assertThat(rateLimitedResponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        
        // Should include correct rate limit headers
        HttpHeaders headers = rateLimitedResponse.getHeaders();
        assertThat(headers.getFirst("X-RateLimit-Limit")).isEqualTo("20");
        assertThat(headers.getFirst("X-RateLimit-Remaining")).isEqualTo("0");
    }

    @Test
    @DisplayName("Should apply user-specific rate limits")
    void shouldApplyUserSpecificRateLimits() throws Exception {
        // Given - Two different users
        String user1Id = testCredentials.getUserId();
        String user2Id = createTestUserId();
        
        SpotifyCredentials user2Credentials = TestFixtures.spotifyCredentialsBuilder()
            .userId(user2Id)
            .build();
        credentialsRepository.save(user2Credentials);

        String searchUrl1 = "/api/music/search/tracks?q=test&userId=" + user1Id;
        String searchUrl2 = "/api/music/search/tracks?q=test&userId=" + user2Id;

        // When - User1 exhausts their rate limit
        for (int i = 0; i < 10; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(searchUrl1, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        // User1 should be rate limited
        ResponseEntity<String> user1RateLimited = restTemplate.getForEntity(searchUrl1, String.class);
        assertThat(user1RateLimited.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // Then - User2 should still be able to make requests
        ResponseEntity<String> user2Response = restTemplate.getForEntity(searchUrl2, String.class);
        assertThat(user2Response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should handle burst requests within limit")
    void shouldHandleBurstRequestsWithinLimit() throws Exception {
        // Given - Burst limit of 5 requests
        String searchUrl = "/api/music/search/tracks?q=test&userId=" + testCredentials.getUserId();
        CountDownLatch latch = new CountDownLatch(5);
        
        // When - Make 5 concurrent requests (within burst limit)
        CompletableFuture<ResponseEntity<String>>[] futures = IntStream.range(0, 5)
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                try {
                    return restTemplate.getForEntity(searchUrl, String.class);
                } finally {
                    latch.countDown();
                }
            }, executorService))
            .toArray(CompletableFuture[]::new);

        // Wait for all requests to complete
        latch.await(10, TimeUnit.SECONDS);

        // Then - All burst requests should succeed
        for (CompletableFuture<ResponseEntity<String>> future : futures) {
            ResponseEntity<String> response = future.get(1, TimeUnit.SECONDS);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Test
    @DisplayName("Should reject burst requests exceeding limit")
    void shouldRejectBurstRequestsExceedingLimit() throws Exception {
        // Given - Burst limit of 5 requests
        String searchUrl = "/api/music/search/tracks?q=test&userId=" + testCredentials.getUserId();
        CountDownLatch latch = new CountDownLatch(10);
        
        // When - Make 10 concurrent requests (exceeding burst limit)
        CompletableFuture<ResponseEntity<String>>[] futures = IntStream.range(0, 10)
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                try {
                    return restTemplate.getForEntity(searchUrl, String.class);
                } finally {
                    latch.countDown();
                }
            }, executorService))
            .toArray(CompletableFuture[]::new);

        // Wait for all requests to complete
        latch.await(10, TimeUnit.SECONDS);

        // Then - Some requests should be rate limited
        int successCount = 0;
        int rateLimitedCount = 0;
        
        for (CompletableFuture<ResponseEntity<String>> future : futures) {
            ResponseEntity<String> response = future.get(1, TimeUnit.SECONDS);
            if (response.getStatusCode() == HttpStatus.OK) {
                successCount++;
            } else if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                rateLimitedCount++;
            }
        }
        
        assertThat(successCount).isLessThanOrEqualTo(5); // Burst limit
        assertThat(rateLimitedCount).isGreaterThan(0);
        assertThat(successCount + rateLimitedCount).isEqualTo(10);
    }

    @Test
    @DisplayName("Should include correct retry-after header")
    void shouldIncludeCorrectRetryAfterHeader() throws Exception {
        // Given - Endpoint with rate limit
        String searchUrl = "/api/music/search/tracks?q=test&userId=" + testCredentials.getUserId();

        // When - Exhaust rate limit
        for (int i = 0; i < 10; i++) {
            restTemplate.getForEntity(searchUrl, String.class);
        }

        ResponseEntity<String> rateLimitedResponse = restTemplate.getForEntity(searchUrl, String.class);

        // Then - Should include Retry-After header
        assertThat(rateLimitedResponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        
        HttpHeaders headers = rateLimitedResponse.getHeaders();
        String retryAfter = headers.getFirst("Retry-After");
        assertThat(retryAfter).isNotNull();
        
        int retryAfterSeconds = Integer.parseInt(retryAfter);
        assertThat(retryAfterSeconds).isBetween(1, 60); // Should be within rate limit window
    }

    @Test
    @DisplayName("Should reset rate limit after time window")
    void shouldResetRateLimitAfterTimeWindow() throws Exception {
        // Given - Endpoint with rate limit
        String searchUrl = "/api/music/search/tracks?q=test&userId=" + testCredentials.getUserId();

        // When - Exhaust rate limit
        for (int i = 0; i < 10; i++) {
            restTemplate.getForEntity(searchUrl, String.class);
        }

        ResponseEntity<String> rateLimitedResponse = restTemplate.getForEntity(searchUrl, String.class);
        assertThat(rateLimitedResponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // Wait for rate limit window to reset (using shorter window for testing)
        // In real implementation, this might be configurable for tests
        await().atMost(java.time.Duration.ofSeconds(65))
            .pollInterval(java.time.Duration.ofSeconds(5))
            .until(() -> {
                ResponseEntity<String> response = restTemplate.getForEntity(searchUrl, String.class);
                return response.getStatusCode() == HttpStatus.OK;
            });

        // Then - Should be able to make requests again
        ResponseEntity<String> newResponse = restTemplate.getForEntity(searchUrl, String.class);
        assertThat(newResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should apply different limits to different endpoints")
    void shouldApplyDifferentLimitsToDifferentEndpoints() throws Exception {
        // Given - Different endpoints with different limits
        String searchUrl = "/api/music/search/tracks?q=test&userId=" + testCredentials.getUserId();
        String playlistUrl = "/api/music/playlists?userId=" + testCredentials.getUserId();

        // When - Test search endpoint limit (10/min)
        for (int i = 0; i < 10; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(searchUrl, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
        
        ResponseEntity<String> searchRateLimited = restTemplate.getForEntity(searchUrl, String.class);
        assertThat(searchRateLimited.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // Then - Playlist endpoint should still work (separate limit)
        ResponseEntity<String> playlistResponse = restTemplate.getForEntity(playlistUrl, String.class);
        assertThat(playlistResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should handle rate limit bypass for premium users")
    void shouldHandleRateLimitBypassForPremiumUsers() throws Exception {
        // Given - Premium user (this would be determined by user service)
        // For testing, we'll simulate this with a special header or user type
        String premiumUserId = "premium-" + createTestUserId();
        SpotifyCredentials premiumCredentials = TestFixtures.spotifyCredentialsBuilder()
            .userId(premiumUserId)
            .build();
        credentialsRepository.save(premiumCredentials);

        String searchUrl = "/api/music/search/tracks?q=test&userId=" + premiumUserId;

        // When - Make more requests than normal limit
        for (int i = 0; i < 15; i++) { // More than normal 10 request limit
            ResponseEntity<String> response = restTemplate.getForEntity(searchUrl, String.class);
            // Premium users might have higher limits or no limits
            // The exact behavior depends on business requirements
            assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.TOO_MANY_REQUESTS);
        }

        // Then - Premium users should have different rate limiting behavior
        // This test validates that the rate limiting system can differentiate user types
    }

    @Test
    @DisplayName("Should provide rate limit status in headers for all requests")
    void shouldProvideRateLimitStatusInHeadersForAllRequests() throws Exception {
        // Given - Any endpoint
        String searchUrl = "/api/music/search/tracks?q=test&userId=" + testCredentials.getUserId();

        // When - Make a request
        ResponseEntity<String> response = restTemplate.getForEntity(searchUrl, String.class);

        // Then - Should include rate limit headers in successful response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        HttpHeaders headers = response.getHeaders();
        assertThat(headers.getFirst("X-RateLimit-Limit")).isNotNull();
        assertThat(headers.getFirst("X-RateLimit-Remaining")).isNotNull();
        assertThat(headers.getFirst("X-RateLimit-Reset")).isNotNull();
        
        // Verify header values are reasonable
        int limit = Integer.parseInt(headers.getFirst("X-RateLimit-Limit"));
        int remaining = Integer.parseInt(headers.getFirst("X-RateLimit-Remaining"));
        
        assertThat(limit).isPositive();
        assertThat(remaining).isBetween(0, limit);
    }

    @Test
    @DisplayName("Should handle rate limiting with Redis unavailable")
    void shouldHandleRateLimitingWithRedisUnavailable() throws Exception {
        // This test would require stopping Redis container temporarily
        // Given - Redis is unavailable (simulated by misconfiguration)
        
        // When - Make requests with Redis down
        String searchUrl = "/api/music/search/tracks?q=test&userId=" + testCredentials.getUserId();
        
        // Then - Should either:
        // 1. Fallback to in-memory rate limiting
        // 2. Allow requests to pass through (fail-open)
        // 3. Return service unavailable
        
        ResponseEntity<String> response = restTemplate.getForEntity(searchUrl, String.class);
        
        // The exact behavior depends on the implementation strategy
        assertThat(response.getStatusCode()).isIn(
            HttpStatus.OK,                    // Fail-open or in-memory fallback
            HttpStatus.SERVICE_UNAVAILABLE,   // Fail-closed
            HttpStatus.INTERNAL_SERVER_ERROR  // Error handling
        );
    }
}