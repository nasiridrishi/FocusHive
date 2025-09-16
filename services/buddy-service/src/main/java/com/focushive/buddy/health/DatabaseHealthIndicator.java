package com.focushive.buddy.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Custom health indicator for database connectivity
 * Performs a simple query to verify database is accessible and responsive
 */
@Component("database")
public class DatabaseHealthIndicator implements HealthIndicator {

    @Autowired
    private DataSource dataSource;

    @Override
    public Health health() {
        try {
            return checkDatabaseHealth();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private Health checkDatabaseHealth() throws Exception {
        long startTime = System.currentTimeMillis();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1")) {

            if (resultSet.next()) {
                long responseTime = System.currentTimeMillis() - startTime;

                return Health.up()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("responseTime", responseTime + "ms")
                        .withDetail("status", "Available")
                        .build();
            }
        }

        return Health.down()
                .withDetail("error", "Failed to execute test query")
                .build();
    }
}