package com.focushive.notification.entity;

import com.focushive.notification.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing security audit logs for tracking security events
 * and operations within the notification service.
 */
@Entity
@Table(name = "security_audit_log")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAuditLog extends BaseEntity {

    /**
     * The action that was performed (e.g., "CREATE_PREFERENCE", "LOGIN_ATTEMPT")
     */
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    /**
     * Username of the user who performed the action
     */
    @Column(name = "username", length = 100)
    private String username;

    /**
     * IP address from which the action was performed
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent string from the request
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Detailed description of what happened
     */
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    /**
     * Whether the action was successful or not
     */
    @Column(name = "success", nullable = false)
    private Boolean success;

    /**
     * Timestamp when the action occurred
     */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /**
     * Session ID if available
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    /**
     * Additional metadata as JSON
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}