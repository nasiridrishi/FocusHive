package com.focushive.buddy.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
// CacheMetricsRegistrar removed - not available in Spring Boot 3.x
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Configuration for cache metrics monitoring.
 * Provides cache hit ratio monitoring, performance metrics,
 * and cache health indicators.
 */
@Slf4j
@Configuration
@EnableScheduling
public class CacheMetricsConfig {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MeterRegistry meterRegistry;

    /**
     * Registers cache metrics with Micrometer.
     * Enables automatic cache hit/miss ratio monitoring.
     */
    @PostConstruct
    public void registerCacheMetrics() {
        log.info("Registering cache metrics for buddy-service");

        // Register all cache metrics
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                // Manual metric registration for Spring Boot 3.x
                meterRegistry.gauge("cache.size", Tags.of("cache", cacheName, "service", "buddy"), cache, c -> 0);
                log.debug("Registered metrics for cache: {}", cacheName);
            }
        }

        // Register custom metrics
        registerCustomMetrics();
    }

    /**
     * Registers custom cache metrics for detailed monitoring.
     */
    private void registerCustomMetrics() {
        // Custom cache performance metrics will be handled by CacheMonitor component
        meterRegistry.gauge("buddy.cache.total_caches", cacheManager.getCacheNames().size());
        log.info("Registered {} custom cache metrics", cacheManager.getCacheNames().size());
    }

    /**
     * Cache monitoring component for performance tracking.
     */
    @Component
    @Slf4j
    public static class CacheMonitor {

        @Autowired
        private CacheManager cacheManager;

        @Autowired
        private MeterRegistry meterRegistry;

        private final Map<String, CacheStats> cacheStatsMap = new ConcurrentHashMap<>();

        /**
         * Statistics holder for individual cache instances.
         */
        public static class CacheStats {
            private final AtomicLong hits = new AtomicLong(0);
            private final AtomicLong misses = new AtomicLong(0);
            private final AtomicLong evictions = new AtomicLong(0);
            private final AtomicLong puts = new AtomicLong(0);
            private final AtomicLong gets = new AtomicLong(0);

            public double getHitRatio() {
                long totalGets = gets.get();
                return totalGets > 0 ? (double) hits.get() / totalGets : 0.0;
            }

            public long getHits() { return hits.get(); }
            public long getMisses() { return misses.get(); }
            public long getEvictions() { return evictions.get(); }
            public long getPuts() { return puts.get(); }
            public long getGets() { return gets.get(); }

            public void recordHit() { hits.incrementAndGet(); gets.incrementAndGet(); }
            public void recordMiss() { misses.incrementAndGet(); gets.incrementAndGet(); }
            public void recordEviction() { evictions.incrementAndGet(); }
            public void recordPut() { puts.incrementAndGet(); }
        }

        /**
         * Scheduled task to collect and report cache metrics.
         * Runs every 5 minutes to provide regular monitoring.
         */
        @Scheduled(fixedRate = 300000) // 5 minutes
        public void collectCacheMetrics() {
            try {
                log.debug("Collecting cache metrics for buddy-service");

                for (String cacheName : cacheManager.getCacheNames()) {
                    Cache cache = cacheManager.getCache(cacheName);
                    if (cache != null) {
                        collectCacheStats(cacheName, cache);
                    }
                }

                // Log summary every hour
                if (System.currentTimeMillis() % 3600000 < 300000) { // Within 5 minutes of the hour
                    logCacheSummary();
                }

            } catch (Exception e) {
                log.warn("Error collecting cache metrics: {}", e.getMessage());
            }
        }

        /**
         * Collects statistics for a specific cache.
         */
        private void collectCacheStats(String cacheName, Cache cache) {
            CacheStats stats = cacheStatsMap.computeIfAbsent(cacheName, k -> new CacheStats());

            // Record metrics to Micrometer
            meterRegistry.gauge("buddy.cache.hit_ratio", Tags.of("cache", cacheName), stats.getHitRatio());
            meterRegistry.gauge("buddy.cache.size", Tags.of("cache", cacheName), getCacheSize(cache));

            // Log cache performance if hit ratio is low
            double hitRatio = stats.getHitRatio();
            if (hitRatio < 0.5 && stats.getGets() > 100) {
                log.warn("Low cache hit ratio for {}: {:.2f}% (hits: {}, misses: {})",
                        cacheName, hitRatio * 100, stats.getHits(), stats.getMisses());
            }
        }

        /**
         * Gets the approximate size of a cache.
         * Returns -1 if size cannot be determined.
         */
        private long getCacheSize(Cache cache) {
            try {
                // Try to get size from native cache if available
                Object nativeCache = cache.getNativeCache();
                if (nativeCache instanceof org.springframework.data.redis.cache.RedisCache) {
                    // For Redis cache, we can't easily get the size, return -1
                    return -1;
                }
                return -1; // Size not available
            } catch (Exception e) {
                return -1; // Size not available
            }
        }

        /**
         * Logs a summary of all cache performance.
         */
        private void logCacheSummary() {
            log.info("Cache Performance Summary:");
            log.info("Active caches: {}", cacheManager.getCacheNames().size());

            for (Map.Entry<String, CacheStats> entry : cacheStatsMap.entrySet()) {
                CacheStats stats = entry.getValue();
                log.info("Cache {}: hit ratio: {:.2f}%, hits: {}, misses: {}, puts: {}",
                        entry.getKey(),
                        stats.getHitRatio() * 100,
                        stats.getHits(),
                        stats.getMisses(),
                        stats.getPuts());
            }
        }

        /**
         * Gets cache statistics for monitoring dashboard.
         */
        public Map<String, Object> getCacheStatistics() {
            Map<String, Object> statistics = new ConcurrentHashMap<>();
            statistics.put("totalCaches", cacheManager.getCacheNames().size());
            statistics.put("cacheNames", cacheManager.getCacheNames());

            Map<String, Object> cacheStats = new ConcurrentHashMap<>();
            for (Map.Entry<String, CacheStats> entry : cacheStatsMap.entrySet()) {
                CacheStats stats = entry.getValue();
                Map<String, Object> cacheInfo = Map.of(
                        "hitRatio", stats.getHitRatio(),
                        "hits", stats.getHits(),
                        "misses", stats.getMisses(),
                        "evictions", stats.getEvictions(),
                        "puts", stats.getPuts(),
                        "gets", stats.getGets()
                );
                cacheStats.put(entry.getKey(), cacheInfo);
            }

            statistics.put("cacheStatistics", cacheStats);
            statistics.put("timestamp", System.currentTimeMillis());

            return statistics;
        }

        /**
         * Gets the cache statistics for a specific cache.
         */
        public CacheStats getCacheStats(String cacheName) {
            return cacheStatsMap.get(cacheName);
        }

        /**
         * Clears statistics for all caches.
         */
        public void clearStatistics() {
            cacheStatsMap.clear();
            log.info("Cleared all cache statistics");
        }

        /**
         * Records a cache hit for the specified cache.
         */
        public void recordCacheHit(String cacheName) {
            cacheStatsMap.computeIfAbsent(cacheName, k -> new CacheStats()).recordHit();
        }

        /**
         * Records a cache miss for the specified cache.
         */
        public void recordCacheMiss(String cacheName) {
            cacheStatsMap.computeIfAbsent(cacheName, k -> new CacheStats()).recordMiss();
        }

        /**
         * Records a cache put operation for the specified cache.
         */
        public void recordCachePut(String cacheName) {
            cacheStatsMap.computeIfAbsent(cacheName, k -> new CacheStats()).recordPut();
        }

        /**
         * Records a cache eviction for the specified cache.
         */
        public void recordCacheEviction(String cacheName) {
            cacheStatsMap.computeIfAbsent(cacheName, k -> new CacheStats()).recordEviction();
        }
    }
}