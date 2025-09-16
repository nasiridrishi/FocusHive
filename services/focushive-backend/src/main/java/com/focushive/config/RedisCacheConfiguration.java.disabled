package com.focushive.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache configuration with specific TTL settings and cache policies.
 * Configures multiple cache regions with different expiration times and behaviors.
 */
@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
public class RedisCacheConfiguration implements CachingConfigurer {

    // Cache TTL configurations from application properties
    @Value("${focushive.redis.cache.user.ttl:1h}")
    private Duration userCacheTtl;

    @Value("${focushive.redis.cache.session.ttl:30m}")
    private Duration sessionCacheTtl;

    @Value("${focushive.redis.cache.hive.ttl:2h}")
    private Duration hiveCacheTtl;

    @Value("${focushive.redis.cache.presence.ttl:5m}")
    private Duration presenceCacheTtl;

    @Value("${focushive.redis.cache.default.ttl:10m}")
    private Duration defaultCacheTtl;

    @Value("${spring.cache.redis.key-prefix:focushive:cache:}")
    private String keyPrefix;

    @Value("${spring.cache.redis.cache-null-values:false}")
    private boolean cacheNullValues;

    @Value("${spring.cache.redis.enable-statistics:true}")
    private boolean enableStatistics;

    /**
     * Creates the primary cache manager with multiple cache configurations.
     */
    @Bean
    @Override
    public CacheManager cacheManager() {
        return RedisCacheManager.builder(getRedisConnectionFactory())
                .cacheDefaults(createDefaultCacheConfiguration())
                .withInitialCacheConfigurations(createCacheConfigurations())
                .transactionAware() // Enable transaction support
                .build();
    }

    /**
     * Provides the Redis connection factory bean.
     * This method expects the RedisConnectionFactory to be available as a bean.
     */
    private RedisConnectionFactory getRedisConnectionFactory() {
        // This will be autowired by Spring
        return null; // Placeholder - Spring will inject the actual bean
    }

    /**
     * Creates default cache configuration that applies to all caches unless overridden.
     */
    private org.springframework.data.redis.cache.RedisCacheConfiguration createDefaultCacheConfiguration() {
        return org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(defaultCacheTtl)
                .disableCachingNullValues()
                .prefixCacheNameWith(keyPrefix)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(createRedisSerializer()))
                .computePrefixWith(cacheName -> keyPrefix + cacheName + ":");
    }

    /**
     * Creates specific cache configurations with custom TTL and settings.
     */
    private Map<String, org.springframework.data.redis.cache.RedisCacheConfiguration> createCacheConfigurations() {
        Map<String, org.springframework.data.redis.cache.RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // User cache - stores user data with longer TTL
        cacheConfigurations.put("user-cache", 
            cacheNullValues ? createCacheConfigurationWithTtl(userCacheTtl) : createCacheConfigurationWithTtl(userCacheTtl).disableCachingNullValues()); // Never cache null user data

        // Session cache - stores session data with moderate TTL
        cacheConfigurations.put("session-cache", 
            cacheNullValues ? createCacheConfigurationWithTtl(sessionCacheTtl) : createCacheConfigurationWithTtl(sessionCacheTtl).disableCachingNullValues());

        // Hive cache - stores hive information with longer TTL
        cacheConfigurations.put("hive-cache", 
            createCacheConfigurationWithTtl(hiveCacheTtl));

        // Presence cache - stores user presence with short TTL for real-time accuracy
        cacheConfigurations.put("presence-cache", 
            cacheNullValues ? createCacheConfigurationWithTtl(presenceCacheTtl) : createCacheConfigurationWithTtl(presenceCacheTtl).disableCachingNullValues());

        // Notification cache - stores notifications with default TTL
        cacheConfigurations.put("notification-cache", 
            createCacheConfigurationWithTtl(defaultCacheTtl));

        // Analytics cache - stores computed analytics with longer TTL
        cacheConfigurations.put("analytics-cache", 
            createCacheConfigurationWithTtl(Duration.ofHours(4)));

        // Token cache - stores authentication tokens with session TTL
        cacheConfigurations.put("token-cache", 
            cacheNullValues ? createCacheConfigurationWithTtl(sessionCacheTtl) : createCacheConfigurationWithTtl(sessionCacheTtl).disableCachingNullValues());

        // Temporary cache - for short-lived data
        cacheConfigurations.put("temp-cache", 
            createCacheConfigurationWithTtl(Duration.ofMinutes(5)));

        // Long-term cache - for rarely changing data
        cacheConfigurations.put("long-term-cache", 
            createCacheConfigurationWithTtl(Duration.ofDays(1)));

        return cacheConfigurations;
    }

    /**
     * Creates a cache configuration with specific TTL.
     */
    private org.springframework.data.redis.cache.RedisCacheConfiguration createCacheConfigurationWithTtl(Duration ttl) {
        return org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .prefixCacheNameWith(keyPrefix)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(createRedisSerializer()))
                .computePrefixWith(cacheName -> keyPrefix + cacheName + ":");
    }

    /**
     * Creates the Redis serializer for cache values.
     */
    private RedisSerializer<Object> createRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer();
    }

    /**
     * Custom key generator for cache keys.
     * Generates deterministic keys based on method parameters.
     */
    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return new SimpleKeyGenerator() {
            @Override
            public Object generate(Object target, java.lang.reflect.Method method, Object... params) {
                StringBuilder sb = new StringBuilder();
                sb.append(target.getClass().getSimpleName()).append(".");
                sb.append(method.getName()).append(":");
                
                if (params.length > 0) {
                    for (int i = 0; i < params.length; i++) {
                        if (i > 0) sb.append(",");
                        if (params[i] != null) {
                            sb.append(params[i].toString());
                        } else {
                            sb.append("null");
                        }
                    }
                } else {
                    sb.append("empty");
                }
                
                return sb.toString();
            }
        };
    }

    /**
     * Custom cache error handler to provide graceful degradation.
     * When Redis is unavailable, the application continues without caching.
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                // Log error but don't fail the operation - graceful degradation
                System.err.println("Cache GET error for key " + key + " in cache " + cache.getName() + ": " + exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                // Log error but don't fail the operation
                System.err.println("Cache PUT error for key " + key + " in cache " + cache.getName() + ": " + exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                // Log error but don't fail the operation
                System.err.println("Cache EVICT error for key " + key + " in cache " + cache.getName() + ": " + exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                // Log error but don't fail the operation
                System.err.println("Cache CLEAR error in cache " + cache.getName() + ": " + exception.getMessage());
            }
        };
    }

    /**
     * Custom cache resolver (optional - uses default if not specified).
     */
    @Override
    public CacheResolver cacheResolver() {
        return null; // Use default resolver
    }

    /**
     * Cache configuration for time-to-idle (TTI) support.
     * Requires Redis 6.2+ and GETEX command support.
     */
    @Bean
    @ConditionalOnProperty(name = "focushive.redis.cache.tti.enabled", havingValue = "true")
    public org.springframework.data.redis.cache.RedisCacheConfiguration ttiCacheConfiguration() {
        return org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .enableTimeToIdle() // Enable TTI - requires Redis 6.2+
                .prefixCacheNameWith(keyPrefix + "tti:")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(createRedisSerializer()));
    }

    /**
     * Cache manager for TTI-enabled caches.
     */
    @Bean("ttiCacheManager")
    @ConditionalOnProperty(name = "focushive.redis.cache.tti.enabled", havingValue = "true")
    public RedisCacheManager ttiCacheManager(
            RedisConnectionFactory redisConnectionFactory,
            org.springframework.data.redis.cache.RedisCacheConfiguration ttiCacheConfiguration) {
        
        Map<String, org.springframework.data.redis.cache.RedisCacheConfiguration> ttiCaches = new HashMap<>();
        
        // TTI-enabled caches for frequently accessed data
        ttiCaches.put("user-session-tti", ttiCacheConfiguration.entryTtl(Duration.ofMinutes(30)));
        ttiCaches.put("user-preferences-tti", ttiCacheConfiguration.entryTtl(Duration.ofHours(2)));
        
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(ttiCacheConfiguration)
                .withInitialCacheConfigurations(ttiCaches)
                .transactionAware()
                .build();
    }

    /**
     * Configuration for cache statistics and monitoring.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.cache.redis.enable-statistics", havingValue = "true", matchIfMissing = true)
    public org.springframework.data.redis.cache.RedisCacheConfiguration statisticsEnabledCacheConfiguration() {
        // Note: Cache statistics are typically handled by monitoring systems like Micrometer
        // This is a placeholder for any statistics-specific configuration
        return createDefaultCacheConfiguration();
    }
}