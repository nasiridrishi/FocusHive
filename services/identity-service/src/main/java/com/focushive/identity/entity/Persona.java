package com.focushive.identity.entity;

import com.focushive.identity.security.encryption.IEncryptionService;
import com.focushive.identity.security.encryption.converters.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persona entity representing different profiles/contexts for a user.
 * Each user can have multiple personas (work, personal, gaming, etc.)
 */
@Entity
@Table(name = "personas", 
       indexes = {
           @Index(name = "idx_persona_user", columnList = "user_id"),
           @Index(name = "idx_persona_active", columnList = "user_id, is_active")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_persona_user_name", columnNames = {"user_id", "name"})
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString(exclude = {"user"})
public class Persona extends BaseEncryptedEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, length = 50)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PersonaType type;
    
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = false;
    
    @Column(name = "display_name", length = 255)
    @Convert(converter = EncryptedStringConverter.class)
    private String displayName;
    
    @Column(name = "avatar_url")
    private String avatarUrl;
    
    @Column(length = 1000)
    @Convert(converter = EncryptedStringConverter.class)
    private String bio;
    
    @Column(name = "status_message", length = 500)
    @Convert(converter = EncryptedStringConverter.class)
    private String statusMessage;
    
    // Privacy settings as embedded object
    @Embedded
    @Builder.Default
    private PrivacySettings privacySettings = new PrivacySettings();
    
    // Custom attributes for the persona
    @ElementCollection
    @CollectionTable(name = "persona_attributes", 
                     joinColumns = @JoinColumn(name = "persona_id"))
    @MapKeyColumn(name = "attribute_key")
    @Column(name = "attribute_value")
    @org.hibernate.annotations.BatchSize(size = 16)
    @Convert(converter = EncryptedStringMapConverter.class, attributeName = "value")
    @Builder.Default
    private Map<String, String> customAttributes = new HashMap<>();
    
    // Notification preferences specific to this persona
    @Column(name = "notification_preferences")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Convert(converter = EncryptedBooleanMapConverter.class)
    @Builder.Default
    private Map<String, Boolean> notificationPreferences = new HashMap<>();
    
    // Theme and UI preferences
    @Column(name = "theme_preference", length = 20)
    @Builder.Default
    private String themePreference = "system";
    
    @Column(name = "language_preference", length = 10)
    private String languagePreference;
    
    // Audit fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "last_active_at")
    private Instant lastActiveAt;
    
    /**
     * Types of personas available.
     */
    public enum PersonaType {
        WORK("Work Profile"),
        PERSONAL("Personal Profile"),
        GAMING("Gaming Profile"),
        STUDY("Study Profile"),
        CUSTOM("Custom Profile");
        
        private final String displayName;
        
        PersonaType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Privacy settings for a persona.
     */
    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrivacySettings implements java.io.Serializable {
        
        @Builder.Default
        @Column(name = "show_real_name", nullable = false)
        private boolean showRealName = false;
        
        @Builder.Default
        @Column(name = "show_email", nullable = false)
        private boolean showEmail = false;
        
        @Builder.Default
        @Column(name = "show_activity", nullable = false)
        private boolean showActivity = true;
        
        @Builder.Default
        @Column(name = "allow_direct_messages", nullable = false)
        private boolean allowDirectMessages = true;
        
        @Builder.Default
        @Column(name = "visibility_level", nullable = false, length = 20)
        private String visibilityLevel = "FRIENDS"; // PUBLIC, FRIENDS, PRIVATE
        
        @Builder.Default
        @Column(name = "searchable", nullable = false)
        private boolean searchable = true;
        
        @Builder.Default
        @Column(name = "show_online_status", nullable = false)
        private boolean showOnlineStatus = true;
        
        @Builder.Default
        @Column(name = "share_focus_sessions", nullable = false)
        private boolean shareFocusSessions = true;
        
        @Builder.Default
        @Column(name = "share_achievements", nullable = false)
        private boolean shareAchievements = true;
    }
    
    /**
     * Getter for isActive field - explicitly defined to avoid Lombok issues with boolean fields starting with "is"
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Getter for isDefault field - explicitly defined to avoid Lombok issues with boolean fields starting with "is"
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Activate this persona and deactivate others for the user.
     */
    @PrePersist
    @PreUpdate
    public void ensureSingleActivePersona() {
        if (isActive && user != null) {
            // This will be handled by the repository method
            // to ensure only one active persona per user
        }
    }
    
    /**
     * Update searchable hashes for encrypted fields.
     * Called before persisting or updating the entity.
     */
    @Override
    protected void updateSearchableHashes(IEncryptionService encryptionService) {
        // Persona doesn't have any searchable encrypted fields
        // All PII fields use regular encryption without search capability
        // This is intentional as personas are looked up by user relationship, not by content
    }
}