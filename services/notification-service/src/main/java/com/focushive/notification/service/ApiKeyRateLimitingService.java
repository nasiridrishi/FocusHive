package com.focushive.notification.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing API key-based rate limiting.
 * Provides enhanced rate limiting capabilities for API keys with different tiers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyRateLimitingService {

    private static final String API_KEY_RATE_LIMIT_PREFIX = "rate_limit:apikey:";
    private static final String API_KEY_BLOCKED_PREFIX = "blocked:apikey:";
    private static final String API_KEY_VIOLATIONS_PREFIX = "violations:apikey:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;

    // API key tier configurations (in production, these would be in database)
    private final Map<String, ApiKeyTier> apiKeyTiers = new ConcurrentHashMap<>();

    /**
     * API key tier configuration.
     */
    public enum ApiKeyTier {
        FREE(100, 1000, 10000),      // 100 req/min, 1000 req/hour, 10000 req/day
        BASIC(500, 5000, 50000),     // 500 req/min, 5000 req/hour, 50000 req/day
        PREMIUM(1000, 10000, 100000), // 1000 req/min, 10000 req/hour, 100000 req/day
        ENTERPRISE(5000, 50000, 500000), // 5000 req/min, 50000 req/hour, 500000 req/day
        ADMIN(10000, 100000, 1000000); // Admin/internal use

        private final int requestsPerMinute;
        private final int requestsPerHour;
        private final int requestsPerDay;

        ApiKeyTier(int requestsPerMinute, int requestsPerHour, int requestsPerDay) {
            this.requestsPerMinute = requestsPerMinute;
            this.requestsPerHour = requestsPerHour;
            this.requestsPerDay = requestsPerDay;
        }

        public int getRequestsPerMinute() { return requestsPerMinute; }
        public int getRequestsPerHour() { return requestsPerHour; }
        public int getRequestsPerDay() { return requestsPerDay; }
    }

    /**
     * Check if an API key request is allowed.
     */
    public boolean isAllowed(String apiKey) {
        // Check if API key is blocked
        if (isBlocked(apiKey)) {
            recordViolation(apiKey, "blocked");
            return false;
        }

        // Get API key tier
        ApiKeyTier tier = getApiKeyTier(apiKey);

        // Check rate limits
        boolean withinMinuteLimit = checkRateLimit(apiKey, "minute", 60, tier.getRequestsPerMinute());
        boolean withinHourLimit = checkRateLimit(apiKey, "hour", 3600, tier.getRequestsPerHour());
        boolean withinDayLimit = checkRateLimit(apiKey, "day", 86400, tier.getRequestsPerDay());

        boolean allowed = withinMinuteLimit && withinHourLimit && withinDayLimit;

        if (allowed) {
            meterRegistry.counter("api.key.rate.limit.allowed",
                "apiKey", apiKey,
                "tier", tier.name()).increment();
        } else {
            handleViolation(apiKey, tier);
        }

        return allowed;
    }

    /**
     * Check rate limit for a specific time window.
     */
    private boolean checkRateLimit(String apiKey, String window, int windowSeconds, int limit) {
        long currentWindow = System.currentTimeMillis() / (windowSeconds * 1000);
        String key = API_KEY_RATE_LIMIT_PREFIX + apiKey + ":" + window + ":" + currentWindow;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }

        return count <= limit;
    }

    /**
     * Check if an API key is blocked.
     */
    public boolean isBlocked(String apiKey) {
        String blockKey = API_KEY_BLOCKED_PREFIX + apiKey;
        Boolean blocked = (Boolean) redisTemplate.opsForValue().get(blockKey);
        return blocked != null && blocked;
    }

    /**
     * Block an API key.
     */
    public void blockApiKey(String apiKey, Duration duration) {
        String blockKey = API_KEY_BLOCKED_PREFIX + apiKey;
        redisTemplate.opsForValue().set(blockKey, true, duration);

        log.warn("API key blocked: {} for {}", apiKey, duration);
        meterRegistry.counter("api.key.blocked").increment();
    }

    /**
     * Unblock an API key.
     */
    public void unblockApiKey(String apiKey) {
        String blockKey = API_KEY_BLOCKED_PREFIX + apiKey;
        redisTemplate.delete(blockKey);

        log.info("API key unblocked: {}", apiKey);
        meterRegistry.counter("api.key.unblocked").increment();
    }

    /**
     * Get API key tier.
     */
    public ApiKeyTier getApiKeyTier(String apiKey) {
        // Check if tier is cached
        ApiKeyTier cachedTier = apiKeyTiers.get(apiKey);
        if (cachedTier != null) {
            return cachedTier;
        }

        // Determine tier based on API key prefix (in production, fetch from database)
        ApiKeyTier tier;
        if (apiKey.startsWith("admin-")) {
            tier = ApiKeyTier.ADMIN;
        } else if (apiKey.startsWith("enterprise-")) {
            tier = ApiKeyTier.ENTERPRISE;
        } else if (apiKey.startsWith("premium-")) {
            tier = ApiKeyTier.PREMIUM;
        } else if (apiKey.startsWith("basic-")) {
            tier = ApiKeyTier.BASIC;
        } else {
            tier = ApiKeyTier.FREE;
        }

        // Cache the tier
        apiKeyTiers.put(apiKey, tier);
        return tier;
    }

    /**
     * Set API key tier.
     */
    public void setApiKeyTier(String apiKey, ApiKeyTier tier) {
        apiKeyTiers.put(apiKey, tier);
        log.info("Set API key {} to tier {}", apiKey, tier);
    }

    /**
     * Handle rate limit violation.
     */
    private void handleViolation(String apiKey, ApiKeyTier tier) {
        String violationKey = API_KEY_VIOLATIONS_PREFIX + apiKey;
        Long violations = redisTemplate.opsForValue().increment(violationKey);

        if (violations == 1) {
            redisTemplate.expire(violationKey, Duration.ofHours(1));
        }

        log.warn("Rate limit exceeded for API key: {} (tier: {}, violations: {})",
            apiKey, tier, violations);

        meterRegistry.counter("api.key.rate.limit.exceeded",
            "apiKey", apiKey,
            "tier", tier.name(),
            "violations", violations.toString()).increment();

        // Block API key after multiple violations
        int violationThreshold = switch (tier) {
            case FREE -> 5;
            case BASIC -> 10;
            case PREMIUM -> 20;
            case ENTERPRISE -> 50;
            case ADMIN -> 100;
        };

        if (violations >= violationThreshold) {
            Duration blockDuration = switch (tier) {
                case FREE -> Duration.ofMinutes(30);
                case BASIC -> Duration.ofMinutes(15);
                case PREMIUM -> Duration.ofMinutes(10);
                case ENTERPRISE -> Duration.ofMinutes(5);
                case ADMIN -> Duration.ofMinutes(1);
            };

            blockApiKey(apiKey, blockDuration);
        }
    }

    /**
     * Record a violation.
     */
    private void recordViolation(String apiKey, String reason) {
        meterRegistry.counter("api.key.violation",
            "apiKey", apiKey,
            "reason", reason).increment();
    }

    /**
     * Get remaining requests for an API key.
     */
    public ApiKeyUsage getUsage(String apiKey) {
        ApiKeyTier tier = getApiKeyTier(apiKey);

        long minuteWindow = System.currentTimeMillis() / 60000;
        long hourWindow = System.currentTimeMillis() / 3600000;
        long dayWindow = System.currentTimeMillis() / 86400000;

        String minuteKey = API_KEY_RATE_LIMIT_PREFIX + apiKey + ":minute:" + minuteWindow;
        String hourKey = API_KEY_RATE_LIMIT_PREFIX + apiKey + ":hour:" + hourWindow;
        String dayKey = API_KEY_RATE_LIMIT_PREFIX + apiKey + ":day:" + dayWindow;

        Object minuteCountObj = redisTemplate.opsForValue().get(minuteKey);
        Object hourCountObj = redisTemplate.opsForValue().get(hourKey);
        Object dayCountObj = redisTemplate.opsForValue().get(dayKey);

        String minuteCount = minuteCountObj != null ? minuteCountObj.toString() : null;
        String hourCount = hourCountObj != null ? hourCountObj.toString() : null;
        String dayCount = dayCountObj != null ? dayCountObj.toString() : null;

        int minuteUsed = minuteCount != null ? Integer.parseInt(minuteCount) : 0;
        int hourUsed = hourCount != null ? Integer.parseInt(hourCount) : 0;
        int dayUsed = dayCount != null ? Integer.parseInt(dayCount) : 0;

        return ApiKeyUsage.builder()
            .apiKey(apiKey)
            .tier(tier)
            .minuteUsed(minuteUsed)
            .minuteLimit(tier.getRequestsPerMinute())
            .minuteRemaining(Math.max(0, tier.getRequestsPerMinute() - minuteUsed))
            .hourUsed(hourUsed)
            .hourLimit(tier.getRequestsPerHour())
            .hourRemaining(Math.max(0, tier.getRequestsPerHour() - hourUsed))
            .dayUsed(dayUsed)
            .dayLimit(tier.getRequestsPerDay())
            .dayRemaining(Math.max(0, tier.getRequestsPerDay() - dayUsed))
            .blocked(isBlocked(apiKey))
            .nextResetMinute(System.currentTimeMillis() + (60 - (System.currentTimeMillis() / 1000 % 60)) * 1000)
            .nextResetHour(System.currentTimeMillis() + (3600 - (System.currentTimeMillis() / 1000 % 3600)) * 1000)
            .nextResetDay(System.currentTimeMillis() + (86400 - (System.currentTimeMillis() / 1000 % 86400)) * 1000)
            .build();
    }

    /**
     * Reset rate limits for an API key.
     */
    public void resetLimits(String apiKey) {
        // Delete all rate limit keys for this API key
        List<String> keysToDelete = Arrays.asList(
            API_KEY_RATE_LIMIT_PREFIX + apiKey + ":minute:*",
            API_KEY_RATE_LIMIT_PREFIX + apiKey + ":hour:*",
            API_KEY_RATE_LIMIT_PREFIX + apiKey + ":day:*"
        );
        redisTemplate.delete(keysToDelete);

        // Clear violations
        redisTemplate.delete(API_KEY_VIOLATIONS_PREFIX + apiKey);

        log.info("Reset rate limits for API key: {}", apiKey);
        meterRegistry.counter("api.key.limits.reset").increment();
    }

    /**
     * API key usage statistics.
     */
    @lombok.Builder
    @lombok.Data
    public static class ApiKeyUsage {
        private String apiKey;
        private ApiKeyTier tier;
        private int minuteUsed;
        private int minuteLimit;
        private int minuteRemaining;
        private int hourUsed;
        private int hourLimit;
        private int hourRemaining;
        private int dayUsed;
        private int dayLimit;
        private int dayRemaining;
        private boolean blocked;
        private long nextResetMinute;
        private long nextResetHour;
        private long nextResetDay;
    }
}