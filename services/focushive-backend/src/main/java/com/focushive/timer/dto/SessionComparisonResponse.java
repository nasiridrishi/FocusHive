package com.focushive.timer.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionComparisonResponse {
    private List<String> sessionIds;
    private Map<String, Double> productivityScores;
    private Map<String, Integer> durations;
    private Map<String, Integer> tasksCompleted;
    private String bestPerformer;
    private Map<String, String> insights;
}