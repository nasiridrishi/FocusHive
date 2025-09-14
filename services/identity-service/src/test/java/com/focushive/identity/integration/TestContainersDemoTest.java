package com.focushive.identity.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple demonstration that TestContainers is working correctly.
 * This test uses minimal configuration to prove the core functionality.
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersDemoTest.Initializer.class)
@Testcontainers
public class TestContainersDemoTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("demo_test")
            .withUsername("demo")
            .withPassword("demo");

    @Autowired
    private DataSource dataSource;

    @Test
    public void testPostgreSQLConnection() throws Exception {
        assertTrue(postgres.isRunning(), "PostgreSQL container should be running");
        
        try (Connection connection = dataSource.getConnection()) {
            assertTrue(connection.isValid(1), "Database connection should be valid");
            assertEquals("demo_test", connection.getCatalog(), "Should connect to demo_test database");
        }
    }


    @Test
    public void testContainerProperties() {
        // Test PostgreSQL container properties
        assertNotNull(postgres.getJdbcUrl(), "JDBC URL should be available");
        assertTrue(postgres.getJdbcUrl().contains("demo_test"), "JDBC URL should reference demo_test");
        assertEquals("demo", postgres.getUsername(), "Username should be demo");
        assertEquals("demo", postgres.getPassword(), "Password should be demo");
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues.of(
                "spring.datasource.url=" + postgres.getJdbcUrl(),
                "spring.datasource.username=" + postgres.getUsername(),
                "spring.datasource.password=" + postgres.getPassword(),
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop"
            ).applyTo(context.getEnvironment());
        }
    }
}