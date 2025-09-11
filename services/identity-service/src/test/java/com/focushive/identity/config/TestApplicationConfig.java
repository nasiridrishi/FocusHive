package com.focushive.identity.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.IdentityServiceApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * Test application configuration to ensure proper component scanning,
 * controller registration, and JSON serialization for integration tests.
 */
@TestConfiguration
@Profile("test")
@ComponentScan(basePackages = "com.focushive.identity")
@Import({TestConfig.class})
public class TestApplicationConfig {

    /**
     * Configure ObjectMapper for proper JSON serialization in tests.
     * This ensures ResponseEntity<Map<String, Object>> returns are properly serialized to JSON.
     */
    @Bean
    @Primary
    public ObjectMapper testObjectMapper() {
        return new ObjectMapper();
    }
    
    /**
     * Configure Jackson HTTP message converter for MockMvc tests.
     * This ensures proper JSON serialization of ResponseEntity<Map> objects.
     */
    @Bean
    @Primary
    public MappingJackson2HttpMessageConverter testMessageConverter() {
        return new MappingJackson2HttpMessageConverter(testObjectMapper());
    }

    /**
     * Ensures all controllers are properly scanned and registered in test context.
     * This addresses the issue where controllers were not being registered
     * when running multiple tests together.
     */
    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}