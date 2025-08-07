package com.focushive.music.repository;

import com.focushive.music.dto.RecommendationFeedbackDTO;
import com.focushive.music.model.RecommendationFeedback;
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
 * Repository interface for managing user feedback on music recommendations.
 * Provides methods for storing, retrieving, and analyzing feedback data.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Repository
public interface RecommendationFeedbackRepository extends JpaRepository<RecommendationFeedback, UUID> {

    /**
     * Finds all feedback for a specific user.
     */
    Page<RecommendationFeedback> findByUserIdOrderByFeedbackAtDesc(UUID userId, Pageable pageable);

    /**
     * Finds feedback for a specific track by a user.
     */
    List<RecommendationFeedback> findByUserIdAndTrackIdOrderByFeedbackAtDesc(UUID userId, String trackId);

    /**
     * Finds the latest feedback for a specific recommendation.
     */
    Optional<RecommendationFeedback> findTopByRecommendationIdOrderByFeedbackAtDesc(UUID recommendationId);

    /**
     * Finds feedback by type.
     */
    List<RecommendationFeedback> findByUserIdAndFeedbackType(UUID userId, RecommendationFeedbackDTO.FeedbackType feedbackType);

    /**
     * Finds positive feedback for a user.
     */
    @Query("SELECT rf FROM RecommendationFeedback rf WHERE rf.userId = :userId " +
           "AND (rf.liked = true OR rf.overallRating >= 4 OR " +
           "rf.interactionType IN ('ADDED_TO_PLAYLIST', 'SAVED', 'SHARED')) " +
           "ORDER BY rf.feedbackAt DESC")
    List<RecommendationFeedback> findPositiveFeedback(@Param("userId") UUID userId);

    /**
     * Finds negative feedback for analysis.
     */
    @Query("SELECT rf FROM RecommendationFeedback rf WHERE rf.userId = :userId " +
           "AND (rf.liked = false OR rf.overallRating <= 2 OR " +
           "rf.interactionType IN ('SKIPPED_IMMEDIATELY', 'BLOCKED', 'REMOVED')) " +
           "ORDER BY rf.feedbackAt DESC")
    List<RecommendationFeedback> findNegativeFeedback(@Param("userId") UUID userId);

    /**
     * Gets feedback statistics for a track.
     */
    @Query("SELECT COUNT(rf), AVG(CAST(rf.overallRating AS double)), " +
           "SUM(CASE WHEN rf.liked = true THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN rf.liked = false THEN 1 ELSE 0 END) " +
           "FROM RecommendationFeedback rf WHERE rf.trackId = :trackId")
    Object[] getTrackFeedbackStats(@Param("trackId") String trackId);

    /**
     * Gets user feedback statistics.
     */
    @Query("SELECT COUNT(rf), AVG(CAST(rf.overallRating AS double)), " +
           "AVG(CAST(rf.productivityImpact AS double)), " +
           "AVG(CAST(rf.focusEnhancement AS double)) " +
           "FROM RecommendationFeedback rf WHERE rf.userId = :userId")
    Object[] getUserFeedbackStats(@Param("userId") UUID userId);

    /**
     * Finds feedback for tracks in a specific genre.
     */
    @Query("SELECT rf FROM RecommendationFeedback rf " +
           "WHERE rf.userId = :userId AND :genre IN rf.additionalData['genres'] " +
           "ORDER BY rf.feedbackAt DESC")
    List<RecommendationFeedback> findFeedbackForGenre(
            @Param("userId") UUID userId, 
            @Param("genre") String genre);

    /**
     * Finds feedback based on productivity impact.
     */
    @Query("SELECT rf FROM RecommendationFeedback rf WHERE rf.userId = :userId " +
           "AND rf.productivityImpact >= :minProductivityImpact " +
           "ORDER BY rf.productivityImpact DESC")
    List<RecommendationFeedback> findHighProductivityFeedback(
            @Param("userId") UUID userId, 
            @Param("minProductivityImpact") Integer minProductivityImpact);

    /**
     * Gets feedback trends over time.
     */
    @Query("SELECT DATE(rf.feedbackAt) as date, AVG(CAST(rf.overallRating AS double)) as avgRating, " +
           "COUNT(rf) as feedbackCount " +
           "FROM RecommendationFeedback rf " +
           "WHERE rf.userId = :userId AND rf.feedbackAt >= :since " +
           "GROUP BY DATE(rf.feedbackAt) ORDER BY date")
    List<Object[]> getFeedbackTrends(
            @Param("userId") UUID userId,
            @Param("since") LocalDateTime since);

    /**
     * Finds most liked artists based on feedback.
     */
    @Query("SELECT rf.additionalData['artist'] as artist, " +
           "AVG(CAST(rf.overallRating AS double)) as avgRating, " +
           "COUNT(rf) as feedbackCount " +
           "FROM RecommendationFeedback rf " +
           "WHERE rf.userId = :userId AND rf.liked = true " +
           "GROUP BY rf.additionalData['artist'] " +
           "HAVING feedbackCount >= :minFeedbackCount " +
           "ORDER BY avgRating DESC, feedbackCount DESC")
    List<Object[]> getMostLikedArtists(
            @Param("userId") UUID userId,
            @Param("minFeedbackCount") Long minFeedbackCount);

    /**
     * Finds most disliked genres for filtering.
     */
    @Query("SELECT rf.additionalData['genres'] as genre, " +
           "COUNT(rf) as negativeCount " +
           "FROM RecommendationFeedback rf " +
           "WHERE rf.userId = :userId AND rf.liked = false " +
           "GROUP BY rf.additionalData['genres'] " +
           "ORDER BY negativeCount DESC")
    List<Object[]> getMostDislikedGenres(@Param("userId") UUID userId);

    /**
     * Finds feedback patterns by task type.
     */
    @Query("SELECT rf.context.currentTaskType, " +
           "AVG(CAST(rf.taskSuitability AS double)) as avgSuitability, " +
           "AVG(CAST(rf.focusEnhancement AS double)) as avgFocus, " +
           "COUNT(rf) as sampleSize " +
           "FROM RecommendationFeedback rf " +
           "WHERE rf.userId = :userId AND rf.context.currentTaskType IS NOT NULL " +
           "GROUP BY rf.context.currentTaskType " +
           "ORDER BY avgSuitability DESC")
    List<Object[]> getFeedbackPatternsByTaskType(@Param("userId") UUID userId);

    /**
     * Finds feedback patterns by mood.
     */
    @Query("SELECT rf.context.currentMood, " +
           "AVG(CAST(rf.moodAppropriateness AS double)) as avgMoodMatch, " +
           "AVG(CAST(rf.overallRating AS double)) as avgRating, " +
           "COUNT(rf) as sampleSize " +
           "FROM RecommendationFeedback rf " +
           "WHERE rf.userId = :userId AND rf.context.currentMood IS NOT NULL " +
           "GROUP BY rf.context.currentMood " +
           "ORDER BY avgMoodMatch DESC")
    List<Object[]> getFeedbackPatternsByMood(@Param("userId") UUID userId);

    /**
     * Finds tracks with consistently positive feedback.
     */
    @Query("SELECT rf.trackId, AVG(CAST(rf.overallRating AS double)) as avgRating, " +
           "COUNT(rf) as feedbackCount " +
           "FROM RecommendationFeedback rf " +
           "WHERE rf.overallRating >= 4 " +
           "GROUP BY rf.trackId " +
           "HAVING feedbackCount >= :minFeedbackCount " +
           "ORDER BY avgRating DESC, feedbackCount DESC")
    List<Object[]> getConsistentlyPositiveTracks(@Param("minFeedbackCount") Long minFeedbackCount);

    /**
     * Finds skip reasons for improving recommendations.
     */
    @Query("SELECT rf.skipReason, COUNT(rf) as frequency " +
           "FROM RecommendationFeedback rf " +
           "WHERE rf.userId = :userId AND rf.skipReason IS NOT NULL " +
           "GROUP BY rf.skipReason ORDER BY frequency DESC")
    List<Object[]> getSkipReasonFrequency(@Param("userId") UUID userId);

    /**
     * Finds feedback for tracks with specific audio features.
     */
    @Query("SELECT rf FROM RecommendationFeedback rf " +
           "WHERE rf.userId = :userId " +
           "AND CAST(rf.additionalData['energy'] AS double) BETWEEN :minEnergy AND :maxEnergy " +
           "AND CAST(rf.additionalData['valence'] AS double) BETWEEN :minValence AND :maxValence " +
           "ORDER BY rf.feedbackAt DESC")
    List<RecommendationFeedback> findFeedbackByAudioFeatures(
            @Param("userId") UUID userId,
            @Param("minEnergy") Double minEnergy,
            @Param("maxEnergy") Double maxEnergy,
            @Param("minValence") Double minValence,
            @Param("maxValence") Double maxValence);

    /**
     * Gets listening behavior patterns.
     */
    @Query("SELECT rf.listeningBehavior.completionPercentage, " +
           "rf.listeningBehavior.repeatCount, " +
           "rf.overallRating " +
           "FROM RecommendationFeedback rf " +
           "WHERE rf.userId = :userId AND rf.listeningBehavior IS NOT NULL")
    List<Object[]> getListeningBehaviorPatterns(@Param("userId") UUID userId);

    /**
     * Finds collaborative session feedback.
     */
    @Query("SELECT rf FROM RecommendationFeedback rf " +
           "WHERE rf.context.collaboratorCount > 1 " +
           "ORDER BY rf.feedbackAt DESC")
    List<RecommendationFeedback> findCollaborativeFeedback();

    /**
     * Gets feedback influence weights for machine learning.
     */
    @Query("SELECT rf.trackId, rf.overallRating, rf.confidenceLevel, " +
           "rf.feedbackAt, rf.influenceFuture " +
           "FROM RecommendationFeedback rf " +
           "WHERE rf.userId = :userId AND rf.influenceFuture = true " +
           "ORDER BY rf.feedbackAt DESC")
    List<Object[]> getFeedbackInfluenceData(@Param("userId") UUID userId);

    /**
     * Finds recent feedback needing response/analysis.
     */
    @Query("SELECT rf FROM RecommendationFeedback rf " +
           "WHERE rf.feedbackAt >= :since " +
           "AND (rf.overallRating <= 2 OR rf.feedbackText IS NOT NULL) " +
           "ORDER BY rf.feedbackAt DESC")
    List<RecommendationFeedback> findRecentNegativeOrDetailed(@Param("since") LocalDateTime since);

    /**
     * Counts feedback by interaction type.
     */
    @Query("SELECT rf.interactionType, COUNT(rf) " +
           "FROM RecommendationFeedback rf " +
           "WHERE rf.userId = :userId " +
           "GROUP BY rf.interactionType " +
           "ORDER BY COUNT(rf) DESC")
    List<Object[]> getInteractionTypeFrequency(@Param("userId") UUID userId);

    /**
     * Finds users with similar feedback patterns.
     */
    @Query("SELECT rf2.userId, COUNT(DISTINCT rf1.trackId) as commonTracks " +
           "FROM RecommendationFeedback rf1, RecommendationFeedback rf2 " +
           "WHERE rf1.userId = :userId AND rf1.userId != rf2.userId " +
           "AND rf1.trackId = rf2.trackId " +
           "AND rf1.liked = rf2.liked " +
           "GROUP BY rf2.userId " +
           "HAVING commonTracks >= :minCommonTracks " +
           "ORDER BY commonTracks DESC")
    List<Object[]> findUsersWithSimilarTaste(
            @Param("userId") UUID userId,
            @Param("minCommonTracks") Long minCommonTracks);

    /**
     * Gets average ratings by time of day.
     */
    @Query("SELECT EXTRACT(HOUR FROM rf.feedbackAt) as hour, " +
           "AVG(CAST(rf.overallRating AS double)) as avgRating, " +
           "COUNT(rf) as sampleSize " +
           "FROM RecommendationFeedback rf " +
           "WHERE rf.userId = :userId " +
           "GROUP BY EXTRACT(HOUR FROM rf.feedbackAt) " +
           "HAVING sampleSize >= :minSamples " +
           "ORDER BY hour")
    List<Object[]> getRatingsByTimeOfDay(
            @Param("userId") UUID userId,
            @Param("minSamples") Long minSamples);

    /**
     * Finds feedback requiring attention (very negative or detailed).
     */
    @Query("SELECT rf FROM RecommendationFeedback rf " +
           "WHERE (rf.overallRating = 1 OR " +
           "rf.feedbackText IS NOT NULL OR " +
           "rf.negativeReason IS NOT NULL) " +
           "ORDER BY rf.feedbackAt DESC")
    List<RecommendationFeedback> findFeedbackRequiringAttention();

    /**
     * Deletes old feedback data.
     */
    @Query("DELETE FROM RecommendationFeedback rf WHERE rf.feedbackAt < :cutoffDate")
    void deleteOldFeedback(@Param("cutoffDate") LocalDateTime cutoffDate);
}