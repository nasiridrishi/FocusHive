package com.focushive.timer.dto;

import lombok.*;

/**
 * Request DTO for timer data import.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimerDataImportRequest {
    private ExportFormat format;
    private byte[] data;
    private boolean overwriteExisting;
}