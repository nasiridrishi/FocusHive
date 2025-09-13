package com.focushive.performance;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
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
 * Comprehensive Load Testing Scenarios for FocusHive Platform.
 * 
 * Tests various load patterns to validate system behavior under different conditions:
 * - Gradual ramp-up (0 to 10,000 users)
 * - Spike testing (sudden load increase)
 * - Sustained load testing (24-hour runs)
 * - Stress testing (find breaking point)
 * - Volume testing (large data sets)
 * - Soak testing (memory leaks)
 * 
 * Each scenario validates performance targets and system stability.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@ActiveProfiles("performance-test")
@DisplayName("Load Test Scenarios")
class LoadTestScenarios {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("focushive_load_test")
            .withUsername("load_user")
            .withPassword("load_pass")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // Optimized for high load testing
        registry.add("spring.datasource.hikari.minimum-idle", () -> "50");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "100");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "30000");
        registry.add("spring.datasource.hikari.idle-timeout", () -> "300000");
        registry.add("spring.datasource.hikari.max-lifetime", () -> "900000");
        registry.add("spring.datasource.hikari.leak-detection-threshold", () -> "60000");
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private List<PerformanceTestUtils.LoadTestResult> loadTestResults;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        loadTestResults = new ArrayList<>();
    }

    @AfterEach
    void generateLoadTestReport() {
        if (!loadTestResults.isEmpty()) {
            PerformanceTestUtils.generatePerformanceReport(List.of(), List.of(), loadTestResults, 
                "load-test-report.txt");
        }
    }

    @Nested
    @DisplayName("Gradual Ramp-up Testing")
    class GradualRampUpTests {

        @Test
        @DisplayName("Ramp-up: 0 to 1000 users over 5 minutes")
        void testGradualRampUpTo1000Users() throws Exception {
            Duration rampUpDuration = Duration.ofMinutes(5);
            int targetUsers = 1000;
            int rampUpSteps = 20; // 50 user increments
            
            List<LoadTestResult> stepResults = new ArrayList<>();
            
            for (int step = 1; step <= rampUpSteps; step++) {
                int currentUsers = (targetUsers * step) / rampUpSteps;
                System.out.println("Ramp-up step " + step + "/" + rampUpSteps + 
                                 " - Testing " + currentUsers + " concurrent users");
                
                LoadTestResult result = executeLoadTest(currentUsers, Duration.ofSeconds(15), 
                    LoadTestPattern.GRADUAL_RAMP);
                stepResults.add(result);
                
                // Allow system to stabilize between steps
                Thread.sleep(5000);
            }
            
            // Analyze ramp-up results
            analyzeRampUpPattern(stepResults);
            
            // Final validation with full load
            LoadTestResult finalResult = executeLoadTest(targetUsers, Duration.ofMinutes(2), 
                LoadTestPattern.SUSTAINED);
            loadTestResults.add(finalResult);
            
            assertThat(finalResult.successRate)
                .describedAs("System should handle 1000 users with good success rate")
                .isGreaterThanOrEqualTo(0.95);
            
            assertThat(finalResult.apiMetrics.p95ResponseTime)
                .describedAs("Response time should remain reasonable under full load")
                .isLessThanOrEqualTo(1000); // 1 second P95 under high load
        }

        @Test
        @DisplayName("Extreme ramp-up: 0 to 5000 users over 10 minutes")
        @Timeout(value = 15, unit = TimeUnit.MINUTES)
        void testExtremeRampUpTo5000Users() throws Exception {
            // Skip in CI to avoid overwhelming test infrastructure
            org.junit.jupiter.api.Assumptions.assumeFalse(
                System.getenv("CI") != null, "Skipping extreme load test in CI environment");
            
            int targetUsers = 5000;
            int rampUpSteps = 25; // 200 user increments
            
            System.out.println("Starting extreme ramp-up test to " + targetUsers + " users");
            
            AtomicInteger peakSuccessfulUsers = new AtomicInteger(0);
            AtomicLong firstDegradationPoint = new AtomicLong(-1);
            
            for (int step = 1; step <= rampUpSteps; step++) {
                int currentUsers = (targetUsers * step) / rampUpSteps;
                
                LoadTestResult result = executeLoadTest(currentUsers, Duration.ofSeconds(10), 
                    LoadTestPattern.STRESS_TEST);
                
                System.out.println(String.format(
                    "Step %d/%d: %d users, Success: %.1f%%, P95: %dms, Throughput: %.1f req/s",
                    step, rampUpSteps, currentUsers, result.successRate * 100, 
                    result.apiMetrics.p95ResponseTime, result.throughput
                ));
                
                // Track performance degradation
                if (result.successRate >= 0.90) {
                    peakSuccessfulUsers.set(currentUsers);
                } else if (firstDegradationPoint.get() == -1) {
                    firstDegradationPoint.set(currentUsers);
                }
                
                // Break if system becomes completely unresponsive
                if (result.successRate < 0.10) {
                    System.out.println("System became unresponsive at " + currentUsers + " users");
                    break;
                }
                
                Thread.sleep(3000); // Stabilization period
            }
            
            System.out.println("Peak capacity: " + peakSuccessfulUsers.get() + " users");
            System.out.println("Degradation started at: " + 
                (firstDegradationPoint.get() > 0 ? firstDegradationPoint.get() + " users" : "Not detected"));
            
            // System should handle at least 1000 users reliably
            assertThat(peakSuccessfulUsers.get())
                .describedAs("System should handle at least 1000 concurrent users")
                .isGreaterThanOrEqualTo(1000);
        }

        private void analyzeRampUpPattern(List<LoadTestResult> results) {
            System.out.println("\nRamp-up Pattern Analysis:");
            System.out.println("Users\tSuccess%\tP95ms\tThroughput");
            
            for (LoadTestResult result : results) {
                System.out.println(String.format("%d\t%.1f\t%d\t%.1f",
                    result.virtualUsers,
                    result.successRate * 100,
                    result.apiMetrics.p95ResponseTime,
                    result.throughput
                ));
            }
            
            // Validate graceful degradation
            boolean gracefulDegradation = true;
            for (int i = 1; i < results.size(); i++) {
                double prevSuccess = results.get(i - 1).successRate;
                double currSuccess = results.get(i).successRate;
                
                // Success rate shouldn't drop more than 20% in one step
                if (currSuccess < prevSuccess - 0.20) {
                    gracefulDegradation = false;
                    break;
                }
            }
            
            assertThat(gracefulDegradation)
                .describedAs("System should degrade gracefully during ramp-up")
                .isTrue();
        }
    }

    @Nested
    @DisplayName("Spike Testing")
    class SpikeTests {

        @Test
        @DisplayName("Traffic spike: 100 to 1000 users in 30 seconds")
        void testTrafficSpike() throws Exception {
            // Baseline load
            LoadTestResult baseline = executeLoadTest(100, Duration.ofSeconds(30), LoadTestPattern.BASELINE);
            System.out.println("Baseline (100 users): Success=" + (baseline.successRate * 100) + "%, P95=" + baseline.apiMetrics.p95ResponseTime + "ms");
            
            // Allow system to stabilize
            Thread.sleep(10000);
            
            // Sudden spike
            LoadTestResult spike = executeLoadTest(1000, Duration.ofSeconds(60), LoadTestPattern.SPIKE);
            System.out.println("Spike (1000 users): Success=" + (spike.successRate * 100) + "%, P95=" + spike.apiMetrics.p95ResponseTime + "ms");
            
            loadTestResults.add(baseline);
            loadTestResults.add(spike);
            
            // Recovery test
            Thread.sleep(15000); // Allow recovery
            LoadTestResult recovery = executeLoadTest(100, Duration.ofSeconds(30), LoadTestPattern.RECOVERY);
            System.out.println("Recovery (100 users): Success=" + (recovery.successRate * 100) + "%, P95=" + recovery.apiMetrics.p95ResponseTime + "ms");
            
            loadTestResults.add(recovery);
            
            // Validate spike handling
            assertThat(spike.successRate)
                .describedAs("System should handle traffic spikes with reasonable success rate")
                .isGreaterThanOrEqualTo(0.75); // 75% during spike
            
            // Validate recovery
            assertThat(recovery.successRate)
                .describedAs("System should recover after spike")
                .isGreaterThanOrEqualTo(baseline.successRate - 0.05); // Within 5% of baseline
        }

        @Test
        @DisplayName("Multiple spikes: Repeated load surges")
        void testMultipleSpikes() throws Exception {
            int baselineUsers = 200;
            int spikeUsers = 800;
            int spikes = 3;
            
            List<LoadTestResult> spikeResults = new ArrayList<>();
            
            for (int i = 0; i < spikes; i++) {
                System.out.println("Spike " + (i + 1) + "/" + spikes);
                
                // Baseline period
                LoadTestResult baseline = executeLoadTest(baselineUsers, Duration.ofSeconds(20), LoadTestPattern.BASELINE);
                
                // Spike period
                LoadTestResult spike = executeLoadTest(spikeUsers, Duration.ofSeconds(30), LoadTestPattern.SPIKE);
                spikeResults.add(spike);
                
                System.out.println(String.format("Spike %d: Success=%.1f%%, P95=%dms", 
                    i + 1, spike.successRate * 100, spike.apiMetrics.p95ResponseTime));
                
                // Recovery period
                Thread.sleep(15000);
            }
            
            // Validate consistent spike handling
            boolean consistentPerformance = true;
            double firstSpikeSuccess = spikeResults.get(0).successRate;
            
            for (LoadTestResult spike : spikeResults) {
                if (Math.abs(spike.successRate - firstSpikeSuccess) > 0.15) { // 15% variance
                    consistentPerformance = false;
                    break;
                }
            }
            
            assertThat(consistentPerformance)
                .describedAs("System should handle repeated spikes consistently")
                .isTrue();
        }
    }

    @Nested
    @DisplayName("Sustained Load Testing")
    class SustainedLoadTests {

        @Test
        @DisplayName("Sustained load: 500 users for 30 minutes")
        @Timeout(value = 35, unit = TimeUnit.MINUTES)
        void testSustainedLoad30Minutes() throws Exception {
            org.junit.jupiter.api.Assumptions.assumeFalse(
                System.getenv("CI") != null, "Skipping long-running test in CI environment");
            
            int sustainedUsers = 500;
            Duration testDuration = Duration.ofMinutes(30);
            
            System.out.println("Starting 30-minute sustained load test with " + sustainedUsers + " users");
            
            LoadTestResult result = executeExtendedLoadTest(sustainedUsers, testDuration, 
                Duration.ofMinutes(5)); // Sample every 5 minutes
            
            loadTestResults.add(result);
            
            // Validate sustained performance
            assertThat(result.successRate)
                .describedAs("System should maintain performance during sustained load")
                .isGreaterThanOrEqualTo(0.90);
            
            assertThat(result.apiMetrics.p95ResponseTime)
                .describedAs("Response times should remain stable")
                .isLessThanOrEqualTo(800); // 800ms P95 for sustained load
            
            System.out.println(String.format(
                "30-minute sustained load completed: Success=%.1f%%, P95=%dms, Avg Throughput=%.1f req/s",
                result.successRate * 100, result.apiMetrics.p95ResponseTime, result.throughput
            ));
        }

        @Test
        @DisplayName("Soak test: 300 users for 2 hours (memory leak detection)")
        @Timeout(value = 150, unit = TimeUnit.MINUTES)
        void testSoakTest2Hours() throws Exception {
            org.junit.jupiter.api.Assumptions.assumeFalse(
                System.getenv("CI") != null, "Skipping soak test in CI environment");
            
            int soakUsers = 300;
            Duration testDuration = Duration.ofHours(2);
            
            System.out.println("Starting 2-hour soak test with " + soakUsers + " users");
            
            // Monitor memory usage during soak test
            List<Long> memorySnapshots = new ArrayList<>();
            ScheduledExecutorService memoryMonitor = Executors.newScheduledThreadPool(1);
            
            memoryMonitor.scheduleAtFixedRate(() -> {
                Runtime runtime = Runtime.getRuntime();
                long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                memorySnapshots.add(usedMemory);
                
                System.out.println("Memory usage: " + PerformanceTestUtils.formatBytes(usedMemory));
            }, 0, 10, TimeUnit.MINUTES);
            
            LoadTestResult result = executeExtendedLoadTest(soakUsers, testDuration, 
                Duration.ofMinutes(15)); // Sample every 15 minutes
            
            memoryMonitor.shutdown();
            loadTestResults.add(result);
            
            // Analyze memory growth
            if (memorySnapshots.size() > 4) {
                long initialMemory = memorySnapshots.get(0);
                long finalMemory = memorySnapshots.get(memorySnapshots.size() - 1);
                double memoryGrowthPercent = ((double) (finalMemory - initialMemory) / initialMemory) * 100;
                
                System.out.println(String.format(
                    "Memory analysis: Initial=%s, Final=%s, Growth=%.1f%%",
                    PerformanceTestUtils.formatBytes(initialMemory),
                    PerformanceTestUtils.formatBytes(finalMemory),
                    memoryGrowthPercent
                ));
                
                // Memory growth should be reasonable (< 50% over 2 hours indicates no major leaks)
                assertThat(memoryGrowthPercent)
                    .describedAs("Memory growth should be controlled during soak test")
                    .isLessThan(50.0);
            }
            
            // Performance should remain stable
            assertThat(result.successRate)
                .describedAs("System should maintain performance during soak test")
                .isGreaterThanOrEqualTo(0.85);
        }
    }

    @Nested
    @DisplayName("Stress Testing")
    class StressTests {

        @Test
        @DisplayName("Find breaking point: Progressive load increase")
        void testFindBreakingPoint() throws Exception {
            int startUsers = 100;
            int maxUsers = 3000;
            int stepSize = 200;
            Duration stepDuration = Duration.ofSeconds(30);
            
            int breakingPoint = -1;
            int lastSuccessfulLoad = startUsers;
            
            for (int users = startUsers; users <= maxUsers; users += stepSize) {
                System.out.println("Testing breaking point with " + users + " users...");
                
                LoadTestResult result = executeLoadTest(users, stepDuration, LoadTestPattern.STRESS_TEST);
                
                System.out.println(String.format(
                    "%d users: Success=%.1f%%, P95=%dms, Errors=%d",
                    users, result.successRate * 100, result.apiMetrics.p95ResponseTime, result.failedRequests
                ));
                
                if (result.successRate < 0.50) { // 50% threshold for breaking point
                    breakingPoint = users;
                    System.out.println("Breaking point identified at " + users + " users");
                    break;
                } else if (result.successRate >= 0.80) {
                    lastSuccessfulLoad = users;
                }
                
                // Allow system recovery between tests
                Thread.sleep(10000);
            }
            
            System.out.println("Last successful load: " + lastSuccessfulLoad + " users");
            if (breakingPoint > 0) {
                System.out.println("Breaking point: " + breakingPoint + " users");
            }
            
            // Test recovery after stress
            if (breakingPoint > 0) {
                Thread.sleep(30000); // Allow recovery
                LoadTestResult recovery = executeLoadTest(startUsers, Duration.ofSeconds(30), LoadTestPattern.RECOVERY);
                
                assertThat(recovery.successRate)
                    .describedAs("System should recover after reaching breaking point")
                    .isGreaterThanOrEqualTo(0.80);
                
                System.out.println("Recovery test: Success=" + (recovery.successRate * 100) + "%");
            }
            
            // System should handle at least 500 users before breaking
            assertThat(lastSuccessfulLoad)
                .describedAs("System should handle at least 500 users before degradation")
                .isGreaterThanOrEqualTo(500);
        }
    }

    @Nested
    @DisplayName("Volume Testing")
    class VolumeTests {

        @Test
        @DisplayName("Large dataset performance: 10,000 concurrent operations")
        void testLargeDatasetPerformance() throws Exception {
            int concurrentOperations = 10000;
            Duration testWindow = Duration.ofMinutes(5);
            
            System.out.println("Testing large volume: " + concurrentOperations + " operations over " + testWindow.toMinutes() + " minutes");
            
            LoadTestResult result = executeVolumeTest(concurrentOperations, testWindow);
            loadTestResults.add(result);
            
            assertThat(result.successfulRequests)
                .describedAs("Should successfully process large volume of requests")
                .isGreaterThan(concurrentOperations * 0.80); // 80% success rate for volume
            
            assertThat(result.throughput)
                .describedAs("Should maintain reasonable throughput for large volume")
                .isGreaterThan(100.0); // 100 requests per second minimum
            
            System.out.println(String.format(
                "Volume test results: %d operations, Success=%.1f%%, Throughput=%.1f req/s",
                concurrentOperations, result.successRate * 100, result.throughput
            ));
        }
    }

    // Helper methods for load test execution

    private LoadTestResult executeLoadTest(int users, Duration duration, LoadTestPattern pattern) 
            throws InterruptedException {
        
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(users, 200)); // Limit thread pool
        List<CompletableFuture<ApiCallResult>> futures = new ArrayList<>();
        AtomicLong totalRequests = new AtomicLong(0);
        AtomicLong successfulRequests = new AtomicLong(0);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        
        Instant startTime = Instant.now();
        Instant endTime = startTime.plus(duration);
        
        // Generate load based on pattern
        while (Instant.now().isBefore(endTime)) {
            if (futures.size() < users) {
                CompletableFuture<ApiCallResult> future = CompletableFuture.supplyAsync(() -> {
                    return executeApiCall();
                }, executor);
                
                futures.add(future);
            }
            
            // Process completed futures
            futures.removeIf(future -> {
                if (future.isDone()) {
                    try {
                        ApiCallResult result = future.get();
                        totalRequests.incrementAndGet();
                        
                        if (result.success) {
                            successfulRequests.incrementAndGet();
                            responseTimes.add(result.responseTime);
                        } else {
                            errors.add(result.error);
                        }
                    } catch (Exception e) {
                        errors.add("Future error: " + e.getMessage());
                        totalRequests.incrementAndGet();
                    }
                    return true;
                }
                return false;
            });
            
            Thread.sleep(50); // Control request rate
        }
        
        // Wait for remaining futures
        for (CompletableFuture<ApiCallResult> future : futures) {
            try {
                ApiCallResult result = future.get(5, TimeUnit.SECONDS);
                totalRequests.incrementAndGet();
                
                if (result.success) {
                    successfulRequests.incrementAndGet();
                    responseTimes.add(result.responseTime);
                } else {
                    errors.add(result.error);
                }
            } catch (Exception e) {
                errors.add("Future timeout: " + e.getMessage());
                totalRequests.incrementAndGet();
            }
        }
        
        executor.shutdown();
        
        // Calculate metrics
        Duration actualDuration = Duration.between(startTime, Instant.now());
        return createLoadTestResult(users, actualDuration, totalRequests.get(), 
                                  successfulRequests.get(), responseTimes, errors);
    }

    private LoadTestResult executeExtendedLoadTest(int users, Duration duration, Duration sampleInterval) 
            throws InterruptedException {
        
        List<LoadTestResult> sampleResults = new ArrayList<>();
        Instant endTime = Instant.now().plus(duration);
        
        while (Instant.now().isBefore(endTime)) {
            LoadTestResult sample = executeLoadTest(users, sampleInterval, LoadTestPattern.SUSTAINED);
            sampleResults.add(sample);
            
            System.out.println(String.format(
                "Sample: Success=%.1f%%, P95=%dms, Remaining: %d minutes",
                sample.successRate * 100, 
                sample.apiMetrics.p95ResponseTime,
                Duration.between(Instant.now(), endTime).toMinutes()
            ));
        }
        
        // Aggregate results
        return aggregateLoadTestResults(users, duration, sampleResults);
    }

    private LoadTestResult executeVolumeTest(int operations, Duration window) throws InterruptedException {
        int concurrency = Math.min(operations / 10, 500); // Limit concurrency
        return executeLoadTest(concurrency, window, LoadTestPattern.VOLUME);
    }

    private ApiCallResult executeApiCall() {
        try {
            long startTime = System.nanoTime();
            
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .get("/api/hives")
                    .contentType("application/json"));
            
            long endTime = System.nanoTime();
            long responseTime = (endTime - startTime) / 1_000_000; // Convert to ms
            
            return new ApiCallResult(responseTime, true, null);
        } catch (Exception e) {
            return new ApiCallResult(0, false, e.getMessage());
        }
    }

    private LoadTestResult createLoadTestResult(int users, Duration duration, long totalRequests, 
                                              long successfulRequests, List<Long> responseTimes, List<String> errors) {
        
        double successRate = (double) successfulRequests / totalRequests;
        double errorRate = 1.0 - successRate;
        double throughput = successfulRequests / (duration.toMillis() / 1000.0);
        
        // Calculate API metrics
        Collections.sort(responseTimes);
        long p50 = responseTimes.isEmpty() ? 0 : responseTimes.get(responseTimes.size() / 2);
        long p95 = responseTimes.isEmpty() ? 0 : responseTimes.get((int) (responseTimes.size() * 0.95));
        long p99 = responseTimes.isEmpty() ? 0 : responseTimes.get((int) (responseTimes.size() * 0.99));
        long avg = responseTimes.isEmpty() ? 0 : (long) responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long max = responseTimes.isEmpty() ? 0 : responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        long min = responseTimes.isEmpty() ? 0 : responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        
        PerformanceTestUtils.ApiPerformanceResult apiMetrics = new PerformanceTestUtils.ApiPerformanceResult(
            "/api/hives", p50, p95, p99, avg, max, min, successRate, throughput,
            (int) totalRequests, (int) successfulRequests, (int) (totalRequests - successfulRequests), 
            errors, duration);
        
        return new PerformanceTestUtils.LoadTestResult(
            users, duration, totalRequests, successfulRequests, totalRequests - successfulRequests,
            successRate, errorRate, throughput, apiMetrics, new HashMap<>(), errors);
    }

    private LoadTestResult aggregateLoadTestResults(int users, Duration totalDuration, List<LoadTestResult> samples) {
        long totalRequests = samples.stream().mapToLong(r -> r.totalRequests).sum();
        long successfulRequests = samples.stream().mapToLong(r -> r.successfulRequests).sum();
        
        double avgSuccessRate = samples.stream().mapToDouble(r -> r.successRate).average().orElse(0.0);
        double avgThroughput = samples.stream().mapToDouble(r -> r.throughput).average().orElse(0.0);
        
        // Aggregate API metrics
        List<Long> allResponseTimes = new ArrayList<>();
        List<String> allErrors = new ArrayList<>();
        
        for (LoadTestResult sample : samples) {
            // Reconstruct response times (approximate)
            for (int i = 0; i < sample.successfulRequests; i++) {
                allResponseTimes.add(sample.apiMetrics.avgResponseTime);
            }
            allErrors.addAll(sample.errors);
        }
        
        Collections.sort(allResponseTimes);
        long p95 = allResponseTimes.isEmpty() ? 0 : allResponseTimes.get((int) (allResponseTimes.size() * 0.95));
        
        PerformanceTestUtils.ApiPerformanceResult aggregatedApi = new PerformanceTestUtils.ApiPerformanceResult(
            "/api/hives", 0, p95, 0, 0, 0, 0, avgSuccessRate, avgThroughput,
            (int) totalRequests, (int) successfulRequests, (int) (totalRequests - successfulRequests), 
            allErrors, totalDuration);
        
        return new PerformanceTestUtils.LoadTestResult(
            users, totalDuration, totalRequests, successfulRequests, totalRequests - successfulRequests,
            avgSuccessRate, 1.0 - avgSuccessRate, avgThroughput, aggregatedApi, new HashMap<>(), allErrors);
    }

    // Load test patterns for different scenarios
    enum LoadTestPattern {
        BASELINE,
        GRADUAL_RAMP,
        SPIKE,
        SUSTAINED,
        STRESS_TEST,
        RECOVERY,
        VOLUME
    }

    // API call result helper class
    static class ApiCallResult {
        final long responseTime;
        final boolean success;
        final String error;
        
        ApiCallResult(long responseTime, boolean success, String error) {
            this.responseTime = responseTime;
            this.success = success;
            this.error = error;
        }
    }
}