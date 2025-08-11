package com.focushive.buddy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuddyMatchDTO {
    private Long userId;
    private String username;
    private String avatar;
    private String bio;
    private Double matchScore;
    private List<String> commonFocusAreas;
    private Integer timezoneOverlapHours;
    private String communicationStyle;
    private Map<String, Object> matchReasons;
    private Integer activeBuddyCount;
    private Integer completedGoalsCount;
    private Double averageSessionRating;
}