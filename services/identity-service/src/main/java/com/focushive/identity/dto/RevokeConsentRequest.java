package com.focushive.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request DTO for revoking consent with consent type.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to revoke consent with specific type")
public class RevokeConsentRequest {

    /**
     * Specific consent ID to revoke (optional - alternative to consent type).
     */
    @Schema(description = "Specific consent ID to revoke (alternative to consent type)")
    private UUID consentId;

    /**
     * Type of consent being revoked (optional - alternative to consent ID).
     */
    @Schema(description = "Type of consent being revoked (alternative to consent ID)")
    private ConsentType consentType;

    /**
     * Reason for revoking the consent.
     */
    @Size(max = 1000, message = "Reason must not exceed 1000 characters")
    @Schema(description = "Reason for revoking consent", example = "No longer wish to receive marketing emails")
    private String reason;

    /**
     * Service or application the consent revocation applies to.
     */
    @Size(max = 100, message = "Service ID must not exceed 100 characters")
    @Schema(description = "Service the consent revocation applies to", example = "focushive-backend")
    private String serviceId;

    /**
     * Scope to revoke (optional - for partial revocation).
     */
    @Size(max = 1000, message = "Scope must not exceed 1000 characters")
    @Schema(description = "Specific scope to revoke (for partial revocation)", example = "analytics.track")
    private String scope;

    /**
     * Whether to revoke all related consents.
     */
    @Builder.Default
    @Schema(description = "Whether to revoke all related consents", example = "false")
    private Boolean revokeAll = false;

    /**
     * User confirmation of the revocation action.
     */
    @NotNull(message = "Revocation confirmation is required")
    @Schema(description = "User confirmation of revocation", example = "true")
    private Boolean confirmRevocation;

    /**
     * Whether to delete associated data as well.
     */
    @Builder.Default
    @Schema(description = "Whether to delete associated data", example = "false")
    private Boolean deleteAssociatedData = false;

    /**
     * Context in which the revocation was requested.
     */
    @Size(max = 200, message = "Context must not exceed 200 characters")
    @Schema(description = "Context of revocation request", example = "Privacy settings update")
    private String context;

    /**
     * Additional metadata about the revocation.
     */
    @Size(max = 1000, message = "Metadata must not exceed 1000 characters")
    @Schema(description = "Additional metadata about the revocation")
    private String metadata;

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
     * Validates that either consentId or consentType is provided.
     */
    public boolean isValid() {
        return consentId != null || consentType != null;
    }
}