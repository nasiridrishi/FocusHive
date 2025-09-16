package com.focushive.notification.cache;

import com.focushive.notification.config.CacheConfig;
import com.focushive.notification.dto.NotificationDto;
import com.focushive.notification.dto.NotificationResponse;
import com.focushive.notification.entity.Notification.NotificationPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify proper Redis cache serialization/deserialization
 * of NotificationResponse objects to prevent ClassCastException.
 */
@SpringBootTest(classes = {CacheConfig.class})
@Testcontainers
@EnableCaching
@Import(CacheConfig.class)
class CacheSerializationIntegrationTest {

    @Container
    private static final GenericContainer<?> REDIS_CONTAINER =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private CacheManager cacheManager;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
        registry.add("spring.cache.type", () -> "redis");
    }

    @BeforeEach
    void setUp() {
        // Clear all caches before each test
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    @Test
    void testNotificationResponseCacheSerialization() {
        // Given
        String cacheName = "userNotifications";
        String cacheKey = "user-123:0:10:unsorted";

        // Create test notification response
        NotificationResponse testResponse = createTestNotificationResponse();

        // Get the cache
        var cache = cacheManager.getCache(cacheName);
        assertThat(cache).isNotNull();

        // When - Store in cache
        cache.put(cacheKey, testResponse);

        // Then - Retrieve from cache
        var cachedValue = cache.get(cacheKey);
        assertThat(cachedValue).isNotNull();
        assertThat(cachedValue.get()).isNotNull();

        // Verify the cached value is the correct type (not LinkedHashMap)
        Object retrievedObject = cachedValue.get();
        assertThat(retrievedObject).isInstanceOf(NotificationResponse.class);

        // Cast to NotificationResponse and verify content
        NotificationResponse retrievedResponse = (NotificationResponse) retrievedObject;
        assertThat(retrievedResponse.getNotifications()).hasSize(2);
        assertThat(retrievedResponse.getPage()).isEqualTo(0);
        assertThat(retrievedResponse.getSize()).isEqualTo(10);
        assertThat(retrievedResponse.getTotalElements()).isEqualTo(2);

        // Verify notification details are preserved
        NotificationDto firstNotification = retrievedResponse.getNotifications().get(0);
        assertThat(firstNotification.getUserId()).isEqualTo("user-123");
        assertThat(firstNotification.getTitle()).isEqualTo("Test Notification 1");
        assertThat(firstNotification.getType()).isEqualTo("SYSTEM_NOTIFICATION");
    }

    @Test
    void testNotificationDtoCacheSerialization() {
        // Given
        String cacheName = "notifications";
        String cacheKey = "notification:test-id";

        NotificationDto testDto = createTestNotificationDto("test-id", "Test Title");

        // Get the cache
        var cache = cacheManager.getCache(cacheName);
        assertThat(cache).isNotNull();

        // When - Store in cache
        cache.put(cacheKey, testDto);

        // Then - Retrieve from cache
        var cachedValue = cache.get(cacheKey);
        assertThat(cachedValue).isNotNull();
        assertThat(cachedValue.get()).isNotNull();

        // Verify the cached value is the correct type
        Object retrievedObject = cachedValue.get();
        assertThat(retrievedObject).isInstanceOf(NotificationDto.class);

        // Cast and verify content
        NotificationDto retrievedDto = (NotificationDto) retrievedObject;
        assertThat(retrievedDto.getId()).isEqualTo("test-id");
        assertThat(retrievedDto.getTitle()).isEqualTo("Test Title");
        assertThat(retrievedDto.getUserId()).isEqualTo("user-123");
        assertThat(retrievedDto.getType()).isEqualTo("SYSTEM_NOTIFICATION");
    }

    @Test
    void testComplexObjectGraphSerialization() {
        // Given - A complex NotificationResponse with nested objects
        String cacheName = "userNotifications";
        String cacheKey = "complex:user-456:page";

        NotificationResponse complexResponse = NotificationResponse.builder()
            .notifications(Arrays.asList(
                createTestNotificationDto("id1", "Title 1"),
                createTestNotificationDto("id2", "Title 2"),
                createTestNotificationDto("id3", "Title 3")
            ))
            .page(1)
            .size(20)
            .totalElements(100)
            .totalPages(5)
            .first(false)
            .last(false)
            .numberOfElements(3)
            .empty(false)
            .hasNext(true)
            .hasPrevious(true)
            .build();

        // Get the cache
        var cache = cacheManager.getCache(cacheName);
        assertThat(cache).isNotNull();

        // When - Store and retrieve
        cache.put(cacheKey, complexResponse);
        var cachedValue = cache.get(cacheKey);

        // Then - Verify type and content
        assertThat(cachedValue).isNotNull();
        Object retrievedObject = cachedValue.get();
        assertThat(retrievedObject).isInstanceOf(NotificationResponse.class);

        NotificationResponse retrieved = (NotificationResponse) retrievedObject;
        assertThat(retrieved.getNotifications()).hasSize(3);
        assertThat(retrieved.getTotalElements()).isEqualTo(100);
        assertThat(retrieved.isHasNext()).isTrue();
        assertThat(retrieved.isHasPrevious()).isTrue();

        // Verify nested notification objects
        retrieved.getNotifications().forEach(notification -> {
            assertThat(notification).isInstanceOf(NotificationDto.class);
            assertThat(notification.getUserId()).isEqualTo("user-123");
            assertThat(notification.getType()).isNotNull();
        });
    }

    @Test
    void testCacheClearAndEviction() {
        // Given
        String cacheName = "notificationCount";
        String cacheKey = "user-789";
        Long countValue = 42L;

        var cache = cacheManager.getCache(cacheName);
        assertThat(cache).isNotNull();

        // When - Store value
        cache.put(cacheKey, countValue);

        // Then - Verify it's stored
        var cachedValue = cache.get(cacheKey);
        assertThat(cachedValue).isNotNull();
        assertThat(cachedValue.get()).isEqualTo(42L);

        // When - Clear cache
        cache.clear();

        // Then - Verify it's cleared
        var clearedValue = cache.get(cacheKey);
        assertThat(clearedValue).isNull();
    }

    private NotificationResponse createTestNotificationResponse() {
        return NotificationResponse.builder()
            .notifications(Arrays.asList(
                createTestNotificationDto("id1", "Test Notification 1"),
                createTestNotificationDto("id2", "Test Notification 2")
            ))
            .page(0)
            .size(10)
            .totalElements(2)
            .totalPages(1)
            .first(true)
            .last(true)
            .numberOfElements(2)
            .empty(false)
            .hasNext(false)
            .hasPrevious(false)
            .build();
    }

    private NotificationDto createTestNotificationDto(String id, String title) {
        return NotificationDto.builder()
            .id(id)
            .userId("user-123")
            .type("SYSTEM_NOTIFICATION")
            .title(title)
            .content("Test content")
            .priority(NotificationPriority.NORMAL)
            .isRead(false)
            .data(new HashMap<>())
            .createdAt(LocalDateTime.now())
            .build();
    }
}