package com.focushive.identity.repository;

import com.focushive.identity.entity.OAuth2Session;
import com.focushive.identity.entity.OAuthClient;
import com.focushive.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for OAuth2 session management.
 */
@Repository
public interface OAuth2SessionRepository extends JpaRepository<OAuth2Session, UUID> {

    /**
     * Find session by session token.
     */
    Optional<OAuth2Session> findBySessionToken(String sessionToken);

    /**
     * Find active session by session token.
     */
    @Query("SELECT s FROM OAuth2Session s WHERE s.sessionToken = :token AND s.active = true AND s.expiresAt > :now")
    Optional<OAuth2Session> findActiveSessionByToken(@Param("token") String token, @Param("now") Instant now);

    /**
     * Find all active sessions for a user.
     */
    @Query("SELECT s FROM OAuth2Session s WHERE s.user.id = :userId AND s.active = true AND s.expiresAt > :now")
    List<OAuth2Session> findActiveSessionsByUser(@Param("userId") UUID userId, @Param("now") Instant now);

    /**
     * Find all active sessions for a user and client.
     */
    @Query("SELECT s FROM OAuth2Session s WHERE s.user.id = :userId AND s.client.id = :clientId " +
           "AND s.active = true AND s.expiresAt > :now")
    List<OAuth2Session> findActiveSessionsByUserAndClient(@Param("userId") UUID userId,
                                                          @Param("clientId") UUID clientId,
                                                          @Param("now") Instant now);

    /**
     * Find sessions by user.
     */
    List<OAuth2Session> findByUser(User user);

    /**
     * Find sessions by client.
     */
    List<OAuth2Session> findByClient(OAuthClient client);

    /**
     * Count active sessions for a user.
     */
    @Query("SELECT COUNT(s) FROM OAuth2Session s WHERE s.user.id = :userId AND s.active = true AND s.expiresAt > :now")
    long countActiveSessionsByUser(@Param("userId") UUID userId, @Param("now") Instant now);

    /**
     * Terminate all sessions for a user.
     */
    @Modifying
    @Query("UPDATE OAuth2Session s SET s.active = false, s.terminatedAt = :now, " +
           "s.terminationReason = :reason WHERE s.user.id = :userId AND s.active = true")
    int terminateAllSessionsForUser(@Param("userId") UUID userId,
                                    @Param("now") Instant now,
                                    @Param("reason") String reason);

    /**
     * Terminate all sessions for a client.
     */
    @Modifying
    @Query("UPDATE OAuth2Session s SET s.active = false, s.terminatedAt = :now, " +
           "s.terminationReason = :reason WHERE s.client.id = :clientId AND s.active = true")
    int terminateAllSessionsForClient(@Param("clientId") UUID clientId,
                                      @Param("now") Instant now,
                                      @Param("reason") String reason);

    /**
     * Terminate session by token.
     */
    @Modifying
    @Query("UPDATE OAuth2Session s SET s.active = false, s.terminatedAt = :now, " +
           "s.terminationReason = :reason WHERE s.sessionToken = :token AND s.active = true")
    int terminateSessionByToken(@Param("token") String token,
                               @Param("now") Instant now,
                               @Param("reason") String reason);

    /**
     * Delete expired sessions.
     */
    @Modifying
    @Query("DELETE FROM OAuth2Session s WHERE s.expiresAt <= :threshold")
    int deleteExpiredSessions(@Param("threshold") Instant threshold);

    /**
     * Find sessions that will expire soon.
     */
    @Query("SELECT s FROM OAuth2Session s WHERE s.active = true AND s.expiresAt BETWEEN :now AND :threshold")
    List<OAuth2Session> findExpiringSessions(@Param("now") Instant now, @Param("threshold") Instant threshold);

    /**
     * Find idle sessions that haven't been accessed recently.
     */
    @Query("SELECT s FROM OAuth2Session s WHERE s.active = true AND s.lastAccessedAt < :idleThreshold")
    List<OAuth2Session> findIdleSessions(@Param("idleThreshold") Instant idleThreshold);

    /**
     * Update last accessed time for a session.
     */
    @Modifying
    @Query("UPDATE OAuth2Session s SET s.lastAccessedAt = :now WHERE s.id = :sessionId")
    int updateLastAccessedTime(@Param("sessionId") UUID sessionId, @Param("now") Instant now);

    /**
     * Find sessions by IP address.
     */
    @Query("SELECT s FROM OAuth2Session s WHERE s.ipAddress = :ipAddress AND s.active = true")
    List<OAuth2Session> findActiveSessionsByIpAddress(@Param("ipAddress") String ipAddress);

    /**
     * Find sessions by device fingerprint.
     */
    @Query("SELECT s FROM OAuth2Session s WHERE s.deviceFingerprint = :fingerprint AND s.active = true")
    List<OAuth2Session> findActiveSessionsByDeviceFingerprint(@Param("fingerprint") String fingerprint);

    /**
     * Check if user has active session with specific auth level.
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM OAuth2Session s " +
           "WHERE s.user.id = :userId AND s.authLevel = :authLevel AND s.active = true AND s.expiresAt > :now")
    boolean hasActiveSessionWithAuthLevel(@Param("userId") UUID userId,
                                         @Param("authLevel") String authLevel,
                                         @Param("now") Instant now);

    /**
     * Find sessions needing refresh (about to expire).
     */
    @Query("SELECT s FROM OAuth2Session s WHERE s.active = true AND s.user.id = :userId " +
           "AND s.expiresAt BETWEEN :now AND :refreshThreshold")
    List<OAuth2Session> findSessionsNeedingRefresh(@Param("userId") UUID userId,
                                                   @Param("now") Instant now,
                                                   @Param("refreshThreshold") Instant refreshThreshold);
}