package com.focushive.identity.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

/**
 * Test database configuration for H2 database schema initialization.
 */
@TestConfiguration
@Profile("test")
public class TestDatabaseConfig {

    @Bean
    @Primary
    public DataSourceInitializer dataSourceInitializer(DataSource dataSource) {
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        
        // Create a database populator that creates the schema
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        
        // Add schema creation script - we'll create this manually since Hibernate isn't working
        populator.addScript(new ClassPathResource("test-schema.sql"));
        populator.setContinueOnError(false);
        populator.setSeparator(";");
        
        initializer.setDatabasePopulator(populator);
        
        return initializer;
    }
}