package com.focushive.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.*;
import java.util.*;

/**
 * Migration Validator Utility
 *
 * A standalone utility to validate database migrations outside of JUnit framework.
 * This helps debug migration issues without dealing with JUnit platform problems.
 */
public class MigrationValidator {

    public static void main(String[] args) {
        System.out.println("=== FocusHive Migration Validation ===\n");

        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
                .withDatabaseName("migration_test_db")
                .withUsername("test_user")
                .withPassword("test_password")) {

            postgres.start();
            System.out.println("✓ PostgreSQL TestContainer started successfully");

            Flyway flyway = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .cleanDisabled(false)
                    .mixed(true)
                    .validateOnMigrate(true)
                    .baselineOnMigrate(true)
                    .load();

            System.out.println("✓ Flyway configured successfully");

            // Test 1: List all migrations
            System.out.println("\n=== Test 1: Migration File Analysis ===");
            validateMigrationFiles(flyway);

            // Test 2: Execute migrations
            System.out.println("\n=== Test 2: Migration Execution ===");
            executeMigrations(flyway);

            // Test 3: Validate schema
            System.out.println("\n=== Test 3: Schema Validation ===");
            validateSchema(postgres);

            // Test 4: Basic operations
            System.out.println("\n=== Test 4: Basic Database Operations ===");
            testBasicOperations(postgres);

            System.out.println("\n=== Migration Validation COMPLETED SUCCESSFULLY ===");

        } catch (Exception e) {
            System.err.println("MIGRATION VALIDATION FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void validateMigrationFiles(Flyway flyway) {
        MigrationInfoService infoService = flyway.info();
        MigrationInfo[] migrations = infoService.all();

        System.out.println("Found " + migrations.length + " migration files:");

        boolean hasNamingIssues = false;

        for (MigrationInfo migration : migrations) {
            String version = migration.getVersion().toString();
            String script = migration.getScript();
            String description = migration.getDescription();

            System.out.printf("  - V%s: %s (file: %s)%n", version, description, script);

            // Check for date-based naming
            if (script.matches(".*V202\\d+.*")) {
                System.err.println("    ERROR: Date-based naming detected in " + script);
                hasNamingIssues = true;
            }

            // Check for reasonable version numbers
            try {
                int versionNum = Integer.parseInt(version.split("\\.")[0]);
                if (versionNum > 100) {
                    System.err.println("    WARNING: High version number " + versionNum + " in " + script);
                }
            } catch (NumberFormatException e) {
                System.err.println("    ERROR: Non-numeric version in " + script);
                hasNamingIssues = true;
            }
        }

        if (hasNamingIssues) {
            throw new RuntimeException("Migration naming convention issues detected");
        }

        System.out.println("✓ All migration files follow proper naming convention");
    }

    private static void executeMigrations(Flyway flyway) {
        try {
            System.out.println("Executing migrations...");
            flyway.clean(); // Clean first to ensure clean state
            flyway.migrate();

            MigrationInfoService infoService = flyway.info();
            MigrationInfo[] migrations = infoService.all();

            int successful = 0;
            int failed = 0;

            for (MigrationInfo migration : migrations) {
                MigrationState state = migration.getState();
                if (state == MigrationState.SUCCESS) {
                    successful++;
                } else if (state == MigrationState.FAILED) {
                    failed++;
                    System.err.println("  FAILED: V" + migration.getVersion() + " - " + migration.getDescription());
                }
            }

            System.out.printf("✓ Migration execution completed: %d successful, %d failed%n", successful, failed);

            if (failed > 0) {
                throw new RuntimeException("Some migrations failed");
            }

        } catch (Exception e) {
            System.err.println("Migration execution failed: " + e.getMessage());
            throw new RuntimeException("Migration execution failed", e);
        }
    }

    private static void validateSchema(PostgreSQLContainer<?> postgres) throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            DatabaseMetaData metaData = conn.getMetaData();

            // Check for required tables
            Set<String> requiredTables = Set.of(
                "users", "hives", "hive_members", "focus_sessions",
                "chat_messages", "security_audit_log"
            );

            Set<String> existingTables = new HashSet<>();

            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME").toLowerCase();
                    if (!tableName.startsWith("pg_") && !tableName.equals("flyway_schema_history")) {
                        existingTables.add(tableName);
                    }
                }
            }

            System.out.println("Found tables: " + existingTables);

            Set<String> missingTables = new HashSet<>(requiredTables);
            missingTables.removeAll(existingTables);

            if (!missingTables.isEmpty()) {
                throw new RuntimeException("Missing required tables: " + missingTables);
            }

            // Check for duplicates
            Map<String, Integer> tableCounts = new HashMap<>();
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME").toLowerCase();
                    if (!tableName.startsWith("pg_") && !tableName.equals("flyway_schema_history")) {
                        tableCounts.merge(tableName, 1, Integer::sum);
                    }
                }
            }

            List<String> duplicates = tableCounts.entrySet().stream()
                    .filter(entry -> entry.getValue() > 1)
                    .map(Map.Entry::getKey)
                    .toList();

            if (!duplicates.isEmpty()) {
                throw new RuntimeException("Found duplicate tables: " + duplicates);
            }

            System.out.printf("✓ Schema validation passed: %d tables created, no duplicates%n", existingTables.size());
        }
    }

    private static void testBasicOperations(PostgreSQLContainer<?> postgres) throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            // Test user creation
            String insertUserSql = "INSERT INTO users (email, username, password, display_name) VALUES (?, ?, ?, ?) RETURNING id";
            UUID userId = null;

            try (PreparedStatement stmt = conn.prepareStatement(insertUserSql)) {
                stmt.setString(1, "test@example.com");
                stmt.setString(2, "testuser");
                stmt.setString(3, "password");
                stmt.setString(4, "Test User");

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        userId = UUID.fromString(rs.getString("id"));
                        System.out.println("✓ User created successfully: " + userId);
                    } else {
                        throw new RuntimeException("Failed to create user");
                    }
                }
            }

            // Test audit log creation (from V15 migration)
            String insertAuditSql = "INSERT INTO security_audit_log (user_id, operation, resource_type, access_granted) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertAuditSql)) {
                stmt.setObject(1, userId);
                stmt.setString(2, "CREATE_USER");
                stmt.setString(3, "USER");
                stmt.setBoolean(4, true);

                int result = stmt.executeUpdate();
                if (result == 1) {
                    System.out.println("✓ Audit log entry created successfully");
                } else {
                    throw new RuntimeException("Failed to create audit log entry");
                }
            }

            // Test query operations
            String countUsersSql = "SELECT COUNT(*) FROM users WHERE email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(countUsersSql)) {
                stmt.setString(1, "test@example.com");

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 1) {
                        System.out.println("✓ User query operation successful");
                    } else {
                        throw new RuntimeException("User query failed");
                    }
                }
            }

            System.out.println("✓ All basic database operations completed successfully");
        }
    }
}