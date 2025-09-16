package com.focushive.api.service;

import com.focushive.api.client.IdentityServiceClient;
import com.focushive.api.dto.identity.*;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced Identity Service Integration with comprehensive resilience patterns.
 *
 * Provides:
 * - Token validation with caching (5ms cached, 50ms primary)
 * - Circuit breaker protection
 * - Retry logic with exponential backoff
 * - Rate limiting
 * - Bulkhead isolation
 * - Graceful fallback mechanisms
 * - 80% cache hit ratio target
 *
 * Performance Requirements:
 * - Cached validation: < 5ms
 * - Primary validation: < 50ms
 * - Circuit breaker response: < 100ms
 * - 80% cache hit ratio target
 */
@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.context.annotation.Profile("!test") // Disable in test profile
public class IdentityIntegrationService {

    private static final String IDENTITY_SERVICE = "identity-service";
    private final IdentityServiceClient identityServiceClient;

    /**
     * Validates a JWT token with caching and resilience patterns.
     *
     * Performance:
     * - First call: ~50ms (network + validation)
     * - Cached calls: ~5ms (Redis lookup)
     *
     * @param token Bearer token to validate
     * @return Token validation response
     */
    @Cacheable(value = "identity-validation",
               key = "#token.hashCode()",
               unless = "#result == null or !#result.valid",
               condition = "#token != null and #token.startsWith('Bearer ')")
    @CircuitBreaker(name = IDENTITY_SERVICE, fallbackMethod = "validateTokenAsyncFallback")
    @Retry(name = IDENTITY_SERVICE)
    @TimeLimiter(name = IDENTITY_SERVICE)
    @RateLimiter(name = IDENTITY_SERVICE)
    @Bulkhead(name = IDENTITY_SERVICE)
    public CompletableFuture<TokenValidationResponse> validateTokenAsync(String token) {
        log.debug("Validating token with Identity Service (async): {}", token.substring(0, 20) + "...");

        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.nanoTime();
            try {
                TokenValidationResponse response = identityServiceClient.validateToken(token);
                long endTime = System.nanoTime();
                long duration = (endTime - startTime) / 1_000_000; // Convert to milliseconds

                log.debug("Token validation completed in {}ms, valid: {}", duration, response.isValid());

                // Log performance metric for monitoring
                if (duration > 50) {
                    log.warn("Token validation exceeded 50ms threshold: {}ms", duration);
                }

                return response;
            } catch (Exception e) {
                long endTime = System.nanoTime();
                long duration = (endTime - startTime) / 1_000_000;
                log.error("Token validation failed after {}ms: {}", duration, e.getMessage());
                throw e;
            }
        });
    }

    /**
     * Synchronous token validation with caching and resilience patterns.
     * Uses the async method internally but blocks for compatibility.
     */
    @CircuitBreaker(name = IDENTITY_SERVICE, fallbackMethod = "validateTokenFallback")
    public TokenValidationResponse validateToken(String token) {
        try {
            return validateTokenAsync(token).get();
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return validateTokenFallback(token, e);
        }
    }

    /**
     * Retrieves user information with caching.
     */
    @Cacheable(value = "user-info", key = "#userId", unless = "#result == null")
    @CircuitBreaker(name = IDENTITY_SERVICE, fallbackMethod = "getUserAsyncFallback")
    @Retry(name = IDENTITY_SERVICE)
    @TimeLimiter(name = IDENTITY_SERVICE)
    @RateLimiter(name = IDENTITY_SERVICE)
    @Bulkhead(name = IDENTITY_SERVICE)
    public CompletableFuture<UserDto> getUserAsync(UUID userId) {
        log.debug("Fetching user information from Identity Service: {}", userId);

        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.nanoTime();
            try {
                UserDto user = identityServiceClient.getUser(userId);
                long endTime = System.nanoTime();
                long duration = (endTime - startTime) / 1_000_000;

                log.debug("User info retrieval completed in {}ms for user: {}", duration, userId);
                return user;
            } catch (Exception e) {
                long endTime = System.nanoTime();
                long duration = (endTime - startTime) / 1_000_000;
                log.error("User info retrieval failed after {}ms for user {}: {}", duration, userId, e.getMessage());
                throw e;
            }
        });
    }

    /**
     * Synchronous user retrieval.
     */
    public UserDto getUser(UUID userId) {
        try {
            return getUserAsync(userId).get();
        } catch (Exception e) {
            log.error("User retrieval failed for {}: {}", userId, e.getMessage());
            return getUserFallback(userId, e);
        }
    }

    /**
     * Retrieves user by email with caching.
     */
    @Cacheable(value = "user-by-email", key = "#email", unless = "#result == null")
    @CircuitBreaker(name = IDENTITY_SERVICE, fallbackMethod = "getUserByEmailFallback")
    @Retry(name = IDENTITY_SERVICE)
    @TimeLimiter(name = IDENTITY_SERVICE)
    @RateLimiter(name = IDENTITY_SERVICE)
    @Bulkhead(name = IDENTITY_SERVICE)
    public CompletableFuture<UserDto> getUserByEmailAsync(String email) {
        log.debug("Fetching user by email from Identity Service: {}", email);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return identityServiceClient.getUserByEmail(email);
            } catch (Exception e) {
                log.error("User retrieval by email failed for {}: {}", email, e.getMessage());
                throw e;
            }
        });
    }

    /**
     * Retrieves user personas with caching.
     */
    @Cacheable(value = "user-personas", key = "#userId", unless = "#result == null")
    @CircuitBreaker(name = IDENTITY_SERVICE, fallbackMethod = "getUserPersonasFallback")
    @Retry(name = IDENTITY_SERVICE)
    @TimeLimiter(name = IDENTITY_SERVICE)
    @RateLimiter(name = IDENTITY_SERVICE)
    @Bulkhead(name = IDENTITY_SERVICE)
    public CompletableFuture<PersonaListResponse> getUserPersonasAsync(UUID userId) {
        log.debug("Fetching user personas from Identity Service: {}", userId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return identityServiceClient.getUserPersonas(userId);
            } catch (Exception e) {
                log.error("User personas retrieval failed for {}: {}", userId, e.getMessage());
                throw e;
            }
        });
    }

    /**
     * Gets active persona for user with caching.
     */
    @Cacheable(value = "active-persona", key = "#userId", unless = "#result == null")
    @CircuitBreaker(name = IDENTITY_SERVICE, fallbackMethod = "getActivePersonaFallback")
    @Retry(name = IDENTITY_SERVICE)
    @TimeLimiter(name = IDENTITY_SERVICE)
    @RateLimiter(name = IDENTITY_SERVICE)
    @Bulkhead(name = IDENTITY_SERVICE)
    public CompletableFuture<PersonaDto> getActivePersonaAsync(UUID userId) {
        log.debug("Fetching active persona from Identity Service: {}", userId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return identityServiceClient.getActivePersona(userId);
            } catch (Exception e) {
                log.error("Active persona retrieval failed for {}: {}", userId, e.getMessage());
                throw e;
            }
        });
    }

    /**
     * Checks Identity Service health.
     */
    @CircuitBreaker(name = IDENTITY_SERVICE, fallbackMethod = "checkHealthFallback")
    @TimeLimiter(name = IDENTITY_SERVICE)
    public CompletableFuture<HealthResponse> checkHealthAsync() {
        log.debug("Checking Identity Service health");

        return CompletableFuture.supplyAsync(() -> {
            try {
                return identityServiceClient.checkHealth();
            } catch (Exception e) {
                log.error("Identity Service health check failed: {}", e.getMessage());
                throw e;
            }
        });
    }

    /**
     * Evicts cached validation for a token (e.g., on logout).
     */
    @CacheEvict(value = "identity-validation", key = "#token.hashCode()")
    public void evictTokenValidation(String token) {
        log.debug("Evicting token validation cache for token: {}", token.substring(0, 20) + "...");
    }

    /**
     * Evicts all user-related caches.
     */
    @CacheEvict(value = {"user-info", "user-by-email", "user-personas", "active-persona"}, key = "#userId")
    public void evictUserCaches(UUID userId) {
        log.debug("Evicting all user caches for user: {}", userId);
    }

    // ========== FALLBACK METHODS ==========

    /**
     * Fallback method for token validation when Identity Service is unavailable.
     * Returns invalid token response with clear error message.
     */
    public TokenValidationResponse validateTokenFallback(String token, Exception ex) {
        log.error("Token validation fallback triggered due to: {}", ex.getMessage());

        return TokenValidationResponse.builder()
            .valid(false)
            .errorMessage("Identity Service unavailable - token validation failed")
            .build();
    }

    /**
     * Fallback method for async token validation.
     */
    public CompletableFuture<TokenValidationResponse> validateTokenAsyncFallback(String token, Exception ex) {
        return CompletableFuture.completedFuture(validateTokenFallback(token, ex));
    }

    /**
     * Fallback method for user retrieval.
     */
    public UserDto getUserFallback(UUID userId, Exception ex) {
        log.error("User retrieval fallback triggered for user {} due to: {}", userId, ex.getMessage());
        return null; // Graceful degradation - return null when service unavailable
    }

    /**
     * Fallback method for async user retrieval.
     */
    public CompletableFuture<UserDto> getUserAsyncFallback(UUID userId, Exception ex) {
        return CompletableFuture.completedFuture(getUserFallback(userId, ex));
    }

    /**
     * Fallback method for user by email retrieval.
     */
    public CompletableFuture<UserDto> getUserByEmailFallback(String email, Exception ex) {
        log.error("User by email retrieval fallback triggered for {} due to: {}", email, ex.getMessage());
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Fallback method for user personas retrieval.
     */
    public CompletableFuture<PersonaListResponse> getUserPersonasFallback(UUID userId, Exception ex) {
        log.error("User personas retrieval fallback triggered for {} due to: {}", userId, ex.getMessage());
        return CompletableFuture.completedFuture(
            PersonaListResponse.builder()
                .personas(java.util.Collections.emptyList())
                .totalCount(0)
                .build()
        );
    }

    /**
     * Fallback method for active persona retrieval.
     */
    public CompletableFuture<PersonaDto> getActivePersonaFallback(UUID userId, Exception ex) {
        log.error("Active persona retrieval fallback triggered for {} due to: {}", userId, ex.getMessage());
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Fallback method for health check.
     */
    public CompletableFuture<HealthResponse> checkHealthFallback(Exception ex) {
        log.error("Identity Service health check fallback triggered due to: {}", ex.getMessage());
        return CompletableFuture.completedFuture(
            HealthResponse.builder()
                .status("DOWN")
                .version("unknown")
                .build()
        );
    }
}