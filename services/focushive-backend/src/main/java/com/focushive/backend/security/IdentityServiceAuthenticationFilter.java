package com.focushive.backend.security;

import com.focushive.api.dto.identity.TokenValidationResponse;
import com.focushive.api.service.IdentityIntegrationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT authentication filter that delegates token validation to Identity Service.
 * Replaces local JWT validation with microservice-based validation.
 */
@Slf4j
@Component("backendIdentityServiceAuthenticationFilter")
@RequiredArgsConstructor
@org.springframework.context.annotation.Profile("!test") // Disable in test profile
public class IdentityServiceAuthenticationFilter extends OncePerRequestFilter {

    private final IdentityIntegrationService identityIntegrationService;
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String token = extractToken(request);
        
        if (token != null) {
            try {
                // Validate token with Identity Service
                TokenValidationResponse validationResponse = identityIntegrationService.validateToken(BEARER_PREFIX + token);
                
                if (validationResponse.isValid()) {
                    // Create authentication object
                    List<SimpleGrantedAuthority> authorities = validationResponse.getAuthorities().stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                    
                    // Create custom principal with user details
                    String activePersonaId = validationResponse.getActivePersona() != null
                            ? validationResponse.getActivePersona().getId().toString()
                            : null;
                    IdentityServicePrincipal principal = new IdentityServicePrincipal(
                            validationResponse.getUserId(),
                            validationResponse.getEmail(),
                            activePersonaId
                    );
                    
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    // Authentication successful - detailed logging controlled by config
                    if (log.isDebugEnabled()) {
                        log.debug("User authentication successful");
                    }
                } else {
                    // Token validation failed - detailed logging controlled by config
                    if (log.isDebugEnabled()) {
                        log.debug("Token validation failed: {}", validationResponse.getErrorMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Error during token validation", e);
                // Continue without authentication
            }
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header.
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Skip filter for public endpoints
        String path = request.getServletPath();
        return path.startsWith("/api/public/") || 
               path.startsWith("/actuator/") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/api-docs");
    }
}