package com.focushive.identity.integration;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
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

    @Container
    static RedisContainer redis = new RedisContainer("redis:7-alpine");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    public void testPostgreSQLConnection() throws Exception {
        assertTrue(postgres.isRunning(), "PostgreSQL container should be running");
        
        try (Connection connection = dataSource.getConnection()) {
            assertTrue(connection.isValid(1), "Database connection should be valid");
            assertEquals("demo_test", connection.getCatalog(), "Should connect to demo_test database");
        }
    }

    @Test
    public void testRedisConnection() {
        assertTrue(redis.isRunning(), "Redis container should be running");
        
        String testKey = "testkey";
        String testValue = "testvalue";
        
        redisTemplate.opsForValue().set(testKey, testValue);
        String retrievedValue = (String) redisTemplate.opsForValue().get(testKey);
        
        assertEquals(testValue, retrievedValue, "Should be able to store and retrieve data from Redis");
    }

    @Test
    public void testContainerProperties() {
        // Test PostgreSQL container properties
        assertNotNull(postgres.getJdbcUrl(), "JDBC URL should be available");
        assertTrue(postgres.getJdbcUrl().contains("demo_test"), "JDBC URL should reference demo_test");
        assertEquals("demo", postgres.getUsername(), "Username should be demo");
        assertEquals("demo", postgres.getPassword(), "Password should be demo");
        
        // Test Redis container properties
        assertNotNull(redis.getHost(), "Redis host should be available");
        assertTrue(redis.getFirstMappedPort() > 0, "Redis port should be mapped");
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues.of(
                "spring.datasource.url=" + postgres.getJdbcUrl(),
                "spring.datasource.username=" + postgres.getUsername(),
                "spring.datasource.password=" + postgres.getPassword(),
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                "spring.redis.host=" + redis.getHost(),
                "spring.redis.port=" + redis.getFirstMappedPort(),
                "spring.jpa.hibernate.ddl-auto=create-drop"
            ).applyTo(context.getEnvironment());
        }
    }
}