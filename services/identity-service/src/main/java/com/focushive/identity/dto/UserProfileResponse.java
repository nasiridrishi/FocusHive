package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for user profile information.
 * Contains non-sensitive user data only.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private UUID userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String displayName;
    private boolean emailVerified;
    private boolean twoFactorEnabled;
    private String preferredLanguage;
    private String timezone;
    private Map<String, Boolean> notificationPreferences;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime updatedAt;
    private List<PersonaDto> personas;
}