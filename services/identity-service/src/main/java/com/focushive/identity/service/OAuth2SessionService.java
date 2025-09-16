package com.focushive.identity.service;

import com.focushive.identity.dto.OAuth2SessionRequest;
import com.focushive.identity.dto.OAuth2SessionResponse;
import com.focushive.identity.entity.OAuth2Session;
import com.focushive.identity.entity.OAuthClient;
import com.focushive.identity.entity.User;
import com.focushive.identity.exception.OAuth2AuthorizationException;
import com.focushive.identity.metrics.OAuth2MetricsService;
import com.focushive.identity.repository.OAuth2SessionRepository;
import com.focushive.identity.repository.OAuthClientRepository;
import com.focushive.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing OAuth2 sessions.
 * Handles session creation, validation, refresh, and termination.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2SessionService {

    private final OAuth2SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final OAuthClientRepository clientRepository;
    private final OAuth2AuditService auditService;
    private final OAuth2MetricsService metricsService;

    @Value("${oauth2.session.default-duration-hours:8}")
    private long defaultSessionDurationHours;

    @Value("${oauth2.session.max-duration-hours:24}")
    private long maxSessionDurationHours;

    @Value("${oauth2.session.idle-timeout-minutes:30}")
    private long idleTimeoutMinutes;

    @Value("${oauth2.session.max-concurrent-sessions:5}")
    private int maxConcurrentSessions;

    @Value("${oauth2.session.require-device-fingerprint:false}")
    private boolean requireDeviceFingerprint;

    @Value("${oauth2.session.cleanup-expired-days:7}")
    private long cleanupExpiredDays;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Create a new OAuth2 session.
     */
    @Transactional
    public OAuth2SessionResponse createSession(OAuth2SessionRequest request) {
        log.info("Creating OAuth2 session for user {} and client {}", request.getUserId(), request.getClientId());

        // Validate user and client
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new OAuth2AuthorizationException("User not found"));

        OAuthClient client = clientRepository.findByClientIdAndEnabledTrue(request.getClientId())
            .orElseThrow(() -> new OAuth2AuthorizationException("Invalid client"));

        // Check concurrent session limit
        long activeSessions = sessionRepository.countActiveSessionsByUser(user.getId(), Instant.now());
        if (activeSessions >= maxConcurrentSessions) {
            log.warn("User {} has reached max concurrent sessions limit", user.getId());
            // Terminate oldest session
            terminateOldestSession(user.getId());
        }

        // Generate session token
        String sessionToken = generateSessionToken();

        // Calculate expiration
        long durationHours = request.getDurationHours() != null ?
            Math.min(request.getDurationHours(), maxSessionDurationHours) :
            defaultSessionDurationHours;
        Instant expiresAt = Instant.now().plus(durationHours, ChronoUnit.HOURS);

        // Create session
        OAuth2Session session = OAuth2Session.builder()
            .user(user)
            .client(client)
            .sessionToken(sessionToken)
            .authMethod(request.getAuthMethod())
            .authLevel(request.getAuthLevel())
            .ipAddress(request.getIpAddress())
            .userAgent(request.getUserAgent())
            .deviceFingerprint(request.getDeviceFingerprint())
            .expiresAt(expiresAt)
            .grantedScopes(request.getGrantedScopes())
            .idToken(request.getIdToken())
            .metadata(request.getMetadata())
            .active(true)
            .build();

        session = sessionRepository.save(session);

        // Audit log
        auditService.logSessionCreated(user.getId(), client.getClientId(),
            session.getId().toString(), request.getIpAddress());

        // Metrics
        metricsService.recordSessionCreated(client.getClientId());

        log.info("Created OAuth2 session {} for user {}", session.getId(), user.getUsername());

        return mapToResponse(session);
    }

    /**
     * Validate a session token.
     */
    @Transactional(readOnly = true)
    public OAuth2SessionResponse validateSession(String sessionToken) {
        log.debug("Validating session token");

        OAuth2Session session = sessionRepository.findActiveSessionByToken(sessionToken, Instant.now())
            .orElseThrow(() -> new OAuth2AuthorizationException("Invalid or expired session"));

        // Check idle timeout
        if (isSessionIdle(session)) {
            log.info("Session {} is idle, terminating", session.getId());
            terminateSession(session.getId(), "idle_timeout");
            throw new OAuth2AuthorizationException("Session expired due to inactivity");
        }

        // Update last accessed time
        sessionRepository.updateLastAccessedTime(session.getId(), Instant.now());

        return mapToResponse(session);
    }

    /**
     * Refresh a session.
     */
    @Transactional
    public OAuth2SessionResponse refreshSession(String sessionToken, long additionalHours) {
        log.info("Refreshing session");

        OAuth2Session session = sessionRepository.findBySessionToken(sessionToken)
            .orElseThrow(() -> new OAuth2AuthorizationException("Session not found"));

        if (!session.isValid()) {
            throw new OAuth2AuthorizationException("Session is not valid");
        }

        // Calculate new expiration
        long maxExtension = Math.min(additionalHours, maxSessionDurationHours);
        Instant newExpiresAt = session.getExpiresAt().plus(maxExtension, ChronoUnit.HOURS);

        // Ensure we don't exceed max duration from creation
        Instant maxExpiry = session.getCreatedAt().plus(maxSessionDurationHours * 2, ChronoUnit.HOURS);
        if (newExpiresAt.isAfter(maxExpiry)) {
            newExpiresAt = maxExpiry;
        }

        session.setExpiresAt(newExpiresAt);
        session.updateLastAccessed();
        session = sessionRepository.save(session);

        // Audit log
        auditService.logSessionRefreshed(session.getUser().getId(), session.getClient().getClientId(),
            session.getId().toString(), auditService.getClientIpAddress());

        log.info("Refreshed session {} until {}", session.getId(), newExpiresAt);

        return mapToResponse(session);
    }

    /**
     * Terminate a session.
     */
    @Transactional
    public void terminateSession(UUID sessionId, String reason) {
        log.info("Terminating session {} for reason: {}", sessionId, reason);

        OAuth2Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new OAuth2AuthorizationException("Session not found"));

        session.terminate(reason);
        sessionRepository.save(session);

        // Audit log
        auditService.logSessionTerminated(session.getUser().getId(), session.getClient().getClientId(),
            sessionId.toString(), reason, auditService.getClientIpAddress());

        // Metrics
        metricsService.recordSessionTerminated(session.getClient().getClientId());
    }

    /**
     * Terminate all sessions for a user.
     */
    @Transactional
    public int terminateAllUserSessions(UUID userId, String reason) {
        log.info("Terminating all sessions for user {}", userId);

        int terminated = sessionRepository.terminateAllSessionsForUser(userId, Instant.now(), reason);

        if (terminated > 0) {
            // Audit log
            auditService.logAllSessionsTerminated(userId, reason, auditService.getClientIpAddress());

            log.info("Terminated {} sessions for user {}", terminated, userId);
        }

        return terminated;
    }

    /**
     * Get all active sessions for a user.
     */
    @Transactional(readOnly = true)
    public List<OAuth2SessionResponse> getUserSessions(UUID userId) {
        List<OAuth2Session> sessions = sessionRepository.findActiveSessionsByUser(userId, Instant.now());
        return sessions.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Check if user has valid session.
     */
    @Transactional(readOnly = true)
    public boolean hasValidSession(UUID userId, String clientId) {
        OAuthClient client = clientRepository.findByClientIdAndEnabledTrue(clientId)
            .orElse(null);

        if (client == null) {
            return false;
        }

        List<OAuth2Session> sessions = sessionRepository.findActiveSessionsByUserAndClient(
            userId, client.getId(), Instant.now()
        );

        return sessions.stream().anyMatch(OAuth2Session::isValid);
    }

    /**
     * Elevate session authentication level (e.g., after MFA).
     */
    @Transactional
    public OAuth2SessionResponse elevateSession(String sessionToken, String newAuthLevel) {
        log.info("Elevating session authentication level to {}", newAuthLevel);

        OAuth2Session session = sessionRepository.findBySessionToken(sessionToken)
            .orElseThrow(() -> new OAuth2AuthorizationException("Session not found"));

        if (!session.isValid()) {
            throw new OAuth2AuthorizationException("Session is not valid");
        }

        session.setAuthLevel(newAuthLevel);
        session.updateLastAccessed();
        session = sessionRepository.save(session);

        // Audit log
        auditService.logSessionElevated(session.getUser().getId(), session.getClient().getClientId(),
            session.getId().toString(), newAuthLevel, auditService.getClientIpAddress());

        return mapToResponse(session);
    }

    /**
     * Clean up expired sessions (scheduled task).
     */
    @Scheduled(cron = "${oauth2.session.cleanup-cron:0 0 2 * * ?}") // Daily at 2 AM
    @Transactional
    public void cleanupExpiredSessions() {
        log.info("Starting expired session cleanup");

        Instant threshold = Instant.now().minus(cleanupExpiredDays, ChronoUnit.DAYS);
        int deleted = sessionRepository.deleteExpiredSessions(threshold);

        if (deleted > 0) {
            log.info("Deleted {} expired sessions", deleted);
        }
    }

    /**
     * Check for suspicious session activity.
     */
    @Transactional(readOnly = true)
    public boolean detectSuspiciousActivity(String sessionToken, String ipAddress, String deviceFingerprint) {
        OAuth2Session session = sessionRepository.findBySessionToken(sessionToken)
            .orElse(null);

        if (session == null) {
            return false;
        }

        // Check for IP address change
        if (session.getIpAddress() != null && !session.getIpAddress().equals(ipAddress)) {
            log.warn("Session {} IP address changed from {} to {}",
                session.getId(), session.getIpAddress(), ipAddress);
            return true;
        }

        // Check for device fingerprint change
        if (requireDeviceFingerprint && session.getDeviceFingerprint() != null &&
            !session.getDeviceFingerprint().equals(deviceFingerprint)) {
            log.warn("Session {} device fingerprint changed", session.getId());
            return true;
        }

        return false;
    }

    /**
     * Get session statistics for monitoring.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSessionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSessions", sessionRepository.count());
        stats.put("activeSessions", sessionRepository.findAll().stream()
            .filter(OAuth2Session::isValid)
            .count());
        stats.put("expiringSessions", sessionRepository.findExpiringSessions(
            Instant.now(),
            Instant.now().plus(1, ChronoUnit.HOURS)
        ).size());
        return stats;
    }

    /**
     * Check if session is idle.
     */
    private boolean isSessionIdle(OAuth2Session session) {
        Instant idleThreshold = Instant.now().minus(idleTimeoutMinutes, ChronoUnit.MINUTES);
        return session.getLastAccessedAt().isBefore(idleThreshold);
    }

    /**
     * Terminate oldest session for user.
     */
    private void terminateOldestSession(UUID userId) {
        List<OAuth2Session> sessions = sessionRepository.findActiveSessionsByUser(userId, Instant.now());
        sessions.stream()
            .min(Comparator.comparing(OAuth2Session::getCreatedAt))
            .ifPresent(session -> terminateSession(session.getId(), "max_concurrent_sessions"));
    }

    /**
     * Generate a secure session token.
     */
    private String generateSessionToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Map session entity to response DTO.
     */
    private OAuth2SessionResponse mapToResponse(OAuth2Session session) {
        return OAuth2SessionResponse.builder()
            .sessionId(session.getId())
            .sessionToken(session.getSessionToken())
            .userId(session.getUser().getId())
            .clientId(session.getClient().getClientId())
            .authMethod(session.getAuthMethod())
            .authLevel(session.getAuthLevel())
            .createdAt(session.getCreatedAt())
            .expiresAt(session.getExpiresAt())
            .lastAccessedAt(session.getLastAccessedAt())
            .active(session.isActive())
            .grantedScopes(session.getGrantedScopes())
            .metadata(session.getMetadata())
            .build();
    }
}