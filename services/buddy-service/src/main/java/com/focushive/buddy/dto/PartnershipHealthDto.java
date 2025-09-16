package com.focushive.buddy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for partnership health metrics and analysis.
 * Contains detailed health assessment and intervention recommendations.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PartnershipHealthDto {

    private UUID partnershipId;
    private BigDecimal overallHealthScore;
    @JsonProperty("healthScore")  // Also expose as healthScore for backward compatibility
    private BigDecimal healthScore;
    private ZonedDateTime lastAssessmentAt;
    private String healthStatus; // "EXCELLENT", "GOOD", "FAIR", "POOR", "CRITICAL"

    // Component scores
    private BigDecimal communicationScore;
    private BigDecimal engagementScore;
    private BigDecimal goalAlignmentScore;
    private BigDecimal consistencyScore;
    private BigDecimal responsiveScore;

    // Metrics
    private HealthMetricsDto metrics;
    private List<String> interventionSuggestions;
    private List<String> positiveIndicators;
    private List<String> concernIndicators;

    // Trends
    private String healthTrend; // "IMPROVING", "STABLE", "DECLINING"
    private BigDecimal trendScore; // -1.0 to 1.0, negative = declining

    /**
     * Nested class for detailed health metrics
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthMetricsDto {
        private Integer totalCheckinsLast7Days;
        private Integer totalCheckinsLast30Days;
        private Integer mutualGoalsCount;
        private Integer completedGoalsCount;
        private Long daysSinceLastInteraction;
        private BigDecimal averageResponseTimeHours;
        private Integer totalMessagesExchanged;
        private BigDecimal goalCompletionRate;
        private Integer missedCheckinsCount;
        private Integer consecutiveActivedays;
    }

    /**
     * Checks if the partnership health is critical
     */
    public boolean isCritical() {
        return "CRITICAL".equals(healthStatus) ||
               (overallHealthScore != null && overallHealthScore.compareTo(BigDecimal.valueOf(0.3)) < 0);
    }

    /**
     * Checks if interventions are recommended
     */
    public boolean needsIntervention() {
        return interventionSuggestions != null && !interventionSuggestions.isEmpty();
    }
}