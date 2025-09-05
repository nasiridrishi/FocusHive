package com.focushive.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.mock;

/**
 * DEPRECATED: This configuration conflicts with TestMockConfig.
 * All test configurations have been consolidated into TestMockConfig.
 * This class is kept for compatibility with existing imports but provides no beans.
 */
@TestConfiguration
@Profile("deprecated-test-config")  // Changed profile to avoid conflicts
public class TestConfig {
    // All beans moved to TestMockConfig to avoid @Primary conflicts
}