package com.focushive.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Data Export Request entity for GDPR data portability compliance.
 * Manages user requests for data exports with proper audit trails.
 */
@Entity
@Table(name = "data_export_requests", indexes = {
    @Index(name = "idx_export_request_user", columnList = "user_id"),
    @Index(name = "idx_export_request_status", columnList = "status"),
    @Index(name = "idx_export_request_type", columnList = "request_type"),
    @Index(name = "idx_export_request_created", columnList = "created_at"),
    @Index(name = "idx_export_request_expires", columnList = "expires_at"),
    @Index(name = "idx_export_request_completed", columnList = "completed_at"),
    @Index(name = "idx_export_request_failed", columnList = "failed_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"user"})
@ToString(exclude = {"user"})
public class DataExportRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * User requesting the data export
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * Type of export request (full_export, partial_export, profile_only, etc.)
     */
    @Column(name = "request_type", nullable = false, length = 50)
    private String requestType;
    
    /**
     * Export format (JSON, CSV, XML, PDF)
     */
    @Column(nullable = false, length = 10)
    @Builder.Default
    private String format = "JSON";
    
    /**
     * Categories of data to export
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "data_export_categories", 
                     joinColumns = @JoinColumn(name = "export_request_id"))
    @Column(name = "category")
    @Builder.Default
    private Set<String> dataCategories = new HashSet<>();
    
    /**
     * Current status of the export request
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, PROCESSING, COMPLETED, FAILED, EXPIRED
    
    /**
     * Reason for the export request
     */
    @Column(length = 1000)
    private String reason;
    
    /**
     * Method used to verify the user's identity
     */
    @Column(name = "verification_method", length = 50)
    private String verificationMethod;
    
    /**
     * When processing started
     */
    @Column(name = "processing_started_at")
    private Instant processingStartedAt;
    
    /**
     * When the export was completed
     */
    @Column(name = "completed_at")
    private Instant completedAt;
    
    /**
     * When the export failed (if applicable)
     */
    @Column(name = "failed_at")
    private Instant failedAt;
    
    /**
     * File path where the export is stored
     */
    @Column(name = "export_file_path", length = 500)
    private String exportFilePath;
    
    /**
     * Size of the exported file in bytes
     */
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;
    
    /**
     * Number of records included in the export
     */
    @Column(name = "record_count")
    private Integer recordCount;
    
    /**
     * When the export expires and will be deleted
     */
    @Column(name = "expires_at")
    private Instant expiresAt;
    
    /**
     * Error message if the export failed
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    /**
     * Error code for categorizing failures
     */
    @Column(name = "error_code", length = 50)
    private String errorCode;
    
    /**
     * When the export was downloaded by the user
     */
    @Column(name = "downloaded_at")
    private Instant downloadedAt;
    
    /**
     * Number of times the export has been downloaded
     */
    @Column(name = "download_count")
    @Builder.Default
    private Integer downloadCount = 0;
    
    /**
     * IP address of the last download
     */
    @Column(name = "last_download_ip", length = 45)
    private String lastDownloadIp;
    
    /**
     * Additional metadata about the export request
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "data_export_metadata", 
                     joinColumns = @JoinColumn(name = "export_request_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();
    
    /**
     * Retention period for the export file in days
     */
    @Column(name = "retention_days")
    @Builder.Default
    private Integer retentionDays = 30;
    
    /**
     * Checksum of the export file for integrity verification
     */
    @Column(name = "file_checksum")
    private String fileChecksum;
    
    /**
     * Whether the export is encrypted
     */
    @Column(name = "encrypted")
    @Builder.Default
    private boolean encrypted = true;
    
    /**
     * Encryption key identifier (not the key itself)
     */
    @Column(name = "encryption_key_id")
    private String encryptionKeyId;
    
    // Audit fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    /**
     * Check if the export has expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
    
    /**
     * Check if the export is ready for download
     */
    public boolean isReadyForDownload() {
        return "COMPLETED".equals(status) && !isExpired();
    }
    
    /**
     * Mark the export as started
     */
    public void markAsStarted() {
        this.status = "PROCESSING";
        this.processingStartedAt = Instant.now();
    }
    
    /**
     * Mark the export as completed
     */
    public void markAsCompleted(String filePath, long fileSize, int recordCount) {
        this.status = "COMPLETED";
        this.completedAt = Instant.now();
        this.exportFilePath = filePath;
        this.fileSizeBytes = fileSize;
        this.recordCount = recordCount;
        
        // Set expiration if not already set
        if (this.expiresAt == null && this.retentionDays != null) {
            this.expiresAt = Instant.now().plus(retentionDays, ChronoUnit.DAYS);
        }
    }
    
    /**
     * Mark the export as failed
     */
    public void markAsFailed(String errorCode, String errorMessage) {
        this.status = "FAILED";
        this.failedAt = Instant.now();
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
    
    /**
     * Record a download
     */
    public void recordDownload(String ipAddress) {
        this.downloadedAt = Instant.now();
        this.downloadCount = (this.downloadCount != null ? this.downloadCount : 0) + 1;
        this.lastDownloadIp = ipAddress;
    }
    
    /**
     * Add metadata entry
     */
    public void addMetadata(String key, String value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }
    
    /**
     * Get metadata value
     */
    public String getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
    
    /**
     * Calculate processing duration in minutes
     */
    public Long getProcessingDurationMinutes() {
        if (processingStartedAt == null) {
            return null;
        }
        
        Instant endTime = completedAt != null ? completedAt : 
                         (failedAt != null ? failedAt : Instant.now());
        
        return ChronoUnit.MINUTES.between(processingStartedAt, endTime);
    }
    
    /**
     * Check if the export should be retained based on retention policy
     */
    public boolean shouldBeRetained() {
        if (retentionDays == null || createdAt == null) {
            return true; // No retention policy
        }
        
        Instant retentionExpiry = createdAt.plus(retentionDays, ChronoUnit.DAYS);
        return Instant.now().isBefore(retentionExpiry);
    }
    
    /**
     * Get days remaining until deletion
     */
    public Long getDaysUntilDeletion() {
        if (expiresAt == null) {
            return null;
        }
        
        long daysRemaining = ChronoUnit.DAYS.between(Instant.now(), expiresAt);
        return Math.max(0, daysRemaining);
    }
    
    /**
     * Check if this is a GDPR data portability request
     */
    public boolean isGDPRRequest() {
        return reason != null && 
               (reason.toLowerCase().contains("gdpr") ||
                reason.toLowerCase().contains("data portability") ||
                reason.toLowerCase().contains("article 20"));
    }
    
    /**
     * Get file size in human-readable format
     */
    public String getFormattedFileSize() {
        if (fileSizeBytes == null) {
            return "Unknown";
        }
        
        if (fileSizeBytes < 1024) {
            return fileSizeBytes + " B";
        } else if (fileSizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", fileSizeBytes / 1024.0);
        } else if (fileSizeBytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", fileSizeBytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", fileSizeBytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * Check if the export has been downloaded
     */
    public boolean hasBeenDownloaded() {
        return downloadedAt != null && downloadCount != null && downloadCount > 0;
    }
    
    /**
     * Validate that all required data categories are supported
     */
    public boolean hasValidDataCategories() {
        if (dataCategories == null || dataCategories.isEmpty()) {
            return false;
        }
        
        // Define supported categories - in real implementation this would come from configuration
        Set<String> supportedCategories = Set.of(
            "profile", "preferences", "activities", "audit_logs", "permissions",
            "consent_records", "data_exports", "oauth_tokens", "personas", "all"
        );
        
        return supportedCategories.containsAll(dataCategories);
    }
    
    /**
     * Get estimated processing time based on data categories and historical data
     */
    public Long getEstimatedProcessingMinutes() {
        if (dataCategories == null || dataCategories.isEmpty()) {
            return 5L; // Default estimate
        }
        
        // Simple estimation logic - in reality this would be more sophisticated
        long estimatedMinutes = 0;
        for (String category : dataCategories) {
            switch (category.toLowerCase()) {
                case "all":
                    return 60L; // Full export takes about an hour
                case "audit_logs":
                case "activities":
                    estimatedMinutes += 15;
                    break;
                case "profile":
                case "preferences":
                case "personas":
                    estimatedMinutes += 2;
                    break;
                default:
                    estimatedMinutes += 5;
            }
        }
        
        return Math.max(5, estimatedMinutes);
    }
}