package com.focushive.notification.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify API version for controllers and methods.
 * Supports multiple versions for backward compatibility.
 *
 * Example usage:
 * <pre>
 * {@code
 * @RestController
 * @ApiVersion("1")
 * public class NotificationController {
 *     // Available at /api/v1/notifications
 * }
 *
 * @RestController
 * @ApiVersion({"1", "2"})
 * public class UserController {
 *     // Available at both /api/v1/users and /api/v2/users
 * }
 * }
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiVersion {
    /**
     * API version(s) supported by this controller or method.
     * Multiple versions can be specified for backward compatibility.
     *
     * @return array of supported version strings
     */
    String[] value() default {"1"};

    /**
     * Whether this version is deprecated.
     * Used for documentation and warning purposes.
     *
     * @return true if this version is deprecated
     */
    boolean deprecated() default false;

    /**
     * Date when this version will be removed (optional).
     * Format: YYYY-MM-DD
     *
     * @return sunset date for this API version
     */
    String sunsetDate() default "";

    /**
     * Message to display for deprecated versions.
     *
     * @return deprecation message
     */
    String deprecationMessage() default "";
}