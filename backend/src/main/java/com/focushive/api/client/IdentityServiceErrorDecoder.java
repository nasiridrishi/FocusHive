package com.focushive.api.client;

import com.focushive.api.exception.IdentityServiceException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

/**
 * Custom error decoder for Identity Service responses.
 */
@Slf4j
public class IdentityServiceErrorDecoder implements ErrorDecoder {
    
    private final ErrorDecoder defaultErrorDecoder = new Default();
    
    @Override
    public Exception decode(String methodKey, Response response) {
        HttpStatus status = HttpStatus.valueOf(response.status());
        
        if (status == HttpStatus.UNAUTHORIZED) {
            log.error("Authentication failed when calling Identity Service method: {}", methodKey);
            return new IdentityServiceException("Authentication failed", status.value());
        }
        
        if (status == HttpStatus.FORBIDDEN) {
            log.error("Access denied when calling Identity Service method: {}", methodKey);
            return new IdentityServiceException("Access denied", status.value());
        }
        
        if (status == HttpStatus.NOT_FOUND) {
            log.error("Resource not found when calling Identity Service method: {}", methodKey);
            return new IdentityServiceException("Resource not found", status.value());
        }
        
        if (status.is5xxServerError()) {
            log.error("Identity Service error when calling method: {} - Status: {}", methodKey, status);
            return new IdentityServiceException("Identity Service unavailable", status.value());
        }
        
        return defaultErrorDecoder.decode(methodKey, response);
    }
}