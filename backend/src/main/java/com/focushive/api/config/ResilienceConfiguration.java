package com.focushive.api.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for Resilience4j patterns used in inter-service communication.
 * Provides circuit breaker, retry, rate limiter, time limiter, and bulkhead patterns.
 */
@Slf4j
@Configuration
@Profile("!test")
@RequiredArgsConstructor
public class ResilienceConfiguration {
    
    private static final String IDENTITY_SERVICE = "identity-service";
    
    /**
     * Configure circuit breaker event listeners for monitoring.
     */
    @Bean
    public CircuitBreaker identityServiceCircuitBreaker(
            io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry circuitBreakerRegistry) {
        
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(IDENTITY_SERVICE);
        
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.info("Identity Service Circuit Breaker state transition: {} -> {}", 
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()))
            .onFailureRateExceeded(event ->
                log.warn("Identity Service Circuit Breaker failure rate exceeded: {}%",
                        event.getFailureRate()))
            .onSlowCallRateExceeded(event ->
                log.warn("Identity Service Circuit Breaker slow call rate exceeded: {}%",
                        event.getSlowCallRate()))
            .onCallNotPermitted(event ->
                log.warn("Identity Service Circuit Breaker call not permitted"))
            .onError(event ->
                log.error("Identity Service Circuit Breaker error: {}",
                        event.getThrowable().getMessage()));
        
        return circuitBreaker;
    }
    
    /**
     * Configure retry event listeners for monitoring.
     */
    @Bean
    public Retry identityServiceRetry(
            io.github.resilience4j.retry.RetryRegistry retryRegistry) {
        
        Retry retry = retryRegistry.retry(IDENTITY_SERVICE);
        
        retry.getEventPublisher()
            .onRetry(event ->
                log.warn("Identity Service retry attempt {} for call: {}",
                        event.getNumberOfRetryAttempts(),
                        event.getCreationTime()))
            .onError(event ->
                log.error("Identity Service retry failed after {} attempts: {}",
                        event.getNumberOfRetryAttempts(),
                        event.getLastThrowable().getMessage()));
        
        return retry;
    }
    
    /**
     * Configure rate limiter event listeners for monitoring.
     */
    @Bean
    public RateLimiter identityServiceRateLimiter(
            io.github.resilience4j.ratelimiter.RateLimiterRegistry rateLimiterRegistry) {
        
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(IDENTITY_SERVICE);
        
        rateLimiter.getEventPublisher()
            .onSuccess(event ->
                log.debug("Identity Service rate limiter call succeeded"))
            .onFailure(event ->
                log.warn("Identity Service rate limiter call rejected"));
        
        return rateLimiter;
    }
    
    /**
     * Configure time limiter for timeout handling.
     */
    @Bean
    public TimeLimiter identityServiceTimeLimiter(
            io.github.resilience4j.timelimiter.TimeLimiterRegistry timeLimiterRegistry) {
        
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter(IDENTITY_SERVICE);
        
        timeLimiter.getEventPublisher()
            .onTimeout(event ->
                log.warn("Identity Service call timed out: {}", 
                        event.getTimeLimiterName()));
        
        return timeLimiter;
    }
    
    /**
     * Configure bulkhead for concurrent call limiting.
     */
    @Bean
    public Bulkhead identityServiceBulkhead(
            io.github.resilience4j.bulkhead.BulkheadRegistry bulkheadRegistry) {
        
        Bulkhead bulkhead = bulkheadRegistry.bulkhead(IDENTITY_SERVICE);
        
        bulkhead.getEventPublisher()
            .onCallPermitted(event ->
                log.debug("Identity Service bulkhead call permitted"))
            .onCallRejected(event ->
                log.warn("Identity Service bulkhead call rejected"));
        
        return bulkhead;
    }
}