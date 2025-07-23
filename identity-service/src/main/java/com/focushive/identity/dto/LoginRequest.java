package com.focushive.identity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for user login.
 */
@Data
public class LoginRequest {
    
    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    // Optional: Specific persona to activate on login
    private String personaId;
    
    // Optional: Remember me flag for longer refresh token validity
    private boolean rememberMe = false;
}