package com.focushive.buddy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuddyMatchScoreDTO {
    private Long user1Id;
    private Long user2Id;
    private Double totalScore;
    private Double focusAreaScore;
    private Double timezoneScore;
    private Double communicationScore;
    private Double availabilityScore;
    private Map<String, Object> scoreBreakdown;
    private String recommendation;
}