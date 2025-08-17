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
 * Data Permission entity for managing granular data access permissions.
 * Supports GDPR compliance, data retention policies, and fine-grained access control.
 */
@Entity
@Table(name = "data_permissions", indexes = {
    @Index(name = "idx_data_permission_user", columnList = "user_id"),
    @Index(name = "idx_data_permission_client", columnList = "client_id"),
    @Index(name = "idx_data_permission_type", columnList = "data_type"),
    @Index(name = "idx_data_permission_active", columnList = "active"),
    @Index(name = "idx_data_permission_expires", columnList = "expires_at"),
    @Index(name = "idx_data_permission_revoked", columnList = "revoked_at"),
    @Index(name = "idx_data_permission_internal", columnList = "is_internal")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"user", "client", "parentPermission"})
@ToString(exclude = {"user", "client", "parentPermission"})
public class DataPermission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * User who owns the data
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * OAuth client requesting access (null for internal permissions)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private OAuthClient client;
    
    /**
     * Type of data being accessed (e.g., "profile", "activity", "analytics")
     */
    @Column(name = "data_type", nullable = false, length = 50)
    private String dataType;
    
    /**
     * Specific permissions granted (read, write, delete, export, etc.)
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "data_permission_grants", 
                     joinColumns = @JoinColumn(name = "permission_id"))
    @Column(name = "permission")
    @Builder.Default
    private Set<String> permissions = new HashSet<>();
    
    /**
     * Purpose statement for data processing (GDPR requirement)
     */
    @Column(nullable = false, length = 1000)
    private String purpose;
    
    /**
     * Legal basis for data processing (consent, contract, legitimate_interest, etc.)
     */
    @Column(name = "legal_basis", length = 50)
    @Builder.Default
    private String legalBasis = "consent";
    
    /**
     * Data retention period in days (null = indefinite)
     */
    @Column(name = "retention_period_days")
    private Integer retentionPeriodDays;
    
    /**
     * Permission expiration time (null = no expiration)
     */
    @Column(name = "expires_at")
    private Instant expiresAt;
    
    /**
     * Whether this permission is currently active
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
    
    /**
     * When the permission was revoked (if applicable)
     */
    @Column(name = "revoked_at")
    private Instant revokedAt;
    
    /**
     * Reason for revocation
     */
    @Column(name = "revocation_reason", length = 500)
    private String revocationReason;
    
    /**
     * Whether this is an internal permission (vs external client)
     */
    @Column(name = "is_internal", nullable = false)
    @Builder.Default
    private boolean isInternal = false;
    
    /**
     * Additional conditions and constraints for data access
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "data_permission_conditions", 
                     joinColumns = @JoinColumn(name = "permission_id"))
    @MapKeyColumn(name = "condition_key")
    @Column(name = "condition_value")
    @Builder.Default
    private Map<String, String> conditions = new HashMap<>();
    
    /**
     * Parent permission (for hierarchical permissions)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_permission_id")
    private DataPermission parentPermission;
    
    /**
     * IP address from which permission was granted
     */
    @Column(name = "granted_ip", length = 45)
    private String grantedIp;
    
    /**
     * User agent from permission grant
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * Last time this permission was used
     */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;
    
    /**
     * Usage count for analytics
     */
    @Column(name = "usage_count")
    @Builder.Default
    private Long usageCount = 0L;
    
    // Audit fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    /**
     * Check if the permission is expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
    
    /**
     * Check if the data retention period has expired
     */
    public boolean isRetentionExpired() {
        if (retentionPeriodDays == null) {
            return false; // No retention limit
        }
        return createdAt != null && 
               Instant.now().isAfter(createdAt.plus(retentionPeriodDays, ChronoUnit.DAYS));
    }
    
    /**
     * Check if the permission is currently valid
     */
    public boolean isValid() {
        return active && !isExpired() && !isRetentionExpired();
    }
    
    /**
     * Check if permission includes a specific grant
     */
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
    
    /**
     * Check if permission includes all required grants
     */
    public boolean hasAllPermissions(Set<String> requiredPermissions) {
        return permissions != null && permissions.containsAll(requiredPermissions);
    }
    
    /**
     * Revoke the permission
     */
    public void revoke(String reason) {
        this.active = false;
        this.revokedAt = Instant.now();
        this.revocationReason = reason;
    }
    
    /**
     * Mark the permission as used
     */
    public void markAsUsed() {
        this.lastUsedAt = Instant.now();
        this.usageCount = (this.usageCount != null ? this.usageCount : 0L) + 1;
    }
    
    /**
     * Add a permission grant
     */
    public void addPermission(String permission) {
        if (permissions == null) {
            permissions = new HashSet<>();
        }
        permissions.add(permission);
    }
    
    /**
     * Remove a permission grant
     */
    public void removePermission(String permission) {
        if (permissions != null) {
            permissions.remove(permission);
        }
    }
    
    /**
     * Add a condition
     */
    public void addCondition(String key, String value) {
        if (conditions == null) {
            conditions = new HashMap<>();
        }
        conditions.put(key, value);
    }
    
    /**
     * Get a condition value
     */
    public String getCondition(String key) {
        return conditions != null ? conditions.get(key) : null;
    }
    
    /**
     * Check if a condition is satisfied
     */
    public boolean isConditionSatisfied(String key, String expectedValue) {
        String actualValue = getCondition(key);
        return actualValue != null && actualValue.equals(expectedValue);
    }
    
    /**
     * Calculate days remaining until retention expiry
     */
    public Long getDaysUntilRetentionExpiry() {
        if (retentionPeriodDays == null || createdAt == null) {
            return null; // No retention limit
        }
        
        Instant expiryDate = createdAt.plus(retentionPeriodDays, ChronoUnit.DAYS);
        long daysRemaining = ChronoUnit.DAYS.between(Instant.now(), expiryDate);
        return Math.max(0, daysRemaining);
    }
    
    /**
     * Create a derived permission based on this one
     */
    public DataPermission createDerivedPermission(Set<String> derivedPermissions, String derivedPurpose) {
        return DataPermission.builder()
                .user(this.user)
                .client(this.client)
                .dataType(this.dataType)
                .permissions(derivedPermissions)
                .purpose(derivedPurpose)
                .legalBasis(this.legalBasis)
                .retentionPeriodDays(this.retentionPeriodDays)
                .parentPermission(this)
                .isInternal(this.isInternal)
                .build();
    }
}