package com.focushive.api.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@Slf4j
@Profile("!test") // Don't load in test profile
public class CachingOptimizationConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "caffeine", matchIfMissing = false)
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(caffeineCacheBuilder());

        // Configure specific cache names with different settings
        cacheManager.setCacheNames(List.of("hives", "users", "presence", "analytics", "jwt-tokens"));

        log.info("Configured Caffeine cache manager with optimized settings");
        return cacheManager;
    }

    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(500)
            .expireAfterAccess(Duration.ofMinutes(10))
            .expireAfterWrite(Duration.ofMinutes(30))
            .recordStats();
    }

    @Bean
    public Cache<String, Object> performanceCache() {
        Cache<String, Object> cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats()
            .build();

        log.info("Created performance cache for expensive operations");
        return cache;
    }
}