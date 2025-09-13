package com.focushive.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for Redis Cache Configuration in FocusHive Backend
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cache.type=redis",
    "spring.redis.host=localhost",
    "spring.redis.port=6379"
})
class CacheConfigTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private KeyGenerator keyGenerator;

    @Autowired
    private CacheErrorHandler cacheErrorHandler;

    @Test
    void contextLoads() {
        assertThat(cacheManager).isNotNull();
        assertThat(redisConnectionFactory).isNotNull();
        assertThat(redisTemplate).isNotNull();
        assertThat(keyGenerator).isNotNull();
        assertThat(cacheErrorHandler).isNotNull();
    }

    @Test
    void cacheManagerHasExpectedCaches() {
        // Verify all expected cache names are configured
        assertThat(cacheManager.getCacheNames()).containsExactlyInAnyOrder(
            CacheConfig.HIVES_ACTIVE_CACHE,
            CacheConfig.HIVES_USER_CACHE,
            CacheConfig.HIVE_DETAILS_CACHE,
            CacheConfig.HIVE_MEMBERS_CACHE,
            CacheConfig.TIMER_SESSION_CACHE,
            CacheConfig.PRESENCE_CACHE,
            CacheConfig.USER_PROFILE_CACHE,
            CacheConfig.LEADERBOARD_CACHE,
            CacheConfig.ANALYTICS_CACHE
        );
    }

    @Test
    void hivesActiveCacheIsConfigured() {
        var cache = cacheManager.getCache(CacheConfig.HIVES_ACTIVE_CACHE);
        assertThat(cache).isNotNull();
        assertThat(cache.getName()).isEqualTo(CacheConfig.HIVES_ACTIVE_CACHE);
    }

    @Test
    void hiveDetailsCacheIsConfigured() {
        var cache = cacheManager.getCache(CacheConfig.HIVE_DETAILS_CACHE);
        assertThat(cache).isNotNull();
        assertThat(cache.getName()).isEqualTo(CacheConfig.HIVE_DETAILS_CACHE);
    }

    @Test
    void presenceCacheIsConfigured() {
        var cache = cacheManager.getCache(CacheConfig.PRESENCE_CACHE);
        assertThat(cache).isNotNull();
        assertThat(cache.getName()).isEqualTo(CacheConfig.PRESENCE_CACHE);
    }

    @Test
    void timerSessionCacheIsConfigured() {
        var cache = cacheManager.getCache(CacheConfig.TIMER_SESSION_CACHE);
        assertThat(cache).isNotNull();
        assertThat(cache.getName()).isEqualTo(CacheConfig.TIMER_SESSION_CACHE);
    }

    @Test
    void keyGeneratorHandlesUUIDParams() {
        Object target = new Object();
        String methodName = "testMethod";
        Object[] params = {java.util.UUID.randomUUID(), "stringParam"};

        String key = (String) keyGenerator.generate(target, null, params);

        assertThat(key).isNotNull();
        assertThat(key).contains("Object");
        assertThat(key).contains("stringParam");
    }

    @Test
    void redisTemplateCanPerformBasicOperations() {
        String testKey = "test:cache:key";
        String testValue = "test-value";

        // Test SET operation
        redisTemplate.opsForValue().set(testKey, testValue);

        // Test GET operation
        Object retrieved = redisTemplate.opsForValue().get(testKey);
        assertThat(retrieved).isEqualTo(testValue);

        // Clean up
        redisTemplate.delete(testKey);
    }

    @Test
    void cacheErrorHandlerIsConfigured() {
        assertThat(cacheErrorHandler).isNotNull();
        
        // Test that error handler doesn't throw exceptions
        org.springframework.cache.Cache cache = cacheManager.getCache(CacheConfig.HIVE_DETAILS_CACHE);
        
        // This should not throw an exception even if there's an error
        cacheErrorHandler.handleCacheGetError(new RuntimeException("Test error"), cache, "test-key");
        cacheErrorHandler.handleCachePutError(new RuntimeException("Test error"), cache, "test-key", "test-value");
        cacheErrorHandler.handleCacheEvictError(new RuntimeException("Test error"), cache, "test-key");
        cacheErrorHandler.handleCacheClearError(new RuntimeException("Test error"), cache);
    }
}