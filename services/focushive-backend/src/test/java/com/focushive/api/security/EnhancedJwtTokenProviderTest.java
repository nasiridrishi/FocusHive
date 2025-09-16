package com.focushive.api.security;

import com.focushive.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TDD Tests for Enhanced JWT Token Provider with Blacklist Integration
 * These tests should FAIL initially - implementing TDD RED phase
 */
@ExtendWith(MockitoExtension.class)
class EnhancedJwtTokenProviderTest {

    @Mock
    private JwtTokenBlacklistService blacklistService;

    private JwtTokenProvider tokenProvider;
    private User testUser;

    @BeforeEach
    void setUp() {
        // This enhanced constructor doesn't exist yet - will fail
        tokenProvider = new JwtTokenProvider(
            "test-jwt-signing-key-at-least-256-bits-long-for-validation",
            3600000L,
            Optional.of(blacklistService)
        );

        testUser = new User();
        testUser.setId("test-user-id");
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setRole(User.UserRole.USER);
    }

    @Test
    void shouldValidateTokenAndCheckBlacklist() {
        // Given
        String token = tokenProvider.generateToken(testUser);
        when(blacklistService.isTokenBlacklisted(token)).thenReturn(false);

        // When - This enhanced method doesn't exist yet - will fail
        boolean isValid = tokenProvider.validateTokenWithBlacklist(token);

        // Then
        assertThat(isValid).isTrue();
        verify(blacklistService).isTokenBlacklisted(token);
    }

    @Test
    void shouldRejectBlacklistedToken() {
        // Given
        String token = tokenProvider.generateToken(testUser);
        when(blacklistService.isTokenBlacklisted(token)).thenReturn(true);

        // When
        boolean isValid = tokenProvider.validateTokenWithBlacklist(token);

        // Then
        assertThat(isValid).isFalse();
        verify(blacklistService).isTokenBlacklisted(token);
    }

    @Test
    void shouldInvalidateTokenByAddingToBlacklist() {
        // Given
        String token = tokenProvider.generateToken(testUser);

        // When - This method doesn't exist yet - will fail
        tokenProvider.invalidateToken(token);

        // Then - Should add token to blacklist with appropriate TTL
        verify(blacklistService).blacklistToken(eq(token), any(Duration.class));
    }

    @Test
    void shouldGenerateTokenWithJtiClaim() {
        // When
        String token = tokenProvider.generateToken(testUser);

        // Then - Should include JTI (JWT ID) claim for blacklist tracking
        String jti = tokenProvider.extractJti(token); // This method doesn't exist yet - will fail
        assertThat(jti).isNotNull();
        assertThat(jti).isNotEmpty();
    }

    @Test
    void shouldValidateTokenPerformanceWithBlacklist() {
        // Given
        String token = tokenProvider.generateToken(testUser);
        when(blacklistService.isTokenBlacklisted(token)).thenReturn(false);

        // When - Measure performance
        long startTime = System.currentTimeMillis();
        boolean isValid = tokenProvider.validateTokenWithBlacklist(token);
        long endTime = System.currentTimeMillis();

        // Then - Should meet performance requirement < 10ms
        long duration = endTime - startTime;
        assertThat(duration).isLessThan(10L);
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldHandleBlacklistServiceFailureGracefully() {
        // Given
        String token = tokenProvider.generateToken(testUser);
        when(blacklistService.isTokenBlacklisted(token))
            .thenThrow(new RuntimeException("Redis connection error"));

        // When - Should not throw exception
        boolean isValid = tokenProvider.validateTokenWithBlacklist(token);

        // Then - Should fail-safe (reject token on blacklist error for security)
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldExtractJtiFromExistingToken() {
        // Given - Generate token with existing provider (no JTI yet)
        JwtTokenProvider basicProvider = new JwtTokenProvider(
            "test-jwt-signing-key-at-least-256-bits-long-for-validation",
            3600000L
        );
        String token = basicProvider.generateToken(testUser);

        // When - Try to extract JTI
        String jti = basicProvider.extractJti(token); // This method doesn't exist yet - will fail

        // Then - Should handle gracefully for tokens without JTI
        // For now, might return null or generate one based on token hash
        // This test defines the expected behavior
    }

    @Test
    void shouldCacheValidatedTokensForPerformance() {
        // Given
        String token = tokenProvider.generateToken(testUser);
        when(blacklistService.isTokenBlacklisted(token)).thenReturn(false);

        // When - Validate same token multiple times
        tokenProvider.validateTokenWithBlacklist(token);
        tokenProvider.validateTokenWithBlacklist(token);
        tokenProvider.validateTokenWithBlacklist(token);

        // Then - Should cache result and not call blacklist service multiple times
        // This might involve a local cache with short TTL
        verify(blacklistService).isTokenBlacklisted(token); // Should be called only once if cached
    }
}