package com.focushive.notification.config;

import com.focushive.notification.entity.NotificationTemplate;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * Test configuration for integration tests.
 * Provides test-specific bean configurations.
 */
@TestConfiguration
public class NotificationTemplateTestConfiguration {

    /**
     * Create a mock RedisTemplate for tests.
     */
    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, NotificationTemplate> redisTemplate() {
        RedisTemplate<String, NotificationTemplate> mockRedisTemplate = Mockito.mock(RedisTemplate.class);
        ValueOperations<String, NotificationTemplate> mockValueOps = Mockito.mock(ValueOperations.class);
        
        // Configure the mock to return null for get operations (simulating cache miss)
        Mockito.when(mockValueOps.get(Mockito.anyString())).thenReturn(null);
        Mockito.when(mockRedisTemplate.opsForValue()).thenReturn(mockValueOps);
        
        return mockRedisTemplate;
    }
}