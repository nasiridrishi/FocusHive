package com.focushive.buddy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC configuration to ensure proper Jackson setup for LocalDate/LocalDateTime.
 * This configures the HTTP message converters used by Spring MVC.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Find the Jackson converter and configure it
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                MappingJackson2HttpMessageConverter jsonConverter = (MappingJackson2HttpMessageConverter) converter;

                // Get the existing ObjectMapper or create a new one
                ObjectMapper objectMapper = jsonConverter.getObjectMapper();

                // Register JavaTimeModule for Java 8 time support
                objectMapper.registerModule(new JavaTimeModule());

                // Configure serialization features
                objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

                // Also enable finding and registering all modules on classpath
                objectMapper.findAndRegisterModules();

                // Set the updated ObjectMapper back
                jsonConverter.setObjectMapper(objectMapper);
            }
        }
    }
}