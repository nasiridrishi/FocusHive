package com.focushive.identity.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * OWASP A01 compliant CORS filter that properly handles unauthorized origins.
 *
 * For unauthorized origins:
 * - Returns 200 OK for OPTIONS requests (CORS spec compliance)
 * - Does NOT set Access-Control-Allow-Origin header (security)
 * - Logs security violations for monitoring
 *
 * For authorized origins:
 * - Returns 200 OK for OPTIONS requests
 * - Sets appropriate CORS headers
 * - Allows the request to proceed
 */
@Component
public class OWASPCompliantCorsFilter implements Filter {

    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY.CORS");

    @Value("${security.cors.allowed-origins}")
    private String corsAllowedOrigins;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Only handle CORS for OPTIONS requests - let Spring handle other requests
        if ("OPTIONS".equals(httpRequest.getMethod())) {
            handleOptionsRequest(httpRequest, httpResponse);
            return; // Don't continue the filter chain for OPTIONS
        }

        // For non-OPTIONS requests, continue with normal processing
        chain.doFilter(request, response);
    }

    private void handleOptionsRequest(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");

        // Parse trusted origins from configuration
        List<String> trustedOrigins = Arrays.asList(corsAllowedOrigins.split(","));

        // Check if origin is trusted
        boolean isTrusted = origin != null && trustedOrigins.contains(origin);

        if (isTrusted) {
            // Trusted origin - set full CORS headers
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            response.setHeader("Access-Control-Allow-Headers",
                "Content-Type,Authorization,X-Requested-With,Accept,Origin,Access-Control-Request-Method,Access-Control-Request-Headers");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Max-Age", "3600");

            securityLogger.debug("CORS: Authorized origin access: {}", origin);
        } else {
            // Unauthorized origin - return 200 OK but NO CORS headers
            // This is OWASP A01 compliant: allow preflight but don't enable CORS
            if (origin != null) {
                securityLogger.warn("CORS violation: Unauthorized origin attempted access: {}", origin);
            }
            // Intentionally NOT setting any Access-Control-* headers
        }

        // Always return 200 OK for OPTIONS (CORS spec compliance)
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain");
        response.setContentLength(0);
    }
}