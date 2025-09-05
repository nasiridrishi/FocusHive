package com.focushive.identity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing blacklisted/revoked JWT tokens.
 * Uses Redis to store blacklisted tokens until their expiration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    
    private static final String BLACKLIST_PREFIX = "blacklist:token:";
    
    private final RedisTemplate<String, String> redisTemplate;
    
    /**
     * Add token to blacklist until its expiration time.
     */
    public void blacklistToken(String token, Instant expirationTime) {
        String key = BLACKLIST_PREFIX + token;
        
        // Calculate TTL based on token expiration
        long ttlSeconds = Duration.between(Instant.now(), expirationTime).getSeconds();
        
        if (ttlSeconds > 0) {
            redisTemplate.opsForValue().set(key, "revoked", ttlSeconds, TimeUnit.SECONDS);
            log.info("Token blacklisted for {} seconds", ttlSeconds);
        }
    }
    
    /**
     * Check if token is blacklisted.
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    /**
     * Remove token from blacklist (rarely needed).
     */
    public void removeFromBlacklist(String token) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.delete(key);
        log.info("Token removed from blacklist");
    }
    
    /**
     * Blacklist all tokens for a user (e.g., on password change).
     * This would require storing user tokens separately.
     */
    public void blacklistAllUserTokens(String userId) {
        // TODO: Implement if needed - would require tracking tokens by user
        log.warn("Blacklisting all tokens for user {} - not implemented", userId);
    }
}