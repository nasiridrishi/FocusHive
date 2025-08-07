package com.focushive.music.client;

import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Configuration for Feign clients used in inter-service communication.
 * 
 * Provides common configuration including authentication, error handling,
 * and logging for all Feign clients in the music service.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Slf4j
@Configuration
public class FeignClientConfig {

    @Value("${services.music-service.api-key:}")
    private String musicServiceApiKey;

    /**
     * Configures request interceptor to add authentication headers.
     * 
     * @return RequestInterceptor for adding auth headers
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // Add JWT token if available in security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                requestTemplate.header("Authorization", "Bearer " + jwt.getTokenValue());
            }

            // Add API key for service-to-service authentication
            if (musicServiceApiKey != null && !musicServiceApiKey.isEmpty()) {
                requestTemplate.header("X-API-Key", musicServiceApiKey);
            }

            // Add common headers
            requestTemplate.header("Content-Type", "application/json");
            requestTemplate.header("Accept", "application/json");
            requestTemplate.header("User-Agent", "focushive-music-service/1.0");
        };
    }

    /**
     * Configures Feign logger level.
     * 
     * @return Logger level for Feign clients
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Configures custom error decoder for better error handling.
     * 
     * @return ErrorDecoder for handling Feign errors
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }

    /**
     * Custom error decoder for Feign client errors.
     */
    @Slf4j
    public static class FeignErrorDecoder implements ErrorDecoder {
        
        private final ErrorDecoder defaultErrorDecoder = new Default();

        @Override
        public Exception decode(String methodKey, feign.Response response) {
            HttpStatus status = HttpStatus.valueOf(response.status());
            String reason = response.reason();
            
            log.warn("Feign client error - Method: {}, Status: {}, Reason: {}", 
                methodKey, status, reason);

            return switch (status) {
                case BAD_REQUEST -> new FeignBadRequestException(
                    String.format("Bad request to %s: %s", methodKey, reason));
                case UNAUTHORIZED -> new FeignUnauthorizedException(
                    String.format("Unauthorized access to %s: %s", methodKey, reason));
                case FORBIDDEN -> new FeignForbiddenException(
                    String.format("Forbidden access to %s: %s", methodKey, reason));
                case NOT_FOUND -> new FeignNotFoundException(
                    String.format("Resource not found for %s: %s", methodKey, reason));
                case INTERNAL_SERVER_ERROR -> new FeignServerException(
                    String.format("Server error for %s: %s", methodKey, reason));
                case SERVICE_UNAVAILABLE -> new FeignServiceUnavailableException(
                    String.format("Service unavailable for %s: %s", methodKey, reason));
                default -> defaultErrorDecoder.decode(methodKey, response);
            };
        }
    }

    /**
     * Base class for Feign client exceptions.
     */
    public static abstract class FeignClientException extends RuntimeException {
        protected FeignClientException(String message) {
            super(message);
        }
        
        protected FeignClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception for 400 Bad Request responses.
     */
    public static class FeignBadRequestException extends FeignClientException {
        public FeignBadRequestException(String message) {
            super(message);
        }
    }

    /**
     * Exception for 401 Unauthorized responses.
     */
    public static class FeignUnauthorizedException extends FeignClientException {
        public FeignUnauthorizedException(String message) {
            super(message);
        }
    }

    /**
     * Exception for 403 Forbidden responses.
     */
    public static class FeignForbiddenException extends FeignClientException {
        public FeignForbiddenException(String message) {
            super(message);
        }
    }

    /**
     * Exception for 404 Not Found responses.
     */
    public static class FeignNotFoundException extends FeignClientException {
        public FeignNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception for 500 Internal Server Error responses.
     */
    public static class FeignServerException extends FeignClientException {
        public FeignServerException(String message) {
            super(message);
        }
    }

    /**
     * Exception for 503 Service Unavailable responses.
     */
    public static class FeignServiceUnavailableException extends FeignClientException {
        public FeignServiceUnavailableException(String message) {
            super(message);
        }
    }
}