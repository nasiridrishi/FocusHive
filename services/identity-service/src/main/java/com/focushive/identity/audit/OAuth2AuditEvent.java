package com.focushive.identity.audit;

import com.focushive.identity.util.JsonAttributeConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * OAuth2 security audit event for tracking all OAuth2 operations.
 * Stored in PostgreSQL with JSONB for flexible data storage.
 */
@Entity
@Table(name = "oauth2_audit_log", indexes = {
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_event_type", columnList = "event_type"),
    @Index(name = "idx_audit_client_id", columnList = "client_id"),
    @Index(name = "idx_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_risk_level", columnList = "risk_level"),
    @Index(name = "idx_audit_success", columnList = "success"),
    @Index(name = "idx_audit_suspicious", columnList = "suspicious_activity"),
    @Index(name = "idx_audit_ip_address", columnList = "ip_address")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2AuditEvent {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Event type for categorization
     */
    public enum EventType {
        // Authorization events
        AUTHORIZATION_REQUEST,
        AUTHORIZATION_SUCCESS,
        AUTHORIZATION_FAILURE,
        AUTHORIZATION_CODE_ISSUED,
        
        // Token events
        TOKEN_REQUEST,
        ACCESS_TOKEN_ISSUED,
        REFRESH_TOKEN_ISSUED,
        TOKEN_REFRESH,
        TOKEN_REVOCATION,
        TOKEN_INTROSPECTION,
        TOKEN_VALIDATION_SUCCESS,
        TOKEN_VALIDATION_FAILURE,
        TOKEN_ROTATION,
        TOKEN_REUSE_DETECTED,
        
        // Client events
        CLIENT_AUTHENTICATION_SUCCESS,
        CLIENT_AUTHENTICATION_FAILURE,
        CLIENT_REGISTRATION,
        CLIENT_UPDATE,
        CLIENT_DELETION,
        CLIENT_SECRET_ROTATION,
        CLIENT_STATUS_CHANGE,
        
        // User events
        USER_INFO_ACCESS,
        USER_CONSENT_GRANTED,
        USER_CONSENT_REVOKED,
        
        // Security events
        SUSPICIOUS_ACTIVITY,
        RATE_LIMIT_EXCEEDED,
        INVALID_SCOPE_REQUEST,
        PKCE_VALIDATION_FAILURE,
        CROSS_ORIGIN_ATTEMPT,
        
        // Session events
        SESSION_CREATED,
        SESSION_REFRESHED,
        SESSION_TERMINATED,
        SESSION_ELEVATED,
        SESSION_EXPIRED,
        SESSION_INVALIDATED
    }

    /**
     * Risk level for security monitoring
     */
    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
    
    // Actor information
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "client_id", length = 100)
    private String clientId;

    @Column(name = "client_name", length = 255)
    private String clientName;
    
    // Request information
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "session_id", length = 100)
    private String sessionId;
    
    // OAuth2 specific
    @Column(name = "grant_type", length = 50)
    private String grantType;

    @Column(name = "scope", length = 500)
    private String scope;

    @Column(name = "redirect_uri", length = 500)
    private String redirectUri;

    @Column(name = "response_type", length = 50)
    private String responseType;

    @Column(name = "token_id", length = 100)
    private String tokenId;

    @Column(name = "authorization_code", length = 255)
    private String authorizationCode;
    
    // Result information
    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_description", length = 500)
    private String errorDescription;

    @Column(name = "http_status")
    private Integer httpStatus;
    
    // Additional context - stored as JSONB
    @Convert(converter = JsonAttributeConverter.class)
    @Column(name = "additional_data", columnDefinition = "jsonb")
    private Map<String, Object> additionalData;
    
    // Compliance and security
    @Column(name = "geolocation", length = 255)
    private String geolocation;

    @Column(name = "suspicious_activity")
    private boolean suspiciousActivity;

    @Column(name = "threat_indicators", length = 500)
    private String threatIndicators;

    // Performance metrics
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;
    
    /**
     * Create a success audit event
     */
    public static OAuth2AuditEvent success(EventType eventType, String clientId, UUID userId) {
        return OAuth2AuditEvent.builder()
            // ID will be auto-generated by JPA
            .eventType(eventType)
            .riskLevel(RiskLevel.LOW)
            .timestamp(Instant.now())
            .clientId(clientId)
            .userId(userId)
            .success(true)
            .build();
    }
    
    /**
     * Create a failure audit event
     */
    public static OAuth2AuditEvent failure(EventType eventType, String clientId, 
                                          String errorCode, String errorDescription) {
        return OAuth2AuditEvent.builder()
            // ID will be auto-generated by JPA
            .eventType(eventType)
            .riskLevel(RiskLevel.HIGH)
            .timestamp(Instant.now())
            .clientId(clientId)
            .success(false)
            .errorCode(errorCode)
            .errorDescription(errorDescription)
            .suspiciousActivity(true)
            .build();
    }
    
    /**
     * Create a security alert event
     */
    public static OAuth2AuditEvent securityAlert(EventType eventType, String threat, 
                                                String ipAddress, String clientId) {
        return OAuth2AuditEvent.builder()
            // ID will be auto-generated by JPA
            .eventType(eventType)
            .riskLevel(RiskLevel.CRITICAL)
            .timestamp(Instant.now())
            .clientId(clientId)
            .ipAddress(ipAddress)
            .success(false)
            .suspiciousActivity(true)
            .threatIndicators(threat)
            .build();
    }
}