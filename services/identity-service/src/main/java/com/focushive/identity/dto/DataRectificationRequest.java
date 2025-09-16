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
 * Request DTO for correcting/rectifying user data with field and new value.
 * This supports GDPR "Right to Rectification" requirements.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to correct/rectify user data")
public class DataRectificationRequest {

    /**
     * Field name that needs to be corrected.
     */
    @NotBlank(message = "Field name is required")
    @Size(max = 100, message = "Field name must not exceed 100 characters")
    @Schema(description = "Name of the field to be corrected", example = "email")
    private String fieldName;

    /**
     * Current value of the field (for verification).
     */
    @Size(max = 1000, message = "Current value must not exceed 1000 characters")
    @Schema(description = "Current value of the field", example = "old.email@example.com")
    private String currentValue;

    /**
     * New corrected value for the field.
     */
    @NotBlank(message = "New value is required")
    @Size(max = 1000, message = "New value must not exceed 1000 characters")
    @Schema(description = "New corrected value", example = "new.email@example.com")
    private String newValue;

    /**
     * Reason for the data correction.
     */
    @NotBlank(message = "Reason for correction is required")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    @Schema(description = "Reason for the data correction", example = "Email address was entered incorrectly")
    private String reason;

    /**
     * Category of data being corrected.
     */
    @NotNull(message = "Data category is required")
    @Schema(description = "Category of data being corrected")
    private DataCategory dataCategory;

    /**
     * Supporting evidence or documentation for the correction.
     */
    @Size(max = 2000, message = "Evidence must not exceed 2000 characters")
    @Schema(description = "Supporting evidence for the correction")
    private String evidence;

    /**
     * Whether this correction requires verification.
     */
    @Builder.Default
    @Schema(description = "Whether verification is required", example = "true")
    private Boolean requiresVerification = true;

    /**
     * Urgency level of the correction.
     */
    @Builder.Default
    @Schema(description = "Urgency level of the correction")
    private UrgencyLevel urgency = UrgencyLevel.NORMAL;

    /**
     * User's password for verification (for sensitive changes).
     */
    @Schema(description = "User's password for verification (required for sensitive data)")
    private String passwordVerification;

    /**
     * Additional context or notes about the correction.
     */
    @Size(max = 1000, message = "Additional context must not exceed 1000 characters")
    @Schema(description = "Additional context about the correction")
    private String additionalContext;

    /**
     * Preferred method for verification (if required).
     */
    @Schema(description = "Preferred verification method")
    private VerificationMethod preferredVerificationMethod;

    /**
     * Category of data being corrected.
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
     * Urgency level for the correction.
     */
    public enum UrgencyLevel {
        LOW,                  // Can be processed within a week
        NORMAL,               // Standard processing time (24-48 hours)
        HIGH,                 // Needs attention within hours
        CRITICAL              // Immediate attention required
    }

    /**
     * Method for verifying the correction.
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
     * Validates if password verification is required for this field.
     */
    public boolean requiresPasswordVerification() {
        return dataCategory == DataCategory.AUTHENTICATION_DATA ||
               dataCategory == DataCategory.BILLING_INFO ||
               dataCategory == DataCategory.CONTACT_INFO;
    }

    /**
     * Validates if the field name is allowed to be modified.
     */
    public boolean isFieldModifiable() {
        // Some fields like user ID, creation date should not be modifiable
        String[] nonModifiableFields = {"id", "userId", "createdAt", "createdBy"};
        String lowerFieldName = fieldName.toLowerCase();

        for (String field : nonModifiableFields) {
            if (lowerFieldName.equals(field)) {
                return false;
            }
        }
        return true;
    }
}