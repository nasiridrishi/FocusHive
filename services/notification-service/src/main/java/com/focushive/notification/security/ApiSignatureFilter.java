package com.focushive.notification.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter to verify API request signatures for secure API communication.
 * This filter checks the signature of incoming API requests to ensure authenticity.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class ApiSignatureFilter extends OncePerRequestFilter {

    private final ApiSignatureService signatureService;

    @Value("${api.security.signature.enabled:true}")
    private boolean signatureEnabled;

    @Value("${api.security.signature.header:X-API-Signature}")
    private String signatureHeader;

    @Value("${api.security.signature.timestamp-header:X-API-Timestamp}")
    private String timestampHeader;

    @Value("${api.security.signature.key-header:X-API-Key}")
    private String apiKeyHeader;

    @Value("${api.security.signature.nonce-header:X-API-Nonce}")
    private String nonceHeader;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        if (!signatureEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        // Create a cacheable request wrapper to read body multiple times
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);

        // Extract signature components from headers
        String apiKey = cachedRequest.getHeader(apiKeyHeader);
        String signature = cachedRequest.getHeader(signatureHeader);
        String timestampStr = cachedRequest.getHeader(timestampHeader);
        String nonce = cachedRequest.getHeader(nonceHeader);

        // Check if all required headers are present
        if (apiKey == null || signature == null || timestampStr == null || nonce == null) {
            log.debug("Missing signature headers for request: {}", cachedRequest.getRequestURI());
            // For now, allow requests without signature for backward compatibility
            // In strict mode, this would return 401
            filterChain.doFilter(cachedRequest, response);
            return;
        }

        try {
            long timestamp = Long.parseLong(timestampStr);

            // Extract request components
            String httpMethod = cachedRequest.getMethod();
            String path = cachedRequest.getRequestURI();
            Map<String, String> headers = extractHeaders(cachedRequest);
            String body = cachedRequest.getBody();

            // Verify the signature
            boolean isValid = signatureService.verifySignature(
                apiKey, signature, timestamp, nonce,
                httpMethod, path, headers, body
            );

            if (!isValid) {
                sendUnauthorizedResponse(response, "Invalid API signature");
                return;
            }

            // Add API key to request attributes for downstream use
            cachedRequest.setAttribute("api.key", apiKey);
            cachedRequest.setAttribute("api.signature.verified", true);

            log.debug("API signature verified for key: {}", apiKey);

        } catch (Exception e) {
            log.error("Error verifying API signature", e);
            sendUnauthorizedResponse(response, "API signature verification failed");
            return;
        }

        // Continue with the filter chain
        filterChain.doFilter(cachedRequest, response);
    }

    /**
     * Extract headers from the request.
     */
    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();

        // Extract only X-API-* headers for signature verification
        var headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (headerName.toLowerCase().startsWith("x-api-")) {
                headers.put(headerName, request.getHeader(headerName));
            }
        }

        return headers;
    }

    /**
     * Send unauthorized response.
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write(String.format("""
            {
                "error": "Unauthorized",
                "message": "%s",
                "timestamp": %d
            }
            """, message, System.currentTimeMillis()));
    }

    /**
     * Determine if signature verification should be applied to this request.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Don't require signature for public endpoints
        if (path.equals("/health") ||
            path.equals("/actuator/health") ||
            path.equals("/actuator/prometheus") ||
            path.equals("/v3/api-docs") ||
            path.startsWith("/swagger-ui")) {
            return true;
        }

        // Don't require signature for authentication endpoints
        if (path.equals("/api/auth/login") ||
            path.equals("/api/auth/register") ||
            path.equals("/api/auth/refresh")) {
            return true;
        }

        // Require signature for all other API endpoints
        return !path.startsWith("/api/");
    }

    /**
     * Wrapper for HttpServletRequest that caches the request body.
     */
    private static class CachedBodyHttpServletRequest extends jakarta.servlet.http.HttpServletRequestWrapper {

        private final byte[] cachedBody;

        public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
        }

        @Override
        public jakarta.servlet.ServletInputStream getInputStream() {
            return new CachedBodyServletInputStream(cachedBody);
        }

        public String getBody() {
            return new String(cachedBody, StandardCharsets.UTF_8);
        }
    }

    /**
     * ServletInputStream implementation for cached body.
     */
    private static class CachedBodyServletInputStream extends jakarta.servlet.ServletInputStream {

        private final java.io.ByteArrayInputStream inputStream;

        public CachedBodyServletInputStream(byte[] cachedBody) {
            this.inputStream = new java.io.ByteArrayInputStream(cachedBody);
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(jakarta.servlet.ReadListener readListener) {
            // Not implemented for cached body
        }

        @Override
        public int read() {
            return inputStream.read();
        }
    }
}