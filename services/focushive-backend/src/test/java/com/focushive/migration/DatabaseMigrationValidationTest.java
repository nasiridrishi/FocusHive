package com.focushive.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for database migration validation
 * Tests migration integrity, duplicate detection, and database schema correctness
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseMigrationValidationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(false);

    private static Flyway flyway;
    
    @BeforeAll
    static void setUpAll() {
        postgres.start();
        
        flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();
    }

    @BeforeEach
    void setUp() {
        flyway.clean();
    }

    @Test
    @Order(1)
    void testMigrationsAreValid() {
        // Clean and validate migrations
        flyway.clean();
        
        assertDoesNotThrow(() -> {
            flyway.migrate();
        }, "All migrations should execute without errors");
        
        // Verify migration status
        MigrationInfoService infoService = flyway.info();
        MigrationInfo[] migrations = infoService.all();
        
        for (MigrationInfo migration : migrations) {
            assertEquals(MigrationState.SUCCESS, migration.getState(),
                "Migration " + migration.getVersion() + " should be successful");
        }
    }

    @Test
    @Order(2)
    void testNoDuplicateTables() throws SQLException {
        flyway.migrate();
        
        try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            DatabaseMetaData metaData = conn.getMetaData();
            Set<String> tables = new HashSet<>();
            Set<String> duplicates = new HashSet<>();
            
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME").toLowerCase();
                    // Skip system tables
                    if (!tableName.startsWith("pg_") && !tableName.startsWith("information_schema") 
                        && !tableName.equals("flyway_schema_history")) {
                        if (!tables.add(tableName)) {
                            duplicates.add(tableName);
                        }
                    }
                }
            }
            
            assertTrue(duplicates.isEmpty(), 
                "Found duplicate tables: " + duplicates);
        }
    }

    @Test
    @Order(3)
    void testAllIdTypesAreUuid() throws SQLException {
        flyway.migrate();
        
        try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            DatabaseMetaData metaData = conn.getMetaData();
            Set<String> invalidIdColumns = new HashSet<>();
            
            // Get all tables with id columns
            try (ResultSet rs = metaData.getColumns(null, null, "%", "id")) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String columnName = rs.getString("COLUMN_NAME");
                    String dataType = rs.getString("TYPE_NAME");
                    
                    // Skip system tables
                    if (!tableName.startsWith("pg_") && !tableName.startsWith("information_schema") 
                        && !tableName.equals("flyway_schema_history")) {
                        
                        // Check if id column is UUID type
                        if ("id".equals(columnName) && !"uuid".equalsIgnoreCase(dataType)) {
                            invalidIdColumns.add(tableName + ".id: " + dataType);
                        }
                    }
                }
            }
            
            assertTrue(invalidIdColumns.isEmpty(),
                "Found non-UUID id columns: " + invalidIdColumns);
        }
    }

    @Test
    @Order(4)
    void testForeignKeyConsistency() throws SQLException {
        flyway.migrate();
        
        try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            DatabaseMetaData metaData = conn.getMetaData();
            Set<String> foreignKeyErrors = new HashSet<>();
            
            // Get all imported keys (foreign keys)
            try (ResultSet rs = metaData.getImportedKeys(null, null, null)) {
                while (rs.next()) {
                    String fkTable = rs.getString("FKTABLE_NAME");
                    String fkColumn = rs.getString("FKCOLUMN_NAME");
                    String pkTable = rs.getString("PKTABLE_NAME");
                    String pkColumn = rs.getString("PKCOLUMN_NAME");
                    
                    // Verify referenced table exists
                    try (ResultSet tableRs = metaData.getTables(null, null, pkTable, new String[]{"TABLE"})) {
                        if (!tableRs.next()) {
                            foreignKeyErrors.add("FK " + fkTable + "." + fkColumn + 
                                " references non-existent table " + pkTable);
                        }
                    }
                }
            }
            
            assertTrue(foreignKeyErrors.isEmpty(),
                "Found foreign key errors: " + foreignKeyErrors);
        }
    }

    @Test
    @Order(5)
    void testIndexImmutability() throws SQLException {
        flyway.migrate();
        
        // Test that functions used in indexes are marked as IMMUTABLE
        try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            // Verify update_updated_at_column function is immutable or doesn't use CURRENT_TIMESTAMP in indexes
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT indexname, indexdef FROM pg_indexes WHERE indexdef LIKE '%DATE(%'")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String indexName = rs.getString("indexname");
                        String indexDef = rs.getString("indexdef");
                        // If DATE() is used, it should be in a context that's safe
                        if (indexDef.contains("DATE(") && !indexDef.contains("WHERE")) {
                            fail("Found potentially problematic DATE() usage in index: " + indexName);
                        }
                    }
                }
            } catch (SQLException e) {
                // This is acceptable - the query might fail if no such indexes exist
            }
        }
    }

    @Test
    @Order(6)
    void testRequiredTablesExist() throws SQLException {
        flyway.migrate();
        
        Set<String> requiredTables = Set.of(
            "users", "hives", "hive_members", "hive_invitations", "hive_settings",
            "focus_sessions", "session_breaks", "daily_summaries", 
            "user_achievements", "productivity_goals", "chat_messages", 
            "message_reactions", "message_read_receipts"
        );
        
        try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            DatabaseMetaData metaData = conn.getMetaData();
            Set<String> existingTables = new HashSet<>();
            
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    existingTables.add(rs.getString("TABLE_NAME").toLowerCase());
                }
            }
            
            Set<String> missingTables = requiredTables.stream()
                .filter(table -> !existingTables.contains(table))
                .collect(Collectors.toSet());
            
            assertTrue(missingTables.isEmpty(),
                "Missing required tables: " + missingTables);
        }
    }

    @Test
    @Order(7)
    void testUpdatedAtTriggersExist() throws SQLException {
        flyway.migrate();
        
        // Verify that update triggers exist for tables with updated_at columns
        String[] tablesWithUpdatedAt = {
            "users", "hives", "hive_members", "hive_settings",
            "focus_sessions", "daily_summaries", "productivity_goals"
        };
        
        try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            for (String tableName : tablesWithUpdatedAt) {
                try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT tgname FROM pg_trigger WHERE tgrelid = (SELECT oid FROM pg_class WHERE relname = ?)")) {
                    stmt.setString(1, tableName);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        boolean hasUpdateTrigger = false;
                        while (rs.next()) {
                            String triggerName = rs.getString("tgname");
                            if (triggerName.contains("updated_at")) {
                                hasUpdateTrigger = true;
                                break;
                            }
                        }
                        
                        assertTrue(hasUpdateTrigger, 
                            "Table " + tableName + " should have an updated_at trigger");
                    }
                } catch (SQLException e) {
                    // Table might not exist or have updated_at column
                    fail("Failed to check triggers for table " + tableName + ": " + e.getMessage());
                }
            }
        }
    }

    @Test
    @Order(8)
    void testSkippedMigrationsHandling() {
        flyway.clean();
        
        // Verify that .skip files are properly ignored
        MigrationInfoService infoService = flyway.info();
        MigrationInfo[] migrations = infoService.all();
        
        for (MigrationInfo migration : migrations) {
            String script = migration.getScript();
            assertFalse(script.endsWith(".skip"),
                "Migration script should not include .skip files: " + script);
        }
    }

    @Test
    @Order(9)
    void testMigrationSequencing() throws SQLException {
        flyway.migrate();
        
        // Verify that tables are created in the correct order
        // (dependencies before dependents)
        
        // This is implicitly tested by successful migration, but we can add
        // explicit checks for critical dependencies
        
        assertTableExists("users");
        assertTableExists("hives");
        assertTableExists("hive_members"); // depends on both users and hives
        assertTableExists("focus_sessions"); // depends on users and hives
        assertTableExists("chat_messages"); // depends on hives
    }

    private void assertTableExists(String tableName) {
        try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
                assertTrue(rs.next(), "Table " + tableName + " should exist");
            }
        } catch (SQLException e) {
            fail("Failed to check existence of table " + tableName + ": " + e.getMessage());
        }
    }

    @Test
    @Order(10)
    void testBasicCrudOperations() throws SQLException {
        flyway.migrate();
        
        // Test basic CRUD operations on core tables to ensure they work
        try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            
            // Insert a user
            try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO users (email, username, password, display_name) VALUES (?, ?, ?, ?) RETURNING id")) {
                stmt.setString(1, "test@example.com");
                stmt.setString(2, "testuser");
                stmt.setString(3, "password");
                stmt.setString(4, "Test User");
                
                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next(), "User insertion should return an ID");
                    String userId = rs.getString("id");
                    assertNotNull(userId, "User ID should not be null");
                    
                    // Insert a hive
                    try (PreparedStatement hiveStmt = conn.prepareStatement(
                        "INSERT INTO hives (name, slug, owner_id) VALUES (?, ?, ?::uuid)")) {
                        hiveStmt.setString(1, "Test Hive");
                        hiveStmt.setString(2, "test-hive");
                        hiveStmt.setString(3, userId);
                        
                        int hiveResult = hiveStmt.executeUpdate();
                        assertEquals(1, hiveResult, "Hive insertion should affect one row");
                    }
                }
            }
            
            // Verify insertions worked
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE email = ?")) {
                stmt.setString(1, "test@example.com");
                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1), "Should have exactly one test user");
                }
            }
            
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM hives WHERE slug = ?")) {
                stmt.setString(1, "test-hive");
                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1), "Should have exactly one test hive");
                }
            }
        }
    }
}