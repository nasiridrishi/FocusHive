package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for data export history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataExportHistoryResponse {

    private UUID userId;
    private List<DataExportRecord> exports;
    private int totalExports;
    private Instant lastExportDate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataExportRecord {
        private UUID exportId;
        private String status; // pending, processing, completed, expired, failed
        private String format; // json, csv, xml
        private Instant requestedAt;
        private Instant completedAt;
        private Instant expiresAt;
        private Long fileSizeBytes;
        private String downloadUrl;
        private boolean available;
    }
}