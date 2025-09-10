package com.focushive.identity.performance;

import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.User;
import com.focushive.identity.repository.PersonaRepository;
import com.focushive.identity.repository.UserRepository;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive performance tests for UOL-335 N+1 query optimizations.
 * Tests measure query count, execution time, and memory usage with varying data sizes.
 * 
 * Test Coverage:
 * - 10, 100, 1000 users with 3 personas each
 * - Query count validation using Hibernate Statistics
 * - Performance improvement verification (>50% improvement)
 * - Memory usage tracking
 * - Comparison between optimized and non-optimized approaches
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.jpa.show-sql=false",
    "logging.level.org.hibernate.SQL=WARN",
    "spring.jpa.properties.hibernate.generate_statistics=true"
})
@DisplayName("UOL-335: Comprehensive N+1 Query Performance Tests")
class ComprehensiveN1QueryPerformanceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private SessionFactory sessionFactory;
    private Statistics hibernateStatistics;

    // Test data tracking
    private final Map<Integer, List<User>> testDataBySize = new HashMap<>();
    private final Map<Integer, PerformanceMetrics> optimizedResults = new HashMap<>();
    private final Map<Integer, PerformanceMetrics> nonOptimizedResults = new HashMap<>();

    @BeforeEach
    void setUp() {
        sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        hibernateStatistics = sessionFactory.getStatistics();
        hibernateStatistics.setStatisticsEnabled(true);
        hibernateStatistics.clear();
    }

    @AfterEach
    void cleanUp() {
        // Clean up all test data
        testDataBySize.values().forEach(users -> {
            users.forEach(user -> {
                personaRepository.deleteAll(user.getPersonas());
            });
            userRepository.deleteAll(users);
        });
        testDataBySize.clear();
    }

    @Test
    @DisplayName("Performance Test: 10 users with optimized queries")
    void performanceTest_10Users_Optimized() {
        testOptimizedPerformance(10, 3);
    }

    @Test
    @DisplayName("Performance Test: 100 users with optimized queries")
    void performanceTest_100Users_Optimized() {
        testOptimizedPerformance(100, 3);
    }

    @Test
    @DisplayName("Performance Test: 1000 users with optimized queries")
    void performanceTest_1000Users_Optimized() {
        testOptimizedPerformance(1000, 3);
    }

    @Test
    @DisplayName("Performance Test: 10 users with NON-optimized queries")
    void performanceTest_10Users_NonOptimized() {
        testNonOptimizedPerformance(10, 3);
    }

    @Test
    @DisplayName("Performance Test: 100 users with NON-optimized queries")
    void performanceTest_100Users_NonOptimized() {
        testNonOptimizedPerformance(100, 3);
    }

    @Test
    @DisplayName("Performance Test: 1000 users with NON-optimized queries")
    void performanceTest_1000Users_NonOptimized() {
        testNonOptimizedPerformance(1000, 3);
    }

    @Test
    @DisplayName("Comprehensive Performance Comparison Report")
    @Order(Integer.MAX_VALUE)
    void generatePerformanceComparisonReport() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("UOL-335 N+1 QUERY OPTIMIZATION PERFORMANCE REPORT");
        System.out.println("=".repeat(80));

        for (int size : List.of(10, 100, 1000)) {
            PerformanceMetrics optimized = optimizedResults.get(size);
            PerformanceMetrics nonOptimized = nonOptimizedResults.get(size);

            if (optimized != null && nonOptimized != null) {
                printPerformanceComparison(size, optimized, nonOptimized);
            }
        }

        System.out.println("=".repeat(80));
    }

    private void testOptimizedPerformance(int userCount, int personasPerUser) {
        // Setup test data
        List<User> users = createTestData(userCount, personasPerUser);
        testDataBySize.put(userCount, users);

        // Clear statistics before test
        hibernateStatistics.clear();
        long startMemory = getUsedMemory();
        long startTime = System.nanoTime();

        // Execute optimized query using EntityGraph
        List<User> loadedUsers = userRepository.findAll(); // Uses @EntityGraph("User.withPersonas")
        
        // Access personas to trigger loading
        int totalPersonasAccessed = 0;
        for (User user : loadedUsers) {
            List<Persona> personas = user.getPersonas();
            totalPersonasAccessed += personas.size();
            
            // Access persona attributes to test complete loading
            for (Persona persona : personas) {
                persona.getName();
                persona.getDisplayName();
                persona.getCustomAttributes().size();
            }
        }

        long endTime = System.nanoTime();
        long endMemory = getUsedMemory();

        // Capture metrics
        PerformanceMetrics metrics = new PerformanceMetrics(
            userCount,
            totalPersonasAccessed,
            hibernateStatistics.getQueryExecutionCount(),
            hibernateStatistics.getEntityLoadCount(),
            (endTime - startTime) / 1_000_000, // Convert to milliseconds
            endMemory - startMemory
        );

        optimizedResults.put(userCount, metrics);
        printOptimizedResults(metrics);

        // Assertions for optimized performance
        assertThat(loadedUsers.size()).isEqualTo(userCount);
        assertThat(totalPersonasAccessed).isEqualTo(userCount * personasPerUser);
        
        // Optimized queries should execute in O(1) database calls
        assertThat(hibernateStatistics.getQueryExecutionCount())
            .describedAs("Optimized queries should execute in constant time (O(1))")
            .isLessThanOrEqualTo(3); // Should be 1-3 queries regardless of data size
    }

    private void testNonOptimizedPerformance(int userCount, int personasPerUser) {
        // Setup test data
        List<User> users = createTestData(userCount, personasPerUser);
        testDataBySize.put(userCount + 10000, users); // Different key to avoid conflict

        // Clear statistics before test
        hibernateStatistics.clear();
        long startMemory = getUsedMemory();
        long startTime = System.nanoTime();

        // Execute NON-optimized query (without EntityGraph)
        List<UUID> userIds = users.stream().map(User::getId).toList();
        int totalPersonasAccessed = 0;
        
        for (UUID userId : userIds) {
            // This simulates the N+1 problem - separate query for each user's personas
            List<Persona> personas = personaRepository.findByUserId(userId);
            totalPersonasAccessed += personas.size();
            
            // Access persona attributes - triggers additional queries
            for (Persona persona : personas) {
                persona.getName();
                persona.getDisplayName();
                persona.getCustomAttributes().size(); // Each access may trigger additional queries
            }
        }

        long endTime = System.nanoTime();
        long endMemory = getUsedMemory();

        // Capture metrics
        PerformanceMetrics metrics = new PerformanceMetrics(
            userCount,
            totalPersonasAccessed,
            hibernateStatistics.getQueryExecutionCount(),
            hibernateStatistics.getEntityLoadCount(),
            (endTime - startTime) / 1_000_000,
            endMemory - startMemory
        );

        nonOptimizedResults.put(userCount, metrics);
        printNonOptimizedResults(metrics);

        // Assertions for non-optimized performance
        assertThat(totalPersonasAccessed).isEqualTo(userCount * personasPerUser);
        
        // Non-optimized queries should show N+1 behavior
        assertThat(hibernateStatistics.getQueryExecutionCount())
            .describedAs("Non-optimized queries should show N+1 behavior (O(n))")
            .isGreaterThan(userCount); // Should be at least N+1 queries
    }

    private List<User> createTestData(int userCount, int personasPerUser) {
        List<User> users = new ArrayList<>();
        
        for (int i = 0; i < userCount; i++) {
            User user = User.builder()
                .username("perftest_user_" + i + "_" + System.nanoTime())
                .email("perftest_user_" + i + "_" + System.nanoTime() + "@test.com")
                .password("password123")
                .firstName("Perf")
                .lastName("User" + i)
                .emailVerified(true)
                .enabled(true)
                .build();
            
            User savedUser = userRepository.save(user);
            users.add(savedUser);
            
            // Create personas for each user
            for (int j = 0; j < personasPerUser; j++) {
                Persona persona = Persona.builder()
                    .user(savedUser)
                    .name("Persona_" + j)
                    .type(Persona.PersonaType.values()[j % Persona.PersonaType.values().length])
                    .displayName("Display Persona " + j)
                    .bio("Bio for persona " + j)
                    .isDefault(j == 0)
                    .isActive(j == 0)
                    .build();
                
                // Add custom attributes to test ElementCollection loading
                persona.getCustomAttributes().put("key" + j, "value" + j);
                persona.getCustomAttributes().put("category", "performance-test");
                
                personaRepository.save(persona);
            }
        }
        
        entityManager.flush();
        entityManager.clear(); // Clear persistence context
        return users;
    }

    private void printOptimizedResults(PerformanceMetrics metrics) {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("OPTIMIZED PERFORMANCE RESULTS");
        System.out.println("-".repeat(60));
        System.out.println("Dataset size: " + metrics.userCount + " users, " + metrics.totalPersonas + " personas");
        System.out.println("Query execution count: " + metrics.queryCount);
        System.out.println("Entity load count: " + metrics.entityLoadCount);
        System.out.println("Execution time: " + metrics.executionTimeMs + " ms");
        System.out.println("Memory usage: " + formatBytes(metrics.memoryUsageBytes));
        System.out.println("Avg time per user: " + String.format("%.2f", metrics.executionTimeMs / (double) metrics.userCount) + " ms");
        System.out.println("-".repeat(60));
    }

    private void printNonOptimizedResults(PerformanceMetrics metrics) {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("NON-OPTIMIZED PERFORMANCE RESULTS");
        System.out.println("-".repeat(60));
        System.out.println("Dataset size: " + metrics.userCount + " users, " + metrics.totalPersonas + " personas");
        System.out.println("Query execution count: " + metrics.queryCount);
        System.out.println("Entity load count: " + metrics.entityLoadCount);
        System.out.println("Execution time: " + metrics.executionTimeMs + " ms");
        System.out.println("Memory usage: " + formatBytes(metrics.memoryUsageBytes));
        System.out.println("Avg time per user: " + String.format("%.2f", metrics.executionTimeMs / (double) metrics.userCount) + " ms");
        System.out.println("-".repeat(60));
    }

    private void printPerformanceComparison(int size, PerformanceMetrics optimized, PerformanceMetrics nonOptimized) {
        double timeImprovement = ((nonOptimized.executionTimeMs - optimized.executionTimeMs) / (double) nonOptimized.executionTimeMs) * 100;
        double queryImprovement = ((nonOptimized.queryCount - optimized.queryCount) / (double) nonOptimized.queryCount) * 100;
        
        System.out.println("\n" + size + " USERS COMPARISON:");
        System.out.println("-".repeat(40));
        System.out.println("Execution Time:");
        System.out.println("  Optimized:     " + optimized.executionTimeMs + " ms");
        System.out.println("  Non-optimized: " + nonOptimized.executionTimeMs + " ms");
        System.out.println("  Improvement:   " + String.format("%.1f", timeImprovement) + "%");
        System.out.println();
        System.out.println("Query Count:");
        System.out.println("  Optimized:     " + optimized.queryCount);
        System.out.println("  Non-optimized: " + nonOptimized.queryCount);
        System.out.println("  Improvement:   " + String.format("%.1f", queryImprovement) + "%");
        System.out.println();
        System.out.println("Memory Usage:");
        System.out.println("  Optimized:     " + formatBytes(optimized.memoryUsageBytes));
        System.out.println("  Non-optimized: " + formatBytes(nonOptimized.memoryUsageBytes));
        
        // Verify performance requirements
        assertThat(timeImprovement)
            .describedAs("Performance improvement should be at least 50%% for %d users", size)
            .isGreaterThan(50.0);
            
        assertThat(queryImprovement)
            .describedAs("Query count reduction should be significant for %d users", size)
            .isGreaterThan(80.0);
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

    /**
     * Performance metrics container
     */
    private static class PerformanceMetrics {
        final int userCount;
        final int totalPersonas;
        final long queryCount;
        final long entityLoadCount;
        final long executionTimeMs;
        final long memoryUsageBytes;

        PerformanceMetrics(int userCount, int totalPersonas, long queryCount, 
                         long entityLoadCount, long executionTimeMs, long memoryUsageBytes) {
            this.userCount = userCount;
            this.totalPersonas = totalPersonas;
            this.queryCount = queryCount;
            this.entityLoadCount = entityLoadCount;
            this.executionTimeMs = executionTimeMs;
            this.memoryUsageBytes = memoryUsageBytes;
        }
    }
}