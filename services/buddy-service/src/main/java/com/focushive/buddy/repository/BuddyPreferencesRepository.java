package com.focushive.buddy.repository;

import com.focushive.buddy.entity.BuddyPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for BuddyPreferences entity.
 * Provides data access methods for user preferences used in buddy matching.
 *
 * Features:
 * - Basic CRUD operations via JpaRepository
 * - Custom finder methods for matching algorithm
 * - PostgreSQL array and JSONB query support
 * - Batch update operations for performance
 */
@Repository
public interface BuddyPreferencesRepository extends JpaRepository<BuddyPreferences, UUID> {

    /**
     * Finds user preferences by user ID.
     * Used for retrieving a specific user's preferences.
     */
    Optional<BuddyPreferences> findByUserId(UUID userId);

    /**
     * Finds all users who have matching enabled.
     * Primary method for finding active candidates for matching.
     */
    List<BuddyPreferences> findByMatchingEnabledTrue();

    /**
     * Finds all users in a specific timezone.
     * Used for timezone-based matching.
     */
    List<BuddyPreferences> findByPreferredTimezone(String timezone);

    /**
     * Finds all users who speak a specific language.
     * Used for language-based matching.
     */
    List<BuddyPreferences> findByLanguage(String language);

    /**
     * Finds users with timezone flexibility greater than specified hours.
     * Used to find flexible users for cross-timezone matching.
     */
    List<BuddyPreferences> findByTimezoneFlexibilityGreaterThanEqual(Integer flexibility);

    /**
     * Finds users with minimum commitment hours greater than specified.
     * Used to match users with similar commitment levels.
     */
    List<BuddyPreferences> findByMinCommitmentHoursGreaterThan(Integer hours);

    /**
     * Finds users who can accept more than the specified number of partners.
     * Used to find users with available capacity for new partnerships.
     */
    List<BuddyPreferences> findByMaxPartnersGreaterThan(Integer count);

    /**
     * Finds users who haven't been active since the specified date.
     * Used for cleanup and inactive user identification.
     */
    List<BuddyPreferences> findByLastActiveAtBefore(LocalDateTime date);

    /**
     * Finds users who have a specific interest in their focus areas.
     * Uses PostgreSQL array containment operator.
     */
    @Query(value = "SELECT * FROM buddy_preferences bp WHERE :interest = ANY(bp.focus_areas)", nativeQuery = true)
    List<BuddyPreferences> findByFocusAreasContaining(@Param("interest") String interest);

    /**
     * Finds users who have a specific goal.
     * Uses PostgreSQL array containment operator.
     */
    @Query(value = "SELECT * FROM buddy_preferences bp WHERE :goal = ANY(bp.goals)", nativeQuery = true)
    List<BuddyPreferences> findByGoalsContaining(@Param("goal") String goal);

    /**
     * Updates the last active timestamp for a specific user.
     * Used to track user activity without loading the full entity.
     */
    @Modifying
    @Query("UPDATE BuddyPreferences bp SET bp.lastActiveAt = :timestamp WHERE bp.userId = :userId")
    int updateLastActiveAt(@Param("userId") String userId, @Param("timestamp") LocalDateTime timestamp);

    /**
     * Finds users who were inactive before a certain cutoff date.
     * Used for identifying users who need re-engagement.
     */
    @Query("SELECT bp FROM BuddyPreferences bp WHERE bp.lastActiveAt < :cutoff OR bp.lastActiveAt IS NULL")
    List<BuddyPreferences> findInactiveUsers(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Finds users who start work early on Monday (before specified hour).
     * Demonstrates JSONB querying capabilities.
     */
    @Query(value = "SELECT * FROM buddy_preferences bp " +
           "WHERE (bp.preferred_work_hours->>'monday'->>'startHour')::int < :maxStartHour",
           nativeQuery = true)
    List<BuddyPreferences> findEarlyStartersOnMonday(@Param("maxStartHour") int maxStartHour);

    /**
     * Counts users by timezone for analytics.
     */
    @Query("SELECT bp.preferredTimezone, COUNT(bp) FROM BuddyPreferences bp " +
           "GROUP BY bp.preferredTimezone")
    List<Object[]> countUsersByTimezone();

    /**
     * Finds potential matches based on multiple criteria.
     * Complex matching query combining timezone, interests, and availability.
     */
    @Query("SELECT bp FROM BuddyPreferences bp WHERE " +
           "bp.matchingEnabled = true AND " +
           "bp.userId != :excludeUserId AND " +
           "(bp.preferredTimezone = :timezone OR bp.timezoneFlexibility >= :minFlexibility) AND " +
           "bp.maxPartners > 0")
    List<BuddyPreferences> findPotentialMatches(
        @Param("excludeUserId") String excludeUserId,
        @Param("timezone") String timezone,
        @Param("minFlexibility") Integer minFlexibility
    );

    /**
     * Finds users with overlapping focus areas.
     * Uses PostgreSQL array overlap operator (&&).
     */
    @Query(value = "SELECT * FROM buddy_preferences bp1 " +
           "WHERE bp1.user_id != :userId AND " +
           "bp1.matching_enabled = true AND " +
           "bp1.focus_areas && (SELECT focus_areas FROM buddy_preferences WHERE user_id = :userId)",
           nativeQuery = true)
    List<BuddyPreferences> findUsersWithOverlappingInterests(@Param("userId") String userId);

    /**
     * Updates matching enabled status for multiple users.
     * Batch operation for performance.
     */
    @Modifying
    @Query("UPDATE BuddyPreferences bp SET bp.matchingEnabled = :enabled WHERE bp.userId IN :userIds")
    int updateMatchingEnabledForUsers(@Param("userIds") List<String> userIds, @Param("enabled") boolean enabled);

    /**
     * Finds users with compatible communication styles.
     */
    @Query("SELECT bp FROM BuddyPreferences bp WHERE " +
           "bp.matchingEnabled = true AND " +
           "bp.userId != :excludeUserId AND " +
           "((bp.communicationStyle = 'MODERATE') OR " +
           " (bp.communicationStyle = :style) OR " +
           " (:style = 'MODERATE'))")
    List<BuddyPreferences> findUsersWithCompatibleCommunicationStyle(
        @Param("excludeUserId") String excludeUserId,
        @Param("style") String style
    );

    /**
     * Gets user preferences with their focus areas count for analytics.
     */
    @Query("SELECT bp, array_length(bp.focusAreas, 1) as focusCount FROM BuddyPreferences bp " +
           "WHERE bp.matchingEnabled = true ORDER BY focusCount DESC")
    List<Object[]> findUsersWithFocusAreasCount();

    /**
     * Finds recently active users (active within specified days).
     */
    @Query("SELECT bp FROM BuddyPreferences bp WHERE " +
           "bp.lastActiveAt >= :cutoff AND bp.matchingEnabled = true")
    List<BuddyPreferences> findRecentlyActiveUsers(@Param("cutoff") LocalDateTime cutoff);
}