package com.focushive.notification.security;

import com.focushive.notification.config.SecurityProperties;
import com.focushive.notification.service.SecurityAuditService;
import com.focushive.notification.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter with comprehensive security features and audit logging.
 * Handles JWT token validation, user context extraction, and security event tracking.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;
    private final SecurityAuditService securityAuditService;
    private final SecurityProperties securityProperties;
    private final TokenBlacklistService tokenBlacklistService;

    // Track failed attempts per IP/username for suspicious activity detection
    private final Map<String, FailedAttemptRecord> failedAttempts = new ConcurrentHashMap<>();

    /**
     * Record for tracking failed authentication attempts.
     */
    private static class FailedAttemptRecord {
        int count;
        Instant firstAttemptTime;
        Instant lastAttemptTime;

        FailedAttemptRecord() {
            this.count = 1;
            this.firstAttemptTime = Instant.now();
            this.lastAttemptTime = Instant.now();
        }

        void increment() {
            this.count++;
            this.lastAttemptTime = Instant.now();
        }

        boolean isExpired(long windowSeconds) {
            return Instant.now().isAfter(firstAttemptTime.plusSeconds(windowSeconds));
        }
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                  @NonNull HttpServletResponse response, 
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        // Set security headers
        setSecurityHeaders(response);
        
        String token = extractToken(request);
        
        if (token == null) {
            // No token provided - continue without authentication
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIpAddress(request);
        String correlationId = request.getHeader("X-Correlation-ID");

        try {
            // Decode and validate JWT token
            Jwt jwt = jwtDecoder.decode(token);

            // Check if token is blacklisted
            if (tokenBlacklistService.isBlacklisted(jwt)) {
                log.debug("Token is blacklisted for user: {}", jwt.getSubject());
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Token blacklisted\",\"message\":\"This token has been invalidated\"}");
                return;
            }

            // Check if user is globally blacklisted
            if (tokenBlacklistService.isUserBlacklisted(jwt.getSubject())) {
                log.debug("User is globally blacklisted: {}", jwt.getSubject());
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"User blacklisted\",\"message\":\"All tokens for this user have been invalidated\"}");
                return;
            }

            // Extract authorities from JWT roles claim
            List<SimpleGrantedAuthority> authorities = extractAuthorities(jwt);

            // Create authentication token and set in security context
            JwtAuthenticationToken authToken = new JwtAuthenticationToken(jwt, authorities);
            SecurityContextHolder.getContext().setAuthentication(authToken);

            // Clear failed attempts on successful authentication
            failedAttempts.remove(clientIp);
            
            // Log successful authentication
            if (correlationId != null) {
                securityAuditService.logAuthenticationSuccessWithCorrelation(clientIp, correlationId);
            } else {
                securityAuditService.logAuthenticationSuccess(clientIp);
            }
            
            log.debug("Successfully authenticated user: {}", jwt.getSubject());
            
        } catch (JwtException e) {
            // JWT validation failed
            log.debug("JWT authentication failed: {}", e.getMessage());
            
            // Track failed attempts and detect suspicious activity
            String trackingKey = securityProperties.getAuthentication().isTrackByIp() ? clientIp : "global";
            trackFailedAttempt(trackingKey);

            FailedAttemptRecord record = failedAttempts.get(trackingKey);
            if (record != null && record.count >= securityProperties.getAuthentication().getMaxFailedAttempts()) {
                Map<String, Object> metadata = Map.of(
                    "attemptCount", record.count,
                    "timeWindow", securityProperties.getAuthentication().getFailedAttemptsWindow().toSeconds() + "s",
                    "userAgent", request.getHeader("User-Agent"),
                    "firstAttempt", record.firstAttemptTime.toString(),
                    "lastAttempt", record.lastAttemptTime.toString()
                );
                securityAuditService.logSuspiciousActivity("MULTIPLE_FAILED_AUTH_ATTEMPTS", clientIp, metadata);

                // Check if IP should be locked
                if (isLocked(trackingKey)) {
                    response.setStatus(429); // Too Many Requests
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Too many failed attempts\",\"message\":\"Your access has been temporarily locked due to multiple failed authentication attempts\"}");
                    return;
                }
            }
            
            // Log authentication failure
            securityAuditService.logAuthenticationFailure(maskToken(token), clientIp, e.getMessage());
            
            // Clear security context and return unauthorized
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Authentication failed\",\"message\":\"Invalid or expired token\"}");
            return;
        }
        
        // Continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts JWT token from Authorization header.
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        return null;
    }

    /**
     * Extracts authorities from JWT roles claim.
     */
    private List<SimpleGrantedAuthority> extractAuthorities(Jwt jwt) {
        Object rolesClaim = jwt.getClaim("roles");
        
        if (rolesClaim instanceof List<?> roles) {
            return roles.stream()
                    .filter(role -> role instanceof String)
                    .map(role -> new SimpleGrantedAuthority((String) role))
                    .collect(Collectors.toList());
        }
        
        return Collections.emptyList();
    }

    /**
     * Gets the real client IP address considering proxy headers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        
        if (StringUtils.hasText(xForwardedFor)) {
            // Return the first IP in the chain (original client)
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Sets security headers in the response based on configuration.
     */
    private void setSecurityHeaders(HttpServletResponse response) {
        SecurityProperties.HeadersConfig headers = securityProperties.getHeaders();
        if (!headers.isEnabled()) {
            return;
        }

        response.setHeader("X-Content-Type-Options", headers.getContentTypeOptions());
        response.setHeader("X-Frame-Options", headers.getFrameOptions());
        response.setHeader("X-XSS-Protection", headers.getXssProtection());
        response.setHeader("Referrer-Policy", headers.getReferrerPolicy());
        response.setHeader("Content-Security-Policy", headers.getContentSecurityPolicy());

        // Only add HSTS header for HTTPS connections
        if ("https".equalsIgnoreCase(response.getHeader("X-Forwarded-Proto")) ||
            response.isCommitted() && response.getStatus() >= 200 && response.getStatus() < 300) {
            response.setHeader("Strict-Transport-Security", headers.getStrictTransportSecurity());
        }
    }

    /**
     * Masks sensitive token information for logging.
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }

    /**
     * Tracks failed authentication attempts for suspicious activity detection.
     */
    private void trackFailedAttempt(String key) {
        FailedAttemptRecord record = failedAttempts.compute(key, (k, existing) -> {
            if (existing == null) {
                return new FailedAttemptRecord();
            }

            // Check if the record has expired based on the time window
            long windowSeconds = securityProperties.getAuthentication().getFailedAttemptsWindow().toSeconds();
            if (existing.isExpired(windowSeconds)) {
                // Reset the record if outside the time window
                return new FailedAttemptRecord();
            }

            existing.increment();
            return existing;
        });

        log.debug("Failed attempt tracked for {}: count={}", key, record.count);
    }

    /**
     * Checks if a key (IP or username) is currently locked.
     */
    private boolean isLocked(String key) {
        FailedAttemptRecord record = failedAttempts.get(key);
        if (record == null) {
            return false;
        }

        SecurityProperties.AuthenticationConfig authConfig = securityProperties.getAuthentication();

        // Check if max attempts exceeded
        if (record.count < authConfig.getMaxFailedAttempts()) {
            return false;
        }

        // Check if still within lockout duration
        Instant lockoutEnd = record.lastAttemptTime.plus(authConfig.getLockoutDuration());
        boolean isLocked = Instant.now().isBefore(lockoutEnd);

        // Auto-unlock if configured and lockout period has passed
        if (!isLocked && authConfig.isAutoUnlock()) {
            failedAttempts.remove(key);
            log.info("Auto-unlocked key {} after lockout duration", key);
        }

        return isLocked;
    }

    /**
     * Clears failed attempts for a key.
     * Useful for testing and maintenance operations.
     */
    public void clearFailedAttempts(String key) {
        failedAttempts.remove(key);
    }

    /**
     * Gets the current failed attempt count for a key.
     * Package-private for testing.
     */
    int getFailedAttemptCount(String key) {
        FailedAttemptRecord record = failedAttempts.get(key);
        return record != null ? record.count : 0;
    }

    /**
     * Manually unlock a locked key.
     */
    public void unlock(String key) {
        failedAttempts.remove(key);
        log.info("Manually unlocked key: {}", key);
    }
}