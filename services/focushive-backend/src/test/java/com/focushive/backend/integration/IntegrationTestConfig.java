package com.focushive.backend.integration;

import com.focushive.backend.client.fallback.IdentityServiceFallback;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.mock;

/**
 * Test configuration specific to integration tests.
 * This configuration provides real beans for integration testing while mocking only necessary external services.
 */
@TestConfiguration
@Profile("test") 
@Import(FeignClientsConfiguration.class)
public class IntegrationTestConfig {

    /**
     * In-memory cache manager to replace Redis-based caching.
     * Provides actual caching functionality for tests without external dependencies.
     */
    @Bean
    @Primary
    public CacheManager testCacheManager() {
        return new ConcurrentMapCacheManager(
            // Identity service caches
            "identity-user",
            "identity",
            "identity-email",
            "personas", 
            "persona",
            "active-persona",
            "current-user"
        );
    }

    /**
     * Mock Redis dependencies that are not needed for this integration test.
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary 
    public RedisTemplate<String, Object> redisTemplate() {
        return mock(RedisTemplate.class);
    }

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate() {
        return mock(StringRedisTemplate.class);
    }

    @Bean
    @Primary
    public SimpMessagingTemplate simpMessagingTemplate() {
        return mock(SimpMessagingTemplate.class);
    }

    // We don't mock the IdentityServiceClient here - let Feign create the real client
    // We provide real fallback service for circuit breaker testing
}