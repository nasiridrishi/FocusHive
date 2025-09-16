package com.focushive.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for extracting user information from JWT tokens.
 * Provides methods to access current user context and perform authorization checks.
 */
@Service
@Slf4j
public class UserContextService {

    /**
     * Gets the current user ID from JWT token.
     *
     * @return user ID or null if not authenticated
     */
    public String getCurrentUserId() {
        Jwt jwt = getCurrentJwt();
        return jwt != null ? jwt.getSubject() : null;
    }

    /**
     * Gets the current user email from JWT token.
     *
     * @return user email or null if not available
     */
    public String getCurrentUserEmail() {
        Jwt jwt = getCurrentJwt();
        return jwt != null ? jwt.getClaimAsString("email") : null;
    }

    /**
     * Gets the current user name from JWT token.
     *
     * @return user name or null if not available
     */
    public String getCurrentUserName() {
        Jwt jwt = getCurrentJwt();
        return jwt != null ? jwt.getClaimAsString("name") : null;
    }

    /**
     * Gets the current user roles from JWT token.
     *
     * @return list of roles or empty list if not available
     */
    public List<String> getCurrentUserRoles() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return Collections.emptyList();
        }

        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

    /**
     * Checks if the current user has a specific role.
     *
     * @param role the role to check
     * @return true if user has the role, false otherwise
     */
    public boolean hasRole(String role) {
        return getCurrentUserRoles().contains(role);
    }

    /**
     * Checks if the current user has admin role.
     *
     * @return true if user is admin, false otherwise
     */
    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    /**
     * Gets complete user context information.
     *
     * @return UserContext object with all available information
     */
    public UserContext getCurrentUserContext() {
        return new UserContext(
                getCurrentUserId(),
                getCurrentUserEmail(),
                getCurrentUserName(),
                getCurrentUserRoles()
        );
    }

    /**
     * Checks if the current user can access a resource belonging to a specific user.
     * Users can access their own resources, admins can access any resource.
     *
     * @param resourceUserId the user ID that owns the resource
     * @return true if access is allowed, false otherwise
     */
    public boolean canAccessResource(String resourceUserId) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }

        // Users can access their own resources
        if (currentUserId.equals(resourceUserId)) {
            return true;
        }

        // Admins can access any resource
        return isAdmin();
    }

    /**
     * Gets the current JWT token.
     *
     * @return JWT token or null if not available
     */
    private Jwt getCurrentJwt() {
        Authentication authentication = getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken();
        }
        return null;
    }

    /**
     * Gets the current authentication object.
     *
     * @return Authentication object or null if not available
     */
    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Record representing user context information.
     */
    public record UserContext(
            String userId,
            String email,
            String name,
            List<String> roles
    ) {
        /**
         * Checks if the user context represents an admin user.
         *
         * @return true if user has admin role
         */
        public boolean isAdmin() {
            return roles != null && roles.contains("ROLE_ADMIN");
        }

        /**
         * Checks if the user context has a specific role.
         *
         * @param role the role to check
         * @return true if user has the role
         */
        public boolean hasRole(String role) {
            return roles != null && roles.contains(role);
        }

        /**
         * Checks if the user context is authenticated (has a user ID).
         *
         * @return true if authenticated
         */
        public boolean isAuthenticated() {
            return userId != null;
        }
    }
}