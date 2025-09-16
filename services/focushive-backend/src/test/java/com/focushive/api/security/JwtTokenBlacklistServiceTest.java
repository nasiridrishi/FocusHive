package com.focushive.api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for JWT Token Blacklist Service
 * These tests should FAIL initially - implementing TDD RED phase
 */
@ExtendWith(MockitoExtension.class)
class JwtTokenBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    private JwtTokenBlacklistService blacklistService;
    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        // This constructor doesn't exist yet - will fail
        blacklistService = new JwtTokenBlacklistService(redisTemplate);

        // Setup token provider for test tokens
        tokenProvider = new JwtTokenProvider("test-jwt-signing-key-at-least-256-bits-long-for-validation", 3600000L);
    }

    @Test
    void shouldAddTokenToBlacklist() {
        // Given
        String token = "sample.jwt.token";
        Duration expiry = Duration.ofHours(1);

        // When - This method doesn't exist yet - will fail
        blacklistService.blacklistToken(token, expiry);

        // Then
        verify(setOperations).add("jwt:blacklist", token);
        verify(redisTemplate).expire("jwt:blacklist", expiry.toSeconds(), TimeUnit.SECONDS);
    }

    @Test
    void shouldCheckIfTokenIsBlacklisted() {
        // Given
        String token = "blacklisted.jwt.token";
        when(setOperations.isMember("jwt:blacklist", token)).thenReturn(true);

        // When - This method doesn't exist yet - will fail
        boolean isBlacklisted = blacklistService.isTokenBlacklisted(token);

        // Then
        assertThat(isBlacklisted).isTrue();
        verify(setOperations).isMember("jwt:blacklist", token);
    }

    @Test
    void shouldReturnFalseForNonBlacklistedToken() {
        // Given
        String token = "valid.jwt.token";
        when(setOperations.isMember("jwt:blacklist", token)).thenReturn(false);

        // When
        boolean isBlacklisted = blacklistService.isTokenBlacklisted(token);

        // Then
        assertThat(isBlacklisted).isFalse();
        verify(setOperations).isMember("jwt:blacklist", token);
    }

    @Test
    void shouldHandleNullToken() {
        // When
        boolean isBlacklisted = blacklistService.isTokenBlacklisted(null);

        // Then
        assertThat(isBlacklisted).isFalse();
        verifyNoInteractions(setOperations);
    }

    @Test
    void shouldHandleEmptyToken() {
        // When
        boolean isBlacklisted = blacklistService.isTokenBlacklisted("");

        // Then
        assertThat(isBlacklisted).isFalse();
        verifyNoInteractions(setOperations);
    }

    @Test
    void shouldBlacklistTokenWithJtiClaim() {
        // Given - Create a real token with JTI
        String realToken = tokenProvider.generateToken(createTestUser());
        Duration expiry = Duration.ofHours(1);

        // When - Extract JTI and blacklist
        blacklistService.blacklistTokenByJti(realToken, expiry);

        // Then - Should extract JTI and blacklist it
        verify(setOperations).add(eq("jwt:blacklist"), anyString());
        verify(redisTemplate).expire(eq("jwt:blacklist"), eq(expiry.toSeconds()), eq(TimeUnit.SECONDS));
    }

    @Test
    void shouldRemoveExpiredTokensFromBlacklist() {
        // When - This method doesn't exist yet - will fail
        blacklistService.cleanupExpiredTokens();

        // Then - Redis TTL should handle this automatically,
        // but we might want explicit cleanup
        verify(setOperations, atLeastOnce()).members("jwt:blacklist");
    }

    @Test
    void shouldGetBlacklistSize() {
        // Given
        when(setOperations.size("jwt:blacklist")).thenReturn(42L);

        // When - This method doesn't exist yet - will fail
        long size = blacklistService.getBlacklistSize();

        // Then
        assertThat(size).isEqualTo(42L);
        verify(setOperations).size("jwt:blacklist");
    }

    @Test
    void shouldHandleRedisConnectionErrors() {
        // Given
        String token = "test.jwt.token";
        when(setOperations.isMember(anyString(), anyString()))
            .thenThrow(new RuntimeException("Redis connection error"));

        // When - Should handle gracefully and not throw
        boolean isBlacklisted = blacklistService.isTokenBlacklisted(token);

        // Then - Should default to false on error (fail-open for availability)
        assertThat(isBlacklisted).isFalse();
    }

    @Test
    void shouldValidatePerformanceRequirements() {
        // Given
        String token = "performance.test.token";
        when(setOperations.isMember("jwt:blacklist", token)).thenReturn(false);

        // When - Measure performance
        long startTime = System.currentTimeMillis();
        boolean isBlacklisted = blacklistService.isTokenBlacklisted(token);
        long endTime = System.currentTimeMillis();

        // Then - Should complete within performance requirements
        long duration = endTime - startTime;
        assertThat(duration).isLessThan(10L); // < 10ms requirement
        assertThat(isBlacklisted).isFalse();
    }

    /**
     * Helper method to create test user - will need actual User implementation
     */
    private com.focushive.user.entity.User createTestUser() {
        com.focushive.user.entity.User user = new com.focushive.user.entity.User();
        user.setId("test-user-id");
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setRole(com.focushive.user.entity.User.UserRole.USER);
        return user;
    }
}