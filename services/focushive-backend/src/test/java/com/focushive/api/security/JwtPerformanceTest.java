package com.focushive.api.security;

import com.focushive.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Performance Tests for JWT Token Validation Implementation.
 *
 * These tests verify that the JWT validation system meets the specified
 * performance requirements under various load conditions.
 *
 * Performance Requirements:
 * - Token validation < 10ms per request
 * - Cache hit < 1ms
 * - Blacklist check < 2ms
 * - Support 1000+ concurrent validations/second
 */
@ExtendWith(MockitoExtension.class)
class JwtPerformanceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private JwtTokenProvider tokenProvider;
    private JwtTokenBlacklistService blacklistService;
    private JwtCacheService cacheService;
    private User testUser;
    private String testToken;

    @BeforeEach
    void setUp() {
        // Setup Redis template mocks with realistic response times
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Mock fast Redis responses (simulate < 1ms network latency)
        when(setOperations.isMember(anyString(), anyString())).thenReturn(false);
        when(valueOperations.get(anyString())).thenReturn(null);

        // Create services
        blacklistService = new JwtTokenBlacklistService(redisTemplate);
        cacheService = new JwtCacheService(redisTemplate);

        // Create token provider with all services
        tokenProvider = new JwtTokenProvider(
            "test-jwt-signing-key-at-least-256-bits-long-for-validation",
            3600000L,
            Optional.of(blacklistService),
            Optional.of(cacheService)
        );

        // Create test user and token
        testUser = new User();
        testUser.setId("test-user-id");
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setRole(User.UserRole.USER);

        testToken = tokenProvider.generateTokenWithJti(testUser);
    }

    @Test
    void shouldMeetSingleValidationPerformanceRequirement() {
        // Given - Fresh token (no cache)
        when(valueOperations.get(anyString())).thenReturn(null);
        when(setOperations.isMember(anyString(), anyString())).thenReturn(false);

        // When - Measure single validation time
        long startTime = System.nanoTime();
        boolean isValid = tokenProvider.validateTokenWithBlacklist(testToken);
        long endTime = System.nanoTime();

        // Then
        assertThat(isValid).isTrue();
        long durationMs = (endTime - startTime) / 1_000_000;
        assertThat(durationMs).isLessThan(10L); // < 10ms requirement
    }

    @Test
    void shouldMeetCacheHitPerformanceRequirement() {
        // Given - Token validation is cached
        when(valueOperations.get(anyString())).thenReturn("valid");

        // When - Measure cached validation time
        long startTime = System.nanoTime();
        boolean isValid = tokenProvider.validateTokenWithBlacklist(testToken);
        long endTime = System.nanoTime();

        // Then
        assertThat(isValid).isTrue();
        long durationMs = (endTime - startTime) / 1_000_000;
        assertThat(durationMs).isLessThan(1L); // < 1ms requirement for cache hits
    }

    @Test
    void shouldMeetBlacklistCheckPerformanceRequirement() {
        // Given - Token is blacklisted
        when(valueOperations.get(anyString())).thenReturn(null); // No cache
        when(setOperations.isMember(anyString(), anyString())).thenReturn(true); // Blacklisted

        // When - Measure blacklist rejection time
        long startTime = System.nanoTime();
        boolean isValid = tokenProvider.validateTokenWithBlacklist(testToken);
        long endTime = System.nanoTime();

        // Then
        assertThat(isValid).isFalse();
        long durationMs = (endTime - startTime) / 1_000_000;
        assertThat(durationMs).isLessThan(2L); // < 2ms requirement for blacklist check
    }

    @Test
    void shouldHandleHighThroughputValidation() {
        // Given
        int numberOfRequests = 1000;
        int numberOfThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<Future<Boolean>> futures = new ArrayList<>();

        // Mock responses for high throughput
        when(valueOperations.get(anyString())).thenReturn("valid"); // Cache hit for speed

        // When - Submit concurrent validation requests
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numberOfRequests; i++) {
            Future<Boolean> future = executor.submit(() ->
                tokenProvider.validateTokenWithBlacklist(testToken)
            );
            futures.add(future);
        }

        // Wait for all requests to complete
        List<Boolean> results = new ArrayList<>();
        for (Future<Boolean> future : futures) {
            try {
                results.add(future.get(1, TimeUnit.SECONDS));
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new RuntimeException("Request failed", e);
            }
        }

        long endTime = System.currentTimeMillis();
        executor.shutdown();

        // Then
        assertThat(results).hasSize(numberOfRequests);
        assertThat(results).allMatch(result -> result); // All validations successful

        long totalTimeMs = endTime - startTime;
        double requestsPerSecond = (numberOfRequests * 1000.0) / totalTimeMs;

        assertThat(requestsPerSecond).isGreaterThan(1000.0); // > 1000 RPS requirement
    }

    @Test
    void shouldMaintainPerformanceUnderMixedLoad() {
        // This test simulates realistic mixed load:
        // - 70% cache hits
        // - 20% full validations
        // - 10% blacklist rejections

        int totalRequests = 500;
        List<Long> responseTimes = new ArrayList<>();

        // Setup mixed scenarios
        for (int i = 0; i < totalRequests; i++) {
            long startTime = System.nanoTime();

            if (i % 10 < 7) {
                // 70% cache hits
                when(valueOperations.get(anyString())).thenReturn("valid");
                tokenProvider.validateTokenWithBlacklist(testToken);
            } else if (i % 10 < 9) {
                // 20% full validations
                when(valueOperations.get(anyString())).thenReturn(null);
                when(setOperations.isMember(anyString(), anyString())).thenReturn(false);
                tokenProvider.validateTokenWithBlacklist(testToken);
            } else {
                // 10% blacklist rejections
                when(valueOperations.get(anyString())).thenReturn(null);
                when(setOperations.isMember(anyString(), anyString())).thenReturn(true);
                tokenProvider.validateTokenWithBlacklist(testToken);
            }

            long endTime = System.nanoTime();
            responseTimes.add((endTime - startTime) / 1_000_000);
        }

        // Analyze performance statistics
        double averageTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long maxTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0L);
        long p95Time = responseTimes.stream()
            .sorted()
            .skip((long) (responseTimes.size() * 0.95))
            .findFirst()
            .orElse(0L);

        // Performance assertions
        assertThat(averageTime).isLessThan(5.0); // Average < 5ms
        assertThat(maxTime).isLessThan(10L); // Max < 10ms
        assertThat(p95Time).isLessThan(8L); // 95th percentile < 8ms
    }

    @Test
    void shouldScaleWithTokenComplexity() {
        // Test performance with different token complexities

        // Simple token (minimal claims)
        User simpleUser = new User();
        simpleUser.setId("1");
        simpleUser.setEmail("u@x.com");
        simpleUser.setUsername("u");
        simpleUser.setRole(User.UserRole.USER);

        // Complex token (many claims)
        User complexUser = new User();
        complexUser.setId("very-long-user-id-with-complex-structure-12345");
        complexUser.setEmail("complex.user.with.long.email@example.domain.com");
        complexUser.setUsername("complex_username_with_underscores_and_numbers_123");
        complexUser.setRole(User.UserRole.ADMIN);

        String simpleToken = tokenProvider.generateTokenWithJti(simpleUser);
        String complexToken = tokenProvider.generateTokenWithJti(complexUser);

        when(valueOperations.get(anyString())).thenReturn(null);
        when(setOperations.isMember(anyString(), anyString())).thenReturn(false);

        // Measure simple token validation
        long simpleStart = System.nanoTime();
        boolean simpleValid = tokenProvider.validateTokenWithBlacklist(simpleToken);
        long simpleEnd = System.nanoTime();
        long simpleTime = (simpleEnd - simpleStart) / 1_000_000;

        // Measure complex token validation
        long complexStart = System.nanoTime();
        boolean complexValid = tokenProvider.validateTokenWithBlacklist(complexToken);
        long complexEnd = System.nanoTime();
        long complexTime = (complexEnd - complexStart) / 1_000_000;

        // Both should be valid and meet performance requirements
        assertThat(simpleValid).isTrue();
        assertThat(complexValid).isTrue();
        assertThat(simpleTime).isLessThan(10L);
        assertThat(complexTime).isLessThan(10L);

        // Complex tokens shouldn't be significantly slower
        assertThat(complexTime - simpleTime).isLessThan(3L);
    }

    @Test
    void shouldHandleMemoryEfficiently() {
        // This test checks memory efficiency by validating many different tokens

        int numberOfTokens = 1000;
        List<String> tokens = new ArrayList<>();

        // Generate many tokens
        for (int i = 0; i < numberOfTokens; i++) {
            User user = new User();
            user.setId("user-" + i);
            user.setEmail("user" + i + "@example.com");
            user.setUsername("user" + i);
            user.setRole(User.UserRole.USER);

            tokens.add(tokenProvider.generateTokenWithJti(user));
        }

        when(valueOperations.get(anyString())).thenReturn(null);
        when(setOperations.isMember(anyString(), anyString())).thenReturn(false);

        // Measure memory before
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // Validate all tokens
        long startTime = System.currentTimeMillis();
        for (String token : tokens) {
            tokenProvider.validateTokenWithBlacklist(token);
        }
        long endTime = System.currentTimeMillis();

        // Measure memory after
        System.gc(); // Suggest garbage collection
        Thread.yield(); // Give GC a chance to run
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();

        long memoryUsed = memoryAfter - memoryBefore;
        long totalTime = endTime - startTime;
        double averageTimePerToken = (double) totalTime / numberOfTokens;

        // Performance assertions
        assertThat(averageTimePerToken).isLessThan(10.0); // < 10ms per token
        assertThat(memoryUsed).isLessThan(10 * 1024 * 1024); // < 10MB for 1000 tokens
        assertThat(memoryUsed / numberOfTokens).isLessThan(1024 * 10); // < 10KB per token
    }

    @Test
    void shouldRecoverFromHighLoadGracefully() {
        // Test behavior under extreme load and recovery

        int highLoadRequests = 2000;
        int normalLoadRequests = 100;
        List<Long> highLoadTimes = new ArrayList<>();
        List<Long> recoveryTimes = new ArrayList<>();

        when(valueOperations.get(anyString())).thenReturn(null);
        when(setOperations.isMember(anyString(), anyString())).thenReturn(false);

        // High load phase
        for (int i = 0; i < highLoadRequests; i++) {
            long startTime = System.nanoTime();
            tokenProvider.validateTokenWithBlacklist(testToken);
            long endTime = System.nanoTime();
            highLoadTimes.add((endTime - startTime) / 1_000_000);
        }

        // Brief pause to simulate load reduction
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Recovery phase
        for (int i = 0; i < normalLoadRequests; i++) {
            long startTime = System.nanoTime();
            tokenProvider.validateTokenWithBlacklist(testToken);
            long endTime = System.nanoTime();
            recoveryTimes.add((endTime - startTime) / 1_000_000);
        }

        // Analyze performance during high load
        double highLoadAverage = highLoadTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double recoveryAverage = recoveryTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);

        // System should maintain acceptable performance even under high load
        assertThat(highLoadAverage).isLessThan(15.0); // Allow some degradation under high load
        assertThat(recoveryAverage).isLessThan(10.0); // Should recover to normal performance

        // Recovery should be better than or similar to high load
        assertThat(recoveryAverage).isLessThanOrEqualTo(highLoadAverage * 1.1);
    }
}