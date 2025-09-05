package com.focushive.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data export response")
public class DataExportResponse {

    @Schema(description = "Export request ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID exportId;

    @Schema(description = "Export status", example = "processing", 
            allowableValues = {"requested", "processing", "completed", "failed", "expired"})
    private String status;

    @Schema(description = "Export progress percentage", example = "45")
    private Integer progress;

    @Schema(description = "Estimated completion time")
    private Instant estimatedCompletion;

    @Schema(description = "Download URL (available when completed)", example = "https://identity.focushive.com/api/v1/privacy/data/export/550e8400-e29b-41d4-a716-446655440000/download")
    private String downloadUrl;

    @Schema(description = "File size in bytes (when completed)", example = "1048576")
    private Long fileSizeBytes;

    @Schema(description = "Export expiry date")
    private Instant expiresAt;

    @Schema(description = "Request timestamp")
    private Instant requestedAt;

    @Schema(description = "Completion timestamp")
    private Instant completedAt;

    @Schema(description = "Error message (if failed)")
    private String errorMessage;
}