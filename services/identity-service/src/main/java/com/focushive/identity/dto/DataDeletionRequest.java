package com.focushive.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for requesting account deletion with reason and confirmation.
 * This follows GDPR "Right to Erasure" requirements.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for account deletion with reason and confirmation")
public class DataDeletionRequest {

    /**
     * Reason for requesting account deletion.
     */
    @NotBlank(message = "Deletion reason is required")
    @Size(max = 1000, message = "Deletion reason must not exceed 1000 characters")
    @Schema(description = "Reason for requesting account deletion", example = "No longer using the service")
    private String reason;

    /**
     * Confirmation that the user understands the consequences of deletion.
     */
    @NotNull(message = "Deletion confirmation is required")
    @Schema(description = "Confirmation that user understands deletion consequences", example = "true")
    private Boolean confirmDeletion;

    /**
     * User's password for additional security verification.
     */
    @NotBlank(message = "Password confirmation is required for account deletion")
    @Schema(description = "User's current password for verification")
    private String password;

    /**
     * Optional feedback about the service.
     */
    @Size(max = 2000, message = "Feedback must not exceed 2000 characters")
    @Schema(description = "Optional feedback about the service")
    private String feedback;

    /**
     * Whether to keep anonymized analytics data.
     */
    @Builder.Default
    @Schema(description = "Whether to keep anonymized analytics data", example = "false")
    private Boolean keepAnonymizedData = false;

    /**
     * Preferred deletion date (if immediate deletion is not required).
     */
    @Schema(description = "Preferred deletion date (ISO 8601 format)", example = "2025-01-01")
    private String preferredDeletionDate;
}