package com.focushive.music.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

/**
 * Fallback implementation for Analytics Service client.
 * 
 * Provides resilient fallback responses when the Analytics Service is unavailable.
 * Implements circuit breaker pattern for graceful degradation.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class AnalyticsServiceClientFallback implements AnalyticsServiceClient {

    @Override
    public ResponseEntity<EventRecordResponse> recordMusicEvent(MusicEventRequest event, String authorization) {
        log.warn("Analytics Service fallback: recordMusicEvent called for eventType: {}", event.eventType());
        // Return success but log that analytics is unavailable
        return ResponseEntity.ok(new EventRecordResponse(
            UUID.randomUUID().toString(),
            System.currentTimeMillis(),
            "fallback",
            "Event logged locally due to service unavailability"
        ));
    }

    @Override
    public ResponseEntity<UserMusicAnalyticsResponse> getUserMusicAnalytics(
            UUID userId, String timeRange, String authorization) {
        log.warn("Analytics Service fallback: getUserMusicAnalytics called for userId: {}", userId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new UserMusicAnalyticsResponse(
                userId,
                timeRange,
                new ListeningStats(0, 0, 0, 0, 0.0, 0),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap(),
                Collections.emptyMap()
            ));
    }

    @Override
    public ResponseEntity<HiveMusicAnalyticsResponse> getHiveMusicAnalytics(
            UUID hiveId, String timeRange, String authorization) {
        log.warn("Analytics Service fallback: getHiveMusicAnalytics called for hiveId: {}", hiveId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new HiveMusicAnalyticsResponse(
                hiveId,
                timeRange,
                new HiveListeningStats(0, 0, 0, 0, 0.0),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap(),
                Collections.emptyMap()
            ));
    }

    @Override
    public ResponseEntity<MusicRecommendationInsightsResponse> getMusicRecommendationInsights(
            UUID userId, String authorization) {
        log.warn("Analytics Service fallback: getMusicRecommendationInsights called for userId: {}", userId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new MusicRecommendationInsightsResponse(
                userId,
                Collections.emptyList(),
                Collections.emptyList(),
                "neutral",
                "anytime",
                Collections.emptyMap(),
                Collections.emptyList()
            ));
    }

    @Override
    public ResponseEntity<PlaylistAnalyticsResponse> getPlaylistAnalytics(UUID playlistId, String authorization) {
        log.warn("Analytics Service fallback: getPlaylistAnalytics called for playlistId: {}", playlistId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new PlaylistAnalyticsResponse(
                playlistId,
                "Unknown Playlist",
                null,
                new PlaylistStats(0, 0, 0, 0, 0.0, 0),
                Collections.emptyList(),
                Collections.emptyMap()
            ));
    }

    @Override
    public ResponseEntity<BatchEventRecordResponse> recordBatchMusicEvents(
            BatchMusicEventsRequest events, String authorization) {
        log.warn("Analytics Service fallback: recordBatchMusicEvents called for {} events", 
            events.events().size());
        // Return success but log that analytics is unavailable
        return ResponseEntity.ok(new BatchEventRecordResponse(
            events.events().size(),
            0,
            Collections.emptyList(),
            "fallback"
        ));
    }

    @Override
    public ResponseEntity<TrendingMusicResponse> getTrendingMusic(
            String timeRange, int limit, String authorization) {
        log.warn("Analytics Service fallback: getTrendingMusic called");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new TrendingMusicResponse(
                timeRange,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                System.currentTimeMillis()
            ));
    }

    @Override
    public ResponseEntity<SessionMusicAnalyticsResponse> getSessionMusicAnalytics(
            UUID sessionId, String authorization) {
        log.warn("Analytics Service fallback: getSessionMusicAnalytics called for sessionId: {}", sessionId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new SessionMusicAnalyticsResponse(
                sessionId,
                0,
                0,
                0.0,
                Collections.emptyList(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                0.0
            ));
    }
}