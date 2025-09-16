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

    /**
     * Increments the violation count for a key.
     * Used for progressive penalties and monitoring.
     *
     * @param key unique identifier for the rate limit bucket
     * @return new violation count
     */
    default long incrementViolationCount(String key) {
        return 0; // Default implementation for backwards compatibility
    }

    /**
     * Suspends a client for a specified duration.
     * Used for progressive penalties when violations exceed threshold.
     *
     * @param key unique identifier for the rate limit bucket
     * @param durationSeconds suspension duration in seconds
     */
    default void suspendClient(String key, long durationSeconds) {
        // Default implementation for backwards compatibility - no-op
    }

    /**
     * Checks if a client is currently suspended.
     *
     * @param key unique identifier for the rate limit bucket
     * @return true if client is suspended, false otherwise
     */
    default boolean isSuspended(String key) {
        return false; // Default implementation for backwards compatibility
    }

    /**
     * Gets the reset time for a rate limit bucket.
     *
     * @param key unique identifier for the rate limit bucket
     * @param windowMinutes window duration in minutes
     * @return timestamp when the rate limit resets
     */
    default long getResetTime(String key, int windowMinutes) {
        return System.currentTimeMillis() / 1000 + (windowMinutes * 60); // Default implementation
    }

    /**
     * Resets the rate limit for a specific key.
     *
     * @param key unique identifier for the rate limit bucket
     */
    default void resetLimit(String key) {
        // Default implementation for backwards compatibility - no-op
    }

    /**
     * Clears violations for a pattern of keys.
     *
     * @param pattern pattern to match keys for clearing violations
     */
    default void clearViolations(String pattern) {
        // Default implementation for backwards compatibility - no-op
    }

    /**
     * Clears suspension for a specific key.
     *
     * @param key unique identifier for the rate limit bucket
     */
    default void clearSuspension(String key) {
        // Default implementation for backwards compatibility - no-op
    }
}
