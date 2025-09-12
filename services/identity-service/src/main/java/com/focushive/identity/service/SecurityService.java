package com.focushive.identity.service;

import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.Role;
import com.focushive.identity.entity.User;
import com.focushive.identity.repository.PersonaRepository;
import com.focushive.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Security service for centralized authorization logic in the Identity Service.
 * Provides methods for checking user access permissions and ownership.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityService {

    private final UserRepository userRepository;
    private final PersonaRepository personaRepository;

    /**
     * Get the currently authenticated user.
     *
     * @return Optional containing the current user, or empty if not authenticated
     */
    public Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("No authenticated user found in security context");
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return Optional.of(user);
        }

        if (principal instanceof String username) {
            // Handle JWT tokens where principal is username string
            return userRepository.findByUsernameOrEmail(username, username);
        }

        log.warn("Unknown principal type in security context: {}", principal.getClass());
        return Optional.empty();
    }

    /**
     * Get the current user's ID.
     *
     * @return Optional containing current user ID, or empty if not authenticated
     */
    public Optional<UUID> getCurrentUserId() {
        return getCurrentUser().map(User::getId);
    }

    /**
     * Check if the current user can access user data for the given user ID.
     *
     * @param userId The user ID to check access for
     * @return true if current user can access the user data
     */
    public boolean hasAccessToUser(UUID userId) {
        if (userId == null) {
            log.debug("hasAccessToUser called with null userId");
            return false;
        }

        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) {
            log.debug("No authenticated user for hasAccessToUser check");
            return false;
        }

        User user = currentUser.get();
        
        // Users can always access their own data
        if (userId.equals(user.getId())) {
            log.debug("User {} accessing own data", user.getUsername());
            return true;
        }

        // Admins can access any user data
        if (user.hasRoleLevel(Role.ADMIN)) {
            log.debug("Admin user {} accessing user data for {}", user.getUsername(), userId);
            return true;
        }

        log.debug("User {} denied access to user data for {}", user.getUsername(), userId);
        return false;
    }

    /**
     * Check if the current user owns the specified persona.
     *
     * @param personaId The persona ID to check ownership for
     * @return true if current user owns the persona
     */
    public boolean hasAccessToPersona(UUID personaId) {
        if (personaId == null) {
            log.debug("hasAccessToPersona called with null personaId");
            return false;
        }

        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) {
            log.debug("No authenticated user for hasAccessToPersona check");
            return false;
        }

        Optional<Persona> persona = personaRepository.findById(personaId);
        if (persona.isEmpty()) {
            log.debug("Persona {} not found", personaId);
            return false;
        }

        User user = currentUser.get();
        UUID personaOwnerId = persona.get().getUser().getId();

        // Users can access their own personas
        if (user.getId().equals(personaOwnerId)) {
            log.debug("User {} accessing own persona {}", user.getUsername(), personaId);
            return true;
        }

        // Admins can access any persona
        if (user.hasRoleLevel(Role.ADMIN)) {
            log.debug("Admin user {} accessing persona {} for user {}", 
                     user.getUsername(), personaId, personaOwnerId);
            return true;
        }

        log.debug("User {} denied access to persona {}", user.getUsername(), personaId);
        return false;
    }

    /**
     * Check if the current user is the owner of a resource.
     *
     * @param ownerId The ID of the resource owner
     * @return true if current user is the owner
     */
    public boolean isOwner(UUID ownerId) {
        if (ownerId == null) {
            log.debug("isOwner called with null ownerId");
            return false;
        }

        return getCurrentUserId()
                .map(currentUserId -> currentUserId.equals(ownerId))
                .orElse(false);
    }

    /**
     * Check if the current user has the specified role.
     *
     * @param role The role to check for
     * @return true if current user has the role
     */
    public boolean hasRole(String role) {
        if (role == null) {
            log.debug("hasRole called with null role");
            return false;
        }

        try {
            Role roleEnum = Role.valueOf(role);
            return hasRole(roleEnum);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role name: {}", role);
            return false;
        }
    }

    /**
     * Check if the current user has the specified role.
     *
     * @param role The role to check for
     * @return true if current user has the role
     */
    public boolean hasRole(Role role) {
        if (role == null) {
            log.debug("hasRole called with null role");
            return false;
        }

        return getCurrentUser()
                .map(user -> user.hasRole(role))
                .orElse(false);
    }

    /**
     * Check if the current user has a role with equal or higher privilege level.
     *
     * @param minimumRole The minimum role level required
     * @return true if current user has the required privilege level
     */
    public boolean hasRoleLevel(String minimumRole) {
        if (minimumRole == null) {
            log.debug("hasRoleLevel called with null minimumRole");
            return false;
        }

        try {
            Role roleEnum = Role.valueOf(minimumRole);
            return hasRoleLevel(roleEnum);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role name: {}", minimumRole);
            return false;
        }
    }

    /**
     * Check if the current user has a role with equal or higher privilege level.
     *
     * @param minimumRole The minimum role level required
     * @return true if current user has the required privilege level
     */
    public boolean hasRoleLevel(Role minimumRole) {
        if (minimumRole == null) {
            log.debug("hasRoleLevel called with null minimumRole");
            return false;
        }

        return getCurrentUser()
                .map(user -> user.hasRoleLevel(minimumRole))
                .orElse(false);
    }

    /**
     * Check if the current user can perform administrative actions.
     *
     * @return true if current user has administrative privileges
     */
    public boolean isAdmin() {
        return hasRoleLevel(Role.ADMIN);
    }

    /**
     * Check if the current user can manage hives.
     *
     * @return true if current user can create and manage hives
     */
    public boolean canManageHives() {
        return hasRoleLevel(Role.HIVE_OWNER);
    }

    /**
     * Check if the current user has premium features access.
     *
     * @return true if current user has premium features
     */
    public boolean hasPremiumFeatures() {
        return hasRoleLevel(Role.PREMIUM_USER);
    }

    /**
     * Log an authorization attempt for audit purposes.
     *
     * @param operation The operation being attempted
     * @param resourceId The resource being accessed
     * @param granted Whether access was granted
     */
    public void logAuthorizationAttempt(String operation, String resourceId, boolean granted) {
        Optional<User> currentUser = getCurrentUser();
        String username = currentUser.map(User::getUsername).orElse("anonymous");
        
        if (granted) {
            log.info("Authorization granted - User: {}, Operation: {}, Resource: {}", 
                    username, operation, resourceId);
        } else {
            log.warn("Authorization denied - User: {}, Operation: {}, Resource: {}", 
                    username, operation, resourceId);
        }
    }
}