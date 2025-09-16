package com.focushive.api.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import java.util.HashMap;
import java.util.Map;

/**
 * Health indicator for Circuit Breaker states.
 * Provides visibility into circuit breaker status for monitoring.
 */
@Slf4j
@Profile("!test")
@Component("circuitBreaker")
@RequiredArgsConstructor
public class CircuitBreakerHealthIndicator implements HealthIndicator {
    
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        
        // Check all registered circuit breakers and collect their health status
        boolean allCircuitsHealthy = circuitBreakerRegistry.getAllCircuitBreakers().stream()
            .allMatch(circuitBreaker -> {
                String name = circuitBreaker.getName();
                CircuitBreaker.State state = circuitBreaker.getState();
                
                Map<String, Object> circuitDetails = new HashMap<>();
                circuitDetails.put("state", state.name());
                
                // Get circuit breaker metrics
                CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
                circuitDetails.put("failureRate", String.format("%.2f%%", metrics.getFailureRate()));
                circuitDetails.put("slowCallRate", String.format("%.2f%%", metrics.getSlowCallRate()));
                circuitDetails.put("numberOfCalls", metrics.getNumberOfBufferedCalls());
                circuitDetails.put("numberOfFailedCalls", metrics.getNumberOfFailedCalls());
                circuitDetails.put("numberOfSlowCalls", metrics.getNumberOfSlowCalls());
                
                details.put(name, circuitDetails);
                
                // Circuit breaker is unhealthy if it's OPEN or FORCED_OPEN
                boolean isHealthy = !(state == CircuitBreaker.State.OPEN || state == CircuitBreaker.State.FORCED_OPEN);
                if (!isHealthy) {
                    log.warn("Circuit breaker '{}' is in {} state", name, state);
                }
                return isHealthy;
            });
        
        Health.Builder healthBuilder = allCircuitsHealthy ? Health.up() : Health.down();
        
        return healthBuilder
            .withDetail("circuitBreakers", details)
            .withDetail("totalCircuitBreakers", details.size())
            .build();
    }
}