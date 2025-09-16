package com.focushive.timer.dto;

import lombok.*;

/**
 * DTO for updating session metrics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSessionMetricsRequest {
    private Integer tabSwitches;
    private Integer distractionMinutes;
    private Integer focusBreaks;
    private Integer notesCount;
    private Integer tasksCompleted;
    private Integer productivityScore;
}