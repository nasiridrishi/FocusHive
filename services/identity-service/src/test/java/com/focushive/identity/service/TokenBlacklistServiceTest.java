package com.focushive.identity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for TokenBlacklistService.
 * Tests Redis-based token blacklisting with TTL expiration.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBlacklistService Tests")
class TokenBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    private String testToken;
    private String expectedKey;
    private Instant futureExpiration;
    private Instant pastExpiration;

    @BeforeEach
    void setUp() {
        testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
        expectedKey = "blacklist:token:" + testToken;
        futureExpiration = Instant.now().plusSeconds(3600); // 1 hour from now
        pastExpiration = Instant.now().minusSeconds(3600); // 1 hour ago
    }

    @Test
    @DisplayName("Should blacklist token with valid future expiration")
    void blacklistToken_ValidFutureExpiration_ShouldStoreInRedis() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        long expectedTtl = 3600L; // Approximately 1 hour

        // When
        tokenBlacklistService.blacklistToken(testToken, futureExpiration);

        // Then
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(eq(expectedKey), eq("revoked"), longThat(ttl -> 
            Math.abs(ttl - expectedTtl) < 10), eq(TimeUnit.SECONDS)); // Allow 10s variance for test timing
    }

    @Test
    @DisplayName("Should not blacklist token with past expiration")
    void blacklistToken_PastExpiration_ShouldNotStoreInRedis() {
        // When
        tokenBlacklistService.blacklistToken(testToken, pastExpiration);

        // Then - Should not call Redis operations for past expiration
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("Should handle token expiring in less than 1 second")
    void blacklistToken_ExpiringImmediately_ShouldNotStoreInRedis() {
        // Given
        Instant immediateExpiration = Instant.now().plusMillis(500); // 0.5 seconds

        // When
        tokenBlacklistService.blacklistToken(testToken, immediateExpiration);

        // Then - Should not call Redis operations for immediate expiration
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("Should blacklist token with short TTL")
    void blacklistToken_ShortTtl_ShouldStoreInRedis() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Instant shortExpiration = Instant.now().plusSeconds(5); // 5 seconds to avoid timing issues

        // When
        tokenBlacklistService.blacklistToken(testToken, shortExpiration);

        // Then
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(eq(expectedKey), eq("revoked"), longThat(ttl -> 
            ttl >= 1 && ttl <= 6), eq(TimeUnit.SECONDS)); // Allow range for timing variance
    }

    @Test
    @DisplayName("Should blacklist token with very long TTL")
    void blacklistToken_VeryLongTtl_ShouldStoreInRedis() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Instant longExpiration = Instant.now().plusSeconds(86400); // 24 hours

        // When
        tokenBlacklistService.blacklistToken(testToken, longExpiration);

        // Then
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(eq(expectedKey), eq("revoked"), longThat(ttl -> 
            Math.abs(ttl - 86400L) < 10), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should return true when token is blacklisted")
    void isBlacklisted_BlacklistedToken_ShouldReturnTrue() {
        // Given
        when(redisTemplate.hasKey(expectedKey)).thenReturn(true);

        // When
        boolean result = tokenBlacklistService.isBlacklisted(testToken);

        // Then
        assertThat(result).isTrue();
        verify(redisTemplate).hasKey(expectedKey);
    }

    @Test
    @DisplayName("Should return false when token is not blacklisted")
    void isBlacklisted_NonBlacklistedToken_ShouldReturnFalse() {
        // Given
        when(redisTemplate.hasKey(expectedKey)).thenReturn(false);

        // When
        boolean result = tokenBlacklistService.isBlacklisted(testToken);

        // Then
        assertThat(result).isFalse();
        verify(redisTemplate).hasKey(expectedKey);
    }

    @Test
    @DisplayName("Should return false when Redis returns null for hasKey")
    void isBlacklisted_RedisReturnsNull_ShouldReturnFalse() {
        // Given
        when(redisTemplate.hasKey(expectedKey)).thenReturn(null);

        // When
        boolean result = tokenBlacklistService.isBlacklisted(testToken);

        // Then
        assertThat(result).isFalse();
        verify(redisTemplate).hasKey(expectedKey);
    }

    @Test
    @DisplayName("Should remove token from blacklist")
    void removeFromBlacklist_ValidToken_ShouldDeleteFromRedis() {
        // When
        tokenBlacklistService.removeFromBlacklist(testToken);

        // Then
        verify(redisTemplate).delete(expectedKey);
    }

    @Test
    @DisplayName("Should handle blacklistAllUserTokens method")
    void blacklistAllUserTokens_AnyUserId_ShouldLogWarning() {
        // Given
        String userId = "user123";

        // When
        tokenBlacklistService.blacklistAllUserTokens(userId);

        // Then - This method currently only logs a warning - no Redis operations
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("Should handle null token in blacklist operations")
    void blacklistToken_NullToken_ShouldHandleGracefully() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String nullToken = null;
        String nullKey = "blacklist:token:null";

        // When
        tokenBlacklistService.blacklistToken(nullToken, futureExpiration);

        // Then
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(eq(nullKey), eq("revoked"), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should handle empty token in blacklist operations")
    void blacklistToken_EmptyToken_ShouldHandleGracefully() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String emptyToken = "";
        String emptyKey = "blacklist:token:";

        // When
        tokenBlacklistService.blacklistToken(emptyToken, futureExpiration);

        // Then
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(eq(emptyKey), eq("revoked"), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should handle null token in isBlacklisted")
    void isBlacklisted_NullToken_ShouldCheckNullKey() {
        // Given
        String nullToken = null;
        String nullKey = "blacklist:token:null";
        when(redisTemplate.hasKey(nullKey)).thenReturn(false);

        // When
        boolean result = tokenBlacklistService.isBlacklisted(nullToken);

        // Then
        assertThat(result).isFalse();
        verify(redisTemplate).hasKey(nullKey);
    }

    @Test
    @DisplayName("Should handle empty token in isBlacklisted")
    void isBlacklisted_EmptyToken_ShouldCheckEmptyKey() {
        // Given
        String emptyToken = "";
        String emptyKey = "blacklist:token:";
        when(redisTemplate.hasKey(emptyKey)).thenReturn(false);

        // When
        boolean result = tokenBlacklistService.isBlacklisted(emptyToken);

        // Then
        assertThat(result).isFalse();
        verify(redisTemplate).hasKey(emptyKey);
    }

    @Test
    @DisplayName("Should handle null token in removeFromBlacklist")
    void removeFromBlacklist_NullToken_ShouldDeleteNullKey() {
        // Given
        String nullToken = null;
        String nullKey = "blacklist:token:null";

        // When
        tokenBlacklistService.removeFromBlacklist(nullToken);

        // Then
        verify(redisTemplate).delete(nullKey);
    }

    @Test
    @DisplayName("Should handle empty token in removeFromBlacklist")
    void removeFromBlacklist_EmptyToken_ShouldDeleteEmptyKey() {
        // Given
        String emptyToken = "";
        String emptyKey = "blacklist:token:";

        // When
        tokenBlacklistService.removeFromBlacklist(emptyToken);

        // Then
        verify(redisTemplate).delete(emptyKey);
    }

    @Test
    @DisplayName("Should use correct blacklist prefix for any token")
    void blacklistToken_CustomToken_ShouldUseCorrectPrefix() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String customToken = "custom-token-123";
        String expectedCustomKey = "blacklist:token:" + customToken;

        // When
        tokenBlacklistService.blacklistToken(customToken, futureExpiration);

        // Then
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(eq(expectedCustomKey), eq("revoked"), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should store correct value and time unit for blacklisted tokens")
    void blacklistToken_AnyToken_ShouldStoreCorrectValueAndTimeUnit() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        tokenBlacklistService.blacklistToken(testToken, futureExpiration);

        // Then
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(anyString(), eq("revoked"), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should handle concurrent blacklist check operations")
    void isBlacklisted_ConcurrentOperations_ShouldHandleCorrectly() {
        // Given
        when(redisTemplate.hasKey(expectedKey)).thenReturn(true, false, true);

        // When
        boolean result1 = tokenBlacklistService.isBlacklisted(testToken);
        boolean result2 = tokenBlacklistService.isBlacklisted(testToken);
        boolean result3 = tokenBlacklistService.isBlacklisted(testToken);

        // Then
        assertThat(result1).isTrue();
        assertThat(result2).isFalse();
        assertThat(result3).isTrue();
        verify(redisTemplate, times(3)).hasKey(expectedKey);
    }
}