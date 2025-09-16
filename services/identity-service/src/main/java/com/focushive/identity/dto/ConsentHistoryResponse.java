package com.focushive.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for paginated consent history with grant/revoke records.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated consent history with grant/revoke records")
public class ConsentHistoryResponse {

    /**
     * List of consent history entries for the current page.
     */
    @Schema(description = "List of consent history entries")
    private List<ConsentHistoryEntry> entries;

    /**
     * Current page number (0-based).
     */
    @Schema(description = "Current page number", example = "0")
    private Integer page;

    /**
     * Number of entries per page.
     */
    @Schema(description = "Number of entries per page", example = "20")
    private Integer size;

    /**
     * Total number of consent history entries.
     */
    @Schema(description = "Total number of consent history entries", example = "45")
    private Long totalElements;

    /**
     * Total number of pages.
     */
    @Schema(description = "Total number of pages", example = "3")
    private Integer totalPages;

    /**
     * Whether this is the first page.
     */
    @Schema(description = "Whether this is the first page", example = "true")
    private Boolean first;

    /**
     * Whether this is the last page.
     */
    @Schema(description = "Whether this is the last page", example = "false")
    private Boolean last;

    /**
     * Summary of consent types by current status.
     */
    @Schema(description = "Summary of current consent status")
    private ConsentSummary summary;

    /**
     * Individual consent history entry.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Individual consent history entry")
    public static class ConsentHistoryEntry {

        /**
         * Unique identifier for the consent record.
         */
        @Schema(description = "Unique identifier for the consent record")
        private UUID consentId;

        /**
         * Type of consent.
         */
        @Schema(description = "Type of consent")
        private ConsentType consentType;

        /**
         * Action performed (grant, revoke, update, expire).
         */
        @Schema(description = "Action performed")
        private ConsentAction action;

        /**
         * Status after the action.
         */
        @Schema(description = "Status after the action")
        private ConsentStatus status;

        /**
         * Timestamp when the action occurred.
         */
        @Schema(description = "Timestamp of the action")
        private Instant timestamp;

        /**
         * Scope or permissions involved.
         */
        @Schema(description = "Scope of the consent", example = "profile.read,analytics.track")
        private String scope;

        /**
         * Purpose of the consent.
         */
        @Schema(description = "Purpose of the consent")
        private String purpose;

        /**
         * Service the consent applies to.
         */
        @Schema(description = "Service the consent applies to")
        private String serviceId;

        /**
         * Reason for the action (especially for revocations).
         */
        @Schema(description = "Reason for the action")
        private String reason;

        /**
         * Version of terms that were consented to.
         */
        @Schema(description = "Version of terms", example = "1.2")
        private String termsVersion;

        /**
         * Legal basis for the processing.
         */
        @Schema(description = "Legal basis for processing")
        private LegalBasis legalBasis;

        /**
         * IP address from which the action was performed.
         */
        @Schema(description = "IP address", example = "192.168.1.1")
        private String ipAddress;

        /**
         * User agent string from the request.
         */
        @Schema(description = "User agent string")
        private String userAgent;

        /**
         * Expiration date if applicable.
         */
        @Schema(description = "Expiration timestamp")
        private Instant expiresAt;

        /**
         * Additional metadata.
         */
        @Schema(description = "Additional metadata")
        private String metadata;
    }

    /**
     * Summary of consent status by type.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Summary of current consent status")
    public static class ConsentSummary {

        /**
         * Total number of consent records.
         */
        @Schema(description = "Total number of consent records", example = "12")
        private Integer totalConsents;

        /**
         * Number of currently active consents.
         */
        @Schema(description = "Number of active consents", example = "8")
        private Integer activeConsents;

        /**
         * Number of revoked consents.
         */
        @Schema(description = "Number of revoked consents", example = "3")
        private Integer revokedConsents;

        /**
         * Number of expired consents.
         */
        @Schema(description = "Number of expired consents", example = "1")
        private Integer expiredConsents;

        /**
         * Most recent consent action timestamp.
         */
        @Schema(description = "Most recent action timestamp")
        private Instant lastActionTimestamp;

        /**
         * Breakdown by consent type.
         */
        @Schema(description = "Breakdown by consent type")
        private List<ConsentTypeCount> typeBreakdown;
    }

    /**
     * Count of consents by type.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Count of consents by type")
    public static class ConsentTypeCount {

        /**
         * Type of consent.
         */
        @Schema(description = "Type of consent")
        private ConsentType consentType;

        /**
         * Number of active consents of this type.
         */
        @Schema(description = "Number of active consents", example = "2")
        private Integer activeCount;

        /**
         * Number of revoked consents of this type.
         */
        @Schema(description = "Number of revoked consents", example = "1")
        private Integer revokedCount;
    }

    /**
     * Actions that can be performed on consent.
     */
    public enum ConsentAction {
        GRANTED,              // Consent was granted
        REVOKED,              // Consent was revoked
        UPDATED,              // Consent was updated/modified
        EXPIRED,              // Consent expired automatically
        SUPERSEDED,           // Consent was replaced by newer version
        REINSTATED            // Previously revoked consent was granted again
    }

    /**
     * Type of consent (reused for consistency).
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
     * Status of consent (reused for consistency).
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
     * Legal basis for data processing (reused for consistency).
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