package com.focushive.identity.service;

import java.time.Instant;

/**
 * Interface for token blacklist service implementations.
 * Provides abstraction between production Redis-based and test in-memory implementations.
 */
public interface ITokenBlacklistService {
    
    /**
     * Add token to blacklist until its expiration time.
     * 
     * @param token The JWT token to blacklist
     * @param expirationTime When the token expires naturally
     */
    void blacklistToken(String token, Instant expirationTime);
    
    /**
     * Check if token is blacklisted.
     * 
     * @param token The JWT token to check
     * @return true if token is blacklisted, false otherwise
     */
    boolean isBlacklisted(String token);
    
    /**
     * Remove token from blacklist (rarely needed).
     * 
     * @param token The JWT token to remove from blacklist
     */
    void removeFromBlacklist(String token);
    
    /**
     * Blacklist all tokens for a user (e.g., on password change).
     * 
     * @param userId The user ID whose tokens should be blacklisted
     */
    void blacklistAllUserTokens(String userId);
}