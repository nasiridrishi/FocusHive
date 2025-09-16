package com.focushive.buddy.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.web.server.LocalServerPort;
import com.focushive.buddy.repository.BuddyPartnershipRepository;
import com.focushive.buddy.repository.BuddyGoalRepository;
import com.focushive.buddy.repository.BuddyCheckinRepository;
import org.springframework.data.redis.core.RedisTemplate;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Production Integration Test
 * Verifies all components work correctly in production-like environment
 * Tests internal-only database connectivity
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Production Integration Tests")
public class ProductionIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private BuddyPartnershipRepository partnershipRepository;

    @Autowired
    private BuddyGoalRepository goalRepository;

    @Autowired
    private BuddyCheckinRepository checkinRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    @Test
    @DisplayName("Health endpoint should return UP status")
    void testHealthEndpoint() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            baseUrl + "/api/v1/health",
            Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
        assertThat(response.getBody().get("service")).isEqualTo("buddy-service");
    }

    @Test
    @DisplayName("Database connectivity should work internally")
    void testDatabaseConnectivity() throws Exception {
        // Test database connection
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection).isNotNull();
            assertThat(connection.isValid(1)).isTrue();

            // Verify database name
            String catalog = connection.getCatalog();
            assertThat(catalog).contains("buddy");
        }

        // Test repository access
        long partnershipCount = partnershipRepository.count();
        assertThat(partnershipCount).isGreaterThanOrEqualTo(0);

        long goalCount = goalRepository.count();
        assertThat(goalCount).isGreaterThanOrEqualTo(0);

        long checkinCount = checkinRepository.count();
        assertThat(checkinCount).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Redis connectivity should work internally")
    void testRedisConnectivity() {
        // Test Redis connection
        String testKey = "test:production:key";
        String testValue = "test-value";

        // Set value
        redisTemplate.opsForValue().set(testKey, testValue);

        // Get value
        Object retrievedValue = redisTemplate.opsForValue().get(testKey);
        assertThat(retrievedValue).isEqualTo(testValue);

        // Clean up
        redisTemplate.delete(testKey);

        // Verify deletion
        Object deletedValue = redisTemplate.opsForValue().get(testKey);
        assertThat(deletedValue).isNull();
    }

    @Test
    @DisplayName("Health check should report all components as UP")
    void testComponentHealth() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            baseUrl + "/api/v1/health",
            Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        Map<String, String> components = (Map<String, String>) response.getBody().get("components");
        assertThat(components).isNotNull();
        assertThat(components.get("database")).isEqualTo("UP");
        assertThat(components.get("matching")).isEqualTo("UP");
    }

    @Test
    @DisplayName("Swagger UI should be accessible")
    void testSwaggerUI() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/swagger-ui/index.html",
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("swagger");
    }

    @Test
    @DisplayName("API documentation should be available")
    void testApiDocumentation() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            baseUrl + "/v3/api-docs",
            Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("openapi")).isNotNull();
        assertThat(response.getBody().get("paths")).isNotNull();
    }

    @Test
    @DisplayName("Unauthorized requests should return 401")
    void testUnauthorizedAccess() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            baseUrl + "/api/v1/buddy/matching/suggestions",
            Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Database tables should exist")
    void testDatabaseSchema() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            var metaData = connection.getMetaData();
            var tables = metaData.getTables(null, "public", "%", new String[]{"TABLE"});

            int tableCount = 0;
            while (tables.next()) {
                tableCount++;
            }

            // Should have at least 8 tables as per schema
            assertThat(tableCount).isGreaterThanOrEqualTo(8);
        }
    }
}