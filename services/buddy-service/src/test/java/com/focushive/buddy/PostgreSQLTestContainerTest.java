package com.focushive.buddy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class PostgreSQLTestContainerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("buddy_test_db")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private DataSource dataSource;

    @Test
    void testPostgreSQLConnection() throws Exception {
        assertNotNull(dataSource);

        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection);

            ResultSet rs = connection.createStatement()
                    .executeQuery("SELECT version()");
            rs.next();
            String version = rs.getString(1);
            System.out.println("PostgreSQL version: " + version);
            assertNotNull(version);
            assert(version.contains("PostgreSQL"));
        }
    }

    @Test
    void testFlywayMigrations() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ResultSet rs = connection.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'buddy_preferences'");
            rs.next();
            int count = rs.getInt(1);
            assertEquals(1, count, "buddy_preferences table should exist after Flyway migrations");
        }
    }
}