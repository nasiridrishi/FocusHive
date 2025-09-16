package com.focushive.identity.dto;

import com.focushive.identity.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for confirming password reset with token.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetConfirmRequest {

    @NotBlank(message = "Reset token is required")
    private String token;

    @NotBlank(message = "New password is required")
    @StrongPassword(
        minLength = 8,
        maxLength = 128,
        requireUppercase = true,
        requireLowercase = true,
        requireDigit = true,
        requireSpecialChar = true,
        checkCommonPasswords = true
    )
    private String newPassword;
    
    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
}