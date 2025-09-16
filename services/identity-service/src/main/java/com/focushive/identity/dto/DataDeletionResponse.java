package com.focushive.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for account deletion request containing request ID,
 * scheduled deletion date, and cancellation deadline.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response for account deletion request")
public class DataDeletionResponse {

    /**
     * Unique identifier for the deletion request.
     */
    @Schema(description = "Unique identifier for the deletion request")
    private UUID requestId;

    /**
     * When the account is scheduled to be deleted.
     */
    @Schema(description = "Scheduled deletion timestamp (ISO 8601 format)")
    private Instant scheduledDeletionDate;

    /**
     * Deadline for cancelling the deletion request.
     */
    @Schema(description = "Cancellation deadline timestamp (ISO 8601 format)")
    private Instant cancellationDeadline;

    /**
     * Current status of the deletion request.
     */
    @Schema(description = "Current status of the deletion request")
    private DeletionStatus status;

    /**
     * Grace period in days before actual deletion.
     */
    @Schema(description = "Grace period in days before deletion", example = "30")
    private Integer gracePeriodDays;

    /**
     * Instructions for cancelling the deletion if needed.
     */
    @Schema(description = "Instructions for cancelling the deletion")
    private String cancellationInstructions;

    /**
     * Contact information for support during the deletion process.
     */
    @Schema(description = "Support contact information")
    private String supportContact;

    /**
     * Deletion status enumeration.
     */
    public enum DeletionStatus {
        REQUESTED,      // Deletion has been requested
        SCHEDULED,      // Deletion is scheduled
        IN_PROGRESS,    // Deletion is currently happening
        COMPLETED,      // Deletion has been completed
        CANCELLED       // Deletion request was cancelled
    }
}