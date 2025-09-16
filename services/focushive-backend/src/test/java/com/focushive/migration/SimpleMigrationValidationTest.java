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
 * Simple Migration Validation Test
 *
 * This test validates that the database migration strategy works correctly.
 * It tests migration execution in isolation without Spring Boot context.
 *
 * This is part of the TDD approach for Task 1.1: Database Migration Strategy
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SimpleMigrationValidationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("migration_test_db")
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(false);

    private static Flyway flyway;

    @BeforeAll
    static void setUpAll() {
        postgres.start();

        flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .mixed(true)
                .validateOnMigrate(true)
                .baselineOnMigrate(true)
                .load();
    }

    @BeforeEach
    void setUp() {
        flyway.clean();
    }

    @Test
    @Order(1)
    @DisplayName("Migration files should follow consistent naming convention")
    void shouldValidateMigrationNamingConvention() {
        MigrationInfoService infoService = flyway.info();
        MigrationInfo[] migrations = infoService.all();

        List<String> namingIssues = new ArrayList<>();

        for (MigrationInfo migration : migrations) {
            String script = migration.getScript();

            // Check that no migration files have date-based naming
            if (script.matches(".*V202\\d+.*")) {
                namingIssues.add("Migration " + script + " uses date-based naming instead of sequential");
            }

            // Check that version numbers are reasonable
            try {
                String versionStr = migration.getVersion().toString();
                int version = Integer.parseInt(versionStr.split("\\.")[0]);
                if (version > 100) {
                    namingIssues.add("Migration " + script + " has unusually high version number: " + version);
                }
            } catch (NumberFormatException e) {
                namingIssues.add("Migration " + script + " has non-numeric version: " + migration.getVersion());
            }
        }

        // This should now pass since we fixed the naming
        assertTrue(namingIssues.isEmpty(),
            "Found naming convention issues: " + String.join(", ", namingIssues));
    }

    @Test
    @Order(2)
    @DisplayName("All migrations should execute successfully")
    void shouldExecuteAllMigrationsSuccessfully() {
        // Execute all migrations
        assertDoesNotThrow(() -> {
            flyway.migrate();
        }, "All migrations should execute without errors");

        // Verify migration status
        MigrationInfoService infoService = flyway.info();
        MigrationInfo[] migrations = infoService.all();

        List<String> failedMigrations = new ArrayList<>();

        for (MigrationInfo migration : migrations) {
            if (migration.getState() == MigrationState.FAILED) {
                failedMigrations.add("Migration " + migration.getVersion() + ": " +
                    migration.getDescription() + " - " + migration.getState());
            }
        }

        // This should pass now that we fixed the SQL syntax issues
        assertTrue(failedMigrations.isEmpty(),
            "Found failed migrations: " + String.join(", ", failedMigrations));

        // Verify we have a reasonable number of successful migrations
        long successfulMigrations = Arrays.stream(migrations)
            .filter(m -> m.getState() == MigrationState.SUCCESS)
            .count();

        assertTrue(successfulMigrations > 10,
            "Should have more than 10 successful migrations, but found: " + successfulMigrations);
    }

    @Test
    @Order(3)
    @DisplayName("Core tables should exist after migration")
    void shouldCreateCoreTablesAfterMigration() throws SQLException {
        flyway.migrate();

        Set<String> requiredTables = Set.of(
            "users", "hives", "hive_members",
            "focus_sessions", "chat_messages",
            "security_audit_log"  // From the fixed V15 migration
        );

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            DatabaseMetaData metaData = conn.getMetaData();
            Set<String> existingTables = new HashSet<>();

            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME").toLowerCase();
                    if (!tableName.startsWith("pg_") && !tableName.equals("flyway_schema_history")) {
                        existingTables.add(tableName);
                    }
                }
            }

            Set<String> missingTables = requiredTables.stream()
                .filter(table -> !existingTables.contains(table))
                .collect(Collectors.toSet());

            assertTrue(missingTables.isEmpty(),
                "Missing required tables after migration: " + missingTables);

            System.out.println("Successfully created tables: " + existingTables);
        }
    }

    @Test
    @Order(4)
    @DisplayName("No duplicate tables should exist")
    void shouldNotHaveDuplicateTables() throws SQLException {
        flyway.migrate();

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            DatabaseMetaData metaData = conn.getMetaData();
            Map<String, Integer> tableCount = new HashMap<>();

            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME").toLowerCase();
                    if (!tableName.startsWith("pg_") && !tableName.equals("flyway_schema_history")) {
                        tableCount.merge(tableName, 1, Integer::sum);
                    }
                }
            }

            Set<String> duplicates = tableCount.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

            assertTrue(duplicates.isEmpty(),
                "Found duplicate tables: " + duplicates);
        }
    }

    @Test
    @Order(5)
    @DisplayName("Basic database operations should work")
    void shouldAllowBasicDatabaseOperations() throws SQLException {
        flyway.migrate();

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            // Test basic CRUD on users table
            String insertUserSql = "INSERT INTO users (email, username, password, display_name) VALUES (?, ?, ?, ?) RETURNING id";
            UUID userId = null;

            try (PreparedStatement stmt = conn.prepareStatement(insertUserSql)) {
                stmt.setString(1, "test@example.com");
                stmt.setString(2, "testuser");
                stmt.setString(3, "password");
                stmt.setString(4, "Test User");

                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next(), "User insertion should return an ID");
                    userId = UUID.fromString(rs.getString("id"));
                    assertNotNull(userId, "User ID should not be null");
                }
            }

            // Test that the user was created
            String selectUserSql = "SELECT COUNT(*) FROM users WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(selectUserSql)) {
                stmt.setObject(1, userId);

                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1), "Should find exactly one user");
                }
            }

            // Test security audit log (from V15 migration)
            String insertAuditSql = "INSERT INTO security_audit_log (user_id, operation, resource_type, access_granted) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertAuditSql)) {
                stmt.setObject(1, userId);
                stmt.setString(2, "READ");
                stmt.setString(3, "USER");
                stmt.setBoolean(4, true);

                int result = stmt.executeUpdate();
                assertEquals(1, result, "Should insert one audit log record");
            }
        }
    }

    @Test
    @Order(6)
    @DisplayName("Flyway should track migration history correctly")
    void shouldTrackMigrationHistory() throws SQLException {
        flyway.migrate();

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            // Check that flyway_schema_history table exists and has records
            String sql = "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next());
                    int successfulMigrations = rs.getInt(1);
                    assertTrue(successfulMigrations > 0,
                        "Should have successful migration records in flyway_schema_history");
                }
            }
        }
    }
}