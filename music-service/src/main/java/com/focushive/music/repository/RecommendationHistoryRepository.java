package com.focushive.music.repository;

import com.focushive.music.model.RecommendationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing music recommendation history.
 * Provides methods for querying, analyzing, and managing user recommendation data.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Repository
public interface RecommendationHistoryRepository extends JpaRepository<RecommendationHistory, UUID> {

    /**
     * Finds all recommendation history for a specific user.
     */
    Page<RecommendationHistory> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Finds recent recommendation history for a user.
     */
    @Query("SELECT rh FROM RecommendationHistory rh WHERE rh.userId = :userId " +
           "AND rh.createdAt >= :since ORDER BY rh.createdAt DESC")
    List<RecommendationHistory> findByUserIdAndCreatedAtAfter(
            @Param("userId") UUID userId, 
            @Param("since") LocalDateTime since);

    /**
     * Finds recommendation history by task type.
     */
    @Query("SELECT rh FROM RecommendationHistory rh WHERE rh.userId = :userId " +
           "AND rh.taskType = :taskType ORDER BY rh.createdAt DESC")
    List<RecommendationHistory> findByUserIdAndTaskType(
            @Param("userId") UUID userId, 
            @Param("taskType") String taskType);

    /**
     * Finds recommendation history by mood.
     */
    @Query("SELECT rh FROM RecommendationHistory rh WHERE rh.userId = :userId " +
           "AND rh.mood = :mood ORDER BY rh.createdAt DESC")
    List<RecommendationHistory> findByUserIdAndMood(
            @Param("userId") UUID userId, 
            @Param("mood") String mood);

    /**
     * Finds the most successful recommendations for a user.
     */
    @Query("SELECT rh FROM RecommendationHistory rh WHERE rh.userId = :userId " +
           "AND rh.averageRating >= :minRating " +
           "ORDER BY rh.averageRating DESC, rh.acceptanceRate DESC")
    List<RecommendationHistory> findSuccessfulRecommendations(
            @Param("userId") UUID userId, 
            @Param("minRating") Double minRating);

    /**
     * Finds recommendations by session ID.
     */
    Optional<RecommendationHistory> findBySessionId(UUID sessionId);

    /**
     * Finds collaborative recommendations for a hive.
     */
    @Query("SELECT rh FROM RecommendationHistory rh WHERE rh.hiveId = :hiveId " +
           "ORDER BY rh.createdAt DESC")
    List<RecommendationHistory> findByHiveId(@Param("hiveId") UUID hiveId);

    /**
     * Gets recommendation statistics for a user over a time period.
     */
    @Query("SELECT COUNT(rh), AVG(rh.averageRating), AVG(rh.acceptanceRate), " +
           "AVG(rh.diversityScore) FROM RecommendationHistory rh " +
           "WHERE rh.userId = :userId AND rh.createdAt BETWEEN :start AND :end")
    Object[] getRecommendationStats(
            @Param("userId") UUID userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Finds the most frequently recommended genres for a user.
     */
    @Query("SELECT genre, COUNT(genre) as frequency FROM RecommendationHistory rh " +
           "JOIN rh.genreDistribution genre " +
           "WHERE rh.userId = :userId " +
           "GROUP BY genre ORDER BY frequency DESC")
    List<Object[]> getMostRecommendedGenres(@Param("userId") UUID userId);

    /**
     * Finds similar users based on recommendation history.
     */
    @Query("SELECT rh2.userId, COUNT(DISTINCT rh1.recommendationId) as commonCount " +
           "FROM RecommendationHistory rh1, RecommendationHistory rh2 " +
           "WHERE rh1.userId = :userId AND rh1.userId != rh2.userId " +
           "AND EXISTS (SELECT 1 FROM rh1.trackIds t1 WHERE t1 IN rh2.trackIds) " +
           "GROUP BY rh2.userId " +
           "HAVING commonCount >= :minCommonRecommendations " +
           "ORDER BY commonCount DESC")
    List<Object[]> findSimilarUsers(
            @Param("userId") UUID userId,
            @Param("minCommonRecommendations") Long minCommonRecommendations);

    /**
     * Finds tracks that were frequently recommended together.
     */
    @Query("SELECT t1, t2, COUNT(*) as cooccurrence " +
           "FROM RecommendationHistory rh " +
           "JOIN rh.trackIds t1 " +
           "JOIN rh.trackIds t2 " +
           "WHERE rh.userId = :userId AND t1 < t2 " +
           "GROUP BY t1, t2 " +
           "HAVING cooccurrence >= :minCooccurrence " +
           "ORDER BY cooccurrence DESC")
    List<Object[]> findFrequentlyRecommendedTogether(
            @Param("userId") UUID userId,
            @Param("minCooccurrence") Long minCooccurrence);

    /**
     * Gets recommendation performance by time of day.
     */
    @Query("SELECT EXTRACT(HOUR FROM rh.createdAt) as hour, " +
           "AVG(rh.averageRating) as avgRating, " +
           "AVG(rh.acceptanceRate) as avgAcceptance " +
           "FROM RecommendationHistory rh " +
           "WHERE rh.userId = :userId " +
           "GROUP BY EXTRACT(HOUR FROM rh.createdAt) " +
           "ORDER BY hour")
    List<Object[]> getRecommendationPerformanceByHour(@Param("userId") UUID userId);

    /**
     * Finds recommendations that led to high productivity sessions.
     */
    @Query("SELECT rh FROM RecommendationHistory rh WHERE rh.userId = :userId " +
           "AND rh.productivityScore >= :minProductivityScore " +
           "ORDER BY rh.productivityScore DESC")
    List<RecommendationHistory> findHighProductivityRecommendations(
            @Param("userId") UUID userId, 
            @Param("minProductivityScore") Double minProductivityScore);

    /**
     * Gets the most effective task-genre combinations for a user.
     */
    @Query("SELECT rh.taskType, genre, AVG(rh.averageRating) as avgRating " +
           "FROM RecommendationHistory rh " +
           "JOIN rh.genreDistribution genre " +
           "WHERE rh.userId = :userId " +
           "GROUP BY rh.taskType, genre " +
           "HAVING COUNT(rh) >= :minSamples " +
           "ORDER BY avgRating DESC")
    List<Object[]> getEffectiveTaskGenreCombinations(
            @Param("userId") UUID userId,
            @Param("minSamples") Long minSamples);

    /**
     * Counts total recommendations for a user.
     */
    Long countByUserId(UUID userId);

    /**
     * Counts recommendations in a date range.
     */
    @Query("SELECT COUNT(rh) FROM RecommendationHistory rh " +
           "WHERE rh.userId = :userId " +
           "AND rh.createdAt BETWEEN :start AND :end")
    Long countByUserIdAndDateRange(
            @Param("userId") UUID userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Deletes old recommendation history.
     */
    @Query("DELETE FROM RecommendationHistory rh WHERE rh.createdAt < :cutoffDate")
    void deleteOldRecommendations(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Finds recommendations with low acceptance rates for analysis.
     */
    @Query("SELECT rh FROM RecommendationHistory rh WHERE rh.userId = :userId " +
           "AND rh.acceptanceRate < :maxAcceptanceRate " +
           "ORDER BY rh.acceptanceRate ASC, rh.createdAt DESC")
    List<RecommendationHistory> findLowAcceptanceRecommendations(
            @Param("userId") UUID userId, 
            @Param("maxAcceptanceRate") Double maxAcceptanceRate);

    /**
     * Gets aggregated listening patterns for a user.
     */
    @Query("SELECT rh.taskType, rh.mood, COUNT(rh) as frequency, " +
           "AVG(rh.averageRating) as avgRating " +
           "FROM RecommendationHistory rh " +
           "WHERE rh.userId = :userId " +
           "GROUP BY rh.taskType, rh.mood " +
           "ORDER BY frequency DESC")
    List<Object[]> getListeningPatterns(@Param("userId") UUID userId);

    /**
     * Finds recommendations that need feedback analysis.
     */
    @Query("SELECT rh FROM RecommendationHistory rh WHERE rh.userId = :userId " +
           "AND rh.feedbackCount < rh.totalTracks * :minFeedbackRatio " +
           "ORDER BY rh.createdAt DESC")
    List<RecommendationHistory> findRecommendationsNeedingFeedback(
            @Param("userId") UUID userId,
            @Param("minFeedbackRatio") Double minFeedbackRatio);

    /**
     * Gets diversity trends over time.
     */
    @Query("SELECT DATE(rh.createdAt) as date, AVG(rh.diversityScore) as avgDiversity " +
           "FROM RecommendationHistory rh " +
           "WHERE rh.userId = :userId " +
           "AND rh.createdAt >= :since " +
           "GROUP BY DATE(rh.createdAt) " +
           "ORDER BY date")
    List<Object[]> getDiversityTrends(
            @Param("userId") UUID userId,
            @Param("since") LocalDateTime since);

    /**
     * Finds the best performing algorithm version for a user.
     */
    @Query("SELECT rh.algorithmVersion, AVG(rh.averageRating) as avgRating, " +
           "COUNT(rh) as sampleSize " +
           "FROM RecommendationHistory rh " +
           "WHERE rh.userId = :userId " +
           "GROUP BY rh.algorithmVersion " +
           "HAVING sampleSize >= :minSamples " +
           "ORDER BY avgRating DESC")
    List<Object[]> getBestPerformingAlgorithm(
            @Param("userId") UUID userId,
            @Param("minSamples") Long minSamples);
}