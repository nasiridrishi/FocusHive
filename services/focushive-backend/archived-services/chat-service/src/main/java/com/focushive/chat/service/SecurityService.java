package com.focushive.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Security service for centralized authorization logic in the Chat Service.
 * Provides methods for checking user access permissions to hive chats and message operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityService {

    private final RestTemplate restTemplate;
    
    @Value("${backend.api.base-url:http://localhost:8080}")
    private String backendApiUrl;

    /**
     * Get the currently authenticated user.
     *
     * @return Optional containing the current user information, or empty if not authenticated
     */
    public Optional<Map<String, Object>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("No authenticated user found in security context");
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            Map<String, Object> userInfo = Map.of(
                "userId", UUID.fromString(jwt.getClaimAsString("sub")),
                "username", jwt.getClaimAsString("preferred_username"),
                "email", jwt.getClaimAsString("email")
            );
            return Optional.of(userInfo);
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
        return getCurrentUser()
                .map(userInfo -> (UUID) userInfo.get("userId"));
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

        Optional<UUID> currentUserId = getCurrentUserId();
        if (currentUserId.isEmpty()) {
            log.debug("No authenticated user for hasAccessToUser check");
            return false;
        }
        
        // Users can always access their own data
        if (userId.equals(currentUserId.get())) {
            log.debug("User {} accessing own data", userId);
            return true;
        }

        // TODO: Add admin role check when roles are implemented
        log.debug("User {} denied access to user data for {}", currentUserId.get(), userId);
        return false;
    }

    /**
     * Check if the current user can access chat in the specified hive.
     *
     * @param hiveId The hive ID to check chat access for
     * @return true if current user can access chat in the hive
     */
    public boolean hasAccessToChat(UUID hiveId) {
        if (hiveId == null) {
            log.debug("hasAccessToChat called with null hiveId");
            return false;
        }

        Optional<UUID> currentUserId = getCurrentUserId();
        if (currentUserId.isEmpty()) {
            log.debug("No authenticated user for hasAccessToChat check");
            return false;
        }

        return isHiveMember(hiveId) || isHiveOwner(hiveId);
    }

    /**
     * Check if the current user is a member of the specified hive.
     * This calls the backend API to verify hive membership.
     *
     * @param hiveId The hive ID to check membership for
     * @return true if current user is a member of the hive
     */
    public boolean isHiveMember(UUID hiveId) {
        if (hiveId == null) {
            log.debug("isHiveMember called with null hiveId");
            return false;
        }

        Optional<UUID> currentUserId = getCurrentUserId();
        if (currentUserId.isEmpty()) {
            return false;
        }

        try {
            String url = String.format("%s/api/v1/hives/%s/members/check?userId=%s", 
                                     backendApiUrl, hiveId, currentUserId.get());
            
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);
            boolean isMember = Boolean.TRUE.equals(response.getBody());
            
            log.debug("User {} membership check for hive {}: {}", 
                     currentUserId.get(), hiveId, isMember);
            
            return isMember;
        } catch (Exception e) {
            log.warn("Failed to check hive membership for user {} and hive {}: {}", 
                    currentUserId.get(), hiveId, e.getMessage());
            return false;
        }
    }

    /**
     * Check if the current user is the owner of the specified hive.
     * This calls the backend API to verify hive ownership.
     *
     * @param hiveId The hive ID to check ownership for
     * @return true if current user owns the hive
     */
    public boolean isHiveOwner(UUID hiveId) {
        if (hiveId == null) {
            log.debug("isHiveOwner called with null hiveId");
            return false;
        }

        Optional<UUID> currentUserId = getCurrentUserId();
        if (currentUserId.isEmpty()) {
            return false;
        }

        try {
            String url = String.format("%s/api/v1/hives/%s/owner/check?userId=%s", 
                                     backendApiUrl, hiveId, currentUserId.get());
            
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);
            boolean isOwner = Boolean.TRUE.equals(response.getBody());
            
            log.debug("User {} ownership check for hive {}: {}", 
                     currentUserId.get(), hiveId, isOwner);
            
            return isOwner;
        } catch (Exception e) {
            log.warn("Failed to check hive ownership for user {} and hive {}: {}", 
                    currentUserId.get(), hiveId, e.getMessage());
            return false;
        }
    }

    /**
     * Check if the current user can moderate the specified hive.
     * This includes both owners and moderators.
     *
     * @param hiveId The hive ID to check moderator access for
     * @return true if current user can moderate the hive
     */
    public boolean canModerateHive(UUID hiveId) {
        if (hiveId == null) {
            log.debug("canModerateHive called with null hiveId");
            return false;
        }

        Optional<UUID> currentUserId = getCurrentUserId();
        if (currentUserId.isEmpty()) {
            return false;
        }

        // Check if user is owner (owners can moderate)
        if (isHiveOwner(hiveId)) {
            return true;
        }

        // Check if user is moderator
        try {
            String url = String.format("%s/api/v1/hives/%s/moderators/check?userId=%s", 
                                     backendApiUrl, hiveId, currentUserId.get());
            
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);
            boolean canModerate = Boolean.TRUE.equals(response.getBody());
            
            log.debug("User {} moderation check for hive {}: {}", 
                     currentUserId.get(), hiveId, canModerate);
            
            return canModerate;
        } catch (Exception e) {
            log.warn("Failed to check hive moderation for user {} and hive {}: {}", 
                    currentUserId.get(), hiveId, e.getMessage());
            return false;
        }
    }

    /**
     * Check if the current user owns a specific message.
     *
     * @param messageId The message ID to check ownership for
     * @return true if current user owns the message
     */
    public boolean isMessageOwner(UUID messageId) {
        if (messageId == null) {
            log.debug("isMessageOwner called with null messageId");
            return false;
        }

        Optional<UUID> currentUserId = getCurrentUserId();
        if (currentUserId.isEmpty()) {
            return false;
        }

        // TODO: Implement message ownership check by querying chat message repository
        // For now, this is a placeholder implementation
        log.debug("Message ownership check for user {} and message {}: placeholder implementation", 
                 currentUserId.get(), messageId);
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
     * Note: This is a placeholder until role system is fully implemented.
     *
     * @param role The role to check for
     * @return true if current user has the role
     */
    public boolean hasRole(String role) {
        if (role == null) {
            log.debug("hasRole called with null role");
            return false;
        }

        // TODO: Implement role checking when role system is integrated
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
     * Log an authorization attempt for audit purposes.
     *
     * @param operation The operation being attempted
     * @param resourceId The resource being accessed
     * @param granted Whether access was granted
     */
    public void logAuthorizationAttempt(String operation, String resourceId, boolean granted) {
        Optional<Map<String, Object>> currentUser = getCurrentUser();
        String username = currentUser
                .map(userInfo -> (String) userInfo.get("username"))
                .orElse("anonymous");
        
        if (granted) {
            log.info("Authorization granted - User: {}, Operation: {}, Resource: {}", 
                    username, operation, resourceId);
        } else {
            log.warn("Authorization denied - User: {}, Operation: {}, Resource: {}", 
                    username, operation, resourceId);
        }
    }
}