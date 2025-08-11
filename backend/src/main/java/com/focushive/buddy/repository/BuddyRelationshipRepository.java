package com.focushive.buddy.repository;

import com.focushive.buddy.entity.BuddyRelationship;
import com.focushive.buddy.entity.BuddyRelationship.RelationshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BuddyRelationshipRepository extends JpaRepository<BuddyRelationship, Long> {
    
    @Query("SELECT br FROM BuddyRelationship br " +
           "WHERE (br.user1.id = :userId OR br.user2.id = :userId) " +
           "AND br.status = :status")
    List<BuddyRelationship> findByUserIdAndStatus(
        @Param("userId") Long userId,
        @Param("status") RelationshipStatus status
    );
    
    @Query("SELECT br FROM BuddyRelationship br " +
           "WHERE ((br.user1.id = :user1Id AND br.user2.id = :user2Id) " +
           "OR (br.user1.id = :user2Id AND br.user2.id = :user1Id)) " +
           "AND br.status IN :statuses")
    Optional<BuddyRelationship> findByUserIds(
        @Param("user1Id") Long user1Id,
        @Param("user2Id") Long user2Id,
        @Param("statuses") List<RelationshipStatus> statuses
    );
    
    @Query("SELECT br FROM BuddyRelationship br " +
           "WHERE br.user2.id = :userId " +
           "AND br.status = 'PENDING'")
    List<BuddyRelationship> findPendingRequestsForUser(@Param("userId") Long userId);
    
    @Query("SELECT br FROM BuddyRelationship br " +
           "WHERE br.user1.id = :userId " +
           "AND br.status = 'PENDING'")
    List<BuddyRelationship> findSentRequestsByUser(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(br) FROM BuddyRelationship br " +
           "WHERE (br.user1.id = :userId OR br.user2.id = :userId) " +
           "AND br.status = 'ACTIVE'")
    Long countActiveBuddiesForUser(@Param("userId") Long userId);
    
    @Query("SELECT br FROM BuddyRelationship br " +
           "WHERE br.status = 'ACTIVE' " +
           "AND br.endDate IS NOT NULL " +
           "AND br.endDate <= :now")
    List<BuddyRelationship> findExpiredRelationships(@Param("now") LocalDateTime now);
    
    @Query("SELECT br FROM BuddyRelationship br " +
           "WHERE (br.user1.id = :userId OR br.user2.id = :userId) " +
           "AND br.status = 'ACTIVE' " +
           "ORDER BY br.startDate DESC")
    List<BuddyRelationship> findActiveBuddiesForUser(@Param("userId") Long userId);
    
    @Query("SELECT CASE WHEN COUNT(br) > 0 THEN true ELSE false END " +
           "FROM BuddyRelationship br " +
           "WHERE ((br.user1.id = :user1Id AND br.user2.id = :user2Id) " +
           "OR (br.user1.id = :user2Id AND br.user2.id = :user1Id)) " +
           "AND br.status = 'ACTIVE'")
    boolean areUsersBuddies(
        @Param("user1Id") Long user1Id,
        @Param("user2Id") Long user2Id
    );
    
    @Query("SELECT br FROM BuddyRelationship br " +
           "WHERE (br.user1.id = :userId OR br.user2.id = :userId) " +
           "AND br.status IN ('COMPLETED', 'TERMINATED') " +
           "ORDER BY br.endDate DESC")
    List<BuddyRelationship> findPastBuddiesForUser(@Param("userId") Long userId);
}