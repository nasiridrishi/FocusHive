package com.focushive.identity.config;

import org.junit.jupiter.api.Disabled;
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
 * Test class for Redis Cache Configuration
 * DISABLED: Redis tests are disabled in test profile to avoid external dependencies
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cache.type=redis",
    "spring.redis.host=localhost",
    "spring.redis.port=6380"
})
@Disabled("Redis cache tests are disabled to avoid external dependencies in test environment")
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
            CacheConfig.USER_CACHE,
            CacheConfig.USER_PROFILE_CACHE,
            CacheConfig.PERSONAS_CACHE,
            CacheConfig.OAUTH_CLIENT_CACHE,
            CacheConfig.JWT_VALIDATION_CACHE,
            CacheConfig.ROLE_CACHE,
            CacheConfig.PERMISSIONS_CACHE
        );
    }

    @Test
    void userCacheIsConfigured() {
        var cache = cacheManager.getCache(CacheConfig.USER_CACHE);
        assertThat(cache).isNotNull();
        assertThat(cache.getName()).isEqualTo(CacheConfig.USER_CACHE);
    }

    @Test
    void personasCacheIsConfigured() {
        var cache = cacheManager.getCache(CacheConfig.PERSONAS_CACHE);
        assertThat(cache).isNotNull();
        assertThat(cache.getName()).isEqualTo(CacheConfig.PERSONAS_CACHE);
    }

    @Test
    void jwtValidationCacheIsConfigured() {
        var cache = cacheManager.getCache(CacheConfig.JWT_VALIDATION_CACHE);
        assertThat(cache).isNotNull();
        assertThat(cache.getName()).isEqualTo(CacheConfig.JWT_VALIDATION_CACHE);
    }

    @Test
    void keyGeneratorGeneratesConsistentKeys() {
        Object target = new Object();
        String methodName = "testMethod";
        Object[] params = {"param1", "param2"};

        String key1 = (String) keyGenerator.generate(target, null, params);
        String key2 = (String) keyGenerator.generate(target, null, params);

        assertThat(key1).isEqualTo(key2);
        assertThat(key1).contains("Object");
        assertThat(key1).contains("param1");
        assertThat(key1).contains("param2");
    }

    @Test
    void redisConnectionFactoryIsConfigured() {
        assertThat(redisConnectionFactory).isNotNull();
        
        // Test connection
        var connection = redisConnectionFactory.getConnection();
        assertThat(connection).isNotNull();
        
        // Test ping
        assertThat(connection.ping()).isNotEmpty();
        
        connection.close();
    }

    @Test
    void redisTemplateHasCorrectSerializers() {
        assertThat(redisTemplate.getKeySerializer()).isNotNull();
        assertThat(redisTemplate.getValueSerializer()).isNotNull();
        assertThat(redisTemplate.getHashKeySerializer()).isNotNull();
        assertThat(redisTemplate.getHashValueSerializer()).isNotNull();
    }
}