package com.focushive.buddy.repository;

import com.focushive.buddy.entity.BuddyPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository alias for BuddyPreferences with matching-focused naming.
 * This provides a more semantic interface for matching operations
 * while using the same underlying BuddyPreferences entity.
 */
@Repository
public interface MatchingPreferencesRepository extends JpaRepository<BuddyPreferences, UUID> {

    /**
     * Find preferences by user ID.
     */
    Optional<BuddyPreferences> findByUserId(UUID userId);

    /**
     * Find all users with matching enabled.
     */
    List<BuddyPreferences> findByMatchingEnabledTrue();

    /**
     * Find users available for matching (enabled and recently active).
     * TODO: Fix interval syntax for JPQL compatibility
     */
    List<BuddyPreferences> findByMatchingEnabledTrueAndLastActiveAtAfter(LocalDateTime lastActiveAfter);

    /**
     * Find users by timezone flexibility range.
     */
    @Query("SELECT bp FROM BuddyPreferences bp WHERE bp.matchingEnabled = true AND " +
           "bp.timezoneFlexibility >= :minFlexibility")
    List<BuddyPreferences> findByTimezoneFlexibilityGreaterThanEqual(@Param("minFlexibility") Integer minFlexibility);

    /**
     * Find users with specific focus areas using PostgreSQL array operations.
     * Uses native query for optimal PostgreSQL array performance.
     */
    @Query(value = "SELECT * FROM buddy_preferences bp WHERE bp.matching_enabled = true AND " +
           "bp.focus_areas && CAST(:focusAreas AS text[])",
           nativeQuery = true)
    List<BuddyPreferences> findByFocusAreasIn(@Param("focusAreas") String[] focusAreas);

    /**
     * Find users with specific goals using PostgreSQL array operations.
     * Uses native query for optimal PostgreSQL array performance.
     */
    @Query(value = "SELECT * FROM buddy_preferences bp WHERE bp.matching_enabled = true AND " +
           "bp.goals && CAST(:goals AS text[])",
           nativeQuery = true)
    List<BuddyPreferences> findByGoalsIn(@Param("goals") String[] goals);

    /**
     * Find users with ANY of the specified focus areas (optimized for large datasets).
     */
    @Query(value = "SELECT * FROM buddy_preferences bp WHERE bp.matching_enabled = true AND " +
           "EXISTS (SELECT 1 FROM unnest(bp.focus_areas) AS fa WHERE fa = ANY(:focusAreas))",
           nativeQuery = true)
    List<BuddyPreferences> findByAnyFocusArea(@Param("focusAreas") String[] focusAreas);

    /**
     * Find users with ANY of the specified goals (optimized for large datasets).
     */
    @Query(value = "SELECT * FROM buddy_preferences bp WHERE bp.matching_enabled = true AND " +
           "EXISTS (SELECT 1 FROM unnest(bp.goals) AS g WHERE g = ANY(:goals))",
           nativeQuery = true)
    List<BuddyPreferences> findByAnyGoal(@Param("goals") String[] goals);

    /**
     * Find users with capacity for more partners.
     */
    @Query("SELECT bp FROM BuddyPreferences bp WHERE bp.matchingEnabled = true AND " +
           "bp.maxPartners > (SELECT COUNT(p) FROM BuddyPartnership p WHERE " +
           "(p.user1Id = bp.userId OR p.user2Id = bp.userId) AND p.status = 'ACTIVE')")
    List<BuddyPreferences> findUsersWithCapacity();

    /**
     * Check if user has matching enabled.
     */
    @Query("SELECT bp.matchingEnabled FROM BuddyPreferences bp WHERE bp.userId = :userId")
    Boolean isMatchingEnabled(@Param("userId") UUID userId);

    /**
     * Count users available for matching.
     */
    @Query("SELECT COUNT(bp) FROM BuddyPreferences bp WHERE bp.matchingEnabled = true")
    Long countAvailableForMatching();

    /**
     * Find potential matches for a user based on comprehensive criteria.
     * Uses optimized native query for best performance.
     */
    @Query(value = """
        SELECT bp.* FROM buddy_preferences bp
        WHERE bp.matching_enabled = true
        AND bp.user_id != :userId
        AND bp.last_active_at >= :minActiveDate
        AND (bp.timezone_flexibility >= :minTimezoneFlexibility OR bp.preferred_timezone = :userTimezone)
        AND bp.max_partners > (
            SELECT COUNT(p.id) FROM buddy_partnerships p
            WHERE (p.user1_id = bp.user_id OR p.user2_id = bp.user_id)
            AND p.status = 'ACTIVE'
        )
        ORDER BY bp.last_active_at DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<BuddyPreferences> findPotentialMatches(
        @Param("userId") UUID userId,
        @Param("minActiveDate") LocalDateTime minActiveDate,
        @Param("minTimezoneFlexibility") Integer minTimezoneFlexibility,
        @Param("userTimezone") String userTimezone,
        @Param("maxResults") Integer maxResults
    );

    /**
     * Find users with overlapping focus areas and goals (high compatibility).
     */
    @Query(value = """
        SELECT bp.* FROM buddy_preferences bp
        WHERE bp.matching_enabled = true
        AND bp.user_id != :userId
        AND (bp.focus_areas && :userFocusAreas OR bp.goals && :userGoals)
        AND bp.last_active_at >= :minActiveDate
        ORDER BY (
            CASE WHEN bp.focus_areas && :userFocusAreas THEN 2 ELSE 0 END +
            CASE WHEN bp.goals && :userGoals THEN 2 ELSE 0 END +
            CASE WHEN bp.communication_style = :communicationStyle THEN 1 ELSE 0 END
        ) DESC, bp.last_active_at DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<BuddyPreferences> findHighCompatibilityMatches(
        @Param("userId") UUID userId,
        @Param("userFocusAreas") String[] userFocusAreas,
        @Param("userGoals") String[] userGoals,
        @Param("communicationStyle") String communicationStyle,
        @Param("minActiveDate") LocalDateTime minActiveDate,
        @Param("maxResults") Integer maxResults
    );

    /**
     * Count overlapping focus areas between two users.
     */
    @Query(value = """
        SELECT array_length(
            ARRAY(
                SELECT unnest(bp1.focus_areas)
                INTERSECT
                SELECT unnest(bp2.focus_areas)
            ), 1
        ) as overlap_count
        FROM buddy_preferences bp1, buddy_preferences bp2
        WHERE bp1.user_id = :userId1 AND bp2.user_id = :userId2
        """, nativeQuery = true)
    Integer countFocusAreaOverlap(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);

    /**
     * Delete preferences by user ID.
     */
    void deleteByUserId(UUID userId);

    /**
     * Check if preferences exist for user.
     */
    boolean existsByUserId(UUID userId);
}