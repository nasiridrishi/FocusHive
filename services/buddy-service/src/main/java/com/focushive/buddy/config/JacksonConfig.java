package com.focushive.buddy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson configuration for proper handling of Java 8 time types.
 * This creates the primary ObjectMapper bean used by Spring for JSON serialization.
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        // Use Spring's builder to create ObjectMapper with all auto-configured settings
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();

        // Explicitly register JavaTimeModule
        objectMapper.registerModule(new JavaTimeModule());

        // Disable writing dates as timestamps
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Find and register all modules on classpath
        objectMapper.findAndRegisterModules();

        return objectMapper;
    }
}