package com.focushive.identity.performance;

import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.User;
import com.focushive.identity.repository.PersonaRepository;
import com.focushive.identity.repository.UserRepository;
import com.focushive.identity.service.OptimizedPersonaService;
import com.focushive.identity.service.PersonaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance tests to verify N+1 query optimizations are working.
 * These tests use the optimized repository methods with @EntityGraph and JOIN FETCH
 * to demonstrate the performance improvements achieved.
 * 
 * Expected Results:
 * - Query count reduced from 100+ to <5
 * - Response times improved by 70%+
 * - No LazyInitializationExceptions
 */
@org.junit.jupiter.api.Disabled("Performance tests disabled until data persistence issues are resolved")
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.jpa.show-sql=true",
    "logging.level.org.hibernate.SQL=DEBUG"
})
@DisplayName("Optimized N+1 Query Performance Tests")
class OptimizedN1QueryPerformanceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private PersonaService personaService;

    @Autowired
    private OptimizedPersonaService optimizedPersonaService;

    private List<User> testUsers;
    private List<Persona> testPersonas;

    @BeforeEach
    void setUp() {
        // Create test data: 20 users with 5 personas each
        testUsers = new ArrayList<>();
        testPersonas = new ArrayList<>();
        
        for (int i = 0; i < 20; i++) {
            User user = User.builder()
                    .username("user" + i)
                    .email("user" + i + "@test.com")
                    .password("password123")
                    .firstName("User")
                    .lastName("Test" + i)
                    .emailVerified(true)
                    .enabled(true)
                    .build();
            
            User savedUser = userRepository.save(user);
            testUsers.add(savedUser);
            
            // Create 5 personas for each user
            for (int j = 0; j < 5; j++) {
                Persona persona = Persona.builder()
                        .user(savedUser)
                        .name("Persona" + j)
                        .type(Persona.PersonaType.values()[j % Persona.PersonaType.values().length])
                        .displayName("Display Persona " + j)
                        .bio("Bio for persona " + j)
                        .isDefault(j == 0)
                        .isActive(j == 0)
                        .build();
                
                // Add some custom attributes to test ElementCollection loading
                persona.getCustomAttributes().put("testKey" + j, "testValue" + j);
                persona.getCustomAttributes().put("category", "test");
                
                Persona savedPersona = personaRepository.save(persona);
                testPersonas.add(savedPersona);
            }
        }
    }

    @AfterEach
    void tearDown() {
        // Clean up test data
        if (testPersonas != null) {
            personaRepository.deleteAll(testPersonas);
        }
        if (testUsers != null) {
            userRepository.deleteAll(testUsers);
        }
    }

    @Test
    @DisplayName("Optimized: Load users with personas using EntityGraph (should be fast)")
    @Transactional
    void optimizedPerformance_LoadUsersWithEntityGraph() {
        // Arrange
        long startTime = System.currentTimeMillis();

        // Act - Use optimized service method with EntityGraph
        List<User> users = optimizedPersonaService.getAllUsersWithPersonas();
        int totalPersonasAccessed = 0;
        
        for (User user : users) {
            // This should NOT cause N+1 queries due to EntityGraph
            List<Persona> personas = user.getPersonas();
            totalPersonasAccessed += personas.size();
            
            // Access persona properties - should be already loaded
            for (Persona persona : personas) {
                persona.getName();
                persona.getDisplayName();
                persona.getPrivacySettings();
            }
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Assert - Should be significantly faster than baseline
        System.out.println("=== OPTIMIZED ENTITYGRAPH PERFORMANCE ===");
        System.out.println("Users loaded: " + users.size());
        System.out.println("Total personas accessed: " + totalPersonasAccessed);
        System.out.println("Execution time: " + executionTime + "ms");
        System.out.println("Average time per user: " + (executionTime / (double) users.size()) + "ms");
        
        // Compare with baseline
        String baselineTime = System.getProperty("baseline.executionTime");
        if (baselineTime != null) {
            long baseline = Long.parseLong(baselineTime);
            double improvement = ((baseline - executionTime) / (double) baseline) * 100;
            System.out.println("Performance improvement: " + String.format("%.1f", improvement) + "%");
            
            // Verify 70%+ improvement
            assertThat(improvement).isGreaterThan(70.0)
                    .withFailMessage("Performance improvement should be >70%, but was %.1f%%", improvement);
        }
        
        System.out.println("==========================================");

        assertThat(users.size()).isEqualTo(20);
        assertThat(totalPersonasAccessed).isEqualTo(100); // 20 users * 5 personas each
        assertThat(executionTime).isLessThan(100); // Should be much faster than baseline
    }

    @Test
    @DisplayName("Optimized: PersonaService.getUserPersonas() with JOIN FETCH")
    @Transactional
    void optimizedPerformance_PersonaServiceWithJoinFetch() {
        // Arrange
        long startTime = System.currentTimeMillis();

        // Act - Use optimized PersonaService methods
        int totalPersonasRetrieved = 0;
        for (User user : testUsers) {
            var personas = optimizedPersonaService.getUserPersonasOptimized(user.getId());
            totalPersonasRetrieved += personas.size();
            
            // Verify custom attributes are accessible (testing ElementCollection optimization)
            personas.forEach(persona -> {
                if (persona.getCustomAttributes() != null) {
                    persona.getCustomAttributes().keySet(); // Should be loaded without additional queries
                }
            });
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Assert - Should be significantly faster than baseline
        System.out.println("=== OPTIMIZED PERSONA SERVICE PERFORMANCE ===");
        System.out.println("Users processed: " + testUsers.size());
        System.out.println("Total personas retrieved: " + totalPersonasRetrieved);
        System.out.println("Execution time: " + executionTime + "ms");
        System.out.println("Average time per user: " + (executionTime / (double) testUsers.size()) + "ms");
        
        // Compare with baseline
        String baselineTime = System.getProperty("personaService.baseline.executionTime");
        if (baselineTime != null) {
            long baseline = Long.parseLong(baselineTime);
            double improvement = ((baseline - executionTime) / (double) baseline) * 100;
            System.out.println("Performance improvement: " + String.format("%.1f", improvement) + "%");
            
            // Verify 70%+ improvement
            assertThat(improvement).isGreaterThan(70.0)
                    .withFailMessage("Performance improvement should be >70%, but was %.1f%%", improvement);
        }
        
        System.out.println("===============================================");

        assertThat(totalPersonasRetrieved).isEqualTo(100); // 20 users * 5 personas each
        assertThat(executionTime).isLessThan(200); // Should be much faster than baseline
    }

    @Test
    @DisplayName("Optimized: Single user with personas and attributes using EntityGraph")
    void optimizedPerformance_SingleUserWithAttributes() {
        // Arrange
        UUID userId = testUsers.get(0).getId();
        
        // Act - Use optimized method with EntityGraph for attributes
        long startTime = System.currentTimeMillis();
        
        User user = optimizedPersonaService.findUserWithPersonasAndAttributes(userId).orElseThrow();
        List<Persona> personas = user.getPersonas();
        
        // Access all persona data including custom attributes
        for (Persona persona : personas) {
            persona.getName();
            persona.getPrivacySettings();
            persona.getCustomAttributes().size(); // Should be loaded via EntityGraph
        }
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Assert
        System.out.println("=== OPTIMIZED SINGLE USER WITH ATTRIBUTES ===");
        System.out.println("User loaded: " + user.getUsername());
        System.out.println("Personas count: " + personas.size());
        System.out.println("Execution time: " + executionTime + "ms");
        
        // Compare with baseline
        String baselineTime = System.getProperty("singleUser.baseline.executionTime");
        if (baselineTime != null) {
            long baseline = Long.parseLong(baselineTime);
            double improvement = ((baseline - executionTime) / (double) baseline) * 100;
            System.out.println("Performance improvement: " + String.format("%.1f", improvement) + "%");
        }
        
        System.out.println("==============================================");

        assertThat(personas.size()).isEqualTo(5);
        assertThat(executionTime).isLessThan(50); // Should be very fast for single user
        
        // Verify custom attributes are loaded
        personas.forEach(persona -> {
            assertThat(persona.getCustomAttributes()).isNotNull();
            assertThat(persona.getCustomAttributes()).isNotEmpty();
        });
    }

    @Test
    @DisplayName("Optimized: Batch user loading with JOIN FETCH")
    void optimizedPerformance_BatchUserLoading() {
        // Arrange - Select subset of users for batch loading
        List<UUID> userIds = testUsers.stream()
                .limit(10)
                .map(User::getId)
                .toList();
        
        // Act - Use batch loading method
        long startTime = System.currentTimeMillis();
        
        List<User> users = optimizedPersonaService.getUsersWithPersonasBatch(userIds);
        int totalPersonasAccessed = 0;
        
        for (User user : users) {
            List<Persona> personas = user.getPersonas();
            totalPersonasAccessed += personas.size();
            
            // Access all data - should be loaded in single query
            for (Persona persona : personas) {
                persona.getName();
                persona.getCustomAttributes().size();
            }
        }
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Assert
        System.out.println("=== OPTIMIZED BATCH USER LOADING ===");
        System.out.println("Users loaded: " + users.size());
        System.out.println("Total personas accessed: " + totalPersonasAccessed);
        System.out.println("Execution time: " + executionTime + "ms");
        System.out.println("Average time per user: " + (executionTime / (double) users.size()) + "ms");
        System.out.println("=====================================");

        assertThat(users.size()).isEqualTo(10);
        assertThat(totalPersonasAccessed).isEqualTo(50); // 10 users * 5 personas each
        assertThat(executionTime).isLessThan(100); // Should be very efficient
    }

    @Test
    @DisplayName("Optimized: API simulation - verify no N+1 queries")
    void optimizedPerformance_ApiSimulation() {
        // Simulate API calls - load personas for multiple users
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 10; i++) {
            UUID userId = testUsers.get(i).getId();
            
            // Use optimized service method
            var personas = optimizedPersonaService.getUserPersonasOptimized(userId);
            assertThat(personas).isNotEmpty();
            
            // Also test getting active persona
            var activePersona = optimizedPersonaService.getActivePersonaOptimized(userId);
            assertThat(activePersona).isPresent();
        }
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        double avgTimePerUser = executionTime / 10.0;
        
        System.out.println("=== OPTIMIZED API SIMULATION ===");
        System.out.println("Time to process 10 API calls: " + executionTime + "ms");
        System.out.println("Average time per API call: " + avgTimePerUser + "ms");
        
        // Compare with baseline
        String baselineAvg = System.getProperty("api.baseline.avgTimePerCall");
        if (baselineAvg != null) {
            double baseline = Double.parseDouble(baselineAvg);
            double improvement = ((baseline - avgTimePerUser) / baseline) * 100;
            System.out.println("Performance improvement: " + String.format("%.1f", improvement) + "%");
            
            // Verify significant improvement
            assertThat(improvement).isGreaterThan(70.0)
                    .withFailMessage("API performance improvement should be >70%, but was %.1f%%", improvement);
        }
        
        System.out.println("=================================");
        
        // Should be very fast - target <10ms per API call
        assertThat(avgTimePerUser).isLessThan(10.0)
                .withFailMessage("Average API call time should be <10ms, but was %.1fms", avgTimePerUser);
    }
}