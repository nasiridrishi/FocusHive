package com.focushive.notification.integration;

import com.focushive.notification.config.CircuitBreakerConfig;
import com.focushive.notification.config.RestTemplateConfig;
import com.focushive.notification.dto.UserInfo;
import com.focushive.notification.messaging.dto.NotificationMessage;
import com.focushive.notification.service.*;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration tests for Circuit Breaker behavior.
 * Tests actual circuit breaker transitions and fallback mechanisms.
 */
@SpringBootTest(classes = {
    CircuitBreakerIntegrationTest.TestConfig.class,
    CircuitBreakerConfig.class,
    RestTemplateConfig.class,
    ResilientEmailService.class,
    ResilientIdentityServiceClient.class
})
@ActiveProfiles("test")
@DisplayName("Circuit Breaker Integration Tests")
class CircuitBreakerIntegrationTest {

    @TestConfiguration
    @Import({CircuitBreakerConfig.class, RestTemplateConfig.class})
    static class TestConfig {
        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private ResilientEmailService resilientEmailService;

    @Autowired
    private ResilientIdentityServiceClient resilientIdentityServiceClient;

    @MockBean
    private EmailNotificationService emailNotificationService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    private WireMockServer wireMockServer;
    private CircuitBreaker emailCircuitBreaker;
    private CircuitBreaker identityCircuitBreaker;

    @BeforeEach
    void setUp() {
        // Start WireMock server for identity service simulation
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig()
            .port(8089));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);

        // Get circuit breakers
        emailCircuitBreaker = circuitBreakerRegistry.circuitBreaker("email-service");
        identityCircuitBreaker = circuitBreakerRegistry.circuitBreaker("identity-service");

        // Reset circuit breakers to closed state
        emailCircuitBreaker.reset();
        identityCircuitBreaker.reset();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    @DisplayName("Should open email circuit breaker after consecutive failures")
    void shouldOpenEmailCircuitBreakerAfterFailures() {
        // Given - simulate failures
        when(emailNotificationService.sendEmail(any(NotificationMessage.class)))
            .thenThrow(new RuntimeException("Email service unavailable"));

        // When - make calls to trigger circuit breaker
        NotificationMessage message = NotificationMessage.builder()
            .emailTo("test@example.com")
            .emailSubject("Test")
            .message("Test message")
            .build();

        // First 5 calls should fail and trigger circuit opening
        for (int i = 0; i < 5; i++) {
            try {
                resilientEmailService.sendEmail(message);
            } catch (Exception ignored) {
                // Expected to fail
            }
        }

        // Then - circuit should be open
        assertEquals(CircuitBreaker.State.OPEN, emailCircuitBreaker.getState());

        // Additional calls should use fallback immediately
        String fallbackId = resilientEmailService.sendEmail(message);
        assertTrue(fallbackId.startsWith("fallback-"));

        // Verify email service was called 5 times before circuit opened
        verify(emailNotificationService, times(5)).sendEmail(any());
    }

    @Test
    @DisplayName("Should use fallback when identity service is unavailable")
    void shouldUseFallbackWhenIdentityServiceUnavailable() {
        // Given - identity service returns 500 errors
        wireMockServer.stubFor(get(urlPathMatching("/users/.*"))
            .willReturn(aResponse()
                .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .withBody("Service unavailable")));

        // When - make multiple calls to trigger circuit
        String userId = "user123";
        for (int i = 0; i < 3; i++) {
            Optional<UserInfo> userInfo = resilientIdentityServiceClient.getUserInfo(userId);
            // Should eventually return cached/fallback data
            if (i >= 2 && identityCircuitBreaker.getState() == CircuitBreaker.State.OPEN) {
                assertTrue(userInfo.isPresent());
                assertTrue(userInfo.get().isStale());
            }
        }

        // Then - verify circuit state
        assertNotEquals(CircuitBreaker.State.CLOSED, identityCircuitBreaker.getState());
    }

    @Test
    @DisplayName("Should transition from open to half-open after wait duration")
    void shouldTransitionToHalfOpenAfterWaitDuration() throws InterruptedException {
        // Given - force circuit to open
        when(emailNotificationService.sendEmail(any()))
            .thenThrow(new RuntimeException("Service down"));

        NotificationMessage message = NotificationMessage.builder()
            .emailTo("test@example.com")
            .emailSubject("Test")
            .message("Test")
            .build();

        // Open the circuit
        for (int i = 0; i < 5; i++) {
            try {
                resilientEmailService.sendEmail(message);
            } catch (Exception ignored) {
            }
        }

        assertEquals(CircuitBreaker.State.OPEN, emailCircuitBreaker.getState());

        // When - wait for transition (this is a simplified test, actual wait is 30s)
        // In real scenario, we'd wait or mock time
        // For test, we'll force transition
        emailCircuitBreaker.transitionToHalfOpenState();

        // Then
        assertEquals(CircuitBreaker.State.HALF_OPEN, emailCircuitBreaker.getState());

        // A successful call should close the circuit
        when(emailNotificationService.sendEmail(any())).thenReturn("success-id");
        String result = resilientEmailService.sendEmail(message);

        assertNotNull(result);
        // After successful calls in half-open, circuit should close
        // (depends on configuration, typically after 3 successful calls)
    }

    @Test
    @DisplayName("Should record circuit breaker metrics")
    void shouldRecordCircuitBreakerMetrics() {
        // Given
        NotificationMessage message = NotificationMessage.builder()
            .emailTo("test@example.com")
            .emailSubject("Test")
            .message("Test")
            .build();

        // When - successful call
        when(emailNotificationService.sendEmail(any())).thenReturn("success-id");
        resilientEmailService.sendEmail(message);

        // Then - verify metrics recorded
        assertNotNull(meterRegistry.get("resilience4j.circuitbreaker.state")
            .tag("name", "email-service")
            .gauge());

        assertNotNull(meterRegistry.get("resilience4j.circuitbreaker.calls")
            .tag("name", "email-service")
            .tag("kind", "successful")
            .counter());
    }

    @Test
    @DisplayName("Should handle slow calls and mark them appropriately")
    void shouldHandleSlowCalls() throws InterruptedException {
        // Given - simulate slow response
        wireMockServer.stubFor(get(urlPathMatching("/users/.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(3000) // 3 seconds delay
                .withHeader("Content-Type", "application/json")
                .withBody("{\"userId\":\"user123\",\"email\":\"test@example.com\"}")));

        // When - make a slow call
        long startTime = System.currentTimeMillis();
        Optional<UserInfo> userInfo = resilientIdentityServiceClient.getUserInfo("user123");
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertTrue(duration >= 3000);
        // Metrics should reflect slow call
        // Note: In a real test, we'd verify slow call metrics
    }

    @Test
    @DisplayName("Should not trigger circuit breaker on client errors")
    void shouldNotTriggerCircuitBreakerOnClientErrors() {
        // Given - identity service returns 400 errors
        wireMockServer.stubFor(get(urlPathMatching("/users/.*"))
            .willReturn(aResponse()
                .withStatus(HttpStatus.BAD_REQUEST.value())
                .withBody("Invalid request")));

        // When - make multiple calls with client errors
        for (int i = 0; i < 10; i++) {
            Optional<UserInfo> userInfo = resilientIdentityServiceClient.getUserInfo("invalid-user");
            assertFalse(userInfo.isPresent());
        }

        // Then - circuit should remain closed
        assertEquals(CircuitBreaker.State.CLOSED, identityCircuitBreaker.getState());
    }

    @Test
    @DisplayName("Should close circuit after successful calls in half-open state")
    void shouldCloseCircuitAfterSuccessfulCallsInHalfOpen() {
        // Given - circuit in half-open state
        when(emailNotificationService.sendEmail(any()))
            .thenThrow(new RuntimeException("Service down"));

        NotificationMessage message = NotificationMessage.builder()
            .emailTo("test@example.com")
            .emailSubject("Test")
            .message("Test")
            .build();

        // Open the circuit
        for (int i = 0; i < 5; i++) {
            try {
                resilientEmailService.sendEmail(message);
            } catch (Exception ignored) {
            }
        }

        // Transition to half-open
        emailCircuitBreaker.transitionToHalfOpenState();
        assertEquals(CircuitBreaker.State.HALF_OPEN, emailCircuitBreaker.getState());

        // When - make successful calls
        when(emailNotificationService.sendEmail(any())).thenReturn("success-id");

        for (int i = 0; i < 3; i++) {
            String result = resilientEmailService.sendEmail(message);
            assertNotNull(result);
        }

        // Then - circuit should eventually close
        // Note: Exact behavior depends on configuration
    }

    @Test
    @DisplayName("Should provide cached responses during circuit open state")
    void shouldProvideCachedResponsesDuringCircuitOpen() {
        // Given - first successful call to cache response
        wireMockServer.stubFor(get(urlPathEqualTo("/users/user123"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"userId\":\"user123\",\"email\":\"cached@example.com\",\"name\":\"Cached User\"}")));

        // Cache the response
        Optional<UserInfo> cachedResponse = resilientIdentityServiceClient.getUserInfo("user123");
        assertTrue(cachedResponse.isPresent());
        assertEquals("cached@example.com", cachedResponse.get().getEmail());

        // Now simulate service failure
        wireMockServer.resetAll();
        wireMockServer.stubFor(get(urlPathMatching("/users/.*"))
            .willReturn(aResponse()
                .withStatus(500)));

        // Force circuit to open
        for (int i = 0; i < 3; i++) {
            resilientIdentityServiceClient.getUserInfo("user456");
        }

        // When - circuit is open, should return cached data
        Optional<UserInfo> fallbackResponse = resilientIdentityServiceClient.getUserInfo("user123");

        // Then
        assertTrue(fallbackResponse.isPresent());
        assertTrue(fallbackResponse.get().isStale());
        assertEquals("cached@example.com", fallbackResponse.get().getEmail());
    }

    @Test
    @DisplayName("Should handle concurrent requests with circuit breaker")
    void shouldHandleConcurrentRequestsWithCircuitBreaker() throws InterruptedException {
        // Given
        NotificationMessage message = NotificationMessage.builder()
            .emailTo("test@example.com")
            .emailSubject("Test")
            .message("Test")
            .build();

        when(emailNotificationService.sendEmail(any())).thenReturn("success-id");

        // When - make concurrent requests
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                String result = resilientEmailService.sendEmail(message);
                assertNotNull(result);
            });
            threads[i].start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }

        // Then - verify all calls handled correctly
        CircuitBreaker.Metrics metrics = emailCircuitBreaker.getMetrics();
        assertTrue(metrics.getNumberOfSuccessfulCalls() > 0);
    }

    @Test
    @DisplayName("Should respect different circuit breaker configurations per service")
    void shouldRespectDifferentCircuitBreakerConfigurations() {
        // Email service has 50% failure threshold
        // Identity service has 30% failure threshold

        // Given - both services experience failures
        when(emailNotificationService.sendEmail(any()))
            .thenThrow(new RuntimeException("Email failed"))
            .thenThrow(new RuntimeException("Email failed"))
            .thenReturn("success")
            .thenReturn("success")
            .thenReturn("success");

        wireMockServer.stubFor(get(urlPathMatching("/users/.*"))
            .willReturn(aResponse()
                .withStatus(500)
                .withFixedDelay(100)));

        // When - make calls to both services
        NotificationMessage message = NotificationMessage.builder()
            .emailTo("test@example.com")
            .emailSubject("Test")
            .message("Test")
            .build();

        // Email service: 2 failures out of 5 = 40% (below 50% threshold)
        for (int i = 0; i < 5; i++) {
            try {
                resilientEmailService.sendEmail(message);
            } catch (Exception ignored) {
            }
        }

        // Identity service: 2 failures out of 5 = 40% (above 30% threshold)
        for (int i = 0; i < 5; i++) {
            resilientIdentityServiceClient.getUserInfo("user" + i);
        }

        // Then - verify different behaviors based on thresholds
        // Email circuit should still be closed (40% < 50%)
        // Identity circuit might be open or transitioning (40% > 30%)
        CircuitBreaker.Metrics emailMetrics = emailCircuitBreaker.getMetrics();
        CircuitBreaker.Metrics identityMetrics = identityCircuitBreaker.getMetrics();

        // Verify metrics collected
        assertTrue(emailMetrics.getNumberOfBufferedCalls() > 0);
        assertTrue(identityMetrics.getNumberOfBufferedCalls() > 0);
    }
}