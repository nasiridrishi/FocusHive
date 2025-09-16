package com.focushive.notification.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Security filter that adds OWASP recommended security headers to all responses.
 * Implements defense in depth by adding multiple layers of security headers.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Value("${security.headers.csp:default-src 'self'; script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self';}")
    private String contentSecurityPolicy;

    @Value("${security.headers.frame-options:DENY}")
    private String frameOptions;

    @Value("${security.headers.content-type-options:nosniff}")
    private String contentTypeOptions;

    @Value("${security.headers.xss-protection:1; mode=block}")
    private String xssProtection;

    @Value("${security.headers.referrer-policy:strict-origin-when-cross-origin}")
    private String referrerPolicy;

    @Value("${security.headers.permissions-policy:accelerometer=(), camera=(), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), payment=(), usb=()}")
    private String permissionsPolicy;

    @Value("${security.headers.hsts.enabled:true}")
    private boolean hstsEnabled;

    @Value("${security.headers.hsts.max-age:31536000}")
    private long hstsMaxAge;

    @Value("${security.headers.hsts.include-subdomains:true}")
    private boolean hstsIncludeSubdomains;

    @Value("${security.headers.hsts.preload:false}")
    private boolean hstsPreload;

    @Value("${security.headers.cache-control:no-cache, no-store, must-revalidate, private}")
    private String cacheControl;

    @Value("${security.headers.enabled:true}")
    private boolean securityHeadersEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        if (!securityHeadersEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        // Generate a unique nonce for this request (for CSP inline scripts if needed)
        String nonce = UUID.randomUUID().toString();
        request.setAttribute("csp-nonce", nonce);

        // Add security headers before processing the request
        addSecurityHeaders(request, response, nonce);

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Add all security headers to the response.
     */
    private void addSecurityHeaders(HttpServletRequest request, HttpServletResponse response, String nonce) {
        // Content Security Policy - Prevents XSS, clickjacking, and other injection attacks
        String csp = contentSecurityPolicy;
        if (csp.contains("'nonce-")) {
            csp = csp.replace("'nonce-'", "'nonce-" + nonce + "'");
        }
        response.setHeader("Content-Security-Policy", csp);

        // Also set the legacy header for older browsers
        response.setHeader("X-Content-Security-Policy", csp);

        // X-Frame-Options - Prevents clickjacking
        response.setHeader("X-Frame-Options", frameOptions);

        // X-Content-Type-Options - Prevents MIME type sniffing
        response.setHeader("X-Content-Type-Options", contentTypeOptions);

        // X-XSS-Protection - Enables browser XSS filtering (legacy, but still useful)
        response.setHeader("X-XSS-Protection", xssProtection);

        // Referrer-Policy - Controls referrer information
        response.setHeader("Referrer-Policy", referrerPolicy);

        // Permissions-Policy (formerly Feature-Policy) - Controls browser features
        response.setHeader("Permissions-Policy", permissionsPolicy);

        // Strict-Transport-Security - Forces HTTPS
        if (hstsEnabled && isSecureConnection(request)) {
            StringBuilder hsts = new StringBuilder("max-age=").append(hstsMaxAge);
            if (hstsIncludeSubdomains) {
                hsts.append("; includeSubDomains");
            }
            if (hstsPreload) {
                hsts.append("; preload");
            }
            response.setHeader("Strict-Transport-Security", hsts.toString());
        }

        // Cache-Control - Prevents sensitive data caching
        if (isApiRequest(request)) {
            response.setHeader("Cache-Control", cacheControl);
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }

        // Additional security headers
        response.setHeader("X-Permitted-Cross-Domain-Policies", "none");
        response.setHeader("X-Download-Options", "noopen");
        response.setHeader("X-DNS-Prefetch-Control", "off");

        // CORS headers are handled by Spring Security CORS configuration

        // Add custom security headers for API versioning
        response.setHeader("X-API-Version", "1.0");

        // Add rate limit headers if not already present
        if (response.getHeader("X-RateLimit-Limit") == null) {
            response.setHeader("X-RateLimit-Policy", "60 requests per minute");
        }

        log.debug("Security headers added for request: {} {}", request.getMethod(), request.getRequestURI());
    }

    /**
     * Check if the connection is secure (HTTPS).
     */
    private boolean isSecureConnection(HttpServletRequest request) {
        return request.isSecure() ||
               "https".equalsIgnoreCase(request.getScheme()) ||
               "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }

    /**
     * Check if this is an API request.
     */
    private boolean isApiRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/") ||
               path.startsWith("/v1/") ||
               path.startsWith("/v2/");
    }

    /**
     * Determine if security headers should be applied to this request.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Don't add security headers to health check endpoints
        if (path.equals("/health") || path.equals("/actuator/health")) {
            return true;
        }

        // Don't add security headers to static resources
        if (path.startsWith("/static/") ||
            path.startsWith("/css/") ||
            path.startsWith("/js/") ||
            path.startsWith("/images/")) {
            return true;
        }

        return false;
    }
}