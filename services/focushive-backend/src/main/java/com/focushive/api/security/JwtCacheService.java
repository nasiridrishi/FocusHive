package com.focushive.api.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * JWT Token Caching Service for performance optimization.
 *
 * This service provides caching for validated JWT tokens to improve performance
 * by avoiding repeated cryptographic validation operations.
 *
 * Performance Features:
 * - Cache validated tokens with short TTL (5 minutes)
 * - O(1) Redis GET operations for cache hits
 * - Automatic cache invalidation on token expiry
 * - Memory efficient with configurable TTL
 *
 * Security Considerations:
 * - Short cache TTL to limit exposure window
 * - Cache only successful validations
 * - Immediate invalidation when tokens are blacklisted
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtCacheService {

    private static final String VALIDATION_CACHE_KEY_PREFIX = "jwt:validation:";
    private static final Duration DEFAULT_CACHE_TTL = Duration.ofMinutes(5);

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Cache a successful token validation result.
     *
     * @param token JWT token that was successfully validated
     * @param ttl Time to live for the cache entry
     */
    public void cacheValidToken(String token, Duration ttl) {
        try {
            String cacheKey = VALIDATION_CACHE_KEY_PREFIX + generateCacheKey(token);
            redisTemplate.opsForValue().set(cacheKey, "valid", ttl.toSeconds(), TimeUnit.SECONDS);
            log.debug("Token validation cached with TTL: {} seconds", ttl.toSeconds());
        } catch (Exception e) {
            log.error("Failed to cache token validation", e);
            // Don't throw - caching is optional for performance
        }
    }

    /**
     * Cache a successful token validation result with default TTL.
     *
     * @param token JWT token that was successfully validated
     */
    public void cacheValidToken(String token) {
        cacheValidToken(token, DEFAULT_CACHE_TTL);
    }

    /**
     * Check if a token validation result is cached.
     *
     * @param token JWT token to check
     * @return true if token is cached as valid, false otherwise
     */
    public boolean isTokenValidationCached(String token) {
        try {
            String cacheKey = VALIDATION_CACHE_KEY_PREFIX + generateCacheKey(token);
            String cachedValue = redisTemplate.opsForValue().get(cacheKey);
            return "valid".equals(cachedValue);
        } catch (Exception e) {
            log.error("Error checking token validation cache", e);
            return false; // Fail-safe: assume not cached
        }
    }

    /**
     * Invalidate cached validation for a specific token.
     * This should be called when a token is blacklisted.
     *
     * @param token JWT token to invalidate from cache
     */
    public void invalidateTokenCache(String token) {
        try {
            String cacheKey = VALIDATION_CACHE_KEY_PREFIX + generateCacheKey(token);
            redisTemplate.delete(cacheKey);
            log.debug("Token validation cache invalidated");
        } catch (Exception e) {
            log.error("Failed to invalidate token cache", e);
        }
    }

    /**
     * Clear all token validation cache entries.
     * This is primarily for testing and administrative purposes.
     */
    public void clearValidationCache() {
        try {
            var keys = redisTemplate.keys(VALIDATION_CACHE_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Token validation cache cleared - {} entries removed", keys.size());
            }
        } catch (Exception e) {
            log.error("Error clearing token validation cache", e);
        }
    }

    /**
     * Get cache statistics for monitoring and optimization.
     *
     * @return Cache statistics object
     */
    public CacheStats getCacheStats() {
        try {
            var keys = redisTemplate.keys(VALIDATION_CACHE_KEY_PREFIX + "*");
            long cacheSize = keys != null ? keys.size() : 0;

            return CacheStats.builder()
                    .cacheSize(cacheSize)
                    .ttlSeconds(DEFAULT_CACHE_TTL.toSeconds())
                    .build();
        } catch (Exception e) {
            log.error("Error getting cache stats", e);
            return CacheStats.builder().cacheSize(0).ttlSeconds(0).build();
        }
    }

    /**
     * Generate a cache key from a JWT token.
     * Uses a hash to avoid storing full tokens in Redis keys.
     *
     * @param token JWT token
     * @return Hashed cache key
     */
    private String generateCacheKey(String token) {
        // Use the same hashing method as JwtTokenProvider for consistency
        return String.valueOf(token.hashCode());
    }

    /**
     * Cache statistics data class.
     */
    public static class CacheStats {
        private final long cacheSize;
        private final long ttlSeconds;

        private CacheStats(long cacheSize, long ttlSeconds) {
            this.cacheSize = cacheSize;
            this.ttlSeconds = ttlSeconds;
        }

        public static Builder builder() {
            return new Builder();
        }

        public long getCacheSize() {
            return cacheSize;
        }

        public long getTtlSeconds() {
            return ttlSeconds;
        }

        public static class Builder {
            private long cacheSize;
            private long ttlSeconds;

            public Builder cacheSize(long cacheSize) {
                this.cacheSize = cacheSize;
                return this;
            }

            public Builder ttlSeconds(long ttlSeconds) {
                this.ttlSeconds = ttlSeconds;
                return this;
            }

            public CacheStats build() {
                return new CacheStats(cacheSize, ttlSeconds);
            }
        }
    }
}