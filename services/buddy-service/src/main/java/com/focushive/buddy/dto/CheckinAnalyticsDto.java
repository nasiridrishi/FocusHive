package com.focushive.buddy.dto;

import com.focushive.buddy.constant.MoodType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for check-in analytics data
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CheckinAnalyticsDto {
    private Integer totalCheckins;
    private Integer dailyCheckins;
    private Integer weeklyCheckins;
    private Double averageProductivity;
    private Double averageMood;  // Added for analytics
    private Map<MoodType, Integer> moodDistribution;
    private Double completionRate;
    private String mostProductiveDay;
    private String recommendations;
    private Map<String, Object> trends;  // Trend analysis data

    public CheckinAnalyticsDto(Integer totalCheckins, Integer dailyCheckins,
                             Double averageProductivity, Double completionRate) {
        this.totalCheckins = totalCheckins;
        this.dailyCheckins = dailyCheckins;
        this.averageProductivity = averageProductivity;
        this.completionRate = completionRate;
    }
}