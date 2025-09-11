package com.focushive.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;
    
    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
}