package com.focushive.buddy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple PostgreSQL connection test.
 * Extends AbstractTestContainersTest to get proper database setup.
 */
class SimplePostgreSQLTest extends AbstractTestContainersTest {

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
            System.out.println("✅ PostgreSQL version: " + version);
            assertTrue(version.contains("PostgreSQL"));
        }
    }

    @Test
    void testDatabaseTables() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ResultSet rs = connection.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public'");
            rs.next();
            int tableCount = rs.getInt(1);
            System.out.println("✅ Total tables in database: " + tableCount);
            assertTrue(tableCount > 0, "Should have tables after Flyway migrations");
        }
    }
}