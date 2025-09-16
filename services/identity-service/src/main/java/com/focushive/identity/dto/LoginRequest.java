package com.focushive.identity.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.focushive.identity.annotation.InjectionSafeString;
import com.focushive.identity.validation.LDAPInjectionSafe;
import com.focushive.identity.validation.NoSQLInjectionSafe;
import com.focushive.identity.validation.SQLInjectionSafe;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for user login.
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Username or email is required")
    @Size(max = 255, message = "Username or email must not exceed 255 characters")
    @InjectionSafeString
    @SQLInjectionSafe(message = "Username contains potentially unsafe SQL characters")
    @NoSQLInjectionSafe(message = "Username contains potentially unsafe NoSQL patterns")
    @LDAPInjectionSafe(message = "Username contains potentially unsafe LDAP characters")
    @JsonAlias({"email", "username"})  // Accept "email" or "username" as aliases
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    @Size(max = 1000, message = "Password must not exceed 1000 characters")
    private String password;

    // Optional: Specific persona to activate on login
    @Size(max = 36, message = "Persona ID must not exceed 36 characters")
    @InjectionSafeString
    @SQLInjectionSafe(message = "Persona ID contains potentially unsafe SQL characters")
    @NoSQLInjectionSafe(message = "Persona ID contains potentially unsafe NoSQL patterns")
    private String personaId;

    // Optional: Remember me flag for longer refresh token validity
    private boolean rememberMe = false;
}