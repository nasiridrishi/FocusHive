package com.focushive.identity.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that a string field does not contain NoSQL injection patterns.
 * This annotation prevents JSON objects and MongoDB operators in string fields.
 */
@Documented
@Constraint(validatedBy = NoSQLInjectionSafeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoSQLInjectionSafe {

    String message() default "Field contains potentially unsafe characters or patterns";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Allow empty/null values to pass validation.
     * Use with @NotNull/@NotBlank for required field validation.
     */
    boolean allowEmpty() default true;
}