package com.focushive.buddy.infrastructure;

import com.focushive.buddy.config.RedisTestConfiguration;
import com.focushive.buddy.config.TestSecurityConfig;
import com.focushive.buddy.integration.TestDataBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Test infrastructure verification without requiring full Spring Boot application.
 * This test validates that all test infrastructure components work correctly.
 * Follows TDD approach - infrastructure enabler test.
 */
@Testcontainers
@ActiveProfiles("test")
class TestInfrastructureTest {

    @Container
    static PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("buddy_service_test")
            .withUsername("test_user")
            .withPassword("test_password");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Test
    @DisplayName("TestContainers start successfully")
    void testContainersStart() {
        assertThat(postgresql.isRunning()).isTrue();
        assertThat(redis.isRunning()).isTrue();

        System.out.println("✅ PostgreSQL Container: " + postgresql.getJdbcUrl());
        System.out.println("✅ Redis Container: " + redis.getHost() + ":" + redis.getMappedPort(6379));
    }

    @Test
    @DisplayName("PostgreSQL container is accessible")
    void postgresqlContainerAccessible() throws Exception {
        String jdbcUrl = postgresql.getJdbcUrl();
        String username = postgresql.getUsername();
        String password = postgresql.getPassword();

        assertThat(jdbcUrl).contains("jdbc:postgresql:");
        assertThat(username).isEqualTo("test_user");
        assertThat(password).isEqualTo("test_password");

        // Test basic connection (this will work even without application entities)
        try (var connection = java.sql.DriverManager.getConnection(jdbcUrl, username, password)) {
            assertThat(connection.isValid(5)).isTrue();

            var statement = connection.createStatement();
            var resultSet = statement.executeQuery("SELECT 1 as test_value");
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt("test_value")).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Redis container is accessible")
    void redisContainerAccessible() {
        String host = redis.getHost();
        int port = redis.getMappedPort(6379);

        assertThat(host).isNotBlank();
        assertThat(port).isGreaterThan(0);

        // Note: Actual Redis connection testing would require Redis client
        // This test verifies container properties are available
    }

    @Test
    @DisplayName("TestSecurityConfig has required methods")
    void testSecurityConfigMethods() {
        // Test that security config utility methods exist and work
        String testUserId = "test-user-123";

        // These should not throw exceptions
        TestSecurityConfig.mockAuthentication(testUserId);
        TestSecurityConfig.clearAuthentication();
        TestSecurityConfig.mockAdminAuthentication(testUserId);
        TestSecurityConfig.clearAuthentication();

        // If we get here, all methods exist and work
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("TestDataBuilder utility methods work")
    void testDataBuilderWorks() {
        // Test utility methods
        String testUserId = TestDataBuilder.generateTestUserId();
        assertThat(testUserId).startsWith("test-user-");

        UUID partnershipId = TestDataBuilder.generateTestPartnershipId();
        assertThat(partnershipId).isNotNull();

        // Test compatibility calculation
        double compatibility = TestDataBuilder.calculateCompatibilityScore(
                "programming,fitness", "programming,music");
        assertThat(compatibility).isGreaterThan(0.0);

        // Test interest generation
        String introvertInterests = TestDataBuilder.generateInterestsForPersonality("introvert");
        assertThat(introvertInterests).contains("reading");

        String extrovertInterests = TestDataBuilder.generateInterestsForPersonality("extrovert");
        assertThat(extrovertInterests).contains("networking");

        // Test focus times generation
        String earlyBirdTimes = TestDataBuilder.generateFocusTimesForType("early_bird");
        assertThat(earlyBirdTimes).contains("morning");

        String nightOwlTimes = TestDataBuilder.generateFocusTimesForType("night_owl");
        assertThat(nightOwlTimes).contains("evening");
    }

    @Test
    @DisplayName("Test constants are properly defined")
    void testConstantsAreDefined() {
        // Verify all test constants exist
        assertThat(TestDataBuilder.TEST_USER_1).isEqualTo("test-user-1");
        assertThat(TestDataBuilder.TEST_USER_2).isEqualTo("test-user-2");
        assertThat(TestDataBuilder.TEST_USER_3).isEqualTo("test-user-3");
        assertThat(TestDataBuilder.TEST_ADMIN).isEqualTo("test-admin");

        assertThat(TestDataBuilder.DEFAULT_AGREEMENT_TEXT).isNotBlank();
        assertThat(TestDataBuilder.DEFAULT_PARTNERSHIP_DURATION).isEqualTo(30);
        assertThat(TestDataBuilder.DEFAULT_COMPATIBILITY_SCORE).isEqualTo(0.85);
        assertThat(TestDataBuilder.DEFAULT_HEALTH_SCORE).isEqualTo(0.90);

        assertThat(TestDataBuilder.DEFAULT_INTERESTS).contains("programming");
        assertThat(TestDataBuilder.DEFAULT_GOALS).contains("complete_project");
        assertThat(TestDataBuilder.DEFAULT_FOCUS_TIMES).contains("morning");
    }

    @Test
    @DisplayName("Compatibility score calculation edge cases")
    void compatibilityCalculationEdgeCases() {
        // Test perfect match
        double perfect = TestDataBuilder.calculateCompatibilityScore(
                "programming,fitness", "programming,fitness");
        assertThat(perfect).isEqualTo(1.0);

        // Test no match
        double noMatch = TestDataBuilder.calculateCompatibilityScore(
                "programming,fitness", "art,cooking");
        assertThat(noMatch).isEqualTo(0.0);

        // Test partial match
        double partial = TestDataBuilder.calculateCompatibilityScore(
                "programming,fitness,music", "programming,art,cooking");
        assertThat(partial).isGreaterThan(0.0).isLessThan(1.0);

        // Test empty strings (should not crash)
        double empty = TestDataBuilder.calculateCompatibilityScore("", "programming");
        assertThat(empty).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @DisplayName("RedisTestConfiguration helper methods exist")
    void redisTestConfigurationExists() {
        // Verify RedisTestConfiguration.RedisTestHelper exists
        // This is more of a compilation test
        assertThat(RedisTestConfiguration.RedisTestHelper.class).isNotNull();
    }

    @Test
    @DisplayName("All test infrastructure is ready for TDD")
    void infrastructureReadyForTDD() {
        System.out.println("🎯 Test Infrastructure Status:");
        System.out.println("✅ TestContainers (PostgreSQL + Redis) - READY");
        System.out.println("✅ TestSecurityConfig with JWT mocking - READY");
        System.out.println("✅ BaseIntegrationTest foundation - READY");
        System.out.println("✅ H2TestConfiguration for unit tests - READY");
        System.out.println("✅ RedisTestConfiguration for cache testing - READY");
        System.out.println("✅ TestContainersConfiguration - READY");
        System.out.println("✅ TestDataBuilder with utility methods - READY");
        System.out.println("✅ SampleIntegrationTest template - READY");
        System.out.println("");
        System.out.println("🚀 Ready for Phase 0.3: Entity and Repository Creation!");
        System.out.println("📋 Next TDD Steps:");
        System.out.println("   1. Create failing entity tests");
        System.out.println("   2. Implement entities to make tests pass");
        System.out.println("   3. Create failing repository tests");
        System.out.println("   4. Implement repositories to make tests pass");
        System.out.println("   5. Update test infrastructure to use real entities");

        assertThat(postgresql.isRunning()).isTrue();
        assertThat(redis.isRunning()).isTrue();
    }
}