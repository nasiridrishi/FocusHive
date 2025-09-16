package com.focushive.identity.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * Validator for LDAP injection safe strings.
 * Detects and rejects common LDAP injection patterns including:
 * - LDAP special characters: *, (, ), \, NUL, /
 * - LDAP filter syntax
 * - Unicode escapes that could be used for injection
 */
@Slf4j
public class LDAPInjectionSafeValidator implements ConstraintValidator<LDAPInjectionSafe, String> {

    private boolean allowEmpty;

    // LDAP special characters that can be used for injection
    private static final String LDAP_SPECIAL_CHARS = "*()\\\u0000/";

    // Pattern to detect LDAP filter syntax
    private static final Pattern LDAP_FILTER_PATTERN = Pattern.compile(
            "(?:\\*\\)|\\(\\w+\\s*[=<>!~]|\\|\\s*\\(|&\\s*\\()",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to detect LDAP boolean operators
    private static final Pattern LDAP_BOOLEAN_PATTERN = Pattern.compile(
            "(?:^|\\W)(?:&|\\||!)\\s*\\(",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to detect Unicode escape sequences that could be used for injection
    private static final Pattern UNICODE_ESCAPE_PATTERN = Pattern.compile(
            "\\\\u[0-9a-fA-F]{4}|\\\\x[0-9a-fA-F]{2}",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public void initialize(LDAPInjectionSafe constraintAnnotation) {
        this.allowEmpty = constraintAnnotation.allowEmpty();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Allow null/empty values if configured
        if (value == null || value.trim().isEmpty()) {
            return allowEmpty;
        }

        // Check for LDAP special characters
        for (char specialChar : LDAP_SPECIAL_CHARS.toCharArray()) {
            if (value.indexOf(specialChar) != -1) {
                log.warn("LDAP injection attempt detected: special character '{}' in field: {}",
                        specialChar, sanitizeForLogging(value));
                setCustomErrorMessage(context, "LDAP special characters are not allowed in this field");
                return false;
            }
        }

        // Check for LDAP filter syntax
        if (LDAP_FILTER_PATTERN.matcher(value).find()) {
            log.warn("LDAP injection attempt detected: filter syntax pattern in field: {}",
                    sanitizeForLogging(value));
            setCustomErrorMessage(context, "LDAP filter syntax is not allowed in this field");
            return false;
        }

        // Check for LDAP boolean operators
        if (LDAP_BOOLEAN_PATTERN.matcher(value).find()) {
            log.warn("LDAP injection attempt detected: boolean operator pattern in field: {}",
                    sanitizeForLogging(value));
            setCustomErrorMessage(context, "LDAP boolean operators are not allowed in this field");
            return false;
        }

        // Check for Unicode escape sequences
        if (UNICODE_ESCAPE_PATTERN.matcher(value).find()) {
            log.warn("LDAP injection attempt detected: Unicode escape sequence in field: {}",
                    sanitizeForLogging(value));
            setCustomErrorMessage(context, "Unicode escape sequences are not allowed in this field");
            return false;
        }

        return true;
    }

    private void setCustomErrorMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
    }

    /**
     * Sanitize input for logging to prevent log injection.
     */
    private String sanitizeForLogging(String input) {
        if (input == null) {
            return "null";
        }
        // Truncate long inputs and remove newlines
        String sanitized = input.replaceAll("[\r\n\t]", " ");
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100) + "...";
        }
        return sanitized;
    }
}