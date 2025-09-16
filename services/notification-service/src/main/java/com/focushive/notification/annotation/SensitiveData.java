package com.focushive.notification.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark sensitive data fields that should be encrypted.
 * Can be applied to fields, parameters, and methods.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SensitiveData {

    /**
     * The encryption algorithm to use (default: AES).
     */
    String algorithm() default "AES";

    /**
     * Whether to mask the field in logs (default: true).
     */
    boolean maskInLogs() default true;

    /**
     * The masking pattern (default: shows first 3 chars).
     */
    String maskingPattern() default "XXX...";

    /**
     * Whether the field is required to be encrypted (default: true).
     */
    boolean required() default true;
}