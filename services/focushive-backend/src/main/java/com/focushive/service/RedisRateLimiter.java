package com.focushive.service;

import com.focushive.annotation.RateLimit;
import com.focushive.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.EstimationProbe;
import io.github.bucket4j.redis.jedis.cas.JedisBasedProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class RedisRateLimiter {
    
    private final JedisPool jedisPool;
    private final StringRedisTemplate redisTemplate;
    private final JedisBasedProxyManager proxyManager;
    
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    private static final String VIOLATION_KEY_PREFIX = "rate_limit_violations:";
    
    // Cache for bucket configurations to avoid recreating them
    private final ConcurrentHashMap<String, BucketConfiguration> configCache = new ConcurrentHashMap<>();
    
    /**
     * Checks if a request is allowed based on the rate limit configuration.
     */
    public boolean isAllowed(String key, RateLimit rateLimit) {
        try {
            String bucketKey = RATE_LIMIT_KEY_PREFIX + key;
            
            // Get or create bucket configuration
            BucketConfiguration config = getBucketConfiguration(rateLimit);
            
            // Get the bucket from Redis
            Bucket bucket = proxyManager.builder()
                    .build(bucketKey.getBytes(), config);
            
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
     */
    public long getRemainingTokens(String key, RateLimit rateLimit) {
        try {
            String bucketKey = RATE_LIMIT_KEY_PREFIX + key;
            BucketConfiguration config = getBucketConfiguration(rateLimit);
            
            Bucket bucket = proxyManager.builder()
                    .build(bucketKey.getBytes(), config);
            
            return bucket.getAvailableTokens();
            
        } catch (Exception e) {
            log.error("Error getting remaining tokens for key: {}", key, e);
            return rateLimit.value(); // Return max tokens on error
        }
    }
    
    /**
     * Gets the time until the next token is available.
     */
    public long getSecondsUntilRefill(String key, RateLimit rateLimit) {
        try {
            String bucketKey = RATE_LIMIT_KEY_PREFIX + key;
            BucketConfiguration config = getBucketConfiguration(rateLimit);
            
            Bucket bucket = proxyManager.builder()
                    .build(bucketKey.getBytes(), config);
            
            EstimationProbe estimation = bucket.estimateAbilityToConsume(1);
            return estimation.getNanosToWaitForRefill() / 1_000_000_000L;
            
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
    
    private void applyProgressivePenalty(String key, RateLimit rateLimit) {
        try {
            String violationKey = VIOLATION_KEY_PREFIX + key;
            String countStr = redisTemplate.opsForValue().get(violationKey);
            
            int violationCount = countStr != null ? Integer.parseInt(countStr) : 0;
            violationCount++;
            
            // Store violation count with TTL
            long ttlSeconds = rateLimit.timeUnit().toSeconds(rateLimit.window()) * 10;
            redisTemplate.opsForValue().set(violationKey, String.valueOf(violationCount), Duration.ofSeconds(ttlSeconds));
            
            // Apply progressive penalty by reducing the rate limit
            if (violationCount > 3) {
                long penaltyWindow = rateLimit.timeUnit().toSeconds(rateLimit.window()) * violationCount;
                String penaltyKey = RATE_LIMIT_KEY_PREFIX + key + ":penalty";
                
                BucketConfiguration penaltyConfig = BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(1, Duration.ofSeconds(penaltyWindow)))
                        .build();
                
                proxyManager.builder().build(penaltyKey.getBytes(), penaltyConfig);
                
                log.warn("Applied progressive penalty for key: {} (violation count: {})", key, violationCount);
            }
            
        } catch (Exception e) {
            log.error("Error applying progressive penalty for key: {}", key, e);
        }
    }
    
    private void logRateLimitViolation(String key, RateLimit rateLimit) {
        try {
            String violationLogKey = "rate_limit_log:" + Instant.now().toString().substring(0, 16);
            redisTemplate.opsForList().leftPush(violationLogKey, 
                String.format("%s|%s|%d|%s", 
                             Instant.now().toString(),
                             key, 
                             rateLimit.value(),
                             rateLimit.type().toString()));
            
            redisTemplate.expire(violationLogKey, Duration.ofHours(24));
            
            log.warn("Rate limit violation: key={}, limit={}, type={}", key, rateLimit.value(), rateLimit.type());
            
        } catch (Exception e) {
            log.error("Error logging rate limit violation", e);
        }
    }
    
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
}