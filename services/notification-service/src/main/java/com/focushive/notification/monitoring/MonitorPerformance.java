package com.focushive.notification.monitoring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods for performance monitoring.
 * Methods annotated with this will have their execution time tracked.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MonitorPerformance {
    
    /**
     * Optional description for the monitoring metric.
     */
    String value() default "";
    
    /**
     * Whether to log slow operations (operations taking longer than threshold).
     */
    boolean logSlowOperations() default true;
    
    /**
     * Threshold in milliseconds for considering an operation as slow.
     */
    long slowOperationThresholdMs() default 1000;
}