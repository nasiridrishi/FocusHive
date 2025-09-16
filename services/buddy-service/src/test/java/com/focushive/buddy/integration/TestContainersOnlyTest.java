package com.focushive.buddy.integration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure TestContainers test without Spring context loading.
 * This verifies TestContainers configuration works independently.
 */
@Testcontainers
public class TestContainersOnlyTest {

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
    void testPostgreSQLContainerStartsSuccessfully() {
        // Verify PostgreSQL container is running and healthy
        assertTrue(postgresql.isRunning(), "PostgreSQL container should be running");
        assertTrue(postgresql.isHealthy(), "PostgreSQL container should be healthy");
        assertNotNull(postgresql.getJdbcUrl(), "PostgreSQL JDBC URL should be available");

        // Log connection details for verification
        System.out.println("PostgreSQL JDBC URL: " + postgresql.getJdbcUrl());
        System.out.println("PostgreSQL Username: " + postgresql.getUsername());
        System.out.println("PostgreSQL Database: " + postgresql.getDatabaseName());
    }

    @Test
    void testRedisContainerStartsSuccessfully() {
        // Verify Redis container is running and healthy
        assertTrue(redis.isRunning(), "Redis container should be running");
        assertTrue(redis.isHealthy(), "Redis container should be healthy");
        assertTrue(redis.getMappedPort(6379) > 0, "Redis port should be mapped");

        // Log connection details for verification
        System.out.println("Redis Host: " + redis.getHost());
        System.out.println("Redis Port: " + redis.getMappedPort(6379));
    }

    @Test
    void testPostgreSQLConnectionWorks() throws Exception {
        // Test that we can execute a simple SQL command
        var result = postgresql.execInContainer("psql", "-U", postgresql.getUsername(),
                "-d", postgresql.getDatabaseName(), "-c", "SELECT 1 as test;");

        assertEquals(0, result.getExitCode(), "PostgreSQL command should execute successfully");
        assertTrue(result.getStdout().contains("1"), "PostgreSQL should return expected result");

        System.out.println("PostgreSQL test query output: " + result.getStdout());
    }

    @Test
    void testRedisConnectionWorks() throws Exception {
        // Test that we can ping Redis
        var result = redis.execInContainer("redis-cli", "-a", "test-password", "ping");

        assertEquals(0, result.getExitCode(), "Redis command should execute successfully");
        assertTrue(result.getStdout().contains("PONG"), "Redis should respond with PONG");

        System.out.println("Redis ping response: " + result.getStdout());
    }

    @Test
    void testContainersUseIsolatedPorts() {
        int postgresPort = postgresql.getMappedPort(5432);
        int redisPort = redis.getMappedPort(6379);

        assertTrue(postgresPort > 1024 && postgresPort < 65536, "PostgreSQL port should be in valid range");
        assertTrue(redisPort > 1024 && redisPort < 65536, "Redis port should be in valid range");
        assertNotEquals(postgresPort, redisPort, "Containers should use different ports");

        System.out.println("PostgreSQL mapped port: " + postgresPort);
        System.out.println("Redis mapped port: " + redisPort);
    }
}