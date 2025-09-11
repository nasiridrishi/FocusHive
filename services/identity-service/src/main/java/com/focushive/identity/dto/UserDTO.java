package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User data transfer object.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    
    private String id;
    private String username;
    private String email;
    private String displayName;
    private boolean emailVerified;
    private boolean enabled;
    private boolean twoFactorEnabled;
    private String preferredLanguage;
    private String timezone;
    private Map<String, Boolean> notificationPreferences;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private List<PersonaDto> personas;
    
    /**
     * Custom builder to ensure defensive copying
     */
    public static UserDTOBuilder builder() {
        return new UserDTOBuilder();
    }
    
    /**
     * Custom builder class that creates defensive copies
     */
    public static class UserDTOBuilder {
        private String id;
        private String username;
        private String email;
        private String displayName;
        private boolean emailVerified;
        private boolean enabled;
        private boolean twoFactorEnabled;
        private String preferredLanguage;
        private String timezone;
        private Map<String, Boolean> notificationPreferences;
        private LocalDateTime createdAt;
        private LocalDateTime lastLoginAt;
        private List<PersonaDto> personas;
        
        public UserDTOBuilder id(String id) { this.id = id; return this; }
        public UserDTOBuilder username(String username) { this.username = username; return this; }
        public UserDTOBuilder email(String email) { this.email = email; return this; }
        public UserDTOBuilder displayName(String displayName) { this.displayName = displayName; return this; }
        public UserDTOBuilder emailVerified(boolean emailVerified) { this.emailVerified = emailVerified; return this; }
        public UserDTOBuilder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public UserDTOBuilder twoFactorEnabled(boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; return this; }
        public UserDTOBuilder preferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; return this; }
        public UserDTOBuilder timezone(String timezone) { this.timezone = timezone; return this; }
        public UserDTOBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public UserDTOBuilder lastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; return this; }
        
        public UserDTOBuilder notificationPreferences(Map<String, Boolean> notificationPreferences) {
            this.notificationPreferences = notificationPreferences != null ? new HashMap<>(notificationPreferences) : null;
            return this;
        }
        
        public UserDTOBuilder personas(List<PersonaDto> personas) {
            this.personas = personas != null ? new ArrayList<>(personas) : null;
            return this;
        }
        
        public UserDTO build() {
            UserDTO userDTO = new UserDTO();
            userDTO.id = this.id;
            userDTO.username = this.username;
            userDTO.email = this.email;
            userDTO.displayName = this.displayName;
            userDTO.emailVerified = this.emailVerified;
            userDTO.enabled = this.enabled;
            userDTO.twoFactorEnabled = this.twoFactorEnabled;
            userDTO.preferredLanguage = this.preferredLanguage;
            userDTO.timezone = this.timezone;
            userDTO.notificationPreferences = this.notificationPreferences;
            userDTO.createdAt = this.createdAt;
            userDTO.lastLoginAt = this.lastLoginAt;
            userDTO.personas = this.personas;
            return userDTO;
        }
    }
    
    /**
     * Setter for notification preferences that creates a defensive copy.
     */
    public void setNotificationPreferences(Map<String, Boolean> notificationPreferences) {
        this.notificationPreferences = notificationPreferences != null ? new HashMap<>(notificationPreferences) : null;
    }
    
    /**
     * Setter for personas that creates a defensive copy.
     */
    public void setPersonas(List<PersonaDto> personas) {
        this.personas = personas != null ? new ArrayList<>(personas) : null;
    }
    
    /**
     * Getter for notification preferences that returns a defensive copy.
     */
    public Map<String, Boolean> getNotificationPreferences() {
        return notificationPreferences != null ? new HashMap<>(notificationPreferences) : null;
    }
    
    /**
     * Getter for personas that returns a defensive copy.
     */
    public List<PersonaDto> getPersonas() {
        return personas != null ? new ArrayList<>(personas) : null;
    }
}