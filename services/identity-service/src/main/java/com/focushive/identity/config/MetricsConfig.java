package com.focushive.identity.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Metrics configuration for monitoring and observability.
 * Configures custom metrics for authentication, JWT tokens, and rate limiting.
 */
@Slf4j
@Configuration
@Profile("!test")
public class MetricsConfig {

    /**
     * Enable @Timed annotation support for method-level metrics.
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry meterRegistry) {
        return new TimedAspect(meterRegistry);
    }

    /**
     * Configure common tags for all metrics.
     */
    @Bean
    public MeterFilter commonTagsFilter() {
        return MeterFilter.commonTags(Arrays.asList(
            Tag.of("service", "identity-service"),
            Tag.of("environment", System.getProperty("spring.profiles.active", "default"))
        ));
    }

    /**
     * Configure histogram percentiles for timing metrics.
     */
    @Bean
    public MeterFilter distributionConfig() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if (id.getName().startsWith("oauth2.") && id.getName().endsWith(".duration")) {
                    return DistributionStatisticConfig.builder()
                        .percentiles(0.5, 0.75, 0.95, 0.99)
                        .percentilesHistogram(true)
                        .build()
                        .merge(config);
                }
                return config;
            }
        };
    }

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            String environment = System.getProperty("spring.profiles.active");
            if (environment == null) {
                environment = "default";
            }
            registry.config()
                .commonTags("application", "identity-service")
                .commonTags("environment", environment);
        };
    }

    @Bean
    public AuthenticationMetrics authenticationMetrics(MeterRegistry registry) {
        return new AuthenticationMetrics(registry);
    }

    @Bean
    public JWTMetrics jwtMetrics(MeterRegistry registry) {
        return new JWTMetrics(registry);
    }

    @Bean
    public RateLimitMetrics rateLimitMetrics(MeterRegistry registry) {
        return new RateLimitMetrics(registry);
    }

    @Bean
    public OAuth2Metrics oauth2Metrics(MeterRegistry registry) {
        return new OAuth2Metrics(registry);
    }

    /**
     * Authentication metrics tracker.
     */
    @Component
    public static class AuthenticationMetrics {
        private final Counter authAttempts;
        private final Counter authSuccess;
        private final Counter authFailure;
        private final Counter accountLockouts;
        private final Counter passwordResets;
        private final Timer authDuration;
        private final AtomicInteger activeSessions;

        public AuthenticationMetrics(MeterRegistry registry) {
            this.authAttempts = Counter.builder("auth_attempts_total")
                .description("Total number of authentication attempts")
                .register(registry);

            this.authSuccess = Counter.builder("auth_success_total")
                .description("Total number of successful authentications")
                .register(registry);

            this.authFailure = Counter.builder("auth_failure_total")
                .description("Total number of failed authentications")
                .tag("reason", "invalid_credentials")
                .register(registry);

            this.accountLockouts = Counter.builder("auth_account_lockouts_total")
                .description("Total number of account lockouts")
                .register(registry);

            this.passwordResets = Counter.builder("auth_password_resets_total")
                .description("Total number of password reset requests")
                .register(registry);

            this.authDuration = Timer.builder("auth_duration_seconds")
                .description("Duration of authentication operations")
                .register(registry);

            this.activeSessions = new AtomicInteger(0);
            Gauge.builder("auth_active_sessions", activeSessions, AtomicInteger::get)
                .description("Number of active user sessions")
                .register(registry);
        }

        public void recordAuthAttempt() {
            authAttempts.increment();
        }

        public void recordAuthSuccess() {
            authSuccess.increment();
        }

        public void recordAuthFailure(String reason) {
            authFailure.increment();
        }

        public void recordAccountLockout() {
            accountLockouts.increment();
        }

        public void recordPasswordReset() {
            passwordResets.increment();
        }

        public Timer.Sample startAuthTimer() {
            return Timer.start();
        }

        public void stopAuthTimer(Timer.Sample sample) {
            sample.stop(authDuration);
        }

        public void incrementActiveSessions() {
            activeSessions.incrementAndGet();
        }

        public void decrementActiveSessions() {
            activeSessions.decrementAndGet();
        }
    }

    /**
     * JWT token metrics tracker.
     */
    @Component
    public static class JWTMetrics {
        private final Counter tokensIssued;
        private final Counter tokensRefreshed;
        private final Counter tokensRevoked;
        private final Counter tokenValidationSuccess;
        private final Counter tokenValidationFailure;
        private final Timer tokenGenerationTime;
        private final AtomicInteger activeTokens;

        public JWTMetrics(MeterRegistry registry) {
            this.tokensIssued = Counter.builder("jwt_tokens_issued_total")
                .description("Total number of JWT tokens issued")
                .register(registry);

            this.tokensRefreshed = Counter.builder("jwt_tokens_refreshed_total")
                .description("Total number of JWT tokens refreshed")
                .register(registry);

            this.tokensRevoked = Counter.builder("jwt_tokens_revoked_total")
                .description("Total number of JWT tokens revoked")
                .register(registry);

            this.tokenValidationSuccess = Counter.builder("jwt_validation_success_total")
                .description("Total number of successful JWT validations")
                .register(registry);

            this.tokenValidationFailure = Counter.builder("jwt_validation_failure_total")
                .description("Total number of failed JWT validations")
                .register(registry);

            this.tokenGenerationTime = Timer.builder("jwt_generation_duration_seconds")
                .description("Time taken to generate JWT tokens")
                .register(registry);

            this.activeTokens = new AtomicInteger(0);
            Gauge.builder("jwt_active_tokens", activeTokens, AtomicInteger::get)
                .description("Number of active JWT tokens")
                .register(registry);
        }

        public void recordTokenIssued() {
            tokensIssued.increment();
            activeTokens.incrementAndGet();
        }

        public void recordTokenRefreshed() {
            tokensRefreshed.increment();
        }

        public void recordTokenRevoked() {
            tokensRevoked.increment();
            activeTokens.decrementAndGet();
        }

        public void recordValidationSuccess() {
            tokenValidationSuccess.increment();
        }

        public void recordValidationFailure(String reason) {
            tokenValidationFailure.increment();
        }

        public Timer.Sample startGenerationTimer() {
            return Timer.start();
        }

        public void stopGenerationTimer(Timer.Sample sample) {
            sample.stop(tokenGenerationTime);
        }
    }

    /**
     * Rate limiting metrics tracker.
     */
    @Component
    public static class RateLimitMetrics {
        private final Counter rateLimitExceeded;
        private final AtomicInteger remainingRequests;
        private final Gauge rateLimitRemaining;

        public RateLimitMetrics(MeterRegistry registry) {
            this.rateLimitExceeded = Counter.builder("rate_limit_exceeded_total")
                .description("Total number of rate limit exceeded events")
                .register(registry);

            this.remainingRequests = new AtomicInteger(100);
            this.rateLimitRemaining = Gauge.builder("rate_limit_remaining", remainingRequests, AtomicInteger::get)
                .description("Remaining requests in current rate limit window")
                .register(registry);
        }

        public void recordRateLimitExceeded(String endpoint) {
            rateLimitExceeded.increment();
        }

        public void updateRemainingRequests(int remaining) {
            remainingRequests.set(remaining);
        }
    }

    /**
     * OAuth2 metrics tracker.
     */
    @Component
    public static class OAuth2Metrics {
        private final Counter authorizeRequests;
        private final Counter tokenRequests;
        private final Counter revokeRequests;
        private final Counter introspectRequests;
        private final Counter clientRegistrations;
        private final Timer oauth2RequestDuration;

        public OAuth2Metrics(MeterRegistry registry) {
            this.authorizeRequests = Counter.builder("oauth2_authorize_requests_total")
                .description("Total number of OAuth2 authorize requests")
                .register(registry);

            this.tokenRequests = Counter.builder("oauth2_token_requests_total")
                .description("Total number of OAuth2 token requests")
                .register(registry);

            this.revokeRequests = Counter.builder("oauth2_revoke_requests_total")
                .description("Total number of OAuth2 revoke requests")
                .register(registry);

            this.introspectRequests = Counter.builder("oauth2_introspect_requests_total")
                .description("Total number of OAuth2 introspect requests")
                .register(registry);

            this.clientRegistrations = Counter.builder("oauth2_client_registrations_total")
                .description("Total number of OAuth2 client registrations")
                .register(registry);

            this.oauth2RequestDuration = Timer.builder("oauth2_request_duration_seconds")
                .description("Duration of OAuth2 requests")
                .register(registry);
        }

        public void recordAuthorizeRequest(String clientId, String responseType) {
            authorizeRequests.increment();
        }

        public void recordTokenRequest(String clientId, String grantType) {
            tokenRequests.increment();
        }

        public void recordRevokeRequest() {
            revokeRequests.increment();
        }

        public void recordIntrospectRequest() {
            introspectRequests.increment();
        }

        public void recordClientRegistration() {
            clientRegistrations.increment();
        }

        public Timer.Sample startRequestTimer() {
            return Timer.start();
        }

        public void stopRequestTimer(Timer.Sample sample) {
            sample.stop(oauth2RequestDuration);
        }
    }

    /**
     * Thread pool metrics configuration.
     */
    @Bean
    public ThreadPoolMetrics threadPoolMetrics(MeterRegistry registry) {
        return new ThreadPoolMetrics(registry);
    }

    @Component
    public static class ThreadPoolMetrics {
        private final AtomicInteger poolSize;
        private final AtomicInteger queueSize;
        private final AtomicInteger activeThreads;

        public ThreadPoolMetrics(MeterRegistry registry) {
            this.poolSize = new AtomicInteger(10);
            this.queueSize = new AtomicInteger(0);
            this.activeThreads = new AtomicInteger(0);

            Gauge.builder("executor_pool_size", poolSize, AtomicInteger::get)
                .description("Thread pool size")
                .register(registry);

            Gauge.builder("executor_queue_size", queueSize, AtomicInteger::get)
                .description("Thread pool queue size")
                .register(registry);

            Gauge.builder("executor_active_threads", activeThreads, AtomicInteger::get)
                .description("Number of active threads")
                .register(registry);
        }

        public void updatePoolSize(int size) {
            poolSize.set(size);
        }

        public void updateQueueSize(int size) {
            queueSize.set(size);
        }

        public void updateActiveThreads(int count) {
            activeThreads.set(count);
        }
    }
}