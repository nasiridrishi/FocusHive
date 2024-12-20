package com.focushive.notification.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter to handle service-to-service authentication using JWT tokens.
 * This filter checks for service account tokens and validates them accordingly.
 */
@Slf4j
@Component
public class ServiceAuthenticationFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;

    public ServiceAuthenticationFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        // Check if this is a service-to-service request
        String serviceName = request.getHeader("X-Service-Name");
        String correlationId = request.getHeader("X-Correlation-ID");

        if (serviceName != null) {
            log.debug("Service-to-service request detected from: {} with correlation ID: {}",
                     serviceName, correlationId);
        }

        // Extract token from Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.debug("JWT token found in Authorization header for path: {}", request.getRequestURI());

            try {
                // Decode and validate the JWT token
                Jwt jwt = jwtDecoder.decode(token);
                log.debug("JWT token successfully decoded. Issuer: {}, Subject: {}, Expires: {}", 
                    jwt.getIssuer(), jwt.getSubject(), jwt.getExpiresAt());

                // Log all claims for debugging
                if (log.isDebugEnabled()) {
                    log.debug("JWT claims: {}", jwt.getClaims());
                }

                // Check if this is a service account token
                String tokenType = jwt.getClaimAsString("type");
                log.debug("JWT token type: {}", tokenType);
                
                if ("service-account".equals(tokenType)) {
                    handleServiceToken(jwt, serviceName, correlationId);
                } else {
                    log.debug("Regular user token detected, will be handled by Spring Security OAuth2 filter");
                }
                // Regular user tokens are handled by the default Spring Security filter

            } catch (JwtException e) {
                log.warn("JWT validation failed for token from {}: {} ({})", 
                    request.getRemoteAddr(), e.getMessage(), e.getClass().getSimpleName());
                log.debug("JWT validation error details", e);
                // Let Spring Security handle invalid tokens
            }
        } else if (authHeader != null) {
            log.debug("Authorization header present but not Bearer token: {}", 
                authHeader.length() > 20 ? authHeader.substring(0, 20) + "..." : authHeader);
        } else {
            log.debug("No Authorization header found for path: {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Handle service account tokens specially.
     */
    private void handleServiceToken(Jwt jwt, String serviceName, String correlationId) {
        String service = jwt.getClaimAsString("service");
        List<String> roles = jwt.getClaimAsStringList("roles");
        List<String> permissions = jwt.getClaimAsStringList("permissions");
        
        // Validate service account token structure
        if (service == null) {
            log.warn("Service account token is missing 'service' claim. Token subject: {}", jwt.getSubject());
            return;
        }

        log.info("Service authentication successful for service: {} (header: {}) with correlation ID: {}",
                service, serviceName, correlationId);
        
        // Validate header vs token consistency
        if (serviceName != null && !serviceName.equals(service)) {
            log.warn("Service name mismatch: Header says '{}' but JWT token says '{}'", serviceName, service);
        }
        
        // Log roles and permissions for debugging
        log.debug("Service '{}' has roles: {} and permissions: {}", service, roles, permissions);

        // Handle null collections safely
        if (roles == null) {
            roles = Collections.emptyList();
            log.debug("No roles found in service token for: {}", service);
        }
        if (permissions == null) {
            permissions = Collections.emptyList();
            log.debug("No permissions found in service token for: {}", service);
        }

        // Create authorities from roles
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        // Add permissions as authorities too
        permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
        
        log.debug("Created {} total authorities for service '{}': {}", 
            authorities.size(), service, authorities);

        // Set authentication in security context
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "service-" + service,
                        null,
                        authorities
                );
        authentication.setDetails(jwt);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.info("Service authentication context set for: {} with {} authorities", service, authorities.size());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Don't filter public endpoints
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.equals("/health");
    }
}