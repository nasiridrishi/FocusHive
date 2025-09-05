package com.focushive.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Consent Record entity for GDPR compliance and consent management.
 * Tracks all consent given by users with complete audit trail.
 */
@Entity
@Table(name = "consent_records", indexes = {
    @Index(name = "idx_consent_record_user", columnList = "user_id"),
    @Index(name = "idx_consent_record_type", columnList = "consent_type"),
    @Index(name = "idx_consent_record_active", columnList = "active"),
    @Index(name = "idx_consent_record_given", columnList = "consent_given"),
    @Index(name = "idx_consent_record_expires", columnList = "expires_at"),
    @Index(name = "idx_consent_record_withdrawn", columnList = "withdrawn_at"),
    @Index(name = "idx_consent_record_basis", columnList = "legal_basis"),
    @Index(name = "idx_consent_record_version", columnList = "consent_version")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"user", "parentConsent"})
@ToString(exclude = {"user", "parentConsent"})
public class ConsentRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * User who gave (or withdrew) consent
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * Type of consent (e.g., "marketing_emails", "analytics", "cookies")
     */
    @Column(name = "consent_type", nullable = false, length = 100)
    private String consentType;
    
    /**
     * Purpose statement for the consent (GDPR requirement)
     */
    @Column(nullable = false, length = 1000)
    private String purpose;
    
    /**
     * Legal basis for data processing
     */
    @Column(name = "legal_basis", nullable = false, length = 50)
    private String legalBasis;
    
    /**
     * Whether consent was given (true) or withdrawn (false)
     */
    @Column(name = "consent_given", nullable = false)
    private boolean consentGiven;
    
    /**
     * Version of the consent text/policy
     */
    @Column(name = "consent_version", length = 20)
    private String consentVersion;
    
    /**
     * Source of the consent (e.g., "registration_form", "cookie_banner", "email_link")
     */
    @Column(name = "consent_source", length = 100)
    private String consentSource;
    
    /**
     * IP address from which consent was given/withdrawn
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    /**
     * User agent string from consent action
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * Consent expiration time (null = no expiration)
     */
    @Column(name = "expires_at")
    private Instant expiresAt;
    
    /**
     * Whether this consent record is currently active
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
    
    /**
     * When consent was withdrawn (if applicable)
     */
    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;
    
    /**
     * Reason for withdrawal
     */
    @Column(name = "withdrawal_reason", length = 500)
    private String withdrawalReason;
    
    /**
     * When this consent was superseded by a newer version
     */
    @Column(name = "superseded_at")
    private Instant supersededAt;
    
    /**
     * Parent consent record (for consent renewals/updates)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_consent_id")
    private ConsentRecord parentConsent;
    
    /**
     * Additional metadata about the consent
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "consent_record_metadata", 
                     joinColumns = @JoinColumn(name = "consent_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();
    
    /**
     * Geographic location where consent was given (for GDPR applicability)
     */
    @Column(name = "geographic_location", length = 100)
    private String geographicLocation;
    
    /**
     * Language in which consent was presented
     */
    @Column(name = "consent_language", length = 10)
    private String consentLanguage;
    
    /**
     * Method used to verify the user's identity when giving consent
     */
    @Column(name = "verification_method", length = 50)
    private String verificationMethod;
    
    // Audit fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    /**
     * Check if the consent is expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
    
    /**
     * Check if this is an effective consent (given, active, not expired, not withdrawn)
     */
    public boolean isEffectiveConsent() {
        return consentGiven && active && !isExpired() && withdrawnAt == null;
    }
    
    /**
     * Withdraw the consent
     */
    public void withdraw(String reason) {
        this.consentGiven = false;
        this.withdrawnAt = Instant.now();
        this.withdrawalReason = reason;
        this.active = false;
    }
    
    /**
     * Mark as superseded by a newer consent
     */
    public void markAsSuperseded() {
        this.active = false;
        this.supersededAt = Instant.now();
    }
    
    /**
     * Add metadata
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
     * Check if consent applies to a specific purpose
     */
    public boolean appliesTo(String targetPurpose) {
        return purpose != null && purpose.toLowerCase().contains(targetPurpose.toLowerCase());
    }
    
    /**
     * Check if this consent is renewable (has a version and is close to expiry)
     */
    public boolean isRenewable() {
        if (consentVersion == null || expiresAt == null) {
            return false;
        }
        
        // Consider renewable if expiring within 30 days
        Instant thirtyDaysFromNow = Instant.now().plusSeconds(30 * 24 * 3600);
        return expiresAt.isBefore(thirtyDaysFromNow);
    }
    
    /**
     * Create a renewal consent record based on this one
     */
    public ConsentRecord createRenewal(String newVersion, String newPurpose, Instant newExpiresAt) {
        return ConsentRecord.builder()
                .user(this.user)
                .consentType(this.consentType)
                .purpose(newPurpose != null ? newPurpose : this.purpose)
                .legalBasis(this.legalBasis)
                .consentGiven(true)
                .consentVersion(newVersion)
                .consentSource("renewal")
                .expiresAt(newExpiresAt)
                .parentConsent(this)
                .consentLanguage(this.consentLanguage)
                .build();
    }
    
    /**
     * Check if consent is GDPR-applicable based on geographic location
     */
    public boolean isGDPRApplicable() {
        if (geographicLocation == null) {
            return false;
        }
        
        // Simple check - in reality this would be more sophisticated
        String location = geographicLocation.toLowerCase();
        return location.contains("eu") || location.contains("europe") || 
               location.contains("germany") || location.contains("france") ||
               location.contains("uk") || location.contains("spain") ||
               location.contains("italy") || location.contains("poland");
    }
    
    /**
     * Get consent duration in days
     */
    public Long getConsentDurationDays() {
        if (createdAt == null) {
            return null;
        }
        
        Instant endTime = withdrawnAt != null ? withdrawnAt : 
                         (supersededAt != null ? supersededAt : Instant.now());
        
        return java.time.Duration.between(createdAt, endTime).toDays();
    }
    
    /**
     * Validate consent record completeness for legal compliance
     */
    public boolean isLegallyCompliant() {
        // Basic validation - in reality this would be more comprehensive
        return user != null && 
               consentType != null && !consentType.trim().isEmpty() &&
               purpose != null && !purpose.trim().isEmpty() &&
               legalBasis != null && !legalBasis.trim().isEmpty() &&
               createdAt != null;
    }
}