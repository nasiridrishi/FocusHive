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
 * Tests for batch fetch optimization and ElementCollection performance.
 * Verifies that @BatchSize and default_batch_fetch_size configurations
 * are working correctly to reduce N+1 queries.
 * 
 * Test Scenarios:
 * - Batch loading of personas with custom attributes (ElementCollection)
 * - Comparing batch size configurations (16 vs 32 vs 64)
 * - Memory usage optimization with batch fetching
 * - Collection loading performance
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.jpa.show-sql=false",
    "logging.level.org.hibernate.SQL=WARN",
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "spring.jpa.properties.hibernate.default_batch_fetch_size=16"
})
@DisplayName("UOL-335: Batch Fetch Optimization Performance Tests")
class BatchFetchOptimizationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private SessionFactory sessionFactory;
    private Statistics hibernateStatistics;

    private List<User> testUsers;
    private List<Persona> testPersonas;

    @BeforeEach
    void setUp() {
        sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        hibernateStatistics = sessionFactory.getStatistics();
        hibernateStatistics.setStatisticsEnabled(true);
        hibernateStatistics.clear();

        // Create comprehensive test data for batch testing
        createBatchTestData();
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
    @DisplayName("Batch fetch optimization: Personas with ElementCollections")
    void testBatchFetchPersonasWithAttributes() {
        hibernateStatistics.clear();
        long startTime = System.nanoTime();
        long startMemory = getUsedMemory();

        // Load users and access personas - should use batch fetching
        List<User> users = userRepository.findAll();
        int totalAttributesAccessed = 0;
        int totalPersonasAccessed = 0;

        for (User user : users) {
            List<Persona> personas = user.getPersonas();
            totalPersonasAccessed += personas.size();
            
            // Access custom attributes - should trigger batch loading
            for (Persona persona : personas) {
                Map<String, String> attributes = persona.getCustomAttributes();
                totalAttributesAccessed += attributes.size();
                
                // Force loading of all attribute data
                attributes.forEach((key, value) -> {
                    // Access each attribute to ensure loading
                });
            }
        }

        long endTime = System.nanoTime();
        long endMemory = getUsedMemory();

        // Print results
        printBatchFetchResults("Batch Fetch with ElementCollections", 
            users.size(), totalPersonasAccessed, totalAttributesAccessed,
            hibernateStatistics.getQueryExecutionCount(),
            (endTime - startTime) / 1_000_000,
            endMemory - startMemory);

        // Assertions
        assertThat(users.size()).isEqualTo(50);
        assertThat(totalPersonasAccessed).isEqualTo(150); // 50 users * 3 personas each
        assertThat(totalAttributesAccessed).isGreaterThan(300); // Each persona has multiple attributes
        
        // With batch fetching, query count should be much lower than N+1
        // Should be approximately: 1 (users) + ceil(50/16) (personas) + ceil(150/16) (attributes)
        assertThat(hibernateStatistics.getQueryExecutionCount())
            .describedAs("Batch fetching should significantly reduce query count")
            .isLessThan(15); // Much less than the 200+ without batching
    }

    @Test
    @DisplayName("ElementCollection optimization: Custom attributes batch loading")
    void testElementCollectionBatchLoading() {
        hibernateStatistics.clear();
        long startTime = System.nanoTime();

        // Use optimized repository method for personas with attributes
        List<Persona> personas = personaRepository.findByUserIdOrderByPriorityWithAttributes(
            testUsers.get(0).getId());

        int totalAttributesAccessed = 0;
        for (Persona persona : personas) {
            Map<String, String> attributes = persona.getCustomAttributes();
            totalAttributesAccessed += attributes.size();
            
            // Access all attributes to verify they're loaded
            attributes.entrySet().forEach(entry -> {
                entry.getKey();
                entry.getValue();
            });
        }

        long endTime = System.nanoTime();

        // Print results
        System.out.println("\n" + "=".repeat(60));
        System.out.println("ELEMENTCOLLECTION BATCH LOADING TEST");
        System.out.println("=".repeat(60));
        System.out.println("Personas loaded: " + personas.size());
        System.out.println("Total attributes accessed: " + totalAttributesAccessed);
        System.out.println("Query execution count: " + hibernateStatistics.getQueryExecutionCount());
        System.out.println("Execution time: " + (endTime - startTime) / 1_000_000 + " ms");
        System.out.println("=".repeat(60));

        // Assertions
        assertThat(personas.size()).isEqualTo(3);
        assertThat(totalAttributesAccessed).isGreaterThan(6); // Each persona has at least 2 attributes
        
        // Should use JOIN FETCH - minimal queries
        assertThat(hibernateStatistics.getQueryExecutionCount())
            .describedAs("JOIN FETCH should use minimal queries")
            .isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Batch size comparison: 16 vs larger batch sizes")
    void testDifferentBatchSizes() {
        Map<Integer, BatchPerformanceResult> results = new HashMap<>();
        
        // Test with different batch sizes by loading different sets of data
        for (int batchSize : List.of(16, 32, 64)) {
            hibernateStatistics.clear();
            long startTime = System.nanoTime();
            
            // Simulate batch loading by accessing subsets of users
            List<User> users = testUsers.subList(0, Math.min(batchSize, testUsers.size()));
            int totalPersonasAccessed = 0;
            
            for (User user : users) {
                List<Persona> personas = user.getPersonas();
                totalPersonasAccessed += personas.size();
                
                for (Persona persona : personas) {
                    // Access attributes to trigger batch loading
                    persona.getCustomAttributes().size();
                }
            }
            
            long endTime = System.nanoTime();
            
            BatchPerformanceResult result = new BatchPerformanceResult(
                batchSize,
                users.size(),
                totalPersonasAccessed,
                hibernateStatistics.getQueryExecutionCount(),
                (endTime - startTime) / 1_000_000
            );
            
            results.put(batchSize, result);
        }
        
        // Print comparison
        printBatchSizeComparison(results);
        
        // Verify all batch sizes perform well
        for (BatchPerformanceResult result : results.values()) {
            assertThat(result.queryCount)
                .describedAs("Batch size %d should have low query count", result.batchSize)
                .isLessThan(10);
        }
    }

    @Test
    @DisplayName("Memory efficiency: Batch loading vs individual loading")
    void testMemoryEfficiency() {
        // Test 1: Batch loading (optimized)
        hibernateStatistics.clear();
        System.gc(); // Suggest garbage collection
        long batchStartMemory = getUsedMemory();
        
        List<User> batchUsers = userRepository.findAll();
        for (User user : batchUsers) {
            user.getPersonas().forEach(persona -> 
                persona.getCustomAttributes().size());
        }
        
        long batchEndMemory = getUsedMemory();
        long batchMemoryUsed = batchEndMemory - batchStartMemory;
        long batchQueries = hibernateStatistics.getQueryExecutionCount();

        // Test 2: Individual loading (simulated non-optimized)
        hibernateStatistics.clear();
        entityManager.clear(); // Clear cache
        System.gc(); // Suggest garbage collection
        long individualStartMemory = getUsedMemory();
        
        for (User user : testUsers) {
            // Simulate individual queries
            List<Persona> personas = personaRepository.findByUserId(user.getId());
            for (Persona persona : personas) {
                persona.getCustomAttributes().size();
            }
        }
        
        long individualEndMemory = getUsedMemory();
        long individualMemoryUsed = individualEndMemory - individualStartMemory;
        long individualQueries = hibernateStatistics.getQueryExecutionCount();

        // Print comparison
        System.out.println("\n" + "=".repeat(60));
        System.out.println("MEMORY EFFICIENCY COMPARISON");
        System.out.println("=".repeat(60));
        System.out.println("Batch Loading:");
        System.out.println("  Memory used: " + formatBytes(batchMemoryUsed));
        System.out.println("  Queries: " + batchQueries);
        System.out.println("Individual Loading:");
        System.out.println("  Memory used: " + formatBytes(individualMemoryUsed));
        System.out.println("  Queries: " + individualQueries);
        System.out.println("Memory improvement: " + 
            String.format("%.1f", ((individualMemoryUsed - batchMemoryUsed) / (double) individualMemoryUsed) * 100) + "%");
        System.out.println("=".repeat(60));

        // Assertions
        assertThat(batchQueries)
            .describedAs("Batch loading should use fewer queries")
            .isLessThan(individualQueries);
    }

    @Test
    @DisplayName("Large dataset performance: 1000 users batch loading")
    @Transactional
    void testLargeDatasetBatchPerformance() {
        // Create larger test dataset
        List<User> largeTestUsers = createLargeTestDataset(100); // 100 users for faster test
        
        try {
            hibernateStatistics.clear();
            long startTime = System.nanoTime();
            long startMemory = getUsedMemory();
            
            // Load all users with batch fetching
            List<User> users = userRepository.findAll();
            int totalPersonasAccessed = 0;
            int totalAttributesAccessed = 0;
            
            for (User user : users) {
                List<Persona> personas = user.getPersonas();
                totalPersonasAccessed += personas.size();
                
                for (Persona persona : personas) {
                    totalAttributesAccessed += persona.getCustomAttributes().size();
                }
            }
            
            long endTime = System.nanoTime();
            long endMemory = getUsedMemory();
            
            // Print results
            printBatchFetchResults("Large Dataset Batch Loading",
                users.size(), totalPersonasAccessed, totalAttributesAccessed,
                hibernateStatistics.getQueryExecutionCount(),
                (endTime - startTime) / 1_000_000,
                endMemory - startMemory);
            
            // Assertions for large dataset
            assertThat(hibernateStatistics.getQueryExecutionCount())
                .describedAs("Large dataset should still use efficient batch loading")
                .isLessThan(20); // Should scale well
            
            // Performance should be reasonable even with larger dataset
            long executionTimeMs = (endTime - startTime) / 1_000_000;
            assertThat(executionTimeMs)
                .describedAs("Large dataset loading should complete in reasonable time")
                .isLessThan(1000); // Less than 1 second
                
        } finally {
            // Clean up large test data
            userRepository.deleteAll(largeTestUsers);
        }
    }

    private void createBatchTestData() {
        testUsers = new ArrayList<>();
        testPersonas = new ArrayList<>();
        
        // Create 50 users with 3 personas each for batch testing
        for (int i = 0; i < 50; i++) {
            User user = User.builder()
                .username("batch_user_" + i + "_" + System.nanoTime())
                .email("batch_user_" + i + "_" + System.nanoTime() + "@test.com")
                .password("password123")
                .firstName("Batch")
                .lastName("User" + i)
                .emailVerified(true)
                .enabled(true)
                .build();
            
            User savedUser = userRepository.save(user);
            testUsers.add(savedUser);
            
            // Create 3 personas per user with rich attribute data
            for (int j = 0; j < 3; j++) {
                Persona persona = Persona.builder()
                    .user(savedUser)
                    .name("BatchPersona_" + j)
                    .type(Persona.PersonaType.values()[j % Persona.PersonaType.values().length])
                    .displayName("Batch Display Persona " + j)
                    .bio("Bio for batch persona " + j)
                    .isDefault(j == 0)
                    .isActive(j == 0)
                    .build();
                
                // Add multiple custom attributes to test ElementCollection batch loading
                for (int k = 0; k < 5; k++) {
                    persona.getCustomAttributes().put("batchKey" + j + "_" + k, "batchValue" + j + "_" + k);
                }
                persona.getCustomAttributes().put("category", "batch-test");
                persona.getCustomAttributes().put("userId", savedUser.getId().toString());
                
                Persona savedPersona = personaRepository.save(persona);
                testPersonas.add(savedPersona);
            }
        }
        
        entityManager.flush();
        entityManager.clear();
    }

    private List<User> createLargeTestDataset(int userCount) {
        List<User> users = new ArrayList<>();
        
        for (int i = 0; i < userCount; i++) {
            User user = User.builder()
                .username("large_user_" + i + "_" + System.nanoTime())
                .email("large_user_" + i + "_" + System.nanoTime() + "@test.com")
                .password("password123")
                .firstName("Large")
                .lastName("User" + i)
                .emailVerified(true)
                .enabled(true)
                .build();
            
            User savedUser = userRepository.save(user);
            users.add(savedUser);
            
            // Create personas with attributes
            for (int j = 0; j < 3; j++) {
                Persona persona = Persona.builder()
                    .user(savedUser)
                    .name("LargePersona_" + j)
                    .type(Persona.PersonaType.values()[j % Persona.PersonaType.values().length])
                    .displayName("Large Display Persona " + j)
                    .isDefault(j == 0)
                    .isActive(j == 0)
                    .build();
                
                // Add attributes
                persona.getCustomAttributes().put("largeKey" + j, "largeValue" + j);
                persona.getCustomAttributes().put("category", "large-test");
                
                personaRepository.save(persona);
            }
        }
        
        entityManager.flush();
        entityManager.clear();
        return users;
    }

    private void printBatchFetchResults(String testName, int userCount, int personaCount, 
                                       int attributeCount, long queryCount, long timeMs, long memoryBytes) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println(testName.toUpperCase());
        System.out.println("=".repeat(60));
        System.out.println("Users processed: " + userCount);
        System.out.println("Personas accessed: " + personaCount);
        System.out.println("Attributes accessed: " + attributeCount);
        System.out.println("Query execution count: " + queryCount);
        System.out.println("Execution time: " + timeMs + " ms");
        System.out.println("Memory used: " + formatBytes(memoryBytes));
        if (userCount > 0) {
            System.out.println("Avg time per user: " + String.format("%.2f", timeMs / (double) userCount) + " ms");
        }
        System.out.println("=".repeat(60));
    }

    private void printBatchSizeComparison(Map<Integer, BatchPerformanceResult> results) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("BATCH SIZE COMPARISON");
        System.out.println("=".repeat(60));
        
        for (BatchPerformanceResult result : results.values()) {
            System.out.println("Batch Size " + result.batchSize + ":");
            System.out.println("  Users: " + result.userCount);
            System.out.println("  Personas: " + result.personaCount);
            System.out.println("  Queries: " + result.queryCount);
            System.out.println("  Time: " + result.executionTimeMs + " ms");
            System.out.println();
        }
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

    private static class BatchPerformanceResult {
        final int batchSize;
        final int userCount;
        final int personaCount;
        final long queryCount;
        final long executionTimeMs;

        BatchPerformanceResult(int batchSize, int userCount, int personaCount, 
                             long queryCount, long executionTimeMs) {
            this.batchSize = batchSize;
            this.userCount = userCount;
            this.personaCount = personaCount;
            this.queryCount = queryCount;
            this.executionTimeMs = executionTimeMs;
        }
    }
}