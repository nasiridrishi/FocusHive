package com.focushive.music.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Feign client for communicating with the Session Service.
 * 
 * Handles session management, focus tracking, and session-related operations.
 * Includes circuit breaker patterns for resilience and fallback methods.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@FeignClient(
    name = "session-service",
    url = "${services.session-service.url}",
    configuration = FeignClientConfig.class,
    fallback = SessionServiceClientFallback.class
)
public interface SessionServiceClient {

    /**
     * Gets current session for a user.
     * 
     * @param userId The user ID
     * @param authorization Authorization header with JWT token
     * @return Current session information
     */
    @GetMapping("/api/v1/users/{userId}/session/current")
    ResponseEntity<SessionResponse> getCurrentSession(
        @PathVariable("userId") UUID userId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Gets session by ID.
     * 
     * @param sessionId The session ID
     * @param authorization Authorization header with JWT token
     * @return Session information
     */
    @GetMapping("/api/v1/sessions/{sessionId}")
    ResponseEntity<SessionResponse> getSession(
        @PathVariable("sessionId") UUID sessionId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Gets active sessions in a hive.
     * 
     * @param hiveId The hive ID
     * @param authorization Authorization header with JWT token
     * @return List of active sessions
     */
    @GetMapping("/api/v1/hives/{hiveId}/sessions/active")
    ResponseEntity<HiveSessionsResponse> getActiveHiveSessions(
        @PathVariable("hiveId") UUID hiveId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Gets user's session history.
     * 
     * @param userId The user ID
     * @param limit Maximum number of sessions to return
     * @param authorization Authorization header with JWT token
     * @return List of past sessions
     */
    @GetMapping("/api/v1/users/{userId}/sessions/history")
    ResponseEntity<SessionHistoryResponse> getSessionHistory(
        @PathVariable("userId") UUID userId,
        @RequestParam(value = "limit", defaultValue = "10") int limit,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Creates a new session with music preferences.
     * 
     * @param userId The user ID
     * @param request Session creation request
     * @param authorization Authorization header with JWT token
     * @return Created session
     */
    @PostMapping("/api/v1/users/{userId}/sessions")
    ResponseEntity<SessionResponse> createSession(
        @PathVariable("userId") UUID userId,
        @RequestBody CreateSessionRequest request,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Updates session status or preferences.
     * 
     * @param sessionId The session ID
     * @param request Session update request
     * @param authorization Authorization header with JWT token
     * @return Updated session
     */
    @PutMapping("/api/v1/sessions/{sessionId}")
    ResponseEntity<SessionResponse> updateSession(
        @PathVariable("sessionId") UUID sessionId,
        @RequestBody UpdateSessionRequest request,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Ends a session.
     * 
     * @param sessionId The session ID
     * @param authorization Authorization header with JWT token
     * @return Session end response
     */
    @PostMapping("/api/v1/sessions/{sessionId}/end")
    ResponseEntity<SessionEndResponse> endSession(
        @PathVariable("sessionId") UUID sessionId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Gets session metrics for music recommendations.
     * 
     * @param sessionId The session ID
     * @param authorization Authorization header with JWT token
     * @return Session metrics
     */
    @GetMapping("/api/v1/sessions/{sessionId}/metrics")
    ResponseEntity<SessionMetricsResponse> getSessionMetrics(
        @PathVariable("sessionId") UUID sessionId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Response object for session information.
     */
    record SessionResponse(
        UUID id,
        UUID userId,
        UUID hiveId,
        String type,
        String status,
        String goal,
        Map<String, Object> preferences,
        MusicPreferences musicPreferences,
        long startTime,
        Long endTime,
        long duration,
        Map<String, Object> metrics,
        Map<String, Object> metadata
    ) {}

    /**
     * Music preferences for session.
     */
    record MusicPreferences(
        boolean musicEnabled,
        String mood,
        List<String> preferredGenres,
        int volumeLevel,
        boolean backgroundMusicOnly,
        Map<String, Object> customSettings
    ) {}

    /**
     * Response object for hive sessions.
     */
    record HiveSessionsResponse(
        UUID hiveId,
        List<SessionSummary> activeSessions,
        int totalCount,
        Map<String, Object> aggregateMetrics
    ) {}

    /**
     * Session summary information.
     */
    record SessionSummary(
        UUID id,
        UUID userId,
        String username,
        String type,
        String status,
        String goal,
        long startTime,
        long duration,
        boolean musicEnabled,
        String currentMood
    ) {}

    /**
     * Response object for session history.
     */
    record SessionHistoryResponse(
        UUID userId,
        List<SessionResponse> sessions,
        int totalCount,
        Map<String, Object> stats
    ) {}

    /**
     * Request object for creating a session.
     */
    record CreateSessionRequest(
        UUID hiveId,
        String type,
        String goal,
        Map<String, Object> preferences,
        MusicPreferences musicPreferences,
        int plannedDuration
    ) {}

    /**
     * Request object for updating a session.
     */
    record UpdateSessionRequest(
        String status,
        String goal,
        Map<String, Object> preferences,
        MusicPreferences musicPreferences,
        Map<String, Object> metrics
    ) {}

    /**
     * Response object for session end.
     */
    record SessionEndResponse(
        UUID sessionId,
        long endTime,
        long totalDuration,
        Map<String, Object> finalMetrics,
        String summary
    ) {}

    /**
     * Response object for session metrics.
     */
    record SessionMetricsResponse(
        UUID sessionId,
        long duration,
        int focusScore,
        int productivityScore,
        Map<String, Integer> activityBreakdown,
        Map<String, Object> musicMetrics,
        List<MetricEntry> timelineMetrics
    ) {}

    /**
     * Metric entry for timeline.
     */
    record MetricEntry(
        long timestamp,
        String metricType,
        Object value,
        Map<String, Object> context
    ) {}
}