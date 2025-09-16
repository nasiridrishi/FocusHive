package com.focushive.identity.service;

import com.focushive.identity.annotation.RateLimit;
import com.focushive.identity.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.EstimationProbe;
import io.github.bucket4j.redis.jedis.cas.JedisBasedProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPool;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based distributed rate limiter using Bucket4j.
 * Provides thread-safe, distributed rate limiting with progressive penalties.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Profile("!test")
@ConditionalOnProperty(name = "focushive.rate-limiting.enabled", havingValue = "true", matchIfMissing = true)
public class RedisRateLimiter implements IRateLimiter {

    private final JedisPool jedisPool;
    private final StringRedisTemplate redisTemplate;
    private final JedisBasedProxyManager<String> proxyManager;
    
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    private static final String VIOLATION_KEY_PREFIX = "rate_limit_violations:";
    
    // Cache for bucket configurations to avoid recreating them
    private final ConcurrentHashMap<String, BucketConfiguration> configCache = new ConcurrentHashMap<>();
    
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
            String bucketKey = RATE_LIMIT_KEY_PREFIX + key;
            
            // Get or create bucket configuration
            BucketConfiguration config = getBucketConfiguration(rateLimit);
            
            // Get the bucket from Redis
            Bucket bucket = proxyManager.builder()
                    .build(bucketKey, config);
            
            // Check if request is allowed
            boolean allowed = bucket.tryConsume(1);
            
            if (!allowed) {
                handleRateLimitExceeded(key, rateLimit);
                return false;
            }
            
            log.debug("Rate limit check passed for key: {}", key);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking rate limit for key: {}", key, e);
            // In case of Redis failure, allow the request but log the error
            // This ensures service availability even if Redis is down
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
            String bucketKey = RATE_LIMIT_KEY_PREFIX + key;
            BucketConfiguration config = getBucketConfiguration(rateLimit);

            Bucket bucket = proxyManager.builder()
                    .build(bucketKey, config);
            
            return bucket.getAvailableTokens();
            
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
            String bucketKey = RATE_LIMIT_KEY_PREFIX + key;
            BucketConfiguration config = getBucketConfiguration(rateLimit);

            Bucket bucket = proxyManager.builder()
                    .build(bucketKey, config);
            
            EstimationProbe probe = bucket.estimateAbilityToConsume(1);
            // Return seconds to wait for refill, or 0 if tokens are available
            return probe.canBeConsumed() ? 0 : probe.getNanosToWaitForRefill() / 1_000_000_000;
            
        } catch (Exception e) {
            log.error("Error calculating refill time for key: {}", key, e);
            return rateLimit.timeUnit().toSeconds(rateLimit.window());
        }
    }
    
    /**
     * Handles rate limit exceeded scenarios with progressive penalties.
     */
    private void handleRateLimitExceeded(String key, RateLimit rateLimit) {
        if (rateLimit.progressivePenalties()) {
            applyProgressivePenalty(key, rateLimit);
        }
        
        // Log the violation
        logRateLimitViolation(key, rateLimit);
        
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
    private void applyProgressivePenalty(String key, RateLimit rateLimit) {
        try {
            String violationKey = VIOLATION_KEY_PREFIX + key;
            String countStr = redisTemplate.opsForValue().get(violationKey);
            
            int violationCount = countStr != null ? Integer.parseInt(countStr) : 0;
            violationCount++;
            
            // Store violation count with TTL
            long ttlSeconds = rateLimit.timeUnit().toSeconds(rateLimit.window()) * 10; // 10x the rate limit window
            redisTemplate.opsForValue().set(violationKey, String.valueOf(violationCount), Duration.ofSeconds(ttlSeconds));
            
            // Apply progressive penalty by reducing the rate limit
            if (violationCount > 3) {
                // Create a more restrictive bucket for repeated violators
                long penaltyWindow = rateLimit.timeUnit().toSeconds(rateLimit.window()) * violationCount;
                String penaltyKey = RATE_LIMIT_KEY_PREFIX + key + ":penalty";
                
                BucketConfiguration penaltyConfig = BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(1, Duration.ofSeconds(penaltyWindow)))
                        .build();
                
                proxyManager.builder().build(penaltyKey, penaltyConfig);
                
                log.warn("Applied progressive penalty for key: {} (violation count: {})", key, violationCount);
            }
            
        } catch (Exception e) {
            log.error("Error applying progressive penalty for key: {}", key, e);
        }
    }
    
    /**
     * Logs rate limit violations for monitoring and alerting.
     */
    private void logRateLimitViolation(String key, RateLimit rateLimit) {
        try {
            String violationLogKey = "rate_limit_log:" + Instant.now().toString().substring(0, 16); // Hourly buckets
            redisTemplate.opsForList().leftPush(violationLogKey, 
                String.format("%s|%s|%d|%s", 
                             Instant.now().toString(),
                             key, 
                             rateLimit.value(),
                             rateLimit.type().toString()));
            
            // Set TTL for log entries (keep for 24 hours)
            redisTemplate.expire(violationLogKey, Duration.ofHours(24));
            
            log.warn("Rate limit violation: key={}, limit={}, type={}", key, rateLimit.value(), rateLimit.type());
            
        } catch (Exception e) {
            log.error("Error logging rate limit violation", e);
        }
    }
    
    /**
     * Gets or creates a bucket configuration for the given rate limit.
     */
    private BucketConfiguration getBucketConfiguration(RateLimit rateLimit) {
        String configKey = String.format("%d-%d-%s", 
                                         rateLimit.value(), 
                                         rateLimit.window(), 
                                         rateLimit.timeUnit().toString());
        
        return configCache.computeIfAbsent(configKey, k -> {
            Duration window = Duration.ofMillis(rateLimit.timeUnit().toMillis(rateLimit.window()));
            return BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(rateLimit.value(), window))
                    .build();
        });
    }
    
    /**
     * Clears all rate limit data for a specific key (admin function).
     */
    public void clearRateLimit(String key) {
        try {
            String bucketKey = RATE_LIMIT_KEY_PREFIX + key;
            String violationKey = VIOLATION_KEY_PREFIX + key;
            String penaltyKey = RATE_LIMIT_KEY_PREFIX + key + ":penalty";
            
            redisTemplate.delete(bucketKey);
            redisTemplate.delete(violationKey);
            redisTemplate.delete(penaltyKey);
            
            log.info("Cleared rate limit data for key: {}", key);
            
        } catch (Exception e) {
            log.error("Error clearing rate limit for key: {}", key, e);
        }
    }
    
    /**
     * Gets violation statistics for monitoring.
     */
    public int getViolationCount(String key) {
        try {
            String violationKey = VIOLATION_KEY_PREFIX + key;
            String countStr = redisTemplate.opsForValue().get(violationKey);
            return countStr != null ? Integer.parseInt(countStr) : 0;
        } catch (Exception e) {
            log.error("Error getting violation count for key: {}", key, e);
            return 0;
        }
    }

    /**
     * Increments violation count and returns the new count.
     */
    public long incrementViolationCount(String key) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            // Set expiry for violation count (1 hour)
            redisTemplate.expire(key, Duration.ofHours(1));
            return count != null ? count : 1;
        } catch (Exception e) {
            log.error("Error incrementing violation count for key: {}", key, e);
            return 1;
        }
    }

    /**
     * Suspends a client for a specified duration.
     */
    public void suspendClient(String suspensionKey, long durationSeconds) {
        try {
            redisTemplate.opsForValue().set(suspensionKey, "suspended", Duration.ofSeconds(durationSeconds));
            log.warn("Client suspended: {} for {} seconds", suspensionKey, durationSeconds);
        } catch (Exception e) {
            log.error("Error suspending client: {}", suspensionKey, e);
        }
    }

    /**
     * Checks if a client is currently suspended.
     */
    public boolean isSuspended(String suspensionKey) {
        try {
            return redisTemplate.hasKey(suspensionKey);
        } catch (Exception e) {
            log.error("Error checking suspension for key: {}", suspensionKey, e);
            return false;
        }
    }

    /**
     * Gets the reset time for a rate limit window.
     */
    public long getResetTime(String key, int windowMinutes) {
        try {
            Long ttl = redisTemplate.getExpire(RATE_LIMIT_KEY_PREFIX + key, TimeUnit.MILLISECONDS);
            if (ttl != null && ttl > 0) {
                return System.currentTimeMillis() + ttl;
            } else {
                // If no TTL or key doesn't exist, return window time from now
                return System.currentTimeMillis() + (windowMinutes * 60 * 1000L);
            }
        } catch (Exception e) {
            log.error("Error getting reset time for key: {}", key, e);
            return System.currentTimeMillis() + (windowMinutes * 60 * 1000L);
        }
    }

    /**
     * Resets rate limit for a specific key.
     */
    public void resetLimit(String key) {
        try {
            redisTemplate.delete(key);
            log.info("Reset rate limit for key: {}", key);
        } catch (Exception e) {
            log.error("Error resetting rate limit for key: {}", key, e);
        }
    }

    /**
     * Clears violations matching a pattern.
     */
    public void clearViolations(String pattern) {
        try {
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} violation keys matching pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.error("Error clearing violations for pattern: {}", pattern, e);
        }
    }

    /**
     * Clears suspension for a client.
     */
    public void clearSuspension(String suspensionKey) {
        try {
            redisTemplate.delete(suspensionKey);
            log.info("Cleared suspension for key: {}", suspensionKey);
        } catch (Exception e) {
            log.error("Error clearing suspension for key: {}", suspensionKey, e);
        }
    }
}