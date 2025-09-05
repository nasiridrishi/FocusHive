package com.focushive.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to validate basic migration execution
 */
@Testcontainers
public class SimpleMigrationTest {

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
    void testMigrationsExecuteOneByOne() {
        // Test migrations one by one to identify which one fails
        MigrationInfoService infoService = flyway.info();
        MigrationInfo[] allMigrations = infoService.all();
        
        System.out.println("Available migrations:");
        for (MigrationInfo migration : allMigrations) {
            System.out.println("  " + migration.getVersion() + ": " + migration.getDescription());
        }
        
        try {
            // Try to migrate and see where it fails
            flyway.migrate();
            
            // Check final state
            MigrationInfoService finalInfo = flyway.info();
            MigrationInfo[] finalMigrations = finalInfo.all();
            
            System.out.println("\nFinal migration states:");
            for (MigrationInfo migration : finalMigrations) {
                System.out.println("  " + migration.getVersion() + ": " + 
                    migration.getDescription() + " - " + migration.getState());
            }
            
        } catch (Exception e) {
            System.out.println("\nMigration failed with error: " + e.getMessage());
            e.printStackTrace();
            
            // Show which migrations succeeded
            MigrationInfoService partialInfo = flyway.info();
            MigrationInfo[] partialMigrations = partialInfo.all();
            
            System.out.println("\nPartial migration states:");
            for (MigrationInfo migration : partialMigrations) {
                System.out.println("  " + migration.getVersion() + ": " + 
                    migration.getDescription() + " - " + migration.getState());
            }
            
            fail("Migration failed: " + e.getMessage());
        }
    }
}