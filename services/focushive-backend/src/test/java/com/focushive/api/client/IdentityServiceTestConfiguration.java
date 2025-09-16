package com.focushive.api.client;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration for Identity Service integration tests.
 * Provides simplified caching and mocked dependencies for testing.
 */
@TestConfiguration
@EnableCaching
@Profile("test")
public class IdentityServiceTestConfiguration {

    /**
     * Simple in-memory cache manager for tests.
     * Replaces Redis-based caching with simple concurrent map.
     */
    @Bean
    @Primary
    public CacheManager testCacheManager() {
        return new ConcurrentMapCacheManager(
            "identity-validation",
            "user-info",
            "user-by-email",
            "user-personas",
            "active-persona"
        );
    }
}