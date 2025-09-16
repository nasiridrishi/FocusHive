package com.focushive.timer.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductivityInsightsResponse {
    private String userId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Double overallProductivity;
    private Map<String, Double> productivityByCategory;
    private List<String> recommendations;
    private Map<Integer, Double> productivityByHour;
    private List<String> topDistractions;
}