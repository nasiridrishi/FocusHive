package com.focushive.music.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Feign client for communicating with the Hive Service.
 * 
 * Handles hive management, membership, and collaborative features.
 * Includes circuit breaker patterns for resilience and fallback methods.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@FeignClient(
    name = "hive-service",
    url = "${services.hive-service.url}",
    configuration = FeignClientConfig.class,
    fallback = HiveServiceClientFallback.class
)
public interface HiveServiceClient {

    /**
     * Gets hive information by ID.
     * 
     * @param hiveId The hive ID
     * @param authorization Authorization header with JWT token
     * @return Hive information
     */
    @GetMapping("/api/v1/hives/{hiveId}")
    ResponseEntity<HiveResponse> getHive(
        @PathVariable("hiveId") UUID hiveId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Gets all hives for a user.
     * 
     * @param userId The user ID
     * @param authorization Authorization header with JWT token
     * @return List of user's hives
     */
    @GetMapping("/api/v1/users/{userId}/hives")
    ResponseEntity<UserHivesResponse> getUserHives(
        @PathVariable("userId") UUID userId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Gets active members of a hive.
     * 
     * @param hiveId The hive ID
     * @param authorization Authorization header with JWT token
     * @return List of active hive members
     */
    @GetMapping("/api/v1/hives/{hiveId}/members/active")
    ResponseEntity<HiveMembersResponse> getActiveHiveMembers(
        @PathVariable("hiveId") UUID hiveId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Gets all members of a hive.
     * 
     * @param hiveId The hive ID
     * @param authorization Authorization header with JWT token
     * @return List of all hive members
     */
    @GetMapping("/api/v1/hives/{hiveId}/members")
    ResponseEntity<HiveMembersResponse> getAllHiveMembers(
        @PathVariable("hiveId") UUID hiveId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Checks if user is member of a hive.
     * 
     * @param hiveId The hive ID
     * @param userId The user ID
     * @param authorization Authorization header with JWT token
     * @return Membership status
     */
    @GetMapping("/api/v1/hives/{hiveId}/members/{userId}")
    ResponseEntity<MembershipStatusResponse> getMembershipStatus(
        @PathVariable("hiveId") UUID hiveId,
        @PathVariable("userId") UUID userId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Gets hive settings including music-related settings.
     * 
     * @param hiveId The hive ID
     * @param authorization Authorization header with JWT token
     * @return Hive settings
     */
    @GetMapping("/api/v1/hives/{hiveId}/settings")
    ResponseEntity<HiveSettingsResponse> getHiveSettings(
        @PathVariable("hiveId") UUID hiveId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Gets hive activity for music recommendations.
     * 
     * @param hiveId The hive ID
     * @param authorization Authorization header with JWT token
     * @return Hive activity data
     */
    @GetMapping("/api/v1/hives/{hiveId}/activity")
    ResponseEntity<HiveActivityResponse> getHiveActivity(
        @PathVariable("hiveId") UUID hiveId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Gets batch hive information.
     * 
     * @param hiveIds List of hive IDs
     * @param authorization Authorization header with JWT token
     * @return Batch hive information
     */
    @PostMapping("/api/v1/hives/batch")
    ResponseEntity<BatchHiveResponse> getBatchHives(
        @RequestBody BatchHiveRequest hiveIds,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Verifies if a user is a member of a hive.
     * 
     * @param hiveId The hive ID
     * @param userId The user ID
     * @return true if user is member of hive
     */
    default boolean verifyHiveMembership(String hiveId, String userId) {
        try {
            ResponseEntity<MembershipStatusResponse> response = getMembershipStatus(
                UUID.fromString(hiveId), UUID.fromString(userId), null);
            return response.getBody() != null && response.getBody().isMember();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets hive members as string list for playlist sharing.
     * 
     * @param hiveId The hive ID
     * @return List of member user IDs as strings
     */
    default List<String> getHiveMembers(String hiveId) {
        try {
            ResponseEntity<HiveMembersResponse> response = getAllHiveMembers(
                UUID.fromString(hiveId), null);
            if (response.getBody() != null) {
                return response.getBody().members().stream()
                    .map(member -> member.userId().toString())
                    .toList();
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Response object for hive information.
     */
    record HiveResponse(
        UUID id,
        String name,
        String description,
        String type,
        UUID ownerId,
        int memberCount,
        int activeMembers,
        Map<String, Object> settings,
        Map<String, Object> metadata,
        boolean isActive,
        long createdAt,
        long updatedAt
    ) {}

    /**
     * Response object for user's hives.
     */
    record UserHivesResponse(
        UUID userId,
        List<HiveInfo> hives,
        int totalCount
    ) {}

    /**
     * Hive information summary.
     */
    record HiveInfo(
        UUID id,
        String name,
        String type,
        UUID ownerId,
        int memberCount,
        int activeMembers,
        boolean isOwner,
        String memberRole,
        boolean isActive
    ) {}

    /**
     * Response object for hive members.
     */
    record HiveMembersResponse(
        UUID hiveId,
        List<HiveMember> members,
        int totalCount,
        int activeCount
    ) {}

    /**
     * Hive member information.
     */
    record HiveMember(
        UUID userId,
        String username,
        String displayName,
        String avatar,
        String role,
        boolean isActive,
        boolean isOnline,
        String status,
        long joinedAt,
        long lastActiveAt
    ) {}

    /**
     * Response object for membership status.
     */
    record MembershipStatusResponse(
        UUID hiveId,
        UUID userId,
        boolean isMember,
        String role,
        boolean isActive,
        long joinedAt,
        Map<String, Object> permissions
    ) {}

    /**
     * Response object for hive settings.
     */
    record HiveSettingsResponse(
        UUID hiveId,
        Map<String, Object> generalSettings,
        MusicSettings musicSettings,
        Map<String, Object> privacySettings,
        Map<String, Object> collaborationSettings
    ) {}

    /**
     * Music-related settings for hive.
     */
    record MusicSettings(
        boolean musicEnabled,
        boolean collaborativePlaylistsEnabled,
        boolean backgroundMusicEnabled,
        List<String> allowedGenres,
        int volumeLimit,
        Map<String, Object> streamingSettings
    ) {}

    /**
     * Response object for hive activity.
     */
    record HiveActivityResponse(
        UUID hiveId,
        List<ActivityEntry> recentActivity,
        ActivityStats stats,
        long lastUpdated
    ) {}

    /**
     * Activity entry information.
     */
    record ActivityEntry(
        UUID userId,
        String username,
        String activity,
        String description,
        long timestamp,
        Map<String, Object> metadata
    ) {}

    /**
     * Activity statistics.
     */
    record ActivityStats(
        int totalSessions,
        long totalDuration,
        int uniqueUsers,
        Map<String, Integer> activityTypes,
        Map<String, Object> musicStats
    ) {}

    /**
     * Request object for batch hive operations.
     */
    record BatchHiveRequest(List<UUID> hiveIds) {}

    /**
     * Response object for batch hive information.
     */
    record BatchHiveResponse(
        List<HiveResponse> hives,
        List<UUID> notFound
    ) {}
}