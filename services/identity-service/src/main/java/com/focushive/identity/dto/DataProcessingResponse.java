package com.focushive.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for listing data processing activities.
 * This supports GDPR transparency requirements about data processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "List of data processing activities")
public class DataProcessingResponse {

    /**
     * List of data processing activities.
     */
    @Schema(description = "List of data processing activities")
    private List<DataProcessingActivity> activities;

    /**
     * Timestamp when this information was generated.
     */
    @Schema(description = "Timestamp when information was generated")
    private Instant generatedAt;

    /**
     * Total number of processing activities.
     */
    @Schema(description = "Total number of processing activities", example = "12")
    private Integer totalActivities;

    /**
     * User ID this processing information relates to.
     */
    @Schema(description = "User ID this processing relates to")
    private String userId;

    /**
     * Summary of processing by category.
     */
    @Schema(description = "Summary of processing by category")
    private ProcessingSummary summary;

    /**
     * Individual data processing activity.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Individual data processing activity")
    public static class DataProcessingActivity {

        /**
         * Unique identifier for the processing activity.
         */
        @Schema(description = "Unique identifier for the processing activity")
        private String activityId;

        /**
         * Name/title of the processing activity.
         */
        @Schema(description = "Name of the processing activity", example = "User Profile Management")
        private String activityName;

        /**
         * Detailed description of the processing.
         */
        @Schema(description = "Description of the processing activity")
        private String description;

        /**
         * Purpose of the data processing.
         */
        @Schema(description = "Purpose of the processing", example = "Provide personalized user experience")
        private String purpose;

        /**
         * Category of processing.
         */
        @Schema(description = "Category of processing")
        private ProcessingCategory category;

        /**
         * Legal basis for the processing.
         */
        @Schema(description = "Legal basis for processing")
        private LegalBasis legalBasis;

        /**
         * Types of personal data processed.
         */
        @Schema(description = "Types of personal data processed")
        private List<DataType> dataTypes;

        /**
         * Recipients or categories of recipients.
         */
        @Schema(description = "Recipients of the data")
        private List<String> recipients;

        /**
         * Retention period for the data.
         */
        @Schema(description = "Data retention period", example = "2 years after account closure")
        private String retentionPeriod;

        /**
         * Security measures in place.
         */
        @Schema(description = "Security measures")
        private List<String> securityMeasures;

        /**
         * Whether data is transferred outside EU/EEA.
         */
        @Builder.Default
        @Schema(description = "Whether data is transferred internationally", example = "false")
        private Boolean internationalTransfer = false;

        /**
         * Countries where data may be transferred.
         */
        @Schema(description = "Countries for international transfers")
        private List<String> transferCountries;

        /**
         * Safeguards for international transfers.
         */
        @Schema(description = "Safeguards for international transfers")
        private List<String> transferSafeguards;

        /**
         * Whether automated decision making is involved.
         */
        @Builder.Default
        @Schema(description = "Whether automated decision making is involved", example = "false")
        private Boolean automatedDecisionMaking = false;

        /**
         * Logic of automated decision making (if applicable).
         */
        @Schema(description = "Logic of automated decision making")
        private String automatedLogic;

        /**
         * Current status of the processing.
         */
        @Schema(description = "Current status of the processing")
        private ProcessingStatus status;

        /**
         * When this processing activity started.
         */
        @Schema(description = "When processing started")
        private Instant startDate;

        /**
         * When this processing activity will end (if applicable).
         */
        @Schema(description = "When processing will end")
        private Instant endDate;

        /**
         * Data controller information.
         */
        @Schema(description = "Data controller details")
        private ControllerInfo controller;

        /**
         * Data processor information (if applicable).
         */
        @Schema(description = "Data processor details")
        private List<ProcessorInfo> processors;
    }

    /**
     * Summary of processing activities.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Summary of processing activities")
    public static class ProcessingSummary {

        /**
         * Count by processing category.
         */
        @Schema(description = "Count by processing category")
        private List<CategoryCount> categoryBreakdown;

        /**
         * Count by legal basis.
         */
        @Schema(description = "Count by legal basis")
        private List<LegalBasisCount> legalBasisBreakdown;

        /**
         * Number of activities with international transfers.
         */
        @Schema(description = "Activities with international transfers", example = "2")
        private Integer internationalTransferCount;

        /**
         * Number of activities with automated decision making.
         */
        @Schema(description = "Activities with automated decision making", example = "1")
        private Integer automatedDecisionCount;
    }

    /**
     * Count by category.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Count by processing category")
    public static class CategoryCount {
        @Schema(description = "Processing category")
        private ProcessingCategory category;

        @Schema(description = "Number of activities", example = "3")
        private Integer count;
    }

    /**
     * Count by legal basis.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Count by legal basis")
    public static class LegalBasisCount {
        @Schema(description = "Legal basis")
        private LegalBasis legalBasis;

        @Schema(description = "Number of activities", example = "5")
        private Integer count;
    }

    /**
     * Data controller information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Data controller information")
    public static class ControllerInfo {
        @Schema(description = "Name of the controller", example = "FocusHive Ltd")
        private String name;

        @Schema(description = "Contact information")
        private String contactInfo;

        @Schema(description = "DPO contact information")
        private String dpoContact;
    }

    /**
     * Data processor information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Data processor information")
    public static class ProcessorInfo {
        @Schema(description = "Name of the processor")
        private String name;

        @Schema(description = "Contact information")
        private String contactInfo;

        @Schema(description = "Processing activities")
        private String activities;
    }

    /**
     * Category of data processing.
     */
    public enum ProcessingCategory {
        AUTHENTICATION,       // User authentication and access control
        PROFILE_MANAGEMENT,   // User profile data management
        COMMUNICATION,        // Communications and messaging
        ANALYTICS,            // Analytics and insights
        MARKETING,            // Marketing and promotions
        CUSTOMER_SUPPORT,     // Customer service and support
        SECURITY,             // Security monitoring and fraud prevention
        LEGAL_COMPLIANCE,     // Legal and regulatory compliance
        RESEARCH,             // Research and development
        PAYMENTS,             // Payment processing
        CONTENT_DELIVERY,     // Content delivery and personalization
        SOCIAL_FEATURES       // Social and community features
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
     * Legal basis for processing.
     */
    public enum LegalBasis {
        CONSENT,              // User consent (GDPR Article 6(1)(a))
        CONTRACT,             // Contractual necessity (GDPR Article 6(1)(b))
        LEGAL_OBLIGATION,     // Legal obligation (GDPR Article 6(1)(c))
        VITAL_INTEREST,       // Vital interest (GDPR Article 6(1)(d))
        PUBLIC_TASK,          // Public task (GDPR Article 6(1)(e))
        LEGITIMATE_INTEREST   // Legitimate interest (GDPR Article 6(1)(f))
    }

    /**
     * Status of processing activity.
     */
    public enum ProcessingStatus {
        ACTIVE,               // Currently active
        SUSPENDED,            // Temporarily suspended
        TERMINATED,           // Permanently ended
        PLANNED               // Planned for future
    }
}