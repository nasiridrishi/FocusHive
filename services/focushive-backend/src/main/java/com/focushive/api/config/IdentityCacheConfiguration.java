package com.focushive.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;

/**
 * Cache configuration specifically for Identity Service integration.
 *
 * Provides optimized caching strategies for:
 * - Token validation (TTL: 5 minutes, high frequency)
 * - User information (TTL: 15 minutes, moderate frequency)
 * - User personas (TTL: 30 minutes, low frequency)
 *
 * Performance targets:
 * - Cached token validation: < 5ms
 * - Cache hit ratio: > 80%
 * - Memory efficient with automatic eviction
 */
@Slf4j
@Configuration
@EnableCaching
@Profile("!test") // Use simple cache in tests
public class IdentityCacheConfiguration {

    @Value("${identity.service.cache.token-validation-ttl:300}") // 5 minutes
    private int tokenValidationTtl;

    @Value("${identity.service.cache.user-info-ttl:900}") // 15 minutes
    private int userInfoTtl;

    @Value("${identity.service.cache.personas-ttl:1800}") // 30 minutes
    private int personasTtl;

    @Value("${identity.service.cache.max-entries:1000}")
    private int maxEntries;

    /**
     * Redis cache configuration customizer for Identity Service caches.
     * Configures different TTL policies per cache type using Redis.
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        log.info("Configuring Identity Service Redis cache customizer with TTL policies");

        return (builder) -> builder
            .withCacheConfiguration("identity-validation",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofSeconds(tokenValidationTtl))
                    .disableCachingNullValues()
                    .prefixCacheNameWith("identity:"))
            .withCacheConfiguration("user-info",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofSeconds(userInfoTtl))
                    .disableCachingNullValues()
                    .prefixCacheNameWith("identity:"))
            .withCacheConfiguration("user-by-email",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofSeconds(userInfoTtl))
                    .disableCachingNullValues()
                    .prefixCacheNameWith("identity:"))
            .withCacheConfiguration("user-personas",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofSeconds(personasTtl))
                    .disableCachingNullValues()
                    .prefixCacheNameWith("identity:"))
            .withCacheConfiguration("active-persona",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofSeconds(personasTtl))
                    .disableCachingNullValues()
                    .prefixCacheNameWith("identity:"));
    }

    /**
     * Cache statistics reporter for monitoring cache performance.
     * Logs cache statistics periodically to help optimize cache configuration.
     */
    @Bean
    public IdentityCacheMonitor identityCacheMonitor() {
        return new IdentityCacheMonitor();
    }

    /**
     * Component to monitor cache performance and log statistics.
     */
    public static class IdentityCacheMonitor {

        @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 300000) // Every 5 minutes
        public void logCacheStatistics() {
            // This would be implemented to log cache hit rates, eviction rates, etc.
            // For now, we'll rely on the cache managers' built-in statistics
            log.debug("Identity Service cache statistics logging (detailed stats available via actuator)");
        }
    }
}