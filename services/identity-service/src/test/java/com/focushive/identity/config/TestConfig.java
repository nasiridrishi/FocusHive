package com.focushive.identity.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.service.EmailService;
import com.focushive.identity.service.TokenBlacklistService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Test configuration for Identity Service tests.
 * Provides mock implementations for external services and Redis.
 */
@TestConfiguration
@Profile("test")
public class TestConfig {
    
    /**
     * Mock EmailService for tests.
     */
    @Bean
    @Primary
    public EmailService mockEmailService() {
        return Mockito.mock(EmailService.class);
    }
    
    /**
     * Mock TokenBlacklistService for tests.
     */
    @Bean
    @Primary
    public TokenBlacklistService mockTokenBlacklistService() {
        TokenBlacklistService mockService = Mockito.mock(TokenBlacklistService.class);
        // By default, no tokens are blacklisted
        Mockito.when(mockService.isBlacklisted(Mockito.anyString())).thenReturn(false);
        return mockService;
    }
    
    /**
     * Mock Redis connection factory for tests when Redis is disabled.
     */
    @Bean
    @Primary
    public RedisConnectionFactory mockRedisConnectionFactory() {
        return Mockito.mock(RedisConnectionFactory.class);
    }
    
    /**
     * Mock RedisTemplate for tests.
     */
    @Bean
    @Primary
    public RedisTemplate<String, String> mockRedisTemplate() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> mockTemplate = Mockito.mock(RedisTemplate.class);
        return mockTemplate;
    }
    
    /**
     * Mock JSON RedisTemplate for tests.
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> mockJsonRedisTemplate() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> mockTemplate = Mockito.mock(RedisTemplate.class);
        return mockTemplate;
    }
    
}