package com.focushive.identity.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validator for strong password requirements.
 * Implements OWASP guidelines for password security.
 */
@Slf4j
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private int minLength;
    private int maxLength;
    private boolean requireUppercase;
    private boolean requireLowercase;
    private boolean requireDigit;
    private boolean requireSpecialChar;
    private boolean checkCommonPasswords;

    // Common weak passwords to reject
    private static final Set<String> COMMON_PASSWORDS = new HashSet<>(Arrays.asList(
            "password", "123456", "123456789", "12345678", "12345", "1234567",
            "password123", "admin", "qwerty", "abc123", "letmein", "monkey",
            "1234567890", "dragon", "111111", "baseball", "iloveyou", "trustno1",
            "sunshine", "master", "123123", "welcome", "shadow", "ashley",
            "football", "jesus", "michael", "ninja", "mustang", "password1",
            "123qwe", "qwerty123", "admin123", "root", "toor", "pass", "test",
            "guest", "info", "adm", "mysql", "user", "administrator", "oracle",
            "ftp", "pi", "puppet", "ansible", "jenkins", "docker", "postgres",
            "redis", "mongodb", "elastic", "kibana", "grafana", "prometheus"
    ));

    // Patterns for character requirements
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~`]");

    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        this.minLength = constraintAnnotation.minLength();
        this.maxLength = constraintAnnotation.maxLength();
        this.requireUppercase = constraintAnnotation.requireUppercase();
        this.requireLowercase = constraintAnnotation.requireLowercase();
        this.requireDigit = constraintAnnotation.requireDigit();
        this.requireSpecialChar = constraintAnnotation.requireSpecialChar();
        this.checkCommonPasswords = constraintAnnotation.checkCommonPasswords();
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            setCustomErrorMessage(context, "Password cannot be null");
            return false;
        }

        // Check length requirements
        if (password.length() < minLength) {
            setCustomErrorMessage(context,
                String.format("Password must be at least %d characters long", minLength));
            return false;
        }

        if (password.length() > maxLength) {
            setCustomErrorMessage(context,
                String.format("Password must not exceed %d characters", maxLength));
            return false;
        }

        // Check character requirements
        if (requireUppercase && !UPPERCASE_PATTERN.matcher(password).find()) {
            setCustomErrorMessage(context, "Password must contain at least one uppercase letter");
            return false;
        }

        if (requireLowercase && !LOWERCASE_PATTERN.matcher(password).find()) {
            setCustomErrorMessage(context, "Password must contain at least one lowercase letter");
            return false;
        }

        if (requireDigit && !DIGIT_PATTERN.matcher(password).find()) {
            setCustomErrorMessage(context, "Password must contain at least one digit");
            return false;
        }

        if (requireSpecialChar && !SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            setCustomErrorMessage(context, "Password must contain at least one special character (!@#$%^&*()_+-=[]{}|;':\"\\<>?,.~`)");
            return false;
        }

        // Check against common passwords
        if (checkCommonPasswords && isCommonPassword(password)) {
            setCustomErrorMessage(context, "This password is too common and easily guessable. Please choose a more secure password");
            return false;
        }

        // Check for patterns (repeated characters, sequences)
        if (hasWeakPatterns(password)) {
            setCustomErrorMessage(context, "Password contains weak patterns. Avoid repeated characters or sequences");
            return false;
        }

        return true;
    }

    private boolean isCommonPassword(String password) {
        String lowerPassword = password.toLowerCase();

        // Direct match against common passwords
        if (COMMON_PASSWORDS.contains(lowerPassword)) {
            log.warn("Common password attempted: {}", sanitizeForLogging(password));
            return true;
        }

        // Check variations with numbers appended (password123, admin123, etc.)
        for (String commonPwd : COMMON_PASSWORDS) {
            if (lowerPassword.startsWith(commonPwd) &&
                lowerPassword.substring(commonPwd.length()).matches("\\d*")) {
                log.warn("Common password with number suffix attempted: {}", sanitizeForLogging(password));
                return true;
            }
        }

        return false;
    }

    private boolean hasWeakPatterns(String password) {
        // Check for repeated characters (more than 2 consecutive)
        if (password.matches(".*(..)\\1+.*")) {
            return true;
        }

        // Check for simple sequences
        String lowerPassword = password.toLowerCase();
        String[] sequences = {
            "abcdefg", "1234567", "qwertyuiop", "asdfghjkl", "zxcvbnm",
            "!@#$%^&", "()_+-=", "[]{}|;", "0987654321"
        };

        for (String sequence : sequences) {
            if (lowerPassword.contains(sequence.substring(0, Math.min(4, sequence.length())))) {
                return true;
            }
        }

        return false;
    }

    private void setCustomErrorMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
    }

    /**
     * Sanitize password for logging to prevent sensitive data exposure.
     */
    private String sanitizeForLogging(String password) {
        if (password == null || password.length() <= 2) {
            return "[REDACTED]";
        }
        // Show only first and last character with * in between
        return password.charAt(0) + "*".repeat(password.length() - 2) + password.charAt(password.length() - 1);
    }
}