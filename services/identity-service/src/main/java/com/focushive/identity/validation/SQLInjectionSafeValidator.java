package com.focushive.identity.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * Validator for SQL injection safe strings.
 * Detects and rejects common SQL injection patterns including:
 * - SQL keywords and operators
 * - Comment sequences
 * - Union attacks
 * - Boolean-based injection patterns
 */
@Slf4j
public class SQLInjectionSafeValidator implements ConstraintValidator<SQLInjectionSafe, String> {

    private boolean allowEmpty;

    // Pattern to detect SQL injection keywords
    private static final Pattern SQL_KEYWORDS_PATTERN = Pattern.compile(
            "(?:\\b(?:SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|EXECUTE|UNION|OR|AND|XOR|NOT|NULL|TRUE|FALSE|SLEEP|BENCHMARK|WAITFOR|DELAY)\\b|" +
            // SQL operators and special characters
            "(?:--|#|/\\*|\\*/|;|\\||'|\"|`|\\\\|=|<|>|!|&|\\^|~|\\+|\\*|%|\\(|\\)|\\[|\\]|\\{|\\}))",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to detect classic SQL injection patterns
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?:'\\s*(?:OR|AND)\\s*'|'\\s*=\\s*'|1\\s*=\\s*1|1'\\s*=\\s*'1|'\\s*OR\\s*1\\s*=\\s*1|" +
            "'\\s*UNION\\s*SELECT|'\\s*;\\s*DROP|'\\s*;\\s*DELETE|'\\s*;\\s*UPDATE|'\\s*;\\s*INSERT)",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to detect comment-based injection
    private static final Pattern SQL_COMMENT_PATTERN = Pattern.compile(
            "(?:--|/\\*.*?\\*/|#.*?(?:\\n|$))",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    @Override
    public void initialize(SQLInjectionSafe constraintAnnotation) {
        this.allowEmpty = constraintAnnotation.allowEmpty();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Allow null/empty values if configured
        if (value == null || value.trim().isEmpty()) {
            return allowEmpty;
        }

        // Check for SQL keywords and operators
        if (SQL_KEYWORDS_PATTERN.matcher(value).find()) {
            log.warn("SQL injection attempt detected: SQL keyword/operator pattern in field: {}",
                    sanitizeForLogging(value));
            setCustomErrorMessage(context, "SQL keywords and operators are not allowed in this field");
            return false;
        }

        // Check for classic SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(value).find()) {
            log.warn("SQL injection attempt detected: classic injection pattern in field: {}",
                    sanitizeForLogging(value));
            setCustomErrorMessage(context, "SQL injection patterns are not allowed in this field");
            return false;
        }

        // Check for SQL comments
        if (SQL_COMMENT_PATTERN.matcher(value).find()) {
            log.warn("SQL injection attempt detected: comment pattern in field: {}",
                    sanitizeForLogging(value));
            setCustomErrorMessage(context, "SQL comments are not allowed in this field");
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