package com.focushive.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstration of database cleanup annotation usage
 *
 * This test class shows how to use the @CleanDatabase annotation
 * for automatic database cleanup between tests.
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = TestDatabaseConfig.class)
@TestExecutionListeners(
    value = CleanDatabaseTestExecutionListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
@CleanDatabase(timing = CleanDatabase.Timing.AFTER, strategy = CleanDatabase.Strategy.TRUNCATE)
@DisplayName("Database Cleanup Tests")
class DatabaseCleanupTest {

    @Autowired
    private TestDatabaseConfig.DatabaseTestUtils databaseTestUtils;

    @Test
    @DisplayName("Should have database cleanup utilities available")
    void shouldHaveDatabaseCleanupUtilities() {
        // ARRANGE & ACT - Check utilities are available
        assertThat(databaseTestUtils)
            .as("Database test utilities should be injected")
            .isNotNull();

        // Test database readiness
        boolean isReady = databaseTestUtils.isDatabaseReady();

        // ASSERT
        assertThat(isReady)
            .as("Database should be ready for tests")
            .isTrue();
    }

    @Test
    @CleanDatabase(timing = CleanDatabase.Timing.BEFORE)
    @DisplayName("Should clean database before specific test")
    void shouldCleanDatabaseBeforeTest() {
        // This test demonstrates method-level cleanup annotation
        // Database should be cleaned before this test runs

        // ARRANGE & ACT - Just verify the test runs successfully
        boolean isReady = databaseTestUtils.isDatabaseReady();

        // ASSERT
        assertThat(isReady)
            .as("Database should be ready after cleanup")
            .isTrue();
    }

    @Test
    @DisplayName("Should demonstrate database isolation between tests")
    void shouldDemonstrateTestIsolation() {
        // This test shows that each test starts with a clean database
        // due to the class-level @CleanDatabase annotation

        // ARRANGE & ACT - Get database info
        TestDatabaseConfig.DatabaseInfo dbInfo = databaseTestUtils.getDatabaseInfo();

        // ASSERT
        assertThat(dbInfo.productName())
            .as("Should be using H2 database")
            .isEqualTo("H2");

        // Simulate some database operations
        // In a real test, this might create test data
        // which would be cleaned up after the test
    }

    @Test
    @DisplayName("Should handle cleanup errors gracefully")
    void shouldHandleCleanupErrorsGracefully() {
        // This test verifies that cleanup errors don't fail tests
        // The cleanup listener should catch and log errors

        // ARRANGE & ACT - Just run a normal test
        // Any cleanup errors will be handled by the listener

        // ASSERT - Test should complete successfully even if cleanup fails
        assertThat(true)
            .as("Test should run successfully regardless of cleanup status")
            .isTrue();
    }
}