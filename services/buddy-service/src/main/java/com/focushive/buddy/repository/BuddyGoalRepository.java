package com.focushive.buddy.repository;

import com.focushive.buddy.constant.GoalStatus;
import com.focushive.buddy.entity.BuddyGoal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
 * Repository interface for BuddyGoal entities.
 * Provides CRUD operations and custom queries for goal management,
 * including partnership-based queries, progress tracking, and statistics.
 *
 * Query Methods:
 * - Basic CRUD operations via JpaRepository
 * - Partnership-specific queries (goals by partnership, counts)
 * - Status-based queries (active goals, completed goals, etc.)
 * - Date-based queries (overdue goals, upcoming deadlines, completion ranges)
 * - Progress-based queries (high progress goals, goals needing attention)
 * - Statistical queries (average progress, completion rates)
 * - Bulk operations (progress updates, batch operations)
 * - Pagination support for large datasets
 */
@Repository
public interface BuddyGoalRepository extends JpaRepository<BuddyGoal, UUID> {

    // Basic Partnership Queries

    /**
     * Find all goals for a specific partnership.
     * @param partnershipId the partnership ID
     * @return list of goals for the partnership
     */
    List<BuddyGoal> findByPartnershipId(UUID partnershipId);

    /**
     * Find goals by status.
     * @param status the goal status
     * @return list of goals with the specified status
     */
    List<BuddyGoal> findByStatus(GoalStatus status);

    /**
     * Find goals created by specific user.
     * @param userId the user ID
     * @return list of goals created by the user
     */
    List<BuddyGoal> findByCreatedBy(UUID userId);

    /**
     * Find goals created by specific user with specific status.
     * @param userId the user ID
     * @param status the goal status
     * @return list of goals created by the user with the status
     */
    List<BuddyGoal> findByCreatedByAndStatus(UUID userId, GoalStatus status);

    /**
     * Find goals by partnership and multiple statuses.
     * @param partnershipId the partnership ID
     * @param statuses list of statuses
     * @return list of goals matching partnership and statuses
     */
    List<BuddyGoal> findByPartnershipIdAndStatusIn(UUID partnershipId, List<GoalStatus> statuses);

    /**
     * Count goals by user.
     * @param userId the user ID
     * @return total number of goals for user
     */
    long countByCreatedBy(UUID userId);

    // Count Queries

    /**
     * Count goals for a partnership.
     * @param partnershipId the partnership ID
     * @return number of goals for the partnership
     */
    long countByPartnershipId(UUID partnershipId);

    /**
     * Count goals for a partnership with specific status.
     * @param partnershipId the partnership ID
     * @param status the goal status
     * @return number of goals with the specified status
     */
    long countByPartnershipIdAndStatus(UUID partnershipId, GoalStatus status);

    /**
     * Find goals by partnership and status.
     * @param partnershipId the partnership ID
     * @param status the goal status
     * @return list of goals matching partnership and status
     */
    List<BuddyGoal> findByPartnershipIdAndStatus(UUID partnershipId, GoalStatus status);

    // Date Queries

    /**
     * Find overdue goals (active goals past their target date).
     * @param date the current date to compare against
     * @return list of overdue goals
     */
    @Query("SELECT g FROM BuddyGoal g WHERE g.targetDate < :date AND g.status = 'ACTIVE'")
    List<BuddyGoal> findOverdueGoals(@Param("date") LocalDate date);

    /**
     * Find goals with upcoming deadlines within a date range.
     * @param start the start date of the range
     * @param end the end date of the range
     * @return list of goals with deadlines in the range
     */
    @Query("SELECT g FROM BuddyGoal g WHERE g.targetDate BETWEEN :start AND :end AND g.status = 'ACTIVE'")
    List<BuddyGoal> findUpcomingDeadlines(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * Find goals completed within a date range.
     * @param start the start timestamp
     * @param end the end timestamp
     * @return list of goals completed in the range
     */
    @Query("SELECT g FROM BuddyGoal g WHERE g.completedAt BETWEEN :start AND :end")
    List<BuddyGoal> findCompletedInDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Progress Queries

    /**
     * Find goals with progress percentage greater than threshold.
     * @param threshold the minimum progress percentage
     * @return list of goals with high progress
     */
    @Query("SELECT g FROM BuddyGoal g WHERE g.progressPercentage > :threshold")
    List<BuddyGoal> findByProgressPercentageGreaterThan(@Param("threshold") Integer threshold);

    /**
     * Find goals with progress at or above threshold.
     * @param threshold the minimum progress percentage
     * @return list of goals with high progress
     */
    @Query("SELECT g FROM BuddyGoal g WHERE g.progressPercentage >= :threshold")
    List<BuddyGoal> findGoalsWithHighProgress(@Param("threshold") Integer threshold);

    /**
     * Find goals needing attention (low progress with approaching deadline).
     * @param date the date to check deadlines against
     * @param threshold the maximum progress percentage for "low progress"
     * @return list of goals needing attention
     */
    @Query("SELECT g FROM BuddyGoal g WHERE g.progressPercentage < :threshold AND g.targetDate <= :date AND g.status = 'ACTIVE'")
    List<BuddyGoal> findGoalsNeedingAttention(@Param("date") LocalDate date, @Param("threshold") Integer threshold);

    // Statistics Queries

    /**
     * Calculate average progress for a partnership.
     * @param partnershipId the partnership ID
     * @return average progress percentage
     */
    @Query("SELECT AVG(g.progressPercentage) FROM BuddyGoal g WHERE g.partnershipId = :partnershipId")
    Double calculateAverageProgressByPartnership(@Param("partnershipId") UUID partnershipId);

    /**
     * Calculate average progress for all goals.
     * @return average progress percentage
     */
    @Query("SELECT AVG(g.progressPercentage) FROM BuddyGoal g WHERE g.partnershipId = :partnershipId")
    Double calculateAverageProgress(@Param("partnershipId") UUID partnershipId);

    /**
     * Count completed goals for a partnership.
     * @param partnershipId the partnership ID
     * @return number of completed goals
     */
    @Query("SELECT COUNT(g) FROM BuddyGoal g WHERE g.partnershipId = :partnershipId AND g.status = 'COMPLETED'")
    long countCompletedGoals(@Param("partnershipId") UUID partnershipId);

    // Bulk Operations

    /**
     * Update progress for a specific goal.
     * @param id the goal ID
     * @param progress the new progress percentage
     */
    @Modifying
    @Query("UPDATE BuddyGoal g SET g.progressPercentage = :progress WHERE g.id = :id")
    void updateProgress(@Param("id") UUID id, @Param("progress") Integer progress);

    // Pagination Queries

    /**
     * Find all goals with pagination.
     * @param pageable pagination parameters
     * @return page of goals
     */
    Page<BuddyGoal> findAll(Pageable pageable);

    /**
     * Find goals by partnership with pagination.
     * @param partnershipId the partnership ID
     * @param pageable pagination parameters
     * @return page of goals for the partnership
     */
    Page<BuddyGoal> findByPartnershipId(UUID partnershipId, Pageable pageable);
}