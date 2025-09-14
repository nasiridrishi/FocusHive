package com.focushive.identity.service;

import com.focushive.identity.annotation.RateLimit;
import com.focushive.identity.exception.RateLimitExceededException;
import com.focushive.identity.service.IRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Test implementation of RedisRateLimiter that uses in-memory storage
 * instead of Redis for test environments.
 * 
 * This provides the same interface as the production RedisRateLimiter
 * but uses simple in-memory counters for rate limiting during tests.
 */
@Service
@Profile("test")
public class TestRedisRateLimiter implements IRateLimiter {
    
    private static final Logger log = LoggerFactory.getLogger(TestRedisRateLimiter.class);
    
    // In-memory storage for rate limit tracking
    private final ConcurrentMap<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> violations = new ConcurrentHashMap<>();
    
    /**
     * Checks if a request is allowed based on the rate limit configuration.
     * 
     * @param key unique identifier for the rate limit bucket
     * @param rateLimit rate limit configuration
     * @return true if request is allowed, false otherwise
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    public boolean isAllowed(String key, RateLimit rateLimit) {
        try {
            RateLimitBucket bucket = buckets.computeIfAbsent(key, k -> 
                new RateLimitBucket(rateLimit.value(), rateLimit.window(), rateLimit.timeUnit()));
            
            boolean allowed = bucket.tryConsume();
            
            if (!allowed) {
                handleRateLimitExceeded(key, rateLimit);
                return false;
            }
            
            log.debug("Rate limit check passed for key: {}", key);
            return true;
            
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error checking rate limit for key: {}", key, e);
            // In case of error, allow the request
            return true;
        }
    }
    
    /**
     * Gets the remaining tokens in the bucket.
     * 
     * @param key unique identifier for the rate limit bucket
     * @param rateLimit rate limit configuration
     * @return number of remaining tokens
     */
    public long getRemainingTokens(String key, RateLimit rateLimit) {
        try {
            RateLimitBucket bucket = buckets.get(key);
            return bucket != null ? bucket.getAvailableTokens() : rateLimit.value();
        } catch (Exception e) {
            log.error("Error getting remaining tokens for key: {}", key, e);
            return rateLimit.value(); // Return max tokens on error
        }
    }
    
    /**
     * Gets the time until the next token is available.
     * 
     * @param key unique identifier for the rate limit bucket
     * @param rateLimit rate limit configuration
     * @return seconds until next token is available
     */
    public long getSecondsUntilRefill(String key, RateLimit rateLimit) {
        try {
            RateLimitBucket bucket = buckets.get(key);
            return bucket != null ? bucket.getSecondsUntilRefill() : 0;
        } catch (Exception e) {
            log.error("Error calculating refill time for key: {}", key, e);
            return rateLimit.timeUnit().toSeconds(rateLimit.window());
        }
    }
    
    /**
     * Clears all rate limit data - useful for test cleanup.
     */
    public void clearAll() {
        buckets.clear();
        violations.clear();
        log.debug("Cleared all rate limit data from memory");
    }
    
    /**
     * Gets the current violation count for a key - useful for testing.
     */
    public int getViolationCount(String key) {
        return violations.getOrDefault(key, 0);
    }
    
    /**
     * Handles rate limit exceeded scenarios.
     */
    private void handleRateLimitExceeded(String key, RateLimit rateLimit) {
        if (rateLimit.progressivePenalties()) {
            applyProgressivePenalty(key);
        }
        
        // Log the violation
        log.debug("Rate limit violated for key: {}", key);
        
        // Prepare error message
        String message = rateLimit.message().isEmpty() 
            ? String.format("Rate limit exceeded. Maximum %d requests per %d %s allowed.", 
                           rateLimit.value(), rateLimit.window(), rateLimit.timeUnit().toString().toLowerCase())
            : rateLimit.message();
        
        long retryAfter = getSecondsUntilRefill(key, rateLimit);
        
        throw new RateLimitExceededException(message, retryAfter);
    }
    
    /**
     * Applies progressive penalty for repeated violations.
     */
    private void applyProgressivePenalty(String key) {
        int violationCount = violations.compute(key, (k, v) -> (v == null) ? 1 : v + 1);
        log.debug("Progressive penalty applied for key: {} (violation count: {})", key, violationCount);
    }
    
    /**
     * Simple in-memory rate limit bucket implementation.
     */
    private static class RateLimitBucket {
        private final long capacity;
        private final long windowMs;
        private long tokens;
        private long lastRefillTime;
        
        public RateLimitBucket(long capacity, long window, java.util.concurrent.TimeUnit timeUnit) {
            this.capacity = capacity;
            this.windowMs = timeUnit.toMillis(window);
            this.tokens = capacity;
            this.lastRefillTime = System.currentTimeMillis();
        }
        
        public synchronized boolean tryConsume() {
            refill();
            
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }
        
        public synchronized long getAvailableTokens() {
            refill();
            return tokens;
        }
        
        public synchronized long getSecondsUntilRefill() {
            if (tokens > 0) {
                return 0;
            }
            
            long now = System.currentTimeMillis();
            long nextRefillTime = lastRefillTime + windowMs;
            
            return Math.max(0, (nextRefillTime - now) / 1000);
        }
        
        private void refill() {
            long now = System.currentTimeMillis();
            if (now >= lastRefillTime + windowMs) {
                tokens = capacity;
                lastRefillTime = now;
            }
        }
    }
}