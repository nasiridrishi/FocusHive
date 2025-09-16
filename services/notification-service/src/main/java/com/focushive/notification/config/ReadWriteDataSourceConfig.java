package com.focushive.notification.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for read/write splitting data sources.
 * Allows routing queries to read replicas when available.
 *
 * Enable with property: notification.datasource.read-write-splitting.enabled=true
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "notification.datasource.read-write-splitting.enabled", havingValue = "true")
public class ReadWriteDataSourceConfig {

    @Value("${notification.datasource.write.url}")
    private String writeDbUrl;

    @Value("${notification.datasource.write.username}")
    private String writeDbUsername;

    @Value("${notification.datasource.write.password}")
    private String writeDbPassword;

    @Value("${notification.datasource.read.url:${notification.datasource.write.url}}")
    private String readDbUrl;

    @Value("${notification.datasource.read.username:${notification.datasource.write.username}}")
    private String readDbUsername;

    @Value("${notification.datasource.read.password:${notification.datasource.write.password}}")
    private String readDbPassword;

    @Value("${notification.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    /**
     * Create write (master) data source.
     */
    @Bean
    public DataSource writeDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(writeDbUrl);
        config.setUsername(writeDbUsername);
        config.setPassword(writeDbPassword);
        config.setDriverClassName(driverClassName);
        config.setPoolName("write-pool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);

        log.info("Configured WRITE data source: {}", writeDbUrl);
        return new HikariDataSource(config);
    }

    /**
     * Create read (replica) data source.
     */
    @Bean
    public DataSource readDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(readDbUrl);
        config.setUsername(readDbUsername);
        config.setPassword(readDbPassword);
        config.setDriverClassName(driverClassName);
        config.setPoolName("read-pool");
        config.setMaximumPoolSize(15); // More connections for read pool
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setReadOnly(true); // Optimize for read-only operations

        log.info("Configured READ data source: {}", readDbUrl);
        return new HikariDataSource(config);
    }

    /**
     * Create routing data source that switches between read and write.
     */
    @Bean
    public DataSource dataSource() {
        RoutingDataSource routingDataSource = new RoutingDataSource();

        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put(DataSourceType.WRITE, writeDataSource());
        dataSourceMap.put(DataSourceType.READ, readDataSource());

        routingDataSource.setTargetDataSources(dataSourceMap);
        routingDataSource.setDefaultTargetDataSource(writeDataSource());

        // Wrap with lazy connection proxy to delay connection acquisition
        LazyConnectionDataSourceProxy lazyDataSource = new LazyConnectionDataSourceProxy(routingDataSource);

        log.info("Read/Write splitting data source configured");
        return lazyDataSource;
    }

    /**
     * Enum for data source types.
     */
    public enum DataSourceType {
        READ, WRITE
    }

    /**
     * Thread-local context holder for current data source type.
     */
    public static class DataSourceContextHolder {
        private static final ThreadLocal<DataSourceType> contextHolder = new ThreadLocal<>();

        public static void setDataSourceType(DataSourceType dataSourceType) {
            contextHolder.set(dataSourceType);
        }

        public static DataSourceType getDataSourceType() {
            return contextHolder.get();
        }

        public static void clearDataSourceType() {
            contextHolder.remove();
        }
    }

    /**
     * Routing data source implementation.
     */
    public static class RoutingDataSource extends AbstractRoutingDataSource {
        @Override
        protected Object determineCurrentLookupKey() {
            DataSourceType type = DataSourceContextHolder.getDataSourceType();
            if (type == null) {
                type = DataSourceType.WRITE; // Default to write
            }
            log.trace("Routing to {} data source", type);
            return type;
        }
    }
}