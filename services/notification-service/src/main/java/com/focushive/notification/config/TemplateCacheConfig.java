package com.focushive.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Configuration for template caching using Redis.
 * Provides multi-level caching for templates with different TTLs.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "cache.template.enabled", havingValue = "true", matchIfMissing = true)
public class TemplateCacheConfig {

    @Value("${cache.template.ttl.hours:24}")
    private int templateCacheTtlHours;

    @Value("${cache.template.rendered.ttl.hours:1}")
    private int renderedCacheTtlHours;

    @Value("${cache.template.metadata.ttl.hours:48}")
    private int metadataCacheTtlHours;

    @Value("${cache.template.prefix:notification:template:}")
    private String cachePrefix;

    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public TemplateCacheConfig(ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Create template cache manager with Redis.
     */
    @Bean
    @Primary
    public CacheManager templateCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheWriter cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory);

        // Default cache configuration
        RedisCacheConfiguration defaultConfig = templateCacheConfiguration();

        // Specific cache configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("templates", templateCacheConfiguration());
        cacheConfigurations.put("rendered-templates", renderedTemplateCacheConfiguration());
        cacheConfigurations.put("template-metadata", metadataCacheConfiguration());
        cacheConfigurations.put("template-variables", variablesCacheConfiguration());

        RedisCacheManager cacheManager = RedisCacheManager.builder(cacheWriter)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware() // Enable transaction support
            .build();

        // Register cache metrics
        registerCacheMetrics(cacheManager);

        log.info("Template cache manager configured with caches: {}", cacheConfigurations.keySet());
        return cacheManager;
    }

    /**
     * Configuration for template cache.
     */
    @Bean
    public RedisCacheConfiguration templateCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(templateCacheTtlHours))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer(objectMapper)))
            .computePrefixWith(cacheName -> cachePrefix + cacheName + ":")
            .disableCachingNullValues(); // Don't cache null values
    }

    /**
     * Configuration for rendered template cache with shorter TTL.
     */
    @Bean
    public RedisCacheConfiguration renderedTemplateCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(renderedCacheTtlHours))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new StringRedisSerializer())) // Rendered HTML is string
            .computePrefixWith(cacheName -> cachePrefix + cacheName + ":")
            .disableCachingNullValues();
    }

    /**
     * Configuration for template metadata cache.
     */
    private RedisCacheConfiguration metadataCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(metadataCacheTtlHours))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer(objectMapper)))
            .computePrefixWith(cacheName -> cachePrefix + cacheName + ":")
            .disableCachingNullValues();
    }

    /**
     * Configuration for template variables cache.
     */
    private RedisCacheConfiguration variablesCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1)) // Short TTL for variables
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer(objectMapper)))
            .computePrefixWith(cacheName -> cachePrefix + cacheName + ":")
            .disableCachingNullValues();
    }

    /**
     * Custom key generator for template caching.
     * Generates keys based on template name, language, and variables.
     */
    @Bean
    public KeyGenerator keyGenerator() {
        return new TemplateKeyGenerator();
    }

    /**
     * Template cache key generator.
     */
    public static class TemplateKeyGenerator implements KeyGenerator {
        @Override
        public Object generate(Object target, java.lang.reflect.Method method, Object... params) {
            StringJoiner joiner = new StringJoiner(":");

            // Add method-specific prefix
            if (method != null) {
                joiner.add(method.getName());
            }

            // Add parameters to key
            for (Object param : params) {
                if (param == null) {
                    joiner.add("null");
                } else if (param instanceof Map) {
                    // For maps (like template variables), create a sorted string representation
                    Map<?, ?> map = (Map<?, ?>) param;
                    joiner.add(createMapKey(map));
                } else {
                    joiner.add(param.toString());
                }
            }

            return joiner.toString();
        }

        private String createMapKey(Map<?, ?> map) {
            StringJoiner mapJoiner = new StringJoiner(",");
            map.entrySet().stream()
                .sorted((e1, e2) -> String.valueOf(e1.getKey()).compareTo(String.valueOf(e2.getKey())))
                .forEach(entry -> mapJoiner.add(entry.getKey() + "=" + entry.getValue()));
            return "{" + mapJoiner.toString() + "}";
        }
    }

    /**
     * Cache error handler for graceful degradation.
     */
    @Bean
    public CacheErrorHandler errorHandler() {
        return new TemplateCacheErrorHandler();
    }

    /**
     * Custom error handler that logs errors but doesn't throw exceptions.
     */
    public static class TemplateCacheErrorHandler implements CacheErrorHandler {
        @Override
        public void handleCacheGetError(RuntimeException exception,
                                       org.springframework.cache.Cache cache, Object key) {
            log.warn("Cache get error for cache: {} key: {}", cache.getName(), key, exception);
            // Continue without cache - graceful degradation
        }

        @Override
        public void handleCachePutError(RuntimeException exception,
                                       org.springframework.cache.Cache cache, Object key, Object value) {
            log.warn("Cache put error for cache: {} key: {}", cache.getName(), key, exception);
            // Continue without caching - graceful degradation
        }

        @Override
        public void handleCacheEvictError(RuntimeException exception,
                                        org.springframework.cache.Cache cache, Object key) {
            log.warn("Cache evict error for cache: {} key: {}", cache.getName(), key, exception);
            // Continue without eviction - will expire naturally
        }

        @Override
        public void handleCacheClearError(RuntimeException exception,
                                        org.springframework.cache.Cache cache) {
            log.warn("Cache clear error for cache: {}", cache.getName(), exception);
            // Continue without clearing - will expire naturally
        }
    }

    /**
     * Register cache metrics with Micrometer.
     */
    private void registerCacheMetrics(CacheManager cacheManager) {
        if (meterRegistry == null) {
            return;
        }

        cacheManager.getCacheNames().forEach(cacheName -> {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                // Register cache size gauge
                meterRegistry.gauge("cache.size",
                    cache,
                    c -> getCacheSize(c));

                log.debug("Registered metrics for cache: {}", cacheName);
            }
        });
    }

    /**
     * Get cache size (implementation specific).
     */
    private double getCacheSize(org.springframework.cache.Cache cache) {
        // This is a simplified implementation
        // In production, you'd need to access Redis-specific metrics
        try {
            if (cache.getNativeCache() instanceof org.springframework.data.redis.cache.RedisCache) {
                // Access Redis cache statistics if available
                return 0; // Placeholder - implement actual size retrieval
            }
        } catch (Exception e) {
            log.debug("Could not get cache size for: {}", cache.getName(), e);
        }
        return 0;
    }

    /**
     * Configure cache statistics collection.
     */
    public void configureCacheMetrics(CacheManager cacheManager, MeterRegistry meterRegistry) {
        cacheManager.getCacheNames().forEach(cacheName -> {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);

            // Register various cache metrics
            meterRegistry.gauge("cache.size", cache, c -> getCacheSize(c));

            // Note: functionCounter is not available in current MeterRegistry version
            // These metrics would need to be implemented using counter instruments
            // with manual increment calls in the cache service layer
        });
    }
}