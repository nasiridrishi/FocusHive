package com.focushive.identity.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Security Headers Configuration for production security.
 * Implements OWASP recommended security headers.
 */
@Component
public class SecurityHeadersConfig extends OncePerRequestFilter {

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${security.headers.hsts.max-age:31536000}")
    private long hstsMaxAge;

    @Value("${security.headers.hsts.include-subdomains:true}")
    private boolean hstsIncludeSubdomains;

    @Value("${security.headers.hsts.preload:true}")
    private boolean hstsPreload;

    @Value("${security.headers.frame-options:DENY}")
    private String frameOptions;

    @Value("${security.headers.content-type-options:nosniff}")
    private String contentTypeOptions;

    @Value("${security.headers.xss-protection:1; mode=block}")
    private String xssProtection;

    @Value("${security.headers.csp:default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';}")
    private String contentSecurityPolicy;

    @Value("${security.headers.referrer-policy:strict-origin-when-cross-origin}")
    private String referrerPolicy;

    @Value("${security.headers.permissions-policy:geolocation=(), microphone=(), camera=()}")
    private String permissionsPolicy;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        // HSTS (HTTP Strict Transport Security)
        if (sslEnabled) {
            StringBuilder hstsValue = new StringBuilder("max-age=").append(hstsMaxAge);
            if (hstsIncludeSubdomains) {
                hstsValue.append("; includeSubDomains");
            }
            if (hstsPreload) {
                hstsValue.append("; preload");
            }
            response.setHeader("Strict-Transport-Security", hstsValue.toString());
        }

        // X-Frame-Options - Prevent clickjacking
        response.setHeader("X-Frame-Options", frameOptions);

        // X-Content-Type-Options - Prevent MIME sniffing
        response.setHeader("X-Content-Type-Options", contentTypeOptions);

        // X-XSS-Protection - Enable browser XSS filter
        response.setHeader("X-XSS-Protection", xssProtection);

        // Content-Security-Policy - Prevent XSS, injection attacks
        response.setHeader("Content-Security-Policy", contentSecurityPolicy);

        // Referrer-Policy - Control referrer information
        response.setHeader("Referrer-Policy", referrerPolicy);

        // Permissions-Policy (formerly Feature-Policy)
        response.setHeader("Permissions-Policy", permissionsPolicy);

        // Additional security headers for production
        if (sslEnabled) {
            // Expect-CT for certificate transparency
            response.setHeader("Expect-CT", "max-age=86400, enforce");
        }

        // Remove server identification headers
        response.setHeader("X-Powered-By", "");
        response.setHeader("Server", "");

        filterChain.doFilter(request, response);
    }
}
