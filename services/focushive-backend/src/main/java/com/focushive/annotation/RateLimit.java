package com.focushive.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Annotation to apply rate limiting to controller methods.
 * Supports both IP-based and user-based rate limiting with configurable limits and time windows.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    
    /**
     * The number of requests allowed within the time window.
     * Default is 5 requests.
     */
    int value() default 5;
    
    /**
     * The time window duration for rate limiting.
     * Default is 1 minute.
     */
    long window() default 1;
    
    /**
     * The time unit for the window.
     * Default is MINUTES.
     */
    TimeUnit timeUnit() default TimeUnit.MINUTES;
    
    /**
     * The type of rate limiting to apply.
     * Default is IP-based rate limiting.
     */
    RateLimitType type() default RateLimitType.IP;
    
    /**
     * Custom key prefix for the rate limit bucket.
     * If not specified, will use method name and type.
     */
    String keyPrefix() default "";
    
    /**
     * Whether to skip rate limiting for authenticated users.
     * Default is false - rate limiting applies to all requests.
     */
    boolean skipAuthenticated() default false;
    
    /**
     * Custom error message to return when rate limit is exceeded.
     * If not specified, will use default message.
     */
    String message() default "";
    
    /**
     * Whether to apply progressive penalties for repeated violations.
     * Default is true - each violation increases the penalty duration.
     */
    boolean progressivePenalties() default true;
    
    /**
     * Types of rate limiting strategies.
     */
    enum RateLimitType {
        /**
         * Rate limit based on IP address.
         */
        IP,
        
        /**
         * Rate limit based on authenticated user ID.
         */
        USER,
        
        /**
         * Rate limit based on both IP and user (more restrictive).
         */
        IP_AND_USER,
        
        /**
         * Rate limit based on IP or user (less restrictive).
         */
        IP_OR_USER
    }
}