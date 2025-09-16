package com.focushive.notification.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
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
 * Cache configuration for query result caching.
 * Implements proper Redis caching with TTL and eviction policies.
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    /**
     * Configure Redis cache manager with proper serialization and TTL.
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Configure Jackson serializer for cache values with type information
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Enable default typing to preserve class information during serialization
        // This prevents ClassCastException when deserializing cached objects
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .allowIfSubType("com.focushive.notification.")
                .allowIfSubType("java.")
                .build();

        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        // Cache-specific configurations
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // Notification cache - shorter TTL for frequently changing data
        cacheConfigs.put("notifications", defaultConfig
                .entryTtl(Duration.ofMinutes(5)));

        // User notifications cache
        cacheConfigs.put("userNotifications", defaultConfig
                .entryTtl(Duration.ofMinutes(3)));

        // Notification count cache - very short TTL
        cacheConfigs.put("notificationCount", defaultConfig
                .entryTtl(Duration.ofMinutes(1)));

        // Templates cache - longer TTL for rarely changing data
        cacheConfigs.put("notificationTemplates", defaultConfig
                .entryTtl(Duration.ofHours(1)));

        // Preferences cache - medium TTL
        cacheConfigs.put("notificationPreferences", defaultConfig
                .entryTtl(Duration.ofMinutes(15)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }

    /**
     * Custom key generator for complex method parameters.
     */
    @Bean
    public KeyGenerator customKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName());
            sb.append(":");
            sb.append(method.getName());
            for (Object param : params) {
                sb.append(":");
                if (param != null) {
                    sb.append(param.toString());
                } else {
                    sb.append("null");
                }
            }
            return sb.toString();
        };
    }

    /**
     * Configure Redis cache manager customizer for additional settings.
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return builder -> builder
                .enableStatistics()
                .withInitialCacheConfigurations(getCacheConfigurations());
    }

    private Map<String, RedisCacheConfiguration> getCacheConfigurations() {
        Map<String, RedisCacheConfiguration> configs = new HashMap<>();

        // Configure cache statistics collection
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("notification-service:")
                .entryTtl(Duration.ofMinutes(10));

        configs.put("default", config);
        return configs;
    }
}