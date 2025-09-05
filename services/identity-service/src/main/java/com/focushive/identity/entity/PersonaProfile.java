package com.focushive.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * PersonaProfile entity for storing detailed profile information for each persona.
 * Provides flexible key-value storage with metadata and validation.
 */
@Entity
@Table(name = "persona_profiles", 
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_persona_profile_key", 
                           columnNames = {"persona_id", "profile_key"})
       },
       indexes = {
           @Index(name = "idx_persona_profile_persona", columnList = "persona_id"),
           @Index(name = "idx_persona_profile_category", columnList = "category"),
           @Index(name = "idx_persona_profile_visibility", columnList = "visibility"),
           @Index(name = "idx_persona_profile_enabled", columnList = "enabled"),
           @Index(name = "idx_persona_profile_data_type", columnList = "data_type")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"persona"})
@ToString(exclude = {"persona"})
public class PersonaProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Persona this profile belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;
    
    /**
     * Profile key/field name
     */
    @Column(name = "profile_key", nullable = false, length = 100)
    private String profileKey;
    
    /**
     * Profile value (can store any type of data as string)
     */
    @Column(name = "profile_value", nullable = false, length = 2000)
    private String profileValue;
    
    /**
     * Category for grouping profile fields
     */
    @Column(length = 50)
    private String category;
    
    /**
     * Data type for validation and parsing
     */
    @Column(name = "data_type", nullable = false, length = 20)
    @Builder.Default
    private String dataType = "STRING"; // STRING, NUMBER, BOOLEAN, DATE, JSON, EMAIL, PHONE, URL
    
    /**
     * Visibility level for this profile field
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String visibility = "PRIVATE"; // PUBLIC, FRIENDS, PRIVATE
    
    /**
     * Human-readable description of this profile field
     */
    @Column(length = 500)
    private String description;
    
    /**
     * Whether this profile field is currently enabled
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;
    
    /**
     * Priority/order for display purposes
     */
    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;
    
    /**
     * Whether this field is required for the persona
     */
    @Column(name = "required_field")
    @Builder.Default
    private boolean requiredField = false;
    
    /**
     * Whether this field is editable by the user
     */
    @Column(name = "user_editable")
    @Builder.Default
    private boolean userEditable = true;
    
    /**
     * Source of this profile data
     */
    @Column(length = 50)
    @Builder.Default
    private String source = "user_input"; // user_input, imported, calculated, system
    
    /**
     * Additional metadata for this profile field
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "persona_profile_metadata", 
                     joinColumns = @JoinColumn(name = "profile_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();
    
    /**
     * Validation rules for this profile field
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "persona_profile_validation", 
                     joinColumns = @JoinColumn(name = "profile_id"))
    @MapKeyColumn(name = "rule_key")
    @Column(name = "rule_value")
    @Builder.Default
    private Map<String, String> validationRules = new HashMap<>();
    
    /**
     * Source metadata (how this data was obtained)
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "persona_profile_source_metadata", 
                     joinColumns = @JoinColumn(name = "profile_id"))
    @MapKeyColumn(name = "source_key")
    @Column(name = "source_value")
    @Builder.Default
    private Map<String, String> sourceMetadata = new HashMap<>();
    
    /**
     * When this profile field was last verified/validated
     */
    @Column(name = "verified_at")
    private Instant verifiedAt;
    
    /**
     * Method used to verify this profile field
     */
    @Column(name = "verification_method", length = 50)
    private String verificationMethod;
    
    // Audit fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    /**
     * Get the boolean value of the profile
     */
    public Boolean getBooleanValue() {
        if ("BOOLEAN".equals(dataType)) {
            return Boolean.parseBoolean(profileValue);
        }
        return null;
    }
    
    /**
     * Get the numeric value of the profile
     */
    public Double getNumericValue() {
        if ("NUMBER".equals(dataType)) {
            try {
                return Double.parseDouble(profileValue);
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
        this.profileValue = String.valueOf(value);
        this.dataType = "BOOLEAN";
    }
    
    /**
     * Set a numeric value
     */
    public void setNumericValue(Number value) {
        this.profileValue = String.valueOf(value);
        this.dataType = "NUMBER";
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
     * Add validation rule
     */
    public void addValidationRule(String rule, String value) {
        if (validationRules == null) {
            validationRules = new HashMap<>();
        }
        validationRules.put(rule, value);
    }
    
    /**
     * Get validation rule
     */
    public String getValidationRule(String rule) {
        return validationRules != null ? validationRules.get(rule) : null;
    }
    
    /**
     * Add source metadata
     */
    public void addSourceMetadata(String key, String value) {
        if (sourceMetadata == null) {
            sourceMetadata = new HashMap<>();
        }
        sourceMetadata.put(key, value);
    }
    
    /**
     * Get source metadata
     */
    public String getSourceMetadata(String key) {
        return sourceMetadata != null ? sourceMetadata.get(key) : null;
    }
    
    /**
     * Check if profile value is valid for its data type
     */
    public boolean isValidValue() {
        if (profileValue == null || profileValue.trim().isEmpty()) {
            return !requiredField; // Empty is valid only if not required
        }
        
        switch (dataType.toUpperCase()) {
            case "BOOLEAN":
                return "true".equalsIgnoreCase(profileValue.trim()) || 
                       "false".equalsIgnoreCase(profileValue.trim());
            case "NUMBER":
                try {
                    Double.parseDouble(profileValue.trim());
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            case "EMAIL":
                return profileValue.trim().matches("^[A-Za-z0-9+_.-]+@(.+)$");
            case "PHONE":
                return profileValue.trim().matches("^[+]?[0-9\\s\\-\\(\\)]+$");
            case "URL":
                return profileValue.trim().matches("^https?://.*");
            case "DATE":
                try {
                    Instant.parse(profileValue.trim());
                    return true;
                } catch (Exception e) {
                    return false;
                }
            case "JSON":
                return profileValue.trim().startsWith("{") || profileValue.trim().startsWith("[");
            case "STRING":
            default:
                return true;
        }
    }
    
    /**
     * Check if this profile is publicly visible
     */
    public boolean isPublic() {
        return "PUBLIC".equals(visibility);
    }
    
    /**
     * Check if this profile is visible to friends
     */
    public boolean isFriendsVisible() {
        return "FRIENDS".equals(visibility) || isPublic();
    }
    
    /**
     * Check if this profile is private
     */
    public boolean isPrivate() {
        return "PRIVATE".equals(visibility);
    }
    
    /**
     * Check if this profile field is verified
     */
    public boolean isVerified() {
        return verifiedAt != null && verificationMethod != null;
    }
    
    /**
     * Mark this profile field as verified
     */
    public void markAsVerified(String method) {
        this.verifiedAt = Instant.now();
        this.verificationMethod = method;
    }
    
    /**
     * Check if this profile field needs verification
     */
    public boolean needsVerification() {
        return ("EMAIL".equals(dataType) || "PHONE".equals(dataType)) && !isVerified();
    }
    
    /**
     * Validate against all configured rules
     */
    public boolean passesValidation() {
        if (!isValidValue()) {
            return false;
        }
        
        if (validationRules == null || validationRules.isEmpty()) {
            return true;
        }
        
        // Check specific validation rules
        for (Map.Entry<String, String> rule : validationRules.entrySet()) {
            if (!validateRule(rule.getKey(), rule.getValue())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Validate a specific rule
     */
    private boolean validateRule(String ruleKey, String ruleValue) {
        switch (ruleKey.toLowerCase()) {
            case "min_length":
                try {
                    int minLength = Integer.parseInt(ruleValue);
                    return profileValue.length() >= minLength;
                } catch (NumberFormatException e) {
                    return true;
                }
            case "max_length":
                try {
                    int maxLength = Integer.parseInt(ruleValue);
                    return profileValue.length() <= maxLength;
                } catch (NumberFormatException e) {
                    return true;
                }
            case "pattern":
                try {
                    return profileValue.matches(ruleValue);
                } catch (Exception e) {
                    return true;
                }
            case "required":
                return !"true".equals(ruleValue) || (profileValue != null && !profileValue.trim().isEmpty());
            default:
                return true; // Unknown rules pass by default
        }
    }
    
    /**
     * Create a copy of this profile for another persona
     */
    public PersonaProfile copyForPersona(Persona targetPersona) {
        return PersonaProfile.builder()
                .persona(targetPersona)
                .profileKey(this.profileKey)
                .profileValue(this.profileValue)
                .category(this.category)
                .dataType(this.dataType)
                .visibility(this.visibility)
                .description(this.description)
                .enabled(this.enabled)
                .displayOrder(this.displayOrder)
                .requiredField(this.requiredField)
                .userEditable(this.userEditable)
                .source("copied")
                .metadata(new HashMap<>(this.metadata != null ? this.metadata : new HashMap<>()))
                .validationRules(new HashMap<>(this.validationRules != null ? this.validationRules : new HashMap<>()))
                .build();
    }
    
    /**
     * Get display value (formatted for presentation)
     */
    public String getDisplayValue() {
        if (profileValue == null) {
            return "";
        }
        
        // For sensitive data types, show masked values when not verified
        if ("EMAIL".equals(dataType) && !isVerified()) {
            return maskEmail(profileValue);
        } else if ("PHONE".equals(dataType) && !isVerified()) {
            return maskPhone(profileValue);
        }
        
        return profileValue;
    }
    
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        String username = parts[0];
        String domain = parts[1];
        
        if (username.length() <= 2) {
            return "*@" + domain;
        }
        
        return username.charAt(0) + "***" + username.charAt(username.length() - 1) + "@" + domain;
    }
    
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "***";
        }
        
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 4) {
            return "***";
        }
        
        return "***-***-" + digits.substring(digits.length() - 4);
    }
}