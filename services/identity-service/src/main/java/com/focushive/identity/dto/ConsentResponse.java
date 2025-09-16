package com.focushive.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for consent grant/revoke operations containing
 * consent ID, type, status, and timestamp.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response for consent grant/revoke operations")
public class ConsentResponse {

    /**
     * Unique identifier for the consent record.
     */
    @Schema(description = "Unique identifier for the consent record")
    private UUID consentId;

    /**
     * Type of consent that was granted or revoked.
     */
    @Schema(description = "Type of consent")
    private ConsentType consentType;

    /**
     * Current status of the consent.
     */
    @Schema(description = "Current status of the consent")
    private ConsentStatus status;

    /**
     * Timestamp when the consent was granted or revoked.
     */
    @Schema(description = "Timestamp of the consent action")
    private Instant timestamp;

    /**
     * Scope or specific permissions covered by this consent.
     */
    @Schema(description = "Scope of the consent", example = "profile.read,profile.write")
    private String scope;

    /**
     * Purpose for which consent was granted.
     */
    @Schema(description = "Purpose of the consent", example = "User profile management")
    private String purpose;

    /**
     * Expiration date of the consent (if applicable).
     */
    @Schema(description = "Expiration timestamp of the consent")
    private Instant expiresAt;

    /**
     * Whether the consent can be revoked by the user.
     */
    @Builder.Default
    @Schema(description = "Whether the consent can be revoked", example = "true")
    private Boolean revocable = true;

    /**
     * Legal basis for processing under this consent.
     */
    @Schema(description = "Legal basis for processing")
    private LegalBasis legalBasis;

    /**
     * Additional metadata about the consent.
     */
    @Schema(description = "Additional metadata about the consent")
    private String metadata;

    /**
     * Service or application the consent applies to.
     */
    @Schema(description = "Service the consent applies to", example = "focushive-backend")
    private String serviceId;

    /**
     * Version of the consent terms that were agreed to.
     */
    @Schema(description = "Version of consent terms", example = "1.0")
    private String termsVersion;

    /**
     * Type of consent.
     */
    public enum ConsentType {
        DATA_PROCESSING,      // General data processing
        MARKETING,            // Marketing communications
        ANALYTICS,            // Analytics and tracking
        THIRD_PARTY_SHARING,  // Sharing with third parties
        COOKIES,              // Cookie usage
        PERSONALIZATION,      // Personalized content
        RESEARCH,             // Research participation
        NOTIFICATIONS,        // Push notifications
        GEOLOCATION,          // Location tracking
        CAMERA_AUDIO,         // Camera and microphone access
        PROFILE_VISIBILITY,   // Profile visibility settings
        DATA_EXPORT,          // Data export permissions
        AUTOMATIC_UPDATES     // Automatic software updates
    }

    /**
     * Status of the consent.
     */
    public enum ConsentStatus {
        GRANTED,              // Consent has been granted
        REVOKED,              // Consent has been revoked
        EXPIRED,              // Consent has expired
        PENDING,              // Consent is pending user action
        WITHDRAWN,            // Consent was withdrawn by user
        SUPERSEDED            // Consent was replaced by newer version
    }

    /**
     * Legal basis for data processing.
     */
    public enum LegalBasis {
        CONSENT,              // User consent (GDPR Article 6(1)(a))
        CONTRACT,             // Contractual necessity (GDPR Article 6(1)(b))
        LEGAL_OBLIGATION,     // Legal obligation (GDPR Article 6(1)(c))
        VITAL_INTEREST,       // Vital interest (GDPR Article 6(1)(d))
        PUBLIC_TASK,          // Public task (GDPR Article 6(1)(e))
        LEGITIMATE_INTEREST   // Legitimate interest (GDPR Article 6(1)(f))
    }
}