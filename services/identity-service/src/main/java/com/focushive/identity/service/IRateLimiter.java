package com.focushive.identity.service;

import com.focushive.identity.annotation.RateLimit;
import com.focushive.identity.exception.RateLimitExceededException;

/**
 * Interface for rate limiter service implementations.
 * Provides abstraction between production Redis-based and test in-memory implementations.
 */
public interface IRateLimiter {
    
    /**
     * Checks if a request is allowed based on the rate limit configuration.
     * 
     * @param key unique identifier for the rate limit bucket
     * @param rateLimit rate limit configuration
     * @return true if request is allowed, false otherwise
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    boolean isAllowed(String key, RateLimit rateLimit) throws RateLimitExceededException;
    
    /**
     * Gets the remaining tokens in the bucket.
     * 
     * @param key unique identifier for the rate limit bucket
     * @param rateLimit rate limit configuration
     * @return number of remaining tokens
     */
    long getRemainingTokens(String key, RateLimit rateLimit);
    
    /**
     * Gets the time until the next token is available.
     * 
     * @param key unique identifier for the rate limit bucket
     * @param rateLimit rate limit configuration
     * @return seconds until next token is available
     */
    long getSecondsUntilRefill(String key, RateLimit rateLimit);
    
    /**
     * Gets the current violation count for a key.
     * Used for progressive penalties and monitoring.
     * 
     * @param key unique identifier for the rate limit bucket
     * @return number of violations for this key
     */
    default int getViolationCount(String key) {
        return 0; // Default implementation for backwards compatibility
    }
}
