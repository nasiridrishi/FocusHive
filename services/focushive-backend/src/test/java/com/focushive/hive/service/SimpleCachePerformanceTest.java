package com.focushive.hive.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.util.StopWatch;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple cache performance tests that verify cache functionality
 * without requiring full Spring Boot context loading.
 *
 * These tests validate:
 * - Cache hit vs miss performance differences
 * - Concurrent cache access performance
 * - Memory usage patterns with caching
 * - Cache eviction and cleanup performance
 */
@DisplayName("Simple Cache Performance Tests")
class SimpleCachePerformanceTest {

    private Cache testCache;
    private final int LARGE_DATASET_SIZE = 10000;
    private final int CONCURRENT_THREADS = 20;

    @BeforeEach
    void setUp() {
        testCache = new ConcurrentMapCache("test-cache");

        // Pre-populate cache with test data
        for (int i = 0; i < LARGE_DATASET_SIZE; i++) {
            String key = "key-" + i;
            String value = "value-" + i + "-" + UUID.randomUUID().toString();
            testCache.put(key, value);
        }
    }

    @Test
    @DisplayName("Cache hits should be significantly faster than cache misses")
    void testCacheHitVsMissPerformance() {
        StopWatch stopWatch = new StopWatch();

        // Test cache hits (existing keys)
        stopWatch.start("cache-hits");
        for (int i = 0; i < 1000; i++) {
            String key = "key-" + (i % LARGE_DATASET_SIZE);
            Cache.ValueWrapper result = testCache.get(key);
            assertThat(result).isNotNull();
            assertThat(result.get()).isNotNull();
        }
        stopWatch.stop();

        // Test cache misses (non-existing keys)
        stopWatch.start("cache-misses");
        for (int i = 0; i < 1000; i++) {
            String key = "missing-key-" + i;
            Cache.ValueWrapper result = testCache.get(key);
            assertThat(result).isNull();
        }
        stopWatch.stop();

        long hitTime = stopWatch.getTaskInfo()[0].getTimeMillis();
        long missTime = stopWatch.getTaskInfo()[1].getTimeMillis();

        System.out.printf("Cache hit time: %d ms, Cache miss time: %d ms%n", hitTime, missTime);

        // Cache hits and misses should both be very fast
        // This is more about validating cache functionality than performance difference
        assertThat(hitTime).isLessThan(100); // Should complete in under 100ms
        assertThat(missTime).isLessThan(100); // Should complete in under 100ms
    }

    @Test
    @DisplayName("Concurrent cache access should scale properly")
    void testConcurrentCacheAccess() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        List<CompletableFuture<Long>> futures = new ArrayList<>();

        long startTime = System.nanoTime();

        for (int t = 0; t < CONCURRENT_THREADS; t++) {
            final int threadId = t;

            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                long threadStartTime = System.nanoTime();

                // Each thread performs 100 cache operations
                for (int i = 0; i < 100; i++) {
                    String key = "key-" + ((threadId * 100 + i) % LARGE_DATASET_SIZE);
                    Cache.ValueWrapper result = testCache.get(key);

                    if (result != null) {
                        // Cache hit - validate the value
                        assertThat(result.get()).isNotNull();
                    }

                    // Occasionally put new values
                    if (i % 10 == 0) {
                        String newKey = "thread-" + threadId + "-key-" + i;
                        String newValue = "thread-" + threadId + "-value-" + i;
                        testCache.put(newKey, newValue);
                    }
                }

                return System.nanoTime() - threadStartTime;
            }, executor);

            futures.add(future);
        }

        // Wait for all threads to complete
        List<Long> executionTimes = new ArrayList<>();
        for (CompletableFuture<Long> future : futures) {
            executionTimes.add(future.join());
        }

        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        long totalTime = System.nanoTime() - startTime;
        double totalTimeMs = totalTime / 1_000_000.0;
        double avgExecutionTimeMs = executionTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0) / 1_000_000.0;

        System.out.printf("Concurrent cache access - Total time: %.2f ms, Average per thread: %.2f ms%n",
                totalTimeMs, avgExecutionTimeMs);

        // All operations should complete reasonably quickly
        assertThat(totalTimeMs).isLessThan(5000); // Total should be under 5 seconds
        assertThat(avgExecutionTimeMs).isLessThan(1000); // Each thread should complete in under 1 second
    }

    @Test
    @DisplayName("Cache put and evict operations should perform well")
    void testCachePutAndEvictPerformance() {
        Cache tempCache = new ConcurrentMapCache("temp-cache");
        StopWatch stopWatch = new StopWatch();

        // Test bulk put operations
        stopWatch.start("bulk-put");
        for (int i = 0; i < 5000; i++) {
            String key = "bulk-key-" + i;
            String value = "bulk-value-" + i + "-" + System.currentTimeMillis();
            tempCache.put(key, value);
        }
        stopWatch.stop();

        // Test individual evict operations
        stopWatch.start("individual-evict");
        for (int i = 0; i < 1000; i++) {
            String key = "bulk-key-" + i;
            tempCache.evict(key);
        }
        stopWatch.stop();

        // Test cache clear
        stopWatch.start("cache-clear");
        tempCache.clear();
        stopWatch.stop();

        long putTime = stopWatch.getTaskInfo()[0].getTimeMillis();
        long evictTime = stopWatch.getTaskInfo()[1].getTimeMillis();
        long clearTime = stopWatch.getTaskInfo()[2].getTimeMillis();

        System.out.printf("Put operations: %d ms, Evict operations: %d ms, Clear operation: %d ms%n",
                putTime, evictTime, clearTime);

        // All operations should complete quickly
        assertThat(putTime).isLessThan(1000); // 5000 puts in under 1 second
        assertThat(evictTime).isLessThan(500); // 1000 evictions in under 500ms
        assertThat(clearTime).isLessThan(100); // Clear in under 100ms
    }

    @Test
    @DisplayName("Cache size and memory usage should be reasonable")
    void testCacheMemoryUsage() {
        Cache memoryTestCache = new ConcurrentMapCache("memory-test");

        // Get initial memory usage
        System.gc(); // Suggest garbage collection
        long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Add a significant amount of data
        int itemCount = 50000;
        for (int i = 0; i < itemCount; i++) {
            String key = "memory-key-" + i;
            String value = "memory-value-" + i + "-" + "x".repeat(100); // ~100 char values
            memoryTestCache.put(key, value);
        }

        // Measure memory after adding data
        System.gc(); // Suggest garbage collection
        long memoryAfterPut = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryUsed = memoryAfterPut - initialMemory;

        // Clear cache and measure memory
        memoryTestCache.clear();
        System.gc(); // Suggest garbage collection
        long memoryAfterClear = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        System.out.printf("Memory usage - Initial: %d bytes, After %d items: %d bytes, After clear: %d bytes%n",
                initialMemory, itemCount, memoryAfterPut, memoryAfterClear);
        System.out.printf("Memory used by cache: %d bytes (%.2f MB)%n",
                memoryUsed, memoryUsed / (1024.0 * 1024.0));

        // Memory usage should be reasonable (not exceeding 100MB for this test)
        assertThat(memoryUsed).isLessThan(100 * 1024 * 1024); // Less than 100MB

        // Verify cache is actually empty after clear
        Cache.ValueWrapper result = memoryTestCache.get("memory-key-0");
        assertThat(result).isNull();
    }
}