package com.focushive.identity.service;

import com.focushive.identity.config.CacheConfig;
import com.focushive.identity.entity.User;
import com.focushive.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Integration tests for caching functionality in CustomUserDetailsService
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cache.type=redis",
    "spring.redis.host=localhost",
    "spring.redis.port=6380"
})
@Transactional
class CustomUserDetailsServiceCacheTest {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private UserRepository userRepository;

    private User testUser;
    private Cache userCache;
    private Cache userProfileCache;

    @BeforeEach
    void setUp() {
        // Clear caches before each test
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });

        userCache = cacheManager.getCache(CacheConfig.USER_CACHE);
        userProfileCache = cacheManager.getCache(CacheConfig.USER_PROFILE_CACHE);

        // Create test user
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
    }

    @Test
    void loadUserByUsername_CachesUser_OnFirstCall() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // When - First call
        UserDetails result1 = userDetailsService.loadUserByUsername("test@example.com");

        // Then - Should query repository
        verify(userRepository, times(1)).findByEmail("test@example.com");
        assertThat(result1).isNotNull();
        assertThat(result1.getUsername()).isEqualTo("testuser");

        // Verify cache contains the user
        Cache.ValueWrapper cachedValue = userCache.get("test@example.com");
        assertThat(cachedValue).isNotNull();
        assertThat(cachedValue.get()).isEqualTo(testUser);
    }

    @Test
    void loadUserByUsername_UsesCache_OnSecondCall() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // When - First call to populate cache
        userDetailsService.loadUserByUsername("test@example.com");

        // Reset mock
        reset(userRepository);

        // When - Second call should use cache
        UserDetails result2 = userDetailsService.loadUserByUsername("test@example.com");

        // Then - Should not query repository again
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).findByUsername(anyString());
        assertThat(result2).isNotNull();
        assertThat(result2.getUsername()).isEqualTo("testuser");
    }

    @Test
    void loadUserByUsername_DoesNotCacheNull_WhenUserNotFound() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then - Should throw exception
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);

        // Verify no value is cached
        Cache.ValueWrapper cachedValue = userCache.get("nonexistent@example.com");
        assertThat(cachedValue).isNull();
    }

    @Test
    void loadUserById_CachesUser_OnFirstCall() {
        // Given
        String userId = testUser.getId().toString();
        when(userRepository.findActiveById(testUser.getId())).thenReturn(Optional.of(testUser));

        // When - First call
        User result1 = userDetailsService.loadUserById(userId);

        // Then - Should query repository
        verify(userRepository, times(1)).findActiveById(testUser.getId());
        assertThat(result1).isNotNull();
        assertThat(result1.getId()).isEqualTo(testUser.getId());

        // Verify cache contains the user
        Cache.ValueWrapper cachedValue = userProfileCache.get(userId);
        assertThat(cachedValue).isNotNull();
        assertThat(cachedValue.get()).isEqualTo(testUser);
    }

    @Test
    void loadUserById_UsesCache_OnSecondCall() {
        // Given
        String userId = testUser.getId().toString();
        when(userRepository.findActiveById(testUser.getId())).thenReturn(Optional.of(testUser));

        // When - First call to populate cache
        userDetailsService.loadUserById(userId);

        // Reset mock
        reset(userRepository);

        // When - Second call should use cache
        User result2 = userDetailsService.loadUserById(userId);

        // Then - Should not query repository again
        verify(userRepository, never()).findActiveById(any());
        assertThat(result2).isNotNull();
        assertThat(result2.getId()).isEqualTo(testUser.getId());
    }

    @Test
    void evictUserCache_ClearsAllUserCaches() {
        // Given - Populate caches
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        String userId = testUser.getId().toString();
        when(userRepository.findActiveById(testUser.getId())).thenReturn(Optional.of(testUser));

        userDetailsService.loadUserByUsername("test@example.com");
        userDetailsService.loadUserById(userId);

        // Verify caches are populated
        assertThat(userCache.get("test@example.com")).isNotNull();
        assertThat(userProfileCache.get(userId)).isNotNull();

        // When - Evict cache
        userDetailsService.evictUserCache("test@example.com");

        // Then - Caches should be cleared
        assertThat(userCache.get("test@example.com")).isNull();
        // Note: evictUserCache clears all entries, so user profile cache will also be empty
    }

    @Test
    void evictUserCacheById_ClearsSpecificUserCache() {
        // Given - Populate cache
        String userId = testUser.getId().toString();
        when(userRepository.findActiveById(testUser.getId())).thenReturn(Optional.of(testUser));

        userDetailsService.loadUserById(userId);

        // Verify cache is populated
        assertThat(userProfileCache.get(userId)).isNotNull();

        // When - Evict specific user cache
        userDetailsService.evictUserCacheById(userId);

        // Then - Cache should be cleared for this specific user
        assertThat(userProfileCache.get(userId)).isNull();
    }

    @Test
    void cachePerformanceImprovement_VerifyFasterSecondCall() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // When - Measure first call (with DB query)
        long startTime1 = System.nanoTime();
        userDetailsService.loadUserByUsername("test@example.com");
        long duration1 = System.nanoTime() - startTime1;

        // Reset mock to ensure second call doesn't hit DB
        reset(userRepository);

        // When - Measure second call (from cache)
        long startTime2 = System.nanoTime();
        userDetailsService.loadUserByUsername("test@example.com");
        long duration2 = System.nanoTime() - startTime2;

        // Then - Second call should be significantly faster
        // Note: This is a rough test - actual performance gains will be much more significant in real scenarios
        assertThat(duration2).isLessThan(duration1);
        
        // Verify repository was not called on second invocation
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).findByUsername(anyString());
    }
}