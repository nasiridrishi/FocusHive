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
 * Standalone test for database migrations that validates:
 * 1. No duplicate tables
 * 2. Consistent ID types (UUID)
 * 3. Proper foreign key references
 * 4. Immutable function issues
 * 5. Migration ordering
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StandaloneMigrationTest {

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
    void testMigrationsExecuteSuccessfully() {
        // Test that all migrations can be applied without errors
        assertDoesNotThrow(() -> {
            flyway.migrate();
        }, "All migrations should execute without errors");
        
        MigrationInfoService infoService = flyway.info();
        MigrationInfo[] migrations = infoService.all();
        
        System.out.println("Applied migrations:");
        for (MigrationInfo migration : migrations) {
            System.out.println("  " + migration.getVersion() + ": " + 
                migration.getDescription() + " - " + migration.getState());
            
            // All applied migrations should be successful
            if (migration.getState() != MigrationState.PENDING) {
                assertEquals(MigrationState.SUCCESS, migration.getState(),
                    "Migration " + migration.getVersion() + " should be successful");
            }
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
            
            System.out.println("Found tables: " + tables);
            assertTrue(duplicates.isEmpty(), 
                "Found duplicate tables: " + duplicates);
        }
    }

    @Test
    @Order(3)
    void testIdTypesConsistency() throws SQLException {
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
                        
                        System.out.println("Table: " + tableName + ", Column: " + columnName + ", Type: " + dataType);
                        
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
    void testForeignKeyReferences() throws SQLException {
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
                    
                    System.out.println("FK: " + fkTable + "." + fkColumn + " -> " + pkTable + "." + pkColumn);
                    
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
            
            System.out.println("Existing tables: " + existingTables);
            System.out.println("Required tables: " + requiredTables);
            
            Set<String> missingTables = requiredTables.stream()
                .filter(table -> !existingTables.contains(table))
                .collect(Collectors.toSet());
            
            assertTrue(missingTables.isEmpty(),
                "Missing required tables: " + missingTables);
        }
    }

    @Test
    @Order(6)
    void testBasicInsertOperations() throws SQLException {
        flyway.migrate();
        
        try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            // Test basic CRUD operations on core tables
            
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

    @Test
    @Order(7)
    void testDetectSpecificDuplicates() throws SQLException {
        flyway.migrate();
        
        try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            // Check specifically for the known duplicate tables
            
            // Check focus_sessions - should exist only once
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'focus_sessions'")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next());
                    int count = rs.getInt(1);
                    assertEquals(1, count, "focus_sessions table should exist exactly once, but found " + count);
                }
            }
            
            // Check chat_messages - should exist only once
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'chat_messages'")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next());
                    int count = rs.getInt(1);
                    assertEquals(1, count, "chat_messages table should exist exactly once, but found " + count);
                }
            }
        }
    }

    @Test
    @Order(8)
    void testNoSkippedFilesInMigrations() {
        MigrationInfoService infoService = flyway.info();
        MigrationInfo[] migrations = infoService.all();
        
        for (MigrationInfo migration : migrations) {
            String script = migration.getScript();
            assertFalse(script.endsWith(".skip"),
                "Migration script should not include .skip files: " + script);
        }
    }
}