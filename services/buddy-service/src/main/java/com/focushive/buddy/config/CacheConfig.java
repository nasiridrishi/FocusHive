package com.focushive.buddy.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache configuration for buddy-service.
 * Provides Redis-based caching with TTL policies, eviction strategies,
 * and graceful failure handling.
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    @Value("${spring.cache.redis.time-to-live:3600000}")
    private long defaultTtl;

    @Value("${spring.cache.redis.cache-null-values:false}")
    private boolean cacheNullValues;

    @Value("${spring.cache.redis.key-prefix:buddy:}")
    private String keyPrefix;

    // Cache names constants
    public static final String USER_PREFERENCES_CACHE = "userPreferences";
    public static final String PARTNERSHIPS_CACHE = "partnerships";
    public static final String ACTIVE_GOALS_CACHE = "activeGoals";
    public static final String COMPATIBILITY_CACHE = "compatibility";
    public static final String MATCHING_QUEUE_CACHE = "matchingQueue";
    public static final String GOAL_ANALYTICS_CACHE = "goalAnalytics";
    public static final String USER_ACHIEVEMENTS_CACHE = "userAchievements";
    public static final String TEMPLATES_CACHE = "templates";

    @Bean
    @Primary
    @Override
    public CacheManager cacheManager() {
        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager
                .RedisCacheManagerBuilder
                .fromConnectionFactory(redisConnectionFactory())
                .cacheDefaults(createDefaultCacheConfiguration());

        // Configure cache-specific settings
        Map<String, RedisCacheConfiguration> cacheConfigurations = createCacheConfigurations();
        builder.withInitialCacheConfigurations(cacheConfigurations);

        return builder.build();
    }

    /**
     * Create default cache configuration.
     * Applied to all caches unless overridden.
     */
    private RedisCacheConfiguration createDefaultCacheConfiguration() {
        // Create ObjectMapper with Java Time support and type information
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Configure polymorphic type handling for proper deserialization
        // This ensures objects are deserialized to their correct types, not LinkedHashMap
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("com.focushive.buddy.dto.")
            .allowIfSubType("com.focushive.buddy.entity.")
            .allowIfSubType("java.util.")
            .allowIfSubType("java.time.")
            .allowIfSubType("java.math.")  // Allow BigDecimal and other math types
            .allowIfSubType("java.lang.")  // Allow String, Integer, etc.
            .build();

        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY);

        // Use GenericJackson2JsonRedisSerializer which stores type information
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMillis(defaultTtl))
                .disableCachingNullValues()
                .prefixCacheNameWith(keyPrefix)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer));
    }

    /**
     * Configure cache-specific TTL and policies.
     * Different data types have different caching requirements.
     */
    private Map<String, RedisCacheConfiguration> createCacheConfigurations() {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // User preferences - medium TTL, frequently accessed
        cacheConfigurations.put(USER_PREFERENCES_CACHE,
                createDefaultCacheConfiguration()
                        .entryTtl(Duration.ofMinutes(30)));

        // Partnerships - longer TTL, less frequently changed
        cacheConfigurations.put(PARTNERSHIPS_CACHE,
                createDefaultCacheConfiguration()
                        .entryTtl(Duration.ofHours(2)));

        // Active goals - medium TTL, moderate changes
        cacheConfigurations.put(ACTIVE_GOALS_CACHE,
                createDefaultCacheConfiguration()
                        .entryTtl(Duration.ofHours(1)));

        // Compatibility scores - long TTL, expensive to calculate
        cacheConfigurations.put(COMPATIBILITY_CACHE,
                createDefaultCacheConfiguration()
                        .entryTtl(Duration.ofHours(24)));

        // Matching queue - short TTL, dynamic data
        cacheConfigurations.put(MATCHING_QUEUE_CACHE,
                createDefaultCacheConfiguration()
                        .entryTtl(Duration.ofMinutes(5)));

        // Goal analytics - medium TTL, CPU intensive
        cacheConfigurations.put(GOAL_ANALYTICS_CACHE,
                createDefaultCacheConfiguration()
                        .entryTtl(Duration.ofMinutes(30)));

        // User achievements - longer TTL, less frequently changed
        cacheConfigurations.put(USER_ACHIEVEMENTS_CACHE,
                createDefaultCacheConfiguration()
                        .entryTtl(Duration.ofHours(4)));

        // Templates - very long TTL, rarely changed
        cacheConfigurations.put(TEMPLATES_CACHE,
                createDefaultCacheConfiguration()
                        .entryTtl(Duration.ofHours(24)));

        return cacheConfigurations;
    }

    /**
     * Custom cache error handler for graceful failure handling.
     * When cache operations fail, the application continues normally
     * without caching (fallback to database).
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new GracefulCacheErrorHandler();
    }

    /**
     * Custom key generator for cache keys.
     * Ensures consistent and meaningful cache keys.
     */
    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return new BuddyServiceKeyGenerator();
    }

    /**
     * Graceful cache error handler that logs errors but doesn't break functionality.
     */
    public static class GracefulCacheErrorHandler extends SimpleCacheErrorHandler {

        @Override
        public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
            log.warn("Cache GET error for cache '{}' and key '{}': {}",
                    cache.getName(), key, exception.getMessage());
            // Don't throw exception - gracefully degrade to non-cached operation
        }

        @Override
        public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
            log.warn("Cache PUT error for cache '{}' and key '{}': {}",
                    cache.getName(), key, exception.getMessage());
            // Don't throw exception - gracefully degrade to non-cached operation
        }

        @Override
        public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
            log.warn("Cache EVICT error for cache '{}' and key '{}': {}",
                    cache.getName(), key, exception.getMessage());
            // Don't throw exception - gracefully degrade to non-cached operation
        }

        @Override
        public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
            log.warn("Cache CLEAR error for cache '{}': {}",
                    cache.getName(), exception.getMessage());
            // Don't throw exception - gracefully degrade to non-cached operation
        }
    }

    /**
     * Custom key generator for buddy service cache keys.
     */
    public static class BuddyServiceKeyGenerator implements KeyGenerator {

        @Override
        public Object generate(Object target, java.lang.reflect.Method method, Object... params) {
            StringBuilder keyBuilder = new StringBuilder();
            keyBuilder.append(target.getClass().getSimpleName())
                     .append(":")
                     .append(method.getName());

            for (Object param : params) {
                if (param != null) {
                    keyBuilder.append(":").append(param.toString());
                }
            }

            return keyBuilder.toString();
        }
    }

    // Dependency injection - this will be satisfied by existing RedisConfig
    private RedisConnectionFactory redisConnectionFactory;

    public CacheConfig(RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
    }

    private RedisConnectionFactory redisConnectionFactory() {
        return this.redisConnectionFactory;
    }
}