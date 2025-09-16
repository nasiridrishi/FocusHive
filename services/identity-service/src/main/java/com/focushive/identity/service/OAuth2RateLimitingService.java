package com.focushive.identity.service;

import com.focushive.identity.annotation.RateLimit;
import com.focushive.identity.annotation.RateLimit.RateLimitType;
import com.focushive.identity.entity.OAuthClient;
import com.focushive.identity.exception.RateLimitExceededException;
import com.focushive.identity.metrics.OAuth2MetricsService;
import com.focushive.identity.repository.OAuthClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * OAuth2-specific rate limiting service.
 * Enforces per-client and per-endpoint rate limits for OAuth2 operations.
 *
 * Rate limit hierarchy:
 * 1. Client-specific limits (if configured)
 * 2. Default endpoint limits
 * 3. Global fallback limits
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2RateLimitingService {

    private final IRateLimiter rateLimiter;
    private final OAuthClientRepository clientRepository;
    private final OAuth2MetricsService metricsService;
    private final OAuth2AuditService auditService;

    // Default rate limits for OAuth2 endpoints
    @Value("${oauth2.rate-limit.authorize:10}")
    private int authorizeRateLimit;

    @Value("${oauth2.rate-limit.authorize.window:1}")
    private int authorizeWindowMinutes;

    @Value("${oauth2.rate-limit.token:60}")
    private int tokenRateLimit;

    @Value("${oauth2.rate-limit.token.window:1}")
    private int tokenWindowMinutes;

    @Value("${oauth2.rate-limit.introspect:100}")
    private int introspectRateLimit;

    @Value("${oauth2.rate-limit.introspect.window:1}")
    private int introspectWindowMinutes;

    @Value("${oauth2.rate-limit.revoke:30}")
    private int revokeRateLimit;

    @Value("${oauth2.rate-limit.revoke.window:1}")
    private int revokeWindowMinutes;

    @Value("${oauth2.rate-limit.userinfo:60}")
    private int userInfoRateLimit;

    @Value("${oauth2.rate-limit.userinfo.window:1}")
    private int userInfoWindowMinutes;

    @Value("${oauth2.rate-limit.client.burst:3}")
    private int clientBurstMultiplier;

    // Endpoint types for rate limiting
    public enum OAuth2Endpoint {
        AUTHORIZE("authorize", 10, 1),
        TOKEN("token", 60, 1),
        INTROSPECT("introspect", 100, 1),
        REVOKE("revoke", 30, 1),
        USERINFO("userinfo", 60, 1),
        JWKS("jwks", 100, 5),
        DISCOVERY("discovery", 100, 5),
        CLIENT_REGISTRATION("client_registration", 5, 10);

        private final String name;
        private final int defaultLimit;
        private final int defaultWindowMinutes;

        OAuth2Endpoint(String name, int defaultLimit, int defaultWindowMinutes) {
            this.name = name;
            this.defaultLimit = defaultLimit;
            this.defaultWindowMinutes = defaultWindowMinutes;
        }
    }

    /**
     * Check if a request from a client to an endpoint is allowed.
     *
     * @param clientId OAuth2 client ID
     * @param endpoint OAuth2 endpoint being accessed
     * @param ipAddress IP address of the request
     * @return true if allowed, false if rate limited
     */
    public boolean checkClientRateLimit(String clientId, OAuth2Endpoint endpoint, String ipAddress) {
        if (clientId == null || clientId.trim().isEmpty()) {
            // For unauthenticated requests, use IP-based rate limiting with stricter limits
            return checkAnonymousRateLimit(endpoint, ipAddress);
        }

        try {
            // Check if client is suspended first
            if (isClientSuspended(clientId)) {
                log.warn("Suspended client {} attempted to access endpoint {}", clientId, endpoint.name);
                auditService.logRateLimitExceeded(clientId, ipAddress, endpoint.name);
                metricsService.recordRateLimitExceeded(clientId);
                return false;
            }

            // Get client-specific rate limits if configured
            Optional<OAuthClient> clientOpt = clientRepository.findByClientId(clientId);

            int limit = getClientRateLimit(clientOpt, endpoint);
            int windowMinutes = getClientWindowMinutes(clientOpt, endpoint);

            // Create rate limit key combining client and endpoint
            String rateLimitKey = String.format("oauth2:%s:%s", clientId, endpoint.name);

            // Create dynamic rate limit annotation
            RateLimit rateLimit = createRateLimit(limit, windowMinutes);

            boolean allowed = rateLimiter.isAllowed(rateLimitKey, rateLimit);

            if (!allowed) {
                // Record rate limit exceeded event
                metricsService.recordRateLimitExceeded(clientId);
                auditService.logRateLimitExceeded(clientId, ipAddress, endpoint.name);

                // Check if client should be temporarily suspended
                checkForSuspension(clientId, endpoint, ipAddress);

                log.warn("Rate limit exceeded for client {} on endpoint {}", clientId, endpoint.name);
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Error checking rate limit for client {} on endpoint {}", clientId, endpoint.name, e);
            // Allow request in case of error to maintain availability
            return true;
        }
    }

    /**
     * Check rate limit for anonymous requests (no client ID).
     */
    private boolean checkAnonymousRateLimit(OAuth2Endpoint endpoint, String ipAddress) {
        // Use stricter limits for anonymous requests
        int limit = endpoint.defaultLimit / 2; // Half the normal limit
        int windowMinutes = endpoint.defaultWindowMinutes;

        String rateLimitKey = String.format("oauth2:anonymous:%s:%s", ipAddress, endpoint.name);
        RateLimit rateLimit = createRateLimit(limit, windowMinutes);

        boolean allowed = rateLimiter.isAllowed(rateLimitKey, rateLimit);

        if (!allowed) {
            log.warn("Anonymous rate limit exceeded for IP {} on endpoint {}", ipAddress, endpoint.name);
            metricsService.recordRateLimitExceeded("anonymous");
            auditService.logRateLimitExceeded("anonymous", ipAddress, endpoint.name);
        }

        return allowed;
    }

    /**
     * Get the rate limit for a client on a specific endpoint.
     */
    private int getClientRateLimit(Optional<OAuthClient> clientOpt, OAuth2Endpoint endpoint) {
        if (clientOpt.isEmpty()) {
            return getDefaultLimit(endpoint);
        }

        OAuthClient client = clientOpt.get();

        // Check for client-specific rate limits
        if (client.getRateLimitOverride() != null && client.getRateLimitOverride() > 0) {
            return client.getRateLimitOverride();
        }

        // Check for premium/trusted clients
        if (client.isTrusted()) {
            return getDefaultLimit(endpoint) * 2; // Double limits for trusted clients
        }

        return getDefaultLimit(endpoint);
    }

    /**
     * Get the rate limit window for a client on a specific endpoint.
     */
    private int getClientWindowMinutes(Optional<OAuthClient> clientOpt, OAuth2Endpoint endpoint) {
        if (clientOpt.isPresent()) {
            OAuthClient client = clientOpt.get();
            if (client.getRateLimitWindowMinutes() != null && client.getRateLimitWindowMinutes() > 0) {
                return client.getRateLimitWindowMinutes();
            }
        }
        return getDefaultWindowMinutes(endpoint);
    }

    /**
     * Get default rate limit for an endpoint.
     */
    private int getDefaultLimit(OAuth2Endpoint endpoint) {
        switch (endpoint) {
            case AUTHORIZE:
                return authorizeRateLimit;
            case TOKEN:
                return tokenRateLimit;
            case INTROSPECT:
                return introspectRateLimit;
            case REVOKE:
                return revokeRateLimit;
            case USERINFO:
                return userInfoRateLimit;
            default:
                return endpoint.defaultLimit;
        }
    }

    /**
     * Get default window minutes for an endpoint.
     */
    private int getDefaultWindowMinutes(OAuth2Endpoint endpoint) {
        switch (endpoint) {
            case AUTHORIZE:
                return authorizeWindowMinutes;
            case TOKEN:
                return tokenWindowMinutes;
            case INTROSPECT:
                return introspectWindowMinutes;
            case REVOKE:
                return revokeWindowMinutes;
            case USERINFO:
                return userInfoWindowMinutes;
            default:
                return endpoint.defaultWindowMinutes;
        }
    }

    /**
     * Create a dynamic RateLimit annotation.
     */
    private RateLimit createRateLimit(final int limit, final int windowMinutes) {
        return new RateLimit() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return RateLimit.class;
            }

            @Override
            public int value() {
                return limit;
            }

            @Override
            public long window() {
                return windowMinutes;
            }

            @Override
            public TimeUnit timeUnit() {
                return TimeUnit.MINUTES;
            }

            @Override
            public RateLimitType type() {
                return RateLimitType.IP;
            }

            @Override
            public String keyPrefix() {
                return "";
            }

            @Override
            public boolean skipAuthenticated() {
                return false;
            }

            @Override
            public String message() {
                return "";
            }

            @Override
            public boolean progressivePenalties() {
                return true;
            }
        };
    }

    /**
     * Check if a client should be temporarily suspended for excessive rate limit violations.
     */
    private void checkForSuspension(String clientId, OAuth2Endpoint endpoint, String ipAddress) {
        String suspensionKey = String.format("oauth2:suspension:%s", clientId);
        String violationKey = String.format("oauth2:violations:%s:%s", clientId, endpoint.name);

        // Increment violation count
        long violations = rateLimiter.incrementViolationCount(violationKey);

        // If violations exceed threshold, suspend client temporarily
        if (violations > 10) { // 10 violations in a window triggers suspension
            long suspensionDuration = Math.min(violations * 60, 3600); // Max 1 hour suspension
            rateLimiter.suspendClient(suspensionKey, suspensionDuration);

            log.error("Client {} suspended for {} seconds due to excessive rate limit violations",
                     clientId, suspensionDuration);

            auditService.logSecurityAlert(
                com.focushive.identity.audit.OAuth2AuditEvent.EventType.RATE_LIMIT_EXCEEDED,
                "Client suspended for excessive rate limit violations",
                ipAddress,
                clientId,
                java.util.Map.of(
                    "violations", violations,
                    "suspensionSeconds", suspensionDuration,
                    "endpoint", endpoint.name
                )
            );
        }
    }

    /**
     * Check if a client is currently suspended.
     */
    public boolean isClientSuspended(String clientId) {
        String suspensionKey = String.format("oauth2:suspension:%s", clientId);
        return rateLimiter.isSuspended(suspensionKey);
    }

    /**
     * Get remaining requests for a client on an endpoint.
     */
    public long getRemainingRequests(String clientId, OAuth2Endpoint endpoint) {
        Optional<OAuthClient> clientOpt = clientRepository.findByClientId(clientId);
        int limit = getClientRateLimit(clientOpt, endpoint);
        int windowMinutes = getClientWindowMinutes(clientOpt, endpoint);

        String rateLimitKey = String.format("oauth2:%s:%s", clientId, endpoint.name);
        RateLimit rateLimit = createRateLimit(limit, windowMinutes);

        return rateLimiter.getRemainingTokens(rateLimitKey, rateLimit);
    }

    /**
     * Get rate limit reset time for a client on an endpoint.
     */
    public long getResetTime(String clientId, OAuth2Endpoint endpoint) {
        Optional<OAuthClient> clientOpt = clientRepository.findByClientId(clientId);
        int windowMinutes = getClientWindowMinutes(clientOpt, endpoint);

        String rateLimitKey = String.format("oauth2:%s:%s", clientId, endpoint.name);
        return rateLimiter.getResetTime(rateLimitKey, windowMinutes);
    }

    /**
     * Reset rate limit for a client (admin operation).
     */
    public void resetClientRateLimit(String clientId) {
        for (OAuth2Endpoint endpoint : OAuth2Endpoint.values()) {
            String rateLimitKey = String.format("oauth2:%s:%s", clientId, endpoint.name);
            rateLimiter.resetLimit(rateLimitKey);
        }

        // Clear violation counts
        String violationPattern = String.format("oauth2:violations:%s:*", clientId);
        rateLimiter.clearViolations(violationPattern);

        // Clear suspension
        String suspensionKey = String.format("oauth2:suspension:%s", clientId);
        rateLimiter.clearSuspension(suspensionKey);

        log.info("Rate limits reset for client: {}", clientId);
    }
}