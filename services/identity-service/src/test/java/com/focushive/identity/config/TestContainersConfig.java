package com.focushive.identity.config;

import com.focushive.identity.service.EmailService;
import com.focushive.identity.service.RedisRateLimiter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import static org.mockito.Mockito.mock;

/**
 * TestContainers-specific configuration for integration tests.
 * 
 * This configuration provides only the essential mocked beans that are not available 
 * in the TestContainers environment, while allowing real database beans to be used.
 */
@TestConfiguration
public class TestContainersConfig {

    /**
     * Mock RedisRateLimiter since we're not using Redis in this test.
     */
    @Bean
    @Primary
    public RedisRateLimiter mockRedisRateLimiter() {
        return mock(RedisRateLimiter.class);
    }

    /**
     * Mock EmailService for tests to prevent actual emails being sent.
     */
    @Bean
    @Primary
    public EmailService mockEmailService() {
        return mock(EmailService.class);
    }
    
    /**
     * Mock RedisTemplate since we're excluding Redis autoconfiguration.
     */
    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, String> mockRedisTemplate() {
        return mock(RedisTemplate.class);
    }
    
    /**
     * Mock JSON RedisTemplate since we're excluding Redis autoconfiguration.
     */
    @Bean
    @Primary
    @SuppressWarnings("unchecked") 
    public RedisTemplate<String, Object> mockJsonRedisTemplate() {
        return mock(RedisTemplate.class);
    }
    
    /**
     * Mock JedisConnectionFactory since we're excluding Redis autoconfiguration.
     */
    @Bean
    @Primary
    public JedisConnectionFactory mockJedisConnectionFactory() {
        return mock(JedisConnectionFactory.class);
    }
    
    /**
     * Mock SecurityHeadersProperties to prevent duplicate bean issues.
     */
    @Bean
    @Primary 
    public SecurityHeadersProperties mockSecurityHeadersProperties() {
        return mock(SecurityHeadersProperties.class);
    }
}