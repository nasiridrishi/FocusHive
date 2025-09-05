package com.focushive.test;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.mock;

/**
 * DEPRECATED: This configuration conflicts with TestMockConfig.
 * All test configurations have been consolidated into TestMockConfig.
 * This class is kept for compatibility with existing imports but provides no beans.
 */
@TestConfiguration
@Profile("deprecated-minimal-test")  // Changed profile to avoid conflicts
public class MinimalTestConfig {
    // All beans moved to TestMockConfig to avoid @Primary conflicts
}