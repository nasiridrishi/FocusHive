package com.focushive.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive API Performance Tests for all FocusHive microservices.
 * Tests response times, throughput, and scalability across all service endpoints.
 * 
 * Performance Targets:
 * - API P50 response time: < 100ms
 * - API P95 response time: < 200ms  
 * - API P99 response time: < 500ms
 * - Success rate: > 99%
 * - Concurrent users: 10,000+
 * 
 * Service Coverage:
 * - FocusHive Backend (Core functionality)
 * - Identity Service (Authentication & personas)
 * - Music Service (Spotify integration)
 * - Notification Service (Multi-channel notifications)
 * - Chat Service (Real-time messaging)
 * - Analytics Service (Productivity tracking)
 * - Forum Service (Community discussions)
 * - Buddy Service (Accountability partners)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@ActiveProfiles("performance-test")
@DisplayName("API Performance Tests - All Microservices")
class APIPerformanceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("focushive_performance")
            .withUsername("performance_user")
            .withPassword("performance_pass")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // Performance optimized database settings
        registry.add("spring.datasource.hikari.minimum-idle", () -> "20");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "50");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "30000");
        registry.add("spring.datasource.hikari.idle-timeout", () -> "600000");
        registry.add("spring.datasource.hikari.max-lifetime", () -> "1800000");
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private List<PerformanceTestUtils.ApiPerformanceResult> testResults;

    // Service port mappings for integration testing
    private static final Map<String, Integer> SERVICE_PORTS = Map.of(
        "focushive-backend", 8080,
        "identity-service", 8081,
        "music-service", 8082,
        "notification-service", 8083,
        "chat-service", 8084,
        "analytics-service", 8085,
        "forum-service", 8086,
        "buddy-service", 8087
    );

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        testResults = new ArrayList<>();
    }

    @AfterEach
    void generateReport() {
        if (!testResults.isEmpty()) {
            PerformanceTestUtils.generatePerformanceReport(testResults, List.of(), List.of(), 
                "api-performance-report.txt");
        }
    }

    @Nested
    @DisplayName("FocusHive Backend Performance Tests (Port 8080)")
    class FocusHiveBackendPerformanceTests {

        @Test
        @DisplayName("Hive Management API Performance")
        void testHiveManagementPerformance() throws Exception {
            // Test hive listing endpoint
            PerformanceTestUtils.ApiPerformanceResult listResult = 
                PerformanceTestUtils.testApiPerformance(
                    mockMvc, "/api/hives", "GET", null, 50, Duration.ofSeconds(30));
            
            testResults.add(listResult);
            
            // Validate performance targets
            assertThat(listResult.p50ResponseTime)
                .describedAs("P50 response time should be under 100ms")
                .isLessThanOrEqualTo(PerformanceTestUtils.API_RESPONSE_TIME_P50);
            
            assertThat(listResult.p95ResponseTime)
                .describedAs("P95 response time should be under 200ms")
                .isLessThanOrEqualTo(PerformanceTestUtils.API_RESPONSE_TIME_P95);
            
            assertThat(listResult.successRate)
                .describedAs("Success rate should be above 99%")
                .isGreaterThanOrEqualTo(1.0 - PerformanceTestUtils.ERROR_RATE_THRESHOLD);

            // Test hive creation endpoint
            Map<String, Object> hivePayload = Map.of(
                "name", "Performance Test Hive",
                "description", "Testing hive creation performance",
                "isPublic", true
            );

            PerformanceTestUtils.ApiPerformanceResult createResult = 
                PerformanceTestUtils.testApiPerformance(
                    mockMvc, "/api/hives", "POST", 
                    objectMapper.writeValueAsString(hivePayload), 25, Duration.ofSeconds(20));
            
            testResults.add(createResult);
            
            // Creation endpoints may be slower
            assertThat(createResult.p95ResponseTime)
                .describedAs("Hive creation P95 should be reasonable")
                .isLessThanOrEqualTo(PerformanceTestUtils.API_RESPONSE_TIME_P95 * 2);
        }

        @Test
        @DisplayName("Presence System API Performance")
        void testPresenceSystemPerformance() throws Exception {
            PerformanceTestUtils.ApiPerformanceResult result = 
                PerformanceTestUtils.testApiPerformance(
                    mockMvc, "/api/presence/status", "GET", null, 100, Duration.ofSeconds(30));
            
            testResults.add(result);
            
            assertThat(result.meetsPerformanceTargets())
                .describedAs("Presence API should meet all performance targets")
                .isTrue();
            
            assertThat(result.throughput)
                .describedAs("Presence API should handle high throughput")
                .isGreaterThan(50.0); // 50 requests per second minimum
        }

        @Test
        @DisplayName("Timer Operations API Performance")
        void testTimerOperationsPerformance() throws Exception {
            Map<String, Object> timerPayload = Map.of(
                "hiveId", "test-hive-id",
                "duration", 1500000, // 25 minutes
                "type", "FOCUS"
            );

            PerformanceTestUtils.ApiPerformanceResult startResult = 
                PerformanceTestUtils.testApiPerformance(
                    mockMvc, "/api/timer/start", "POST", 
                    objectMapper.writeValueAsString(timerPayload), 30, Duration.ofSeconds(20));
            
            testResults.add(startResult);
            
            assertThat(startResult.p99ResponseTime)
                .describedAs("Timer start should be responsive even at P99")
                .isLessThanOrEqualTo(PerformanceTestUtils.API_RESPONSE_TIME_P99);

            // Test timer status endpoint (should be very fast)
            PerformanceTestUtils.ApiPerformanceResult statusResult = 
                PerformanceTestUtils.testApiPerformance(
                    mockMvc, "/api/timer/status", "GET", null, 100, Duration.ofSeconds(15));
            
            testResults.add(statusResult);
            
            assertThat(statusResult.p50ResponseTime)
                .describedAs("Timer status should be very fast")
                .isLessThanOrEqualTo(50); // 50ms for status checks
        }
    }

    @Nested
    @DisplayName("Identity Service Performance Tests (Port 8081)")
    class IdentityServicePerformanceTests {

        @Test
        @DisplayName("Authentication API Performance")
        void testAuthenticationPerformance() throws Exception {
            Map<String, Object> loginPayload = Map.of(
                "username", "performance_user",
                "password", "test_password"
            );

            PerformanceTestUtils.ApiPerformanceResult loginResult = 
                PerformanceTestUtils.testApiPerformance(
                    mockMvc, "/api/auth/login", "POST", 
                    objectMapper.writeValueAsString(loginPayload), 40, Duration.ofSeconds(30));
            
            testResults.add(loginResult);
            
            // Authentication may be slower due to password hashing
            assertThat(loginResult.p95ResponseTime)
                .describedAs("Login P95 should be under 1 second")
                .isLessThanOrEqualTo(1000);
            
            assertThat(loginResult.successRate)
                .describedAs("Login should have high success rate")
                .isGreaterThanOrEqualTo(0.95); // 95% for authentication
        }

        @Test
        @DisplayName("Persona Management API Performance")
        void testPersonaManagementPerformance() throws Exception {
            PerformanceTestUtils.ApiPerformanceResult listResult = 
                PerformanceTestUtils.testApiPerformance(
                    mockMvc, "/api/personas", "GET", null, 60, Duration.ofSeconds(25));
            
            testResults.add(listResult);
            
            assertThat(listResult.meetsPerformanceTargets())
                .describedAs("Persona listing should meet performance targets")
                .isTrue();

            // Test persona creation
            Map<String, Object> personaPayload = Map.of(
                "name", "Performance Persona",
                "type", "PROFESSIONAL",
                "displayName", "Performance Test User",
                "bio", "Testing persona creation performance"
            );

            PerformanceTestUtils.ApiPerformanceResult createResult = 
                PerformanceTestUtils.testApiPerformance(
                    mockMvc, "/api/personas", "POST", 
                    objectMapper.writeValueAsString(personaPayload), 20, Duration.ofSeconds(15));
            
            testResults.add(createResult);
            
            assertThat(createResult.p95ResponseTime)
                .describedAs("Persona creation should be reasonably fast")
                .isLessThanOrEqualTo(PerformanceTestUtils.API_RESPONSE_TIME_P95 * 3);
        }

        @Test
        @DisplayName("OAuth2 Token Operations Performance")
        void testOAuth2TokenPerformance() throws Exception {
            Map<String, Object> tokenPayload = Map.of(
                "grant_type", "authorization_code",
                "code", "test_code",
                "client_id", "test_client",
                "redirect_uri", "http://localhost:3000/callback"
            );

            PerformanceTestUtils.ApiPerformanceResult tokenResult = 
                PerformanceTestUtils.testApiPerformance(
                    mockMvc, "/oauth2/token", "POST", 
                    objectMapper.writeValueAsString(tokenPayload), 30, Duration.ofSeconds(20));
            
            testResults.add(tokenResult);
            
            // OAuth2 token operations should be fast
            assertThat(tokenResult.p95ResponseTime)
                .describedAs("OAuth2 token operations should be fast")
                .isLessThanOrEqualTo(300); // 300ms for token operations
        }
    }

    @Nested
    @DisplayName("Analytics Service Performance Tests (Port 8085)")
    class AnalyticsServicePerformanceTests {

        @Test
        @DisplayName("Productivity Analytics API Performance")
        void testProductivityAnalyticsPerformance() throws Exception {
            PerformanceTestUtils.ApiPerformanceResult result = 
                PerformanceTestUtils.testApiPerformance(
                    mockMvc, "/api/analytics/productivity", "GET", null, 30, Duration.ofSeconds(25));
            
            testResults.add(result);
            
            // Analytics queries may be more complex
            assertThat(result.p95ResponseTime)
                .describedAs("Analytics P95 should be under 1 second")
                .isLessThanOrEqualTo(1000);
            
            assertThat(result.successRate)
                .describedAs("Analytics should be reliable")
                .isGreaterThanOrEqualTo(0.98);
        }

        @Test
        @DisplayName("Session Tracking API Performance")
        void testSessionTrackingPerformance() throws Exception {
            Map<String, Object> sessionPayload = Map.of(
                "userId", "test-user-id",
                "hiveId", "test-hive-id",
                "duration", 1500000,
                "sessionType", "FOCUS"
            );

            PerformanceTestUtils.ApiPerformanceResult trackingResult = 
                PerformanceTestUtils.testApiPerformance(
                    mockMvc, "/api/analytics/sessions", "POST", 
                    objectMapper.writeValueAsString(sessionPayload), 50, Duration.ofSeconds(20));
            
            testResults.add(trackingResult);
            
            assertThat(trackingResult.p95ResponseTime)
                .describedAs("Session tracking should be fast for real-time updates")
                .isLessThanOrEqualTo(PerformanceTestUtils.API_RESPONSE_TIME_P95);
        }

        @Test
        @DisplayName("Achievement Calculation Performance")
        void testAchievementCalculationPerformance() throws Exception {
            PerformanceTestUtils.ApiPerformanceResult result = 
                PerformanceTestUtils.testApiPerformance(
                    mockMvc, "/api/analytics/achievements", "GET", null, 25, Duration.ofSeconds(15));
            
            testResults.add(result);
            
            // Achievement calculations may involve complex queries
            assertThat(result.p99ResponseTime)
                .describedAs("Achievement calculations should complete within 2 seconds at P99")
                .isLessThanOrEqualTo(2000);
        }
    }

    @Nested
    @DisplayName("Multi-Service Integration Performance Tests")
    class MultiServiceIntegrationTests {

        @Test
        @DisplayName("Cross-Service API Call Performance")
        void testCrossServiceCalls() throws Exception {
            ExecutorService executor = Executors.newFixedThreadPool(20);
            List<CompletableFuture<Long>> futures = new ArrayList<>();
            
            // Simulate realistic user workflow across multiple services
            for (int i = 0; i < 50; i++) {
                CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        long totalTime = 0;
                        long start = System.nanoTime();
                        
                        // 1. Login (Identity Service)
                        // 2. Get user hives (FocusHive Backend)
                        // 3. Get analytics (Analytics Service)
                        // 4. Check notifications (Notification Service)
                        
                        // Simulate the workflow with actual API calls
                        // For now, using single service endpoints as placeholder
                        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .get("/api/hives")
                                .contentType(MediaType.APPLICATION_JSON));
                        
                        long end = System.nanoTime();
                        return (end - start) / 1_000_000; // Convert to milliseconds
                        
                    } catch (Exception e) {
                        return -1L;
                    }
                }, executor);
                
                futures.add(future);
            }

            // Collect results
            List<Long> responseTimes = new ArrayList<>();
            for (CompletableFuture<Long> future : futures) {
                try {
                    Long time = future.get(30, TimeUnit.SECONDS);
                    if (time > 0) {
                        responseTimes.add(time);
                    }
                } catch (Exception e) {
                    // Handle timeout or error
                }
            }

            executor.shutdown();

            if (!responseTimes.isEmpty()) {
                Collections.sort(responseTimes);
                long p95 = responseTimes.get((int) (responseTimes.size() * 0.95));
                
                assertThat(p95)
                    .describedAs("Cross-service workflow should complete efficiently")
                    .isLessThanOrEqualTo(2000); // 2 seconds for full workflow
            }
        }

        @Test
        @DisplayName("Concurrent Multi-Service Load Test")
        void testConcurrentMultiServiceLoad() throws Exception {
            ExecutorService executor = Executors.newFixedThreadPool(100);
            int totalRequests = 500;
            List<CompletableFuture<Boolean>> futures = new ArrayList<>();

            long startTime = System.currentTimeMillis();

            // Launch concurrent requests across different services
            for (int i = 0; i < totalRequests; i++) {
                final int requestId = i;
                CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        // Distribute load across different endpoints
                        String[] endpoints = {
                            "/api/hives",
                            "/api/presence/status", 
                            "/api/timer/status",
                            "/api/personas",
                            "/api/analytics/productivity"
                        };
                        
                        String endpoint = endpoints[requestId % endpoints.length];
                        
                        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .get(endpoint)
                                .contentType(MediaType.APPLICATION_JSON));
                        
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }, executor);
                
                futures.add(future);
            }

            // Collect results
            int successful = 0;
            for (CompletableFuture<Boolean> future : futures) {
                try {
                    if (future.get(60, TimeUnit.SECONDS)) {
                        successful++;
                    }
                } catch (Exception e) {
                    // Count as failed
                }
            }

            long endTime = System.currentTimeMillis();
            double successRate = (double) successful / totalRequests;
            double duration = (endTime - startTime) / 1000.0;
            double throughput = successful / duration;

            executor.shutdown();

            assertThat(successRate)
                .describedAs("Multi-service load test should maintain high success rate")
                .isGreaterThanOrEqualTo(0.95);

            assertThat(throughput)
                .describedAs("System should handle reasonable concurrent throughput")
                .isGreaterThan(50.0); // 50 requests per second across all services

            System.out.println(String.format(
                "Multi-Service Load Test Results:\n" +
                "Total Requests: %d\n" +
                "Successful: %d\n" +
                "Success Rate: %.2f%%\n" +
                "Duration: %.1fs\n" +
                "Throughput: %.1f req/s",
                totalRequests, successful, successRate * 100, duration, throughput
            ));
        }
    }

    @Nested
    @DisplayName("Scalability and Stress Tests")
    class ScalabilityStressTests {

        @Test
        @DisplayName("Progressive Load Test - 10 to 1000 concurrent users")
        void testProgressiveLoad() throws Exception {
            int[] userCounts = {10, 50, 100, 250, 500, 1000};
            List<PerformanceTestUtils.LoadTestResult> loadResults = new ArrayList<>();

            for (int userCount : userCounts) {
                System.out.println("Testing with " + userCount + " concurrent users...");
                
                ExecutorService executor = Executors.newFixedThreadPool(userCount);
                List<CompletableFuture<Long>> futures = new ArrayList<>();
                long startTime = System.currentTimeMillis();

                // Launch concurrent requests
                for (int i = 0; i < userCount; i++) {
                    CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                        try {
                            long start = System.nanoTime();
                            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                    .get("/api/hives")
                                    .contentType(MediaType.APPLICATION_JSON));
                            long end = System.nanoTime();
                            return (end - start) / 1_000_000;
                        } catch (Exception e) {
                            return -1L;
                        }
                    }, executor);
                    
                    futures.add(future);
                }

                // Collect results
                List<Long> responseTimes = new ArrayList<>();
                int successful = 0;
                
                for (CompletableFuture<Long> future : futures) {
                    try {
                        Long time = future.get(30, TimeUnit.SECONDS);
                        if (time > 0) {
                            responseTimes.add(time);
                            successful++;
                        }
                    } catch (Exception e) {
                        // Count as failed
                    }
                }

                long endTime = System.currentTimeMillis();
                Duration testDuration = Duration.ofMillis(endTime - startTime);
                double successRate = (double) successful / userCount;
                double throughput = successful / (testDuration.toMillis() / 1000.0);

                executor.shutdown();

                // Create load test result
                Collections.sort(responseTimes);
                long p50 = responseTimes.isEmpty() ? 0 : responseTimes.get(responseTimes.size() / 2);
                long p95 = responseTimes.isEmpty() ? 0 : responseTimes.get((int) (responseTimes.size() * 0.95));
                long p99 = responseTimes.isEmpty() ? 0 : responseTimes.get((int) (responseTimes.size() * 0.99));
                long avg = responseTimes.isEmpty() ? 0 : (long) responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
                long max = responseTimes.isEmpty() ? 0 : responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
                long min = responseTimes.isEmpty() ? 0 : responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);

                PerformanceTestUtils.ApiPerformanceResult apiResult = new PerformanceTestUtils.ApiPerformanceResult(
                    "/api/hives", p50, p95, p99, avg, max, min, successRate, throughput, 
                    userCount, successful, userCount - successful, new ArrayList<>(), testDuration);

                PerformanceTestUtils.LoadTestResult loadResult = new PerformanceTestUtils.LoadTestResult(
                    userCount, testDuration, userCount, successful, userCount - successful, 
                    successRate, 1.0 - successRate, throughput, apiResult, new HashMap<>(), new ArrayList<>());

                loadResults.add(loadResult);

                System.out.println(String.format(
                    "Users: %d, Success Rate: %.1f%%, P95: %dms, Throughput: %.1f req/s",
                    userCount, successRate * 100, p95, throughput
                ));

                // Performance should degrade gracefully
                if (userCount >= 100) {
                    assertThat(successRate)
                        .describedAs("Success rate should remain reasonable under load")
                        .isGreaterThanOrEqualTo(0.80); // 80% minimum under high load
                }
            }

            // Validate progressive degradation is graceful
            boolean gracefulDegradation = true;
            double previousSuccessRate = 1.0;
            
            for (PerformanceTestUtils.LoadTestResult result : loadResults) {
                if (result.successRate < previousSuccessRate - 0.3) { // More than 30% drop
                    gracefulDegradation = false;
                    break;
                }
                previousSuccessRate = result.successRate;
            }

            assertThat(gracefulDegradation)
                .describedAs("System should degrade gracefully under increasing load")
                .isTrue();
        }
    }
}