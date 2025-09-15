package com.focushive.notification.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Filter to handle API key authentication for service-to-service communication.
 * This provides an alternative authentication mechanism for internal services
 * that cannot use JWT tokens or when JWT validation fails.
 */
@Slf4j
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String SOURCE_SERVICE_HEADER = "X-Source-Service";

    // Map of service names to their API keys
    private final Map<String, String> serviceApiKeys = new HashMap<>();

    public ApiKeyAuthenticationFilter(
            @Value("${service.api-keys.identity-service:}") String identityServiceApiKey,
            @Value("${service.api-keys.backend-service:}") String backendServiceApiKey,
            @Value("${service.api-keys.buddy-service:}") String buddyServiceApiKey,
            @Value("${service.api-keys.admin-service:}") String adminServiceApiKey) {

        // Initialize API keys for known services
        if (identityServiceApiKey != null && !identityServiceApiKey.isEmpty()) {
            serviceApiKeys.put("identity-service", identityServiceApiKey);
            log.info("API key configured for identity-service");
        }
        if (backendServiceApiKey != null && !backendServiceApiKey.isEmpty()) {
            serviceApiKeys.put("backend-service", backendServiceApiKey);
            log.info("API key configured for backend-service");
        }
        if (buddyServiceApiKey != null && !buddyServiceApiKey.isEmpty()) {
            serviceApiKeys.put("buddy-service", buddyServiceApiKey);
            log.info("API key configured for buddy-service");
        }
        if (adminServiceApiKey != null && !adminServiceApiKey.isEmpty()) {
            serviceApiKeys.put("admin-service", adminServiceApiKey);
            log.info("API key configured for admin-service");
        }

        if (serviceApiKeys.isEmpty()) {
            log.warn("No API keys configured for service authentication. API key authentication will be disabled.");
        } else {
            log.info("API key authentication filter initialized with {} service keys", serviceApiKeys.size());
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Check if request already has authentication
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            // Already authenticated, skip API key check
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(API_KEY_HEADER);
        String sourceService = request.getHeader(SOURCE_SERVICE_HEADER);

        // If no API key provided, continue to next filter (JWT authentication)
        if (apiKey == null || apiKey.trim().isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Log API key authentication attempt
        log.debug("API key authentication attempt from service: {} for path: {}",
                  sourceService, request.getRequestURI());

        // Validate API key
        boolean authenticated = false;
        String authenticatedService = null;

        if (sourceService != null && !sourceService.trim().isEmpty()) {
            // Check if source service header matches known service
            String expectedApiKey = serviceApiKeys.get(sourceService);
            if (expectedApiKey != null && expectedApiKey.equals(apiKey)) {
                authenticated = true;
                authenticatedService = sourceService;
            } else if (expectedApiKey != null) {
                log.warn("Invalid API key for service: {} from IP: {}",
                        sourceService, request.getRemoteAddr());
            }
        } else {
            // No source service specified, check all known API keys
            for (Map.Entry<String, String> entry : serviceApiKeys.entrySet()) {
                if (entry.getValue().equals(apiKey)) {
                    authenticated = true;
                    authenticatedService = entry.getKey();
                    log.debug("API key matched for service: {}", authenticatedService);
                    break;
                }
            }
        }

        if (authenticated && authenticatedService != null) {
            // Create authentication token for service account
            ServiceApiKeyAuthenticationToken authToken = new ServiceApiKeyAuthenticationToken(
                    authenticatedService,
                    apiKey,
                    List.of(
                            new SimpleGrantedAuthority("SERVICE"),
                            new SimpleGrantedAuthority("ROLE_SERVICE"),
                            new SimpleGrantedAuthority("notification.send"),
                            new SimpleGrantedAuthority("notification.bulk")
                    )
            );

            authToken.setDetails(new ServiceAuthenticationDetails(
                    authenticatedService,
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent")
            ));

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authToken);

            log.info("Service '{}' authenticated successfully via API key from IP: {}",
                    authenticatedService, request.getRemoteAddr());
        } else if (apiKey != null) {
            // API key provided but invalid
            log.warn("Invalid API key attempted from IP: {} with source service: {}",
                    request.getRemoteAddr(), sourceService);
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Custom authentication token for API key authenticated services
     */
    public static class ServiceApiKeyAuthenticationToken extends AbstractAuthenticationToken {
        private final String serviceName;
        private final String apiKey;

        public ServiceApiKeyAuthenticationToken(String serviceName, String apiKey,
                                                List<SimpleGrantedAuthority> authorities) {
            super(authorities);
            this.serviceName = serviceName;
            this.apiKey = apiKey;
            setAuthenticated(true);
        }

        @Override
        public Object getCredentials() {
            return apiKey;
        }

        @Override
        public Object getPrincipal() {
            return serviceName;
        }

        public String getServiceName() {
            return serviceName;
        }
    }

    /**
     * Additional details about service authentication
     */
    public static class ServiceAuthenticationDetails {
        private final String serviceName;
        private final String remoteAddress;
        private final String userAgent;

        public ServiceAuthenticationDetails(String serviceName, String remoteAddress, String userAgent) {
            this.serviceName = serviceName;
            this.remoteAddress = remoteAddress;
            this.userAgent = userAgent;
        }

        public String getServiceName() {
            return serviceName;
        }

        public String getRemoteAddress() {
            return remoteAddress;
        }

        public String getUserAgent() {
            return userAgent;
        }

        @Override
        public String toString() {
            return String.format("Service: %s, IP: %s, Agent: %s",
                    serviceName, remoteAddress, userAgent);
        }
    }
}