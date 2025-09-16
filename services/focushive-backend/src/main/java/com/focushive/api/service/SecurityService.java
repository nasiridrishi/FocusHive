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
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Security service implementing comprehensive authorization rules.
 * Phase 2, Task 2.4: Authorization Rules implementation with TDD approach.
 *
 * Features:
 * - Resource ownership verification
 * - Role-based access control (RBAC)
 * - Permission-based access control
 * - Dynamic SpEL expression evaluation
 * - Security audit logging
 * - Permission change tracking
 */
@Service("securityService")
@Slf4j
@RequiredArgsConstructor
public class SecurityService {

    private final UserRepository userRepository;
    private final HiveRepository hiveRepository;
    private final HiveMemberRepository hiveMemberRepository;
    private final FocusSessionRepository focusSessionRepository;

    // For audit logging and permission tracking
    private final Queue<SecurityAuditEvent> auditEvents = new ConcurrentLinkedQueue<>();
    private final Map<String, List<PermissionChangeEvent>> permissionHistory = new ConcurrentHashMap<>();

    // SpEL expression parser for dynamic permission evaluation
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    // ========================================================================
    // CORE USER AND AUTHENTICATION METHODS
    // ========================================================================

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
            // Try as ID first, then username/email
            Optional<User> userById = userRepository.findById(identifier);
            if (userById.isPresent()) {
                return userById;
            }
            // Not found by ID, try as username/email
            return userRepository.findByUsername(identifier);
        }

        log.warn("Unknown principal type in security context: {}", principal.getClass());
        return Optional.empty();
    }

    /**
     * Get the current user's ID.
     *
     * @return Optional containing current user ID, or empty if not authenticated
     */
    public Optional<String> getCurrentUserId() {
        return getCurrentUser().map(User::getId);
    }

    // ========================================================================
    // RESOURCE ACCESS CONTROL METHODS
    // ========================================================================

    /**
     * Check if the current user can access user data for the given user ID.
     *
     * @param userId The user ID to check access for
     * @return true if current user can access the user data
     */
    public boolean hasAccessToUser(String userId) {
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

        // Check if user has ADMIN role
        if (hasSystemPermission("system:manage-users")) {
            log.debug("Admin user {} accessing user data for {}", user.getUsername(), userId);
            return true;
        }

        log.debug("User {} denied access to user data for {}", user.getUsername(), userId);
        return false;
    }

    /**
     * Check if the current user can access the specified hive.
     *
     * @param hiveId The hive ID to check access for
     * @return true if current user can access the hive
     */
    public boolean hasAccessToHive(String hiveId) {
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
    public boolean isHiveMember(String hiveId) {
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
    public boolean isHiveOwner(String hiveId) {
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
    public boolean canModerateHive(String hiveId) {
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
     * Check if the current user can manage hives.
     *
     * @return true if current user can create and manage hives
     */
    public boolean canManageHives() {
        // For now, all authenticated users can create hives
        return getCurrentUser().isPresent();
    }

    // ========================================================================
    // PERMISSION-BASED ACCESS CONTROL METHODS
    // ========================================================================

    /**
     * Check if current user has a specific permission.
     *
     * @param permission The permission to check (e.g., "hive:create")
     * @return true if user has the permission
     */
    public boolean hasPermission(String permission) {
        if (permission == null) {
            return false;
        }

        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) {
            return false;
        }

        // Basic permission mapping based on user roles and authentication
        return switch (permission) {
            case "hive:create" -> canManageHives();
            case "hive:read" -> true; // Authenticated users can read public hives
            case "user:profile" -> true; // Users can access their own profile
            default -> false;
        };
    }

    /**
     * Check if current user has a specific permission on a resource.
     *
     * @param permission The permission to check (e.g., "hive:update")
     * @param resourceId The resource ID
     * @return true if user has the permission on the resource
     */
    public boolean hasPermissionOnResource(String permission, String resourceId) {
        if (permission == null || resourceId == null) {
            return false;
        }

        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) {
            return false;
        }

        return switch (permission) {
            case "hive:read" -> hasAccessToHive(resourceId);
            case "hive:update" -> canModerateHive(resourceId);
            case "hive:delete" -> isHiveOwner(resourceId);
            case "member:invite" -> canModerateHive(resourceId);
            case "member:remove" -> canModerateHive(resourceId);
            case "member:promote" -> isHiveOwner(resourceId);
            default -> false;
        };
    }

    /**
     * Check if current user has system-level permissions.
     *
     * @param permission The system permission to check
     * @return true if user has the system permission
     */
    public boolean hasSystemPermission(String permission) {
        if (permission == null) {
            return false;
        }

        // Get current user's authorities
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        boolean hasAdminRole = authorities.stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        return switch (permission) {
            case "system:manage-users" -> hasAdminRole;
            case "system:manage-hives" -> hasAdminRole;
            case "system:access-any-hive" -> hasAdminRole;
            case "system:admin-panel" -> hasAdminRole;
            default -> false;
        };
    }

    /**
     * Evaluate a SpEL expression for dynamic permission checking.
     *
     * @param expression The SpEL expression to evaluate
     * @return true if expression evaluates to true
     */
    public boolean evaluatePermission(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }

        try {
            StandardEvaluationContext context = new StandardEvaluationContext();

            // Set up context with current authentication
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            context.setVariable("authentication", auth);

            // Add common functions
            context.setVariable("securityService", this);

            // Add isAuthenticated() function
            context.registerFunction("isAuthenticated",
                SecurityService.class.getDeclaredMethod("isCurrentUserAuthenticated"));

            // Add hasRole() function
            context.registerFunction("hasRole",
                SecurityService.class.getDeclaredMethod("hasRole", String.class));

            Boolean result = expressionParser.parseExpression(expression).getValue(context, Boolean.class);
            return Boolean.TRUE.equals(result);

        } catch (Exception e) {
            log.warn("Failed to evaluate permission expression: {}", expression, e);
            return false;
        }
    }

    /**
     * Helper method for SpEL expression evaluation.
     * @return true if current user is authenticated
     */
    public static boolean isCurrentUserAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
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

        // Get current user's authorities
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        return authorities.stream()
            .anyMatch(authority -> ("ROLE_" + role).equals(authority.getAuthority()));
    }

    // ========================================================================
    // AUDIT AND PERMISSION CHANGE TRACKING
    // ========================================================================

    /**
     * Log an authorization attempt for audit purposes.
     *
     * @param operation The operation being attempted
     * @param resourceId The resource being accessed
     * @param granted Whether access was granted
     */
    public void logAuthorizationAttempt(String operation, String resourceId, boolean granted) {
        Optional<User> currentUser = getCurrentUser();
        String userId = currentUser.map(User::getId).orElse("anonymous");
        String username = currentUser.map(User::getUsername).orElse("anonymous");

        // Create audit event
        SecurityAuditEvent auditEvent = new SecurityAuditEvent(
            userId, operation, resourceId, granted, LocalDateTime.now()
        );
        auditEvents.offer(auditEvent);

        // Limit audit event queue size (keep last 10000 events)
        while (auditEvents.size() > 10000) {
            auditEvents.poll();
        }

        if (granted) {
            log.info("Authorization granted - User: {}, Operation: {}, Resource: {}",
                    username, operation, resourceId);
        } else {
            log.warn("Authorization denied - User: {}, Operation: {}, Resource: {}",
                    username, operation, resourceId);
        }
    }

    /**
     * Get the last audit event (for testing purposes).
     *
     * @return The most recent audit event, or null if none
     */
    public SecurityAuditEvent getLastAuditEvent() {
        return auditEvents.isEmpty() ? null :
            auditEvents.toArray(new SecurityAuditEvent[0])[auditEvents.size() - 1];
    }

    /**
     * Record a permission change event.
     *
     * @param userId The user whose permissions changed
     * @param permission The permission that changed
     * @param resourceId The resource affected
     * @param changeType The type of change (GRANTED/REVOKED)
     */
    public void recordPermissionChange(String userId, String permission, String resourceId,
                                     PermissionChangeEvent.PermissionChangeType changeType) {
        Optional<User> currentUser = getCurrentUser();
        String changedByUserId = currentUser.map(User::getId).orElse("system");

        PermissionChangeEvent event = new PermissionChangeEvent(
            userId, permission, resourceId, changeType, LocalDateTime.now(),
            changedByUserId, "Permission change via SecurityService"
        );

        String key = userId + ":" + resourceId;
        permissionHistory.computeIfAbsent(key, k -> new ArrayList<>()).add(event);

        log.info("Permission change recorded: {}", event);
    }

    /**
     * Get permission change history for a user and resource.
     *
     * @param userId The user ID
     * @param resourceId The resource ID
     * @return List of permission change events
     */
    public List<PermissionChangeEvent> getPermissionChangeHistory(String userId, String resourceId) {
        String key = userId + ":" + resourceId;
        return permissionHistory.getOrDefault(key, Collections.emptyList());
    }

    // ========================================================================
    // ADDITIONAL HELPER METHODS
    // ========================================================================

    /**
     * Check if the current user can access the specified timer session.
     *
     * @param sessionId The session ID to check access for
     * @return true if current user can access the session
     */
    public boolean hasAccessToTimer(String sessionId) {
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
    public boolean hasAccessToChat(String hiveId) {
        // Chat access is the same as hive member access
        return isHiveMember(hiveId) || isHiveOwner(hiveId);
    }

    /**
     * Check if the current user is the owner of a resource.
     *
     * @param ownerId The ID of the resource owner
     * @return true if current user is the owner
     */
    public boolean isOwner(String ownerId) {
        if (ownerId == null) {
            log.debug("isOwner called with null ownerId");
            return false;
        }

        return getCurrentUserId()
                .map(currentUserId -> currentUserId.equals(ownerId))
                .orElse(false);
    }

    /**
     * Check if the current user can perform administrative actions.
     *
     * @return true if current user has administrative privileges
     */
    public boolean isAdmin() {
        return hasSystemPermission("system:admin-panel");
    }
}