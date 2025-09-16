package com.focushive.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request DTO for objecting to data processing with type and reason.
 * This supports GDPR "Right to Object" requirements.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to object to data processing")
public class DataProcessingObjectionRequest {

    /**
     * Type of processing being objected to.
     */
    @NotNull(message = "Processing type is required")
    @Schema(description = "Type of processing being objected to")
    private ProcessingType processingType;

    /**
     * Specific processing activity ID (optional).
     */
    @Schema(description = "Specific processing activity ID")
    private String activityId;

    /**
     * Reason for objecting to the processing.
     */
    @NotBlank(message = "Objection reason is required")
    @Size(max = 1000, message = "Reason must not exceed 1000 characters")
    @Schema(description = "Reason for objecting to processing", example = "No longer relevant to my use of the service")
    private String reason;

    /**
     * Specific grounds for objection under GDPR.
     */
    @NotNull(message = "Legal grounds for objection is required")
    @Schema(description = "Legal grounds for the objection")
    private ObjectionGrounds objectionGrounds;

    /**
     * Specific data types to object to (optional - for partial objection).
     */
    @Schema(description = "Specific data types to object to")
    private List<DataType> dataTypes;

    /**
     * Service or application the objection applies to.
     */
    @Size(max = 100, message = "Service ID must not exceed 100 characters")
    @Schema(description = "Service the objection applies to", example = "focushive-backend")
    private String serviceId;

    /**
     * Whether to object to all related processing.
     */
    @Builder.Default
    @Schema(description = "Whether to object to all related processing", example = "false")
    private Boolean objectToAll = false;

    /**
     * Preferred resolution for the objection.
     */
    @Schema(description = "Preferred resolution")
    private PreferredResolution preferredResolution;

    /**
     * Evidence supporting the objection.
     */
    @Size(max = 2000, message = "Evidence must not exceed 2000 characters")
    @Schema(description = "Evidence supporting the objection")
    private String evidence;

    /**
     * Contact preference for follow-up.
     */
    @Schema(description = "Preferred contact method for follow-up")
    private ContactPreference contactPreference;

    /**
     * Additional context about the objection.
     */
    @Size(max = 1000, message = "Additional context must not exceed 1000 characters")
    @Schema(description = "Additional context about the objection")
    private String additionalContext;

    /**
     * Whether this is a partial or full objection.
     */
    @Builder.Default
    @Schema(description = "Whether this is a partial objection", example = "false")
    private Boolean partialObjection = false;

    /**
     * Urgency level of the objection.
     */
    @Builder.Default
    @Schema(description = "Urgency level of the objection")
    private UrgencyLevel urgency = UrgencyLevel.NORMAL;

    /**
     * Type of data processing.
     */
    public enum ProcessingType {
        MARKETING,            // Marketing and promotional activities
        PROFILING,            // Profiling and automated decision-making
        ANALYTICS,            // Analytics and tracking
        RESEARCH,             // Research and development
        PERSONALIZATION,      // Content personalization
        DIRECT_MARKETING,     // Direct marketing communications
        BEHAVIOURAL_ADVERTISING, // Behavioural advertising
        DATA_MINING,          // Data mining and pattern analysis
        SOCIAL_MEDIA_ANALYSIS, // Social media data analysis
        LOCATION_TRACKING,    // Location-based processing
        RECOMMENDATION_ENGINE, // Recommendation algorithms
        PERFORMANCE_MONITORING, // Performance and usage monitoring
        THIRD_PARTY_SHARING,  // Sharing with third parties
        ALL_PROCESSING        // Object to all processing activities
    }

    /**
     * Legal grounds for objection under GDPR.
     */
    public enum ObjectionGrounds {
        ARTICLE_21_1_F,       // Objection to legitimate interest processing (Art 21(1))
        ARTICLE_21_1_E,       // Objection to public task processing (Art 21(1))
        ARTICLE_21_2,         // Objection to direct marketing (Art 21(2))
        ARTICLE_21_3,         // Objection to research/statistics (Art 21(3))
        ARTICLE_21_4,         // Objection to automated decision-making (Art 21(4))
        COMPELLING_LEGITIMATE_GROUNDS, // Compelling legitimate grounds
        PARTICULAR_SITUATION  // Particular situation of the data subject
    }

    /**
     * Types of personal data.
     */
    public enum DataType {
        IDENTIFICATION,       // Name, ID numbers
        CONTACT,             // Email, phone, address
        DEMOGRAPHIC,         // Age, gender, location
        BEHAVIOURAL,         // Usage patterns, preferences
        TECHNICAL,           // IP address, device info
        FINANCIAL,           // Payment information
        COMMUNICATION,       // Messages, call logs
        LOCATION,            // Geographic location data
        BIOMETRIC,           // Biometric identifiers
        HEALTH,              // Health-related data
        SPECIAL_CATEGORY     // Special category data under GDPR
    }

    /**
     * Preferred resolution for the objection.
     */
    public enum PreferredResolution {
        STOP_PROCESSING,      // Completely stop the processing
        MODIFY_PROCESSING,    // Modify the processing to address concerns
        LIMIT_PROCESSING,     // Limit the scope of processing
        OPT_OUT_ONLY,        // Provide opt-out mechanism
        ANONYMIZE_DATA,      // Anonymize the data instead
        DELETE_DATA,         // Delete the data entirely
        DISCUSSION           // Discuss the concerns first
    }

    /**
     * Contact preference for follow-up.
     */
    public enum ContactPreference {
        EMAIL,               // Email communication
        PHONE,               // Phone call
        IN_APP_NOTIFICATION, // In-app notification
        POSTAL_MAIL,         // Physical mail
        NO_CONTACT          // No follow-up contact needed
    }

    /**
     * Urgency level for the objection.
     */
    public enum UrgencyLevel {
        LOW,                 // Can be processed within standard timeframe
        NORMAL,              // Standard processing (1 month under GDPR)
        HIGH,                // Needs expedited processing
        URGENT               // Immediate attention required
    }

    /**
     * Validates that the objection request is complete.
     */
    public boolean isValid() {
        // Either processingType should be specified or activityId
        return processingType != null || activityId != null;
    }

    /**
     * Checks if this is a direct marketing objection (which must be honored unconditionally).
     */
    public boolean isDirectMarketingObjection() {
        return processingType == ProcessingType.DIRECT_MARKETING ||
               processingType == ProcessingType.MARKETING ||
               objectionGrounds == ObjectionGrounds.ARTICLE_21_2;
    }

    /**
     * Checks if this objection requires balancing test.
     */
    public boolean requiresBalancingTest() {
        return !isDirectMarketingObjection() &&
               (objectionGrounds == ObjectionGrounds.ARTICLE_21_1_F ||
                objectionGrounds == ObjectionGrounds.COMPELLING_LEGITIMATE_GROUNDS);
    }
}