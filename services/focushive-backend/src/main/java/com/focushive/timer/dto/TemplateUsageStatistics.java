package com.focushive.timer.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateUsageStatistics {
    private String templateId;
    private String templateName;
    private Integer usageCount;
    private Double averageProductivity;
    private Integer totalMinutes;
    private LocalDateTime lastUsed;
    private Integer uniqueUsers;
}