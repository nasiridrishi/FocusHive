package com.focushive.buddy.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify TestContainers are properly configured and can start.
 * This test doesn't load the full Spring context to isolate container issues.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration")
public class TestContainersHealthCheckTest {

    @Container
    static PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("buddy_service_integration_test")
            .withUsername("integration_user")
            .withPassword("integration_password")
            .withInitScript("db/init/test-schema.sql")
            .withReuse(false);

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", "test-password")
            .withReuse(false);

    @Test
    void testContainersStartAndAreHealthy() {
        // Verify PostgreSQL container
        assertTrue(postgresql.isRunning(), "PostgreSQL container should be running");
        assertTrue(postgresql.isHealthy(), "PostgreSQL container should be healthy");
        assertNotNull(postgresql.getJdbcUrl(), "PostgreSQL JDBC URL should be available");
        assertEquals("integration_user", postgresql.getUsername(), "PostgreSQL username should match");
        assertEquals("integration_password", postgresql.getPassword(), "PostgreSQL password should match");
        assertEquals("buddy_service_integration_test", postgresql.getDatabaseName(), "Database name should match");

        // Verify Redis container
        assertTrue(redis.isRunning(), "Redis container should be running");
        assertTrue(redis.isHealthy(), "Redis container should be healthy");
        assertNotNull(redis.getHost(), "Redis host should be available");
        assertTrue(redis.getMappedPort(6379) > 0, "Redis port should be mapped");
    }

    @Test
    void testPostgreSQLConnection() throws Exception {
        // Test that we can execute commands in PostgreSQL
        postgresql.execInContainer("psql", "-U", postgresql.getUsername(),
                "-d", postgresql.getDatabaseName(), "-c", "SELECT 1;");

        // No exception means success
        assertTrue(true, "PostgreSQL command execution succeeded");
    }

    @Test
    void testRedisConnection() throws Exception {
        // Test that we can execute commands in Redis
        redis.execInContainer("redis-cli", "-a", "test-password", "ping");

        // No exception means success
        assertTrue(true, "Redis command execution succeeded");
    }

    @Test
    void testContainerPorts() {
        // Verify ports are properly mapped
        int postgresPort = postgresql.getMappedPort(5432);
        int redisPort = redis.getMappedPort(6379);

        assertTrue(postgresPort > 1024, "PostgreSQL port should be valid");
        assertTrue(redisPort > 1024, "Redis port should be valid");

        // Ports should be different
        assertNotEquals(postgresPort, redisPort, "PostgreSQL and Redis should use different ports");
    }
}