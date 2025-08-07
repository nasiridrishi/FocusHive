package com.focushive.music.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Feign client for communicating with the Analytics Service.
 * 
 * Handles analytics data collection, metrics reporting, and insights generation.
 * Includes circuit breaker patterns for resilience and fallback methods.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@FeignClient(
    name = "analytics-service",
    url = "${services.analytics-service.url}",
    configuration = FeignClientConfig.class,
    fallback = AnalyticsServiceClientFallback.class
)
public interface AnalyticsServiceClient {

    /**
     * Records a music-related event for analytics.
     * 
     * @param event The music event to record
     * @param authorization Authorization header with JWT token
     * @return Event recording confirmation
     */
    @PostMapping("/api/v1/events/music")
    ResponseEntity<EventRecordResponse> recordMusicEvent(
        @RequestBody MusicEventRequest event,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Gets user music listening analytics.
     * 
     * @param userId The user ID
     * @param timeRange Time range for analytics (e.g., "7d", "30d", "90d")
     * @param authorization Authorization header with JWT token
     * @return User music analytics
     */
    @GetMapping("/api/v1/users/{userId}/analytics/music")
    ResponseEntity<UserMusicAnalyticsResponse> getUserMusicAnalytics(
        @PathVariable("userId") UUID userId,
        @RequestParam(value = "timeRange", defaultValue = "30d") String timeRange,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Gets hive music analytics.
     * 
     * @param hiveId The hive ID
     * @param timeRange Time range for analytics
     * @param authorization Authorization header with JWT token
     * @return Hive music analytics
     */
    @GetMapping("/api/v1/hives/{hiveId}/analytics/music")
    ResponseEntity<HiveMusicAnalyticsResponse> getHiveMusicAnalytics(
        @PathVariable("hiveId") UUID hiveId,
        @RequestParam(value = "timeRange", defaultValue = "30d") String timeRange,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Gets music recommendation insights based on analytics.
     * 
     * @param userId The user ID
     * @param authorization Authorization header with JWT token
     * @return Recommendation insights
     */
    @GetMapping("/api/v1/users/{userId}/insights/music-recommendations")
    ResponseEntity<MusicRecommendationInsightsResponse> getMusicRecommendationInsights(
        @PathVariable("userId") UUID userId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Gets playlist performance analytics.
     * 
     * @param playlistId The playlist ID
     * @param authorization Authorization header with JWT token
     * @return Playlist analytics
     */
    @GetMapping("/api/v1/playlists/{playlistId}/analytics")
    ResponseEntity<PlaylistAnalyticsResponse> getPlaylistAnalytics(
        @PathVariable("playlistId") UUID playlistId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Records batch music events for better performance.
     * 
     * @param events List of music events
     * @param authorization Authorization header with JWT token
     * @return Batch recording confirmation
     */
    @PostMapping("/api/v1/events/music/batch")
    ResponseEntity<BatchEventRecordResponse> recordBatchMusicEvents(
        @RequestBody BatchMusicEventsRequest events,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Gets trending music based on analytics across all users.
     * 
     * @param timeRange Time range for trends
     * @param limit Maximum number of results
     * @param authorization Authorization header with JWT token
     * @return Trending music data
     */
    @GetMapping("/api/v1/analytics/music/trending")
    ResponseEntity<TrendingMusicResponse> getTrendingMusic(
        @RequestParam(value = "timeRange", defaultValue = "7d") String timeRange,
        @RequestParam(value = "limit", defaultValue = "50") int limit,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Gets music analytics for a specific session.
     * 
     * @param sessionId The session ID
     * @param authorization Authorization header with JWT token
     * @return Session music analytics
     */
    @GetMapping("/api/v1/sessions/{sessionId}/analytics/music")
    ResponseEntity<SessionMusicAnalyticsResponse> getSessionMusicAnalytics(
        @PathVariable("sessionId") UUID sessionId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Request object for recording music events.
     */
    record MusicEventRequest(
        UUID userId,
        UUID sessionId,
        UUID hiveId,
        String eventType, // "play", "pause", "skip", "like", "dislike", "playlist_create", etc.
        String trackId,
        String artistId,
        String playlistId,
        long timestamp,
        Map<String, Object> properties,
        Map<String, Object> context
    ) {}

    /**
     * Request object for batch music events.
     */
    record BatchMusicEventsRequest(
        List<MusicEventRequest> events
    ) {}

    /**
     * Response object for event recording.
     */
    record EventRecordResponse(
        String eventId,
        long timestamp,
        String status,
        String message
    ) {}

    /**
     * Response object for batch event recording.
     */
    record BatchEventRecordResponse(
        int recordedCount,
        int failedCount,
        List<String> errors,
        String status
    ) {}

    /**
     * Response object for user music analytics.
     */
    record UserMusicAnalyticsResponse(
        UUID userId,
        String timeRange,
        ListeningStats listeningStats,
        List<GenreDistribution> topGenres,
        List<ArtistStats> topArtists,
        List<TrackStats> topTracks,
        Map<String, Integer> hourlyActivity,
        Map<String, Object> insights
    ) {}

    /**
     * Listening statistics.
     */
    record ListeningStats(
        long totalPlayTime,
        int totalTracks,
        int uniqueArtists,
        int uniqueGenres,
        double averageSessionDuration,
        int sessionsWithMusic
    ) {}

    /**
     * Genre distribution data.
     */
    record GenreDistribution(
        String genre,
        int playCount,
        long playTime,
        double percentage
    ) {}

    /**
     * Artist statistics.
     */
    record ArtistStats(
        String artistId,
        String artistName,
        int playCount,
        long playTime,
        int uniqueTracks
    ) {}

    /**
     * Track statistics.
     */
    record TrackStats(
        String trackId,
        String trackName,
        String artistName,
        int playCount,
        long playTime,
        double skipRate
    ) {}

    /**
     * Response object for hive music analytics.
     */
    record HiveMusicAnalyticsResponse(
        UUID hiveId,
        String timeRange,
        HiveListeningStats hiveStats,
        List<GenreDistribution> popularGenres,
        List<ArtistStats> popularArtists,
        List<CollaborativePlaylistStats> playlistStats,
        Map<String, Integer> memberActivity,
        Map<String, Object> insights
    ) {}

    /**
     * Hive listening statistics.
     */
    record HiveListeningStats(
        long totalPlayTime,
        int totalTracks,
        int activeMembersWithMusic,
        int collaborativePlaylists,
        double averageMemberEngagement
    ) {}

    /**
     * Collaborative playlist statistics.
     */
    record CollaborativePlaylistStats(
        UUID playlistId,
        String playlistName,
        int contributors,
        int totalTracks,
        long playTime,
        double engagementScore
    ) {}

    /**
     * Response object for music recommendation insights.
     */
    record MusicRecommendationInsightsResponse(
        UUID userId,
        List<String> recommendedGenres,
        List<String> recommendedArtists,
        String preferredMood,
        String optimalListeningTime,
        Map<String, Double> affinityScores,
        List<String> diversificationSuggestions
    ) {}

    /**
     * Response object for playlist analytics.
     */
    record PlaylistAnalyticsResponse(
        UUID playlistId,
        String playlistName,
        UUID creatorId,
        PlaylistStats stats,
        List<TrackPerformance> trackPerformance,
        Map<String, Object> insights
    ) {}

    /**
     * Playlist statistics.
     */
    record PlaylistStats(
        int totalTracks,
        long totalDuration,
        int totalPlays,
        int uniqueListeners,
        double averageRating,
        long lastPlayedAt
    ) {}

    /**
     * Track performance in playlist.
     */
    record TrackPerformance(
        String trackId,
        String trackName,
        int playCount,
        double skipRate,
        int position,
        double engagementScore
    ) {}

    /**
     * Response object for trending music.
     */
    record TrendingMusicResponse(
        String timeRange,
        List<TrendingTrack> trendingTracks,
        List<TrendingArtist> trendingArtists,
        List<TrendingGenre> trendingGenres,
        long lastUpdated
    ) {}

    /**
     * Trending track information.
     */
    record TrendingTrack(
        String trackId,
        String trackName,
        String artistName,
        int playCount,
        double trendScore,
        int rank
    ) {}

    /**
     * Trending artist information.
     */
    record TrendingArtist(
        String artistId,
        String artistName,
        int playCount,
        int uniqueTracks,
        double trendScore,
        int rank
    ) {}

    /**
     * Trending genre information.
     */
    record TrendingGenre(
        String genre,
        int playCount,
        double trendScore,
        int rank
    ) {}

    /**
     * Response object for session music analytics.
     */
    record SessionMusicAnalyticsResponse(
        UUID sessionId,
        long sessionDuration,
        long musicPlayTime,
        double musicToSessionRatio,
        List<TrackStats> tracksPlayed,
        Map<String, Integer> genreBreakdown,
        Map<String, Object> moodAnalysis,
        double focusImpactScore
    ) {}
}