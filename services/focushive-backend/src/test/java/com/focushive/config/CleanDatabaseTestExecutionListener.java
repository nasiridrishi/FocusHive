package com.focushive.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.lang.reflect.Method;

/**
 * Test execution listener that handles @CleanDatabase annotation
 *
 * This listener automatically cleans the database when @CleanDatabase
 * annotation is present on test classes or methods.
 */
public class CleanDatabaseTestExecutionListener implements TestExecutionListener {

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        CleanDatabase annotation = getCleanDatabaseAnnotation(testContext);
        if (annotation != null && shouldCleanBefore(annotation.timing())) {
            performCleanup(testContext, annotation);
        }
    }

    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {
        CleanDatabase annotation = getCleanDatabaseAnnotation(testContext);
        if (annotation != null && shouldCleanAfter(annotation.timing())) {
            performCleanup(testContext, annotation);
        }
    }

    @Override
    public void beforeTestClass(TestContext testContext) throws Exception {
        CleanDatabase annotation = testContext.getTestClass().getAnnotation(CleanDatabase.class);
        if (annotation != null && shouldCleanBefore(annotation.timing())) {
            performCleanup(testContext, annotation);
        }
    }

    @Override
    public void afterTestClass(TestContext testContext) throws Exception {
        CleanDatabase annotation = testContext.getTestClass().getAnnotation(CleanDatabase.class);
        if (annotation != null && shouldCleanAfter(annotation.timing())) {
            performCleanup(testContext, annotation);
        }
    }

    private CleanDatabase getCleanDatabaseAnnotation(TestContext testContext) {
        Method testMethod = testContext.getTestMethod();
        CleanDatabase methodAnnotation = testMethod.getAnnotation(CleanDatabase.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return testContext.getTestClass().getAnnotation(CleanDatabase.class);
    }

    private boolean shouldCleanBefore(CleanDatabase.Timing timing) {
        return timing == CleanDatabase.Timing.BEFORE || timing == CleanDatabase.Timing.BOTH;
    }

    private boolean shouldCleanAfter(CleanDatabase.Timing timing) {
        return timing == CleanDatabase.Timing.AFTER || timing == CleanDatabase.Timing.BOTH;
    }

    private void performCleanup(TestContext testContext, CleanDatabase annotation) {
        try {
            // Get the database test utils from the application context
            TestDatabaseConfig.DatabaseTestUtils databaseTestUtils =
                testContext.getApplicationContext().getBean(TestDatabaseConfig.DatabaseTestUtils.class);

            // Perform cleanup based on the annotation configuration
            switch (annotation.strategy()) {
                case TRUNCATE:
                    databaseTestUtils.cleanupTestData();
                    break;
                case DELETE:
                    // Alternative cleanup strategy - could be implemented
                    databaseTestUtils.cleanupTestData(); // For now, use same implementation
                    break;
                case DROP_CREATE:
                    // Most thorough cleanup - could trigger schema recreation
                    databaseTestUtils.cleanupTestData(); // For now, use same implementation
                    break;
            }

        } catch (Exception e) {
            // Log the error but don't fail the test if cleanup fails
            System.err.println("Database cleanup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}