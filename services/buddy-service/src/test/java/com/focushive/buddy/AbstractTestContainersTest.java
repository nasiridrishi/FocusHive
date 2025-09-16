package com.focushive.buddy;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Abstract base class for all integration tests that require TestContainers.
 * This class manages PostgreSQL and Redis containers lifecycle and provides
 * proper Spring configuration for tests.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractTestContainersTest {

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("buddy_test_db")
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(true);  // Reuse container across tests for performance

    @Container
    protected static final GenericContainer<?> REDIS_CONTAINER =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withCommand("redis-server")
            .withReuse(true);  // Reuse container across tests for performance

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL properties
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // Redis properties
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");

        // Flyway properties
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");

        // JPA properties
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");

        // Cache properties
        registry.add("spring.cache.type", () -> "redis");

        // OAuth2 properties for tests
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost:8081");

        // Test-specific properties
        registry.add("server.port", () -> "0");  // Random port for tests
    }
}