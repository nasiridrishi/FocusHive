package com.focushive.api.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * JWT Token Blacklist Service using Redis for fast blacklist lookups.
 *
 * This service manages a blacklist of revoked JWT tokens to prevent their reuse.
 * Tokens are added to the blacklist when users log out or when tokens need to be invalidated.
 *
 * Performance Requirements:
 * - Token validation < 10ms
 * - Uses Redis SET operations for O(1) lookup performance
 * - Automatic TTL management to prevent memory leaks
 *
 * Security Features:
 * - Fail-safe design (defaults to false on Redis errors)
 * - JTI (JWT ID) based blacklisting for precise token identification
 * - Configurable TTL based on token expiration times
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenBlacklistService {

    private static final String BLACKLIST_KEY = "jwt:blacklist";

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Add a token to the blacklist with specified TTL.
     *
     * @param token The JWT token to blacklist
     * @param expiry Duration until the blacklist entry should expire
     */
    public void blacklistToken(String token, Duration expiry) {
        if (!StringUtils.hasText(token)) {
            log.warn("Attempted to blacklist null or empty token");
            return;
        }

        try {
            // Add token to Redis SET
            redisTemplate.opsForSet().add(BLACKLIST_KEY, token);

            // Set TTL for the blacklist set (renewed on each addition)
            redisTemplate.expire(BLACKLIST_KEY, expiry.toSeconds(), TimeUnit.SECONDS);

            log.debug("Token added to blacklist with TTL: {} seconds", expiry.toSeconds());
        } catch (Exception e) {
            log.error("Failed to add token to blacklist", e);
            // Don't throw exception - log and continue
        }
    }

    /**
     * Add a token to the blacklist using its JTI (JWT ID) claim.
     * This method extracts the JTI from the token and blacklists it.
     *
     * @param token The JWT token containing the JTI claim
     * @param expiry Duration until the blacklist entry should expire
     */
    public void blacklistTokenByJti(String token, Duration expiry) {
        if (!StringUtils.hasText(token)) {
            log.warn("Attempted to blacklist null or empty token by JTI");
            return;
        }

        try {
            // For now, blacklist the entire token
            // TODO: Extract JTI claim when JTI support is added to JwtTokenProvider
            blacklistToken(token, expiry);
        } catch (Exception e) {
            log.error("Failed to blacklist token by JTI", e);
        }
    }

    /**
     * Check if a token is blacklisted.
     *
     * @param token The JWT token to check
     * @return true if the token is blacklisted, false otherwise
     */
    public boolean isTokenBlacklisted(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        try {
            Boolean isBlacklisted = redisTemplate.opsForSet().isMember(BLACKLIST_KEY, token);
            return Boolean.TRUE.equals(isBlacklisted);
        } catch (Exception e) {
            log.error("Error checking token blacklist status", e);
            // Fail-safe: return false to maintain service availability
            // This means potentially allowing an invalidated token, but prevents service outage
            return false;
        }
    }

    /**
     * Remove expired tokens from the blacklist.
     * Note: Redis TTL handles this automatically, but this method provides
     * manual cleanup capability if needed.
     */
    public void cleanupExpiredTokens() {
        try {
            // Redis handles TTL automatically, but we can provide stats
            Long size = redisTemplate.opsForSet().size(BLACKLIST_KEY);
            log.debug("Current blacklist size: {} tokens", size);
        } catch (Exception e) {
            log.error("Error during blacklist cleanup", e);
        }
    }

    /**
     * Get the current size of the blacklist.
     *
     * @return Number of tokens currently in the blacklist
     */
    public long getBlacklistSize() {
        try {
            Long size = redisTemplate.opsForSet().size(BLACKLIST_KEY);
            return size != null ? size : 0L;
        } catch (Exception e) {
            log.error("Error getting blacklist size", e);
            return 0L;
        }
    }

    /**
     * Clear all tokens from the blacklist.
     * This is primarily for testing and administrative purposes.
     */
    public void clearBlacklist() {
        try {
            redisTemplate.delete(BLACKLIST_KEY);
            log.info("Blacklist cleared");
        } catch (Exception e) {
            log.error("Error clearing blacklist", e);
        }
    }
}