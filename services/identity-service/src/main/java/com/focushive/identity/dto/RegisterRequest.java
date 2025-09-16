package com.focushive.identity.dto;

import com.focushive.identity.annotation.InjectionSafeString;
import com.focushive.identity.validation.LDAPInjectionSafe;
import com.focushive.identity.validation.NoSQLInjectionSafe;
import com.focushive.identity.validation.SQLInjectionSafe;
import com.focushive.identity.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for user registration.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @InjectionSafeString
    @SQLInjectionSafe(message = "Email contains potentially unsafe SQL characters")
    @NoSQLInjectionSafe(message = "Email contains potentially unsafe NoSQL patterns")
    @LDAPInjectionSafe(message = "Email contains potentially unsafe LDAP characters")
    private String email;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @InjectionSafeString
    @SQLInjectionSafe(message = "Username contains potentially unsafe SQL characters")
    @NoSQLInjectionSafe(message = "Username contains potentially unsafe NoSQL patterns")
    @LDAPInjectionSafe(message = "Username contains potentially unsafe LDAP characters")
    private String username;

    @NotBlank(message = "Password is required")
    @StrongPassword(
        minLength = 8,
        maxLength = 128,
        requireUppercase = true,
        requireLowercase = true,
        requireDigit = true,
        requireSpecialChar = true,
        checkCommonPasswords = true
    )
    private String password;

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    @InjectionSafeString
    @SQLInjectionSafe(message = "First name contains potentially unsafe SQL characters")
    @NoSQLInjectionSafe(message = "First name contains potentially unsafe NoSQL patterns")
    @LDAPInjectionSafe(message = "First name contains potentially unsafe LDAP characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    @InjectionSafeString
    @SQLInjectionSafe(message = "Last name contains potentially unsafe SQL characters")
    @NoSQLInjectionSafe(message = "Last name contains potentially unsafe NoSQL patterns")
    @LDAPInjectionSafe(message = "Last name contains potentially unsafe LDAP characters")
    private String lastName;

    // Optional: Initial persona type (defaults to PERSONAL if not provided)
    @Size(max = 20, message = "Persona type must not exceed 20 characters")
    @InjectionSafeString
    @SQLInjectionSafe(message = "Persona type contains potentially unsafe SQL characters")
    @NoSQLInjectionSafe(message = "Persona type contains potentially unsafe NoSQL patterns")
    private String personaType = "PERSONAL";

    // Optional: Initial persona name (defaults to username if not provided)
    @Size(max = 100, message = "Persona name must not exceed 100 characters")
    @InjectionSafeString
    @SQLInjectionSafe(message = "Persona name contains potentially unsafe SQL characters")
    @NoSQLInjectionSafe(message = "Persona name contains potentially unsafe NoSQL patterns")
    @LDAPInjectionSafe(message = "Persona name contains potentially unsafe LDAP characters")
    private String personaName;
}