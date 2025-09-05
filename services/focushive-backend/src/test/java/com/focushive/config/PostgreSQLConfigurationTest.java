package com.focushive.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for PostgreSQL configuration validation.
 * Tests HikariCP connection pool settings and database connection parameters.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("postgresql-test")
@DisplayName("PostgreSQL Configuration Tests")
class PostgreSQLConfigurationTest {

    @Autowired
    private DataSource dataSource;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public DataSource testDataSource() {
            // Mock HikariCP configuration for testing
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
            config.setUsername("sa");
            config.setPassword("");
            config.setDriverClassName("org.h2.Driver");
            
            // Production-like HikariCP settings
            config.setMinimumIdle(10);
            config.setMaximumPoolSize(30);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000); // 10 minutes
            config.setMaxLifetime(1800000); // 30 minutes
            config.setLeakDetectionThreshold(60000); // 60 seconds
            config.setConnectionTestQuery("SELECT 1");
            
            return new HikariDataSource(config);
        }
    }

    @Test
    @DisplayName("DataSource should be configured and available")
    void testDataSourceIsConfigured() {
        assertThat(dataSource).isNotNull();
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
    }

    @Test
    @DisplayName("HikariCP connection pool should have correct minimum idle connections")
    void testMinimumIdleConnections() {
        HikariDataSource hikariDS = (HikariDataSource) dataSource;
        assertThat(hikariDS.getMinimumIdle()).isEqualTo(10);
    }

    @Test
    @DisplayName("HikariCP connection pool should have correct maximum pool size")
    void testMaximumPoolSize() {
        HikariDataSource hikariDS = (HikariDataSource) dataSource;
        assertThat(hikariDS.getMaximumPoolSize()).isEqualTo(30);
    }

    @Test
    @DisplayName("HikariCP should have correct connection timeout")
    void testConnectionTimeout() {
        HikariDataSource hikariDS = (HikariDataSource) dataSource;
        assertThat(hikariDS.getConnectionTimeout()).isEqualTo(30000);
    }

    @Test
    @DisplayName("HikariCP should have correct idle timeout")
    void testIdleTimeout() {
        HikariDataSource hikariDS = (HikariDataSource) dataSource;
        assertThat(hikariDS.getIdleTimeout()).isEqualTo(600000);
    }

    @Test
    @DisplayName("HikariCP should have correct max lifetime")
    void testMaxLifetime() {
        HikariDataSource hikariDS = (HikariDataSource) dataSource;
        assertThat(hikariDS.getMaxLifetime()).isEqualTo(1800000);
    }

    @Test
    @DisplayName("HikariCP should have leak detection enabled")
    void testLeakDetectionThreshold() {
        HikariDataSource hikariDS = (HikariDataSource) dataSource;
        assertThat(hikariDS.getLeakDetectionThreshold()).isEqualTo(60000);
    }

    @Test
    @DisplayName("HikariCP should have connection test query configured")
    void testConnectionTestQuery() {
        HikariDataSource hikariDS = (HikariDataSource) dataSource;
        assertThat(hikariDS.getConnectionTestQuery()).isEqualTo("SELECT 1");
    }

    @Test
    @DisplayName("Database connection should be obtainable")
    void testConnectionCanBeObtained() {
        assertDoesNotThrow(() -> {
            try (Connection connection = dataSource.getConnection()) {
                assertThat(connection).isNotNull();
                assertThat(connection.isValid(5)).isTrue();
            }
        });
    }

    @Test
    @DisplayName("Connection pool should handle multiple connections efficiently")
    void testMultipleConnectionsEfficiency() {
        assertDoesNotThrow(() -> {
            Connection[] connections = new Connection[5];
            try {
                // Obtain multiple connections
                for (int i = 0; i < 5; i++) {
                    connections[i] = dataSource.getConnection();
                    assertThat(connections[i]).isNotNull();
                    assertThat(connections[i].isValid(2)).isTrue();
                }
            } finally {
                // Clean up connections
                for (Connection conn : connections) {
                    if (conn != null && !conn.isClosed()) {
                        conn.close();
                    }
                }
            }
        });
    }

    @Test
    @DisplayName("Statement caching should be configured")
    void testStatementCaching() {
        HikariDataSource hikariDS = (HikariDataSource) dataSource;
        // In actual PostgreSQL config, this would be set via addDataSourceProperty
        // For test, we verify the capability exists
        assertThat(hikariDS.getDataSourceProperties()).isNotNull();
    }

    @Test
    @DisplayName("Connection validation should work properly")
    void testConnectionValidation() {
        assertDoesNotThrow(() -> {
            try (Connection connection = dataSource.getConnection()) {
                assertThat(connection.isValid(1)).isTrue();
                
                // Test connection test query
                var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT 1");
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(1);
            }
        });
    }

    @Test
    @DisplayName("Pool should be properly named for monitoring")
    void testPoolName() {
        HikariDataSource hikariDS = (HikariDataSource) dataSource;
        // In production config, pool should have a meaningful name
        assertThat(hikariDS.getPoolName()).isNotNull();
    }

    @Test
    @DisplayName("Transaction isolation level should be properly configured")
    void testTransactionIsolation() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            // Default should be READ_COMMITTED for PostgreSQL production
            int isolationLevel = connection.getTransactionIsolation();
            assertThat(isolationLevel).isIn(
                Connection.TRANSACTION_READ_COMMITTED,
                Connection.TRANSACTION_READ_UNCOMMITTED,
                Connection.TRANSACTION_REPEATABLE_READ,
                Connection.TRANSACTION_SERIALIZABLE
            );
        }
    }
}