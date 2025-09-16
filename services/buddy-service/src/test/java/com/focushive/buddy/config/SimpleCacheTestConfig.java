package com.focushive.buddy.config;

import com.focushive.buddy.service.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;

/**
 * Simple test configuration for cache integration tests.
 * Imports the main CacheConfig and enables caching for tests.
 */
@TestConfiguration
@EnableCaching
@Import({CacheConfig.class, RedisConfig.class})
public class SimpleCacheTestConfig {
    // Just enable caching and import the main cache configuration
    // The actual services will be created by Spring Boot's component scan
}