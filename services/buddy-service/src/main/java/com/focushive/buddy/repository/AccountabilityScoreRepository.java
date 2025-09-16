package com.focushive.buddy.repository;

import com.focushive.buddy.entity.AccountabilityScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for AccountabilityScore entities.
 * Provides data access methods for accountability metrics and scoring operations.
 */
@Repository
public interface AccountabilityScoreRepository extends JpaRepository<AccountabilityScore, UUID> {

    // ===============================================================================
    // BASIC QUERIES
    // ===============================================================================

    /**
     * Finds accountability score by user ID and partnership ID.
     * This is the most common query for partnership-specific accountability.
     */
    Optional<AccountabilityScore> findByUserIdAndPartnershipId(UUID userId, UUID partnershipId);

    /**
     * Finds all accountability scores for a specific partnership.
     * Used to get both partners' scores for comparison.
     */
    List<AccountabilityScore> findByPartnershipId(UUID partnershipId);

    /**
     * Finds all accountability scores for a specific user across all partnerships.
     * Used to get overall user accountability metrics.
     */
    List<AccountabilityScore> findByUserId(UUID userId);

    /**
     * Finds accountability scores for users with no partnership (overall user scores).
     */
    List<AccountabilityScore> findByPartnershipIdIsNull();

    // ===============================================================================
    // UPDATE OPERATIONS
    // ===============================================================================

    /**
     * Updates accountability score and related metrics.
     * Used for periodic score recalculations.
     */
    @Modifying
    @Query("UPDATE AccountabilityScore a SET a.score = :score, a.checkinsCompleted = :checkins, " +
           "a.streakDays = :streak, a.calculatedAt = :timestamp WHERE a.id = :id")
    void updateScore(@Param("id") UUID id, @Param("score") BigDecimal score,
                    @Param("checkins") Integer checkins, @Param("streak") Integer streak,
                    @Param("timestamp") LocalDateTime timestamp);

    /**
     * Updates response rate for a specific accountability score.
     */
    @Modifying
    @Query("UPDATE AccountabilityScore a SET a.responseRate = :responseRate, a.calculatedAt = :timestamp " +
           "WHERE a.userId = :userId AND a.partnershipId = :partnershipId")
    void updateResponseRate(@Param("userId") UUID userId, @Param("partnershipId") UUID partnershipId,
                           @Param("responseRate") BigDecimal responseRate, @Param("timestamp") LocalDateTime timestamp);

    /**
     * Updates goal achievement count.
     */
    @Modifying
    @Query("UPDATE AccountabilityScore a SET a.goalsAchieved = :goalsAchieved, a.calculatedAt = :timestamp " +
           "WHERE a.userId = :userId AND a.partnershipId = :partnershipId")
    void updateGoalsAchieved(@Param("userId") UUID userId, @Param("partnershipId") UUID partnershipId,
                            @Param("goalsAchieved") Integer goalsAchieved, @Param("timestamp") LocalDateTime timestamp);

    /**
     * Increments check-in completion count.
     */
    @Modifying
    @Query("UPDATE AccountabilityScore a SET a.checkinsCompleted = a.checkinsCompleted + 1, a.calculatedAt = CURRENT_TIMESTAMP " +
           "WHERE a.userId = :userId AND a.partnershipId = :partnershipId")
    void incrementCheckinsCompleted(@Param("userId") UUID userId, @Param("partnershipId") UUID partnershipId);

    /**
     * Updates streak days for a user.
     */
    @Modifying
    @Query("UPDATE AccountabilityScore a SET a.streakDays = :streakDays, a.calculatedAt = :timestamp " +
           "WHERE a.userId = :userId AND a.partnershipId = :partnershipId")
    void updateStreakDays(@Param("userId") UUID userId, @Param("partnershipId") UUID partnershipId,
                         @Param("streakDays") Integer streakDays, @Param("timestamp") LocalDateTime timestamp);

    // ===============================================================================
    // ANALYTICS AND REPORTING
    // ===============================================================================

    /**
     * Finds top performers by accountability score.
     */
    @Query("SELECT a FROM AccountabilityScore a WHERE a.partnershipId IS NOT NULL ORDER BY a.score DESC")
    List<AccountabilityScore> findTopPerformers();

    /**
     * Finds users with low accountability scores who may need support.
     */
    @Query("SELECT a FROM AccountabilityScore a WHERE a.score < :threshold ORDER BY a.score ASC")
    List<AccountabilityScore> findLowAccountabilityUsers(@Param("threshold") BigDecimal threshold);

    /**
     * Finds users on active streaks.
     */
    @Query("SELECT a FROM AccountabilityScore a WHERE a.streakDays >= :minStreak ORDER BY a.streakDays DESC")
    List<AccountabilityScore> findUsersOnStreak(@Param("minStreak") Integer minStreak);

    /**
     * Calculates average accountability score for a partnership.
     */
    @Query("SELECT AVG(a.score) FROM AccountabilityScore a WHERE a.partnershipId = :partnershipId")
    Double calculateAveragePartnershipScore(@Param("partnershipId") UUID partnershipId);

    /**
     * Calculates overall statistics for accountability scores.
     */
    @Query("SELECT " +
           "AVG(a.score) as avgScore, " +
           "MAX(a.score) as maxScore, " +
           "MIN(a.score) as minScore, " +
           "AVG(a.streakDays) as avgStreak, " +
           "COUNT(a) as totalUsers " +
           "FROM AccountabilityScore a WHERE a.partnershipId IS NOT NULL")
    List<Object[]> calculateAccountabilityStatistics();

    // ===============================================================================
    // PARTNERSHIP-SPECIFIC QUERIES
    // ===============================================================================

    /**
     * Finds accountability scores for partnerships with scores below threshold.
     * Used to identify partnerships that may need intervention.
     */
    @Query("SELECT a FROM AccountabilityScore a " +
           "WHERE a.partnershipId = :partnershipId " +
           "AND a.score < :threshold")
    List<AccountabilityScore> findLowScorePartnerships(@Param("partnershipId") UUID partnershipId,
                                                      @Param("threshold") BigDecimal threshold);

    /**
     * Finds partnerships where both users have high accountability.
     */
    @Query("SELECT a.partnershipId FROM AccountabilityScore a " +
           "WHERE a.score >= :threshold " +
           "GROUP BY a.partnershipId " +
           "HAVING COUNT(a.partnershipId) = 2")
    List<UUID> findHighPerformingPartnerships(@Param("threshold") BigDecimal threshold);

    /**
     * Finds partnerships with mismatched accountability scores.
     */
    @Query(value = """
        WITH partnership_scores AS (
            SELECT partnership_id,
                   MIN(score) as min_score,
                   MAX(score) as max_score,
                   MAX(score) - MIN(score) as score_diff
            FROM accountability_scores
            WHERE partnership_id IS NOT NULL
            GROUP BY partnership_id
            HAVING COUNT(*) = 2
        )
        SELECT partnership_id
        FROM partnership_scores
        WHERE score_diff >= :threshold
        """, nativeQuery = true)
    List<UUID> findMismatchedPartnerships(@Param("threshold") BigDecimal threshold);

    // ===============================================================================
    // TIME-BASED QUERIES
    // ===============================================================================

    /**
     * Finds accountability scores that haven't been updated recently.
     * Used to identify scores that need recalculation.
     */
    @Query("SELECT a FROM AccountabilityScore a WHERE a.calculatedAt < :cutoffTime")
    List<AccountabilityScore> findStaleScores(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Finds recently updated accountability scores.
     */
    @Query("SELECT a FROM AccountabilityScore a WHERE a.calculatedAt >= :since ORDER BY a.calculatedAt DESC")
    List<AccountabilityScore> findRecentlyUpdatedScores(@Param("since") LocalDateTime since);

    /**
     * Finds scores that were calculated today.
     */
    @Query("SELECT a FROM AccountabilityScore a WHERE a.calculatedAt >= :startOfDay AND a.calculatedAt < :endOfDay")
    List<AccountabilityScore> findTodaysCalculations(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * Helper method to find today's calculations.
     */
    default List<AccountabilityScore> findTodaysCalculations() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().plusDays(1).atStartOfDay();
        return findTodaysCalculations(startOfDay, endOfDay);
    }

    // ===============================================================================
    // BULK OPERATIONS
    // ===============================================================================

    /**
     * Deletes accountability scores for ended partnerships.
     */
    @Modifying
    @Query("DELETE FROM AccountabilityScore a WHERE a.partnershipId IN " +
           "(SELECT p.id FROM BuddyPartnership p WHERE p.status = 'ENDED')")
    void deleteScoresForEndedPartnerships();

    /**
     * Resets streaks that are older than the specified days.
     */
    @Modifying
    @Query("UPDATE AccountabilityScore a SET a.streakDays = 0, a.calculatedAt = CURRENT_TIMESTAMP " +
           "WHERE a.calculatedAt < :cutoffTime AND a.streakDays > 0")
    void resetOldStreaks(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Recalculates all accountability scores (triggers entity calculateScore method).
     */
    @Query("SELECT a FROM AccountabilityScore a")
    List<AccountabilityScore> findAllForRecalculation();

    // ===============================================================================
    // VALIDATION QUERIES
    // ===============================================================================

    /**
     * Checks if a user already has an accountability score for a partnership.
     */
    @Query("SELECT COUNT(a) > 0 FROM AccountabilityScore a WHERE a.userId = :userId AND a.partnershipId = :partnershipId")
    boolean existsByUserIdAndPartnershipId(@Param("userId") UUID userId, @Param("partnershipId") UUID partnershipId);

    /**
     * Counts accountability scores for a partnership.
     */
    long countByPartnershipId(UUID partnershipId);

    /**
     * Finds duplicate accountability scores (should not exist due to unique constraint).
     */
    @Query("SELECT a.userId, a.partnershipId, COUNT(*) FROM AccountabilityScore a " +
           "GROUP BY a.userId, a.partnershipId HAVING COUNT(*) > 1")
    List<Object[]> findDuplicateScores();

    // ===============================================================================
    // RANKING AND COMPARISON
    // ===============================================================================

    /**
     * Gets user ranking by accountability score within partnerships.
     */
    @Query(value = """
        SELECT user_id, score,
               RANK() OVER (ORDER BY score DESC) as ranking
        FROM accountability_scores
        WHERE partnership_id IS NOT NULL
        AND user_id = :userId
        """, nativeQuery = true)
    List<Object[]> getUserRanking(@Param("userId") UUID userId);

    /**
     * Compares user score with partnership average.
     */
    @Query("SELECT a.score, " +
           "(SELECT AVG(a2.score) FROM AccountabilityScore a2 WHERE a2.partnershipId = a.partnershipId) as partnershipAvg " +
           "FROM AccountabilityScore a WHERE a.userId = :userId AND a.partnershipId = :partnershipId")
    List<Object[]> compareWithPartnershipAverage(@Param("userId") UUID userId, @Param("partnershipId") UUID partnershipId);

    /**
     * Finds improvement trends by comparing current score with historical data.
     */
    @Query(value = """
        WITH score_history AS (
            SELECT score, updated_at,
                   LAG(score) OVER (ORDER BY updated_at) as previous_score
            FROM accountability_scores
            WHERE user_id = :userId AND partnership_id = :partnershipId
            ORDER BY updated_at DESC
            LIMIT 10
        )
        SELECT
            CASE
                WHEN AVG(score - COALESCE(previous_score, score)) > 0.05 THEN 'IMPROVING'
                WHEN AVG(score - COALESCE(previous_score, score)) < -0.05 THEN 'DECLINING'
                ELSE 'STABLE'
            END as trend
        FROM score_history
        WHERE previous_score IS NOT NULL
        """, nativeQuery = true)
    String getScoreTrend(@Param("userId") UUID userId, @Param("partnershipId") UUID partnershipId);
}