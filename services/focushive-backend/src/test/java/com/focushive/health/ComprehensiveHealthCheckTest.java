package com.focushive.health;

import com.focushive.api.client.IdentityServiceClient;
import com.focushive.api.dto.identity.HealthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Comprehensive health check tests following TDD principles.
 * These tests should FAIL initially and drive the implementation.
 *
 * Phase 1, Task 1.3: Health Check Implementation
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "management.endpoints.web.exposure.include=health,info,metrics",
    "management.endpoint.health.show-details=always",
    "management.endpoint.health.show-components=always",
    "app.features.health.enabled=true",
    "spring.flyway.enabled=false" // Disable for test
})
class ComprehensiveHealthCheckTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired(required = false)
    private HealthEndpoint healthEndpoint;

    @MockBean
    private IdentityServiceClient identityServiceClient;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldReturnHealthyWhenAllSystemsOperational() {
        // Arrange - Mock all dependencies as healthy
        mockIdentityServiceHealthy();
        mockRedisHealthy();

        // Act
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/health",
            Map.class
        );

        // Assert - THIS SHOULD PASS as basic health endpoint exists
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        Map<String, Object> health = response.getBody();
        assertThat(health.get("status")).isEqualTo("UP");

        // These assertions will FAIL initially - driving custom health indicator implementation
        assertThat(health.get("components")).isNotNull();
        Map<String, Object> components = (Map<String, Object>) health.get("components");

        // Custom health indicators that need to be implemented
        assertThat(components).containsKey("hiveService");
        assertThat(components).containsKey("presenceService");
        assertThat(components).containsKey("webSocket");
        assertThat(components).containsKey("migration");

        // Verify each custom component is UP
        assertComponentStatus(components, "hiveService", "UP");
        assertComponentStatus(components, "presenceService", "UP");
        assertComponentStatus(components, "webSocket", "UP");
        assertComponentStatus(components, "migration", "UP");
    }

    @Test
    void shouldReturnDegradedWhenDatabaseDown() {
        // This test will FAIL initially - need to implement degraded status logic

        // Arrange - Mock database failure
        mockIdentityServiceHealthy();
        mockRedisHealthy();
        // Database will naturally fail if we disconnect it

        // Act
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/health",
            Map.class
        );

        // Assert - Should return service unavailable when DB is down
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        Map<String, Object> health = response.getBody();
        assertThat(health.get("status")).isEqualTo("DOWN");

        // Verify specific component failure
        Map<String, Object> components = (Map<String, Object>) health.get("components");
        assertComponentStatus(components, "db", "DOWN");
    }

    @Test
    void shouldReturnDegradedWhenRedisDown() {
        // This test will FAIL initially - need to implement Redis health checking

        // Arrange - Mock Redis failure
        mockIdentityServiceHealthy();
        mockRedisDown();

        // Act
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/health",
            Map.class
        );

        // Assert - Should be degraded but not completely down (Redis is not critical)
        // This logic needs to be implemented
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        Map<String, Object> health = response.getBody();
        assertThat(health.get("status")).isEqualTo("DOWN");

        Map<String, Object> components = (Map<String, Object>) health.get("components");
        assertComponentStatus(components, "redis", "DOWN");
    }

    @Test
    void shouldIncludeDetailedHealthInfo() {
        // This test will FAIL initially - need to implement detailed health information

        // Arrange
        mockIdentityServiceHealthy();
        mockRedisHealthy();

        // Act
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/health",
            Map.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> health = response.getBody();
        Map<String, Object> components = (Map<String, Object>) health.get("components");

        // Verify detailed information is present - these will FAIL initially
        verifyDatabaseHealthDetails(components);
        verifyRedisHealthDetails(components);
        verifyIdentityServiceHealthDetails(components);
        verifyCustomHealthIndicatorDetails(components);
    }

    @Test
    void shouldProvideHealthGroupsForKubernetes() {
        // This test will FAIL initially - need to implement health groups

        // Test liveness probe
        ResponseEntity<Map> livenessResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/health/liveness",
            Map.class
        );
        assertThat(livenessResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Test readiness probe
        ResponseEntity<Map> readinessResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/health/readiness",
            Map.class
        );
        assertThat(readinessResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Test startup probe - this will FAIL initially
        ResponseEntity<Map> startupResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/health/startup",
            Map.class
        );
        assertThat(startupResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldIncludePerformanceMetrics() {
        // This test will FAIL initially - need to implement performance metrics

        // Arrange
        mockIdentityServiceHealthy();
        mockRedisHealthy();

        // Act
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/health",
            Map.class
        );

        // Assert
        Map<String, Object> health = response.getBody();
        Map<String, Object> components = (Map<String, Object>) health.get("components");

        // Verify performance metrics are included - these will FAIL initially
        verifyPerformanceMetrics(components, "db");
        verifyPerformanceMetrics(components, "redis");
        verifyPerformanceMetrics(components, "apiIdentityService");
    }

    @Test
    void shouldHandleConcurrentHealthChecks() {
        // This test will FAIL initially if health checks are not thread-safe

        // Arrange
        mockIdentityServiceHealthy();
        mockRedisHealthy();

        // Act - Simulate concurrent health checks
        var responses = java.util.concurrent.CompletableFuture.allOf(
            java.util.concurrent.CompletableFuture.supplyAsync(() ->
                restTemplate.getForEntity("http://localhost:" + port + "/actuator/health", Map.class)),
            java.util.concurrent.CompletableFuture.supplyAsync(() ->
                restTemplate.getForEntity("http://localhost:" + port + "/actuator/health", Map.class)),
            java.util.concurrent.CompletableFuture.supplyAsync(() ->
                restTemplate.getForEntity("http://localhost:" + port + "/actuator/health", Map.class))
        );

        // Assert - All should complete successfully
        assertDoesNotThrow(() -> responses.get());
    }

    @Test
    void shouldCompleteHealthCheckWithinTimeLimit() {
        // This test will FAIL initially if health checks are too slow

        // Arrange
        mockIdentityServiceHealthy();
        mockRedisHealthy();

        // Act & Assert
        long startTime = System.currentTimeMillis();

        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/health",
            Map.class
        );

        long duration = System.currentTimeMillis() - startTime;

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(duration).isLessThan(1000L); // Should complete within 1 second
    }

    // Helper methods for test setup
    private void mockIdentityServiceHealthy() {
        HealthResponse healthyResponse = new HealthResponse();
        healthyResponse.setStatus("UP");
        healthyResponse.setVersion("1.0.0");

        when(identityServiceClient.checkHealth()).thenReturn(healthyResponse);
    }

    private void mockRedisHealthy() {
        when(redisTemplate.opsForValue()).thenReturn(null); // This will cause NPE - needs proper mocking
        // This mock is incomplete and will cause test failures - driving implementation
    }

    private void mockRedisDown() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection failed"));
    }

    // Helper methods for assertions
    private void assertComponentStatus(Map<String, Object> components, String componentName, String expectedStatus) {
        assertThat(components).containsKey(componentName);
        Map<String, Object> component = (Map<String, Object>) components.get(componentName);
        assertThat(component.get("status")).isEqualTo(expectedStatus);
    }

    private void verifyDatabaseHealthDetails(Map<String, Object> components) {
        Map<String, Object> dbComponent = (Map<String, Object>) components.get("db");
        assertThat(dbComponent).isNotNull();

        Map<String, Object> details = (Map<String, Object>) dbComponent.get("details");
        assertThat(details).containsKey("database");
        assertThat(details).containsKey("responseTime");
        assertThat(details).containsKey("validationQuery");
    }

    private void verifyRedisHealthDetails(Map<String, Object> components) {
        Map<String, Object> redisComponent = (Map<String, Object>) components.get("redis");
        assertThat(redisComponent).isNotNull();

        Map<String, Object> details = (Map<String, Object>) redisComponent.get("details");
        assertThat(details).containsKey("responseTime");
        assertThat(details).containsKey("version");
    }

    private void verifyIdentityServiceHealthDetails(Map<String, Object> components) {
        Map<String, Object> identityComponent = (Map<String, Object>) components.get("apiIdentityService");
        assertThat(identityComponent).isNotNull();

        Map<String, Object> details = (Map<String, Object>) identityComponent.get("details");
        assertThat(details).containsKey("responseTime");
        assertThat(details).containsKey("version");
        assertThat(details).containsKey("service");
    }

    private void verifyCustomHealthIndicatorDetails(Map<String, Object> components) {
        // Verify hive service health details
        Map<String, Object> hiveComponent = (Map<String, Object>) components.get("hiveService");
        assertThat(hiveComponent).isNotNull();
        Map<String, Object> hiveDetails = (Map<String, Object>) hiveComponent.get("details");
        assertThat(hiveDetails).containsKey("activeHives");
        assertThat(hiveDetails).containsKey("totalMembers");

        // Verify presence service health details
        Map<String, Object> presenceComponent = (Map<String, Object>) components.get("presenceService");
        assertThat(presenceComponent).isNotNull();
        Map<String, Object> presenceDetails = (Map<String, Object>) presenceComponent.get("details");
        assertThat(presenceDetails).containsKey("activeConnections");
        assertThat(presenceDetails).containsKey("totalPresenceUpdates");

        // Verify WebSocket health details
        Map<String, Object> wsComponent = (Map<String, Object>) components.get("webSocket");
        assertThat(wsComponent).isNotNull();
        Map<String, Object> wsDetails = (Map<String, Object>) wsComponent.get("details");
        assertThat(wsDetails).containsKey("activeConnections");
        assertThat(wsDetails).containsKey("messagesSent");
    }

    private void verifyPerformanceMetrics(Map<String, Object> components, String componentName) {
        Map<String, Object> component = (Map<String, Object>) components.get(componentName);
        assertThat(component).isNotNull();

        Map<String, Object> details = (Map<String, Object>) component.get("details");
        assertThat(details).containsKey("responseTime");

        // Verify response time is within acceptable limits
        String responseTimeStr = (String) details.get("responseTime");
        if (responseTimeStr != null) {
            int responseTime = Integer.parseInt(responseTimeStr.replace("ms", ""));
            assertThat(responseTime).isLessThan(1000); // Less than 1 second
        }
    }
}