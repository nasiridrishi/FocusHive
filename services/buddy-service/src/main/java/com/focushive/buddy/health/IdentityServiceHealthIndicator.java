package com.focushive.buddy.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.function.Supplier;

/**
 * Custom health indicator for Identity Service dependency
 * Checks if the identity service is accessible for JWT validation
 */
@Component("identityService")
public class IdentityServiceHealthIndicator implements HealthIndicator {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8081}")
    private String identityServiceUrl;

    @Autowired(required = false)
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Health health() {
        try {
            // Use circuit breaker if available
            if (circuitBreakerRegistry != null) {
                CircuitBreaker circuitBreaker = circuitBreakerRegistry
                        .circuitBreaker("identity-service");

                Supplier<Health> healthSupplier = CircuitBreaker
                        .decorateSupplier(circuitBreaker, this::checkIdentityServiceHealth);

                return healthSupplier.get();
            } else {
                return checkIdentityServiceHealth();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("service", "Identity Service")
                    .build();
        }
    }

    private Health checkIdentityServiceHealth() {
        long startTime = System.currentTimeMillis();

        try {
            // Check the health endpoint of identity service
            String healthUrl = identityServiceUrl + "/actuator/health";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);

            long responseTime = System.currentTimeMillis() - startTime;

            if (response.getStatusCode() == HttpStatus.OK) {
                Health.Builder builder = Health.up()
                        .withDetail("service", "Identity Service")
                        .withDetail("url", identityServiceUrl)
                        .withDetail("responseTime", responseTime + "ms")
                        .withDetail("status", "Available");

                // Add circuit breaker status if available
                if (circuitBreakerRegistry != null) {
                    CircuitBreaker circuitBreaker = circuitBreakerRegistry
                            .circuitBreaker("identity-service");
                    builder.withDetail("circuitBreakerState",
                            circuitBreaker.getState().toString());
                }

                return builder.build();
            } else {
                return Health.down()
                        .withDetail("service", "Identity Service")
                        .withDetail("url", identityServiceUrl)
                        .withDetail("httpStatus", response.getStatusCode().value())
                        .withDetail("error", "Unexpected status code")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("service", "Identity Service")
                    .withDetail("url", identityServiceUrl)
                    .withDetail("error", "Cannot connect: " + e.getMessage())
                    .build();
        }
    }
}