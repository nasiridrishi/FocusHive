package com.focushive.music.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

/**
 * Fallback implementation for User Service client.
 * 
 * Provides resilient fallback responses when the User Service is unavailable.
 * Implements circuit breaker pattern for graceful degradation.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public ResponseEntity<UserProfileResponse> getUserProfile(UUID userId, String authorization) {
        log.warn("User Service fallback: getUserProfile called for userId: {}", userId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<UserPreferencesResponse> getUserPreferences(UUID userId, String authorization) {
        log.warn("User Service fallback: getUserPreferences called for userId: {}", userId);
        // Return default preferences to allow music service to continue functioning
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new UserPreferencesResponse(
                userId,
                Collections.emptyMap(),
                new MusicPreferencesData(
                    Collections.emptyList(), // favoriteGenres
                    Collections.emptyList(), // favoriteArtists
                    "spotify", // preferredStreamingService
                    false, // explicitContentAllowed
                    70, // volumeLevel
                    Collections.emptyMap() // algorithmPreferences
                ),
                Collections.emptyMap()
            ));
    }

    @Override
    public ResponseEntity<MusicPreferencesResponse> updateMusicPreferences(
            UUID userId, UpdateMusicPreferencesRequest preferences, String authorization) {
        log.warn("User Service fallback: updateMusicPreferences called for userId: {}", userId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<UserFriendsResponse> getUserFriends(UUID userId, String authorization) {
        log.warn("User Service fallback: getUserFriends called for userId: {}", userId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new UserFriendsResponse(
                userId,
                Collections.emptyList(),
                0
            ));
    }

    @Override
    public ResponseEntity<FriendshipStatusResponse> getFriendshipStatus(
            UUID userId, UUID friendId, String authorization) {
        log.warn("User Service fallback: getFriendshipStatus called for userId: {}, friendId: {}", userId, friendId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new FriendshipStatusResponse(
                userId,
                friendId,
                false, // Not friends by default in fallback
                "unknown",
                0
            ));
    }

    @Override
    public ResponseEntity<BatchUserProfileResponse> getBatchUserProfiles(
            BatchUserRequest userIds, String authorization) {
        log.warn("User Service fallback: getBatchUserProfiles called for {} users", 
            userIds.userIds().size());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new BatchUserProfileResponse(
                Collections.emptyList(),
                userIds.userIds() // All users marked as not found
            ));
    }
}