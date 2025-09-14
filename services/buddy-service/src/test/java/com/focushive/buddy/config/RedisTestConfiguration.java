package com.focushive.buddy.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;

import java.time.Duration;

/**
 * Redis configuration specifically for testing.
 * Provides cache management and Redis template configuration
 * optimized for test scenarios.
 */
@TestConfiguration
@EnableCaching
@Profile("test")
public class RedisTestConfiguration {

    /**
     * Cache manager for testing with shorter TTL values
     */
    @Bean
    @Primary
    @Profile("test")
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(
                    org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(5)) // Shorter TTL for tests
                        .disableCachingNullValues()
                )
                .build();
    }

    /**
     * Redis template specifically configured for testing
     */
    @Bean(name = "testRedisTemplate")
    @Profile("test")
    public RedisTemplate<String, Object> testRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Configure serializers for test scenarios
        template.setDefaultSerializer(new org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer());
        template.setKeySerializer(new org.springframework.data.redis.serializer.StringRedisSerializer());
        template.setHashKeySerializer(new org.springframework.data.redis.serializer.StringRedisSerializer());
        template.setValueSerializer(new org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Helper bean for clearing Redis cache during tests
     */
    @Bean
    @Profile("test")
    public RedisTestHelper redisTestHelper(RedisTemplate<String, Object> redisTemplate) {
        return new RedisTestHelper(redisTemplate);
    }

    /**
     * Helper class for Redis operations in tests
     */
    public static class RedisTestHelper {
        private final RedisTemplate<String, Object> redisTemplate;

        public RedisTestHelper(RedisTemplate<String, Object> redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        /**
         * Clear all Redis keys matching a pattern
         */
        public void clearKeysWithPattern(String pattern) {
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }

        /**
         * Clear all Redis data
         */
        public void clearAll() {
            var keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }

        /**
         * Check if a key exists
         */
        public boolean exists(String key) {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        }

        /**
         * Get value by key
         */
        public Object getValue(String key) {
            return redisTemplate.opsForValue().get(key);
        }

        /**
         * Set value with key
         */
        public void setValue(String key, Object value) {
            redisTemplate.opsForValue().set(key, value);
        }

        /**
         * Set value with TTL
         */
        public void setValue(String key, Object value, Duration timeout) {
            redisTemplate.opsForValue().set(key, value, timeout);
        }

        /**
         * Get all keys matching pattern
         */
        public java.util.Set<String> getKeysWithPattern(String pattern) {
            return redisTemplate.keys(pattern);
        }
    }
}