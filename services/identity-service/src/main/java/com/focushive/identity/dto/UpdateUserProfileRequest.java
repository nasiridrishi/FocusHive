package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * Request DTO for updating user profile information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Email(message = "Email should be valid")
    private String email;

    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @Size(max = 100, message = "Display name must not exceed 100 characters")
    private String displayName;

    @Size(max = 10, message = "Language code must not exceed 10 characters")
    private String preferredLanguage;

    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    private String timezone;

    private Map<String, Boolean> notificationPreferences;
}