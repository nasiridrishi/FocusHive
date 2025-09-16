package com.focushive.cache.impl;

import com.focushive.cache.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-based cache implementation for production environments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class RedisCacheService implements CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void put(String key, Object value, Duration ttl) {
        if (key == null || value == null) {
            return;
        }

        try {
            if (ttl != null) {
                redisTemplate.opsForValue().set(key, value, ttl);
            } else {
                redisTemplate.opsForValue().set(key, value);
            }
            log.debug("Cached value in Redis for key: {} with TTL: {}", key, ttl);
        } catch (Exception e) {
            log.error("Failed to cache value for key: {}", key, e);
        }
    }

    @Override
    public void put(String key, Object value) {
        put(key, value, Duration.ofHours(1)); // Default 1 hour TTL
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        if (key == null) {
            return Optional.empty();
        }

        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("Cache hit in Redis for key: {}", key);
                return Optional.of((T) value);
            }
            log.debug("Cache miss in Redis for key: {}", key);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get value from Redis for key: {}", key, e);
            return Optional.empty();
        }
    }

    @Override
    public void evict(String key) {
        if (key == null) {
            return;
        }

        try {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Evicted Redis cache entry for key: {}", key);
            }
        } catch (Exception e) {
            log.error("Failed to evict Redis cache for key: {}", key, e);
        }
    }

    @Override
    public void evictAll(String cacheName) {
        try {
            // Pattern-based deletion for cache name prefix
            var keys = redisTemplate.keys(cacheName + "*");
            if (keys != null && !keys.isEmpty()) {
                Long deleted = redisTemplate.delete(keys);
                log.info("Evicted {} Redis cache entries for pattern: {}*", deleted, cacheName);
            }
        } catch (Exception e) {
            log.error("Failed to evict all Redis cache entries for pattern: {}*", cacheName, e);
        }
    }

    @Override
    public boolean exists(String key) {
        if (key == null) {
            return false;
        }

        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Failed to check Redis key existence for: {}", key, e);
            return false;
        }
    }

    @Override
    public CacheStatistics getStatistics() {
        // Redis doesn't provide direct statistics without additional configuration
        // This would require Redis INFO command parsing or external monitoring
        log.debug("Redis statistics not implemented - use Redis monitoring tools");
        return CacheStatistics.empty();
    }
}