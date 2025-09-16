package com.focushive.buddy.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;

import javax.sql.DataSource;

/**
 * E2E Test configuration that explicitly configures PostgreSQL DataSource
 */
@TestConfiguration
public class E2ETestConfig {

    private static PostgreSQLContainer<?> postgresql;
    private static GenericContainer<?> redis;

    static {
        postgresql = new PostgreSQLContainer<>("postgres:15")
                .withDatabaseName("buddy_service_e2e")
                .withUsername("e2e_user")
                .withPassword("e2e_password");
        postgresql.start();

        redis = new GenericContainer<>("redis:7-alpine")
                .withExposedPorts(6379)
                .withCommand("redis-server");
        redis.start();
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(postgresql.getJdbcUrl());
        hikariConfig.setUsername(postgresql.getUsername());
        hikariConfig.setPassword(postgresql.getPassword());
        hikariConfig.setDriverClassName("org.postgresql.Driver");
        hikariConfig.setMaximumPoolSize(5);
        return new HikariDataSource(hikariConfig);
    }

    public static String getRedisHost() {
        return redis.getHost();
    }

    public static Integer getRedisPort() {
        return redis.getMappedPort(6379);
    }
}