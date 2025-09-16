package com.focushive.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Test-specific cache configuration that provides a single, simple CacheManager
 * to avoid bean conflicts in test environment.
 *
 * This configuration takes priority in tests and prevents multiple cache managers
 * from being created, which was causing NoUniqueBeanDefinitionException.
 */
@Slf4j
@Configuration
@EnableCaching
@Profile("test")
public class TestCacheConfig implements CachingConfigurer {

    /**
     * Creates a simple in-memory cache manager for tests.
     * This is marked as @Primary to ensure it's used when multiple cache managers exist.
     */
    @Bean
    @Primary
    @Override
    public CacheManager cacheManager() {
        log.info("Creating test-specific ConcurrentMapCacheManager");
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setAllowNullValues(false);

        // Configure cache names used in the application
        cacheManager.setCacheNames(java.util.Arrays.asList(
            "hives-active",
            "hives-user",
            "hives-stats",
            "hive-details",
            "timer-sessions",
            "presence",
            "user-profiles",
            "identity-validation",
            "user-info",
            "user-by-email",
            "user-personas",
            "active-persona",
            "hives",
            "users",
            "analytics",
            "jwt-tokens",
            "userPresence",
            "activeFocusSession",
            "hiveActiveUsers",
            "hiveFocusSessions",
            "dailySummaries",
            "dailySummaryRanges",
            "hiveMembership",
            "hiveUserIds",
            "userSettings",
            "systemSettings",
            "notificationTemplates"
        ));

        return cacheManager;
    }

    /**
     * Simple key generator for tests
     */
    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return new SimpleKeyGenerator();
    }

    /**
     * Simple cache resolver for tests
     */
    @Bean
    @Override
    public CacheResolver cacheResolver() {
        return new SimpleCacheResolver(cacheManager());
    }

    /**
     * Simple error handler that logs cache errors but doesn't fail tests
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler();
    }
}