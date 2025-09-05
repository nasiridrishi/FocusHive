package com.focushive.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced Audit Log entity for comprehensive security and compliance tracking.
 * Supports detailed logging of all identity-related events.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_log_user", columnList = "user_id"),
    @Index(name = "idx_audit_log_client", columnList = "client_id"),
    @Index(name = "idx_audit_log_event_type", columnList = "event_type"),
    @Index(name = "idx_audit_log_category", columnList = "event_category"),
    @Index(name = "idx_audit_log_severity", columnList = "severity"),
    @Index(name = "idx_audit_log_outcome", columnList = "outcome"),
    @Index(name = "idx_audit_log_created", columnList = "created_at"),
    @Index(name = "idx_audit_log_ip", columnList = "ip_address"),
    @Index(name = "idx_audit_log_resource", columnList = "resource")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"user", "client"})
@ToString(exclude = {"user", "client"})
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * User associated with this audit event (null for system events)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    /**
     * OAuth client associated with this event (if applicable)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private OAuthClient client;
    
    /**
     * Type of event (e.g., "USER_LOGIN", "OAUTH_TOKEN_ISSUED", "DATA_ACCESS")
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    
    /**
     * Category of the event for grouping
     */
    @Column(name = "event_category", nullable = false, length = 50)
    private String eventCategory;
    
    /**
     * Human-readable description of the event
     */
    @Column(nullable = false, length = 1000)
    private String description;
    
    /**
     * Resource that was accessed or modified
     */
    @Column(length = 200)
    private String resource;
    
    /**
     * Action performed (CREATE, READ, UPDATE, DELETE, LOGIN, LOGOUT, etc.)
     */
    @Column(nullable = false, length = 20)
    private String action;
    
    /**
     * Outcome of the action (SUCCESS, FAILURE, PARTIAL)
     */
    @Column(nullable = false, length = 20)
    private String outcome;
    
    /**
     * Severity level (INFO, WARNING, ERROR, CRITICAL)
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String severity = "INFO";
    
    /**
     * IP address from which the action was performed
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    /**
     * User agent string
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * Session identifier for tracking related events
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;
    
    /**
     * Request identifier for tracing
     */
    @Column(name = "request_id", length = 100)
    private String requestId;
    
    /**
     * Additional metadata about the event
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "audit_log_metadata", 
                     joinColumns = @JoinColumn(name = "audit_log_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();
    
    /**
     * Error code if the outcome was a failure
     */
    @Column(name = "error_code", length = 50)
    private String errorCode;
    
    /**
     * Error message if the outcome was a failure
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    /**
     * Duration of the operation in milliseconds
     */
    @Column(name = "duration_ms")
    private Long durationMs;
    
    /**
     * Geographic location where the event occurred
     */
    @Column(name = "geographic_location", length = 100)
    private String geographicLocation;
    
    /**
     * Risk score associated with this event (0-100)
     */
    @Column(name = "risk_score")
    private Integer riskScore;
    
    /**
     * Whether this event triggered any automated actions
     */
    @Column(name = "automated_action_triggered")
    @Builder.Default
    private boolean automatedActionTriggered = false;
    
    /**
     * Details of any automated action taken
     */
    @Column(name = "automated_action_details", length = 500)
    private String automatedActionDetails;
    
    // Audit fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
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
     * Check if this is a security-related event
     */
    public boolean isSecurityEvent() {
        return "SECURITY".equals(eventCategory) || 
               "AUTHENTICATION".equals(eventCategory) ||
               "AUTHORIZATION".equals(eventCategory);
    }
    
    /**
     * Check if this is a data privacy event
     */
    public boolean isDataPrivacyEvent() {
        return "DATA_PRIVACY".equals(eventCategory) ||
               eventType.contains("DATA_ACCESS") ||
               eventType.contains("DATA_EXPORT") ||
               eventType.contains("GDPR");
    }
    
    /**
     * Check if this event indicates a potential security threat
     */
    public boolean isHighRisk() {
        return riskScore != null && riskScore >= 70;
    }
    
    /**
     * Check if this is a system event (no user involved)
     */
    public boolean isSystemEvent() {
        return user == null;
    }
    
    /**
     * Check if this is a failed event
     */
    public boolean isFailed() {
        return "FAILURE".equals(outcome);
    }
    
    /**
     * Check if this is a critical severity event
     */
    public boolean isCritical() {
        return "CRITICAL".equals(severity);
    }
    
    /**
     * Get formatted log message for external logging systems
     */
    public String getFormattedLogMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(severity).append("] ");
        sb.append(eventCategory).append(".").append(eventType);
        
        if (user != null) {
            sb.append(" - User: ").append(user.getUsername());
        }
        
        if (client != null) {
            sb.append(" - Client: ").append(client.getClientName());
        }
        
        sb.append(" - ").append(action).append(" ").append(outcome);
        
        if (resource != null) {
            sb.append(" - Resource: ").append(resource);
        }
        
        if (ipAddress != null) {
            sb.append(" - IP: ").append(ipAddress);
        }
        
        sb.append(" - ").append(description);
        
        return sb.toString();
    }
    
    /**
     * Create a follow-up audit log entry
     */
    public AuditLog createFollowUp(String followUpEventType, String followUpDescription, String followUpOutcome) {
        return AuditLog.builder()
                .user(this.user)
                .client(this.client)
                .eventType(followUpEventType)
                .eventCategory(this.eventCategory)
                .description(followUpDescription)
                .resource(this.resource)
                .action("FOLLOW_UP")
                .outcome(followUpOutcome)
                .severity(this.severity)
                .ipAddress(this.ipAddress)
                .userAgent(this.userAgent)
                .sessionId(this.sessionId)
                .requestId(this.requestId)
                .geographicLocation(this.geographicLocation)
                .build();
    }
    
    /**
     * Builder helper for common audit log types
     */
    public static class AuditLogBuilder {
        
        public AuditLogBuilder loginSuccess(User user, String ipAddress) {
            return this.user(user)
                    .eventType("USER_LOGIN")
                    .eventCategory("AUTHENTICATION")
                    .description("User successfully logged in")
                    .action("LOGIN")
                    .outcome("SUCCESS")
                    .severity("INFO")
                    .ipAddress(ipAddress);
        }
        
        public AuditLogBuilder loginFailure(String username, String ipAddress, String reason) {
            return this.eventType("USER_LOGIN_FAILED")
                    .eventCategory("SECURITY")
                    .description("Failed login attempt for user: " + username + ". Reason: " + reason)
                    .action("LOGIN")
                    .outcome("FAILURE")
                    .severity("WARNING")
                    .ipAddress(ipAddress);
        }
        
        public AuditLogBuilder dataAccess(User user, OAuthClient client, String dataType, String permissions) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("data_type", dataType);
            metadata.put("permissions", permissions);
            
            return this.user(user)
                    .client(client)
                    .eventType("DATA_ACCESS")
                    .eventCategory("DATA_PRIVACY")
                    .description("Client accessed user data: " + dataType)
                    .resource("data/" + dataType)
                    .action("read")
                    .outcome("SUCCESS")
                    .severity("INFO")
                    .metadata(metadata);
        }
        
        public AuditLogBuilder consentGiven(User user, String consentType, String version) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("consent_type", consentType);
            metadata.put("consent_version", version);
            
            return this.user(user)
                    .eventType("CONSENT_GIVEN")
                    .eventCategory("DATA_PRIVACY")
                    .description("User gave consent for: " + consentType)
                    .action("GRANT")
                    .outcome("SUCCESS")
                    .severity("INFO")
                    .metadata(metadata);
        }
    }
}