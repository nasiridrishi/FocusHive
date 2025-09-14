package com.focushive.buddy.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Test configuration for H2 database and mock Redis for fast unit tests.
 * This configuration provides in-memory alternatives for unit testing
 * without the overhead of TestContainers.
 */
@TestConfiguration
@Profile("test")
public class H2TestConfiguration {

    /**
     * Mock Redis connection factory for unit tests
     * Uses embedded Redis or mock implementation
     */
    @Bean
    @Primary
    @Profile("test")
    public RedisConnectionFactory redisConnectionFactory() {
        // For unit tests, we'll use the embedded Redis from TestContainers
        // or this can be replaced with a mock implementation
        return new LettuceConnectionFactory("localhost", 6379);
    }

    /**
     * Redis template configuration for testing
     */
    @Bean
    @Primary
    @Profile("test")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}