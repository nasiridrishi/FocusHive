package com.focushive.identity.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.HttpMediaTypeNotSupportedException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for Identity Service REST controllers.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * Handle validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        response.put("error", "Validation failed");
        response.put("errors", errors);
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Handle malformed JSON or missing request body.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex) {
        log.error("Malformed JSON request: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Invalid JSON format or missing request body");
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Handle type mismatch exceptions (e.g., invalid UUID format in path parameters).
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, TypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> handleTypeMismatchException(
            Exception ex) {
        log.error("Type mismatch error: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        
        if (ex instanceof MethodArgumentTypeMismatchException typeMismatchEx) {
            String paramName = typeMismatchEx.getName();
            String paramType = typeMismatchEx.getRequiredType() != null ? 
                typeMismatchEx.getRequiredType().getSimpleName() : "unknown";
            
            if ("UUID".equals(paramType)) {
                response.put("error", "Invalid " + paramName + " format. Expected valid UUID.");
            } else if ("PersonaType".equals(paramType)) {
                response.put("error", "Invalid " + paramName + ". Expected one of: WORK, PERSONAL, GAMING, STUDY, CUSTOM");
            } else {
                response.put("error", "Invalid " + paramName + " format");
            }
        } else {
            response.put("error", "Invalid parameter format");
        }
        
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Handle data integrity violations (e.g., unique constraint violations).
     */
    @ExceptionHandler({DataIntegrityViolationException.class, JpaSystemException.class})
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolationException(
            Exception ex) {
        log.error("Data integrity/JPA exception: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        
        String message = ex.getMessage();
        
        // For JpaSystemException, check the cause chain for the actual constraint violation
        if (ex instanceof JpaSystemException && ex.getCause() != null) {
            Throwable cause = ex.getCause();
            while (cause != null) {
                if (cause.getMessage() != null) {
                    message = cause.getMessage();
                    break;
                }
                cause = cause.getCause();
            }
        }
        
        // Check if it's an email uniqueness constraint violation
        if (message.contains("users_email_key") || 
            (message.contains("duplicate key value") && message.contains("email"))) {
            response.put("error", "Email already registered");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        
        // Check if it's a username uniqueness constraint violation
        if (message.contains("users_username_key") || 
            (message.contains("duplicate key value") && message.contains("username"))) {
            response.put("error", "Username already taken");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        
        // Generic constraint violation
        response.put("error", "Data constraint violation");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
    
    /**
     * Handle account locked exceptions.
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountLockedException(
            AccountLockedException ex) {
        log.warn("Account locked: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Account locked");
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle authentication exceptions.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(
            AuthenticationException ex) {
        log.error("Authentication failed: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("error", ex.getMessage());

        // Registration errors should return 409 Conflict
        if (ex.getMessage().contains("already registered") ||
            ex.getMessage().contains("already taken")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    /**
     * Handle bad credentials exception.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(
            BadCredentialsException ex) {
        log.error("Bad credentials: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Invalid credentials");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    /**
     * Handle runtime exceptions.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex) {
        log.error("Runtime exception occurred: {}", ex.getMessage(), ex);
        Map<String, Object> response = new HashMap<>();
        
        if (ex.getMessage().contains("Persona not found")) {
            response.put("error", "Persona not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        
        if (ex.getMessage().contains("Unauthorized access to persona")) {
            response.put("error", "Unauthorized access to persona");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        response.put("error", "Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * Handle illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        log.error("Invalid argument: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("error", ex.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle rate limit exceeded exceptions.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceededException(
            RateLimitExceededException ex) {
        log.warn("Rate limit exceeded for client: {} on endpoint: {}",
                ex.getClientId(), ex.getEndpoint());

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Rate limit exceeded");
        response.put("message", ex.getMessage());
        response.put("retry_after", ex.getRetryAfterSeconds());

        // Add rate limit information if available
        if (ex.getRemainingRequests() != null) {
            response.put("remaining_requests", ex.getRemainingRequests());
        }
        if (ex.getRateLimit() != null) {
            response.put("rate_limit", ex.getRateLimit());
        }

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("X-RateLimit-Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(response);
    }

    /**
     * Handle unsupported media type (A08: Prevents deserialization attacks).
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException ex) {
        log.warn("Unsupported media type attempted: {}", ex.getContentType());
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Unsupported media type");
        response.put("message", "Only JSON content type is supported for API endpoints");
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
    }

    /**
     * Handle unsupported HTTP methods.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex) {
        log.warn("Unsupported HTTP method attempted: {}", ex.getMethod());
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Method not allowed");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * Handle 404 Not Found errors (A05: Security Misconfiguration).
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFoundException(
            NoHandlerFoundException ex) {
        log.warn("Resource not found: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Resource not found");
        // Don't expose internal path information
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle general exceptions (A05: Hide debug information).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(
            Exception ex) {
        log.error("Unexpected error occurred", ex);
        Map<String, Object> response = new HashMap<>();
        response.put("error", "An unexpected error occurred");
        // Never expose stack traces, internal details, or debug information
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}