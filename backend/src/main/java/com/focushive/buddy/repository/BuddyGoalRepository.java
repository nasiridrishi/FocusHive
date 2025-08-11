package com.focushive.buddy.repository;

import com.focushive.buddy.entity.BuddyGoal;
import com.focushive.buddy.entity.BuddyGoal.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BuddyGoalRepository extends JpaRepository<BuddyGoal, Long> {
    
    List<BuddyGoal> findByRelationshipIdOrderByCreatedAtDesc(Long relationshipId);
    
    List<BuddyGoal> findByRelationshipIdAndStatus(
        Long relationshipId,
        GoalStatus status
    );
    
    @Query("SELECT bg FROM BuddyGoal bg " +
           "WHERE bg.relationship.id = :relationshipId " +
           "AND bg.dueDate <= :date " +
           "AND bg.status = 'IN_PROGRESS'")
    List<BuddyGoal> findOverdueGoals(
        @Param("relationshipId") Long relationshipId,
        @Param("date") LocalDateTime date
    );
    
    @Query("SELECT bg FROM BuddyGoal bg " +
           "WHERE bg.relationship.id = :relationshipId " +
           "AND bg.dueDate BETWEEN :startDate AND :endDate")
    List<BuddyGoal> findUpcomingGoals(
        @Param("relationshipId") Long relationshipId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT COUNT(bg) FROM BuddyGoal bg " +
           "WHERE bg.relationship.id = :relationshipId " +
           "AND bg.status = :status")
    Long countByRelationshipAndStatus(
        @Param("relationshipId") Long relationshipId,
        @Param("status") GoalStatus status
    );
    
    @Query("SELECT bg FROM BuddyGoal bg " +
           "JOIN bg.relationship br " +
           "WHERE (br.user1.id = :userId OR br.user2.id = :userId) " +
           "AND bg.status = 'IN_PROGRESS' " +
           "ORDER BY bg.dueDate ASC")
    List<BuddyGoal> findActiveGoalsForUser(@Param("userId") Long userId);
    
    @Query("SELECT bg.relationship.id, COUNT(bg) as completedCount " +
           "FROM BuddyGoal bg " +
           "WHERE bg.status = 'COMPLETED' " +
           "AND bg.completedAt >= :since " +
           "GROUP BY bg.relationship.id")
    List<Object[]> getCompletionStatsByRelationship(@Param("since") LocalDateTime since);
}