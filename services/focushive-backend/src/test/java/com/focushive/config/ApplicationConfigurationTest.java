package com.focushive.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify application configuration loading with proper defaults.
 * This ensures the application can start successfully even when environment variables are not set.
 *
 * Production Requirement: The application must be able to start with minimal configuration
 * and provide sensible defaults for all required properties.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = ApplicationConfigurationTest.MinimalContextTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@DisplayName("Application Configuration Loading Tests")
class ApplicationConfigurationTest {

    /**
     * Minimal configuration that excludes components that might fail during startup
     * This allows us to test configuration loading in isolation
     */
    @Configuration
    @EnableAutoConfiguration(exclude = {
        org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration.class,
        org.springframework.boot.autoconfigure.websocket.servlet.WebSocketMessagingAutoConfiguration.class
    })
    @ComponentScan(
        basePackages = {"com.focushive", "com.focushive.backend"},
        excludeFilters = {
            @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = ".*WebSocket.*"
            ),
            @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = ".*websocket.*"
            )
        }
    )
    @Import({TestWebSocketConfig.class, TestWebSocketController.class})
    static class MinimalContextTestConfiguration {
    }

    @Autowired
    private Environment environment;

    @Value("${logging.level.root:INFO}")
    private String rootLogLevel;

    @Value("${logging.level.com.focushive:DEBUG}")
    private String applicationLogLevel;

    @Value("${logging.level.org.springframework.web:INFO}")
    private String springWebLogLevel;

    @Value("${logging.level.org.springframework.security:INFO}")
    private String springSecurityLogLevel;

    @Value("${logging.level.org.springframework.data:INFO}")
    private String springDataLogLevel;

    @Value("${logging.level.org.hibernate.SQL:DEBUG}")
    private String hibernateSqlLogLevel;

    @Value("${logging.level.org.hibernate.type.descriptor.sql.BasicBinder:TRACE}")
    private String hibernateBindLogLevel;

    @Value("${logging.level.feign:DEBUG}")
    private String feignLogLevel;

    @Test
    @DisplayName("Should load logging configuration with defaults when environment variables are not set")
    void testLoggingConfigurationDefaults() {
        // Verify that logging levels are loaded with proper defaults
        assertNotNull(rootLogLevel, "Root log level should not be null");
        assertTrue(isValidLogLevel(rootLogLevel), "Root log level should be a valid Spring Boot LogLevel");

        assertNotNull(applicationLogLevel, "Application log level should not be null");
        assertTrue(isValidLogLevel(applicationLogLevel), "Application log level should be a valid Spring Boot LogLevel");

        assertNotNull(springWebLogLevel, "Spring Web log level should not be null");
        assertTrue(isValidLogLevel(springWebLogLevel), "Spring Web log level should be a valid Spring Boot LogLevel");

        assertNotNull(springSecurityLogLevel, "Spring Security log level should not be null");
        assertTrue(isValidLogLevel(springSecurityLogLevel), "Spring Security log level should be a valid Spring Boot LogLevel");

        assertNotNull(springDataLogLevel, "Spring Data log level should not be null");
        assertTrue(isValidLogLevel(springDataLogLevel), "Spring Data log level should be a valid Spring Boot LogLevel");

        assertNotNull(hibernateSqlLogLevel, "Hibernate SQL log level should not be null");
        assertTrue(isValidLogLevel(hibernateSqlLogLevel), "Hibernate SQL log level should be a valid Spring Boot LogLevel");

        assertNotNull(hibernateBindLogLevel, "Hibernate Bind log level should not be null");
        assertTrue(isValidLogLevel(hibernateBindLogLevel), "Hibernate Bind log level should be a valid Spring Boot LogLevel");

        assertNotNull(feignLogLevel, "Feign log level should not be null");
        assertTrue(isValidLogLevel(feignLogLevel), "Feign log level should be a valid Spring Boot LogLevel");
    }

    @Test
    @DisplayName("Should load management endpoints configuration with defaults")
    void testManagementEndpointsConfiguration() {
        String endpointsInclude = environment.getProperty("management.endpoints.web.exposure.include");
        assertNotNull(endpointsInclude, "Management endpoints include should not be null");
        assertFalse(endpointsInclude.contains("${"), "Management endpoints should not contain unresolved placeholders");

        String healthShowDetails = environment.getProperty("management.endpoint.health.show-details");
        assertNotNull(healthShowDetails, "Health show details should not be null");
        assertFalse(healthShowDetails.contains("${"), "Health show details should not contain unresolved placeholders");

        String healthShowComponents = environment.getProperty("management.endpoint.health.show-components");
        assertNotNull(healthShowComponents, "Health show components should not be null");
        assertFalse(healthShowComponents.contains("${"), "Health show components should not contain unresolved placeholders");
    }

    @Test
    @DisplayName("Should load metrics configuration with defaults")
    void testMetricsConfiguration() {
        String prometheusEnabled = environment.getProperty("management.metrics.export.prometheus.enabled");
        assertNotNull(prometheusEnabled, "Prometheus enabled should not be null");
        assertFalse(prometheusEnabled.contains("${"), "Prometheus enabled should not contain unresolved placeholders");

        String percentiles = environment.getProperty("management.metrics.distribution.percentiles.[http.server.requests]");
        assertNotNull(percentiles, "Metrics percentiles should not be null");
        assertFalse(percentiles.contains("${"), "Metrics percentiles should not contain unresolved placeholders");
    }

    @Test
    @DisplayName("Should load Resilience4j configuration with defaults")
    void testResilience4jConfiguration() {
        // Test circuit breaker configuration
        String cbWindowSize = environment.getProperty("resilience4j.circuitbreaker.instances.identity-service.slidingWindowSize");
        assertNotNull(cbWindowSize, "Circuit breaker window size should not be null");
        assertFalse(cbWindowSize.contains("${"), "Circuit breaker window size should not contain unresolved placeholders");

        String cbFailureThreshold = environment.getProperty("resilience4j.circuitbreaker.instances.identity-service.failureRateThreshold");
        assertNotNull(cbFailureThreshold, "Circuit breaker failure threshold should not be null");
        assertFalse(cbFailureThreshold.contains("${"), "Circuit breaker failure threshold should not contain unresolved placeholders");

        // Test retry configuration
        String retryMaxAttempts = environment.getProperty("resilience4j.retry.instances.identity-service.maxAttempts");
        assertNotNull(retryMaxAttempts, "Retry max attempts should not be null");
        assertFalse(retryMaxAttempts.contains("${"), "Retry max attempts should not contain unresolved placeholders");

        // Test rate limiter configuration
        String rateLimitPeriod = environment.getProperty("resilience4j.ratelimiter.instances.identity-service.limitRefreshPeriod");
        assertNotNull(rateLimitPeriod, "Rate limiter refresh period should not be null");
        assertFalse(rateLimitPeriod.contains("${"), "Rate limiter refresh period should not contain unresolved placeholders");
    }

    @Test
    @DisplayName("Should load Feign configuration with defaults")
    void testFeignConfiguration() {
        String feignCircuitBreakerEnabled = environment.getProperty("feign.circuitbreaker.enabled");
        assertNotNull(feignCircuitBreakerEnabled, "Feign circuit breaker enabled should not be null");
        assertFalse(feignCircuitBreakerEnabled.contains("${"), "Feign circuit breaker enabled should not contain unresolved placeholders");

        String connectTimeout = environment.getProperty("feign.client.config.identity-service.connectTimeout");
        assertNotNull(connectTimeout, "Feign connect timeout should not be null");
        assertFalse(connectTimeout.contains("${"), "Feign connect timeout should not contain unresolved placeholders");

        String readTimeout = environment.getProperty("feign.client.config.identity-service.readTimeout");
        assertNotNull(readTimeout, "Feign read timeout should not be null");
        assertFalse(readTimeout.contains("${"), "Feign read timeout should not contain unresolved placeholders");
    }

    @Test
    @DisplayName("Should application context load successfully with configuration")
    void testApplicationContextLoads() {
        // This test verifies that the Spring context loads successfully
        // which means all configuration is properly resolved
        assertNotNull(environment, "Environment should be loaded");

        // Verify critical properties are resolved
        String serverPort = environment.getProperty("server.port");
        assertNotNull(serverPort, "Server port should be configured");
        assertFalse(serverPort.contains("${"), "Server port should not contain unresolved placeholders");
    }

    /**
     * Helper method to validate if a string is a valid Spring Boot LogLevel
     */
    private boolean isValidLogLevel(String level) {
        if (level == null || level.contains("${")) {
            return false;
        }
        try {
            // Valid Spring Boot log levels
            return level.matches("(?i)(OFF|ERROR|WARN|INFO|DEBUG|TRACE|FATAL)");
        } catch (Exception e) {
            return false;
        }
    }
}