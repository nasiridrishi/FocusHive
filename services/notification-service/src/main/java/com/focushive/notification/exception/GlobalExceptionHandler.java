package com.focushive.notification.exception;

import com.focushive.notification.dto.ErrorResponse;
import com.focushive.notification.service.EmailDeliveryException;
import com.focushive.notification.service.TemplateNotFoundException;
import com.focushive.notification.service.TemplateValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler for the Notification Service.
 * Provides centralized error handling across all controllers with:
 * - Consistent error response format
 * - Proper HTTP status codes
 * - Security-aware error messages (avoiding information leakage)
 * - Correlation IDs for tracking
 * - Comprehensive logging
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String INTERNAL_ERROR_MESSAGE = "An internal error occurred. Please contact support if the issue persists.";
    private static final String VALIDATION_ERROR_MESSAGE = "Validation failed for one or more fields.";
    private static final String ACCESS_DENIED_MESSAGE = "You do not have permission to access this resource.";
    private static final String AUTHENTICATION_ERROR_MESSAGE = "Authentication failed. Please check your credentials.";
    private static final String NOT_FOUND_MESSAGE = "The requested resource was not found.";

    /**
     * Handle validation errors from @Valid annotation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String correlationId = generateCorrelationId();
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed for request to {}: {}, correlationId: {}",
                request.getRequestURI(), errors, correlationId);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message(VALIDATION_ERROR_MESSAGE)
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .validationErrors(errors)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle constraint validation errors.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        String correlationId = generateCorrelationId();
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();

        Map<String, String> errors = violations.stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing
                ));

        log.warn("Constraint violation for request to {}: {}, correlationId: {}",
                request.getRequestURI(), errors, correlationId);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Constraint Violation")
                .message(VALIDATION_ERROR_MESSAGE)
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .validationErrors(errors)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle resource not found exceptions.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        String correlationId = generateCorrelationId();
        log.warn("Resource not found: {}, correlationId: {}", ex.getMessage(), correlationId);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle template not found exceptions.
     */
    @ExceptionHandler(TemplateNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleTemplateNotFoundException(
            TemplateNotFoundException ex,
            HttpServletRequest request) {

        String correlationId = generateCorrelationId();
        log.warn("Template not found: {}, correlationId: {}", ex.getMessage(), correlationId);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Template Not Found")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle template validation exceptions.
     */
    @ExceptionHandler(TemplateValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleTemplateValidationException(
            TemplateValidationException ex,
            HttpServletRequest request) {

        String correlationId = generateCorrelationId();
        log.warn("Template validation failed: {}, correlationId: {}", ex.getMessage(), correlationId);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Template Validation Failed")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle email delivery exceptions.
     */
    @ExceptionHandler(EmailDeliveryException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ResponseEntity<ErrorResponse> handleEmailDeliveryException(
            EmailDeliveryException ex,
            HttpServletRequest request) {

        String correlationId = generateCorrelationId();
        log.error("Email delivery failed: {}, correlationId: {}", ex.getMessage(), correlationId, ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Email Delivery Failed")
                .message("Unable to deliver email notification. Please try again later.")
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    /**
     * Handle data integrity violations.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        String correlationId = generateCorrelationId();
        String userMessage = "A conflict occurred with existing data.";

        // Check for specific constraint violations
        String message = ex.getMessage();
        if (message != null) {
            if (message.contains("unique") || message.contains("duplicate")) {
                userMessage = "This resource already exists.";
            } else if (message.contains("foreign key")) {
                userMessage = "This operation would violate data integrity constraints.";
            }
        }

        log.error("Data integrity violation: {}, correlationId: {}", ex.getMessage(), correlationId);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Data Conflict")
                .message(userMessage)
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handle access denied exceptions.
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {

        String correlationId = generateCorrelationId();
        log.warn("Access denied for request to {}, correlationId: {}",
                request.getRequestURI(), correlationId);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Access Denied")
                .message(ACCESS_DENIED_MESSAGE)
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handle authentication exceptions.
     */
    @ExceptionHandler({AuthenticationException.class, UsernameNotFoundException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            Exception ex,
            HttpServletRequest request) {

        String correlationId = generateCorrelationId();
        log.warn("Authentication failed for request to {}, correlationId: {}",
                request.getRequestURI(), correlationId);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Authentication Failed")
                .message(AUTHENTICATION_ERROR_MESSAGE)
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handle method argument type mismatch.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String correlationId = generateCorrelationId();
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());

        log.warn("Method argument type mismatch: {}, correlationId: {}", message, correlationId);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid Parameter")
                .message(message)
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle missing request parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        String correlationId = generateCorrelationId();
        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());

        log.warn("Missing request parameter: {}, correlationId: {}", message, correlationId);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Missing Parameter")
                .message(message)
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle HTTP message not readable exception.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        String correlationId = generateCorrelationId();
        String message = "Malformed JSON request";

        // Extract more specific error if available
        Throwable cause = ex.getCause();
        if (cause != null) {
            String causeMessage = cause.getMessage();
            if (causeMessage != null && causeMessage.contains("enum")) {
                message = "Invalid enum value provided";
            } else if (causeMessage != null && causeMessage.contains("Date")) {
                message = "Invalid date format. Expected format: yyyy-MM-dd";
            }
        }

        log.warn("HTTP message not readable: {}, correlationId: {}", ex.getMessage(), correlationId);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid Request")
                .message(message)
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle HTTP request method not supported.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        String correlationId = generateCorrelationId();
        String message = String.format("Method '%s' not supported for this endpoint. Supported methods: %s",
                ex.getMethod(), String.join(", ", ex.getSupportedMethods()));

        log.warn("Method not supported: {}, correlationId: {}", message, correlationId);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error("Method Not Allowed")
                .message(message)
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    /**
     * Handle media type not supported.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {

        String correlationId = generateCorrelationId();
        String message = String.format("Media type '%s' not supported. Supported types: %s",
                ex.getContentType(),
                ex.getSupportedMediaTypes().stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(", ")));

        log.warn("Media type not supported: {}, correlationId: {}", message, correlationId);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .error("Unsupported Media Type")
                .message(message)
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(errorResponse);
    }

    /**
     * Handle no handler found exception.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            HttpServletRequest request) {

        String correlationId = generateCorrelationId();
        String message = String.format("No endpoint found for %s %s",
                ex.getHttpMethod(), ex.getRequestURL());

        log.warn("No handler found: {}, correlationId: {}", message, correlationId);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Endpoint Not Found")
                .message(message)
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle all other exceptions (fallback handler).
     * This is the last resort for unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            HttpServletRequest request) {

        String correlationId = generateCorrelationId();

        // Log the full exception for debugging but don't expose details to client
        log.error("Unexpected error processing request to {}, correlationId: {}",
                request.getRequestURI(), correlationId, ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message(INTERNAL_ERROR_MESSAGE)
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Generate a unique correlation ID for request tracking.
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}