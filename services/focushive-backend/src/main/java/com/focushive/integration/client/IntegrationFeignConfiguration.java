package com.focushive.integration.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.api.client.ActuatorAwareDecoder;
import feign.Logger;
import feign.Request;
import feign.Retryer;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.TimeUnit;

/**
 * Shared Feign configuration for all microservice clients.
 * Configures timeouts, retries, logging, and error handling.
 */
@Slf4j
@Configuration
public class IntegrationFeignConfiguration {

    private final ObjectMapper objectMapper;

    @Autowired
    public IntegrationFeignConfiguration(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Custom decoder that handles Spring Boot Actuator vendor JSON types.
     */
    @Bean
    public Decoder feignDecoder() {
        return new ActuatorAwareDecoder(objectMapper);
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
            5000, TimeUnit.MILLISECONDS,  // Connect timeout
            10000, TimeUnit.MILLISECONDS, // Read timeout
            true                           // Follow redirects
        );
    }

    @Bean
    public Retryer retryer() {
        // Retry up to 3 times with 1 second initial interval
        return new Retryer.Default(1000, 3000, 3);
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }

    /**
     * Custom error decoder for handling service-specific errors.
     */
    @Slf4j
    public static class CustomErrorDecoder implements ErrorDecoder {
        private final ErrorDecoder defaultErrorDecoder = new Default();

        @Override
        public Exception decode(String methodKey, feign.Response response) {
            log.error("Feign client error - Method: {}, Status: {}", methodKey, response.status());

            switch (response.status()) {
                case 400:
                    return new ServiceBadRequestException("Bad request to service: " + methodKey);
                case 404:
                    return new ServiceNotFoundException("Service endpoint not found: " + methodKey);
                case 503:
                    return new ServiceUnavailableException("Service temporarily unavailable: " + methodKey);
                default:
                    return defaultErrorDecoder.decode(methodKey, response);
            }
        }
    }

    // Custom exception classes
    public static class ServiceBadRequestException extends RuntimeException {
        public ServiceBadRequestException(String message) {
            super(message);
        }
    }

    public static class ServiceNotFoundException extends RuntimeException {
        public ServiceNotFoundException(String message) {
            super(message);
        }
    }

    public static class ServiceUnavailableException extends RuntimeException {
        public ServiceUnavailableException(String message) {
            super(message);
        }
    }
}