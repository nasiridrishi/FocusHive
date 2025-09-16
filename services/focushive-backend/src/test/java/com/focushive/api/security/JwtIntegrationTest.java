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
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration Tests for JWT Token Validation with Blacklist and Caching.
 *
 * These tests verify the complete JWT validation flow including:
 * - Token validation with blacklist checking
 * - Performance caching integration
 * - Error handling and fail-safe behavior
 * - Performance requirements compliance
 */
@ExtendWith(MockitoExtension.class)
class JwtIntegrationTest {

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

    @BeforeEach
    void setUp() {
        // Setup Redis template mocks
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

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

        // Create test user
        testUser = new User();
        testUser.setId("test-user-id");
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setRole(User.UserRole.USER);
    }

    @Test
    void shouldValidateTokenWithFullIntegration() {
        // Given
        String token = tokenProvider.generateTokenWithJti(testUser);

        // Mock Redis responses - token not blacklisted, not cached
        when(setOperations.isMember("jwt:blacklist", token)).thenReturn(false);
        when(valueOperations.get(anyString())).thenReturn(null);

        // When
        boolean isValid = tokenProvider.validateTokenWithBlacklist(token);

        // Then
        assertThat(isValid).isTrue();

        // Verify blacklist was checked
        verify(setOperations).isMember("jwt:blacklist", token);

        // Verify successful validation was cached
        verify(valueOperations).set(anyString(), eq("valid"), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    void shouldRejectBlacklistedToken() {
        // Given
        String token = tokenProvider.generateTokenWithJti(testUser);

        // Mock Redis responses - token is blacklisted
        when(setOperations.isMember("jwt:blacklist", token)).thenReturn(true);

        // When
        boolean isValid = tokenProvider.validateTokenWithBlacklist(token);

        // Then
        assertThat(isValid).isFalse();

        // Verify blacklist was checked
        verify(setOperations).isMember("jwt:blacklist", token);

        // Verify no caching was attempted
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void shouldUseCachedValidation() {
        // Given
        String token = tokenProvider.generateTokenWithJti(testUser);

        // Mock Redis responses - token validation is cached
        when(valueOperations.get(anyString())).thenReturn("valid");

        // When
        boolean isValid = tokenProvider.validateTokenWithBlacklist(token);

        // Then
        assertThat(isValid).isTrue();

        // Verify cache was checked first
        verify(valueOperations).get(anyString());

        // Verify blacklist check was skipped due to cache hit
        verify(setOperations, never()).isMember(anyString(), anyString());
    }

    @Test
    void shouldInvalidateTokenAndClearCache() {
        // Given
        String token = tokenProvider.generateTokenWithJti(testUser);

        // When
        tokenProvider.invalidateToken(token);

        // Then
        // Verify token was added to blacklist
        verify(setOperations).add("jwt:blacklist", token);
        verify(redisTemplate).expire(eq("jwt:blacklist"), anyLong(), eq(TimeUnit.SECONDS));

        // Verify cache was invalidated
        verify(redisTemplate).delete(anyString());
    }

    @Test
    void shouldHandleRedisFailureGracefully() {
        // Given
        String token = tokenProvider.generateTokenWithJti(testUser);

        // Mock Redis failure
        when(setOperations.isMember(anyString(), anyString()))
            .thenThrow(new RuntimeException("Redis connection error"));

        // When - Should not throw exception
        boolean isValid = tokenProvider.validateTokenWithBlacklist(token);

        // Then - Should fail-safe (reject token for security)
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldMeetPerformanceRequirements() {
        // Given
        String token = tokenProvider.generateTokenWithJti(testUser);

        // Mock fast Redis responses
        when(setOperations.isMember("jwt:blacklist", token)).thenReturn(false);
        when(valueOperations.get(anyString())).thenReturn(null);

        // When - Measure validation performance
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 10; i++) {
            tokenProvider.validateTokenWithBlacklist(token);
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        long averageTime = totalTime / 10;

        // Then - Should meet <10ms requirement per validation
        assertThat(averageTime).isLessThan(10L);
    }

    @Test
    void shouldExtractJtiFromTokenWithJti() {
        // Given
        String token = tokenProvider.generateTokenWithJti(testUser);

        // When
        String jti = tokenProvider.extractJti(token);

        // Then
        assertThat(jti).isNotNull();
        assertThat(jti).isNotEmpty();
        // JTI should be a UUID format (36 characters with dashes)
        assertThat(jti).hasSize(36);
        assertThat(jti).contains("-");
    }

    @Test
    void shouldGenerateHashForTokenWithoutJti() {
        // Given - Create token without JTI using basic method
        String token = tokenProvider.generateToken(testUser);

        // When
        String jti = tokenProvider.extractJti(token);

        // Then
        assertThat(jti).isNotNull();
        assertThat(jti).isNotEmpty();
        // Should be deterministic hash (16 hex characters)
        assertThat(jti).hasSize(16);
        assertThat(jti).matches("[a-f0-9]+");
    }

    @Test
    void shouldValidateTokenWithoutServicesAvailable() {
        // Given - Create token provider without optional services
        JwtTokenProvider basicProvider = new JwtTokenProvider(
            "test-jwt-signing-key-at-least-256-bits-long-for-validation",
            3600000L
        );

        String token = basicProvider.generateToken(testUser);

        // When
        boolean isValid = basicProvider.validateTokenWithBlacklist(token);

        // Then - Should fallback to basic validation
        assertThat(isValid).isTrue();

        // No Redis operations should be attempted
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void shouldCheckServiceAvailability() {
        // When/Then
        assertThat(tokenProvider.isBlacklistServiceAvailable()).isTrue();

        // Given - Provider without services
        JwtTokenProvider basicProvider = new JwtTokenProvider(
            "test-jwt-signing-key-at-least-256-bits-long-for-validation",
            3600000L
        );

        // When/Then
        assertThat(basicProvider.isBlacklistServiceAvailable()).isFalse();
    }

    @Test
    void shouldHandleCompleteWorkflow() {
        // This test simulates a complete user session workflow

        // Given - Generate token for user login
        String token = tokenProvider.generateTokenWithJti(testUser);
        when(setOperations.isMember("jwt:blacklist", token)).thenReturn(false);
        when(valueOperations.get(anyString())).thenReturn(null);

        // When - First validation (should validate and cache)
        boolean firstValidation = tokenProvider.validateTokenWithBlacklist(token);

        // Then
        assertThat(firstValidation).isTrue();
        verify(valueOperations).set(anyString(), eq("valid"), anyLong(), eq(TimeUnit.SECONDS));

        // Given - Second validation (should use cache)
        reset(setOperations, valueOperations);
        when(valueOperations.get(anyString())).thenReturn("valid");

        // When - Second validation
        boolean secondValidation = tokenProvider.validateTokenWithBlacklist(token);

        // Then - Should be cached
        assertThat(secondValidation).isTrue();
        verify(valueOperations).get(anyString());
        verify(setOperations, never()).isMember(anyString(), anyString());

        // When - User logs out (invalidate token)
        tokenProvider.invalidateToken(token);

        // Then - Token should be blacklisted and cache cleared
        verify(setOperations).add("jwt:blacklist", token);
        verify(redisTemplate).delete(anyString());

        // Given - Attempt to use invalidated token
        reset(setOperations);
        when(setOperations.isMember("jwt:blacklist", token)).thenReturn(true);

        // When - Third validation (should be rejected)
        boolean thirdValidation = tokenProvider.validateTokenWithBlacklist(token);

        // Then
        assertThat(thirdValidation).isFalse();
    }
}