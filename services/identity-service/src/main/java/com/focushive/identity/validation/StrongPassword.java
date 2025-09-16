package com.focushive.identity.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that a password meets strong security requirements:
 * - Minimum 8 characters
 * - At least one uppercase letter
 * - At least one lowercase letter
 * - At least one digit
 * - At least one special character
 * - Not a common weak password
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "Password must be at least 8 characters with uppercase, lowercase, number, and special character";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Minimum length requirement.
     */
    int minLength() default 8;

    /**
     * Maximum length requirement.
     */
    int maxLength() default 128;

    /**
     * Require at least one uppercase letter.
     */
    boolean requireUppercase() default true;

    /**
     * Require at least one lowercase letter.
     */
    boolean requireLowercase() default true;

    /**
     * Require at least one digit.
     */
    boolean requireDigit() default true;

    /**
     * Require at least one special character.
     */
    boolean requireSpecialChar() default true;

    /**
     * Check against common weak passwords.
     */
    boolean checkCommonPasswords() default true;
}