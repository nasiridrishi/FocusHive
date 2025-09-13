package com.focushive.hive.service;

import com.focushive.config.CacheConfig;
import com.focushive.hive.dto.HiveResponse;
import com.focushive.hive.entity.Hive;
import com.focushive.hive.repository.HiveMemberRepository;
import com.focushive.hive.repository.HiveRepository;
import com.focushive.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.StopWatch;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Performance tests for caching in HiveService
 * Validates that caching provides significant performance improvements
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cache.type=redis",
    "spring.redis.host=localhost",
    "spring.redis.port=6379",
    "spring.jpa.show-sql=false",
    "logging.level.com.focushive=WARN"
})
class HiveServiceCachePerformanceTest {

    @Autowired
    private HiveService hiveService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private HiveRepository hiveRepository;

    @MockBean
    private HiveMemberRepository hiveMemberRepository;

    private List<Hive> testHives;
    private User testUser;
    private String testUserId;

    @BeforeEach
    void setUp() {
        // Clear all caches before each test
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });

        // Setup test data
        testUserId = UUID.randomUUID().toString();
        testUser = new User();
        testUser.setId(testUserId);

        testHives = createTestHives(10);
    }

    @Test
    void listPublicHives_ShowsPerformanceImprovement_WithCache() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Hive> hivePage = new PageImpl<>(testHives, pageable, testHives.size());

        // Mock repository to simulate slow database query
        when(hiveRepository.findPublicHives(pageable))
                .thenAnswer(invocation -> {
                    // Simulate database latency
                    Thread.sleep(100);
                    return hivePage;
                });

        when(hiveMemberRepository.countByHiveId(anyString())).thenReturn(5L);

        StopWatch stopWatch = new StopWatch();

        // When - First call (should hit database)
        stopWatch.start("First call (DB query)");
        Page<HiveResponse> result1 = hiveService.listPublicHives(pageable);
        stopWatch.stop();

        // Verify repository was called
        verify(hiveRepository, times(1)).findPublicHives(pageable);
        assertThat(result1).isNotNull();
        assertThat(result1.getContent()).hasSize(10);

        // Reset mock for second call
        reset(hiveRepository);

        // When - Second call (should use cache)
        stopWatch.start("Second call (Cache hit)");
        Page<HiveResponse> result2 = hiveService.listPublicHives(pageable);
        stopWatch.stop();

        // Then - Verify performance improvement
        long firstCallTime = stopWatch.getTaskTimeMillis("First call (DB query)");
        long secondCallTime = stopWatch.getTaskTimeMillis("Second call (Cache hit)");

        System.out.println("Performance Test Results:");
        System.out.println("First call (DB): " + firstCallTime + "ms");
        System.out.println("Second call (Cache): " + secondCallTime + "ms");
        System.out.println("Performance improvement: " + 
            ((double)(firstCallTime - secondCallTime) / firstCallTime * 100) + "%");

        // Verify repository was not called on second invocation
        verify(hiveRepository, never()).findPublicHives(any());

        // Verify results are identical
        assertThat(result2).isNotNull();
        assertThat(result2.getContent()).hasSize(10);
        assertThat(result2.getContent().get(0).getId()).isEqualTo(result1.getContent().get(0).getId());

        // Performance assertion - cache should be at least 70% faster
        double performanceImprovement = (double)(firstCallTime - secondCallTime) / firstCallTime * 100;
        assertThat(performanceImprovement).isGreaterThan(70.0);
    }

    @Test
    void listUserHives_ShowsPerformanceImprovement_WithCache() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);

        // Mock repository with simulated latency
        when(hiveMemberRepository.findByUserId(testUserId, pageable))
                .thenAnswer(invocation -> {
                    Thread.sleep(50); // Simulate DB latency
                    return createTestHiveMembers(5, pageable);
                });

        when(hiveMemberRepository.countByHiveId(anyString())).thenReturn(3L);

        StopWatch stopWatch = new StopWatch();

        // When - First call
        stopWatch.start("First call (DB query)");
        Page<HiveResponse> result1 = hiveService.listUserHives(testUserId, pageable);
        stopWatch.stop();

        // Verify repository was called
        verify(hiveMemberRepository, times(1)).findByUserId(testUserId, pageable);
        assertThat(result1).isNotNull();

        // Reset mock
        reset(hiveMemberRepository);

        // When - Second call
        stopWatch.start("Second call (Cache hit)");
        Page<HiveResponse> result2 = hiveService.listUserHives(testUserId, pageable);
        stopWatch.stop();

        // Then - Verify performance and cache usage
        long firstCallTime = stopWatch.getTaskTimeMillis("First call (DB query)");
        long secondCallTime = stopWatch.getTaskTimeMillis("Second call (Cache hit)");

        System.out.println("User Hives Performance Test:");
        System.out.println("First call: " + firstCallTime + "ms");
        System.out.println("Second call: " + secondCallTime + "ms");

        // Verify no repository call on second invocation
        verify(hiveMemberRepository, never()).findByUserId(anyString(), any());

        // Performance should be significantly better
        assertThat(secondCallTime).isLessThan(firstCallTime);
        double performanceImprovement = (double)(firstCallTime - secondCallTime) / firstCallTime * 100;
        assertThat(performanceImprovement).isGreaterThan(50.0);
    }

    @Test
    void getHive_ShowsPerformanceImprovement_WithCache() {
        // Given
        String hiveId = UUID.randomUUID().toString();
        Hive testHive = testHives.get(0);
        testHive.setId(hiveId);
        testHive.setIsPublic(true); // Make it accessible

        // Mock with simulated latency
        when(hiveRepository.findByIdAndActive(hiveId))
                .thenAnswer(invocation -> {
                    Thread.sleep(25); // Simulate DB query time
                    return Optional.of(testHive);
                });

        when(hiveMemberRepository.countByHiveId(hiveId)).thenReturn(8L);

        StopWatch stopWatch = new StopWatch();

        // When - First call
        stopWatch.start("First getHive call");
        HiveResponse result1 = hiveService.getHive(hiveId, testUserId);
        stopWatch.stop();

        assertThat(result1).isNotNull();
        assertThat(result1.getId()).isEqualTo(hiveId);

        // Reset mock
        reset(hiveRepository);
        reset(hiveMemberRepository);

        // When - Second call
        stopWatch.start("Second getHive call");
        HiveResponse result2 = hiveService.getHive(hiveId, testUserId);
        stopWatch.stop();

        // Then - Verify cache performance
        long firstCallTime = stopWatch.getTaskTimeMillis("First getHive call");
        long secondCallTime = stopWatch.getTaskTimeMillis("Second getHive call");

        System.out.println("Get Hive Performance Test:");
        System.out.println("First call: " + firstCallTime + "ms");
        System.out.println("Second call: " + secondCallTime + "ms");

        // Verify no repository calls on second invocation
        verify(hiveRepository, never()).findByIdAndActive(anyString());
        verify(hiveMemberRepository, never()).countByHiveId(anyString());

        // Verify results are identical
        assertThat(result2).isNotNull();
        assertThat(result2.getId()).isEqualTo(result1.getId());
        assertThat(result2.getName()).isEqualTo(result1.getName());

        // Performance improvement should be significant
        assertThat(secondCallTime).isLessThan(firstCallTime);
    }

    @Test
    void concurrentAccess_MaintainsCacheConsistency() throws InterruptedException {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Hive> hivePage = new PageImpl<>(testHives, pageable, testHives.size());

        when(hiveRepository.findPublicHives(pageable)).thenReturn(hivePage);
        when(hiveMemberRepository.countByHiveId(anyString())).thenReturn(5L);

        int threadCount = 10;
        List<Thread> threads = new ArrayList<>();
        List<Page<HiveResponse>> results = Collections.synchronizedList(new ArrayList<>());
        List<Long> executionTimes = Collections.synchronizedList(new ArrayList<>());

        // When - Multiple concurrent requests
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                long startTime = System.nanoTime();
                Page<HiveResponse> result = hiveService.listPublicHives(pageable);
                long endTime = System.nanoTime();
                
                results.add(result);
                executionTimes.add((endTime - startTime) / 1_000_000); // Convert to milliseconds
            });
            threads.add(thread);
        }

        // Start all threads
        threads.forEach(Thread::start);

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then - Verify results
        assertThat(results).hasSize(threadCount);
        
        // All results should be identical
        Page<HiveResponse> firstResult = results.get(0);
        for (Page<HiveResponse> result : results) {
            assertThat(result.getContent()).hasSize(firstResult.getContent().size());
            assertThat(result.getContent().get(0).getId()).isEqualTo(firstResult.getContent().get(0).getId());
        }

        // Later requests should be faster due to caching
        long averageEarlyRequests = executionTimes.subList(0, 3).stream()
                .mapToLong(Long::longValue)
                .sum() / 3;
        
        long averageLaterRequests = executionTimes.subList(7, 10).stream()
                .mapToLong(Long::longValue)
                .sum() / 3;

        System.out.println("Concurrent Access Test:");
        System.out.println("Average early requests: " + averageEarlyRequests + "ms");
        System.out.println("Average later requests: " + averageLaterRequests + "ms");

        // Later requests should be faster due to cache
        assertThat(averageLaterRequests).isLessThanOrEqualTo(averageEarlyRequests);
    }

    private List<Hive> createTestHives(int count) {
        List<Hive> hives = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Hive hive = new Hive();
            hive.setId(UUID.randomUUID().toString());
            hive.setName("Test Hive " + i);
            hive.setDescription("Description for hive " + i);
            hive.setIsPublic(true);
            hive.setIsActive(true);
            hive.setMaxMembers(20);
            hives.add(hive);
        }
        return hives;
    }

    private Page<com.focushive.hive.entity.HiveMember> createTestHiveMembers(int count, Pageable pageable) {
        List<com.focushive.hive.entity.HiveMember> members = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            com.focushive.hive.entity.HiveMember member = new com.focushive.hive.entity.HiveMember();
            member.setUserId(testUserId);
            
            Hive hive = testHives.get(i % testHives.size());
            member.setHive(hive);
            
            members.add(member);
        }
        return new PageImpl<>(members, pageable, count);
    }
}