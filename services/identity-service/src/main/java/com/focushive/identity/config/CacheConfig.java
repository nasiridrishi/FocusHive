package com.focushive.identity.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
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
 * Redis Cache Configuration for Identity Service
 * 
 * Provides comprehensive caching setup with:
 * - Multiple cache regions with different TTL values
 * - JSON serialization for complex objects
 * - Cache monitoring and metrics
 * - Error handling and fallback strategies
 * - Connection pooling optimization
 */
@Slf4j
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class CacheConfig implements CachingConfigurer {

    // Cache Names Constants
    public static final String USER_CACHE = "users";
    public static final String USER_PROFILE_CACHE = "user-profiles";
    public static final String PERSONAS_CACHE = "personas";
    public static final String OAUTH_CLIENT_CACHE = "oauth-clients";
    public static final String JWT_VALIDATION_CACHE = "jwt-validation";
    public static final String ROLE_CACHE = "roles";
    public static final String PERMISSIONS_CACHE = "permissions";

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.cache.redis.time-to-live:3600000}")
    private Duration defaultTtl;

    /**
     * Redis Connection Factory with optimized pool configuration
     */
    @Bean
    @Primary
    public LettuceConnectionFactory redisConnectionFactory() {
        // Configure Redis standalone connection
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }

        // Configure connection pooling
        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(2))
                .shutdownTimeout(Duration.ofMillis(100))
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig);
        factory.setValidateConnection(true);
        
        return factory;
    }

    /**
     * Redis Template with optimized serialization
     */
    @Bean
    public RedisTemplate<String, Object> cacheRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Configure serializers
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
     * Custom ObjectMapper for Redis serialization
     */
    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /**
     * Cache Manager with multiple cache configurations
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
        
        log.info("Redis Cache Manager initialized with {} cache configurations", getCacheConfigurations().size());
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
                .prefixCacheNameWith("identity:");
    }

    /**
     * Cache-specific configurations with different TTL values
     */
    private Map<String, RedisCacheConfiguration> getCacheConfigurations() {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // User profiles - cached for 1 hour (frequently accessed, changes moderately)
        cacheConfigurations.put(USER_CACHE, 
            defaultCacheConfiguration().entryTtl(Duration.ofHours(1)));

        cacheConfigurations.put(USER_PROFILE_CACHE,
            defaultCacheConfiguration().entryTtl(Duration.ofHours(1)));

        // Personas - cached for 2 hours (less frequent changes)
        cacheConfigurations.put(PERSONAS_CACHE,
            defaultCacheConfiguration().entryTtl(Duration.ofHours(2)));

        // OAuth client details - cached for 4 hours (rarely change)
        cacheConfigurations.put(OAUTH_CLIENT_CACHE,
            defaultCacheConfiguration().entryTtl(Duration.ofHours(4)));

        // JWT validation results - cached for 15 minutes (security sensitive)
        cacheConfigurations.put(JWT_VALIDATION_CACHE,
            defaultCacheConfiguration().entryTtl(Duration.ofMinutes(15)));

        // Roles and permissions - cached for 6 hours (very stable)
        cacheConfigurations.put(ROLE_CACHE,
            defaultCacheConfiguration().entryTtl(Duration.ofHours(6)));

        cacheConfigurations.put(PERMISSIONS_CACHE,
            defaultCacheConfiguration().entryTtl(Duration.ofHours(6)));

        return cacheConfigurations;
    }

    /**
     * Custom key generator for complex cache keys
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
                // Continue without cache - graceful degradation
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                log.warn("Cache PUT error for key: {} in cache: {}, error: {}", key, cache.getName(), exception.getMessage());
                // Continue without cache - graceful degradation
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Cache EVICT error for key: {} in cache: {}, error: {}", key, cache.getName(), exception.getMessage());
                // Continue without cache - graceful degradation
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                log.warn("Cache CLEAR error for cache: {}, error: {}", cache.getName(), exception.getMessage());
                // Continue without cache - graceful degradation
            }
        };
    }

    /**
     * Cache metrics for monitoring
     * Note: CacheMeterBinder is abstract in newer Micrometer versions.
     * Spring Boot auto-configures cache metrics, so we can remove this bean.
     * Metrics will be automatically registered for all cache managers.
     */
}