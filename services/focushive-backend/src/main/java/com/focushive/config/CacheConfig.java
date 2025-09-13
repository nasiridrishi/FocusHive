package com.focushive.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CacheMeterBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis Cache Configuration for FocusHive Backend Service
 * 
 * Provides caching for:
 * - Active hives list
 * - User's hives
 * - Hive details
 * - Timer sessions
 * - Presence data
 * - User profiles (from identity service)
 */
@Slf4j
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class CacheConfig implements CachingConfigurer {

    // Cache Names Constants
    public static final String HIVES_ACTIVE_CACHE = "hives-active";
    public static final String HIVES_USER_CACHE = "hives-user";
    public static final String HIVE_DETAILS_CACHE = "hive-details";
    public static final String TIMER_SESSION_CACHE = "timer-sessions";
    public static final String PRESENCE_CACHE = "presence";
    public static final String USER_PROFILE_CACHE = "user-profiles";
    public static final String HIVE_MEMBERS_CACHE = "hive-members";
    public static final String LEADERBOARD_CACHE = "leaderboards";
    public static final String ANALYTICS_CACHE = "analytics";

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.cache.redis.time-to-live:3600000}")
    private Duration defaultTtl;

    /**
     * Redis Connection Factory with optimized configuration
     */
    @Bean
    @Primary
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }

        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(2))
                .shutdownTimeout(Duration.ofMillis(100))
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig);
        factory.setValidateConnection(true);
        
        return factory;
    }

    /**
     * Redis Template for manual cache operations
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper());

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.setDefaultSerializer(jsonSerializer);
        template.afterPropertiesSet();

        return template;
    }

    /**
     * ObjectMapper for Redis serialization
     */
    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /**
     * Cache Manager with domain-specific cache configurations
     */
    @Bean
    @Primary
    @Override
    public CacheManager cacheManager() {
        RedisCacheManager.Builder builder = RedisCacheManager.builder(redisConnectionFactory())
                .cacheDefaults(defaultCacheConfiguration())
                .withInitialCacheConfigurations(getCacheConfigurations())
                .transactionAware();

        RedisCacheManager cacheManager = builder.build();
        
        log.info("FocusHive Redis Cache Manager initialized with {} cache configurations", getCacheConfigurations().size());
        return cacheManager;
    }

    /**
     * Default cache configuration
     */
    private RedisCacheConfiguration defaultCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(defaultTtl)
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper())))
                .prefixCacheNameWith("focushive:");
    }

    /**
     * Cache-specific configurations optimized for FocusHive use cases
     */
    private Map<String, RedisCacheConfiguration> getCacheConfigurations() {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Active hives list - cached for 5 minutes (high read frequency, moderate change rate)
        cacheConfigurations.put(HIVES_ACTIVE_CACHE,
            defaultCacheConfiguration().entryTtl(Duration.ofMinutes(5)));

        // User's hives - cached for 15 minutes (personalized data, changes less frequently)
        cacheConfigurations.put(HIVES_USER_CACHE,
            defaultCacheConfiguration().entryTtl(Duration.ofMinutes(15)));

        // Hive details - cached for 30 minutes (detailed data, changes infrequently)
        cacheConfigurations.put(HIVE_DETAILS_CACHE,
            defaultCacheConfiguration().entryTtl(Duration.ofMinutes(30)));

        // Hive members - cached for 10 minutes (membership changes need faster updates)
        cacheConfigurations.put(HIVE_MEMBERS_CACHE,
            defaultCacheConfiguration().entryTtl(Duration.ofMinutes(10)));

        // Timer sessions - cached for 5 minutes (active data during focus sessions)
        cacheConfigurations.put(TIMER_SESSION_CACHE,
            defaultCacheConfiguration().entryTtl(Duration.ofMinutes(5)));

        // Presence data - cached for 1 minute (real-time data, very short TTL)
        cacheConfigurations.put(PRESENCE_CACHE,
            defaultCacheConfiguration().entryTtl(Duration.ofMinutes(1)));

        // User profiles from identity service - cached for 1 hour (stable data)
        cacheConfigurations.put(USER_PROFILE_CACHE,
            defaultCacheConfiguration().entryTtl(Duration.ofHours(1)));

        // Leaderboards - cached for 10 minutes (competitive data, needs regular updates)
        cacheConfigurations.put(LEADERBOARD_CACHE,
            defaultCacheConfiguration().entryTtl(Duration.ofMinutes(10)));

        // Analytics data - cached for 1 hour (aggregated data, expensive to compute)
        cacheConfigurations.put(ANALYTICS_CACHE,
            defaultCacheConfiguration().entryTtl(Duration.ofHours(1)));

        return cacheConfigurations;
    }

    /**
     * Custom key generator for FocusHive domain objects
     */
    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            StringBuilder key = new StringBuilder();
            key.append(target.getClass().getSimpleName()).append(":");
            key.append(method.getName()).append(":");
            
            for (Object param : params) {
                if (param != null) {
                    // Handle special object types
                    if (param instanceof java.util.UUID) {
                        key.append(param.toString());
                    } else if (param instanceof String) {
                        key.append(param);
                    } else {
                        key.append(param.toString());
                    }
                    key.append(":");
                }
            }
            
            // Remove trailing colon
            if (key.length() > 0 && key.charAt(key.length() - 1) == ':') {
                key.setLength(key.length() - 1);
            }
            
            return key.toString();
        };
    }

    /**
     * Cache error handler for graceful degradation
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Cache GET error for key: {} in cache: {}, error: {}", key, cache.getName(), exception.getMessage());
                // Graceful degradation - continue without cache
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                log.warn("Cache PUT error for key: {} in cache: {}, error: {}", key, cache.getName(), exception.getMessage());
                // Continue without caching
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Cache EVICT error for key: {} in cache: {}, error: {}", key, cache.getName(), exception.getMessage());
                // Continue without cache eviction
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                log.warn("Cache CLEAR error for cache: {}, error: {}", cache.getName(), exception.getMessage());
                // Continue without cache clearing
            }
        };
    }

    /**
     * Cache metrics for monitoring performance
     */
    @Bean
    public CacheMeterBinder cacheMeterBinder(CacheManager cacheManager, MeterRegistry meterRegistry) {
        return new CacheMeterBinder(cacheManager, "focushive-backend", java.util.Collections.emptyList());
    }
}