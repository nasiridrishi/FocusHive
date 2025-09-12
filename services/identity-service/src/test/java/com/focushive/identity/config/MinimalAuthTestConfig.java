package com.focushive.identity.config;

import com.focushive.identity.service.EmailService;
import com.focushive.identity.service.TokenBlacklistService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

/**
 * Minimal test configuration for basic authentication tests.
 * Avoids complex OAuth2 configurations to prevent circular dependencies.
 */
@TestConfiguration
@Profile("test")
public class MinimalAuthTestConfig {

    /**
     * Mock EmailService to avoid email sending in tests.
     */
    @Bean
    @Primary
    public EmailService emailService() {
        return Mockito.mock(EmailService.class);
    }

    /**
     * Mock RedisTemplate to avoid Redis dependency in tests.
     */
    @Bean
    @Primary
    public RedisTemplate<String, String> redisTemplate() {
        return Mockito.mock(RedisTemplate.class);
    }

    /**
     * Mock StringRedisTemplate to avoid Redis dependency in tests.
     */
    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate() {
        return Mockito.mock(StringRedisTemplate.class);
    }

    /**
     * Mock RedisConnectionFactory to avoid Redis dependency in tests.
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return Mockito.mock(RedisConnectionFactory.class);
    }

    /**
     * Password encoder for tests.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Mock TokenBlacklistService to avoid token blacklist dependencies.
     */
    @Bean
    @Primary
    public TokenBlacklistService tokenBlacklistService() {
        return Mockito.mock(TokenBlacklistService.class);
    }
}