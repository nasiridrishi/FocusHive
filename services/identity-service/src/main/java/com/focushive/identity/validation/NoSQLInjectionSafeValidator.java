package com.focushive.identity.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * Validator for NoSQL injection safe strings.
 * Detects and rejects common NoSQL injection patterns including:
 * - JSON objects in string fields
 * - MongoDB operators ($ne, $gt, $regex, etc.)
 * - JavaScript code patterns
 * - Boolean and null literal injections
 */
@Slf4j
public class NoSQLInjectionSafeValidator implements ConstraintValidator<NoSQLInjectionSafe, String> {

    private boolean allowEmpty;

    // Pattern to detect JSON object structure
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile(
            "\\{\\s*[\"']?\\$?\\w+[\"']?\\s*:\\s*.*\\}",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Pattern to detect MongoDB operators
    private static final Pattern MONGO_OPERATOR_PATTERN = Pattern.compile(
            "\\$(?:ne|eq|gt|gte|lt|lte|in|nin|exists|type|mod|regex|where|all|size|slice|elemMatch|not|or|and|nor)",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to detect JavaScript injection attempts
    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile(
            "(?:javascript:|function\\s*\\(|eval\\s*\\(|setTimeout\\s*\\(|setInterval\\s*\\()",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to detect literal boolean/null injections
    private static final Pattern LITERAL_INJECTION_PATTERN = Pattern.compile(
            "^\\s*(?:true|false|null)\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public void initialize(NoSQLInjectionSafe constraintAnnotation) {
        this.allowEmpty = constraintAnnotation.allowEmpty();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Allow null/empty values if configured
        if (value == null || value.trim().isEmpty()) {
            return allowEmpty;
        }

        // Check for JSON object patterns
        if (JSON_OBJECT_PATTERN.matcher(value).find()) {
            log.warn("NoSQL injection attempt detected: JSON object pattern in string field: {}",
                    sanitizeForLogging(value));
            setCustomErrorMessage(context, "JSON objects are not allowed in this field");
            return false;
        }

        // Check for MongoDB operators
        if (MONGO_OPERATOR_PATTERN.matcher(value).find()) {
            log.warn("NoSQL injection attempt detected: MongoDB operator pattern in string field: {}",
                    sanitizeForLogging(value));
            setCustomErrorMessage(context, "Database operators are not allowed in this field");
            return false;
        }

        // Check for JavaScript injection
        if (JAVASCRIPT_PATTERN.matcher(value).find()) {
            log.warn("NoSQL injection attempt detected: JavaScript pattern in string field: {}",
                    sanitizeForLogging(value));
            setCustomErrorMessage(context, "JavaScript code is not allowed in this field");
            return false;
        }

        // Check for literal boolean/null injections
        if (LITERAL_INJECTION_PATTERN.matcher(value).matches()) {
            log.warn("NoSQL injection attempt detected: literal injection pattern in string field: {}",
                    sanitizeForLogging(value));
            setCustomErrorMessage(context, "Literal values (true/false/null) are not allowed as string inputs");
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