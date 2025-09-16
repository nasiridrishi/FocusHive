package com.focushive.notification.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Configuration for Circuit Breakers using Resilience4j.
 * Provides fault tolerance for external service calls.
 */
@Slf4j
@Configuration
public class CircuitBreakerConfig {

    private final MeterRegistry meterRegistry;

    public CircuitBreakerConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Creates the circuit breaker registry with default configuration.
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        // Default configuration for all circuit breakers
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig defaultConfig =
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f) // Open circuit if 50% of calls fail
                .slowCallRateThreshold(50.0f) // Consider circuit unhealthy if 50% calls are slow
                .slowCallDurationThreshold(Duration.ofSeconds(2)) // Calls taking >2s are slow
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10) // Look at last 10 calls
                .minimumNumberOfCalls(5) // Need at least 5 calls before calculating failure rate
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before trying again
                .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 test calls when half-open
                .automaticTransitionFromOpenToHalfOpenEnabled(true) // Auto transition to half-open
                .recordExceptions(IOException.class, TimeoutException.class,
                    ResourceAccessException.class, HttpServerErrorException.class)
                .ignoreExceptions(IllegalArgumentException.class, NullPointerException.class)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);

        // Add event consumers for monitoring
        registry.getEventPublisher()
            .onEntryAdded(this::onCircuitBreakerAdded)
            .onEntryRemoved(this::onCircuitBreakerRemoved);

        return registry;
    }

    /**
     * Circuit breaker for email service with custom configuration.
     */
    @Bean
    public CircuitBreaker emailServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig emailConfig =
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f) // Open circuit if 50% of calls fail
                .slowCallRateThreshold(50.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(IOException.class, TimeoutException.class,
                    ResourceAccessException.class, HttpServerErrorException.class)
                .ignoreExceptions(IllegalArgumentException.class, NullPointerException.class)
                .build();

        CircuitBreaker circuitBreaker = registry.circuitBreaker("email-service", emailConfig);

        // Register metrics for this circuit breaker
        registerMetrics(circuitBreaker);

        // Add event listeners for logging
        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                log.info("Email service circuit breaker state transition: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onFailureRateExceeded(event ->
                log.warn("Email service circuit breaker failure rate exceeded: {}%",
                    event.getFailureRate()))
            .onSlowCallRateExceeded(event ->
                log.warn("Email service circuit breaker slow call rate exceeded: {}%",
                    event.getSlowCallRate()));

        return circuitBreaker;
    }

    /**
     * Circuit breaker for identity service with more lenient configuration.
     */
    @Bean
    public CircuitBreaker identityServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig identityConfig =
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(30.0f) // More lenient - open at 30% failure rate
                .slowCallRateThreshold(50.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(3) // Lower threshold for identity service
                .waitDurationInOpenState(Duration.ofSeconds(20)) // Shorter wait time
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(IOException.class, TimeoutException.class,
                    ResourceAccessException.class, HttpServerErrorException.class)
                .ignoreExceptions(IllegalArgumentException.class, NullPointerException.class,
                    HttpClientErrorException.class)
                .build();

        CircuitBreaker circuitBreaker = registry.circuitBreaker("identity-service", identityConfig);

        // Register metrics
        registerMetrics(circuitBreaker);

        // Add event listeners
        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                log.info("Identity service circuit breaker state transition: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onFailureRateExceeded(event ->
                log.warn("Identity service circuit breaker failure rate exceeded: {}%",
                    event.getFailureRate()));

        return circuitBreaker;
    }

    /**
     * Health indicator for circuit breakers.
     */
    @Bean
    public HealthIndicator circuitBreakerHealthIndicator(CircuitBreakerRegistry registry) {
        return () -> {
            Map<String, Object> details = new java.util.HashMap<>();
            Status overallStatus = Status.UP;
            String message = "All circuit breakers are closed";

            for (CircuitBreaker circuitBreaker : registry.getAllCircuitBreakers()) {
                String name = circuitBreaker.getName();
                CircuitBreaker.State state = circuitBreaker.getState();

                details.put(name + ".state", state.toString());

                // Add metrics if available
                CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
                details.put(name + ".failureRate", metrics.getFailureRate());
                details.put(name + ".slowCallRate", metrics.getSlowCallRate());
                details.put(name + ".successfulCalls", metrics.getNumberOfSuccessfulCalls());
                details.put(name + ".failedCalls", metrics.getNumberOfFailedCalls());
                details.put(name + ".slowCalls", metrics.getNumberOfSlowCalls());
                details.put(name + ".notPermittedCalls", metrics.getNumberOfNotPermittedCalls());

                // Determine overall status based on circuit states
                if (state == CircuitBreaker.State.OPEN) {
                    overallStatus = Status.DOWN;
                    message = "Circuit breaker " + name + " is OPEN";
                } else if (state == CircuitBreaker.State.HALF_OPEN && overallStatus != Status.DOWN) {
                    overallStatus = Status.OUT_OF_SERVICE;
                    message = "Circuit breaker " + name + " is HALF_OPEN";
                }
            }

            details.put("message", message);

            return Health.status(overallStatus)
                .withDetails(details)
                .build();
        };
    }

    /**
     * Register metrics for a circuit breaker.
     */
    public void registerMetrics(CircuitBreaker circuitBreaker) {
        String name = circuitBreaker.getName();
        Tags tags = Tags.of("name", name);

        // State gauge (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
        meterRegistry.gauge("resilience4j.circuitbreaker.state", tags, circuitBreaker,
            cb -> {
                switch (cb.getState()) {
                    case CLOSED: return 0;
                    case OPEN: return 1;
                    case HALF_OPEN: return 2;
                    default: return -1;
                }
            });

        // Failure rate gauge
        meterRegistry.gauge("resilience4j.circuitbreaker.failure.rate", tags, circuitBreaker,
            cb -> cb.getMetrics().getFailureRate());

        // Slow call rate gauge
        meterRegistry.gauge("resilience4j.circuitbreaker.slow.call.rate", tags, circuitBreaker,
            cb -> cb.getMetrics().getSlowCallRate());

        // Buffered calls gauge
        meterRegistry.gauge("resilience4j.circuitbreaker.buffered.calls", tags, circuitBreaker,
            cb -> cb.getMetrics().getNumberOfBufferedCalls());

        // Configuration metrics
        meterRegistry.gauge("resilience4j.circuitbreaker.sliding.window.size", tags, circuitBreaker,
            cb -> cb.getCircuitBreakerConfig().getSlidingWindowSize());

        meterRegistry.gauge("resilience4j.circuitbreaker.failure.threshold", tags, circuitBreaker,
            cb -> cb.getCircuitBreakerConfig().getFailureRateThreshold());

        meterRegistry.gauge("resilience4j.circuitbreaker.wait.duration", tags, circuitBreaker,
            cb -> cb.getCircuitBreakerConfig().getWaitIntervalFunctionInOpenState()
                .apply(1));

        // Add event listeners for counter metrics
        circuitBreaker.getEventPublisher()
            .onSuccess(event -> {
                meterRegistry.counter("resilience4j.circuitbreaker.calls",
                    tags.and("kind", "successful")).increment();
                meterRegistry.timer("resilience4j.circuitbreaker.calls.duration",
                    tags.and("outcome", "success"))
                    .record(event.getElapsedDuration());
            })
            .onError(event -> {
                meterRegistry.counter("resilience4j.circuitbreaker.calls",
                    tags.and("kind", "failed")).increment();
                meterRegistry.counter("resilience4j.circuitbreaker.calls.errors",
                    tags.and("error", event.getThrowable().getClass().getSimpleName()))
                    .increment();
                meterRegistry.timer("resilience4j.circuitbreaker.calls.duration",
                    tags.and("outcome", "error"))
                    .record(event.getElapsedDuration());
            })
            .onSlowCallRateExceeded(event -> {
                meterRegistry.counter("resilience4j.circuitbreaker.calls",
                    tags.and("kind", "slow")).increment();
            })
            .onCallNotPermitted(event -> {
                meterRegistry.counter("resilience4j.circuitbreaker.calls",
                    tags.and("kind", "not_permitted")).increment();
            })
            .onStateTransition(event -> {
                meterRegistry.counter("resilience4j.circuitbreaker.state.transition",
                    tags.and("from", event.getStateTransition().getFromState().toString())
                        .and("to", event.getStateTransition().getToState().toString()))
                    .increment();
            });

        log.info("Registered metrics for circuit breaker: {}", name);
    }

    private void onCircuitBreakerAdded(EntryAddedEvent<CircuitBreaker> event) {
        log.info("Circuit breaker added: {}", event.getAddedEntry().getName());
        registerMetrics(event.getAddedEntry());
    }

    private void onCircuitBreakerRemoved(EntryRemovedEvent<CircuitBreaker> event) {
        log.info("Circuit breaker removed: {}", event.getRemovedEntry().getName());
    }
}