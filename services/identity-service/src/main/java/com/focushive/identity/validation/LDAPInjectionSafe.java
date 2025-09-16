package com.focushive.identity.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that a string field does not contain LDAP injection patterns.
 * This annotation prevents LDAP special characters and injection attempts.
 */
@Documented
@Constraint(validatedBy = LDAPInjectionSafeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface LDAPInjectionSafe {

    String message() default "Field contains potentially unsafe LDAP characters";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Allow empty/null values to pass validation.
     * Use with @NotNull/@NotBlank for required field validation.
     */
    boolean allowEmpty() default true;
}