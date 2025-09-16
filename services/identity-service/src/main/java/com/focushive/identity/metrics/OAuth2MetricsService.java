package com.focushive.identity.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for tracking OAuth2 metrics with Prometheus.
 * Provides comprehensive metrics for monitoring OAuth2 operations and performance.
 */
@Slf4j
@Service
public class OAuth2MetricsService {

    private final MeterRegistry meterRegistry;

    // Metric name prefixes
    private static final String METRIC_PREFIX = "oauth2";

    // Authorization metrics
    private final Counter authorizationRequests;
    private final Counter authorizationSuccess;
    private final Counter authorizationFailures;
    private final Timer authorizationDuration;

    // Token metrics
    private final Counter tokenRequests;
    private final Counter tokenIssuance;
    private final Counter tokenRefresh;
    private final Counter tokenRevocation;
    private final Timer tokenGenerationDuration;

    // Client credentials metrics
    private final Counter clientAuthSuccess;
    private final Counter clientAuthFailures;

    // Introspection metrics
    private final Counter introspectionRequests;
    private final Counter introspectionActive;
    private final Counter introspectionInactive;
    private final Timer introspectionDuration;

    // UserInfo metrics
    private final Counter userInfoRequests;
    private final Counter userInfoSuccess;
    private final Counter userInfoFailures;
    private final Timer userInfoDuration;

    // Security metrics
    private final Counter suspiciousActivities;
    private final Counter rateLimitExceeded;
    private final Counter pkceValidationFailures;
    private final Counter invalidScopeRequests;

    // Performance metrics
    private final AtomicLong activeTokens;
    private final AtomicLong activeRefreshTokens;

    // Client-specific metrics cache
    private final ConcurrentHashMap<String, ClientMetrics> clientMetrics = new ConcurrentHashMap<>();

    public OAuth2MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize authorization metrics
        this.authorizationRequests = Counter.builder(METRIC_PREFIX + ".authorization.requests")
            .description("Total number of authorization requests")
            .register(meterRegistry);

        this.authorizationSuccess = Counter.builder(METRIC_PREFIX + ".authorization.success")
            .description("Number of successful authorizations")
            .register(meterRegistry);

        this.authorizationFailures = Counter.builder(METRIC_PREFIX + ".authorization.failures")
            .description("Number of failed authorizations")
            .register(meterRegistry);

        this.authorizationDuration = Timer.builder(METRIC_PREFIX + ".authorization.duration")
            .description("Authorization request processing time")
            .publishPercentileHistogram()
            .register(meterRegistry);

        // Initialize token metrics
        this.tokenRequests = Counter.builder(METRIC_PREFIX + ".token.requests")
            .description("Total number of token requests")
            .register(meterRegistry);

        this.tokenIssuance = Counter.builder(METRIC_PREFIX + ".token.issued")
            .description("Number of tokens issued")
            .register(meterRegistry);

        this.tokenRefresh = Counter.builder(METRIC_PREFIX + ".token.refresh")
            .description("Number of tokens refreshed")
            .register(meterRegistry);

        this.tokenRevocation = Counter.builder(METRIC_PREFIX + ".token.revoked")
            .description("Number of tokens revoked")
            .register(meterRegistry);

        this.tokenGenerationDuration = Timer.builder(METRIC_PREFIX + ".token.generation.duration")
            .description("Token generation time")
            .publishPercentileHistogram()
            .register(meterRegistry);

        // Initialize client authentication metrics
        this.clientAuthSuccess = Counter.builder(METRIC_PREFIX + ".client.auth.success")
            .description("Successful client authentications")
            .register(meterRegistry);

        this.clientAuthFailures = Counter.builder(METRIC_PREFIX + ".client.auth.failures")
            .description("Failed client authentications")
            .register(meterRegistry);

        // Initialize introspection metrics
        this.introspectionRequests = Counter.builder(METRIC_PREFIX + ".introspection.requests")
            .description("Total introspection requests")
            .register(meterRegistry);

        this.introspectionActive = Counter.builder(METRIC_PREFIX + ".introspection.active")
            .description("Introspection requests returning active tokens")
            .register(meterRegistry);

        this.introspectionInactive = Counter.builder(METRIC_PREFIX + ".introspection.inactive")
            .description("Introspection requests returning inactive tokens")
            .register(meterRegistry);

        this.introspectionDuration = Timer.builder(METRIC_PREFIX + ".introspection.duration")
            .description("Introspection request processing time")
            .publishPercentileHistogram()
            .register(meterRegistry);

        // Initialize UserInfo metrics
        this.userInfoRequests = Counter.builder(METRIC_PREFIX + ".userinfo.requests")
            .description("Total UserInfo requests")
            .register(meterRegistry);

        this.userInfoSuccess = Counter.builder(METRIC_PREFIX + ".userinfo.success")
            .description("Successful UserInfo requests")
            .register(meterRegistry);

        this.userInfoFailures = Counter.builder(METRIC_PREFIX + ".userinfo.failures")
            .description("Failed UserInfo requests")
            .register(meterRegistry);

        this.userInfoDuration = Timer.builder(METRIC_PREFIX + ".userinfo.duration")
            .description("UserInfo request processing time")
            .publishPercentileHistogram()
            .register(meterRegistry);

        // Initialize security metrics
        this.suspiciousActivities = Counter.builder(METRIC_PREFIX + ".security.suspicious")
            .description("Suspicious activity detections")
            .register(meterRegistry);

        this.rateLimitExceeded = Counter.builder(METRIC_PREFIX + ".security.ratelimit")
            .description("Rate limit exceeded events")
            .register(meterRegistry);

        this.pkceValidationFailures = Counter.builder(METRIC_PREFIX + ".security.pkce.failures")
            .description("PKCE validation failures")
            .register(meterRegistry);

        this.invalidScopeRequests = Counter.builder(METRIC_PREFIX + ".security.invalid.scope")
            .description("Invalid scope requests")
            .register(meterRegistry);

        // Initialize gauges for active tokens
        this.activeTokens = new AtomicLong(0);
        meterRegistry.gauge(METRIC_PREFIX + ".tokens.active", activeTokens);

        this.activeRefreshTokens = new AtomicLong(0);
        meterRegistry.gauge(METRIC_PREFIX + ".tokens.refresh.active", activeRefreshTokens);
    }

    // Authorization metrics methods
    public void recordAuthorizationRequest(String clientId) {
        authorizationRequests.increment();
        getClientMetrics(clientId).authorizationRequests.increment();
    }

    public void recordAuthorizationSuccess(String clientId, long durationMs) {
        authorizationSuccess.increment();
        authorizationDuration.record(Duration.ofMillis(durationMs));
        getClientMetrics(clientId).authorizationSuccess.increment();
    }

    public void recordAuthorizationFailure(String clientId, String reason) {
        authorizationFailures.increment();
        Counter.builder(METRIC_PREFIX + ".authorization.failures.reason")
            .tags("reason", reason)
            .register(meterRegistry)
            .increment();
        getClientMetrics(clientId).authorizationFailures.increment();
    }

    // Token metrics methods
    public void recordTokenRequest(String grantType) {
        tokenRequests.increment();
        Counter.builder(METRIC_PREFIX + ".token.requests.grant")
            .tags("grant_type", grantType)
            .register(meterRegistry)
            .increment();
    }

    public void recordTokenIssuance(String clientId, String grantType, long durationMs) {
        tokenIssuance.increment();
        tokenGenerationDuration.record(Duration.ofMillis(durationMs));
        activeTokens.incrementAndGet();

        Counter.builder(METRIC_PREFIX + ".token.issued.grant")
            .tags("grant_type", grantType)
            .register(meterRegistry)
            .increment();

        getClientMetrics(clientId).tokensIssued.increment();
    }

    public void recordTokenRefresh(String clientId, long durationMs) {
        tokenRefresh.increment();
        tokenGenerationDuration.record(Duration.ofMillis(durationMs));
        getClientMetrics(clientId).tokensRefreshed.increment();
    }

    public void recordTokenRevocation(String clientId, String tokenType) {
        tokenRevocation.increment();
        activeTokens.decrementAndGet();

        Counter.builder(METRIC_PREFIX + ".token.revoked.type")
            .tags("token_type", tokenType)
            .register(meterRegistry)
            .increment();

        getClientMetrics(clientId).tokensRevoked.increment();
    }

    // Client authentication metrics
    public void recordClientAuthSuccess(String clientId) {
        clientAuthSuccess.increment();
        getClientMetrics(clientId).authSuccess.increment();
    }

    public void recordClientAuthFailure(String clientId) {
        clientAuthFailures.increment();
        if (clientId != null) {
            getClientMetrics(clientId).authFailures.increment();
        }
    }

    // Introspection metrics
    public void recordIntrospection(String clientId, boolean active, long durationMs) {
        introspectionRequests.increment();
        introspectionDuration.record(Duration.ofMillis(durationMs));

        if (active) {
            introspectionActive.increment();
        } else {
            introspectionInactive.increment();
        }

        getClientMetrics(clientId).introspectionRequests.increment();
    }

    // UserInfo metrics
    public void recordUserInfoRequest(String clientId, boolean success, long durationMs) {
        userInfoRequests.increment();
        userInfoDuration.record(Duration.ofMillis(durationMs));

        if (success) {
            userInfoSuccess.increment();
            getClientMetrics(clientId).userInfoSuccess.increment();
        } else {
            userInfoFailures.increment();
            getClientMetrics(clientId).userInfoFailures.increment();
        }
    }

    // Security metrics
    public void recordSuspiciousActivity(String clientId, String activityType) {
        suspiciousActivities.increment();
        Counter.builder(METRIC_PREFIX + ".security.suspicious.type")
            .tags("activity", activityType)
            .register(meterRegistry)
            .increment();

        if (clientId != null) {
            getClientMetrics(clientId).suspiciousActivities.increment();
        }
    }

    public void recordRateLimitExceeded(String clientId) {
        rateLimitExceeded.increment();
        getClientMetrics(clientId).rateLimitExceeded.increment();
    }

    public void recordPKCEValidationFailure(String clientId) {
        pkceValidationFailures.increment();
        getClientMetrics(clientId).pkceFailures.increment();
    }

    public void recordInvalidScope(String clientId, String scope) {
        invalidScopeRequests.increment();
        Counter.builder(METRIC_PREFIX + ".security.invalid.scope.detail")
            .tags("scope", scope)
            .register(meterRegistry)
            .increment();

        getClientMetrics(clientId).invalidScopes.increment();
    }

    // Token count management
    public void incrementActiveTokens() {
        activeTokens.incrementAndGet();
    }

    public void decrementActiveTokens() {
        activeTokens.decrementAndGet();
    }

    public void incrementActiveRefreshTokens() {
        activeRefreshTokens.incrementAndGet();
    }

    public void decrementActiveRefreshTokens() {
        activeRefreshTokens.decrementAndGet();
    }

    // Get or create client-specific metrics
    private ClientMetrics getClientMetrics(String clientId) {
        return clientMetrics.computeIfAbsent(clientId, id -> new ClientMetrics(id, meterRegistry));
    }

    /**
     * Client-specific metrics collection
     */
    private static class ClientMetrics {
        final Counter authorizationRequests;
        final Counter authorizationSuccess;
        final Counter authorizationFailures;
        final Counter tokensIssued;
        final Counter tokensRefreshed;
        final Counter tokensRevoked;
        final Counter authSuccess;
        final Counter authFailures;
        final Counter introspectionRequests;
        final Counter userInfoSuccess;
        final Counter userInfoFailures;
        final Counter suspiciousActivities;
        final Counter rateLimitExceeded;
        final Counter pkceFailures;
        final Counter invalidScopes;

        ClientMetrics(String clientId, MeterRegistry registry) {
            Tags clientTags = Tags.of("client_id", clientId);

            this.authorizationRequests = Counter.builder(METRIC_PREFIX + ".client.authorization.requests")
                .tags(clientTags)
                .register(registry);

            this.authorizationSuccess = Counter.builder(METRIC_PREFIX + ".client.authorization.success")
                .tags(clientTags)
                .register(registry);

            this.authorizationFailures = Counter.builder(METRIC_PREFIX + ".client.authorization.failures")
                .tags(clientTags)
                .register(registry);

            this.tokensIssued = Counter.builder(METRIC_PREFIX + ".client.tokens.issued")
                .tags(clientTags)
                .register(registry);

            this.tokensRefreshed = Counter.builder(METRIC_PREFIX + ".client.tokens.refreshed")
                .tags(clientTags)
                .register(registry);

            this.tokensRevoked = Counter.builder(METRIC_PREFIX + ".client.tokens.revoked")
                .tags(clientTags)
                .register(registry);

            this.authSuccess = Counter.builder(METRIC_PREFIX + ".client.auth.success")
                .tags(clientTags)
                .register(registry);

            this.authFailures = Counter.builder(METRIC_PREFIX + ".client.auth.failures")
                .tags(clientTags)
                .register(registry);

            this.introspectionRequests = Counter.builder(METRIC_PREFIX + ".client.introspection.requests")
                .tags(clientTags)
                .register(registry);

            this.userInfoSuccess = Counter.builder(METRIC_PREFIX + ".client.userinfo.success")
                .tags(clientTags)
                .register(registry);

            this.userInfoFailures = Counter.builder(METRIC_PREFIX + ".client.userinfo.failures")
                .tags(clientTags)
                .register(registry);

            this.suspiciousActivities = Counter.builder(METRIC_PREFIX + ".client.suspicious.activities")
                .tags(clientTags)
                .register(registry);

            this.rateLimitExceeded = Counter.builder(METRIC_PREFIX + ".client.ratelimit.exceeded")
                .tags(clientTags)
                .register(registry);

            this.pkceFailures = Counter.builder(METRIC_PREFIX + ".client.pkce.failures")
                .tags(clientTags)
                .register(registry);

            this.invalidScopes = Counter.builder(METRIC_PREFIX + ".client.invalid.scopes")
                .tags(clientTags)
                .register(registry);
        }
    }

    /**
     * Record a token rotation event.
     */
    public void recordTokenRotation(String clientId) {
        // Record token rotation for the client
        getClientMetrics(clientId).tokensRefreshed.increment();

        // Record global token rotation
        Counter.builder(METRIC_PREFIX + ".token.rotation")
                .description("Count of token rotations")
                .tag("client_id", clientId)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record a security event.
     */
    public void recordSecurityEvent(String eventType, String clientId) {
        Counter.builder(METRIC_PREFIX + ".security.event")
                .description("Security events")
                .tag("event_type", eventType)
                .tag("client_id", clientId != null ? clientId : "unknown")
                .register(meterRegistry)
                .increment();

        // Log critical security events to metrics
        if ("token_reuse_detected".equals(eventType)) {
            Counter.builder(METRIC_PREFIX + ".security.token_reuse")
                    .description("Token reuse detection events")
                    .tag("client_id", clientId != null ? clientId : "unknown")
                    .register(meterRegistry)
                    .increment();
        }
    }

    /**
     * Record consent granted event.
     */
    public void recordConsentGranted(String clientId) {
        Counter.builder(METRIC_PREFIX + ".consent.granted")
                .description("Count of consents granted")
                .tag("client_id", clientId)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record consent denied event.
     */
    public void recordConsentDenied(String clientId) {
        Counter.builder(METRIC_PREFIX + ".consent.denied")
                .description("Count of consents denied")
                .tag("client_id", clientId)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record consent revoked event.
     */
    public void recordConsentRevoked(String clientId) {
        Counter.builder(METRIC_PREFIX + ".consent.revoked")
                .description("Count of consents revoked")
                .tag("client_id", clientId)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record session created event.
     */
    public void recordSessionCreated(String clientId) {
        Counter.builder(METRIC_PREFIX + ".session.created")
                .description("Count of sessions created")
                .tag("client_id", clientId)
                .register(meterRegistry)
                .increment();

        // Track active sessions gauge
        activeTokens.incrementAndGet();
    }

    /**
     * Record session terminated event.
     */
    public void recordSessionTerminated(String clientId) {
        Counter.builder(METRIC_PREFIX + ".session.terminated")
                .description("Count of sessions terminated")
                .tag("client_id", clientId)
                .register(meterRegistry)
                .increment();

        // Update active sessions gauge
        activeTokens.decrementAndGet();
    }

    /**
     * Record session refreshed event.
     */
    public void recordSessionRefreshed(String clientId) {
        Counter.builder(METRIC_PREFIX + ".session.refreshed")
                .description("Count of sessions refreshed")
                .tag("client_id", clientId)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record session elevated event.
     */
    public void recordSessionElevated(String clientId, String authLevel) {
        Counter.builder(METRIC_PREFIX + ".session.elevated")
                .description("Count of sessions elevated")
                .tag("client_id", clientId)
                .tag("auth_level", authLevel)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record session idle timeout event.
     */
    public void recordSessionIdleTimeout(String clientId) {
        Counter.builder(METRIC_PREFIX + ".session.idle_timeout")
                .description("Count of sessions timed out due to inactivity")
                .tag("client_id", clientId)
                .register(meterRegistry)
                .increment();
    }
}