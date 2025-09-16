package com.focushive.timer.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimerDataImportResponse {
    private String userId;
    private Long sessionsImported;
    private Long templatesImported;
    private LocalDateTime importedAt;
    private boolean success;
    private String message;
}