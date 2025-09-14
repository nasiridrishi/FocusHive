package com.focushive.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD STEP 3: Test the Unified Configuration
 *
 * These tests should PASS after implementing the fix,
 * proving that bean conflicts are resolved.
 */
@SpringBootTest(classes = UnifiedRedisConfig.class)
@TestPropertySource(properties = {
    "spring.cache.type=redis",
    "spring.redis.host=localhost",
    "spring.redis.port=6379",
    "spring.redis.password=",
    "spring.cache.redis.time-to-live=3600000"
})
public class UnifiedRedisConfigTest {

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Test
    @DisplayName("TDD SUCCESS: Should create Redis connection factory bean")
    void shouldCreateRedisConnectionFactory() {
        assertNotNull(redisConnectionFactory, "RedisConnectionFactory should be created");
        assertTrue(redisConnectionFactory.getConnection().isValid(), "Connection should be valid");
    }

    @Test
    @DisplayName("TDD SUCCESS: Should create Redis template bean")
    void shouldCreateRedisTemplate() {
        assertNotNull(redisTemplate, "RedisTemplate should be created");
        assertEquals(redisConnectionFactory, redisTemplate.getConnectionFactory(),
                     "RedisTemplate should use the same connection factory");
    }

    @Test
    @DisplayName("TDD SUCCESS: Should create cache manager with predefined caches")
    void shouldCreateCacheManager() {
        assertNotNull(cacheManager, "CacheManager should be created");

        // Verify expected caches exist
        assertNotNull(cacheManager.getCache(UnifiedRedisConfig.HIVES_ACTIVE_CACHE));
        assertNotNull(cacheManager.getCache(UnifiedRedisConfig.HIVES_USER_CACHE));
        assertNotNull(cacheManager.getCache(UnifiedRedisConfig.HIVE_DETAILS_CACHE));
        assertNotNull(cacheManager.getCache(UnifiedRedisConfig.TIMER_SESSION_CACHE));
        assertNotNull(cacheManager.getCache(UnifiedRedisConfig.PRESENCE_CACHE));
        assertNotNull(cacheManager.getCache(UnifiedRedisConfig.USER_PROFILE_CACHE));
    }

    @Test
    @DisplayName("TDD SUCCESS: Should have no bean conflicts")
    void shouldHaveNoBeanConflicts() {
        // If we get here without exceptions, no bean conflicts exist
        assertTrue(true, "Context loaded successfully without bean conflicts");
    }
}