package com.focushive.identity.controller;

import com.focushive.identity.entity.User;
import com.focushive.identity.entity.Persona;
import com.focushive.identity.repository.UserRepository;
import com.focushive.identity.repository.PersonaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.*;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/performance-test")
public class PerformanceTestController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PersonaRepository personaRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @PostMapping("/setup-test-data")
    @Transactional
    public ResponseEntity<Map<String, Object>> setupTestData(@RequestParam(defaultValue = "10") int userCount) {
        Map<String, Object> result = new HashMap<>();
        
        // Clean existing test data
        personaRepository.deleteAll();
        userRepository.deleteAll();
        
        List<User> users = new ArrayList<>();
        int totalPersonas = 0;
        
        // Create test users with personas
        for (int i = 0; i < userCount; i++) {
            User user = new User();
            user.setUsername("testuser" + i);
            user.setEmail("testuser" + i + "@test.com");
            user.setPassword("hashedpassword");
            user.setFirstName("Test");
            user.setLastName("User" + i);
            user.setCreatedAt(Instant.now());
            user = userRepository.save(user);
            
            // Create 3 personas per user
            for (int j = 0; j < 3; j++) {
                Persona persona = new Persona();
                persona.setUser(user);
                persona.setName("Persona" + j);
                persona.setBio("Test persona " + j + " for user " + i);
                persona.setType(Persona.PersonaType.WORK);
                persona.setActive(j == 0); // First persona is active
                persona.setDefault(j == 0); // First persona is default
                persona.setCreatedAt(Instant.now());
                
                // Add some attributes
                Map<String, String> attributes = new HashMap<>();
                attributes.put("theme", "theme" + j);
                attributes.put("language", "en");
                attributes.put("priority", String.valueOf(j));
                persona.setCustomAttributes(attributes);
                
                personaRepository.save(persona);
                totalPersonas++;
            }
            
            users.add(user);
        }
        
        result.put("usersCreated", userCount);
        result.put("personasCreated", totalPersonas);
        result.put("message", "Test data created successfully");
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/test-optimized")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> testOptimizedQueries() {
        Map<String, Object> result = new HashMap<>();
        
        // Enable Hibernate statistics
        Session session = entityManager.unwrap(Session.class);
        SessionFactory sessionFactory = session.getSessionFactory();
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        
        long startTime = System.currentTimeMillis();
        
        // Optimized query using EntityGraph
        List<User> users = userRepository.findAllWithPersonas();
        
        // Access personas to trigger loading
        int personaCount = 0;
        for (User user : users) {
            personaCount += user.getPersonas().size();
            // Access attributes to ensure they're loaded
            user.getPersonas().forEach(p -> p.getCustomAttributes().size());
        }
        
        long endTime = System.currentTimeMillis();
        
        // Collect statistics
        result.put("queryCount", stats.getQueryExecutionCount());
        result.put("executionTime", endTime - startTime + "ms");
        result.put("usersLoaded", users.size());
        result.put("personasLoaded", personaCount);
        result.put("queriesExecuted", stats.getQueries());
        result.put("entityLoadCount", stats.getEntityLoadCount());
        result.put("collectionLoadCount", stats.getCollectionLoadCount());
        
        stats.setStatisticsEnabled(false);
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/test-non-optimized")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> testNonOptimizedQueries() {
        Map<String, Object> result = new HashMap<>();
        
        // Enable Hibernate statistics
        Session session = entityManager.unwrap(Session.class);
        SessionFactory sessionFactory = session.getSessionFactory();
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        
        long startTime = System.currentTimeMillis();
        
        // Non-optimized query - basic findAll
        List<User> users = userRepository.findAll();
        
        // This will trigger N+1 queries
        int personaCount = 0;
        for (User user : users) {
            personaCount += user.getPersonas().size(); // Each access triggers a query
            // Access attributes - another N queries
            user.getPersonas().forEach(p -> p.getCustomAttributes().size());
        }
        
        long endTime = System.currentTimeMillis();
        
        // Collect statistics
        result.put("queryCount", stats.getQueryExecutionCount());
        result.put("executionTime", endTime - startTime + "ms");
        result.put("usersLoaded", users.size());
        result.put("personasLoaded", personaCount);
        result.put("queriesExecuted", stats.getQueries());
        result.put("entityLoadCount", stats.getEntityLoadCount());
        result.put("collectionLoadCount", stats.getCollectionLoadCount());
        
        stats.setStatisticsEnabled(false);
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/compare")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> comparePerformance() {
        Map<String, Object> result = new HashMap<>();
        
        // Test optimized version
        Map<String, Object> optimized = testOptimizedQueries().getBody();
        
        // Clear cache to ensure fair comparison
        entityManager.clear();
        
        // Test non-optimized version  
        Map<String, Object> nonOptimized = testNonOptimizedQueries().getBody();
        
        // Calculate improvements
        long optimizedQueries = (long) optimized.get("queryCount");
        long nonOptimizedQueries = (long) nonOptimized.get("queryCount");
        
        String optimizedTime = (String) optimized.get("executionTime");
        String nonOptimizedTime = (String) nonOptimized.get("executionTime");
        
        long optimizedMs = Long.parseLong(optimizedTime.replace("ms", ""));
        long nonOptimizedMs = Long.parseLong(nonOptimizedTime.replace("ms", ""));
        
        double queryReduction = ((double)(nonOptimizedQueries - optimizedQueries) / nonOptimizedQueries) * 100;
        double timeImprovement = ((double)(nonOptimizedMs - optimizedMs) / nonOptimizedMs) * 100;
        
        result.put("optimized", optimized);
        result.put("nonOptimized", nonOptimized);
        result.put("queryReductionPercent", String.format("%.2f%%", queryReduction));
        result.put("timeImprovementPercent", String.format("%.2f%%", timeImprovement));
        result.put("summary", String.format(
            "Optimizations reduced queries from %d to %d (%.2f%% reduction) and improved execution time from %dms to %dms (%.2f%% improvement)",
            nonOptimizedQueries, optimizedQueries, queryReduction,
            nonOptimizedMs, optimizedMs, timeImprovement
        ));
        
        return ResponseEntity.ok(result);
    }
}