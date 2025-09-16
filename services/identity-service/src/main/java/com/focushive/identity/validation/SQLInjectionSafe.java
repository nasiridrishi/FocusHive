package com.focushive.identity.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that a string field does not contain SQL injection patterns.
 * This annotation provides additional protection against SQL injection attempts
 * even when using JPA/parameterized queries.
 */
@Documented
@Constraint(validatedBy = SQLInjectionSafeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SQLInjectionSafe {

    String message() default "Field contains potentially unsafe SQL characters or patterns";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Allow empty/null values to pass validation.
     * Use with @NotNull/@NotBlank for required field validation.
     */
    boolean allowEmpty() default true;
}