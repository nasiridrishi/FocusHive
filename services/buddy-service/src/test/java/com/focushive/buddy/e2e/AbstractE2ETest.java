package com.focushive.buddy.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.buddy.config.TestSecurityConfig;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Abstract base class for E2E tests with proper TestContainers setup.
 * Provides PostgreSQL and Redis containers with RestAssured configuration.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect"
    }
)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
public abstract class AbstractE2ETest {

    @Autowired
    protected ObjectMapper objectMapper;

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("buddy_e2e_db")
            .withUsername("e2e_user")
            .withPassword("e2e_password")
            .withReuse(true);

    @Container
    protected static final GenericContainer<?> REDIS_CONTAINER =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withCommand("redis-server")
            .withReuse(true);

    @LocalServerPort
    protected int port;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL properties
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);

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

        // OAuth2 properties for tests
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost:8081");
    }

    @BeforeEach
    protected void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Configure RestAssured to use Spring's ObjectMapper with JavaTimeModule
        RestAssured.config = RestAssuredConfig.config()
            .objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory(
                (type, s) -> objectMapper
            ));
    }
}