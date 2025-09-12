package com.focushive.api.service;

import com.focushive.hive.entity.Hive;
import com.focushive.hive.entity.HiveMember;
import com.focushive.hive.repository.HiveMemberRepository;
import com.focushive.hive.repository.HiveRepository;
import com.focushive.timer.entity.FocusSession;
import com.focushive.timer.repository.FocusSessionRepository;
import com.focushive.user.entity.User;
import com.focushive.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Security service for centralized authorization logic in the FocusHive Backend.
 * Provides methods for checking user access permissions to hives, timers, and other resources.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityService {

    private final UserRepository userRepository;
    private final HiveRepository hiveRepository;
    private final HiveMemberRepository hiveMemberRepository;
    private final FocusSessionRepository focusSessionRepository;

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

        if (principal instanceof String identifier) {
            // Handle JWT tokens where principal is username or userId string
            // Try UUID first, then username/email
            try {
                UUID userId = UUID.fromString(identifier);
                return userRepository.findById(userId);
            } catch (IllegalArgumentException e) {
                // Not a UUID, try as username/email
                return userRepository.findByUsernameOrEmail(identifier, identifier);
            }
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

        // Admins can access any user data (when Role is implemented)
        // TODO: Add role check when User entity includes roles
        // if (user.hasRoleLevel(Role.ADMIN)) {
        //     log.debug("Admin user {} accessing user data for {}", user.getUsername(), userId);
        //     return true;
        // }

        log.debug("User {} denied access to user data for {}", user.getUsername(), userId);
        return false;
    }

    /**
     * Check if the current user can access the specified hive.
     *
     * @param hiveId The hive ID to check access for
     * @return true if current user can access the hive
     */
    public boolean hasAccessToHive(UUID hiveId) {
        if (hiveId == null) {
            log.debug("hasAccessToHive called with null hiveId");
            return false;
        }

        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) {
            log.debug("No authenticated user for hasAccessToHive check");
            return false;
        }

        User user = currentUser.get();
        
        // Check if hive exists
        Optional<Hive> hive = hiveRepository.findById(hiveId);
        if (hive.isEmpty()) {
            log.debug("Hive {} not found", hiveId);
            return false;
        }

        // Check if user is the owner
        if (hive.get().getOwner().getId().equals(user.getId())) {
            log.debug("User {} is owner of hive {}", user.getUsername(), hiveId);
            return true;
        }

        // Check if user is a member
        boolean isMember = hiveMemberRepository.existsByHiveIdAndUserId(hiveId, user.getId());
        if (isMember) {
            log.debug("User {} is member of hive {}", user.getUsername(), hiveId);
            return true;
        }

        // Check if hive is public (for read access)
        if (hive.get().getIsPublic()) {
            log.debug("User {} accessing public hive {}", user.getUsername(), hiveId);
            return true;
        }

        log.debug("User {} denied access to hive {}", user.getUsername(), hiveId);
        return false;
    }

    /**
     * Check if the current user is a member of the specified hive.
     *
     * @param hiveId The hive ID to check membership for
     * @return true if current user is a member of the hive
     */
    public boolean isHiveMember(UUID hiveId) {
        if (hiveId == null) {
            log.debug("isHiveMember called with null hiveId");
            return false;
        }

        return getCurrentUserId()
                .map(userId -> hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId))
                .orElse(false);
    }

    /**
     * Check if the current user is the owner of the specified hive.
     *
     * @param hiveId The hive ID to check ownership for
     * @return true if current user owns the hive
     */
    public boolean isHiveOwner(UUID hiveId) {
        if (hiveId == null) {
            log.debug("isHiveOwner called with null hiveId");
            return false;
        }

        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) {
            return false;
        }

        return hiveRepository.findById(hiveId)
                .map(hive -> hive.getOwner().getId().equals(currentUser.get().getId()))
                .orElse(false);
    }

    /**
     * Check if the current user has moderator or owner role in the specified hive.
     *
     * @param hiveId The hive ID to check moderator access for
     * @return true if current user can moderate the hive
     */
    public boolean canModerateHive(UUID hiveId) {
        if (hiveId == null) {
            log.debug("canModerateHive called with null hiveId");
            return false;
        }

        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) {
            return false;
        }

        // Check if user is owner
        if (isHiveOwner(hiveId)) {
            return true;
        }

        // Check if user is moderator
        return hiveMemberRepository.findByHiveIdAndUserId(hiveId, currentUser.get().getId())
                .map(member -> member.getRole() == HiveMember.MemberRole.MODERATOR)
                .orElse(false);
    }

    /**
     * Check if the current user can access the specified timer session.
     *
     * @param sessionId The session ID to check access for
     * @return true if current user can access the session
     */
    public boolean hasAccessToTimer(UUID sessionId) {
        if (sessionId == null) {
            log.debug("hasAccessToTimer called with null sessionId");
            return false;
        }

        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) {
            return false;
        }

        return focusSessionRepository.findById(sessionId)
                .map(session -> session.getUserId().equals(currentUser.get().getId().toString()))
                .orElse(false);
    }

    /**
     * Check if the current user can access chat in the specified hive.
     *
     * @param hiveId The hive ID to check chat access for
     * @return true if current user can access chat in the hive
     */
    public boolean hasAccessToChat(UUID hiveId) {
        // Chat access is the same as hive member access
        return isHiveMember(hiveId) || isHiveOwner(hiveId);
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
     * Note: This is a placeholder until Role enum is implemented in Backend User entity.
     *
     * @param role The role to check for
     * @return true if current user has the role
     */
    public boolean hasRole(String role) {
        if (role == null) {
            log.debug("hasRole called with null role");
            return false;
        }

        // TODO: Implement role checking when User entity includes roles
        // For now, return true for basic USER role for authenticated users
        return getCurrentUser().isPresent() && "USER".equals(role);
    }

    /**
     * Check if the current user can perform administrative actions.
     * Note: This is a placeholder until role system is fully implemented.
     *
     * @return true if current user has administrative privileges
     */
    public boolean isAdmin() {
        // TODO: Implement admin role check
        return false; // For now, no admin access
    }

    /**
     * Check if the current user can manage hives.
     * Note: This is a placeholder until role system is fully implemented.
     *
     * @return true if current user can create and manage hives
     */
    public boolean canManageHives() {
        // TODO: Implement hive owner role check
        // For now, all authenticated users can create hives
        return getCurrentUser().isPresent();
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