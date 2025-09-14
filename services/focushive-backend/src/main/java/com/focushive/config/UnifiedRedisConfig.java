package com.focushive.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
 * TDD STEP 2: Unified Redis Configuration (Replaces both CacheConfig and RedisConfiguration)
 *
 * This configuration eliminates bean conflicts by providing a single source of truth
 * for Redis beans with proper conditions and no duplicate @Primary annotations.
 */
@Slf4j
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = false)
public class UnifiedRedisConfig implements CachingConfigurer {

    // Cache Names Constants
    public static final String HIVES_ACTIVE_CACHE = "hives-active";
    public static final String HIVES_USER_CACHE = "hives-user";
    public static final String HIVE_DETAILS_CACHE = "hive-details";
    public static final String TIMER_SESSION_CACHE = "timer-sessions";
    public static final String PRESENCE_CACHE = "presence";
    public static final String USER_PROFILE_CACHE = "user-profiles";

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.cache.redis.time-to-live:3600000}")
    private Duration defaultTtl;

    /**
     * TDD STEP 2.1: Single Redis Connection Factory (eliminates conflict)
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
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

        log.info("Unified Redis Connection Factory created: {}:{}", redisHost, redisPort);
        return factory;
    }

    /**
     * TDD STEP 2.2: Single Redis Template (eliminates conflict)
     */
    @Bean
    @Primary
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

        log.info("Unified Redis Template created");
        return template;
    }

    /**
     * TDD STEP 2.3: Cache Manager with domain-specific configurations
     */
    @Bean
    @Primary
    @Override
    public CacheManager cacheManager() {
        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(redisConnectionFactory())
                .cacheDefaults(defaultCacheConfiguration())
                .withInitialCacheConfigurations(getCacheConfigurations())
                .transactionAware();

        RedisCacheManager cacheManager = builder.build();

        log.info("Unified Cache Manager initialized with {} cache configurations", getCacheConfigurations().size());
        return cacheManager;
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
     * Cache-specific configurations for FocusHive domains
     */
    private Map<String, RedisCacheConfiguration> getCacheConfigurations() {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Active hives - 5 minutes TTL
        cacheConfigurations.put(HIVES_ACTIVE_CACHE,
            defaultCacheConfiguration().entryTtl(Duration.ofMinutes(5)));

        // User's hives - 15 minutes TTL
        cacheConfigurations.put(HIVES_USER_CACHE,
            defaultCacheConfiguration().entryTtl(Duration.ofMinutes(15)));

        // Hive details - 30 minutes TTL
        cacheConfigurations.put(HIVE_DETAILS_CACHE,
            defaultCacheConfiguration().entryTtl(Duration.ofMinutes(30)));

        // Timer sessions - 5 minutes TTL
        cacheConfigurations.put(TIMER_SESSION_CACHE,
            defaultCacheConfiguration().entryTtl(Duration.ofMinutes(5)));

        // Presence data - 1 minute TTL
        cacheConfigurations.put(PRESENCE_CACHE,
            defaultCacheConfiguration().entryTtl(Duration.ofMinutes(1)));

        // User profiles - 1 hour TTL
        cacheConfigurations.put(USER_PROFILE_CACHE,
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
                    key.append(param.toString()).append(":");
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
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                log.warn("Cache PUT error for key: {} in cache: {}, error: {}", key, cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Cache EVICT error for key: {} in cache: {}, error: {}", key, cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                log.warn("Cache CLEAR error for cache: {}, error: {}", cache.getName(), exception.getMessage());
            }
        };
    }
}