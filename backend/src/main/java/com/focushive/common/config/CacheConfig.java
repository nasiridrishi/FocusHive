package com.focushive.common.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
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
 * Redis-based caching configuration for performance optimization
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(60)) // Default 1 hour TTL
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Configure specific cache TTLs based on data characteristics
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Short-lived caches (5 minutes) - frequently changing data
        cacheConfigurations.put("userPresence", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("activeFocusSession", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("hiveActiveUsers", defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put("hiveFocusSessions", defaultConfig.entryTtl(Duration.ofMinutes(3)));
        
        // Medium-lived caches (30 minutes) - moderately changing data
        cacheConfigurations.put("dailySummaries", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("dailySummaryRanges", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("hiveMembership", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("hiveUserIds", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        
        // Long-lived caches (2 hours) - stable data
        cacheConfigurations.put("userProfiles", defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigurations.put("hiveDetails", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("userSettings", defaultConfig.entryTtl(Duration.ofHours(2)));
        
        // Very long-lived caches (24 hours) - rarely changing data
        cacheConfigurations.put("systemSettings", defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigurations.put("notificationTemplates", defaultConfig.entryTtl(Duration.ofHours(12)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}