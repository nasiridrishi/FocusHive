package com.focushive.api.security;

import com.focushive.api.service.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Custom Permission Evaluator for FocusHive application.
 * Enables complex authorization checks in @PreAuthorize expressions.
 *
 * Part of Phase 2, Task 2.4: Authorization Rules implementation.
 *
 * Usage in @PreAuthorize:
 * - hasPermission(#hiveId, 'Hive', 'READ')
 * - hasPermission(#sessionId, 'FocusSession', 'WRITE')
 * - hasPermission(null, 'ADMIN_PANEL', 'ACCESS')
 */
@Component("focusHivePermissionEvaluator")
@Slf4j
@RequiredArgsConstructor
public class FocusHivePermissionEvaluator implements PermissionEvaluator {

    private final SecurityService securityService;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String permissionStr = permission.toString();
        log.debug("Evaluating permission: {} on target: {}", permissionStr, targetDomainObject);

        // Handle different target domain objects
        if (targetDomainObject instanceof String targetId) {
            return evaluatePermissionOnResource(permissionStr, targetId);
        }

        // For direct object evaluation (not commonly used but supported)
        return evaluatePermissionOnObject(targetDomainObject, permissionStr);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String permissionStr = permission.toString();
        String resourceId = targetId != null ? targetId.toString() : null;

        log.debug("Evaluating permission: {} on {}: {}", permissionStr, targetType, resourceId);

        return switch (targetType.toUpperCase()) {
            case "HIVE" -> evaluateHivePermission(resourceId, permissionStr);
            case "USER" -> evaluateUserPermission(resourceId, permissionStr);
            case "FOCUSSESSION", "TIMER" -> evaluateTimerPermission(resourceId, permissionStr);
            case "SYSTEM" -> evaluateSystemPermission(permissionStr);
            case "ADMIN_PANEL" -> securityService.hasSystemPermission("system:admin-panel");
            default -> {
                log.warn("Unknown target type for permission evaluation: {}", targetType);
                yield false;
            }
        };
    }

    /**
     * Evaluate permission on a specific resource ID.
     */
    private boolean evaluatePermissionOnResource(String permission, String resourceId) {
        return switch (permission.toUpperCase()) {
            case "READ" -> securityService.hasAccessToHive(resourceId);
            case "WRITE", "UPDATE" -> securityService.canModerateHive(resourceId);
            case "DELETE" -> securityService.isHiveOwner(resourceId);
            case "MODERATE" -> securityService.canModerateHive(resourceId);
            case "MEMBER" -> securityService.isHiveMember(resourceId);
            case "OWNER" -> securityService.isHiveOwner(resourceId);
            default -> securityService.hasPermissionOnResource(permission.toLowerCase(), resourceId);
        };
    }

    /**
     * Evaluate permission on a domain object directly.
     */
    private boolean evaluatePermissionOnObject(Object targetObject, String permission) {
        // This would be used for complex object-level permissions
        // For now, we delegate to resource-based evaluation
        log.debug("Direct object permission evaluation not implemented for: {}", targetObject.getClass());
        return false;
    }

    /**
     * Evaluate hive-specific permissions.
     */
    private boolean evaluateHivePermission(String hiveId, String permission) {
        if (hiveId == null) {
            return false;
        }

        return switch (permission.toUpperCase()) {
            case "READ", "VIEW" -> securityService.hasAccessToHive(hiveId);
            case "WRITE", "UPDATE", "EDIT" -> securityService.canModerateHive(hiveId);
            case "DELETE", "REMOVE" -> securityService.isHiveOwner(hiveId);
            case "MODERATE", "MANAGE" -> securityService.canModerateHive(hiveId);
            case "JOIN" -> securityService.hasAccessToHive(hiveId);
            case "INVITE_MEMBERS" -> securityService.canModerateHive(hiveId);
            case "REMOVE_MEMBERS" -> securityService.canModerateHive(hiveId);
            case "CHAT" -> securityService.hasAccessToChat(hiveId);
            default -> {
                log.debug("Unknown hive permission: {}", permission);
                yield false;
            }
        };
    }

    /**
     * Evaluate user-specific permissions.
     */
    private boolean evaluateUserPermission(String userId, String permission) {
        if (userId == null) {
            return false;
        }

        return switch (permission.toUpperCase()) {
            case "READ", "VIEW" -> securityService.hasAccessToUser(userId);
            case "WRITE", "UPDATE", "EDIT" -> securityService.hasAccessToUser(userId);
            case "DELETE" -> securityService.hasAccessToUser(userId) &&
                          !userId.equals(securityService.getCurrentUserId().orElse(null)); // Can't delete self
            case "PROFILE" -> securityService.hasAccessToUser(userId);
            default -> {
                log.debug("Unknown user permission: {}", permission);
                yield false;
            }
        };
    }

    /**
     * Evaluate timer/focus session permissions.
     */
    private boolean evaluateTimerPermission(String sessionId, String permission) {
        if (sessionId == null) {
            return false;
        }

        return switch (permission.toUpperCase()) {
            case "READ", "VIEW" -> securityService.hasAccessToTimer(sessionId);
            case "WRITE", "UPDATE", "CONTROL" -> securityService.hasAccessToTimer(sessionId);
            case "DELETE", "STOP" -> securityService.hasAccessToTimer(sessionId);
            default -> {
                log.debug("Unknown timer permission: {}", permission);
                yield false;
            }
        };
    }

    /**
     * Evaluate system-level permissions.
     */
    private boolean evaluateSystemPermission(String permission) {
        return switch (permission.toUpperCase()) {
            case "ADMIN", "ADMIN_PANEL" -> securityService.hasSystemPermission("system:admin-panel");
            case "MANAGE_USERS" -> securityService.hasSystemPermission("system:manage-users");
            case "MANAGE_HIVES" -> securityService.hasSystemPermission("system:manage-hives");
            case "ACCESS_ANY_HIVE" -> securityService.hasSystemPermission("system:access-any-hive");
            default -> securityService.hasSystemPermission("system:" + permission.toLowerCase());
        };
    }
}