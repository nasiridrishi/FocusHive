package com.focushive.identity.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic test to verify TestContainers infrastructure is working correctly.
 * This test validates:
 * - PostgreSQL TestContainer setup and connectivity
 * - Redis TestContainer setup and connectivity  
 * - Database schema exists
 * - Spring Boot test context loading
 */
@DisplayName("Basic TestContainers Infrastructure Verification")
class TestContainersBasicTest extends BaseIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    @DisplayName("PostgreSQL TestContainer should be running and accessible")
    void testPostgreSQLContainerIsRunning() throws Exception {
        // Verify container is running
        assertTrue(postgresql.isRunning(), "PostgreSQL container should be running");
        
        // Verify database connection
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection, "Should be able to get database connection");
            assertTrue(connection.isValid(5), "Database connection should be valid");
            
            // Verify database name
            assertEquals("identity_service_test", connection.getCatalog(), 
                "Should be connected to correct test database");
        }
    }

    @Test
    @DisplayName("Redis TestContainer should be running and accessible")
    void testRedisContainerIsRunning() {
        // Verify container is running
        assertTrue(redis.isRunning(), "Redis container should be running");
        
        // Verify Redis connectivity
        try {
            redisTemplate.opsForValue().set("test-key", "test-value");
            String value = (String) redisTemplate.opsForValue().get("test-key");
            assertEquals("test-value", value, "Should be able to read/write to Redis");
            
            // Cleanup
            redisTemplate.delete("test-key");
        } catch (Exception e) {
            fail("Should be able to connect to Redis: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Database schema should be properly created by Hibernate")
    void testDatabaseSchemaExists() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ResultSet tables = connection.getMetaData().getTables(null, null, "%", new String[]{"TABLE"});
            
            boolean hasUsersTable = false;
            boolean hasPersonasTable = false;
            boolean hasOAuthClientsTable = false;
            
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME").toLowerCase();
                if (tableName.equals("users")) hasUsersTable = true;
                if (tableName.equals("personas")) hasPersonasTable = true;
                if (tableName.equals("oauth_clients")) hasOAuthClientsTable = true;
            }
            
            assertTrue(hasUsersTable, "Should have users table");
            assertTrue(hasPersonasTable, "Should have personas table");
            assertTrue(hasOAuthClientsTable, "Should have oauth_clients table");
        }
    }

    @Test
    @DisplayName("Container reuse should work correctly between tests")
    void testContainerReuse() {
        // This test verifies that containers are properly reused
        // and don't restart between tests in the same class
        
        // The containers should still be the same instances
        assertTrue(postgresql.isRunning(), "PostgreSQL container should still be running");
        assertTrue(redis.isRunning(), "Redis container should still be running");
        
        // Container properties should be stable
        assertNotNull(postgresql.getJdbcUrl(), "JDBC URL should be available");
        assertNotNull(redis.getHost(), "Redis host should be available");
        assertTrue(redis.getMappedPort(6379) > 0, "Redis port should be mapped");
    }

    @Test
    @DisplayName("Spring Boot test context should load successfully")
    void testSpringContextLoads() {
        // This test passes if the Spring context loads successfully
        // If there are any configuration issues, this test will fail
        
        assertNotNull(dataSource, "DataSource should be injected");
        assertNotNull(redisTemplate, "RedisTemplate should be injected");
        assertNotNull(mockMvc, "MockMvc should be injected");
        assertNotNull(objectMapper, "ObjectMapper should be injected");
    }

    @Test
    @DisplayName("Test utilities should provide helper methods")
    void testUtilityMethods() {
        // Test BasicAuth header creation
        String authHeader = TestUtils.createBasicAuthHeader("client", "secret");
        assertNotNull(authHeader, "Should create auth header");
        assertTrue(authHeader.startsWith("Basic "), "Should start with Basic prefix");
        
        // Test constants are available
        assertEquals("test-client", TestUtils.TEST_CLIENT_ID, "Should have test client ID");
        assertEquals("test-secret", TestUtils.TEST_CLIENT_SECRET, "Should have test client secret");
        assertEquals("test@example.com", TestUtils.TEST_USER_EMAIL, "Should have test user email");
        assertEquals("testpassword", TestUtils.TEST_USER_PASSWORD, "Should have test password");
    }

    @Test
    @DisplayName("Container configuration should be correct")
    void testContainerConfiguration() {
        // PostgreSQL container configuration
        assertEquals("postgres:16-alpine", postgresql.getDockerImageName(), 
            "Should use correct PostgreSQL version");
        assertEquals("identity_service_test", postgresql.getDatabaseName(), 
            "Should have correct database name");
        assertEquals("test", postgresql.getUsername(), 
            "Should have correct username");
        assertEquals("test", postgresql.getPassword(), 
            "Should have correct password");
        // Container reuse is configured in container definition
        
        // Redis container configuration
        assertTrue(redis.getDockerImageName().contains("redis:7"), 
            "Should use correct Redis version");
    }

    @Test
    @DisplayName("Database connection properties should be correctly configured")
    void testDatabaseConnectionProperties() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            assertTrue(url.startsWith("jdbc:postgresql://"), 
                "Should use PostgreSQL JDBC driver");
            assertTrue(url.contains("identity_service_test"), 
                "Should connect to correct database");
            
            String driverName = connection.getMetaData().getDriverName();
            assertEquals("PostgreSQL JDBC Driver", driverName, 
                "Should use PostgreSQL driver");
        }
    }
}