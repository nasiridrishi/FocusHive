package com.focushive.notification.service;

// import io.github.bucket4j.Bandwidth;
// import io.github.bucket4j.Bucket;
// import io.github.bucket4j.Refill;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for implementing API rate limiting using Redis.
 * Supports different rate limits for different operation types and users.
 */
@Service
@Slf4j
public class RateLimitingService {

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String BLOCKED_PREFIX = "blocked:";
    private static final Duration DEFAULT_BLOCK_DURATION = Duration.ofMinutes(5);
    private static final int VIOLATION_THRESHOLD = 3;

    private final RedisTemplate<String, String> redisTemplate;
    private final MeterRegistry meterRegistry;
    private final Map<String, AtomicInteger> violationCounts = new ConcurrentHashMap<>();
    // private final Map<String, Bucket> localBucketCache = new ConcurrentHashMap<>();
    private final Map<String, Instant> bucketExpiry = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitingService(RedisTemplate<String, String> redisTemplate,
                              MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
    }

    @Value("${notification.rate-limit.read-limit:100}")
    private int readLimit;

    @Value("${notification.rate-limit.write-limit:50}")
    private int writeLimit;

    @Value("${notification.rate-limit.admin-limit:20}")
    private int adminLimit;

    @Value("${notification.rate-limit.public-limit:20}")
    private int publicLimit;

    @Value("${notification.rate-limit.window-minutes:60}")
    private int windowMinutes;

    // Redis Lua script for atomic rate limiting check and increment
    private static final String RATE_LIMIT_SCRIPT = """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            
            local current = redis.call('GET', key)
            if current == false then
                redis.call('SETEX', key, window, 1)
                return {1, limit - 1, window}
            else
                current = tonumber(current)
                if current < limit then
                    local remaining = redis.call('INCR', key)
                    local ttl = redis.call('TTL', key)
                    return {remaining, limit - remaining, ttl}
                else
                    local ttl = redis.call('TTL', key)
                    return {current, 0, ttl}
                end
            end
            """;

    private final RedisScript<Object> rateLimitScript = RedisScript.of(RATE_LIMIT_SCRIPT, Object.class);

    /**
     * Checks if a request is allowed based on rate limits.
     *
     * @param identifier unique identifier (user ID or IP)
     * @param operationType type of operation (READ, WRITE, ADMIN, PUBLIC)
     * @return true if request is allowed, false if rate limited
     */
    public boolean isAllowed(String identifier, String operationType) {
        try {
            String key = buildRateLimitKey(identifier, operationType);
            int limit = getRateLimitForOperation(operationType);
            int windowSeconds = windowMinutes * 60;

            // Execute atomic rate limiting check
            Object result = redisTemplate.execute(
                    rateLimitScript,
                    Arrays.asList(key),
                    String.valueOf(limit),
                    String.valueOf(windowSeconds)
            );

            if (result instanceof java.util.List<?> resultList && resultList.size() >= 3) {
                int current = ((Number) resultList.get(0)).intValue();
                int remaining = ((Number) resultList.get(1)).intValue();
                
                log.debug("Rate limit check for {}: {}/{}, remaining: {}", 
                         key, current, limit, remaining);
                
                return remaining >= 0;
            }

            // Fallback if script execution fails
            log.warn("Rate limit script returned unexpected result: {}", result);
            return true;

        } catch (Exception e) {
            log.error("Error checking rate limit for {}: {}", identifier, e.getMessage());
            // Fail open - allow request if rate limiting fails
            return true;
        }
    }

    /**
     * Gets the remaining requests for a given identifier and operation.
     *
     * @param identifier unique identifier (user ID or IP)
     * @param operationType type of operation
     * @return number of remaining requests
     */
    public int getRemainingRequests(String identifier, String operationType) {
        try {
            String key = buildRateLimitKey(identifier, operationType);
            String current = redisTemplate.opsForValue().get(key);
            
            if (current == null) {
                return getRateLimitForOperation(operationType);
            }
            
            int currentCount = Integer.parseInt(current);
            int limit = getRateLimitForOperation(operationType);
            return Math.max(0, limit - currentCount);

        } catch (Exception e) {
            log.error("Error getting remaining requests for {}: {}", identifier, e.getMessage());
            return 0;
        }
    }

    /**
     * Gets the reset time for a given identifier and operation.
     *
     * @param identifier unique identifier (user ID or IP)
     * @param operationType type of operation
     * @return reset time in milliseconds since epoch
     */
    public long getResetTime(String identifier, String operationType) {
        try {
            String key = buildRateLimitKey(identifier, operationType);
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            
            if (ttl == null || ttl <= 0) {
                return System.currentTimeMillis() + Duration.ofMinutes(windowMinutes).toMillis();
            }
            
            return System.currentTimeMillis() + (ttl * 1000);

        } catch (Exception e) {
            log.error("Error getting reset time for {}: {}", identifier, e.getMessage());
            return System.currentTimeMillis() + Duration.ofMinutes(windowMinutes).toMillis();
        }
    }

    /**
     * Resets rate limit for a specific identifier and operation.
     * Used for testing or administrative purposes.
     *
     * @param identifier unique identifier (user ID or IP)
     * @param operationType type of operation
     */
    public void resetRateLimit(String identifier, String operationType) {
        try {
            String key = buildRateLimitKey(identifier, operationType);
            redisTemplate.delete(key);
            log.info("Reset rate limit for key: {}", key);
        } catch (Exception e) {
            log.error("Error resetting rate limit for {}: {}", identifier, e.getMessage());
        }
    }

    /**
     * Gets current usage for a specific identifier and operation.
     *
     * @param identifier unique identifier (user ID or IP)
     * @param operationType type of operation
     * @return current usage count
     */
    public int getCurrentUsage(String identifier, String operationType) {
        try {
            String key = buildRateLimitKey(identifier, operationType);
            String current = redisTemplate.opsForValue().get(key);
            return current != null ? Integer.parseInt(current) : 0;
        } catch (Exception e) {
            log.error("Error getting current usage for {}: {}", identifier, e.getMessage());
            return 0;
        }
    }

    /**
     * Builds Redis key for rate limiting.
     */
    private String buildRateLimitKey(String identifier, String operationType) {
        return String.format("rate_limit:%s:%s:%d", 
                             identifier, operationType, getCurrentWindow());
    }

    /**
     * Gets current time window for rate limiting.
     */
    private long getCurrentWindow() {
        return System.currentTimeMillis() / (windowMinutes * 60 * 1000);
    }

    /**
     * Gets rate limit based on operation type.
     */
    private int getRateLimitForOperation(String operationType) {
        return switch (operationType.toUpperCase()) {
            case "READ" -> readLimit;
            case "WRITE" -> writeLimit;
            case "ADMIN" -> adminLimit;
            case "PUBLIC" -> publicLimit;
            default -> readLimit; // Default to read limit
        };
    }

    /**
     * Gets the configured limit for an operation type.
     * Used for including in response headers.
     */
    public int getConfiguredLimit(String operationType) {
        return getRateLimitForOperation(operationType);
    }

    /**
     * Checks if rate limiting is enabled.
     */
    public boolean isEnabled() {
        try {
            // Simple connectivity test
            redisTemplate.opsForValue().get("rate_limit_test");
            return true;
        } catch (Exception e) {
            log.warn("Rate limiting disabled due to Redis unavailability: {}", e.getMessage());
            return false;
        }
    }

    // ========= Bucket4j Enhanced Methods =========

    /**
     * Check if a request is allowed based on rate limiting rules using Redis.
     *
     * @param key The rate limit key (user ID, IP address, etc.)
     * @param requestsPerMinute Maximum requests per minute
     * @param requestsPerHour Maximum requests per hour
     * @return true if request is allowed, false otherwise
     */
    public boolean allowRequest(String key, int requestsPerMinute, int requestsPerHour) {
        try {
            // Check if user is blocked
            if (isBlocked(key)) {
                recordMetric(key, false);
                return false;
            }

            // Use Redis-based rate limiting
            String minuteKey = RATE_LIMIT_PREFIX + key + ":minute:" + (System.currentTimeMillis() / 60000);
            String hourKey = RATE_LIMIT_PREFIX + key + ":hour:" + (System.currentTimeMillis() / 3600000);

            // Check minute limit
            Long minuteCount = redisTemplate.opsForValue().increment(minuteKey);
            if (minuteCount == 1) {
                redisTemplate.expire(minuteKey, Duration.ofMinutes(1));
            }

            // Check hour limit
            Long hourCount = redisTemplate.opsForValue().increment(hourKey);
            if (hourCount == 1) {
                redisTemplate.expire(hourKey, Duration.ofHours(1));
            }

            boolean allowed = minuteCount <= requestsPerMinute && hourCount <= requestsPerHour;

            if (!allowed) {
                handleViolation(key);
            }

            recordMetric(key, allowed);
            return allowed;

        } catch (Exception e) {
            log.error("Error checking rate limit for key: {}", key, e);
            // In case of error, allow the request to avoid blocking legitimate users
            return true;
        }
    }

    /**
     * Allow burst requests up to specified capacity.
     *
     * @param key The rate limit key
     * @param burstCapacity Maximum burst capacity
     * @return true if burst request is allowed
     */
    public boolean allowBurstRequest(String key, int burstCapacity) {
        try {
            String burstKey = RATE_LIMIT_PREFIX + key + ":burst:" + (System.currentTimeMillis() / 1000);

            Long count = redisTemplate.opsForValue().increment(burstKey);
            if (count == 1) {
                redisTemplate.expire(burstKey, Duration.ofSeconds(1));
            }

            return count <= burstCapacity;
        } catch (Exception e) {
            log.error("Error checking burst rate limit for key: {}", key, e);
            return true;
        }
    }

    /**
     * Get the number of remaining requests for a key using Redis.
     *
     * @param key The rate limit key
     * @return Number of remaining requests
     */
    public int getRemainingRequests(String key) {
        try {
            String minuteKey = RATE_LIMIT_PREFIX + key + ":minute:" + (System.currentTimeMillis() / 60000);
            String value = redisTemplate.opsForValue().get(minuteKey);

            if (value == null) {
                return 60; // Default requests per minute if not set
            }

            int currentCount = Integer.parseInt(value);
            return Math.max(0, 60 - currentCount);
        } catch (Exception e) {
            log.error("Error getting remaining requests for key: {}", key, e);
            return 0;
        }
    }

    /**
     * Get the reset time for rate limit.
     *
     * @param key The rate limit key
     * @return Reset time in milliseconds since epoch
     */
    public long getResetTime(String key) {
        // For simplicity, return next minute boundary
        return Instant.now().plusSeconds(60 - (Instant.now().getEpochSecond() % 60)).toEpochMilli();
    }

    /**
     * Check if a user/IP is blocked.
     *
     * @param key The key to check
     * @return true if blocked
     */
    public boolean isBlocked(String key) {
        String blockKey = BLOCKED_PREFIX + key;
        String blocked = redisTemplate.opsForValue().get(blockKey);
        return blocked != null;
    }

    // Bucket4j methods removed - using Redis-based rate limiting instead

    /**
     * Check if a cached bucket is expired.
     */
    private boolean isExpired(String bucketKey) {
        Instant expiry = bucketExpiry.get(bucketKey);
        return expiry == null || Instant.now().isAfter(expiry);
    }

    /**
     * Handle rate limit violation.
     */
    private void handleViolation(String key) {
        AtomicInteger violations = violationCounts.computeIfAbsent(key, k -> new AtomicInteger(0));
        int count = violations.incrementAndGet();

        if (count >= VIOLATION_THRESHOLD) {
            // Block the user/IP
            String blockKey = BLOCKED_PREFIX + key;
            redisTemplate.opsForValue().set(blockKey, "blocked", DEFAULT_BLOCK_DURATION.toSeconds(), TimeUnit.SECONDS);
            violationCounts.remove(key);
            log.warn("Blocked key {} due to {} rate limit violations", key, count);

            meterRegistry.counter("rate.limit.blocked", "key", key).increment();
        }
    }

    /**
     * Record metrics for rate limiting.
     */
    private void recordMetric(String key, boolean allowed) {
        String result = allowed ? "allowed" : "denied";
        meterRegistry.counter("rate.limit.requests", "result", result, "key", key).increment();

        if (!allowed) {
            meterRegistry.counter("rate.limit.exceeded", "key", key).increment();
        }
    }

    /**
     * Cleanup expired buckets periodically.
     */
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void cleanupExpiredBuckets() {
        try {
            Instant now = Instant.now();
            bucketExpiry.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));

            log.debug("Cleaned up expired cache entries. Current cache size: {}", bucketExpiry.size());

            // Also cleanup old violation counts
            violationCounts.entrySet().removeIf(entry -> {
                // Remove violation counts older than 1 hour
                String key = entry.getKey();
                String violationCheckKey = RATE_LIMIT_PREFIX + key + ":check";
                Long ttl = redisTemplate.getExpire(violationCheckKey, TimeUnit.SECONDS);
                return ttl == null || ttl <= 0;
            });
        } catch (Exception e) {
            log.error("Error during cleanup", e);
        }
    }
}