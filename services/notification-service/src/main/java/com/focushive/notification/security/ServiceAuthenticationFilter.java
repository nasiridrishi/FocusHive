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

            try {
                // Decode and validate the JWT token
                Jwt jwt = jwtDecoder.decode(token);

                // Check if this is a service account token
                String tokenType = jwt.getClaimAsString("type");
                if ("service-account".equals(tokenType)) {
                    handleServiceToken(jwt, serviceName, correlationId);
                }
                // Regular user tokens are handled by the default Spring Security filter

            } catch (JwtException e) {
                log.debug("JWT validation failed: {}", e.getMessage());
                // Let Spring Security handle invalid tokens
            }
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

        log.info("Service authentication successful for service: {} (header: {}) with correlation ID: {}",
                service, serviceName, correlationId);

        // Create authorities from roles
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        // Add permissions as authorities too
        permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        // Set authentication in security context
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "service-" + service,
                        null,
                        authorities
                );
        authentication.setDetails(jwt);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("Set service authentication for: {} with {} authorities", service, authorities.size());
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