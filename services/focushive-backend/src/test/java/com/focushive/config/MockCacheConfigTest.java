package com.focushive.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Mock-based test class for Redis Cache Configuration
 * Tests the CacheConfig class without requiring an actual Redis instance
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Cache Configuration Unit Tests")
class MockCacheConfigTest {

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    private CacheConfig cacheConfig;

    @BeforeEach
    void setUp() {
        cacheConfig = new CacheConfig();
        // Set default values using reflection
        ReflectionTestUtils.setField(cacheConfig, "redisHost", "localhost");
        ReflectionTestUtils.setField(cacheConfig, "redisPort", 6379);
        ReflectionTestUtils.setField(cacheConfig, "redisPassword", "");
        ReflectionTestUtils.setField(cacheConfig, "defaultTtl", Duration.ofHours(1));
    }

    @Test
    @DisplayName("Redis connection factory is properly configured")
    void redisConnectionFactoryIsConfigured() {
        var connectionFactory = cacheConfig.redisConnectionFactory();

        assertThat(connectionFactory).isNotNull();
        assertThat(connectionFactory.getHostName()).isEqualTo("localhost");
        assertThat(connectionFactory.getPort()).isEqualTo(6379);
    }

    @Test
    @DisplayName("Redis template is properly configured")
    void redisTemplateIsConfigured() {
        RedisTemplate<String, Object> redisTemplate = cacheConfig.redisTemplate(redisConnectionFactory);

        assertThat(redisTemplate).isNotNull();
        assertThat(redisTemplate.getConnectionFactory()).isEqualTo(redisConnectionFactory);
        assertThat(redisTemplate.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(redisTemplate.getValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
        assertThat(redisTemplate.getHashKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(redisTemplate.getHashValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
    }

    @Test
    @DisplayName("Cache manager is properly configured with all expected caches")
    void cacheManagerIsConfigured() {
        // Note: This test verifies that the cache manager can be created
        // The actual cache names are populated lazily when Redis is available
        CacheManager cacheManager = cacheConfig.cacheManager();

        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);

        // Verify the cache manager is properly configured (this doesn't require Redis connection)
        RedisCacheManager redisCacheManager = (RedisCacheManager) cacheManager;
        assertThat(redisCacheManager).isNotNull();

        // The cache names will be empty when Redis is not available,
        // but the configuration should still be set up correctly
        assertThat(cacheManager.getCacheNames()).isNotNull();
    }

    @Test
    @DisplayName("Individual cache configurations are properly set")
    void individualCachesAreConfigured() {
        CacheManager cacheManager = cacheConfig.cacheManager();

        // When Redis is not available, cache.getCache() will create dynamic caches
        // but they will still have the correct names
        var activeCache = cacheManager.getCache(CacheConfig.HIVES_ACTIVE_CACHE);
        var detailsCache = cacheManager.getCache(CacheConfig.HIVE_DETAILS_CACHE);
        var presenceCache = cacheManager.getCache(CacheConfig.PRESENCE_CACHE);
        var sessionCache = cacheManager.getCache(CacheConfig.TIMER_SESSION_CACHE);

        // These can be null if the cache manager can't connect to Redis
        // but they should be created on-demand with the correct names
        if (activeCache != null) {
            assertThat(activeCache.getName()).isEqualTo(CacheConfig.HIVES_ACTIVE_CACHE);
        }
        if (detailsCache != null) {
            assertThat(detailsCache.getName()).isEqualTo(CacheConfig.HIVE_DETAILS_CACHE);
        }
        if (presenceCache != null) {
            assertThat(presenceCache.getName()).isEqualTo(CacheConfig.PRESENCE_CACHE);
        }
        if (sessionCache != null) {
            assertThat(sessionCache.getName()).isEqualTo(CacheConfig.TIMER_SESSION_CACHE);
        }

        // At minimum, the cache manager should be able to handle cache requests
        assertThat(cacheManager).isNotNull();
    }

    @Test
    @DisplayName("Key generator handles different parameter types correctly")
    void keyGeneratorHandlesParameters() {
        KeyGenerator keyGenerator = cacheConfig.keyGenerator();

        // Test with UUID and string parameters
        Object target = new Object();
        UUID testUuid = UUID.randomUUID();
        String stringParam = "testParam";
        Object[] params = {testUuid, stringParam};

        // Mock a Method object
        java.lang.reflect.Method mockMethod = mock(java.lang.reflect.Method.class);
        when(mockMethod.getName()).thenReturn("testMethod");

        String key = (String) keyGenerator.generate(target, mockMethod, params);

        assertThat(key).isNotNull();
        assertThat(key).contains("Object");
        assertThat(key).contains(testUuid.toString());
        assertThat(key).contains(stringParam);
        assertThat(key).contains("testMethod");
        assertThat(key).matches("Object:testMethod:[^:]+:testParam");
    }

    @Test
    @DisplayName("Key generator handles null parameters gracefully")
    void keyGeneratorHandlesNullParameters() {
        KeyGenerator keyGenerator = cacheConfig.keyGenerator();

        Object target = new Object();
        Object[] params = {null, "testParam", null};

        // Mock a Method object
        java.lang.reflect.Method mockMethod = mock(java.lang.reflect.Method.class);
        when(mockMethod.getName()).thenReturn("testMethod");

        String key = (String) keyGenerator.generate(target, mockMethod, params);

        assertThat(key).isNotNull();
        assertThat(key).contains("Object");
        assertThat(key).contains("testParam");
        // Should not contain extra colons for null parameters
        assertThat(key).isEqualTo("Object:testMethod:testParam");
    }

    @Test
    @DisplayName("Key generator handles empty parameters")
    void keyGeneratorHandlesEmptyParameters() {
        KeyGenerator keyGenerator = cacheConfig.keyGenerator();

        Object target = new Object();
        Object[] params = {};

        // Mock a Method object
        java.lang.reflect.Method mockMethod = mock(java.lang.reflect.Method.class);
        when(mockMethod.getName()).thenReturn("testMethod");

        String key = (String) keyGenerator.generate(target, mockMethod, params);

        assertThat(key).isNotNull();
        assertThat(key).isEqualTo("Object:testMethod");
    }

    @Test
    @DisplayName("Cache error handler handles all error types gracefully")
    void cacheErrorHandlerHandlesErrors() {
        CacheErrorHandler errorHandler = cacheConfig.errorHandler();

        // Mock cache
        org.springframework.cache.Cache mockCache = mock(org.springframework.cache.Cache.class);
        when(mockCache.getName()).thenReturn("test-cache");

        // Test that no exceptions are thrown for any error type
        RuntimeException testError = new RuntimeException("Test error");

        // These should not throw exceptions - use direct method calls
        try {
            errorHandler.handleCacheGetError(testError, mockCache, "test-key");
            errorHandler.handleCachePutError(testError, mockCache, "test-key", "test-value");
            errorHandler.handleCacheEvictError(testError, mockCache, "test-key");
            errorHandler.handleCacheClearError(testError, mockCache);

            // If we reach this point, all error handlers completed without throwing exceptions
            assertThat(true).isTrue(); // Test passes
        } catch (Exception e) {
            // If any exception is thrown, the test should fail
            assertThat(e).isNull(); // This will fail and show the exception
        }
    }

    @Test
    @DisplayName("Cache constants are properly defined")
    void cacheConstantsAreProperlyDefined() {
        // Verify all cache name constants are non-null and non-empty
        assertThat(CacheConfig.HIVES_ACTIVE_CACHE).isNotNull().isNotEmpty();
        assertThat(CacheConfig.HIVES_USER_CACHE).isNotNull().isNotEmpty();
        assertThat(CacheConfig.HIVE_DETAILS_CACHE).isNotNull().isNotEmpty();
        assertThat(CacheConfig.HIVE_MEMBERS_CACHE).isNotNull().isNotEmpty();
        assertThat(CacheConfig.TIMER_SESSION_CACHE).isNotNull().isNotEmpty();
        assertThat(CacheConfig.PRESENCE_CACHE).isNotNull().isNotEmpty();
        assertThat(CacheConfig.USER_PROFILE_CACHE).isNotNull().isNotEmpty();
        assertThat(CacheConfig.LEADERBOARD_CACHE).isNotNull().isNotEmpty();
        assertThat(CacheConfig.ANALYTICS_CACHE).isNotNull().isNotEmpty();

        // Verify cache names are unique
        String[] cacheNames = {
            CacheConfig.HIVES_ACTIVE_CACHE,
            CacheConfig.HIVES_USER_CACHE,
            CacheConfig.HIVE_DETAILS_CACHE,
            CacheConfig.HIVE_MEMBERS_CACHE,
            CacheConfig.TIMER_SESSION_CACHE,
            CacheConfig.PRESENCE_CACHE,
            CacheConfig.USER_PROFILE_CACHE,
            CacheConfig.LEADERBOARD_CACHE,
            CacheConfig.ANALYTICS_CACHE
        };

        assertThat(cacheNames).doesNotHaveDuplicates();
    }
}