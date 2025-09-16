package com.focushive.cache.impl;

import com.focushive.cache.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory cache implementation for test environments.
 * This avoids Redis dependency in test profiles.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "simple", matchIfMissing = false)
public class InMemoryCacheService implements CacheService {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private long hits = 0;
    private long misses = 0;
    private long evictions = 0;

    @Override
    public void put(String key, Object value, Duration ttl) {
        if (key == null || value == null) {
            return;
        }

        Instant expiry = ttl != null ? Instant.now().plus(ttl) : Instant.now().plus(Duration.ofHours(1));
        cache.put(key, new CacheEntry(value, expiry));
        log.debug("Cached value for key: {} with TTL: {}", key, ttl);
    }

    @Override
    public void put(String key, Object value) {
        put(key, value, Duration.ofHours(1)); // Default 1 hour TTL
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        if (key == null) {
            misses++;
            return Optional.empty();
        }

        CacheEntry entry = cache.get(key);
        if (entry == null) {
            misses++;
            return Optional.empty();
        }

        // Check if expired
        if (entry.isExpired()) {
            cache.remove(key);
            misses++;
            log.debug("Cache miss (expired) for key: {}", key);
            return Optional.empty();
        }

        hits++;
        log.debug("Cache hit for key: {}", key);

        try {
            return Optional.of((T) entry.value());
        } catch (ClassCastException e) {
            log.warn("Type mismatch for cache key: {}", key, e);
            return Optional.empty();
        }
    }

    @Override
    public void evict(String key) {
        if (key != null && cache.remove(key) != null) {
            evictions++;
            log.debug("Evicted cache entry for key: {}", key);
        }
    }

    @Override
    public void evictAll(String cacheName) {
        // In this simple implementation, we clear all entries
        // In a real implementation, you might want to support multiple cache regions
        int size = cache.size();
        cache.clear();
        evictions += size;
        log.info("Evicted all {} cache entries", size);
    }

    @Override
    public boolean exists(String key) {
        if (key == null) {
            return false;
        }

        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return false;
        }

        // Check if expired
        if (entry.isExpired()) {
            cache.remove(key);
            return false;
        }

        return true;
    }

    @Override
    public CacheStatistics getStatistics() {
        // Clean up expired entries
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());

        return new CacheStatistics(hits, misses, evictions, cache.size());
    }

    private record CacheEntry(Object value, Instant expiry) {
        boolean isExpired() {
            return Instant.now().isAfter(expiry);
        }
    }
}