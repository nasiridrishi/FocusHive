package com.focushive.identity.performance;

import com.focushive.identity.config.QueryCountTestConfiguration;
import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.User;
import com.focushive.identity.repository.PersonaRepository;
import com.focushive.identity.repository.UserRepository;
import com.focushive.identity.service.PersonaService;
import net.ttddyy.dsproxy.QueryCount;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance tests to measure N+1 query problems in User-Persona relationships.
 * This test class identifies the critical performance issue where loading users
 * with their personas causes 100+ database queries instead of an optimal number.
 * 
 * Current Issue:
 * - Loading 20 users with 5 personas each = 20 * 5 = 100+ queries
 * - Target: Reduce to <5 queries total using JOIN FETCH and @EntityGraph
 */
@SpringBootTest
@Import(QueryCountTestConfiguration.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.jpa.show-sql=true",
    "logging.level.org.hibernate.SQL=DEBUG",
    "logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE"
})
@DisplayName("User-Persona N+1 Query Performance Tests")
class UserPersonaN1QueryPerformanceTest {

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
        // Clear query count holder
        QueryCountHolder.clear();
        
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
        
        // Clear query count after setup
        QueryCountHolder.clear();
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
        QueryCountHolder.clear();
    }

    @Test
    @DisplayName("Measure N+1 Query Problem: Load all users and access their personas")
    @Transactional
    void measureN1QueryProblem_LoadUsersWithPersonas() {
        // Arrange
        QueryCountHolder.clear();
        long startTime = System.currentTimeMillis();

        // Act - This should demonstrate the N+1 problem
        List<User> users = userRepository.findAll();
        int totalPersonasAccessed = 0;
        
        for (User user : users) {
            // This triggers lazy loading - causes N+1 queries
            List<Persona> personas = user.getPersonas();
            totalPersonasAccessed += personas.size();
            
            // Access persona properties to trigger additional queries
            for (Persona persona : personas) {
                persona.getName(); // Force loading
                persona.getDisplayName();
                persona.getPrivacySettings(); // This might trigger additional queries
            }
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Assert - Capture the performance metrics
        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        long selectQueries = queryCount.getSelect();
        long totalQueries = queryCount.getTotal();

        System.out.println("=== N+1 QUERY PROBLEM BASELINE METRICS ===");
        System.out.println("Users loaded: " + users.size());
        System.out.println("Total personas accessed: " + totalPersonasAccessed);
        System.out.println("Total SELECT queries: " + selectQueries);
        System.out.println("Total queries (all types): " + totalQueries);
        System.out.println("Execution time: " + executionTime + "ms");
        System.out.println("Average queries per user: " + (totalQueries / (double) users.size()));
        System.out.println("============================================");

        // Current performance should be poor (demonstrates the problem)
        // We expect many queries (likely 100+) due to N+1 problem
        assertThat(users.size()).isEqualTo(20);
        assertThat(totalPersonasAccessed).isEqualTo(100); // 20 users * 5 personas each
        
        // This assertion documents the current poor performance
        // After fixing N+1 queries, these numbers should be much lower
        assertThat(selectQueries).isGreaterThan(20); // Should be much more than 20 due to N+1
        assertThat(executionTime).isGreaterThan(0);
        
        // Store metrics for comparison after optimization
        System.setProperty("baseline.selectQueries", String.valueOf(selectQueries));
        System.setProperty("baseline.totalQueries", String.valueOf(totalQueries));
        System.setProperty("baseline.executionTime", String.valueOf(executionTime));
    }

    @Test
    @DisplayName("Measure N+1 Query Problem: PersonaService.getUserPersonas()")
    @Transactional
    void measureN1QueryProblem_GetUserPersonas() {
        // Arrange
        QueryCountHolder.clear();
        long startTime = System.currentTimeMillis();

        // Act - Test the PersonaService method that likely has N+1 issues
        int totalPersonasRetrieved = 0;
        for (User user : testUsers) {
            var personas = personaService.getUserPersonas(user.getId());
            totalPersonasRetrieved += personas.size();
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Assert - Capture the performance metrics
        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        long selectQueries = queryCount.getSelect();
        long totalQueries = queryCount.getTotal();

        System.out.println("=== PersonaService.getUserPersonas() N+1 BASELINE ===");
        System.out.println("Users processed: " + testUsers.size());
        System.out.println("Total personas retrieved: " + totalPersonasRetrieved);
        System.out.println("Total SELECT queries: " + selectQueries);
        System.out.println("Total queries (all types): " + totalQueries);
        System.out.println("Execution time: " + executionTime + "ms");
        System.out.println("Average queries per user: " + (totalQueries / (double) testUsers.size()));
        System.out.println("===================================================");

        // Performance should be poor due to N+1
        assertThat(totalPersonasRetrieved).isEqualTo(100); // 20 users * 5 personas each
        assertThat(selectQueries).isGreaterThan(20); // Much more due to N+1 problem
        
        // Store metrics for comparison
        System.setProperty("personaService.baseline.selectQueries", String.valueOf(selectQueries));
        System.setProperty("personaService.baseline.executionTime", String.valueOf(executionTime));
    }

    @Test
    @DisplayName("Measure Query Count: Load single user with personas")
    @Transactional
    void measureSingleUserPersonaQueries() {
        // Arrange
        QueryCountHolder.clear();
        UUID userId = testUsers.get(0).getId();
        
        // Act
        User user = userRepository.findById(userId).orElseThrow();
        List<Persona> personas = user.getPersonas();
        
        // Force loading all persona data
        for (Persona persona : personas) {
            persona.getName();
            persona.getPrivacySettings();
            persona.getCustomAttributes().size(); // Force ElementCollection loading
        }

        // Assert
        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        long selectQueries = queryCount.getSelect();
        
        System.out.println("=== SINGLE USER PERSONA QUERIES ===");
        System.out.println("User loaded: " + user.getUsername());
        System.out.println("Personas count: " + personas.size());
        System.out.println("SELECT queries: " + selectQueries);
        System.out.println("===================================");

        // For a single user with 5 personas, we should ideally need only 1-2 queries
        // But due to N+1 problem, we'll likely see many more
        assertThat(personas.size()).isEqualTo(5);
        assertThat(selectQueries).isGreaterThan(1); // Documents the current inefficiency
    }

    @Test
    @DisplayName("Performance Benchmark: Response time threshold test")
    void performanceThresholdTest() {
        // This test establishes our performance expectations
        long startTime = System.currentTimeMillis();
        
        // Load multiple users and their personas (simulating API calls)
        for (int i = 0; i < 10; i++) {
            UUID userId = testUsers.get(i).getId();
            personaService.getUserPersonas(userId);
        }
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        System.out.println("=== PERFORMANCE THRESHOLD TEST ===");
        System.out.println("Time to load 10 users with personas: " + executionTime + "ms");
        System.out.println("Average time per user: " + (executionTime / 10.0) + "ms");
        System.out.println("==================================");
        
        // Current performance is likely poor (>50ms per user)
        // After optimization, should be <10ms per user
        assertThat(executionTime).isGreaterThan(0);
        
        // Document baseline for improvement measurement
        System.setProperty("performance.baseline.timeFor10Users", String.valueOf(executionTime));
    }

    /**
     * Helper method to create realistic test data with varied persona types
     */
    private void createRealisticTestData() {
        // This method can be used to create more realistic test scenarios
        // with different persona configurations, privacy settings, etc.
        // to better simulate production workloads
    }
}