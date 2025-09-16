package com.focushive.timer.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AISessionInsightsResponse {
    private String sessionId;
    private Double productivityScore;
    private List<String> strengths;
    private List<String> improvements;
    private Map<String, String> patterns;
    private List<String> recommendations;
}