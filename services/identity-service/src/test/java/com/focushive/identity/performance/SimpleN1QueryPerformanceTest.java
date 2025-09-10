package com.focushive.identity.performance;

import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.User;
import com.focushive.identity.repository.PersonaRepository;
import com.focushive.identity.repository.UserRepository;
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
 * Simplified performance tests to measure N+1 query problems in User-Persona relationships.
 * This test measures response times and establishes performance baselines before optimization.
 * 
 * Current Issue:
 * - Loading users with their personas likely causes N+1 queries
 * - Target: Improve response times by 70%+
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.jpa.show-sql=true",
    "logging.level.org.hibernate.SQL=DEBUG"
})
@DisplayName("Simple N+1 Query Performance Tests")
class SimpleN1QueryPerformanceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private PersonaService personaService;

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
    @DisplayName("Baseline: Load all users and access their personas (shows N+1 problem)")
    @Transactional
    void baselinePerformance_LoadUsersWithPersonas() {
        // Arrange
        long startTime = System.currentTimeMillis();

        // Act - This should demonstrate the N+1 problem
        List<User> users = userRepository.findAll();
        int totalPersonasAccessed = 0;
        
        for (User user : users) {
            // This triggers lazy loading - causes N+1 queries
            List<Persona> personas = user.getPersonas();
            totalPersonasAccessed += personas.size();
            
            // Access persona properties to fully trigger loading
            for (Persona persona : personas) {
                persona.getName();
                persona.getDisplayName();
                persona.getPrivacySettings();
            }
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Assert - Capture the performance metrics
        System.out.println("=== N+1 QUERY PROBLEM BASELINE METRICS ===");
        System.out.println("Users loaded: " + users.size());
        System.out.println("Total personas accessed: " + totalPersonasAccessed);
        System.out.println("Execution time: " + executionTime + "ms");
        System.out.println("Average time per user: " + (executionTime / (double) users.size()) + "ms");
        System.out.println("============================================");

        // Document baseline for comparison
        assertThat(users.size()).isEqualTo(20);
        assertThat(totalPersonasAccessed).isEqualTo(100); // 20 users * 5 personas each
        assertThat(executionTime).isGreaterThan(0);
        
        // Store baseline metrics in system properties for comparison after optimization
        System.setProperty("baseline.executionTime", String.valueOf(executionTime));
        System.setProperty("baseline.avgTimePerUser", String.valueOf(executionTime / (double) users.size()));
    }

    @Test
    @DisplayName("Baseline: PersonaService.getUserPersonas() performance")
    @Transactional
    void baselinePerformance_GetUserPersonas() {
        // Arrange
        long startTime = System.currentTimeMillis();

        // Act - Test the PersonaService method
        int totalPersonasRetrieved = 0;
        for (User user : testUsers) {
            var personas = personaService.getUserPersonas(user.getId());
            totalPersonasRetrieved += personas.size();
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Assert - Capture the performance metrics
        System.out.println("=== PersonaService.getUserPersonas() BASELINE ===");
        System.out.println("Users processed: " + testUsers.size());
        System.out.println("Total personas retrieved: " + totalPersonasRetrieved);
        System.out.println("Execution time: " + executionTime + "ms");
        System.out.println("Average time per user: " + (executionTime / (double) testUsers.size()) + "ms");
        System.out.println("================================================");

        // Performance baseline
        assertThat(totalPersonasRetrieved).isEqualTo(100); // 20 users * 5 personas each
        assertThat(executionTime).isGreaterThan(0);
        
        // Store baseline for comparison
        System.setProperty("personaService.baseline.executionTime", String.valueOf(executionTime));
        System.setProperty("personaService.baseline.avgTimePerUser", String.valueOf(executionTime / (double) testUsers.size()));
    }

    @Test
    @DisplayName("Baseline: Single user persona loading performance")
    void baselinePerformance_SingleUser() {
        // Arrange
        UUID userId = testUsers.get(0).getId();
        
        // Act
        long startTime = System.currentTimeMillis();
        
        User user = userRepository.findById(userId).orElseThrow();
        List<Persona> personas = user.getPersonas();
        
        // Force loading all persona data
        for (Persona persona : personas) {
            persona.getName();
            persona.getPrivacySettings();
            persona.getCustomAttributes().size();
        }
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Assert
        System.out.println("=== SINGLE USER PERSONA LOADING BASELINE ===");
        System.out.println("User loaded: " + user.getUsername());
        System.out.println("Personas count: " + personas.size());
        System.out.println("Execution time: " + executionTime + "ms");
        System.out.println("=============================================");

        // For a single user with 5 personas, should be fast but may show N+1 inefficiency
        assertThat(personas.size()).isEqualTo(5);
        assertThat(executionTime).isGreaterThan(0);
        
        System.setProperty("singleUser.baseline.executionTime", String.valueOf(executionTime));
    }

    @Test
    @DisplayName("Performance threshold test - API response time simulation")
    void performanceThresholdTest() {
        // Simulate API calls - load personas for multiple users
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 10; i++) {
            UUID userId = testUsers.get(i).getId();
            personaService.getUserPersonas(userId);
        }
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        double avgTimePerUser = executionTime / 10.0;
        
        System.out.println("=== API PERFORMANCE THRESHOLD TEST ===");
        System.out.println("Time to process 10 API calls: " + executionTime + "ms");
        System.out.println("Average time per API call: " + avgTimePerUser + "ms");
        
        // Current performance likely exceeds acceptable thresholds
        // Target after optimization: <10ms per API call
        if (avgTimePerUser > 50) {
            System.out.println("WARNING: Performance exceeds 50ms per API call - optimization needed!");
        }
        
        System.out.println("=======================================");
        
        assertThat(executionTime).isGreaterThan(0);
        System.setProperty("api.baseline.avgTimePerCall", String.valueOf(avgTimePerUser));
    }
}