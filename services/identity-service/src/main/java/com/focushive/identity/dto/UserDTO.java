package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * User data transfer object.
 */
@Data
@Builder
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
}