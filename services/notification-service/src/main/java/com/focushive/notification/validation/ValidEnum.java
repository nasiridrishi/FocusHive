package com.focushive.notification.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation annotation to validate enum values.
 * Checks if the provided string value is a valid enum constant.
 */
@Documented
@Constraint(validatedBy = ValidEnumValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEnum {
    
    /**
     * The enum class to validate against.
     */
    Class<? extends Enum<?>> enumClass();
    
    /**
     * Default error message.
     */
    String message() default "Invalid enum value. Accepted values are: {acceptedValues}";
    
    /**
     * Validation groups.
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for additional metadata.
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Whether to ignore case when comparing values.
     */
    boolean ignoreCase() default false;
}