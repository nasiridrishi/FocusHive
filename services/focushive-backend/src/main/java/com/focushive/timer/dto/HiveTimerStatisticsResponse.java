package com.focushive.timer.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for hive timer statistics response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HiveTimerStatisticsResponse {
    private String hiveId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long totalSessions;
    private Long activeSessions;
    private Long totalMembers;
    private Long totalFocusMinutes;
    private Double averageProductivityScore;
    private Map<String, Long> sessionsByMember;
    private Map<String, Double> productivityByMember;
    private List<TopPerformer> topPerformers;
    private Map<Integer, Long> sessionsByHour;
    private Double collaborationScore;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopPerformer {
        private String userId;
        private String username;
        private Long totalMinutes;
        private Double averageProductivity;
        private Long sessionsCount;
    }
}