package com.focushive.buddy.repository;

import com.focushive.buddy.entity.BuddySession;
import com.focushive.buddy.entity.BuddySession.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BuddySessionRepository extends JpaRepository<BuddySession, String> {
    
    List<BuddySession> findByRelationshipIdOrderBySessionDateDesc(String relationshipId);
    
    List<BuddySession> findByRelationshipIdAndStatus(
        String relationshipId,
        SessionStatus status
    );
    
    @Query("SELECT bs FROM BuddySession bs " +
           "WHERE bs.relationship.id = :relationshipId " +
           "AND bs.sessionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY bs.sessionDate ASC")
    List<BuddySession> findByRelationshipAndDateRange(
        @Param("relationshipId") String relationshipId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT bs FROM BuddySession bs " +
           "JOIN bs.relationship br " +
           "WHERE (br.user1.id = :userId OR br.user2.id = :userId) " +
           "AND bs.status = 'SCHEDULED' " +
           "AND bs.sessionDate >= :now " +
           "ORDER BY bs.sessionDate ASC")
    List<BuddySession> findUpcomingSessionsForUser(
        @Param("userId") String userId,
        @Param("now") LocalDateTime now
    );
    
    @Query("SELECT bs FROM BuddySession bs " +
           "WHERE bs.status = 'SCHEDULED' " +
           "AND bs.sessionDate < :now")
    List<BuddySession> findOverdueSessions(@Param("now") LocalDateTime now);
    
    @Query("SELECT bs FROM BuddySession bs " +
           "WHERE bs.relationship.id = :relationshipId " +
           "AND bs.status = 'SCHEDULED' " +
           "AND bs.sessionDate BETWEEN :windowStart AND :windowEnd")
    Optional<BuddySession> findSessionInTimeWindow(
        @Param("relationshipId") String relationshipId,
        @Param("windowStart") LocalDateTime windowStart,
        @Param("windowEnd") LocalDateTime windowEnd
    );
    
    @Query("SELECT COUNT(bs) FROM BuddySession bs " +
           "WHERE bs.relationship.id = :relationshipId " +
           "AND bs.status = :status")
    Long countByRelationshipAndStatus(
        @Param("relationshipId") String relationshipId,
        @Param("status") SessionStatus status
    );
    
    @Query("SELECT AVG(bs.actualDurationMinutes) FROM BuddySession bs " +
           "WHERE bs.relationship.id = :relationshipId " +
           "AND bs.status = 'COMPLETED'")
    Double getAverageSessionDurationForRelationship(@Param("relationshipId") String relationshipId);
    
    @Query("SELECT AVG((bs.user1Rating + bs.user2Rating) / 2.0) FROM BuddySession bs " +
           "WHERE bs.relationship.id = :relationshipId " +
           "AND bs.status = 'COMPLETED' " +
           "AND bs.user1Rating IS NOT NULL " +
           "AND bs.user2Rating IS NOT NULL")
    Double getAverageRatingForRelationship(@Param("relationshipId") String relationshipId);
    
    @Query("SELECT bs.relationship.id, COUNT(bs) as sessionCount, " +
           "SUM(bs.actualDurationMinutes) as totalMinutes " +
           "FROM BuddySession bs " +
           "WHERE bs.status = 'COMPLETED' " +
           "AND bs.sessionDate >= :since " +
           "GROUP BY bs.relationship.id")
    List<Object[]> getSessionStatsByRelationship(@Param("since") LocalDateTime since);
}