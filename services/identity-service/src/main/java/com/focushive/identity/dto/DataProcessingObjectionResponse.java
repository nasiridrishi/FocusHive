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
 * Response DTO for data processing objection containing objection ID and status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response for data processing objection")
public class DataProcessingObjectionResponse {

    /**
     * Unique identifier for the objection.
     */
    @Schema(description = "Unique identifier for the objection")
    private UUID objectionId;

    /**
     * Current status of the objection.
     */
    @Schema(description = "Current status of the objection")
    private ObjectionStatus status;

    /**
     * Type of processing that was objected to.
     */
    @Schema(description = "Type of processing objected to")
    private ProcessingType processingType;

    /**
     * Specific processing activity ID (if applicable).
     */
    @Schema(description = "Specific processing activity ID")
    private String activityId;

    /**
     * Timestamp when the objection was submitted.
     */
    @Schema(description = "Timestamp when objection was submitted")
    private Instant submittedTimestamp;

    /**
     * Timestamp when the objection was processed.
     */
    @Schema(description = "Timestamp when objection was processed")
    private Instant processedTimestamp;

    /**
     * Deadline for processing the objection (1 month under GDPR).
     */
    @Schema(description = "Deadline for processing the objection")
    private Instant processingDeadline;

    /**
     * Legal grounds for the objection.
     */
    @Schema(description = "Legal grounds for the objection")
    private ObjectionGrounds objectionGrounds;

    /**
     * Decision made on the objection.
     */
    @Schema(description = "Decision made on the objection")
    private ObjectionDecision decision;

    /**
     * Reason for the decision.
     */
    @Schema(description = "Reason for the decision")
    private String decisionReason;

    /**
     * Actions taken as a result of the objection.
     */
    @Schema(description = "Actions taken")
    private List<String> actionsTaken;

    /**
     * Message to the user about the objection.
     */
    @Schema(description = "Message about the objection")
    private String message;

    /**
     * Instructions for next steps (if any).
     */
    @Schema(description = "Next steps for the user")
    private String nextSteps;

    /**
     * Reference number for tracking.
     */
    @Schema(description = "Reference number for tracking", example = "OBJ-2025-001234")
    private String referenceNumber;

    /**
     * Whether the user can appeal this decision.
     */
    @Builder.Default
    @Schema(description = "Whether the user can appeal", example = "true")
    private Boolean appealable = true;

    /**
     * Deadline for appealing the decision.
     */
    @Schema(description = "Deadline for appeal")
    private Instant appealDeadline;

    /**
     * Support contact information.
     */
    @Schema(description = "Support contact for questions")
    private String supportContact;

    /**
     * Details about balancing test (if applicable).
     */
    @Schema(description = "Details about balancing test performed")
    private BalancingTestResult balancingTest;

    /**
     * Processing activities that were stopped.
     */
    @Schema(description = "Processing activities that were stopped")
    private List<String> stoppedActivities;

    /**
     * Processing activities that continue (with justification).
     */
    @Schema(description = "Processing activities that continue")
    private List<ContinuingActivity> continuingActivities;

    /**
     * Status of the objection.
     */
    public enum ObjectionStatus {
        SUBMITTED,            // Objection has been submitted
        UNDER_REVIEW,         // Objection is being reviewed
        BALANCING_TEST,       // Performing balancing test
        UPHELD,              // Objection was upheld
        REJECTED,            // Objection was rejected
        PARTIALLY_UPHELD,    // Objection was partially upheld
        APPEAL_PENDING,      // Appeal is pending review
        WITHDRAWN,           // Objection was withdrawn by user
        EXPIRED              // Objection expired without decision
    }

    /**
     * Decision made on the objection.
     */
    public enum ObjectionDecision {
        STOP_PROCESSING,      // Processing will be stopped
        CONTINUE_PROCESSING,  // Processing will continue
        MODIFY_PROCESSING,    // Processing will be modified
        PARTIAL_STOP,        // Some processing will stop
        ANONYMIZE_DATA,      // Data will be anonymized
        RESTRICT_PROCESSING   // Processing will be restricted
    }

    /**
     * Type of processing (reused for consistency).
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
     * Legal grounds for objection (reused for consistency).
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
     * Result of balancing test.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Result of balancing test")
    public static class BalancingTestResult {

        /**
         * Whether balancing test was performed.
         */
        @Schema(description = "Whether balancing test was performed", example = "true")
        private Boolean performed;

        /**
         * Outcome of the balancing test.
         */
        @Schema(description = "Outcome of the balancing test")
        private BalancingOutcome outcome;

        /**
         * Factors considered in favor of data subject.
         */
        @Schema(description = "Factors in favor of data subject")
        private List<String> dataSubjectFactors;

        /**
         * Factors considered in favor of controller.
         */
        @Schema(description = "Factors in favor of controller")
        private List<String> controllerFactors;

        /**
         * Detailed reasoning for the decision.
         */
        @Schema(description = "Detailed reasoning")
        private String reasoning;

        /**
         * Date when balancing test was performed.
         */
        @Schema(description = "Date of balancing test")
        private Instant performedDate;
    }

    /**
     * Processing activity that continues after objection.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Processing activity that continues")
    public static class ContinuingActivity {

        /**
         * Activity ID.
         */
        @Schema(description = "Activity ID")
        private String activityId;

        /**
         * Activity name.
         */
        @Schema(description = "Activity name")
        private String activityName;

        /**
         * Justification for continuing.
         */
        @Schema(description = "Justification for continuing")
        private String justification;

        /**
         * Legal basis for continuing.
         */
        @Schema(description = "Legal basis for continuing")
        private String legalBasis;
    }

    /**
     * Outcome of balancing test.
     */
    public enum BalancingOutcome {
        FAVOR_DATA_SUBJECT,   // Balancing test favors data subject
        FAVOR_CONTROLLER,     // Balancing test favors controller
        BALANCED,             // Factors are balanced, other considerations apply
        INCONCLUSIVE          // Test was inconclusive
    }

    /**
     * Checks if the objection was successful.
     */
    public boolean isObjectionSuccessful() {
        return status == ObjectionStatus.UPHELD ||
               status == ObjectionStatus.PARTIALLY_UPHELD;
    }

    /**
     * Checks if the objection can be appealed.
     */
    public boolean canBeAppealed() {
        return appealable &&
               (status == ObjectionStatus.REJECTED ||
                status == ObjectionStatus.PARTIALLY_UPHELD) &&
               appealDeadline != null &&
               Instant.now().isBefore(appealDeadline);
    }

    /**
     * Checks if the objection is still being processed.
     */
    public boolean isInProgress() {
        return status == ObjectionStatus.SUBMITTED ||
               status == ObjectionStatus.UNDER_REVIEW ||
               status == ObjectionStatus.BALANCING_TEST;
    }
}