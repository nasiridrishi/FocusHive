package com.focushive.api.client;

import com.focushive.api.dto.identity.*;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for Identity Service client communication.
 * These tests follow TDD approach - written FIRST to fail initially.
 *
 * Tests cover:
 * - Basic service communication
 * - Circuit breaker behavior
 * - Caching mechanisms
 * - Fallback scenarios
 * - Performance requirements
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
    "identity.service.url=http://localhost:${wiremock.server.port}",
    "resilience4j.circuitbreaker.instances.identity-service.failure-rate-threshold=50",
    "resilience4j.circuitbreaker.instances.identity-service.minimum-number-of-calls=5",
    "resilience4j.circuitbreaker.instances.identity-service.sliding-window-size=10",
    "spring.cache.type=simple"
})
@Import(IdentityServiceTestConfiguration.class)
class IdentityServiceIntegrationTest {

    @Autowired
    private IdentityServiceClient identityServiceClient;

    @Autowired
    private WireMockServer wireMockServer;

    private static final String VALID_TOKEN = "Bearer valid-jwt-token";
    private static final String INVALID_TOKEN = "Bearer invalid-jwt-token";
    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.resetAll();
    }

    /**
     * TDD RED PHASE TEST
     * This test SHOULD FAIL initially because IdentityServiceClient integration is not complete.
     * Tests basic token validation functionality.
     */
    @Test
    void shouldValidateTokenSuccessfully() {
        // Arrange
        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .withHeader("Authorization", equalTo(VALID_TOKEN))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "valid": true,
                        "userId": "550e8400-e29b-41d4-a716-446655440000",
                        "username": "testuser",
                        "email": "test@example.com",
                        "authorities": ["USER"],
                        "activePersona": {
                            "id": "persona-id-123",
                            "name": "Work Persona",
                            "isActive": true
                        }
                    }
                    """)));

        // Act & Assert
        TokenValidationResponse response = identityServiceClient.validateToken(VALID_TOKEN);

        assertThat(response).isNotNull();
        assertThat(response.isValid()).isTrue();
        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getAuthorities()).containsExactly("USER");
        assertThat(response.getActivePersona()).isNotNull();
        assertThat(response.getActivePersona().getName()).isEqualTo("Work Persona");
    }

    /**
     * TDD RED PHASE TEST
     * This test SHOULD FAIL initially.
     * Tests error handling for invalid tokens.
     */
    @Test
    void shouldHandleInvalidTokenGracefully() {
        // Arrange
        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .withHeader("Authorization", equalTo(INVALID_TOKEN))
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "valid": false,
                        "errorMessage": "Invalid token"
                    }
                    """)));

        // Act & Assert
        TokenValidationResponse response = identityServiceClient.validateToken(INVALID_TOKEN);

        assertThat(response).isNotNull();
        assertThat(response.isValid()).isFalse();
        assertThat(response.getErrorMessage()).isEqualTo("Invalid token");
        assertThat(response.getUserId()).isNull();
    }

    /**
     * TDD RED PHASE TEST
     * This test SHOULD FAIL initially.
     * Tests circuit breaker behavior when Identity Service times out.
     * Circuit breaker should open after configured failures.
     */
    @Test
    void shouldTriggerCircuitBreakerOnTimeout() {
        // Arrange - simulate slow responses
        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(5000) // 5 second delay to trigger timeout
                .withBody("{\"valid\": true}")));

        // Act & Assert - make multiple calls to trigger circuit breaker
        for (int i = 0; i < 6; i++) {
            try {
                identityServiceClient.validateToken(VALID_TOKEN);
            } catch (Exception e) {
                // Expected timeouts
            }
        }

        // Circuit should now be open - next call should fail fast
        long startTime = System.currentTimeMillis();
        try {
            identityServiceClient.validateToken(VALID_TOKEN);
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            // Should fail fast (< 1000ms) when circuit is open
            assertThat(endTime - startTime).isLessThan(1000);
        }
    }

    /**
     * TDD RED PHASE TEST
     * This test SHOULD FAIL initially.
     * Tests fallback behavior when Identity Service is completely unavailable.
     */
    @Test
    void shouldUseFallbackWhenServiceUnavailable() {
        // Arrange - simulate service down
        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .willReturn(aResponse()
                .withStatus(503)
                .withBody("Service Unavailable")));

        // Act & Assert
        TokenValidationResponse response = identityServiceClient.validateToken(VALID_TOKEN);

        assertThat(response).isNotNull();
        assertThat(response.isValid()).isFalse();
        assertThat(response.getErrorMessage()).contains("Identity Service unavailable");
    }

    /**
     * TDD RED PHASE TEST
     * This test SHOULD FAIL initially.
     * Tests caching mechanism - second call should be faster.
     * Performance requirement: cached validation < 5ms
     */
    @Test
    void shouldCacheSuccessfulValidation() {
        // Arrange
        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .withHeader("Authorization", equalTo(VALID_TOKEN))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "valid": true,
                        "userId": "550e8400-e29b-41d4-a716-446655440000",
                        "username": "testuser",
                        "email": "test@example.com",
                        "authorities": ["USER"]
                    }
                    """)));

        // Act - first call (should hit service)
        long startTime1 = System.nanoTime();
        TokenValidationResponse response1 = identityServiceClient.validateToken(VALID_TOKEN);
        long endTime1 = System.nanoTime();
        long firstCallTime = TimeUnit.NANOSECONDS.toMillis(endTime1 - startTime1);

        // Act - second call (should use cache)
        long startTime2 = System.nanoTime();
        TokenValidationResponse response2 = identityServiceClient.validateToken(VALID_TOKEN);
        long endTime2 = System.nanoTime();
        long secondCallTime = TimeUnit.NANOSECONDS.toMillis(endTime2 - startTime2);

        // Assert
        assertThat(response1.isValid()).isTrue();
        assertThat(response2.isValid()).isTrue();
        assertThat(response1.getUserId()).isEqualTo(response2.getUserId());

        // Performance requirement: cached validation < 5ms
        assertThat(secondCallTime).isLessThan(5);
        assertThat(secondCallTime).isLessThan(firstCallTime);
    }

    /**
     * TDD RED PHASE TEST
     * This test SHOULD FAIL initially.
     * Tests user information retrieval.
     */
    @Test
    void shouldRetrieveUserInformation() {
        // Arrange
        stubFor(get(urlPathEqualTo("/api/v1/users/" + USER_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "id": "550e8400-e29b-41d4-a716-446655440000",
                        "username": "testuser",
                        "email": "test@example.com",
                        "displayName": "Test User",
                        "bio": "Test user for integration testing",
                        "createdAt": "2024-01-01T00:00:00"
                    }
                    """)));

        // Act
        UserDto user = identityServiceClient.getUser(USER_ID);

        // Assert
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(USER_ID);
        assertThat(user.getUsername()).isEqualTo("testuser");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getDisplayName()).isEqualTo("Test User");
        assertThat(user.getBio()).isEqualTo("Test user for integration testing");
    }

    /**
     * TDD RED PHASE TEST
     * This test SHOULD FAIL initially.
     * Tests persona management integration.
     */
    @Test
    void shouldRetrieveUserPersonas() {
        // Arrange
        stubFor(get(urlPathEqualTo("/api/v1/users/" + USER_ID + "/personas"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "personas": [
                            {
                                "id": "work-persona-123",
                                "name": "Work Persona",
                                "description": "Professional work identity",
                                "isActive": true,
                                "settings": {}
                            },
                            {
                                "id": "study-persona-456",
                                "name": "Study Persona",
                                "description": "Academic study identity",
                                "isActive": false,
                                "settings": {}
                            }
                        ],
                        "totalCount": 2
                    }
                    """)));

        // Act
        PersonaListResponse response = identityServiceClient.getUserPersonas(USER_ID);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTotalCount()).isEqualTo(2);
        assertThat(response.getPersonas()).hasSize(2);

        PersonaDto workPersona = response.getPersonas().get(0);
        assertThat(workPersona.getId()).isEqualTo("work-persona-123");
        assertThat(workPersona.getName()).isEqualTo("Work Persona");
        assertThat(workPersona.isActive()).isTrue();
    }

    /**
     * TDD RED PHASE TEST
     * This test SHOULD FAIL initially.
     * Tests performance requirement: primary validation < 50ms
     */
    @Test
    void shouldMeetPerformanceRequirements() {
        // Arrange
        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "valid": true,
                        "userId": "550e8400-e29b-41d4-a716-446655440000"
                    }
                    """)));

        // Act & Assert - test primary validation performance
        long startTime = System.nanoTime();
        TokenValidationResponse response = identityServiceClient.validateToken(VALID_TOKEN);
        long endTime = System.nanoTime();
        long responseTime = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        assertThat(response.isValid()).isTrue();
        // Performance requirement: primary validation < 50ms
        assertThat(responseTime).isLessThan(50);
    }

    /**
     * TDD RED PHASE TEST
     * This test SHOULD FAIL initially.
     * Tests circuit breaker recovery after failures.
     */
    @Test
    void shouldRecoverAfterCircuitBreakerOpens() {
        // Arrange - first cause failures
        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("Internal Server Error")));

        // Trigger circuit breaker to open
        for (int i = 0; i < 6; i++) {
            try {
                identityServiceClient.validateToken(VALID_TOKEN);
            } catch (Exception e) {
                // Expected failures
            }
        }

        // Now stub successful response
        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "valid": true,
                        "userId": "550e8400-e29b-41d4-a716-446655440000"
                    }
                    """)));

        // Wait for circuit breaker to transition to half-open
        await().atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                TokenValidationResponse response = identityServiceClient.validateToken(VALID_TOKEN);
                assertThat(response.isValid()).isTrue();
            });
    }

    /**
     * TDD RED PHASE TEST
     * This test SHOULD FAIL initially.
     * Tests health check functionality.
     */
    @Test
    void shouldCheckServiceHealth() {
        // Arrange
        stubFor(get(urlEqualTo("/actuator/health"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "status": "UP",
                        "version": "1.0.0"
                    }
                    """)));

        // Act
        HealthResponse health = identityServiceClient.checkHealth();

        // Assert
        assertThat(health).isNotNull();
        assertThat(health.getStatus()).isEqualTo("UP");
        assertThat(health.getVersion()).isEqualTo("1.0.0");
    }
}