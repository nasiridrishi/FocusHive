package com.focushive.buddy.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO representing detailed compatibility breakdown between two users.
 * Contains overall score and individual component scores.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CompatibilityScoreDto {

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double overallScore;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double timezoneScore;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double interestScore;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double goalAlignmentScore;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double activityPatternScore;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double communicationStyleScore;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double personalityScore;

    private String explanation;

    /**
     * Get the breakdown of individual scores as a map.
     * This is used for API responses where a nested structure is expected.
     */
    public Map<String, Double> getBreakdown() {
        Map<String, Double> breakdown = new HashMap<>();
        breakdown.put("timezone", timezoneScore);
        breakdown.put("interests", interestScore);
        breakdown.put("goals", goalAlignmentScore);
        breakdown.put("activityPattern", activityPatternScore);
        breakdown.put("communicationStyle", communicationStyleScore);
        breakdown.put("personality", personalityScore);
        return breakdown;
    }
}