package com.focushive.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Request DTO for granting consent with consent type and scope.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to grant consent with specific type and scope")
public class GrantConsentRequest {

    /**
     * Type of consent being granted.
     */
    @NotNull(message = "Consent type is required")
    @Schema(description = "Type of consent being granted")
    private ConsentType consentType;

    /**
     * Scope or specific permissions being granted.
     */
    @Size(max = 1000, message = "Scope must not exceed 1000 characters")
    @Schema(description = "Scope of the consent", example = "profile.read,profile.write,analytics.track")
    private String scope;

    /**
     * Purpose for which consent is being granted.
     */
    @Size(max = 500, message = "Purpose must not exceed 500 characters")
    @Schema(description = "Purpose of the consent", example = "Personalized user experience")
    private String purpose;

    /**
     * Service or application the consent applies to.
     */
    @Size(max = 100, message = "Service ID must not exceed 100 characters")
    @Schema(description = "Service the consent applies to", example = "focushive-backend")
    private String serviceId;

    /**
     * Expiration date for the consent (optional).
     */
    @Schema(description = "Expiration timestamp for the consent (ISO 8601 format)")
    private Instant expiresAt;

    /**
     * Version of the terms being consented to.
     */
    @Size(max = 20, message = "Terms version must not exceed 20 characters")
    @Schema(description = "Version of terms being consented to", example = "1.2")
    private String termsVersion;

    /**
     * Legal basis for the data processing.
     */
    @Schema(description = "Legal basis for processing")
    private LegalBasis legalBasis;

    /**
     * Additional metadata about the consent.
     */
    @Size(max = 2000, message = "Metadata must not exceed 2000 characters")
    @Schema(description = "Additional metadata about the consent")
    private String metadata;

    /**
     * Whether the user explicitly confirmed understanding of the consent.
     */
    @Builder.Default
    @Schema(description = "Whether user confirmed understanding", example = "true")
    private Boolean confirmedUnderstanding = false;

    /**
     * Context in which the consent was requested.
     */
    @Size(max = 200, message = "Context must not exceed 200 characters")
    @Schema(description = "Context of consent request", example = "Initial registration")
    private String context;

    /**
     * Type of consent (reused from ConsentResponse for consistency).
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
     * Legal basis for data processing (reused from ConsentResponse for consistency).
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