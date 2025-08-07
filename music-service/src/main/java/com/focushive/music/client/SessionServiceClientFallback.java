package com.focushive.music.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

/**
 * Fallback implementation for Session Service client.
 * 
 * Provides resilient fallback responses when the Session Service is unavailable.
 * Implements circuit breaker pattern for graceful degradation.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class SessionServiceClientFallback implements SessionServiceClient {

    @Override
    public ResponseEntity<SessionResponse> getCurrentSession(UUID userId, String authorization) {
        log.warn("Session Service fallback: getCurrentSession called for userId: {}", userId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<SessionResponse> getSession(UUID sessionId, String authorization) {
        log.warn("Session Service fallback: getSession called for sessionId: {}", sessionId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<HiveSessionsResponse> getActiveHiveSessions(UUID hiveId, String authorization) {
        log.warn("Session Service fallback: getActiveHiveSessions called for hiveId: {}", hiveId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new HiveSessionsResponse(
                hiveId,
                Collections.emptyList(),
                0,
                Collections.emptyMap()
            ));
    }

    @Override
    public ResponseEntity<SessionHistoryResponse> getSessionHistory(UUID userId, int limit, String authorization) {
        log.warn("Session Service fallback: getSessionHistory called for userId: {}", userId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new SessionHistoryResponse(
                userId,
                Collections.emptyList(),
                0,
                Collections.emptyMap()
            ));
    }

    @Override
    public ResponseEntity<SessionResponse> createSession(
            UUID userId, CreateSessionRequest request, String authorization) {
        log.warn("Session Service fallback: createSession called for userId: {}", userId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<SessionResponse> updateSession(
            UUID sessionId, UpdateSessionRequest request, String authorization) {
        log.warn("Session Service fallback: updateSession called for sessionId: {}", sessionId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<SessionEndResponse> endSession(UUID sessionId, String authorization) {
        log.warn("Session Service fallback: endSession called for sessionId: {}", sessionId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new SessionEndResponse(
                sessionId,
                System.currentTimeMillis(),
                0,
                Collections.emptyMap(),
                "Session ended with fallback due to service unavailability"
            ));
    }

    @Override
    public ResponseEntity<SessionMetricsResponse> getSessionMetrics(UUID sessionId, String authorization) {
        log.warn("Session Service fallback: getSessionMetrics called for sessionId: {}", sessionId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new SessionMetricsResponse(
                sessionId,
                0, // duration
                0, // focusScore
                0, // productivityScore
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyList()
            ));
    }
}