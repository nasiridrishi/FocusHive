package com.focushive.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Privacy Setting entity for storing granular privacy preferences.
 * Supports both user-level and persona-level privacy controls.
 */
@Entity
@Table(name = "privacy_settings", 
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_privacy_user_category_key", 
                           columnNames = {"user_id", "persona_id", "category", "setting_key"})
       },
       indexes = {
           @Index(name = "idx_privacy_setting_user", columnList = "user_id"),
           @Index(name = "idx_privacy_setting_persona", columnList = "persona_id"),
           @Index(name = "idx_privacy_setting_category", columnList = "category"),
           @Index(name = "idx_privacy_setting_enabled", columnList = "enabled")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"user", "persona"})
@ToString(exclude = {"user", "persona"})
public class PrivacySetting {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * User who owns this privacy setting
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * Persona for which this setting applies (optional - null means user-level)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id")
    private Persona persona;
    
    /**
     * Category of the privacy setting (e.g., "profile_visibility", "data_sharing")
     */
    @Column(nullable = false, length = 50)
    private String category;
    
    /**
     * Specific setting key within the category
     */
    @Column(name = "setting_key", nullable = false, length = 100)
    private String settingKey;
    
    /**
     * Setting value (can be boolean, string, JSON, etc.)
     */
    @Column(name = "setting_value", nullable = false, length = 1000)
    private String settingValue;
    
    /**
     * Human-readable description of what this setting controls
     */
    @Column(length = 500)
    private String description;
    
    /**
     * Whether this setting is currently enabled/active
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;
    
    /**
     * Data type of the setting value for validation
     */
    @Column(name = "data_type", length = 20)
    @Builder.Default
    private String dataType = "STRING"; // STRING, BOOLEAN, NUMBER, JSON
    
    /**
     * Priority level for conflict resolution (higher numbers take precedence)
     */
    @Column(name = "priority_level")
    @Builder.Default
    private Integer priorityLevel = 0;
    
    /**
     * Whether this setting can be overridden by persona-specific settings
     */
    @Column(name = "overridable")
    @Builder.Default
    private boolean overridable = true;
    
    /**
     * Source of this setting (user, admin, system, etc.)
     */
    @Column(length = 20)
    @Builder.Default
    private String source = "user";
    
    // Audit fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    /**
     * Check if this is a user-level setting (not persona-specific)
     */
    public boolean isUserLevel() {
        return persona == null;
    }
    
    /**
     * Check if this is a persona-specific setting
     */
    public boolean isPersonaLevel() {
        return persona != null;
    }
    
    /**
     * Get the boolean value of the setting
     */
    public Boolean getBooleanValue() {
        if ("BOOLEAN".equals(dataType)) {
            return Boolean.parseBoolean(settingValue);
        }
        return null;
    }
    
    /**
     * Get the numeric value of the setting
     */
    public Double getNumericValue() {
        if ("NUMBER".equals(dataType)) {
            try {
                return Double.parseDouble(settingValue);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * Set a boolean value
     */
    public void setBooleanValue(boolean value) {
        this.settingValue = String.valueOf(value);
        this.dataType = "BOOLEAN";
    }
    
    /**
     * Set a numeric value
     */
    public void setNumericValue(Number value) {
        this.settingValue = String.valueOf(value);
        this.dataType = "NUMBER";
    }
    
    /**
     * Check if setting value is valid for its data type
     */
    public boolean isValidValue() {
        if (settingValue == null) {
            return false;
        }
        
        switch (dataType) {
            case "BOOLEAN":
                return "true".equalsIgnoreCase(settingValue) || "false".equalsIgnoreCase(settingValue);
            case "NUMBER":
                try {
                    Double.parseDouble(settingValue);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            case "JSON":
                // In a real implementation, you'd validate JSON structure
                return settingValue.trim().startsWith("{") || settingValue.trim().startsWith("[");
            case "STRING":
            default:
                return true;
        }
    }
    
    /**
     * Create a copy of this setting for a different persona
     */
    public PrivacySetting copyForPersona(Persona targetPersona) {
        return PrivacySetting.builder()
                .user(this.user)
                .persona(targetPersona)
                .category(this.category)
                .settingKey(this.settingKey)
                .settingValue(this.settingValue)
                .description(this.description)
                .enabled(this.enabled)
                .dataType(this.dataType)
                .priorityLevel(this.priorityLevel)
                .overridable(this.overridable)
                .source("copied")
                .build();
    }
}