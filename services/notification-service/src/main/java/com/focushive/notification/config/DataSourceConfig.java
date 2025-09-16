package com.focushive.notification.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * DataSource configuration with HikariCP connection pool monitoring.
 * Exposes metrics for connection pool health and performance.
 */
@Slf4j
@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int maximumPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minimumIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;

    @Value("${spring.datasource.hikari.leak-detection-threshold:0}")
    private long leakDetectionThreshold;

    /**
     * Configure HikariCP DataSource with monitoring enabled.
     */
    @Bean
    @Primary
    public DataSource dataSource(MeterRegistry meterRegistry) {
        HikariConfig config = new HikariConfig();

        // Basic configuration
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);

        // Pool configuration
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);

        // Monitoring configuration
        config.setPoolName("notification-service-pool");
        config.setRegisterMbeans(true); // Enable JMX monitoring
        config.setMetricRegistry(meterRegistry); // Enable Micrometer metrics

        // Performance and debugging
        if (leakDetectionThreshold > 0) {
            config.setLeakDetectionThreshold(leakDetectionThreshold);
            log.warn("Connection leak detection enabled with threshold: {}ms", leakDetectionThreshold);
        }

        // Connection test query for PostgreSQL
        config.setConnectionTestQuery("SELECT 1");

        // Additional performance settings
        config.setAutoCommit(true);
        config.setIsolateInternalQueries(false);

        // Connection pool properties
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        HikariDataSource dataSource = new HikariDataSource(config);

        log.info("HikariCP DataSource configured with pool name: {} and monitoring enabled", config.getPoolName());
        log.info("Connection pool settings - Max: {}, Min: {}, Timeout: {}ms",
                maximumPoolSize, minimumIdle, connectionTimeout);

        return dataSource;
    }
}