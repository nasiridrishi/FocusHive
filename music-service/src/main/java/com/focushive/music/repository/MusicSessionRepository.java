package com.focushive.music.repository;

import com.focushive.music.model.MusicSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing music sessions.
 * 
 * Provides data access methods for tracking active music sessions,
 * collaborative listening, session analytics, and playback state management.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Repository
public interface MusicSessionRepository extends JpaRepository<MusicSession, Long> {

    /**
     * Finds the active session for a user (session that hasn't ended).
     * 
     * @param userId The user ID
     * @return Optional containing the active session if found
     */
    @Query("SELECT ms FROM MusicSession ms WHERE ms.userId = :userId " +
           "AND ms.sessionEndTime IS NULL ORDER BY ms.sessionStartTime DESC")
    Optional<MusicSession> findActiveSessionByUserId(@Param("userId") String userId);

    /**
     * Finds all active sessions in a hive.
     * 
     * @param hiveId The hive ID
     * @return List of active sessions in the hive
     */
    @Query("SELECT ms FROM MusicSession ms WHERE ms.hiveId = :hiveId " +
           "AND ms.sessionEndTime IS NULL ORDER BY ms.sessionStartTime DESC")
    List<MusicSession> findActiveSessionsByHiveId(@Param("hiveId") String hiveId);

    /**
     * Finds all currently playing sessions in a hive.
     * 
     * @param hiveId The hive ID
     * @return List of playing sessions in the hive
     */
    @Query("SELECT ms FROM MusicSession ms WHERE ms.hiveId = :hiveId " +
           "AND ms.sessionEndTime IS NULL AND ms.isPlaying = true " +
           "ORDER BY ms.lastActivityTime DESC")
    List<MusicSession> findPlayingSessionsByHiveId(@Param("hiveId") String hiveId);

    /**
     * Finds sessions by type.
     * 
     * @param sessionType The session type
     * @param pageable Pagination information
     * @return Page of sessions of the specified type
     */
    Page<MusicSession> findBySessionTypeAndSessionEndTimeIsNullOrderBySessionStartTimeDesc(
        MusicSession.SessionType sessionType, Pageable pageable);

    /**
     * Finds completed sessions for a user.
     * 
     * @param userId The user ID
     * @param pageable Pagination information
     * @return Page of completed sessions
     */
    Page<MusicSession> findByUserIdAndSessionEndTimeIsNotNullOrderBySessionStartTimeDesc(
        String userId, Pageable pageable);

    /**
     * Finds sessions in a date range for analytics.
     * 
     * @param startDate Start date for the range
     * @param endDate End date for the range
     * @return List of sessions in the date range
     */
    @Query("SELECT ms FROM MusicSession ms WHERE ms.sessionStartTime >= :startDate " +
           "AND ms.sessionStartTime <= :endDate ORDER BY ms.sessionStartTime DESC")
    List<MusicSession> findSessionsInDateRange(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    /**
     * Finds sessions that have been inactive for a specified duration.
     * 
     * @param thresholdTime Threshold for inactivity
     * @return List of inactive sessions
     */
    @Query("SELECT ms FROM MusicSession ms WHERE ms.sessionEndTime IS NULL " +
           "AND ms.lastActivityTime < :thresholdTime")
    List<MusicSession> findInactiveSessions(@Param("thresholdTime") LocalDateTime thresholdTime);

    /**
     * Counts active sessions for a user.
     * 
     * @param userId The user ID
     * @return Number of active sessions
     */
    @Query("SELECT COUNT(*) FROM MusicSession ms WHERE ms.userId = :userId " +
           "AND ms.sessionEndTime IS NULL")
    long countActiveSessionsByUserId(@Param("userId") String userId);

    /**
     * Counts active sessions in a hive.
     * 
     * @param hiveId The hive ID
     * @return Number of active sessions in the hive
     */
    @Query("SELECT COUNT(*) FROM MusicSession ms WHERE ms.hiveId = :hiveId " +
           "AND ms.sessionEndTime IS NULL")
    long countActiveSessionsByHiveId(@Param("hiveId") String hiveId);

    /**
     * Gets total listening time for a user.
     * 
     * @param userId The user ID
     * @return Total listening time in milliseconds
     */
    @Query("SELECT COALESCE(SUM(ms.totalListeningTimeMs), 0) FROM MusicSession ms " +
           "WHERE ms.userId = :userId")
    long getTotalListeningTimeByUserId(@Param("userId") String userId);

    /**
     * Gets listening time for a user in a specific date range.
     * 
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Total listening time in the date range
     */
    @Query("SELECT COALESCE(SUM(ms.totalListeningTimeMs), 0) FROM MusicSession ms " +
           "WHERE ms.userId = :userId AND ms.sessionStartTime >= :startDate " +
           "AND ms.sessionStartTime <= :endDate")
    long getListeningTimeInDateRange(@Param("userId") String userId,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Finds the most active users by session count.
     * 
     * @param limit Maximum number of results
     * @return List of user IDs ordered by session count
     */
    @Query("SELECT ms.userId, COUNT(*) as sessionCount FROM MusicSession ms " +
           "GROUP BY ms.userId ORDER BY sessionCount DESC LIMIT :limit")
    List<Object[]> findMostActiveUsers(@Param("limit") int limit);

    /**
     * Updates the last activity time for a session.
     * 
     * @param sessionId The session ID
     * @param activityTime The new activity timestamp
     * @return Number of records updated
     */
    @Modifying
    @Query("UPDATE MusicSession ms SET ms.lastActivityTime = :activityTime " +
           "WHERE ms.id = :sessionId")
    int updateLastActivityTime(@Param("sessionId") Long sessionId,
                             @Param("activityTime") LocalDateTime activityTime);

    /**
     * Updates the playing state for a session.
     * 
     * @param sessionId The session ID
     * @param isPlaying Whether the session is playing
     * @return Number of records updated
     */
    @Modifying
    @Query("UPDATE MusicSession ms SET ms.isPlaying = :isPlaying, " +
           "ms.lastActivityTime = CURRENT_TIMESTAMP WHERE ms.id = :sessionId")
    int updatePlayingState(@Param("sessionId") Long sessionId,
                         @Param("isPlaying") Boolean isPlaying);

    /**
     * Updates the current track position for a session.
     * 
     * @param sessionId The session ID
     * @param trackPosition The new track position
     * @return Number of records updated
     */
    @Modifying
    @Query("UPDATE MusicSession ms SET ms.currentTrackPosition = :trackPosition, " +
           "ms.lastActivityTime = CURRENT_TIMESTAMP WHERE ms.id = :sessionId")
    int updateTrackPosition(@Param("sessionId") Long sessionId,
                          @Param("trackPosition") Integer trackPosition);

    /**
     * Ends all active sessions for a user.
     * 
     * @param userId The user ID
     * @return Number of sessions ended
     */
    @Modifying
    @Query("UPDATE MusicSession ms SET ms.sessionEndTime = CURRENT_TIMESTAMP, " +
           "ms.isPlaying = false WHERE ms.userId = :userId AND ms.sessionEndTime IS NULL")
    int endAllActiveSessionsForUser(@Param("userId") String userId);

    /**
     * Gets session statistics for analytics.
     * 
     * @return Array of session statistics
     */
    @Query("SELECT " +
           "COUNT(*) as totalSessions, " +
           "COUNT(CASE WHEN ms.sessionEndTime IS NULL THEN 1 END) as activeSessions, " +
           "COUNT(CASE WHEN ms.sessionType = 'COLLABORATIVE' THEN 1 END) as collaborativeSessions, " +
           "COUNT(CASE WHEN ms.sessionType = 'SYNCHRONIZED' THEN 1 END) as synchronizedSessions, " +
           "AVG(ms.totalListeningTimeMs) as avgSessionDuration " +
           "FROM MusicSession ms")
    Object[] getSessionStatistics();

    /**
     * Finds sessions using a specific playlist.
     * 
     * @param playlistId The playlist ID
     * @return List of sessions using the playlist
     */
    @Query("SELECT ms FROM MusicSession ms WHERE ms.currentPlaylist.id = :playlistId")
    List<MusicSession> findSessionsByPlaylistId(@Param("playlistId") Long playlistId);

    /**
     * Finds sessions in a hive within a date range for hive analytics.
     * 
     * @param hiveId The hive ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of sessions in the hive and date range
     */
    @Query("SELECT ms FROM MusicSession ms WHERE ms.hiveId = :hiveId " +
           "AND ms.sessionStartTime >= :startDate AND ms.sessionStartTime <= :endDate " +
           "ORDER BY ms.sessionStartTime DESC")
    List<MusicSession> findHiveSessionsInDateRange(@Param("hiveId") String hiveId,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);
}