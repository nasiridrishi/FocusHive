package com.focushive.identity.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.filter.OncePerRequestFilter;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Production-specific security configuration for FocusHive Identity Service.
 * Provides enterprise-grade security features including:
 * - Advanced security headers
 * - Rate limiting with bucket4j
 * - Enhanced CORS for production
 * - Security monitoring and audit logging
 * - DDoS protection
 */
@Configuration
@EnableWebSecurity
@Profile("prod")
public class ProductionSecurityConfig {

    @Value("${security.rate-limit.auth.requests-per-minute:10}")
    private int authRateLimit;

    @Value("${security.rate-limit.api.requests-per-minute:100}")
    private int apiRateLimit;

    @Value("${security.cors.allowed-origins}")
    private String corsAllowedOrigins;

    /**
     * Production security filter chain with enhanced security headers
     */
    @Bean
    public SecurityFilterChain productionSecurityFilterChain(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
                // Strict Transport Security - Force HTTPS
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                        .maxAgeInSeconds(31536000) // 1 year
                        .includeSubdomains(true)
                        .preload(true)
                )
                
                // Content Security Policy - Prevent XSS
                .contentSecurityPolicy(
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
                    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                    "font-src 'self' https://fonts.gstatic.com; " +
                    "img-src 'self' data: https:; " +
                    "connect-src 'self' https:; " +
                    "frame-ancestors 'none'; " +
                    "base-uri 'self'; " +
                    "form-action 'self'"
                )
                
                // X-Frame-Options - Prevent clickjacking
                .frameOptions().deny()
                
                // X-Content-Type-Options - Prevent MIME sniffing
                .contentTypeOptions().and()
                
                // X-XSS-Protection - Enable XSS filtering
                .addHeaderWriter(new XXssProtectionHeaderWriter())
                
                // Referrer Policy - Control referrer information
                .addHeaderWriter(new ReferrerPolicyHeaderWriter(
                    ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                
                // Permissions Policy - Control browser features
                .addHeaderWriter((request, response) -> {
                    response.setHeader("Permissions-Policy", 
                        "geolocation=(), " +
                        "microphone=(), " +
                        "camera=(), " +
                        "payment=(), " +
                        "usb=(), " +
                        "magnetometer=(), " +
                        "accelerometer=(), " +
                        "gyroscope=()");
                })
                
                // Additional security headers
                .addHeaderWriter((request, response) -> {
                    // Prevent caching of sensitive pages
                    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                    response.setHeader("Pragma", "no-cache");
                    response.setHeader("Expires", "0");
                    
                    // Server information hiding
                    response.setHeader("Server", "FocusHive");
                    
                    // Cross-Origin policies
                    response.setHeader("Cross-Origin-Embedder-Policy", "require-corp");
                    response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
                    response.setHeader("Cross-Origin-Resource-Policy", "same-origin");
                })
        );

        return http.build();
    }

    /**
     * Rate limiting filter for authentication endpoints
     */
    @Bean
    public FilterRegistrationBean<RateLimitingFilter> authRateLimitFilter() {
        FilterRegistrationBean<RateLimitingFilter> registrationBean = 
            new FilterRegistrationBean<>(new RateLimitingFilter(authRateLimit, Duration.ofMinutes(1)));
        registrationBean.addUrlPatterns("/api/v1/auth/*", "/oauth2/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

    /**
     * Rate limiting filter for general API endpoints
     */
    @Bean
    public FilterRegistrationBean<RateLimitingFilter> apiRateLimitFilter() {
        FilterRegistrationBean<RateLimitingFilter> registrationBean = 
            new FilterRegistrationBean<>(new RateLimitingFilter(apiRateLimit, Duration.ofMinutes(1)));
        registrationBean.addUrlPatterns("/api/v1/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registrationBean;
    }

    /**
     * Request logging filter for security monitoring
     */
    @Bean
    public FilterRegistrationBean<SecurityAuditFilter> securityAuditFilter() {
        FilterRegistrationBean<SecurityAuditFilter> registrationBean = 
            new FilterRegistrationBean<>(new SecurityAuditFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return registrationBean;
    }

    /**
     * Rate limiting filter implementation using bucket4j
     */
    public static class RateLimitingFilter extends OncePerRequestFilter {
        private final ConcurrentHashMap<String, Bucket> cache = new ConcurrentHashMap<>();
        private final int requestsPerWindow;
        private final Duration windowDuration;

        public RateLimitingFilter(int requestsPerWindow, Duration windowDuration) {
            this.requestsPerWindow = requestsPerWindow;
            this.windowDuration = windowDuration;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                      FilterChain filterChain) throws ServletException, IOException {
            
            String key = getClientIdentifier(request);
            Bucket bucket = cache.computeIfAbsent(key, this::createNewBucket);

            if (bucket.tryConsume(1)) {
                // Add rate limit headers
                response.setHeader("X-Rate-Limit-Remaining", String.valueOf(bucket.getAvailableTokens()));
                response.setHeader("X-Rate-Limit-Limit", String.valueOf(requestsPerWindow));
                response.setHeader("X-Rate-Limit-Reset", String.valueOf(System.currentTimeMillis() + windowDuration.toMillis()));
                
                filterChain.doFilter(request, response);
            } else {
                response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
                response.setContentType("application/json");
                response.getWriter().write(
                    "{\"error\": \"Rate limit exceeded\", \"code\": \"RATE_LIMIT_EXCEEDED\", " +
                    "\"retry_after\": " + windowDuration.getSeconds() + "}"
                );
            }
        }

        private Bucket createNewBucket(String key) {
            return Bucket4j.builder()
                .addLimit(Bandwidth.classic(requestsPerWindow, Refill.intervally(requestsPerWindow, windowDuration)))
                .build();
        }

        private String getClientIdentifier(HttpServletRequest request) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isEmpty()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
    }

    /**
     * Security audit filter for monitoring and logging
     */
    public static class SecurityAuditFilter extends OncePerRequestFilter {
        
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                      FilterChain filterChain) throws ServletException, IOException {
            
            long startTime = System.currentTimeMillis();
            String method = request.getMethod();
            String uri = request.getRequestURI();
            String userAgent = request.getHeader("User-Agent");
            String clientIp = getClientIp(request);

            try {
                filterChain.doFilter(request, response);
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                int status = response.getStatus();
                
                // Log suspicious activity
                if (isSuspiciousRequest(request, response, duration)) {
                    logSecurityEvent("SUSPICIOUS_ACTIVITY", request, response, duration);
                }
                
                // Log authentication failures
                if (status == HttpServletResponse.SC_UNAUTHORIZED) {
                    logSecurityEvent("AUTHENTICATION_FAILURE", request, response, duration);
                }
                
                // Log slow requests (potential DoS)
                if (duration > 5000) { // 5 seconds
                    logSecurityEvent("SLOW_REQUEST", request, response, duration);
                }
            }
        }

        private boolean isSuspiciousRequest(HttpServletRequest request, HttpServletResponse response, long duration) {
            // Check for common attack patterns
            String uri = request.getRequestURI().toLowerCase();
            String userAgent = request.getHeader("User-Agent");
            
            // SQL injection attempts
            if (uri.contains("'") || uri.contains("--") || uri.contains("union") || uri.contains("select")) {
                return true;
            }
            
            // Path traversal attempts
            if (uri.contains("..") || uri.contains("~") || uri.contains("%2e%2e")) {
                return true;
            }
            
            // Script injection attempts
            if (uri.contains("<script") || uri.contains("javascript:") || uri.contains("onerror=")) {
                return true;
            }
            
            // Missing or suspicious User-Agent
            if (userAgent == null || userAgent.trim().isEmpty() || userAgent.length() < 10) {
                return true;
            }
            
            // Multiple rapid requests (potential brute force)
            if (response.getStatus() == HttpServletResponse.SC_TOO_MANY_REQUESTS) {
                return true;
            }
            
            return false;
        }

        private void logSecurityEvent(String eventType, HttpServletRequest request, HttpServletResponse response, long duration) {
            // In production, this would integrate with your security monitoring system
            // For now, we'll use structured logging
            String clientIp = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            String method = request.getMethod();
            String uri = request.getRequestURI();
            
            System.out.println(String.format(
                "SECURITY_EVENT: {\"type\": \"%s\", \"ip\": \"%s\", \"method\": \"%s\", \"uri\": \"%s\", \"status\": %d, \"duration\": %d, \"userAgent\": \"%s\", \"timestamp\": %d}",
                eventType, clientIp, method, uri, response.getStatus(), duration, userAgent, System.currentTimeMillis()
            ));
        }

        private String getClientIp(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }
            
            return request.getRemoteAddr();
        }
    }
}