package com.focushive.timer.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionRecommendationResponse {
    private String userId;
    private String recommendedDuration;
    private String recommendedType;
    private String recommendedTemplate;
    private List<String> suggestions;
    private String rationale;
}