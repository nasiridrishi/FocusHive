package com.focushive.identity.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.filter.OncePerRequestFilter;

import brave.sampler.Sampler;
import brave.Tracing;
import brave.http.HttpTracing;
import brave.propagation.B3Propagation;
import brave.propagation.Propagation;
import brave.spring.webmvc.TracingHandlerInterceptor;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.okhttp3.OkHttpSender;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

/**
 * Distributed tracing configuration for FocusHive Identity Service.
 * Provides comprehensive tracing capabilities including:
 * - Zipkin integration for trace collection
 * - Custom baggage propagation for user context
 * - Correlation ID management
 * - Performance monitoring
 * - Security event tracing
 */
@Configuration
@Profile("prod")
public class TracingConfig {

    @Value("${management.tracing.zipkin.endpoint:http://zipkin:9411/api/v2/spans}")
    private String zipkinEndpoint;

    @Value("${management.tracing.sampling.probability:0.1}")
    private float samplingProbability;

    @Value("${spring.application.name:identity-service}")
    private String serviceName;

    /**
     * Zipkin span sender configuration
     */
    @Bean
    public OkHttpSender sender() {
        return OkHttpSender.create(zipkinEndpoint);
    }

    /**
     * Async Zipkin span handler for performance
     */
    @Bean
    public AsyncZipkinSpanHandler asyncZipkinSpanHandler(OkHttpSender sender) {
        return AsyncZipkinSpanHandler.create(sender);
    }

    /**
     * Sampling configuration for production
     */
    @Bean
    public Sampler alwaysSample() {
        return Sampler.create(samplingProbability);
    }

    /**
     * B3 propagation for compatibility with other services
     */
    @Bean
    public Propagation.Factory propagationFactory() {
        return B3Propagation.FACTORY;
    }

    /**
     * Main Brave tracing configuration
     */
    @Bean
    public Tracing tracing(AsyncZipkinSpanHandler spanHandler, 
                          Sampler sampler, 
                          Propagation.Factory propagationFactory) {
        return Tracing.newBuilder()
                .localServiceName(serviceName)
                .spanReporter(spanHandler)
                .sampler(sampler)
                .propagationFactory(propagationFactory)
                .build();
    }

    /**
     * HTTP tracing for web requests
     */
    @Bean
    public HttpTracing httpTracing(Tracing tracing) {
        return HttpTracing.create(tracing);
    }

    /**
     * Spring MVC tracing interceptor
     */
    @Bean
    public TracingHandlerInterceptor tracingHandlerInterceptor(HttpTracing httpTracing) {
        return TracingHandlerInterceptor.create(httpTracing);
    }

    /**
     * Micrometer Tracer bridge
     */
    @Bean
    public Tracer tracer(Tracing tracing) {
        return new BraveTracer(tracing.tracer(), new BraveBaggageManager());
    }

    /**
     * Observation aspect for @Observed annotations
     */
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }

    /**
     * Correlation ID filter for request tracing
     */
    @Bean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    /**
     * Custom filter to inject correlation IDs and user context into traces
     */
    public static class CorrelationIdFilter extends OncePerRequestFilter {
        
        private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
        private static final String USER_ID_HEADER = "X-User-ID";
        private static final String SESSION_ID_HEADER = "X-Session-ID";
        
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                      FilterChain filterChain) throws ServletException, IOException {
            
            // Generate or extract correlation ID
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = UUID.randomUUID().toString();
            }
            
            // Extract user context
            String userId = request.getHeader(USER_ID_HEADER);
            String sessionId = request.getHeader(SESSION_ID_HEADER);
            
            try {
                // Set correlation ID in MDC for logging
                MDC.put("correlationId", correlationId);
                if (userId != null) {
                    MDC.put("userId", userId);
                }
                if (sessionId != null) {
                    MDC.put("sessionId", sessionId);
                }
                
                // Add correlation ID to response headers
                response.setHeader(CORRELATION_ID_HEADER, correlationId);
                
                // Add tracing headers
                response.setHeader("X-Trace-Id", getTraceId());
                
                filterChain.doFilter(request, response);
                
            } finally {
                // Clean up MDC to prevent memory leaks
                MDC.remove("correlationId");
                MDC.remove("userId");
                MDC.remove("sessionId");
            }
        }
        
        private String getTraceId() {
            // Extract current trace ID from Brave context
            brave.propagation.TraceContext context = brave.Tracing.current().tracer().currentSpan().context();
            if (context != null) {
                return context.traceIdString();
            }
            return "unknown";
        }
    }

    /**
     * Custom security event tracer
     */
    @Bean
    public SecurityEventTracer securityEventTracer(Tracer tracer) {
        return new SecurityEventTracer(tracer);
    }

    /**
     * Security event tracer for audit and monitoring
     */
    public static class SecurityEventTracer {
        private final Tracer tracer;
        
        public SecurityEventTracer(Tracer tracer) {
            this.tracer = tracer;
        }
        
        public void traceAuthenticationAttempt(String username, boolean successful, String clientIp, String userAgent) {
            var span = tracer.nextSpan()
                .name("authentication.attempt")
                .tag("auth.username", username)
                .tag("auth.successful", String.valueOf(successful))
                .tag("client.ip", clientIp)
                .tag("client.user_agent", userAgent != null ? userAgent : "unknown")
                .tag("event.type", "security")
                .start();
                
            try (var ws = tracer.withSpanInScope(span)) {
                if (successful) {
                    span.tag("auth.result", "success");
                } else {
                    span.tag("auth.result", "failure");
                    span.tag("error", "authentication_failed");
                }
            } finally {
                span.end();
            }
        }
        
        public void traceTokenIssuance(String userId, String clientId, String tokenType, long expirationTime) {
            var span = tracer.nextSpan()
                .name("token.issued")
                .tag("user.id", userId)
                .tag("oauth2.client_id", clientId)
                .tag("token.type", tokenType)
                .tag("token.expiration", String.valueOf(expirationTime))
                .tag("event.type", "security")
                .start();
                
            try (var ws = tracer.withSpanInScope(span)) {
                span.tag("token.status", "issued");
            } finally {
                span.end();
            }
        }
        
        public void traceRateLimitExceeded(String clientIp, String endpoint, String rateLimitType) {
            var span = tracer.nextSpan()
                .name("rate_limit.exceeded")
                .tag("client.ip", clientIp)
                .tag("http.endpoint", endpoint)
                .tag("rate_limit.type", rateLimitType)
                .tag("event.type", "security")
                .tag("severity", "warning")
                .start();
                
            try (var ws = tracer.withSpanInScope(span)) {
                span.tag("rate_limit.status", "exceeded");
            } finally {
                span.end();
            }
        }
        
        public void traceSuspiciousActivity(String clientIp, String activity, String details, String severity) {
            var span = tracer.nextSpan()
                .name("security.suspicious_activity")
                .tag("client.ip", clientIp)
                .tag("security.activity", activity)
                .tag("security.details", details)
                .tag("severity", severity)
                .tag("event.type", "security_alert")
                .start();
                
            try (var ws = tracer.withSpanInScope(span)) {
                span.tag("security.status", "detected");
            } finally {
                span.end();
            }
        }
        
        public void traceDataAccess(String userId, String resource, String operation, boolean authorized) {
            var span = tracer.nextSpan()
                .name("data.access")
                .tag("user.id", userId)
                .tag("resource", resource)
                .tag("operation", operation)
                .tag("authorized", String.valueOf(authorized))
                .tag("event.type", "data_access")
                .start();
                
            try (var ws = tracer.withSpanInScope(span)) {
                if (authorized) {
                    span.tag("access.result", "granted");
                } else {
                    span.tag("access.result", "denied");
                    span.tag("error", "unauthorized_access");
                }
            } finally {
                span.end();
            }
        }
    }

    /**
     * Database query tracer for performance monitoring
     */
    @Bean
    public DatabaseQueryTracer databaseQueryTracer(Tracer tracer) {
        return new DatabaseQueryTracer(tracer);
    }

    /**
     * Database performance tracer
     */
    public static class DatabaseQueryTracer {
        private final Tracer tracer;
        
        public DatabaseQueryTracer(Tracer tracer) {
            this.tracer = tracer;
        }
        
        public void traceQuery(String query, long executionTimeMs, int resultCount, String operation) {
            var span = tracer.nextSpan()
                .name("db.query")
                .tag("db.statement", query.length() > 100 ? query.substring(0, 100) + "..." : query)
                .tag("db.operation", operation)
                .tag("db.execution_time_ms", String.valueOf(executionTimeMs))
                .tag("db.result_count", String.valueOf(resultCount))
                .start();
                
            try (var ws = tracer.withSpanInScope(span)) {
                if (executionTimeMs > 1000) { // Slow query threshold
                    span.tag("db.slow_query", "true");
                    span.tag("performance.issue", "slow_query");
                }
                
                if (executionTimeMs > 5000) { // Very slow query threshold
                    span.tag("severity", "warning");
                }
            } finally {
                span.end();
            }
        }
    }
}