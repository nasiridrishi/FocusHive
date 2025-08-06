package com.focushive.backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache configuration for Identity Service integration.
 * Uses Redis for distributed caching with different TTLs for different cache types.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Configure default cache settings
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Configure specific cache settings
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Short-lived caches (2 minutes)
        cacheConfigurations.put("active-persona", defaultConfig.entryTtl(Duration.ofMinutes(2)));
        
        // Medium-lived caches (5 minutes)
        cacheConfigurations.put("current-user", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("personas", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Long-lived caches (10 minutes)
        cacheConfigurations.put("identity", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("identity-email", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("persona", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
    
    /**
     * Fallback cache manager for testing or when Redis is not available.
     */
    @Bean
    public CacheManager fallbackCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(java.util.Arrays.asList(
                new ConcurrentMapCache("current-user"),
                new ConcurrentMapCache("identity"),
                new ConcurrentMapCache("identity-email"),
                new ConcurrentMapCache("personas"),
                new ConcurrentMapCache("active-persona"),
                new ConcurrentMapCache("persona")
        ));
        return cacheManager;
    }
}