package com.focushive.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * Database configuration for production PostgreSQL setup.
 * Configures HikariCP connection pool with optimal settings for PostgreSQL.
 */
@Configuration
@Profile("prod")
@ConditionalOnProperty(name = "app.database.provider", havingValue = "postgresql", matchIfMissing = true)
@Slf4j
public class DatabaseConfiguration {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    /**
     * Configures production-ready DataSource with HikariCP connection pool.
     * Optimized for PostgreSQL with performance tuning and monitoring.
     */
    @Bean
    public DataSource dataSource() {
        log.info("Configuring PostgreSQL DataSource with HikariCP for production");

        HikariConfig config = new HikariConfig();
        
        // Basic connection properties
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);

        // Connection pool sizing
        config.setMinimumIdle(10);                // Maintain at least 10 idle connections
        config.setMaximumPoolSize(30);            // Maximum 30 connections total
        config.setConnectionTimeout(30000);       // 30 seconds to get connection
        config.setIdleTimeout(600000);            // 10 minutes idle timeout
        config.setMaxLifetime(1800000);           // 30 minutes maximum connection lifetime

        // Leak detection and monitoring
        config.setLeakDetectionThreshold(60000);  // Warn after 60 seconds
        config.setPoolName("FocusHive-HikariCP-Production");

        // Connection validation
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);        // 5 seconds validation timeout

        // PostgreSQL-specific optimizations
        configurePostgreSQLOptimizations(config);

        // JMX monitoring
        config.setRegisterMbeans(true);

        HikariDataSource dataSource = new HikariDataSource(config);
        
        log.info("PostgreSQL DataSource configured successfully");
        logConnectionPoolInfo(config);
        
        return dataSource;
    }

    /**
     * Configures PostgreSQL-specific optimizations for HikariCP.
     */
    private void configurePostgreSQLOptimizations(HikariConfig config) {
        // Prepared statement caching
        config.addDataSourceProperty("preparedStatementCacheSize", "256");
        config.addDataSourceProperty("preparedStatementCacheSqlLimit", "2048");

        // Transaction isolation
        config.addDataSourceProperty("defaultTransactionIsolation", "TRANSACTION_READ_COMMITTED");

        // TCP settings for better performance
        config.addDataSourceProperty("tcpKeepAlive", "true");
        config.addDataSourceProperty("socketTimeout", "0");

        // Application name for PostgreSQL connection tracking
        config.addDataSourceProperty("ApplicationName", "FocusHive-Backend-Production");

        // SSL configuration (environment-specific)
        config.addDataSourceProperty("ssl", System.getProperty("DB_SSL_ENABLED", "true"));
        config.addDataSourceProperty("sslmode", System.getProperty("DB_SSL_MODE", "require"));

        // Performance tuning
        config.addDataSourceProperty("reWriteBatchedInserts", "true");
        config.addDataSourceProperty("defaultRowFetchSize", "1000");

        // Additional PostgreSQL driver properties
        config.addDataSourceProperty("stringtype", "unspecified");  // Better type handling
        config.addDataSourceProperty("sendBufferSize", "65536");    // 64KB send buffer
        config.addDataSourceProperty("receiveBufferSize", "65536"); // 64KB receive buffer
        config.addDataSourceProperty("logUnclosedConnections", "false"); // Reduce log noise in production

        log.debug("PostgreSQL-specific optimizations applied to DataSource");
    }

    /**
     * Logs connection pool configuration for monitoring and debugging.
     */
    private void logConnectionPoolInfo(HikariConfig config) {
        log.info("HikariCP Configuration:");
        log.info("  Pool Name: {}", config.getPoolName());
        log.info("  JDBC URL: {}", maskUrl(config.getJdbcUrl()));
        log.info("  Username: {}", config.getUsername());
        log.info("  Driver: {}", config.getDriverClassName());
        log.info("  Min Idle: {}", config.getMinimumIdle());
        log.info("  Max Pool Size: {}", config.getMaximumPoolSize());
        log.info("  Connection Timeout: {}ms", config.getConnectionTimeout());
        log.info("  Idle Timeout: {}ms", config.getIdleTimeout());
        log.info("  Max Lifetime: {}ms", config.getMaxLifetime());
        log.info("  Leak Detection Threshold: {}ms", config.getLeakDetectionThreshold());
        log.info("  Validation Timeout: {}ms", config.getValidationTimeout());
        log.info("  Connection Test Query: {}", config.getConnectionTestQuery());
    }

    /**
     * Masks sensitive information in JDBC URL for logging.
     */
    private String maskUrl(String jdbcUrl) {
        if (jdbcUrl == null) {
            return null;
        }
        // Mask password if present in URL
        return jdbcUrl.replaceAll("password=[^&;]+", "password=***");
    }
}