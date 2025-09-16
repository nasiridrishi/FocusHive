package com.focushive.buddy.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * DTO for partnership dissolution requests.
 * Used when initiating the end of a partnership with proper workflow.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DissolutionRequestDto {

    @NotNull(message = "Partnership ID is required")
    private UUID partnershipId;

    @NotNull(message = "Initiator ID is required")
    private String initiatorId;

    @NotBlank(message = "Reason is required")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;

    @NotNull(message = "Dissolution type is required")
    private DissolutionType dissolutionType;

    /**
     * Whether this requires partner consent before completion
     */
    @Builder.Default
    private Boolean requiresConsent = true;

    /**
     * Immediate dissolution bypasses consent period
     */
    @Builder.Default
    private Boolean isImmediate = false;

    /**
     * Optional feedback about the partnership
     */
    @Size(max = 1000, message = "Feedback must not exceed 1000 characters")
    private String partnerFeedback;

    /**
     * Reason category for analytics
     */
    private String reasonCategory; // "GOAL_MISMATCH", "TIME_CONFLICT", "COMMUNICATION_ISSUES", etc.

    /**
     * Proposed end date (for graceful dissolution)
     */
    private ZonedDateTime proposedEndDate;

    /**
     * Types of partnership dissolution
     */
    public enum DissolutionType {
        MUTUAL("Both parties agree to end the partnership"),
        UNILATERAL("One party wants to end the partnership"),
        TIMEOUT("Partnership ended due to inactivity"),
        VIOLATION("Partnership ended due to policy violation"),
        COMPLETION("Partnership completed successfully");

        private final String description;

        DissolutionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        /**
         * Checks if this dissolution type requires partner consent
         */
        public boolean requiresConsent() {
            return this == MUTUAL;
        }

        /**
         * Checks if this is an immediate dissolution type
         */
        public boolean isImmediate() {
            return this == VIOLATION || this == TIMEOUT;
        }
    }
}