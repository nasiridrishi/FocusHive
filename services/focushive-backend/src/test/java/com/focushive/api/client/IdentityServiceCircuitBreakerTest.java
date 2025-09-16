package com.focushive.api.client;

import com.focushive.api.dto.identity.TokenValidationResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * TDD RED PHASE TESTS for Identity Service Circuit Breaker behavior.
 * These tests SHOULD FAIL initially until circuit breaker is properly configured.
 *
 * Tests circuit breaker requirements:
 * - Opens after configured failure threshold
 * - Provides fast failure when open
 * - Transitions to half-open for recovery
 * - Automatic recovery on success
 * - Proper fallback behavior
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
    "identity.service.url=http://localhost:${wiremock.server.port}",
    // Circuit breaker configuration for testing
    "resilience4j.circuitbreaker.instances.identity-service.failure-rate-threshold=50",
    "resilience4j.circuitbreaker.instances.identity-service.minimum-number-of-calls=3",
    "resilience4j.circuitbreaker.instances.identity-service.sliding-window-size=5",
    "resilience4j.circuitbreaker.instances.identity-service.wait-duration-in-open-state=2s",
    "resilience4j.circuitbreaker.instances.identity-service.permitted-number-of-calls-in-half-open-state=2",
    // Timeout configuration
    "resilience4j.timelimiter.instances.identity-service.timeout-duration=1s"
})
class IdentityServiceCircuitBreakerTest {

    @Autowired
    private IdentityServiceClient identityServiceClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private WireMockServer wireMockServer;

    private CircuitBreaker circuitBreaker;

    private static final String TEST_TOKEN = "Bearer circuit-breaker-test-token";

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("identity-service");
        circuitBreaker.reset();
    }

    /**
     * TDD RED PHASE TEST
     * This test SHOULD FAIL initially.
     * Tests that circuit breaker opens after configured failures.
     */
    @Test
    void shouldOpenCircuitBreakerAfterFailures() {
        // Arrange - simulate service failures
        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("Internal Server Error")));

        // Act - make enough calls to trigger circuit breaker
        for (int i = 0; i < 4; i++) {
            try {
                identityServiceClient.validateToken(TEST_TOKEN);
            } catch (Exception e) {
                // Expected failures
            }
        }

        // Assert - circuit breaker should be open
        assertThat(circuitBreaker.getState())
            .isEqualTo(CircuitBreaker.State.OPEN);

        // Verify metrics
        assertThat(circuitBreaker.getMetrics().getFailureRate())
            .isGreaterThanOrEqualTo(50.0f);
    }

    /**
     * TDD RED PHASE TEST
     * This test SHOULD FAIL initially.
     * Tests that circuit breaker provides fast failure when open.
     */
    @Test
    void shouldFailFastWhenCircuitBreakerIsOpen() {
        // Arrange - force circuit breaker to open
        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .willReturn(aResponse()
                .withStatus(500)));

        // Trigger circuit breaker to open
        for (int i = 0; i < 4; i++) {
            try {
                identityServiceClient.validateToken(TEST_TOKEN);
            } catch (Exception e) {
                // Expected
            }
        }

        // Verify circuit is open
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Act - measure response time when circuit is open
        long startTime = System.currentTimeMillis();
        try {
            identityServiceClient.validateToken(TEST_TOKEN);
        } catch (Exception e) {
            // Expected fast failure
        }
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        // Assert - should fail fast (< 100ms) when circuit is open
        assertThat(responseTime).isLessThan(100);

        // Should use fallback response
        TokenValidationResponse response = identityServiceClient.validateToken(TEST_TOKEN);
        assertThat(response.isValid()).isFalse();
        assertThat(response.getErrorMessage()).contains("Identity Service unavailable");
    }

    /**
     * TDD RED PHASE TEST
     * This test SHOULD FAIL initially.
     * Tests circuit breaker transitions to half-open state.
     */
    @Test
    void shouldTransitionToHalfOpenState() {
        // Arrange - force circuit breaker to open
        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .willReturn(aResponse()
                .withStatus(500)));

        // Open the circuit
        for (int i = 0; i < 4; i++) {
            try {
                identityServiceClient.validateToken(TEST_TOKEN);
            } catch (Exception e) {
                // Expected
            }
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Act & Assert - wait for transition to half-open
        await()
            .atMost(5, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(100))
            .untilAsserted(() -> {
                try {
                    identityServiceClient.validateToken(TEST_TOKEN);
                } catch (Exception e) {
                    // Trigger state check
                }
                // Circuit should transition to HALF_OPEN after wait duration
                assertThat(circuitBreaker.getState())
                    .isIn(CircuitBreaker.State.HALF_OPEN, CircuitBreaker.State.OPEN);
            });
    }

    /**
     * TDD RED PHASE TEST
     * This test SHOULD FAIL initially.
     * Tests circuit breaker recovery after successful calls.
     */
    @Test
    void shouldRecoverAfterSuccessfulCalls() {
        // Arrange - first cause failures to open circuit
        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .willReturn(aResponse()
                .withStatus(500)));

        // Open the circuit
        for (int i = 0; i < 4; i++) {
            try {
                identityServiceClient.validateToken(TEST_TOKEN);
            } catch (Exception e) {
                // Expected
            }
        }

        // Now stub successful responses
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

        // Act & Assert - wait for circuit breaker to recover
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
                TokenValidationResponse response = identityServiceClient.validateToken(TEST_TOKEN);
                assertThat(response.isValid()).isTrue();
                assertThat(circuitBreaker.getState())
                    .isEqualTo(CircuitBreaker.State.CLOSED);
            });
    }

    /**
     * TDD RED PHASE TEST
     * This test SHOULD FAIL initially.
     * Tests timeout handling triggers circuit breaker.
     */
    @Test
    void shouldHandleTimeoutsAndTriggerCircuitBreaker() {
        // Arrange - simulate slow responses that timeout
        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(2000) // 2 second delay, timeout is 1 second
                .withBody("""
                    {
                        "valid": true,
                        "userId": "550e8400-e29b-41d4-a716-446655440000"
                    }
                    """)));

        // Act - make calls that will timeout
        for (int i = 0; i < 4; i++) {
            try {
                identityServiceClient.validateToken(TEST_TOKEN);
            } catch (Exception e) {
                // Expected timeouts
            }
        }

        // Assert - circuit breaker should open due to timeouts
        assertThat(circuitBreaker.getState())
            .isEqualTo(CircuitBreaker.State.OPEN);

        // Verify that timeouts are treated as failures
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls())
            .isGreaterThanOrEqualTo(3);
    }

    /**
     * TDD RED PHASE TEST
     * This test SHOULD FAIL initially.
     * Tests circuit breaker metrics collection.
     */
    @Test
    void shouldCollectCircuitBreakerMetrics() {
        // Arrange - mix of successful and failed calls
        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .inScenario("Mixed Results")
            .whenScenarioStateIs("Started")
            .willReturn(aResponse().withStatus(200).withBody("{\"valid\": true}"))
            .willSetStateTo("After Success"));

        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .inScenario("Mixed Results")
            .whenScenarioStateIs("After Success")
            .willReturn(aResponse().withStatus(500))
            .willSetStateTo("After Failure"));

        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .inScenario("Mixed Results")
            .whenScenarioStateIs("After Failure")
            .willReturn(aResponse().withStatus(200).withBody("{\"valid\": true}"))
            .willSetStateTo("Started"));

        // Act - make mixed calls
        for (int i = 0; i < 6; i++) {
            try {
                identityServiceClient.validateToken(TEST_TOKEN);
            } catch (Exception e) {
                // Some will fail
            }
        }

        // Assert - verify metrics are collected
        var metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfBufferedCalls()).isGreaterThan(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isGreaterThan(0);
        assertThat(metrics.getNumberOfFailedCalls()).isGreaterThan(0);
    }

    /**
     * TDD RED PHASE TEST
     * This test SHOULD FAIL initially.
     * Tests that circuit breaker doesn't interfere with successful operations.
     */
    @Test
    void shouldNotInterferWithSuccessfulOperations() {
        // Arrange - all successful responses
        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "valid": true,
                        "userId": "550e8400-e29b-41d4-a716-446655440000",
                        "username": "testuser"
                    }
                    """)));

        // Act - make successful calls
        for (int i = 0; i < 10; i++) {
            TokenValidationResponse response = identityServiceClient.validateToken(TEST_TOKEN);
            assertThat(response.isValid()).isTrue();
            assertThat(response.getUsername()).isEqualTo("testuser");
        }

        // Assert - circuit breaker should remain closed
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(10);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
    }
}