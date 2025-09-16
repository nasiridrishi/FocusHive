package com.focushive.notification.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation annotation to prevent XSS attacks.
 * Validates that the string does not contain potentially dangerous HTML/JavaScript.
 */
@Documented
@Constraint(validatedBy = XSSSafeValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface XSSSafe {
    
    /**
     * Default error message.
     */
    String message() default "Input contains potentially dangerous content (XSS detected)";
    
    /**
     * Validation groups.
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for additional metadata.
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Whether to allow basic HTML tags like <b>, <i>, <p>, etc.
     */
    boolean allowBasicHtml() default false;
}