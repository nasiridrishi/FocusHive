package com.focushive.buddy.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation annotation to ensure partnership ID is correctly set based on goal type
 */
@Documented
@Constraint(validatedBy = SharedGoalValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSharedGoal {
    String message() default "Invalid goal configuration";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}