package com.focushive.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for data rectification request containing request ID and status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response for data rectification request")
public class DataRectificationResponse {

    /**
     * Unique identifier for the rectification request.
     */
    @Schema(description = "Unique identifier for the rectification request")
    private UUID requestId;

    /**
     * Current status of the rectification request.
     */
    @Schema(description = "Current status of the rectification request")
    private RectificationStatus status;

    /**
     * Field name that was requested to be corrected.
     */
    @Schema(description = "Field name that was requested to be corrected", example = "email")
    private String fieldName;

    /**
     * Category of data being corrected.
     */
    @Schema(description = "Category of data being corrected")
    private DataCategory dataCategory;

    /**
     * Timestamp when the request was submitted.
     */
    @Schema(description = "Timestamp when request was submitted")
    private Instant requestTimestamp;

    /**
     * Timestamp when the rectification was completed (if applicable).
     */
    @Schema(description = "Timestamp when rectification was completed")
    private Instant completedTimestamp;

    /**
     * Estimated completion time for the rectification.
     */
    @Schema(description = "Estimated completion timestamp")
    private Instant estimatedCompletionTime;

    /**
     * Verification method required (if any).
     */
    @Schema(description = "Verification method required")
    private VerificationMethod verificationMethod;

    /**
     * Instructions for completing verification (if required).
     */
    @Schema(description = "Instructions for verification")
    private String verificationInstructions;

    /**
     * Message or notes about the rectification request.
     */
    @Schema(description = "Message about the rectification request")
    private String message;

    /**
     * Reason for rejection (if applicable).
     */
    @Schema(description = "Reason for rejection (if applicable)")
    private String rejectionReason;

    /**
     * Next steps for the user (if any).
     */
    @Schema(description = "Next steps for the user")
    private String nextSteps;

    /**
     * Support contact information.
     */
    @Schema(description = "Support contact for questions")
    private String supportContact;

    /**
     * Reference number for tracking the request.
     */
    @Schema(description = "Reference number for tracking", example = "REQ-2025-001234")
    private String referenceNumber;

    /**
     * Whether the user can appeal this decision.
     */
    @Builder.Default
    @Schema(description = "Whether the user can appeal", example = "true")
    private Boolean appealable = true;

    /**
     * Deadline for appealing the decision (if rejected).
     */
    @Schema(description = "Deadline for appeal (if rejected)")
    private Instant appealDeadline;

    /**
     * Status of the rectification request.
     */
    public enum RectificationStatus {
        SUBMITTED,            // Request has been submitted
        UNDER_REVIEW,         // Request is being reviewed
        VERIFICATION_REQUIRED, // Additional verification needed
        APPROVED,             // Request has been approved
        IN_PROGRESS,          // Rectification is in progress
        COMPLETED,            // Rectification has been completed
        REJECTED,             // Request was rejected
        CANCELLED,            // Request was cancelled by user
        APPEAL_PENDING,       // Appeal is pending review
        EXPIRED               // Request expired without action
    }

    /**
     * Category of data being corrected (reused for consistency).
     */
    public enum DataCategory {
        PERSONAL_INFO,        // Name, date of birth, etc.
        CONTACT_INFO,         // Email, phone, address
        PROFILE_DATA,         // Profile information, preferences
        ACCOUNT_SETTINGS,     // Account configuration
        PERSONA_DATA,         // User persona information
        PRIVACY_SETTINGS,     // Privacy preferences
        AUTHENTICATION_DATA,  // Login credentials
        BILLING_INFO,         // Payment information
        METADATA             // Other metadata
    }

    /**
     * Method for verifying the correction (reused for consistency).
     */
    public enum VerificationMethod {
        EMAIL,                // Email verification
        SMS,                  // SMS verification
        PHONE_CALL,           // Phone call verification
        DOCUMENT_UPLOAD,      // Document verification
        MANUAL_REVIEW,        // Manual review by support
        NONE                  // No verification needed
    }

    /**
     * Checks if the rectification is completed.
     */
    public boolean isCompleted() {
        return status == RectificationStatus.COMPLETED;
    }

    /**
     * Checks if the rectification requires user action.
     */
    public boolean requiresUserAction() {
        return status == RectificationStatus.VERIFICATION_REQUIRED ||
               status == RectificationStatus.APPEAL_PENDING;
    }

    /**
     * Checks if the rectification can be cancelled.
     */
    public boolean isCancellable() {
        return status == RectificationStatus.SUBMITTED ||
               status == RectificationStatus.UNDER_REVIEW ||
               status == RectificationStatus.VERIFICATION_REQUIRED;
    }
}