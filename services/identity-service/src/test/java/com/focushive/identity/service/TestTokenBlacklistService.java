package com.focushive.identity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import com.focushive.identity.service.ITokenBlacklistService;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Test implementation of TokenBlacklistService that uses in-memory storage
 * instead of Redis for test environments.
 * 
 * This provides the same interface as the production TokenBlacklistService
 * but uses a simple ConcurrentHashMap for storage during tests.
 */
@Service
@Profile("test")
public class TestTokenBlacklistService implements ITokenBlacklistService {
    
    private static final Logger log = LoggerFactory.getLogger(TestTokenBlacklistService.class);
    private final ConcurrentMap<String, TokenEntry> blacklistedTokens = new ConcurrentHashMap<>();
    
    /**
     * Add token to in-memory blacklist until its expiration time.
     */
    public void blacklistToken(String token, Instant expirationTime) {
        blacklistedTokens.put(token, new TokenEntry("revoked", expirationTime));
        log.debug("Token blacklisted in memory until {}", expirationTime);
    }
    
    /**
     * Check if token is blacklisted and not expired.
     */
    public boolean isBlacklisted(String token) {
        TokenEntry entry = blacklistedTokens.get(token);
        if (entry == null) {
            return false;
        }
        
        // Check if token entry has expired
        if (Instant.now().isAfter(entry.expirationTime)) {
            blacklistedTokens.remove(token); // Clean up expired entry
            return false;
        }
        
        return true;
    }
    
    /**
     * Remove token from in-memory blacklist.
     */
    public void removeFromBlacklist(String token) {
        blacklistedTokens.remove(token);
        log.debug("Token removed from in-memory blacklist");
    }
    
    /**
     * Blacklist all tokens for a user (test implementation).
     */
    public void blacklistAllUserTokens(String userId) {
        log.debug("Blacklisting all tokens for user {} - test implementation", userId);
        // In a real implementation, this would require additional user-token mapping
    }
    
    /**
     * Clear all blacklisted tokens - useful for test cleanup.
     */
    public void clearAll() {
        blacklistedTokens.clear();
        log.debug("Cleared all blacklisted tokens from memory");
    }
    
    /**
     * Get count of blacklisted tokens - useful for test assertions.
     */
    public int getBlacklistedTokenCount() {
        // Clean up expired entries first
        blacklistedTokens.entrySet().removeIf(entry -> 
            Instant.now().isAfter(entry.getValue().expirationTime));
        return blacklistedTokens.size();
    }
    
    /**
     * Internal class to store token entry with expiration.
     */
    private static class TokenEntry {
        final String status;
        final Instant expirationTime;
        
        TokenEntry(String status, Instant expirationTime) {
            this.status = status;
            this.expirationTime = expirationTime;
        }
    }
}