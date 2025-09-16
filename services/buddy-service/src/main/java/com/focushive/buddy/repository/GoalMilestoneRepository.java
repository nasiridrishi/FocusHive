package com.focushive.buddy.repository;

import com.focushive.buddy.entity.GoalMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for GoalMilestone entities.
 * Provides CRUD operations and custom queries for milestone management,
 * including goal-based queries, completion tracking, and statistics.
 *
 * Query Methods:
 * - Basic CRUD operations via JpaRepository
 * - Goal-specific queries (milestones by goal, counts)
 * - Completion-based queries (completed milestones, completion tracking)
 * - Date-based queries (upcoming milestones, overdue milestones)
 * - Bulk operations (milestone completion, batch updates)
 * - Statistical queries (completion rates, progress tracking)
 */
@Repository
public interface GoalMilestoneRepository extends JpaRepository<GoalMilestone, UUID> {

    // Basic Goal Queries

    /**
     * Find all milestones for a specific goal.
     * @param goalId the goal ID
     * @return list of milestones for the goal
     */
    List<GoalMilestone> findByGoalId(UUID goalId);

    // Count Queries

    /**
     * Count total milestones for a goal.
     * @param goalId the goal ID
     * @return number of milestones for the goal
     */
    long countByGoalId(UUID goalId);

    /**
     * Count completed milestones for a goal.
     * @param goalId the goal ID
     * @return number of completed milestones
     */
    long countByGoalIdAndCompletedAtIsNotNull(UUID goalId);

    /**
     * Count incomplete milestones for a goal.
     * @param goalId the goal ID
     * @return number of incomplete milestones
     */
    long countByGoalIdAndCompletedAtIsNull(UUID goalId);

    // Completion Queries

    /**
     * Find completed milestones for a goal.
     * @param goalId the goal ID
     * @return list of completed milestones
     */
    List<GoalMilestone> findByGoalIdAndCompletedAtIsNotNull(UUID goalId);

    /**
     * Find incomplete milestones for a goal.
     * @param goalId the goal ID
     * @return list of incomplete milestones
     */
    List<GoalMilestone> findByGoalIdAndCompletedAtIsNull(UUID goalId);

    /**
     * Find milestones completed by a specific user.
     * @param goalId the goal ID
     * @param userId the user ID
     * @return list of milestones completed by the user
     */
    List<GoalMilestone> findByGoalIdAndCompletedBy(UUID goalId, UUID userId);

    // Celebration Queries

    /**
     * Find milestones where celebration has not been sent.
     * @param goalId the goal ID
     * @return list of milestones needing celebration
     */
    List<GoalMilestone> findByGoalIdAndCelebrationSentFalse(UUID goalId);

    /**
     * Find completed milestones where celebration has not been sent.
     * @param goalId the goal ID
     * @return list of completed milestones needing celebration
     */
    List<GoalMilestone> findByGoalIdAndCompletedAtIsNotNullAndCelebrationSentFalse(UUID goalId);

    // Bulk Operations

    /**
     * Complete a milestone by setting completion timestamp and user.
     * @param id the milestone ID
     * @param timestamp the completion timestamp
     * @param userId the user who completed the milestone
     */
    @Modifying
    @Query("UPDATE GoalMilestone m SET m.completedAt = :timestamp, m.completedBy = :userId WHERE m.id = :id")
    void completeMilestone(@Param("id") UUID id, @Param("timestamp") LocalDateTime timestamp, @Param("userId") UUID userId);

    /**
     * Mark celebration as sent for a milestone.
     * @param id the milestone ID
     */
    @Modifying
    @Query("UPDATE GoalMilestone m SET m.celebrationSent = true WHERE m.id = :id")
    void markCelebrationSent(@Param("id") UUID id);

    /**
     * Mark celebration as sent for all completed milestones of a goal.
     * @param goalId the goal ID
     */
    @Modifying
    @Query("UPDATE GoalMilestone m SET m.celebrationSent = true WHERE m.goalId = :goalId AND m.completedAt IS NOT NULL")
    void markAllCelebrationsSentForGoal(@Param("goalId") UUID goalId);

    // Date-based Queries

    /**
     * Find overdue milestones (incomplete milestones past their target date).
     * @param goalId the goal ID
     * @return list of overdue milestones
     */
    @Query("SELECT m FROM GoalMilestone m WHERE m.goalId = :goalId AND m.targetDate < CURRENT_DATE AND m.completedAt IS NULL")
    List<GoalMilestone> findOverdueMilestones(@Param("goalId") UUID goalId);

    /**
     * Find upcoming milestones (incomplete milestones with near target dates).
     * @param goalId the goal ID
     * @param endDate the end date to look until
     * @return list of upcoming milestones
     */
    @Query("SELECT m FROM GoalMilestone m WHERE m.goalId = :goalId AND m.targetDate BETWEEN CURRENT_DATE AND :endDate AND m.completedAt IS NULL")
    List<GoalMilestone> findUpcomingMilestones(@Param("goalId") UUID goalId, @Param("endDate") LocalDate endDate);

    /**
     * Find milestones completed within a date range.
     * @param goalId the goal ID
     * @param start the start timestamp
     * @param end the end timestamp
     * @return list of milestones completed in the range
     */
    @Query("SELECT m FROM GoalMilestone m WHERE m.goalId = :goalId AND m.completedAt BETWEEN :start AND :end")
    List<GoalMilestone> findMilestonesCompletedInRange(@Param("goalId") UUID goalId,
                                                        @Param("start") LocalDateTime start,
                                                        @Param("end") LocalDateTime end);

    // Statistical Queries

    /**
     * Calculate completion percentage for a goal's milestones.
     * @param goalId the goal ID
     * @return completion percentage as double (0.0 to 1.0)
     */
    @Query("SELECT CAST(COUNT(CASE WHEN m.completedAt IS NOT NULL THEN 1 END) AS double) / COUNT(*) FROM GoalMilestone m WHERE m.goalId = :goalId")
    Double calculateCompletionPercentage(@Param("goalId") UUID goalId);

    /**
     * Get the most recent milestone completion for a goal.
     * @param goalId the goal ID
     * @return the most recent completion timestamp, or null if none completed
     */
    @Query("SELECT MAX(m.completedAt) FROM GoalMilestone m WHERE m.goalId = :goalId AND m.completedAt IS NOT NULL")
    LocalDateTime findMostRecentCompletion(@Param("goalId") UUID goalId);

    /**
     * Get the next upcoming milestone target date for a goal.
     * @param goalId the goal ID
     * @return the next target date, or null if no upcoming milestones
     */
    @Query("SELECT MIN(m.targetDate) FROM GoalMilestone m WHERE m.goalId = :goalId AND m.completedAt IS NULL AND m.targetDate >= CURRENT_DATE")
    java.time.LocalDate findNextUpcomingTargetDate(@Param("goalId") UUID goalId);

    // Clean-up Operations

    /**
     * Delete all milestones for a goal (used when goal is deleted).
     * Note: Cascade delete should handle this automatically, but provided for explicit control.
     * @param goalId the goal ID
     */
    @Modifying
    @Query("DELETE FROM GoalMilestone m WHERE m.goalId = :goalId")
    void deleteByGoalId(@Param("goalId") UUID goalId);
}