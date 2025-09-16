package com.focushive.cache;

import java.time.Duration;
import java.util.Optional;

/**
 * Abstraction layer for caching operations to support multiple cache implementations.
 * This allows for Redis in production and simple in-memory caching for tests.
 */
public interface CacheService {

    /**
     * Store a value in the cache with a specific TTL
     */
    void put(String key, Object value, Duration ttl);

    /**
     * Store a value in the cache with default TTL
     */
    void put(String key, Object value);

    /**
     * Retrieve a value from the cache
     */
    <T> Optional<T> get(String key, Class<T> type);

    /**
     * Remove a value from the cache
     */
    void evict(String key);

    /**
     * Clear all entries from a specific cache
     */
    void evictAll(String cacheName);

    /**
     * Check if a key exists in the cache
     */
    boolean exists(String key);

    /**
     * Get cache statistics (optional operation)
     */
    default CacheStatistics getStatistics() {
        return CacheStatistics.empty();
    }

    record CacheStatistics(
        long hits,
        long misses,
        long evictions,
        long size
    ) {
        public static CacheStatistics empty() {
            return new CacheStatistics(0, 0, 0, 0);
        }
    }
}