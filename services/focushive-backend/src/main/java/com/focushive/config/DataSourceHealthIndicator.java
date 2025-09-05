// Health indicator temporarily disabled to focus on core PostgreSQL configuration tests
// Will be re-enabled after core functionality is validated
package com.focushive.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Health indicator for PostgreSQL database connection and HikariCP pool metrics.
 * Currently disabled to focus on core PostgreSQL configuration tests.
 */
@Component
@Slf4j
public class DataSourceHealthIndicator {
    
    public DataSourceHealthIndicator() {
        log.info("DataSource Health Indicator initialized (monitoring features disabled)");
    }
}