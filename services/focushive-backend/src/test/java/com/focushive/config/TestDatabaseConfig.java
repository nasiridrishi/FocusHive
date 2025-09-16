package com.focushive.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Test database configuration for H2 with PostgreSQL compatibility.
 * This configuration ensures that all necessary JPA beans are available for tests.
 */
@TestConfiguration
@EnableJpaRepositories(
    basePackages = "com.focushive",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com.focushive.forum.repository.*"
    )
)
@EnableTransactionManagement
public class TestDatabaseConfig {

    @Bean
    @Primary
    public DataSource testDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS CLOB");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(testDataSource());
        em.setPackagesToScan("com.focushive");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        Properties properties = new Properties();
        properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.put("hibernate.hbm2ddl.auto", "create-drop");
        properties.put("hibernate.show_sql", "false");
        properties.put("hibernate.format_sql", "true");
        properties.put("hibernate.globally_quoted_identifiers", "false");
        properties.put("hibernate.hbm2ddl.halt_on_error", "false");
        properties.put("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        
        em.setJpaProperties(properties);
        return em;
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }

    /**
     * Database cleanup utility for tests
     * This bean helps with cleaning up test data between tests
     */
    @Bean
    public DatabaseTestUtils databaseTestUtils() {
        return new DatabaseTestUtils(testDataSource());
    }

    /**
     * JDBC Template configured specifically for tests
     * Optimized for H2 operations
     */
    @Bean
    public JdbcTemplate testJdbcTemplate() {
        JdbcTemplate template = new JdbcTemplate(testDataSource());
        // Optimize for test performance
        template.setQueryTimeout(5); // 5 second timeout for tests
        template.setFetchSize(100);  // Small fetch size for tests
        return template;
    }

    /**
     * Database validation utilities for TDD tests
     */
    @Bean
    public DatabaseValidationUtils databaseValidationUtils() {
        return new DatabaseValidationUtils(testDataSource());
    }

    /**
     * Utility class for database test operations
     */
    public static class DatabaseTestUtils {
        private final DataSource dataSource;

        public DatabaseTestUtils(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        /**
         * Clean up all test data
         * This method implements the cleanup strategy for database tests
         */
        public void cleanupTestData() {
            try (var connection = dataSource.getConnection();
                 var statement = connection.createStatement()) {

                // Disable foreign key checks temporarily
                statement.execute("SET REFERENTIAL_INTEGRITY FALSE");

                // Clean up tables in dependency order (children first)
                // This list should be maintained as new tables are added
                String[] tablesToClean = {
                    "SECURITY_AUDIT_LOG", "HIVE_MEMBERS", "FOCUS_SESSIONS",
                    "USER_PRESENCE", "ACTIVE_TIMERS", "PRODUCTIVITY_METRICS",
                    "CHAT_MESSAGES", "NOTIFICATIONS", "BUDDY_RELATIONSHIPS",
                    "FORUM_POSTS", "HIVES", "USERS", "USER_PROFILES"
                };

                for (String table : tablesToClean) {
                    try {
                        statement.execute("TRUNCATE TABLE " + table + " RESTART IDENTITY");
                    } catch (Exception e) {
                        // Table might not exist in this test scenario, continue
                        System.out.println("Could not truncate table " + table + ": " + e.getMessage());
                    }
                }

                // Re-enable foreign key checks
                statement.execute("SET REFERENTIAL_INTEGRITY TRUE");

            } catch (Exception e) {
                throw new RuntimeException("Failed to cleanup test data", e);
            }
        }

        /**
         * Check if database is ready for tests
         */
        public boolean isDatabaseReady() {
            try (var connection = dataSource.getConnection()) {
                return connection.isValid(5); // 5 second timeout
            } catch (Exception e) {
                return false;
            }
        }

        /**
         * Get database connection info for validation
         */
        public DatabaseInfo getDatabaseInfo() {
            try (var connection = dataSource.getConnection()) {
                var metaData = connection.getMetaData();
                return new DatabaseInfo(
                    metaData.getDatabaseProductName(),
                    metaData.getDatabaseProductVersion(),
                    metaData.getURL()
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to get database info", e);
            }
        }
    }

    /**
     * Database information record for testing
     */
    public static record DatabaseInfo(String productName, String version, String url) {}

    /**
     * Database validation utilities for TDD tests
     */
    public static class DatabaseValidationUtils {
        private final DataSource dataSource;

        public DatabaseValidationUtils(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        /**
         * Validate that required database features are available
         */
        public ValidationResult validateDatabaseFeatures() {
            try (var connection = dataSource.getConnection()) {
                var statement = connection.createStatement();
                var results = new ValidationResult();

                // Test UUID support
                try {
                    statement.execute("SELECT CAST('123e4567-e89b-12d3-a456-426614174000' AS UUID)");
                    results.uuidSupported = true;
                } catch (Exception e) {
                    results.uuidSupported = false;
                    results.addError("UUID support failed: " + e.getMessage());
                }

                // Test JSONB support
                try {
                    statement.execute("SELECT CAST('{\"key\":\"value\"}' AS JSONB)");
                    results.jsonbSupported = true;
                } catch (Exception e) {
                    results.jsonbSupported = false;
                    results.addError("JSONB support failed: " + e.getMessage());
                }

                // Test PostgreSQL compatibility mode
                var url = connection.getMetaData().getURL();
                results.postgresqlModeEnabled = url.contains("MODE=PostgreSQL");

                return results;
            } catch (Exception e) {
                var result = new ValidationResult();
                result.addError("Database validation failed: " + e.getMessage());
                return result;
            }
        }

        /**
         * Check schema initialization status
         */
        public boolean isSchemaInitialized() {
            try (var connection = dataSource.getConnection()) {
                var metaData = connection.getMetaData();
                var tables = metaData.getTables(null, null, "HIVES", null);
                return tables.next(); // Return true if HIVES table exists
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * Validation result for database features
     */
    public static class ValidationResult {
        public boolean uuidSupported = false;
        public boolean jsonbSupported = false;
        public boolean postgresqlModeEnabled = false;
        private String errors = "";

        public void addError(String error) {
            if (!errors.isEmpty()) {
                errors += "; ";
            }
            errors += error;
        }

        public String getErrors() {
            return errors;
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean isValid() {
            return uuidSupported && jsonbSupported && postgresqlModeEnabled && !hasErrors();
        }
    }
}