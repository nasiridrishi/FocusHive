package com.focushive.music.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

/**
 * Fallback implementation for Hive Service client.
 * 
 * Provides resilient fallback responses when the Hive Service is unavailable.
 * Implements circuit breaker pattern for graceful degradation.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class HiveServiceClientFallback implements HiveServiceClient {

    @Override
    public ResponseEntity<HiveResponse> getHive(UUID hiveId, String authorization) {
        log.warn("Hive Service fallback: getHive called for hiveId: {}", hiveId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<UserHivesResponse> getUserHives(UUID userId, String authorization) {
        log.warn("Hive Service fallback: getUserHives called for userId: {}", userId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new UserHivesResponse(
                userId,
                Collections.emptyList(),
                0
            ));
    }

    @Override
    public ResponseEntity<HiveMembersResponse> getActiveHiveMembers(UUID hiveId, String authorization) {
        log.warn("Hive Service fallback: getActiveHiveMembers called for hiveId: {}", hiveId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new HiveMembersResponse(
                hiveId,
                Collections.emptyList(),
                0,
                0
            ));
    }

    @Override
    public ResponseEntity<HiveMembersResponse> getAllHiveMembers(UUID hiveId, String authorization) {
        log.warn("Hive Service fallback: getAllHiveMembers called for hiveId: {}", hiveId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new HiveMembersResponse(
                hiveId,
                Collections.emptyList(),
                0,
                0
            ));
    }

    @Override
    public ResponseEntity<MembershipStatusResponse> getMembershipStatus(
            UUID hiveId, UUID userId, String authorization) {
        log.warn("Hive Service fallback: getMembershipStatus called for hiveId: {}, userId: {}", hiveId, userId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new MembershipStatusResponse(
                hiveId,
                userId,
                false, // Not a member by default in fallback
                "unknown",
                false,
                0,
                Collections.emptyMap()
            ));
    }

    @Override
    public ResponseEntity<HiveSettingsResponse> getHiveSettings(UUID hiveId, String authorization) {
        log.warn("Hive Service fallback: getHiveSettings called for hiveId: {}", hiveId);
        // Return default settings to allow music features to work
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new HiveSettingsResponse(
                hiveId,
                Collections.emptyMap(),
                new MusicSettings(
                    true, // musicEnabled
                    true, // collaborativePlaylistsEnabled
                    true, // backgroundMusicEnabled
                    Collections.emptyList(), // allowedGenres
                    100, // volumeLimit
                    Collections.emptyMap() // streamingSettings
                ),
                Collections.emptyMap(),
                Collections.emptyMap()
            ));
    }

    @Override
    public ResponseEntity<HiveActivityResponse> getHiveActivity(UUID hiveId, String authorization) {
        log.warn("Hive Service fallback: getHiveActivity called for hiveId: {}", hiveId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new HiveActivityResponse(
                hiveId,
                Collections.emptyList(),
                new ActivityStats(
                    0, 0, 0,
                    Collections.emptyMap(),
                    Collections.emptyMap()
                ),
                System.currentTimeMillis()
            ));
    }

    @Override
    public ResponseEntity<BatchHiveResponse> getBatchHives(BatchHiveRequest hiveIds, String authorization) {
        log.warn("Hive Service fallback: getBatchHives called for {} hives", hiveIds.hiveIds().size());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new BatchHiveResponse(
                Collections.emptyList(),
                hiveIds.hiveIds() // All hives marked as not found
            ));
    }
}