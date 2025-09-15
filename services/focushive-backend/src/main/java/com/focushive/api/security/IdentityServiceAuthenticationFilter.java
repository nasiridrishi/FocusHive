package com.focushive.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Filter that validates JWT tokens with the Identity Service.
 * Replaces the local JWT validation with remote service calls.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdentityServiceAuthenticationFilter extends OncePerRequestFilter {

    private final JwtValidator jwtValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);

            if (StringUtils.hasText(token)) {
                // Use local JWT validation instead of calling Identity Service
                JwtValidator.ValidationResult validation = jwtValidator.validateToken(token);
                
                if (validation.isValid()) {
                    // Extract authorities from claims (default to USER role)
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

                    // Extract user info from claims
                    Map<String, Object> claims = validation.getClaims();

                    // Create custom principal with user info
                    IdentityPrincipal principal = IdentityPrincipal.builder()
                            .userId(validation.getUserId())
                            .username((String) claims.get("sub"))
                            .email(validation.getEmail())
                            // activePersona would need to be fetched separately
                            // For now, leave it null - persona details not in JWT
                            .activePersona(null)
                            .build();
                    
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    // Authentication successful - detailed logging controlled by config
                    if (log.isDebugEnabled()) {
                        log.debug("User authentication successful");
                    }
                } else {
                    // Token validation failed - detailed logging controlled by config
                    if (log.isDebugEnabled()) {
                        log.debug("Token validation failed: {}", validation.getError());
                    }
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