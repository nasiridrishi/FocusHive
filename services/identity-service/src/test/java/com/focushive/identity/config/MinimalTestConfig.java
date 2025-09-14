package com.focushive.identity.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.focushive.identity.config.SecurityHeadersProperties;
import com.focushive.identity.service.RedisRateLimiter;
import com.focushive.identity.service.TokenBlacklistService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.Mockito.mock;

/**
 * Minimal test configuration to provide essential beans for integration tests.
 * This configuration provides only the absolutely necessary beans to get tests running.
 */
@TestConfiguration
public class MinimalTestConfig {
    
    /**
     * ObjectMapper for JSON serialization in tests.
     * Marked as @Primary to ensure it's used over any other ObjectMapper beans.
     */
    @Bean
    @Primary
    public ObjectMapper testObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
    
    /**
     * Mock JedisConnectionFactory to provide Redis connection.
     */
    @Bean
    @Primary
    public JedisConnectionFactory mockJedisConnectionFactory() {
        return mock(JedisConnectionFactory.class);
    }
    
    /**
     * Mock RedisTemplate for tests to replace the real Redis dependency.
     * This prevents the need for a real Redis instance during integration tests.
     */
    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, String> mockRedisTemplate() {
        return mock(RedisTemplate.class);
    }
    
    /**
     * Mock RedisTemplate for JSON objects to replace the real Redis dependency.
     */
    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, Object> mockJsonRedisTemplate() {
        return mock(RedisTemplate.class);
    }
    
    /**
     * Mock RedisRateLimiter to break the circular dependency chain.
     */
    @Bean
    @Primary
    public RedisRateLimiter mockRedisRateLimiter() {
        return mock(RedisRateLimiter.class);
    }
    
    /**
     * Mock SecurityHeadersProperties to avoid configuration binding issues.
     */
    @Bean
    @Primary
    public SecurityHeadersProperties mockSecurityHeadersProperties() {
        return mock(SecurityHeadersProperties.class);
    }
    
    /**
     * PasswordEncoder for authentication tests.
     */
    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * Mock AuthenticationManager for authentication tests.
     */
    @Bean
    @Primary
    public AuthenticationManager mockAuthenticationManager() {
        return mock(AuthenticationManager.class);
    }
    
    /**
     * Mock StringRedisTemplate for tests.
     */
    @Bean
    @Primary
    public StringRedisTemplate mockStringRedisTemplate() {
        return mock(StringRedisTemplate.class);
    }
    
    /**
     * Mock TokenBlacklistService for tests to break dependency chain.
     */
    @Bean
    @Primary
    public TokenBlacklistService mockTokenBlacklistService() {
        return mock(TokenBlacklistService.class);
    }
}
