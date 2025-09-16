package com.focushive.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Enhanced JWT Authentication Filter with blacklist support.
 *
 * This filter intercepts HTTP requests and validates JWT tokens using the enhanced
 * JwtTokenProvider with blacklist checking. It provides:
 *
 * - Fast token validation with blacklist checking
 * - Performance optimization through efficient Redis operations
 * - Graceful error handling without breaking the request flow
 * - Support for public endpoints that don't require authentication
 *
 * Performance Requirements:
 * - Token validation < 10ms including blacklist check
 * - Fail-safe design (continues request on errors)
 * - Minimal memory allocation for high-throughput scenarios
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = extractTokenFromRequest(request);

            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticateWithToken(token, request);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication in security context", e);
            // Continue without authentication - let Spring Security handle authorization
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from the Authorization header.
     *
     * @param request HTTP request
     * @return JWT token or null if not present/invalid format
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    /**
     * Authenticate the user with the provided JWT token.
     * Uses enhanced validation with blacklist checking.
     *
     * @param token JWT token
     * @param request HTTP request for additional context
     */
    private void authenticateWithToken(String token, HttpServletRequest request) {
        try {
            // Use enhanced validation with blacklist checking
            if (tokenProvider.validateTokenWithBlacklist(token)) {
                // Extract user information from token
                String username = tokenProvider.extractUsername(token);
                String userId = tokenProvider.extractUserId(token);
                String email = tokenProvider.extractEmail(token);
                List<SimpleGrantedAuthority> authorities = tokenProvider.extractAuthorities(token);

                // Create UserDetails with extracted information
                UserDetails userDetails = User.builder()
                        .username(username)
                        .password("") // Not used for JWT authentication
                        .authorities(authorities)
                        .build();

                // Create authentication token
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

                // Set additional details
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("User '{}' authenticated successfully", username);
            } else {
                log.debug("JWT token validation failed (expired, invalid, or blacklisted)");
            }
        } catch (Exception e) {
            log.error("Error during JWT authentication", e);
            // Don't throw - let the request continue without authentication
        }
    }

    /**
     * Determine if this filter should be skipped for the current request.
     * Public endpoints don't need JWT authentication.
     *
     * @param request HTTP request
     * @return true if filter should be skipped
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        // Skip authentication for public endpoints
        return path.startsWith("/actuator/") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/api-docs") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/webjars/") ||
               path.startsWith("/api/public/") ||
               path.startsWith("/api/demo/") ||
               path.startsWith("/error") ||
               path.equals("/") ||
               path.equals("/health");
    }

    /**
     * Extract user context information for logging and monitoring.
     * This can be used by other components to access current user information.
     *
     * @param token JWT token
     * @return User context map with essential information
     */
    public static class UserContext {
        public static String getCurrentUserId() {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
            return null;
        }

        public static List<String> getCurrentUserAuthorities() {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getAuthorities()
                        .stream()
                        .map(authority -> authority.getAuthority())
                        .toList();
            }
            return List.of();
        }

        public static boolean isAuthenticated() {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null && authentication.isAuthenticated();
        }
    }
}