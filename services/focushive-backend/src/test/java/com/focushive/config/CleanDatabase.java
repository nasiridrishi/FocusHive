package com.focushive.config;

import org.springframework.test.context.TestExecutionListener;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for automatic database cleanup in tests
 *
 * This annotation can be used on test classes or methods to automatically
 * clean up database data before or after test execution.
 *
 * Usage:
 * ```java
 * @CleanDatabase
 * class MyTest {
 *     // Database will be cleaned before/after tests
 * }
 * ```
 *
 * Or on specific test methods:
 * ```java
 * @Test
 * @CleanDatabase(timing = CleanDatabase.Timing.BEFORE)
 * void testWithCleanDatabase() {
 *     // Database cleaned before this test
 * }
 * ```
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CleanDatabase {

    /**
     * When to perform database cleanup
     */
    enum Timing {
        BEFORE,  // Clean before test execution
        AFTER,   // Clean after test execution
        BOTH     // Clean both before and after
    }

    /**
     * Strategy for database cleanup
     */
    enum Strategy {
        TRUNCATE,    // Truncate tables (faster, preserves structure)
        DELETE,      // Delete all records (slower, but safer with complex constraints)
        DROP_CREATE  // Drop and recreate schema (most thorough)
    }

    /**
     * When to perform the cleanup (default: AFTER)
     */
    Timing timing() default Timing.AFTER;

    /**
     * Cleanup strategy to use (default: TRUNCATE)
     */
    Strategy strategy() default Strategy.TRUNCATE;

    /**
     * Specific tables to clean (empty means all tables)
     */
    String[] tables() default {};

    /**
     * Tables to exclude from cleanup
     */
    String[] excludeTables() default {};

    /**
     * Whether to reset auto-increment sequences
     */
    boolean resetSequences() default true;
}