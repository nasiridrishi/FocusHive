package com.focushive.timer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for timer synchronization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimerSyncResponse {
    private String userId;
    private FocusSessionResponse activeSession;
    private List<FocusSessionResponse> recentSessions;
    private LocalDateTime lastSyncTime;
    private String syncToken;
    private Boolean hasConflicts;
    private List<SyncConflict> conflicts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncConflict {
        private String sessionId;
        private String conflictType;
        private String localValue;
        private String remoteValue;
        private LocalDateTime conflictTime;
    }
}