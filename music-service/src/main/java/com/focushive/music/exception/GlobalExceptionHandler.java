package com.focushive.music.exception;

import com.focushive.music.client.FeignClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the Music Service.
 * 
 * Handles all exceptions thrown by controllers and services, providing
 * consistent error responses across the application.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles Music Service specific exceptions.
     */
    @ExceptionHandler(MusicServiceException.class)
    public ResponseEntity<ErrorResponse> handleMusicServiceException(MusicServiceException ex, WebRequest request) {
        log.error("Music service exception occurred", ex);
        
        HttpStatus status = determineHttpStatus(ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(ex.getMessage())
            .errorCode(ex.getErrorCode())
            .path(extractPath(request))
            .build();
            
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Handles Feign client exceptions.
     */
    @ExceptionHandler(FeignClientConfig.FeignClientException.class)
    public ResponseEntity<ErrorResponse> handleFeignClientException(FeignClientConfig.FeignClientException ex, WebRequest request) {
        log.error("Feign client exception occurred", ex);
        
        HttpStatus status = switch (ex) {
            case FeignClientConfig.FeignBadRequestException ignored -> HttpStatus.BAD_REQUEST;
            case FeignClientConfig.FeignUnauthorizedException ignored -> HttpStatus.UNAUTHORIZED;
            case FeignClientConfig.FeignForbiddenException ignored -> HttpStatus.FORBIDDEN;
            case FeignClientConfig.FeignNotFoundException ignored -> HttpStatus.NOT_FOUND;
            case FeignClientConfig.FeignServerException ignored -> HttpStatus.INTERNAL_SERVER_ERROR;
            case FeignClientConfig.FeignServiceUnavailableException ignored -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(status.value())
            .error(status.getReasonPhrase())
            .message("External service error: " + ex.getMessage())
            .errorCode("EXTERNAL_SERVICE_ERROR")
            .path(extractPath(request))
            .build();
            
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Handles validation errors for request bodies.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation exception occurred", ex);
        
        Map<String, String> fieldErrors = new HashMap<>();
        List<String> globalErrors = new ArrayList<>();
        
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
            } else {
                globalErrors.add(error.getDefaultMessage());
            }
        });
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Request validation failed")
            .errorCode("VALIDATION_ERROR")
            .path(extractPath(request))
            .fieldErrors(fieldErrors)
            .details(globalErrors.isEmpty() ? null : globalErrors)
            .build();
            
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles constraint violations.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        log.warn("Constraint violation exception occurred", ex);
        
        Map<String, String> violations = ex.getConstraintViolations()
            .stream()
            .collect(Collectors.toMap(
                violation -> violation.getPropertyPath().toString(),
                ConstraintViolation::getMessage,
                (existing, replacement) -> existing
            ));
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Constraint Violation")
            .message("Request constraints violated")
            .errorCode("CONSTRAINT_VIOLATION")
            .path(extractPath(request))
            .fieldErrors(violations)
            .build();
            
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles authentication exceptions.
     */
    @ExceptionHandler({AuthenticationException.class, AuthenticationCredentialsNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleAuthenticationException(Exception ex, WebRequest request) {
        log.warn("Authentication exception occurred", ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.UNAUTHORIZED.value())
            .error("Unauthorized")
            .message("Authentication required")
            .errorCode("AUTHENTICATION_REQUIRED")
            .path(extractPath(request))
            .build();
            
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handles access denied exceptions.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied exception occurred", ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Forbidden")
            .message("Access denied: " + ex.getMessage())
            .errorCode("ACCESS_DENIED")
            .path(extractPath(request))
            .build();
            
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handles illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument exception occurred", ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .message("Invalid argument: " + ex.getMessage())
            .errorCode("INVALID_ARGUMENT")
            .path(extractPath(request))
            .build();
            
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles method argument type mismatch exceptions.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException ex, WebRequest request) {
        log.warn("Method argument type mismatch exception occurred", ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .message(String.format("Invalid value '%s' for parameter '%s'. Expected type: %s", 
                ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName()))
            .errorCode("TYPE_MISMATCH")
            .path(extractPath(request))
            .build();
            
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles HTTP message not readable exceptions.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("HTTP message not readable exception occurred", ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .message("Invalid request body")
            .errorCode("INVALID_REQUEST_BODY")
            .path(extractPath(request))
            .build();
            
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles no handler found exceptions (404).
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(NoHandlerFoundException ex, WebRequest request) {
        log.warn("No handler found exception occurred", ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Not Found")
            .message(String.format("No handler found for %s %s", ex.getHttpMethod(), ex.getRequestURL()))
            .errorCode("HANDLER_NOT_FOUND")
            .path(extractPath(request))
            .build();
            
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected exception occurred", ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .errorCode("INTERNAL_ERROR")
            .path(extractPath(request))
            .build();
            
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Determines HTTP status based on exception type.
     */
    private HttpStatus determineHttpStatus(MusicServiceException ex) {
        return switch (ex.getErrorCode()) {
            case "RESOURCE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "UNAUTHORIZED_OPERATION", "ACCESS_DENIED" -> HttpStatus.FORBIDDEN;
            case "VALIDATION_ERROR", "CONSTRAINT_VIOLATION", "INVALID_ARGUMENT" -> HttpStatus.BAD_REQUEST;
            case "BUSINESS_RULE_VIOLATION" -> HttpStatus.UNPROCESSABLE_ENTITY;
            case "RATE_LIMIT_EXCEEDED" -> HttpStatus.TOO_MANY_REQUESTS;
            case "EXTERNAL_SERVICE_ERROR", "STREAMING_SERVICE_ERROR" -> HttpStatus.BAD_GATEWAY;
            case "CONCURRENT_MODIFICATION" -> HttpStatus.CONFLICT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    /**
     * Extracts the path from the web request.
     */
    private String extractPath(WebRequest request) {
        return request.getDescription(false).replaceFirst("uri=", "");
    }

    /**
     * Error response DTO.
     */
    public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String errorCode,
        String path,
        Map<String, String> fieldErrors,
        List<String> details
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private LocalDateTime timestamp;
            private int status;
            private String error;
            private String message;
            private String errorCode;
            private String path;
            private Map<String, String> fieldErrors;
            private List<String> details;

            public Builder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public Builder status(int status) {
                this.status = status;
                return this;
            }

            public Builder error(String error) {
                this.error = error;
                return this;
            }

            public Builder message(String message) {
                this.message = message;
                return this;
            }

            public Builder errorCode(String errorCode) {
                this.errorCode = errorCode;
                return this;
            }

            public Builder path(String path) {
                this.path = path;
                return this;
            }

            public Builder fieldErrors(Map<String, String> fieldErrors) {
                this.fieldErrors = fieldErrors;
                return this;
            }

            public Builder details(List<String> details) {
                this.details = details;
                return this;
            }

            public ErrorResponse build() {
                return new ErrorResponse(timestamp, status, error, message, errorCode, path, fieldErrors, details);
            }
        }
    }
}