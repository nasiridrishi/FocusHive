package com.focushive.music.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Feign client for communicating with the User Service.
 * 
 * Handles user profile management, preferences, and basic user operations.
 * Includes circuit breaker patterns for resilience and fallback methods.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@FeignClient(
    name = "user-service",
    url = "${services.user-service.url}",
    configuration = FeignClientConfig.class,
    fallback = UserServiceClientFallback.class
)
public interface UserServiceClient {

    /**
     * Gets user profile information.
     * 
     * @param userId The user ID
     * @param authorization Authorization header with JWT token
     * @return User profile information
     */
    @GetMapping("/api/v1/users/{userId}")
    ResponseEntity<UserProfileResponse> getUserProfile(
        @PathVariable("userId") UUID userId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Gets user preferences including music preferences.
     * 
     * @param userId The user ID
     * @param authorization Authorization header with JWT token
     * @return User preferences
     */
    @GetMapping("/api/v1/users/{userId}/preferences")
    ResponseEntity<UserPreferencesResponse> getUserPreferences(
        @PathVariable("userId") UUID userId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Updates user music preferences.
     * 
     * @param userId The user ID
     * @param preferences The music preferences to update
     * @param authorization Authorization header with JWT token
     * @return Updated preferences
     */
    @PutMapping("/api/v1/users/{userId}/preferences/music")
    ResponseEntity<MusicPreferencesResponse> updateMusicPreferences(
        @PathVariable("userId") UUID userId,
        @RequestBody UpdateMusicPreferencesRequest preferences,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Gets user's friends for collaborative features.
     * 
     * @param userId The user ID
     * @param authorization Authorization header with JWT token
     * @return List of user friends
     */
    @GetMapping("/api/v1/users/{userId}/friends")
    ResponseEntity<UserFriendsResponse> getUserFriends(
        @PathVariable("userId") UUID userId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Checks if users are friends (for collaborative playlist features).
     * 
     * @param userId The primary user ID
     * @param friendId The friend user ID
     * @param authorization Authorization header with JWT token
     * @return Friendship status
     */
    @GetMapping("/api/v1/users/{userId}/friends/{friendId}")
    ResponseEntity<FriendshipStatusResponse> getFriendshipStatus(
        @PathVariable("userId") UUID userId,
        @PathVariable("friendId") UUID friendId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Gets multiple user profiles (batch operation).
     * 
     * @param userIds List of user IDs
     * @param authorization Authorization header with JWT token
     * @return List of user profiles
     */
    @PostMapping("/api/v1/users/batch")
    ResponseEntity<BatchUserProfileResponse> getBatchUserProfiles(
        @RequestBody BatchUserRequest userIds,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Verifies if a user exists.
     * 
     * @param userId The user ID to verify
     * @return true if user exists
     */
    @GetMapping("/api/v1/users/{userId}/exists")
    ResponseEntity<Boolean> userExists(@PathVariable("userId") String userId);

    /**
     * Response object for user profile.
     */
    record UserProfileResponse(
        UUID id,
        String username,
        String displayName,
        String email,
        String avatar,
        Map<String, Object> metadata,
        boolean isActive
    ) {}

    /**
     * Response object for user preferences.
     */
    record UserPreferencesResponse(
        UUID userId,
        Map<String, Object> generalPreferences,
        MusicPreferencesData musicPreferences,
        Map<String, Object> privacySettings
    ) {}

    /**
     * Music preferences data.
     */
    record MusicPreferencesData(
        List<String> favoriteGenres,
        List<String> favoriteArtists,
        String preferredStreamingService,
        boolean explicitContentAllowed,
        int volumeLevel,
        Map<String, Object> algorithmPreferences
    ) {}

    /**
     * Response object for music preferences update.
     */
    record MusicPreferencesResponse(
        UUID userId,
        MusicPreferencesData musicPreferences,
        long updatedAt
    ) {}

    /**
     * Request object for updating music preferences.
     */
    record UpdateMusicPreferencesRequest(
        List<String> favoriteGenres,
        List<String> favoriteArtists,
        String preferredStreamingService,
        boolean explicitContentAllowed,
        int volumeLevel,
        Map<String, Object> algorithmPreferences
    ) {}

    /**
     * Response object for user friends.
     */
    record UserFriendsResponse(
        UUID userId,
        List<FriendInfo> friends,
        int totalCount
    ) {}

    /**
     * Friend information.
     */
    record FriendInfo(
        UUID id,
        String username,
        String displayName,
        String avatar,
        boolean isOnline,
        String status
    ) {}

    /**
     * Response object for friendship status.
     */
    record FriendshipStatusResponse(
        UUID userId,
        UUID friendId,
        boolean areFriends,
        String relationshipStatus,
        long establishedAt
    ) {}

    /**
     * Request object for batch user operations.
     */
    record BatchUserRequest(List<UUID> userIds) {}

    /**
     * Response object for batch user profiles.
     */
    record BatchUserProfileResponse(
        List<UserProfileResponse> users,
        List<UUID> notFound
    ) {}
}