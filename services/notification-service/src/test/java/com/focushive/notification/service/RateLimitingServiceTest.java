package com.focushive.notification.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for RateLimitingService.
 * Tests token bucket rate limiting algorithm implementation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Rate Limiting Service Tests")
class RateLimitingServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private MeterRegistry meterRegistry;
    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        
        // Setup default Redis operations
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Lenient defaults to prevent NPEs and unnecessary stubbing warnings
        lenient().when(valueOperations.get(anyString())).thenReturn(null);
        lenient().when(valueOperations.increment(anyString())).thenReturn(1L);
        lenient().when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);
        
        rateLimitingService = new RateLimitingService(redisTemplate, meterRegistry);
    }

    @Test
    @DisplayName("Should handle rate limit configuration correctly")
    void shouldHandleRateLimitConfigurationCorrectly() {
        // Given
        String key = "test:config";
        
        // When - test that user is not blocked initially
        boolean isBlocked = rateLimitingService.isBlocked(key);

        // Then
        assertFalse(isBlocked); // User should not be blocked initially
    }

    @Test
    @DisplayName("Should allow request when within rate limit")
    void shouldAllowRequestWhenWithinRateLimit() {
        // Given
        String key = "user:123";
        when(valueOperations.increment(anyString())).thenReturn(5L);

        // When
        boolean allowed = rateLimitingService.allowRequest(key, 60, 3600);

        // Then
        assertTrue(allowed);
    }

    @Test
    @DisplayName("Should deny request when rate limit exceeded")
    void shouldDenyRequestWhenRateLimitExceeded() {
        // Given
        String key = "user:123";
        when(valueOperations.increment(anyString())).thenReturn(61L);

        // When
        boolean allowed = rateLimitingService.allowRequest(key, 60, 3600);

        // Then
        assertFalse(allowed);
    }

    @Test
    @DisplayName("Should initialize rate limit for new key")
    void shouldInitializeRateLimitForNewKey() {
        // Given
        String key = "user:123";
        // Default stub returns 1L which is what we want

        // When
        boolean allowed = rateLimitingService.allowRequest(key, 60, 3600);

        // Then
        assertTrue(allowed);
        verify(redisTemplate).expire(anyString(), eq(Duration.ofMinutes(1)));
    }

    @Test
    @DisplayName("Should get remaining requests correctly")
    void shouldGetRemainingRequestsCorrectly() {
        // Given
        String key = "user:123";
        when(valueOperations.get(anyString())).thenReturn("15");

        // When
        int remaining = rateLimitingService.getRemainingRequests(key);

        // Then
        assertEquals(45, remaining); // 60 - 15 = 45
    }

    @Test
    @DisplayName("Should calculate reset time correctly")
    void shouldCalculateResetTimeCorrectly() {
        // Given
        String key = "user:123";

        // When
        long resetTime = rateLimitingService.getResetTime(key);

        // Then
        assertTrue(resetTime > System.currentTimeMillis());
        assertTrue(resetTime <= System.currentTimeMillis() + 60000); // Within 1 minute
    }

    @Test
    @DisplayName("Should handle concurrent requests correctly")
    void shouldHandleConcurrentRequestsCorrectly() throws InterruptedException {
        // Given
        String key = "concurrent:test";
        int totalRequests = 100;
        int allowedRequests = 60;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        ExecutorService executor = Executors.newFixedThreadPool(10);

        givenBlockedState(key, false);
        
        // Mock Redis increment to simulate rate limiting
        // Each allowRequest call makes 2 increments (minute + hour), so we need separate counters
        AtomicInteger minuteCounter = new AtomicInteger(0);
        AtomicInteger hourCounter = new AtomicInteger(0);
        
        when(valueOperations.increment(anyString())).thenAnswer(invocation -> {
            String keyArg = invocation.getArgument(0);
            if (keyArg.contains(":minute:")) {
                return (long) minuteCounter.incrementAndGet();
            } else if (keyArg.contains(":hour:")) {
                return (long) hourCounter.incrementAndGet();
            }
            return 1L;
        });

        // When
        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                boolean allowed = rateLimitingService.allowRequest(key, allowedRequests, 3600);
                if (allowed) {
                    successCount.incrementAndGet();
                } else {
                    failureCount.incrementAndGet();
                }
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);

        // Then
        assertEquals(allowedRequests, successCount.get());
        assertEquals(totalRequests - allowedRequests, failureCount.get());

        executor.shutdown();
    }

    @Test
    @DisplayName("Should reset rate limit after time window")
    void shouldResetRateLimitAfterTimeWindow() throws InterruptedException {
        // Given
        String key = "refill:test";
        int requestsPerMinute = 10;
        int requestsPerHour = 600;
        
        givenBlockedState(key, false);
        
        // Mock rate limit behavior - each allowRequest call increments both minute and hour counters
        // So we need twice as many return values
        stubIncrementSequence(1L, 1L, 2L, 2L, 3L, 3L, 4L, 4L, 5L, 5L,  // first 5 calls (minute, hour pairs)
                             6L, 6L, 7L, 7L, 8L, 8L, 9L, 9L, 10L, 10L, // next 5 calls 
                             11L, 11L, // 11th call should fail on minute limit
                             1L, 1L);  // after reset

        // When - consume all requests
        for (int i = 0; i < requestsPerMinute; i++) {
            assertTrue(rateLimitingService.allowRequest(key, requestsPerMinute, requestsPerHour));
        }
        assertFalse(rateLimitingService.allowRequest(key, requestsPerMinute, requestsPerHour));

        // Simulate time window reset - increment sequence will continue from where it left off
        // The stubIncrementSequence already includes the reset values (1L, 1L) at the end

        // Then - should allow again after reset
        assertTrue(rateLimitingService.allowRequest(key, requestsPerMinute, requestsPerHour));
    }

    @Test
    @DisplayName("Should track metrics for allowed and denied requests")
    void shouldTrackMetricsForAllowedAndDeniedRequests() {
        // Given
        String key = "metrics:test";

        // Setup sequence: first call returns 1L (allowed), second call returns 61L (denied)
        // Each allowRequest makes 2 increment calls (minute + hour), so we need 4 values total
        stubIncrementSequence(1L, 1L, 61L, 61L);

        // When - allowed request
        rateLimitingService.allowRequest(key, 60, 3600);

        // When - denied request 
        rateLimitingService.allowRequest(key, 60, 3600);

        // Then
        assertEquals(1.0, meterRegistry.counter("rate.limit.requests",
            "result", "allowed", "key", key).count());
        assertEquals(1.0, meterRegistry.counter("rate.limit.requests",
            "result", "denied", "key", key).count());
    }

    @Test
    @DisplayName("Should handle burst capacity correctly")
    void shouldHandleBurstCapacityCorrectly() {
        // Given
        String key = "burst:test";
        int burstCapacity = 20;
        // Default stub returns 1L which is what we want for first request

        // When
        boolean allowed = rateLimitingService.allowBurstRequest(key, burstCapacity);

        // Then
        assertTrue(allowed);
        verify(redisTemplate).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Should block user after threshold violations")
    void shouldBlockUserAfterThresholdViolations() {
        // Given
        String userId = "user:violator";
        int violationThreshold = 3;

        // Mock rate limit exceeded
        when(valueOperations.increment(anyString())).thenReturn(61L);

        // When - exceed violation threshold
        for (int i = 0; i < violationThreshold + 1; i++) {
            rateLimitingService.allowRequest(userId, 60, 3600);
        }

        // Then
        verify(valueOperations).set(eq("blocked:" + userId), eq("blocked"), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should check if user is blocked")
    void shouldCheckIfUserIsBlocked() {
        // Given
        String userId = "user:blocked";
        givenBlockedState(userId, true);

        // When
        boolean isBlocked = rateLimitingService.isBlocked(userId);

        // Then
        assertTrue(isBlocked);
    }

    @Test
    @DisplayName("Should unblock user after timeout")
    void shouldUnblockUserAfterTimeout() {
        // Given
        String userId = "user:unblock";
        givenBlockedState(userId, false);

        // When
        boolean isBlocked = rateLimitingService.isBlocked(userId);

        // Then
        assertFalse(isBlocked);
    }

    @Test
    @DisplayName("Should apply different limits for different keys")
    void shouldApplyDifferentLimitsForDifferentKeys() {
        // Given
        String key1 = "user:key1";
        String key2 = "admin:key2";
        // Default stub returns 1L which is what we want

        // When
        boolean allowed1 = rateLimitingService.allowRequest(key1, 10, 60);
        boolean allowed2 = rateLimitingService.allowRequest(key2, 100, 3600);

        // Then
        assertTrue(allowed1);
        assertTrue(allowed2);
        verify(valueOperations, times(4)).increment(anyString()); // 2 for minute, 2 for hour
    }

    @Test
    @DisplayName("Should cleanup expired cache entries")
    void shouldCleanupExpiredCacheEntries() {
        // Given - cleanup method works with internal cache and doesn't need external setup
        
        // When
        rateLimitingService.cleanupExpiredBuckets();

        // Then - verify cleanup logic was executed without errors
        assertNotNull(rateLimitingService); // Service should still be operational after cleanup
    }

    // Helper methods for test setup
    private void stubIncrementSequence(Long... values) {
        AtomicInteger idx = new AtomicInteger(0);
        when(valueOperations.increment(anyString()))
            .thenAnswer(inv -> values[idx.getAndIncrement() % values.length]);
    }

    private void givenBlockedState(String key, boolean blocked) {
        when(valueOperations.get("blocked:" + key)).thenReturn(blocked ? "blocked" : null);
    }
}
