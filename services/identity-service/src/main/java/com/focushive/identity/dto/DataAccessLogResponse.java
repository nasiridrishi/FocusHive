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
 * Response DTO for paginated log of data access with user ID,
 * accessor details, and timestamps.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated log of data access events")
public class DataAccessLogResponse {

    /**
     * List of data access log entries for the current page.
     */
    @Schema(description = "List of data access log entries")
    private List<DataAccessLogEntry> entries;

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
     * Total number of access log entries.
     */
    @Schema(description = "Total number of access log entries", example = "150")
    private Long totalElements;

    /**
     * Total number of pages.
     */
    @Schema(description = "Total number of pages", example = "8")
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
     * Individual data access log entry.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Individual data access log entry")
    public static class DataAccessLogEntry {

        /**
         * Unique identifier for the access log entry.
         */
        @Schema(description = "Unique identifier for the log entry")
        private UUID logId;

        /**
         * ID of the user whose data was accessed.
         */
        @Schema(description = "ID of the user whose data was accessed")
        private UUID userId;

        /**
         * Details about who accessed the data.
         */
        @Schema(description = "Details about the accessor")
        private AccessorDetails accessor;

        /**
         * Timestamp when the access occurred.
         */
        @Schema(description = "Timestamp when access occurred")
        private Instant accessTimestamp;

        /**
         * Type of data that was accessed.
         */
        @Schema(description = "Type of data accessed", example = "PROFILE")
        private DataType dataType;

        /**
         * Specific action performed on the data.
         */
        @Schema(description = "Action performed", example = "READ")
        private AccessAction action;

        /**
         * Purpose of the data access.
         */
        @Schema(description = "Purpose of data access", example = "User profile display")
        private String purpose;

        /**
         * IP address from which the access was made.
         */
        @Schema(description = "IP address of the accessor", example = "192.168.1.1")
        private String ipAddress;

        /**
         * User agent string from the access request.
         */
        @Schema(description = "User agent string")
        private String userAgent;

        /**
         * Legal basis for the data processing.
         */
        @Schema(description = "Legal basis for processing", example = "CONSENT")
        private LegalBasis legalBasis;
    }

    /**
     * Details about who accessed the data.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Details about the data accessor")
    public static class AccessorDetails {

        /**
         * Type of accessor (user, system, service).
         */
        @Schema(description = "Type of accessor", example = "USER")
        private AccessorType type;

        /**
         * ID of the accessing entity.
         */
        @Schema(description = "ID of the accessing entity")
        private String accessorId;

        /**
         * Name or description of the accessor.
         */
        @Schema(description = "Name of the accessor", example = "FocusHive Backend Service")
        private String accessorName;

        /**
         * Service or application that made the access.
         */
        @Schema(description = "Service that accessed the data", example = "focushive-backend")
        private String serviceName;
    }

    /**
     * Type of data accessed.
     */
    public enum DataType {
        PROFILE,           // User profile information
        AUTHENTICATION,    // Login/authentication data
        PERSONA,          // User persona data
        PREFERENCES,      // User preferences
        AUDIT_LOG,        // Audit log data
        CONSENT,          // Consent records
        SESSION,          // Session data
        OAUTH_TOKEN,      // OAuth token data
        METADATA          // Other metadata
    }

    /**
     * Action performed on the data.
     */
    public enum AccessAction {
        READ,             // Data was read/viewed
        CREATE,           // Data was created
        UPDATE,           // Data was modified
        DELETE,           // Data was deleted
        EXPORT,           // Data was exported
        SHARE             // Data was shared
    }

    /**
     * Type of accessor.
     */
    public enum AccessorType {
        USER,             // End user
        SERVICE,          // Internal service
        ADMIN,            // Administrator
        SYSTEM,           // System process
        THIRD_PARTY       // External third party
    }

    /**
     * Legal basis for data processing.
     */
    public enum LegalBasis {
        CONSENT,          // User consent
        CONTRACT,         // Contractual necessity
        LEGAL_OBLIGATION, // Legal obligation
        VITAL_INTEREST,   // Vital interest
        PUBLIC_TASK,      // Public task
        LEGITIMATE_INTEREST // Legitimate interest
    }
}