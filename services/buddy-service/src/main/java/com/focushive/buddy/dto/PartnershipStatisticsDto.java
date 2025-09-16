package com.focushive.buddy.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for partnership statistics and analytics.
 * Contains aggregate data about partnerships for reporting and insights.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PartnershipStatisticsDto {

    private String userId; // Statistics for this user
    private ZonedDateTime generatedAt;
    private String periodDescription; // e.g., "Last 30 days", "All time"

    // Partnership counts
    private Long totalPartnerships;
    private Long activePartnerships;
    private Long completedPartnerships;
    private Long endedPartnerships;
    private Long pendingRequests;

    // Success metrics
    private BigDecimal averagePartnershipDuration; // in days
    private BigDecimal partnershipSuccessRate;
    private BigDecimal averageHealthScore;
    private BigDecimal averageCompatibilityScore;

    // Engagement metrics
    private Long totalCheckins;
    private Long totalGoalsCreated;
    private Long totalGoalsCompleted;
    private BigDecimal goalCompletionRate;
    private Long totalMessagesExchanged;

    // Performance indicators
    private BigDecimal consistencyScore;
    private BigDecimal responsiveScore;
    private Long averageResponseTimeHours;
    private Long missedCheckinsCount;

    // Trends
    private Map<String, BigDecimal> monthlyTrends; // Month -> score
    private String performanceTrend; // "IMPROVING", "STABLE", "DECLINING"
    private BigDecimal trendScore;

    // Rankings (optional, for gamification)
    private Integer globalRanking;
    private Integer localRanking; // within user's preference group
    private String rankingTier; // "BRONZE", "SILVER", "GOLD", "PLATINUM"

    // Recommendations
    private String primaryRecommendation;
    private String[] improvementAreas;
    private String[] strengths;

    /**
     * Calculates the partnership effectiveness score (0-100)
     */
    public BigDecimal calculateEffectivenessScore() {
        if (partnershipSuccessRate == null || averageHealthScore == null) {
            return BigDecimal.ZERO;
        }

        // Weighted average: 60% success rate, 40% health score
        return partnershipSuccessRate.multiply(BigDecimal.valueOf(0.6))
                .add(averageHealthScore.multiply(BigDecimal.valueOf(0.4)))
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Checks if the user is considered an experienced partner
     */
    public boolean isExperiencedPartner() {
        return totalPartnerships != null && totalPartnerships >= 3;
    }

    /**
     * Checks if the user has high performance metrics
     */
    public boolean isHighPerformer() {
        BigDecimal effectiveness = calculateEffectivenessScore();
        return effectiveness.compareTo(BigDecimal.valueOf(80)) >= 0;
    }
}