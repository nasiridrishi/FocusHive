package com.focushive.buddy.repository;

import com.focushive.buddy.entity.BuddyCheckin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BuddyCheckinRepository extends JpaRepository<BuddyCheckin, String> {
    
    List<BuddyCheckin> findByRelationshipIdOrderByCheckinTimeDesc(String relationshipId);
    
    @Query("SELECT bc FROM BuddyCheckin bc " +
           "WHERE bc.relationship.id = :relationshipId " +
           "AND bc.checkinTime BETWEEN :startDate AND :endDate " +
           "ORDER BY bc.checkinTime DESC")
    List<BuddyCheckin> findByRelationshipAndDateRange(
        @Param("relationshipId") String relationshipId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT bc FROM BuddyCheckin bc " +
           "WHERE bc.relationship.id = :relationshipId " +
           "AND bc.initiatedBy.id = :userId " +
           "ORDER BY bc.checkinTime DESC")
    List<BuddyCheckin> findByRelationshipAndInitiator(
        @Param("relationshipId") String relationshipId,
        @Param("userId") String userId
    );
    
    @Query("SELECT COUNT(bc) FROM BuddyCheckin bc " +
           "WHERE bc.relationship.id = :relationshipId " +
           "AND bc.checkinTime >= :since")
    Long countRecentCheckinsForRelationship(
        @Param("relationshipId") String relationshipId,
        @Param("since") LocalDateTime since
    );
    
    @Query("SELECT bc FROM BuddyCheckin bc " +
           "JOIN bc.relationship br " +
           "WHERE (br.user1.id = :userId OR br.user2.id = :userId) " +
           "AND bc.checkinTime >= :since " +
           "ORDER BY bc.checkinTime DESC")
    List<BuddyCheckin> findRecentCheckinsForUser(
        @Param("userId") String userId,
        @Param("since") LocalDateTime since
    );
    
    @Query("SELECT AVG(bc.moodRating) FROM BuddyCheckin bc " +
           "WHERE bc.relationship.id = :relationshipId " +
           "AND bc.checkinTime >= :since")
    Double getAverageMoodForRelationship(
        @Param("relationshipId") String relationshipId,
        @Param("since") LocalDateTime since
    );
    
    @Query("SELECT bc.relationship.id, COUNT(bc) as checkinCount, AVG(bc.progressRating) as avgProgress " +
           "FROM BuddyCheckin bc " +
           "WHERE bc.checkinTime >= :since " +
           "GROUP BY bc.relationship.id")
    List<Object[]> getCheckinStatsByRelationship(@Param("since") LocalDateTime since);
}