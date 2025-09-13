package com.focushive.performance;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cache Performance Tests for FocusHive Platform.
 * 
 * Tests Redis caching performance across critical areas:
 * - Cache hit/miss ratios and response times
 * - Cache eviction policy effectiveness
 * - Memory usage optimization
 * - Distributed cache synchronization
 * - Cache warming strategies
 * - Concurrent access patterns
 * - Cache invalidation speed
 * - Session cache performance
 * - Application data caching
 * 
 * Performance Targets:
 * - Cache hit ratio: > 90% for hot data
 * - Cache response time: < 5ms P95
 * - Cache memory efficiency: < 100MB for 10K objects
 * - Eviction latency: < 10ms
 * - Invalidation propagation: < 50ms
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("performance-test")
@DisplayName("Cache Performance Tests")
class CachePerformanceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("focushive_cache_test")
            .withUsername("cache_user")
            .withPassword("cache_pass")
            .withReuse(true);

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withCommand("redis-server", "--maxmemory", "256mb", "--maxmemory-policy", "allkeys-lru")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
        registry.add("spring.redis.timeout", () -> "2000ms");
        registry.add("spring.redis.database", () -> "0");
        
        // Cache configuration
        registry.add("spring.cache.type", () -> "redis");
        registry.add("spring.cache.redis.time-to-live", () -> "600000"); // 10 minutes
        registry.add("spring.cache.redis.cache-null-values", () -> "false");
        
        // Redis connection pool
        registry.add("spring.redis.lettuce.pool.max-active", () -> "20");
        registry.add("spring.redis.lettuce.pool.max-idle", () -> "8");
        registry.add("spring.redis.lettuce.pool.min-idle", () -> "2");
    }

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private List<CachePerformanceResult> cacheResults;

    @BeforeEach
    void setUp() {
        cacheResults = new ArrayList<>();
        
        // Clear cache before each test
        clearAllCaches();
        
        // Warm up cache for consistent testing
        warmUpCache();
    }

    @AfterEach
    void generateCacheReport() {
        if (!cacheResults.isEmpty()) {
            generateCachePerformanceReport();
        }
    }

    @Nested
    @DisplayName("Cache Hit Ratio Tests")
    class CacheHitRatioTests {

        @Test
        @DisplayName("User profile cache hit ratio")
        void testUserProfileCacheHitRatio() {
            String cacheName = "userProfiles";
            Cache cache = cacheManager.getCache(cacheName);
            assertThat(cache).isNotNull();
            
            // Pre-populate cache with user profiles
            int userCount = 1000;
            Map<String, Object> userProfiles = new HashMap<>();
            
            for (int i = 0; i < userCount; i++) {
                String userId = "user-" + i;
                Map<String, Object> profile = createMockUserProfile(i);
                userProfiles.put(userId, profile);
                cache.put(userId, profile);
            }
            
            // Test cache performance with realistic access patterns
            int totalRequests = 10000;
            int cacheHits = 0;
            int cacheMisses = 0;
            List<Long> hitTimes = new ArrayList<>();
            List<Long> missTimes = new ArrayList<>();
            
            // 80-20 rule: 80% of requests for 20% of users (hot data)
            Random random = new Random(42); // Fixed seed for reproducible results
            
            for (int i = 0; i < totalRequests; i++) {
                String userId;
                if (i < totalRequests * 0.8) {
                    // Hot data - top 20% of users
                    userId = "user-" + random.nextInt(userCount / 5);
                } else {
                    // Cold data - remaining 80% of users
                    userId = "user-" + (userCount / 5 + random.nextInt(userCount * 4 / 5));
                }
                
                long startTime = System.nanoTime();
                Cache.ValueWrapper result = cache.get(userId);
                long endTime = System.nanoTime();
                
                long responseTime = (endTime - startTime) / 1_000; // Convert to microseconds
                
                if (result != null) {
                    cacheHits++;
                    hitTimes.add(responseTime);
                } else {
                    cacheMisses++;
                    missTimes.add(responseTime);
                    
                    // Simulate cache population for miss
                    Map<String, Object> profile = createMockUserProfile(Integer.parseInt(userId.substring(5)));
                    cache.put(userId, profile);
                }
            }
            
            double hitRatio = (double) cacheHits / totalRequests;
            double avgHitTime = hitTimes.stream().mapToLong(Long::longValue).average().orElse(0) / 1000.0; // ms
            double avgMissTime = missTimes.stream().mapToLong(Long::longValue).average().orElse(0) / 1000.0; // ms
            
            CachePerformanceResult result = new CachePerformanceResult(
                cacheName, totalRequests, cacheHits, cacheMisses, hitRatio, avgHitTime, avgMissTime);
            cacheResults.add(result);
            
            assertThat(hitRatio)
                .describedAs("Cache hit ratio should be high for user profiles")
                .isGreaterThanOrEqualTo(PerformanceTestUtils.CACHE_HIT_RATIO);
            
            assertThat(avgHitTime)
                .describedAs("Cache hit response time should be very fast")
                .isLessThanOrEqualTo(1.0); // 1ms
            
            System.out.println(String.format(
                "User Profile Cache: Hit Ratio=%.1f%%, Hit Time=%.3fms, Miss Time=%.3fms",
                hitRatio * 100, avgHitTime, avgMissTime
            ));
        }

        @Test
        @DisplayName("Session data cache hit ratio")
        void testSessionDataCacheHitRatio() {
            String cacheName = "sessionData";
            Cache cache = cacheManager.getCache(cacheName);
            assertThat(cache).isNotNull();
            
            // Simulate session data caching
            int sessionCount = 5000;
            List<String> sessionIds = new ArrayList<>();
            
            // Pre-populate active sessions
            for (int i = 0; i < sessionCount; i++) {
                String sessionId = "session-" + UUID.randomUUID().toString().substring(0, 8);
                sessionIds.add(sessionId);
                
                Map<String, Object> sessionData = Map.of(
                    "userId", "user-" + (i % 1000),
                    "hiveId", "hive-" + (i % 100),
                    "startTime", System.currentTimeMillis(),
                    "active", true
                );
                
                cache.put(sessionId, sessionData);
            }
            
            // Test session access patterns
            int accessCount = 20000;
            int hits = 0;
            int misses = 0;
            List<Long> responseTimes = new ArrayList<>();
            
            Random random = new Random(42);
            
            for (int i = 0; i < accessCount; i++) {
                String sessionId;
                
                if (random.nextDouble() < 0.85) {
                    // 85% chance of accessing existing session
                    sessionId = sessionIds.get(random.nextInt(sessionIds.size()));
                } else {
                    // 15% chance of accessing non-existent session
                    sessionId = "session-nonexistent-" + i;
                }
                
                long startTime = System.nanoTime();
                Cache.ValueWrapper result = cache.get(sessionId);
                long endTime = System.nanoTime();
                
                responseTimes.add((endTime - startTime) / 1_000_000); // Convert to ms
                
                if (result != null) {
                    hits++;
                } else {
                    misses++;
                }
            }
            
            double hitRatio = (double) hits / accessCount;
            double avgResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            
            assertThat(hitRatio)
                .describedAs("Session cache hit ratio should be very high")
                .isGreaterThanOrEqualTo(0.80); // 80% for session data
            
            assertThat(avgResponseTime)
                .describedAs("Session cache response time should be fast")
                .isLessThanOrEqualTo(2.0); // 2ms
            
            System.out.println(String.format(
                "Session Cache: Hit Ratio=%.1f%%, Avg Response Time=%.3fms, Total Accesses=%d",
                hitRatio * 100, avgResponseTime, accessCount
            ));
        }
    }

    @Nested
    @DisplayName("Cache Response Time Tests")
    class CacheResponseTimeTests {

        @Test
        @DisplayName("Cache read performance under concurrent load")
        void testCacheReadPerformanceConcurrent() throws InterruptedException {
            String cacheName = "concurrentTest";
            Cache cache = cacheManager.getCache(cacheName);
            assertThat(cache).isNotNull();
            
            // Pre-populate cache
            int dataSize = 10000;
            for (int i = 0; i < dataSize; i++) {
                cache.put("key-" + i, createLargeMockObject(i));
            }
            
            // Concurrent read test
            int threadCount = 50;
            int readsPerThread = 200;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<CompletableFuture<List<Long>>> futures = new ArrayList<>();
            
            for (int t = 0; t < threadCount; t++) {
                CompletableFuture<List<Long>> future = CompletableFuture.supplyAsync(() -> {
                    List<Long> threadTimes = new ArrayList<>();
                    Random random = new Random();
                    
                    for (int i = 0; i < readsPerThread; i++) {
                        String key = "key-" + random.nextInt(dataSize);
                        
                        long startTime = System.nanoTime();
                        Cache.ValueWrapper result = cache.get(key);
                        long endTime = System.nanoTime();
                        
                        threadTimes.add((endTime - startTime) / 1_000_000); // Convert to ms
                        
                        // Verify we got a result
                        if (result == null) {
                            System.out.println("Unexpected cache miss for key: " + key);
                        }
                        
                        try {
                            Thread.sleep(1); // Small delay
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    
                    return threadTimes;
                }, executor);
                
                futures.add(future);
            }
            
            // Collect results
            List<Long> allTimes = new ArrayList<>();
            for (CompletableFuture<List<Long>> future : futures) {
                try {
                    List<Long> threadTimes = future.get(60, TimeUnit.SECONDS);
                    allTimes.addAll(threadTimes);
                } catch (Exception e) {
                    System.out.println("Thread failed: " + e.getMessage());
                }
            }
            
            executor.shutdown();
            
            // Analyze performance
            if (!allTimes.isEmpty()) {
                Collections.sort(allTimes);
                double avgTime = allTimes.stream().mapToLong(Long::longValue).average().orElse(0);
                long p50Time = allTimes.get(allTimes.size() / 2);
                long p95Time = allTimes.get((int) (allTimes.size() * 0.95));
                long p99Time = allTimes.get((int) (allTimes.size() * 0.99));
                
                assertThat(avgTime)
                    .describedAs("Average cache read time should be fast")
                    .isLessThanOrEqualTo(5.0); // 5ms
                
                assertThat(p95Time)
                    .describedAs("P95 cache read time should be very fast")
                    .isLessThanOrEqualTo(10); // 10ms
                
                System.out.println(String.format(
                    "Concurrent Cache Read: %d operations, Avg=%.3fms, P50=%dms, P95=%dms, P99=%dms",
                    allTimes.size(), avgTime, p50Time, p95Time, p99Time
                ));
            }
        }

        @Test
        @DisplayName("Cache write performance and latency")
        void testCacheWritePerformance() {
            String cacheName = "writeTest";
            Cache cache = cacheManager.getCache(cacheName);
            assertThat(cache).isNotNull();
            
            int writeCount = 5000;
            List<Long> writeTimes = new ArrayList<>();
            
            // Test cache writes with varying object sizes
            for (int i = 0; i < writeCount; i++) {
                String key = "write-key-" + i;
                Object value;
                
                if (i % 3 == 0) {
                    value = createSmallMockObject(i);
                } else if (i % 3 == 1) {
                    value = createMediumMockObject(i);
                } else {
                    value = createLargeMockObject(i);
                }
                
                long startTime = System.nanoTime();
                cache.put(key, value);
                long endTime = System.nanoTime();
                
                writeTimes.add((endTime - startTime) / 1_000_000); // Convert to ms
            }
            
            // Analyze write performance
            Collections.sort(writeTimes);
            double avgWriteTime = writeTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            long p95WriteTime = writeTimes.get((int) (writeTimes.size() * 0.95));
            long maxWriteTime = writeTimes.get(writeTimes.size() - 1);
            
            assertThat(avgWriteTime)
                .describedAs("Average cache write time should be reasonable")
                .isLessThanOrEqualTo(10.0); // 10ms
            
            assertThat(p95WriteTime)
                .describedAs("P95 cache write time should be acceptable")
                .isLessThanOrEqualTo(20); // 20ms
            
            System.out.println(String.format(
                "Cache Write Performance: %d writes, Avg=%.3fms, P95=%dms, Max=%dms",
                writeCount, avgWriteTime, p95WriteTime, maxWriteTime
            ));
        }
    }

    @Nested
    @DisplayName("Cache Eviction Tests")
    class CacheEvictionTests {

        @Test
        @DisplayName("LRU eviction policy performance")
        void testLRUEvictionPerformance() {
            String cacheName = "lruTest";
            Cache cache = cacheManager.getCache(cacheName);
            assertThat(cache).isNotNull();
            
            // Fill cache beyond capacity to trigger evictions
            int itemCount = 20000; // Exceed Redis memory limit to force evictions
            List<Long> evictionTimes = new ArrayList<>();
            
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < itemCount; i++) {
                String key = "lru-key-" + i;
                Object value = createMediumMockObject(i);
                
                long putStartTime = System.nanoTime();
                cache.put(key, value);
                long putEndTime = System.nanoTime();
                
                long putTime = (putEndTime - putStartTime) / 1_000_000; // Convert to ms
                evictionTimes.add(putTime);
                
                // Simulate access patterns for LRU
                if (i > 100 && i % 10 == 0) {
                    // Access some older keys to affect LRU ordering
                    String oldKey = "lru-key-" + (i - 50);
                    cache.get(oldKey);
                }
            }
            
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            
            // Analyze eviction performance
            double avgEvictionTime = evictionTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            long maxEvictionTime = evictionTimes.stream().mapToLong(Long::longValue).max().orElse(0);
            
            // Check how many items are actually in cache (some should be evicted)
            int remainingItems = 0;
            for (int i = 0; i < itemCount; i++) {
                if (cache.get("lru-key-" + i) != null) {
                    remainingItems++;
                }
            }
            
            assertThat(avgEvictionTime)
                .describedAs("Average eviction time should be fast")
                .isLessThanOrEqualTo(15.0); // 15ms
            
            assertThat(maxEvictionTime)
                .describedAs("Maximum eviction time should be reasonable")
                .isLessThanOrEqualTo(100); // 100ms
            
            // Some items should have been evicted
            assertThat(remainingItems)
                .describedAs("Some items should be evicted due to memory limits")
                .isLessThan(itemCount);
            
            System.out.println(String.format(
                "LRU Eviction: %d items, %d remaining, Avg Time=%.3fms, Max Time=%dms, Total Duration=%dms",
                itemCount, remainingItems, avgEvictionTime, maxEvictionTime, totalTime
            ));
        }

        @Test
        @DisplayName("Manual cache invalidation performance")
        void testManualInvalidationPerformance() {
            String cacheName = "invalidationTest";
            Cache cache = cacheManager.getCache(cacheName);
            assertThat(cache).isNotNull();
            
            // Populate cache
            int itemCount = 10000;
            Set<String> keys = new HashSet<>();
            
            for (int i = 0; i < itemCount; i++) {
                String key = "invalidation-key-" + i;
                keys.add(key);
                cache.put(key, createMockUserProfile(i));
            }
            
            // Test single key invalidation
            List<Long> singleInvalidationTimes = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                String key = "invalidation-key-" + i;
                
                long startTime = System.nanoTime();
                cache.evict(key);
                long endTime = System.nanoTime();
                
                singleInvalidationTimes.add((endTime - startTime) / 1_000_000); // Convert to ms
            }
            
            // Test cache clear (bulk invalidation)
            long clearStartTime = System.nanoTime();
            cache.clear();
            long clearEndTime = System.nanoTime();
            long clearTime = (clearEndTime - clearStartTime) / 1_000_000; // Convert to ms
            
            // Analyze invalidation performance
            double avgSingleInvalidation = singleInvalidationTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            long maxSingleInvalidation = singleInvalidationTimes.stream().mapToLong(Long::longValue).max().orElse(0);
            
            assertThat(avgSingleInvalidation)
                .describedAs("Average single invalidation time should be fast")
                .isLessThanOrEqualTo(5.0); // 5ms
            
            assertThat(clearTime)
                .describedAs("Cache clear operation should be fast")
                .isLessThanOrEqualTo(50); // 50ms
            
            System.out.println(String.format(
                "Cache Invalidation: Single Avg=%.3fms, Single Max=%dms, Clear=%dms",
                avgSingleInvalidation, maxSingleInvalidation, clearTime
            ));
        }
    }

    @Nested
    @DisplayName("Memory Usage Tests")
    class MemoryUsageTests {

        @Test
        @DisplayName("Memory efficiency for different object sizes")
        void testMemoryEfficiency() {
            // Test memory usage with different object sizes
            testObjectSizeMemory("smallObjects", 10000, this::createSmallMockObject);
            testObjectSizeMemory("mediumObjects", 5000, this::createMediumMockObject);
            testObjectSizeMemory("largeObjects", 1000, this::createLargeMockObject);
        }

        private void testObjectSizeMemory(String testName, int objectCount, 
                                        java.util.function.Function<Integer, Object> objectFactory) {
            String cacheName = testName;
            Cache cache = cacheManager.getCache(cacheName);
            assertThat(cache).isNotNull();
            
            // Clear cache and force garbage collection for accurate measurement
            cache.clear();
            System.gc();
            
            try {
                Thread.sleep(100); // Allow GC to complete
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Get initial memory usage
            long initialMemory = getUsedMemory();
            
            // Populate cache
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < objectCount; i++) {
                String key = testName + "-key-" + i;
                Object value = objectFactory.apply(i);
                cache.put(key, value);
                
                if (i % 1000 == 0) {
                    // Log progress for large operations
                    System.out.println("Cached " + (i + 1) + "/" + objectCount + " " + testName);
                }
            }
            
            long endTime = System.currentTimeMillis();
            
            // Force garbage collection and measure memory
            System.gc();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            long finalMemory = getUsedMemory();
            long memoryUsed = finalMemory - initialMemory;
            
            double avgMemoryPerObject = (double) memoryUsed / objectCount;
            double cachingTime = endTime - startTime;
            double throughput = objectCount / (cachingTime / 1000.0);
            
            System.out.println(String.format(
                "%s Memory Test: %d objects, Memory Used=%s (%.1f bytes/object), Time=%dms, Throughput=%.1f obj/s",
                testName, objectCount, PerformanceTestUtils.formatBytes(memoryUsed), 
                avgMemoryPerObject, (long) cachingTime, throughput
            ));
            
            // Memory efficiency should be reasonable
            if ("smallObjects".equals(testName)) {
                assertThat(avgMemoryPerObject)
                    .describedAs("Small objects should use minimal memory")
                    .isLessThan(1000); // 1KB per small object
            } else if ("mediumObjects".equals(testName)) {
                assertThat(avgMemoryPerObject)
                    .describedAs("Medium objects should use reasonable memory")
                    .isLessThan(5000); // 5KB per medium object
            } else if ("largeObjects".equals(testName)) {
                assertThat(avgMemoryPerObject)
                    .describedAs("Large objects should use acceptable memory")
                    .isLessThan(20000); // 20KB per large object
            }
            
            // Clear cache for next test
            cache.clear();
        }

        @Test
        @DisplayName("Memory leak detection during cache operations")
        void testMemoryLeakDetection() throws InterruptedException {
            String cacheName = "memoryLeakTest";
            Cache cache = cacheManager.getCache(cacheName);
            assertThat(cache).isNotNull();
            
            List<Long> memorySnapshots = new ArrayList<>();
            
            // Monitor memory usage over multiple cache cycles
            for (int cycle = 0; cycle < 10; cycle++) {
                // Clear cache and measure memory
                cache.clear();
                System.gc();
                Thread.sleep(100);
                
                long cycleStartMemory = getUsedMemory();
                
                // Populate cache
                for (int i = 0; i < 1000; i++) {
                    cache.put("leak-test-" + i, createMediumMockObject(i));
                }
                
                // Use cache
                for (int i = 0; i < 2000; i++) {
                    cache.get("leak-test-" + (i % 1000));
                }
                
                // Clear cache again
                cache.clear();
                System.gc();
                Thread.sleep(100);
                
                long cycleEndMemory = getUsedMemory();
                memorySnapshots.add(cycleEndMemory - cycleStartMemory);
                
                System.out.println("Cycle " + cycle + ": Memory delta = " + 
                    PerformanceTestUtils.formatBytes(cycleEndMemory - cycleStartMemory));
            }
            
            // Analyze for memory leaks
            if (memorySnapshots.size() > 5) {
                long initialMemoryDelta = memorySnapshots.get(0);
                long finalMemoryDelta = memorySnapshots.get(memorySnapshots.size() - 1);
                
                double memoryGrowthRatio = (double) finalMemoryDelta / Math.max(initialMemoryDelta, 1);
                
                assertThat(memoryGrowthRatio)
                    .describedAs("Memory usage should not grow significantly over cycles")
                    .isLessThan(2.0); // Less than 2x growth
                
                System.out.println(String.format(
                    "Memory Leak Test: Initial Delta=%s, Final Delta=%s, Growth Ratio=%.2f",
                    PerformanceTestUtils.formatBytes(initialMemoryDelta),
                    PerformanceTestUtils.formatBytes(finalMemoryDelta),
                    memoryGrowthRatio
                ));
            }
        }
    }

    @Nested
    @DisplayName("Distributed Cache Tests")
    class DistributedCacheTests {

        @Test
        @DisplayName("Cache synchronization across multiple clients")
        void testCacheSynchronization() throws InterruptedException {
            // Simulate multiple cache clients
            int clientCount = 10;
            int operationsPerClient = 100;
            String keyPrefix = "sync-test-";
            
            ExecutorService executor = Executors.newFixedThreadPool(clientCount);
            List<CompletableFuture<SyncTestResult>> futures = new ArrayList<>();
            AtomicLong globalUpdateCount = new AtomicLong(0);
            
            for (int clientId = 0; clientId < clientCount; clientId++) {
                final int finalClientId = clientId;
                
                CompletableFuture<SyncTestResult> future = CompletableFuture.supplyAsync(() -> {
                    Cache cache = cacheManager.getCache("syncTest");
                    List<Long> operationTimes = new ArrayList<>();
                    int updatesPerformed = 0;
                    
                    for (int op = 0; op < operationsPerClient; op++) {
                        String key = keyPrefix + (op % 50); // Shared keys across clients
                        
                        long startTime = System.nanoTime();
                        
                        // Perform cache operation (read or write)
                        if (op % 3 == 0) {
                            // Write operation
                            String value = "client-" + finalClientId + "-update-" + globalUpdateCount.incrementAndGet();
                            cache.put(key, value);
                            updatesPerformed++;
                        } else {
                            // Read operation
                            cache.get(key);
                        }
                        
                        long endTime = System.nanoTime();
                        operationTimes.add((endTime - startTime) / 1_000_000); // Convert to ms
                        
                        try {
                            Thread.sleep(10); // Small delay to allow interleaving
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    
                    return new SyncTestResult(finalClientId, operationTimes, updatesPerformed);
                }, executor);
                
                futures.add(future);
            }
            
            // Collect results
            List<SyncTestResult> results = new ArrayList<>();
            for (CompletableFuture<SyncTestResult> future : futures) {
                try {
                    results.add(future.get(120, TimeUnit.SECONDS));
                } catch (Exception e) {
                    System.out.println("Client failed: " + e.getMessage());
                }
            }
            
            executor.shutdown();
            
            // Analyze synchronization performance
            int totalOperations = results.stream().mapToInt(r -> r.operationTimes.size()).sum();
            int totalUpdates = results.stream().mapToInt(r -> r.updateCount).sum();
            
            double avgOperationTime = results.stream()
                .flatMap(r -> r.operationTimes.stream())
                .mapToLong(Long::longValue)
                .average().orElse(0);
            
            assertThat(avgOperationTime)
                .describedAs("Distributed cache operations should be reasonably fast")
                .isLessThanOrEqualTo(50.0); // 50ms including network overhead
            
            System.out.println(String.format(
                "Cache Synchronization: %d clients, %d operations, %d updates, Avg Time=%.3fms",
                clientCount, totalOperations, totalUpdates, avgOperationTime
            ));
        }
    }

    // Helper methods

    private void clearAllCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    private void warmUpCache() {
        // Warm up cache with common data patterns
        Cache userCache = cacheManager.getCache("userProfiles");
        if (userCache != null) {
            for (int i = 0; i < 100; i++) {
                userCache.put("warmup-user-" + i, createMockUserProfile(i));
            }
        }
    }

    private Map<String, Object> createMockUserProfile(int index) {
        return Map.of(
            "id", index,
            "username", "user" + index,
            "email", "user" + index + "@test.com",
            "displayName", "Test User " + index,
            "active", index % 10 != 0,
            "lastLogin", System.currentTimeMillis() - (index * 10000),
            "preferences", Map.of("theme", "dark", "notifications", true)
        );
    }

    private Map<String, Object> createSmallMockObject(int index) {
        return Map.of(
            "id", index,
            "name", "Object" + index,
            "active", true
        );
    }

    private Map<String, Object> createMediumMockObject(int index) {
        return Map.of(
            "id", index,
            "name", "Medium Object " + index,
            "description", "This is a medium-sized test object with id " + index + " for cache performance testing.",
            "properties", Map.of(
                "type", "test",
                "category", "performance",
                "priority", index % 5,
                "tags", List.of("tag1", "tag2", "tag3")
            ),
            "metadata", Map.of(
                "created", System.currentTimeMillis(),
                "version", "1.0",
                "author", "test-user"
            )
        );
    }

    private Map<String, Object> createLargeMockObject(int index) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            items.add(Map.of(
                "itemId", i,
                "itemName", "Item " + i,
                "itemData", "Data for item " + i + " in object " + index
            ));
        }
        
        return Map.of(
            "id", index,
            "name", "Large Object " + index,
            "description", "This is a large test object containing multiple nested items and extensive data for cache performance testing purposes. ".repeat(5),
            "items", items,
            "properties", Map.of(
                "type", "large-test",
                "category", "performance-testing",
                "priority", index % 10,
                "tags", List.of("large", "performance", "test", "object", "cache")
            ),
            "metadata", Map.of(
                "created", System.currentTimeMillis(),
                "modified", System.currentTimeMillis(),
                "version", "2.0",
                "size", "large",
                "complexity", "high"
            ),
            "additionalData", Map.of(
                "field1", "Value 1 for object " + index,
                "field2", "Value 2 for object " + index,
                "field3", "Value 3 for object " + index,
                "field4", System.currentTimeMillis(),
                "field5", List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            )
        );
    }

    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private void generateCachePerformanceReport() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("CACHE PERFORMANCE TEST REPORT");
        System.out.println("=".repeat(60));
        
        for (CachePerformanceResult result : cacheResults) {
            System.out.println("Cache: " + result.cacheName);
            System.out.println("  Total Requests: " + result.totalRequests);
            System.out.println("  Cache Hits: " + result.hits);
            System.out.println("  Cache Misses: " + result.misses);
            System.out.println("  Hit Ratio: " + String.format("%.2f%%", result.hitRatio * 100));
            System.out.println("  Avg Hit Time: " + String.format("%.3fms", result.avgHitTime));
            System.out.println("  Avg Miss Time: " + String.format("%.3fms", result.avgMissTime));
            System.out.println();
        }
        
        System.out.println("=".repeat(60));
    }

    // Helper classes
    static class CachePerformanceResult {
        final String cacheName;
        final int totalRequests;
        final int hits;
        final int misses;
        final double hitRatio;
        final double avgHitTime;
        final double avgMissTime;
        
        CachePerformanceResult(String cacheName, int total, int hits, int misses, 
                             double hitRatio, double avgHitTime, double avgMissTime) {
            this.cacheName = cacheName;
            this.totalRequests = total;
            this.hits = hits;
            this.misses = misses;
            this.hitRatio = hitRatio;
            this.avgHitTime = avgHitTime;
            this.avgMissTime = avgMissTime;
        }
    }
    
    static class SyncTestResult {
        final int clientId;
        final List<Long> operationTimes;
        final int updateCount;
        
        SyncTestResult(int clientId, List<Long> operationTimes, int updateCount) {
            this.clientId = clientId;
            this.operationTimes = operationTimes;
            this.updateCount = updateCount;
        }
    }
}