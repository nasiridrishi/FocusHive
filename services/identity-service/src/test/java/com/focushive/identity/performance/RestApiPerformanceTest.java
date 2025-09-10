package com.focushive.identity.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.User;
import com.focushive.identity.repository.PersonaRepository;
import com.focushive.identity.repository.UserRepository;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REST API performance tests to verify N+1 query optimizations work in real API calls.
 * Tests actual HTTP endpoints to ensure optimizations are effective in real-world scenarios.
 * 
 * Test Coverage:
 * - User profile endpoints with personas
 * - Persona listing endpoints
 * - Concurrent API calls performance
 * - Response time benchmarks
 * - Query count verification through HTTP layer
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.jpa.show-sql=false",
    "logging.level.org.hibernate.SQL=WARN",
    "spring.jpa.properties.hibernate.generate_statistics=true"
})
@DisplayName("UOL-335: REST API Performance Tests")
class RestApiPerformanceTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private SessionFactory sessionFactory;
    private Statistics hibernateStatistics;
    
    private List<User> testUsers;
    private List<Persona> testPersonas;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        hibernateStatistics = sessionFactory.getStatistics();
        hibernateStatistics.setStatisticsEnabled(true);
        hibernateStatistics.clear();

        // Create test data for API performance testing
        createApiTestData();
    }

    @AfterEach
    void cleanUp() {
        if (testPersonas != null) {
            personaRepository.deleteAll(testPersonas);
        }
        if (testUsers != null) {
            userRepository.deleteAll(testUsers);
        }
    }

    @Test
    @DisplayName("API Performance: GET /api/users - Load all users with personas")
    void testGetAllUsersApiPerformance() throws Exception {
        hibernateStatistics.clear();
        long startTime = System.nanoTime();

        // Execute API call
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        long endTime = System.nanoTime();
        long executionTimeMs = (endTime - startTime) / 1_000_000;

        // Parse response to verify data
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent).isNotEmpty();

        // Print API performance results
        printApiPerformanceResults("GET /api/users", 
            hibernateStatistics.getQueryExecutionCount(),
            executionTimeMs,
            responseContent.length());

        // Performance assertions
        assertThat(executionTimeMs)
            .describedAs("API response time should be under 500ms")
            .isLessThan(500);

        assertThat(hibernateStatistics.getQueryExecutionCount())
            .describedAs("API should use optimized queries (low count)")
            .isLessThan(5);
    }

    @Test
    @DisplayName("API Performance: GET /api/users/{id}/personas - Load user personas")
    void testGetUserPersonasApiPerformance() throws Exception {
        UUID userId = testUsers.get(0).getId();
        
        hibernateStatistics.clear();
        long startTime = System.nanoTime();

        // Execute API call
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/users/{id}/personas", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        long endTime = System.nanoTime();
        long executionTimeMs = (endTime - startTime) / 1_000_000;

        // Verify response content
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent).contains("\"name\""); // Should contain persona data

        printApiPerformanceResults("GET /api/users/{id}/personas",
            hibernateStatistics.getQueryExecutionCount(),
            executionTimeMs,
            responseContent.length());

        // Performance assertions
        assertThat(executionTimeMs)
            .describedAs("Single user personas API should be very fast")
            .isLessThan(100);

        assertThat(hibernateStatistics.getQueryExecutionCount())
            .describedAs("Single user query should be minimal")
            .isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("API Performance: Concurrent requests simulation")
    void testConcurrentApiRequestsPerformance() throws Exception {
        int concurrentRequests = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        
        hibernateStatistics.clear();
        long startTime = System.nanoTime();

        // Execute concurrent API requests
        List<CompletableFuture<ApiCallResult>> futures = new ArrayList<>();
        
        for (int i = 0; i < concurrentRequests; i++) {
            UUID userId = testUsers.get(i % testUsers.size()).getId();
            
            CompletableFuture<ApiCallResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    long requestStart = System.nanoTime();
                    
                    MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/users/{id}/personas", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn();
                    
                    long requestEnd = System.nanoTime();
                    long requestTime = (requestEnd - requestStart) / 1_000_000;
                    
                    return new ApiCallResult(requestTime, 
                        result.getResponse().getContentAsString().length(), true);
                        
                } catch (Exception e) {
                    return new ApiCallResult(0, 0, false);
                }
            }, executor);
            
            futures.add(future);
        }

        // Wait for all requests to complete
        List<ApiCallResult> results = new ArrayList<>();
        for (CompletableFuture<ApiCallResult> future : futures) {
            results.add(future.get());
        }

        long endTime = System.nanoTime();
        long totalExecutionTimeMs = (endTime - startTime) / 1_000_000;

        executor.shutdown();

        // Analyze concurrent request results
        printConcurrentApiResults(concurrentRequests, results, 
            hibernateStatistics.getQueryExecutionCount(), totalExecutionTimeMs);

        // Performance assertions
        assertThat(results.stream().allMatch(r -> r.success))
            .describedAs("All concurrent requests should succeed")
            .isTrue();

        double avgRequestTime = results.stream().mapToLong(r -> r.executionTimeMs).average().orElse(0);
        assertThat(avgRequestTime)
            .describedAs("Average concurrent request time should be reasonable")
            .isLessThan(200);

        // Total queries should still be reasonable due to optimizations
        assertThat(hibernateStatistics.getQueryExecutionCount())
            .describedAs("Concurrent requests should benefit from query optimizations")
            .isLessThan(concurrentRequests * 3); // Much better than N+1 behavior
    }

    @Test
    @DisplayName("API Performance: Bulk user operations")
    void testBulkUserOperationsApiPerformance() throws Exception {
        hibernateStatistics.clear();
        long startTime = System.nanoTime();

        // Execute bulk operations - multiple API calls in sequence
        List<Long> requestTimes = new ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
            UUID userId = testUsers.get(i).getId();
            long requestStart = System.nanoTime();
            
            // Get user profile
            mockMvc.perform(MockMvcRequestBuilders.get("/api/users/{id}", userId)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
                
            // Get user personas
            mockMvc.perform(MockMvcRequestBuilders.get("/api/users/{id}/personas", userId)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
            
            long requestEnd = System.nanoTime();
            requestTimes.add((requestEnd - requestStart) / 1_000_000);
        }

        long endTime = System.nanoTime();
        long totalExecutionTimeMs = (endTime - startTime) / 1_000_000;

        // Print bulk operations results
        printBulkOperationsResults(requestTimes, hibernateStatistics.getQueryExecutionCount(), 
            totalExecutionTimeMs);

        // Performance assertions
        assertThat(totalExecutionTimeMs)
            .describedAs("Bulk operations should complete efficiently")
            .isLessThan(1000);

        double avgRequestTime = requestTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        assertThat(avgRequestTime)
            .describedAs("Average request time should be fast")
            .isLessThan(100);
    }

    @Test
    @DisplayName("API Performance: Memory usage during API calls")
    void testApiMemoryUsage() throws Exception {
        // Force garbage collection before test
        System.gc();
        Thread.sleep(100); // Allow GC to complete
        
        long startMemory = getUsedMemory();
        hibernateStatistics.clear();

        // Execute multiple API calls to test memory efficiency
        for (int i = 0; i < 20; i++) {
            UUID userId = testUsers.get(i % testUsers.size()).getId();
            
            mockMvc.perform(MockMvcRequestBuilders.get("/api/users/{id}/personas", userId)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        }

        long endMemory = getUsedMemory();
        long memoryUsed = endMemory - startMemory;

        // Print memory usage results
        System.out.println("\n" + "=".repeat(60));
        System.out.println("API MEMORY USAGE TEST");
        System.out.println("=".repeat(60));
        System.out.println("API calls executed: 20");
        System.out.println("Memory used: " + formatBytes(memoryUsed));
        System.out.println("Queries executed: " + hibernateStatistics.getQueryExecutionCount());
        System.out.println("Average memory per call: " + formatBytes(memoryUsed / 20));
        System.out.println("=".repeat(60));

        // Memory assertions
        assertThat(memoryUsed)
            .describedAs("Memory usage should be reasonable for API calls")
            .isLessThan(50 * 1024 * 1024); // Less than 50MB

        assertThat(hibernateStatistics.getQueryExecutionCount())
            .describedAs("Query count should demonstrate optimization")
            .isLessThan(30); // Much better than 20+ queries without optimization
    }

    private void createApiTestData() {
        testUsers = new ArrayList<>();
        testPersonas = new ArrayList<>();
        
        // Create 10 users with 3 personas each for API testing
        for (int i = 0; i < 10; i++) {
            User user = User.builder()
                .username("api_user_" + i + "_" + System.nanoTime())
                .email("api_user_" + i + "_" + System.nanoTime() + "@test.com")
                .password("password123")
                .firstName("API")
                .lastName("User" + i)
                .emailVerified(true)
                .enabled(true)
                .build();
            
            User savedUser = userRepository.save(user);
            testUsers.add(savedUser);
            
            // Create personas for API testing
            for (int j = 0; j < 3; j++) {
                Persona persona = Persona.builder()
                    .user(savedUser)
                    .name("ApiPersona_" + j)
                    .type(Persona.PersonaType.values()[j % Persona.PersonaType.values().length])
                    .displayName("API Display Persona " + j)
                    .bio("Bio for API persona " + j)
                    .isDefault(j == 0)
                    .isActive(j == 0)
                    .build();
                
                // Add custom attributes
                persona.getCustomAttributes().put("apiKey" + j, "apiValue" + j);
                persona.getCustomAttributes().put("category", "api-test");
                
                Persona savedPersona = personaRepository.save(persona);
                testPersonas.add(savedPersona);
            }
        }
        
        entityManager.flush();
        entityManager.clear();
    }

    private void printApiPerformanceResults(String endpoint, long queryCount, 
                                           long executionTimeMs, int responseSize) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("API PERFORMANCE: " + endpoint);
        System.out.println("=".repeat(60));
        System.out.println("Query execution count: " + queryCount);
        System.out.println("Execution time: " + executionTimeMs + " ms");
        System.out.println("Response size: " + formatBytes(responseSize));
        System.out.println("=".repeat(60));
    }

    private void printConcurrentApiResults(int requestCount, List<ApiCallResult> results,
                                          long totalQueries, long totalTimeMs) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("CONCURRENT API REQUESTS PERFORMANCE");
        System.out.println("=".repeat(60));
        System.out.println("Concurrent requests: " + requestCount);
        System.out.println("Total execution time: " + totalTimeMs + " ms");
        System.out.println("Total queries: " + totalQueries);
        
        double avgTime = results.stream().mapToLong(r -> r.executionTimeMs).average().orElse(0);
        long maxTime = results.stream().mapToLong(r -> r.executionTimeMs).max().orElse(0);
        long minTime = results.stream().mapToLong(r -> r.executionTimeMs).min().orElse(0);
        
        System.out.println("Average request time: " + String.format("%.1f", avgTime) + " ms");
        System.out.println("Min request time: " + minTime + " ms");
        System.out.println("Max request time: " + maxTime + " ms");
        System.out.println("Success rate: " + (results.stream().allMatch(r -> r.success) ? "100%" : "< 100%"));
        System.out.println("=".repeat(60));
    }

    private void printBulkOperationsResults(List<Long> requestTimes, long totalQueries, 
                                           long totalTimeMs) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("BULK OPERATIONS PERFORMANCE");
        System.out.println("=".repeat(60));
        System.out.println("Operations executed: " + requestTimes.size() * 2); // 2 API calls per operation
        System.out.println("Total time: " + totalTimeMs + " ms");
        System.out.println("Total queries: " + totalQueries);
        
        double avgTime = requestTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        System.out.println("Average operation time: " + String.format("%.1f", avgTime) + " ms");
        System.out.println("=".repeat(60));
    }

    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private static class ApiCallResult {
        final long executionTimeMs;
        final int responseSize;
        final boolean success;

        ApiCallResult(long executionTimeMs, int responseSize, boolean success) {
            this.executionTimeMs = executionTimeMs;
            this.responseSize = responseSize;
            this.success = success;
        }
    }
}