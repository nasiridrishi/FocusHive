package com.focushive.api.security;

import com.focushive.api.client.IdentityServiceClient;
import com.focushive.api.dto.identity.TokenValidationResponse;
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
import java.util.stream.Collectors;

/**
 * Filter that validates JWT tokens with the Identity Service.
 * Replaces the local JWT validation with remote service calls.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdentityServiceAuthenticationFilter extends OncePerRequestFilter {
    
    private final IdentityServiceClient identityServiceClient;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            
            if (StringUtils.hasText(token)) {
                TokenValidationResponse validation = identityServiceClient.validateToken("Bearer " + token);
                
                if (validation.isValid()) {
                    // Create authentication object
                    List<SimpleGrantedAuthority> authorities = validation.getAuthorities().stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                    
                    // Create custom principal with user info and active persona
                    IdentityPrincipal principal = IdentityPrincipal.builder()
                            .userId(validation.getUserId())
                            .username(validation.getUsername())
                            .email(validation.getEmail())
                            .activePersona(validation.getActivePersona())
                            .build();
                    
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("Authenticated user: {} with persona: {}", 
                            validation.getUsername(), 
                            validation.getActivePersona() != null ? validation.getActivePersona().getName() : "default");
                } else {
                    log.debug("Token validation failed: {}", validation.getErrorMessage());
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: ", e);
            // Continue without authentication - let Spring Security handle it
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extract JWT token from Authorization header.
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}