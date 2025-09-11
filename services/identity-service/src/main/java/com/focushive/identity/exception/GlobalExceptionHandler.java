package com.focushive.identity.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
     * Handle authentication exceptions.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(
            AuthenticationException ex) {
        log.error("Authentication failed: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("error", ex.getMessage());
        
        // Registration errors should return 400 Bad Request
        if (ex.getMessage().contains("already registered") || 
            ex.getMessage().contains("already taken")) {
            return ResponseEntity.badRequest().body(response);
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
     * Handle general exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(
            Exception ex) {
        log.error("Unexpected error occurred", ex);
        Map<String, Object> response = new HashMap<>();
        response.put("error", "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}