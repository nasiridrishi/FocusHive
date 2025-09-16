package com.focushive.notification.service;

import com.focushive.notification.dto.UserInfo;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Resilient client for Identity Service with circuit breaker protection.
 * Provides fallback mechanisms for identity service failures.
 */
@Slf4j
@Service
public class ResilientIdentityServiceClient {

    private final RestTemplate restTemplate;
    private final CircuitBreaker identityServiceCircuitBreaker;
    private final String identityServiceUrl;
    private final MeterRegistry meterRegistry;

    // Cache for user information
    private final ConcurrentHashMap<String, UserInfo> userCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> rolesCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, Object>> preferencesCache = new ConcurrentHashMap<>();

    public ResilientIdentityServiceClient(RestTemplate restTemplate,
                                         CircuitBreaker identityServiceCircuitBreaker,
                                         @Value("${identity.service.url:http://localhost:8081/identity}") String identityServiceUrl,
                                         MeterRegistry meterRegistry) {
        this.restTemplate = restTemplate;
        this.identityServiceCircuitBreaker = identityServiceCircuitBreaker;
        this.identityServiceUrl = identityServiceUrl;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Get user information with circuit breaker protection.
     */
    public Optional<UserInfo> getUserInfo(String userId) {
        try {
            return Optional.ofNullable(identityServiceCircuitBreaker.executeSupplier(() -> {
                String url = identityServiceUrl + "/users/" + userId;
                UserInfo userInfo = restTemplate.getForObject(url, UserInfo.class);

                // Cache successful response
                if (userInfo != null) {
                    userInfo.setLastUpdated(System.currentTimeMillis());
                    userCache.put(userId, userInfo);
                    recordCacheUpdate(userId);
                }

                identityServiceCircuitBreaker.onSuccess(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                return userInfo;
            }));
        } catch (CallNotPermittedException e) {
            log.warn("Circuit breaker is open for identity service, using cached data");
            return getCachedUserInfo(userId);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().is4xxClientError()) {
                // Don't trigger circuit breaker for client errors
                log.debug("Client error from identity service: {}", e.getMessage());
                return Optional.empty();
            }
            throw e;
        } catch (Exception e) {
            log.error("Failed to get user info for user: {}", userId, e);
            identityServiceCircuitBreaker.onError(System.currentTimeMillis(), TimeUnit.MILLISECONDS, e);
            return getCachedUserInfo(userId);
        }
    }

    /**
     * Get cached user information.
     */
    public Optional<UserInfo> getCachedUserInfo(String userId) {
        UserInfo cached = userCache.get(userId);
        if (cached != null) {
            cached.setStale(true); // Mark as stale
            meterRegistry.counter("circuit.breaker.cache.hit", "service", "identity-service").increment();
            log.info("Returning cached user info for user: {}", userId);
            return Optional.of(cached);
        }

        // Return default fallback user info
        UserInfo fallback = UserInfo.builder()
            .userId(userId)
            .email("cached-user@example.com")
            .name("Cached User")
            .stale(true)
            .lastUpdated(System.currentTimeMillis())
            .build();

        meterRegistry.counter("circuit.breaker.cache.miss", "service", "identity-service").increment();
        return Optional.of(fallback);
    }

    /**
     * Validate JWT token with circuit breaker.
     */
    public boolean validateToken(String token) {
        try {
            return identityServiceCircuitBreaker.executeSupplier(() -> {
                String url = identityServiceUrl + "/validate";
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<Boolean> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Boolean.class);

                return Boolean.TRUE.equals(response.getBody());
            });
        } catch (CallNotPermittedException e) {
            log.warn("Circuit breaker is open, rejecting token validation");
            return false; // Fail-safe: reject when can't validate
        } catch (Exception e) {
            log.error("Failed to validate token", e);
            return false;
        }
    }

    /**
     * Get user roles with fallback.
     */
    public List<String> getUserRoles(String userId) {
        try {
            return identityServiceCircuitBreaker.executeSupplier(() -> {
                String url = identityServiceUrl + "/users/" + userId + "/roles";
                String[] roles = restTemplate.getForObject(url, String[].class);
                List<String> rolesList = roles != null ? Arrays.asList(roles) : Collections.emptyList();

                // Cache roles
                rolesCache.put(userId, rolesList);

                return rolesList;
            });
        } catch (CallNotPermittedException e) {
            log.warn("Circuit breaker is open, returning default roles");
            return getDefaultRoles(userId);
        } catch (Exception e) {
            log.error("Failed to get user roles", e);
            return getDefaultRoles(userId);
        }
    }

    /**
     * Get default roles fallback.
     */
    private List<String> getDefaultRoles(String userId) {
        // Check cache first
        List<String> cached = rolesCache.get(userId);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        // Return default role
        return Collections.singletonList("ROLE_USER");
    }

    /**
     * Get user preferences with fallback.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserPreferences(String userId) {
        try {
            return identityServiceCircuitBreaker.executeSupplier(() -> {
                String url = identityServiceUrl + "/users/" + userId + "/preferences";
                Map<String, Object> preferences = restTemplate.getForObject(url, Map.class);

                if (preferences != null) {
                    preferencesCache.put(userId, preferences);
                }

                return preferences != null ? preferences : getDefaultPreferences();
            });
        } catch (CallNotPermittedException e) {
            log.warn("Circuit breaker is open, returning default preferences");
            return getDefaultPreferences();
        } catch (Exception e) {
            log.error("Failed to get user preferences", e);
            return getDefaultPreferences();
        }
    }

    /**
     * Get default preferences.
     */
    private Map<String, Object> getDefaultPreferences() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("emailNotifications", true);
        defaults.put("pushNotifications", false);
        defaults.put("language", "en");
        defaults.put("timezone", "UTC");
        return defaults;
    }

    /**
     * Record cache update for metrics.
     */
    private void recordCacheUpdate(String userId) {
        meterRegistry.counter("identity.cache.update", "userId", userId).increment();
    }

    /**
     * Get health status with fallback.
     */
    public HealthStatus getHealthWithFallback() {
        try {
            return identityServiceCircuitBreaker.executeSupplier(() -> {
                String url = identityServiceUrl + "/health";
                return restTemplate.getForObject(url, HealthStatus.class);
            });
        } catch (CallNotPermittedException e) {
            return HealthStatus.degraded("Circuit open - using cached data");
        } catch (Exception e) {
            return HealthStatus.down("Service unavailable");
        }
    }

    /**
     * Get bulk users with fallback.
     */
    public Map<String, UserInfo> getBulkUsersWithFallback(List<String> userIds) {
        Map<String, UserInfo> users = new HashMap<>();

        for (String userId : userIds) {
            Optional<UserInfo> userInfo = getUserInfo(userId);
            if (userInfo.isPresent()) {
                users.put(userId, userInfo.get());
            } else {
                // Add fallback user info
                users.put(userId, UserInfo.builder()
                    .userId(userId)
                    .name("Unknown User")
                    .stale(true)
                    .build());
            }
        }

        return users;
    }

    /**
     * Health status class.
     */
    public static class HealthStatus {
        public enum Status { UP, DOWN, DEGRADED }

        private Status status;
        private String message;
        private long lastSuccessfulCheck;

        public HealthStatus() {}

        public HealthStatus(Status status, String message) {
            this.status = status;
            this.message = message;
            this.lastSuccessfulCheck = System.currentTimeMillis();
        }

        public static HealthStatus degraded(String message) {
            return new HealthStatus(Status.DEGRADED, message);
        }

        public static HealthStatus down(String message) {
            return new HealthStatus(Status.DOWN, message);
        }

        // Getters and setters
        public Status getStatus() { return status; }
        public void setStatus(Status status) { this.status = status; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public long getLastSuccessfulCheck() { return lastSuccessfulCheck; }
        public void setLastSuccessfulCheck(long lastSuccessfulCheck) { this.lastSuccessfulCheck = lastSuccessfulCheck; }
    }
}