package com.focushive.timer.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Response DTO for timer data export.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimerDataExportResponse {
    private String userId;
    private ExportFormat format;
    private byte[] data;
    private String fileName;
    private LocalDateTime exportedAt;
    private Long recordCount;
}