package com.focushive.identity.exception;

import com.focushive.identity.dto.OAuth2ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global error handler for OAuth2 and authentication exceptions.
 * Provides consistent error responses according to OAuth2 specifications.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class OAuth2ErrorHandler {

    /**
     * Handle OAuth2 authorization exceptions.
     */
    @ExceptionHandler(OAuth2AuthorizationException.class)
    public ResponseEntity<OAuth2ErrorResponse> handleOAuth2AuthorizationException(
            OAuth2AuthorizationException ex, HttpServletRequest request) {

        log.error("OAuth2 authorization error: {}", ex.getMessage());

        OAuth2ErrorResponse error = OAuth2ErrorResponse.builder()
            .error(ex.getErrorCode() != null ? ex.getErrorCode() : "unauthorized")
            .errorDescription(ex.getMessage())
            .errorUri(ex.getErrorUri())
            .state(ex.getState())
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .traceId(UUID.randomUUID().toString())
            .build();

        HttpStatus status = mapOAuth2ErrorToHttpStatus(ex.getErrorCode());
        return ResponseEntity.status(status).body(error);
    }

    /**
     * Handle invalid grant exceptions.
     */
    @ExceptionHandler(InvalidGrantException.class)
    public ResponseEntity<OAuth2ErrorResponse> handleInvalidGrantException(
            InvalidGrantException ex, HttpServletRequest request) {

        log.error("Invalid grant: {}", ex.getMessage());

        OAuth2ErrorResponse error = OAuth2ErrorResponse.builder()
            .error("invalid_grant")
            .errorDescription(ex.getMessage())
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .traceId(UUID.randomUUID().toString())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle invalid client exceptions.
     */
    @ExceptionHandler(InvalidClientException.class)
    public ResponseEntity<OAuth2ErrorResponse> handleInvalidClientException(
            InvalidClientException ex, HttpServletRequest request) {

        log.error("Invalid client: {}", ex.getMessage());

        OAuth2ErrorResponse error = OAuth2ErrorResponse.builder()
            .error("invalid_client")
            .errorDescription(ex.getMessage())
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .traceId(UUID.randomUUID().toString())
            .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle invalid request exceptions.
     */
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<OAuth2ErrorResponse> handleInvalidRequestException(
            InvalidRequestException ex, HttpServletRequest request) {

        log.error("Invalid request: {}", ex.getMessage());

        OAuth2ErrorResponse error = OAuth2ErrorResponse.builder()
            .error("invalid_request")
            .errorDescription(ex.getMessage())
            .errorUri(ex.getErrorUri())
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .traceId(UUID.randomUUID().toString())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle invalid scope exceptions.
     */
    @ExceptionHandler(InvalidScopeException.class)
    public ResponseEntity<OAuth2ErrorResponse> handleInvalidScopeException(
            InvalidScopeException ex, HttpServletRequest request) {

        log.error("Invalid scope: {}", ex.getMessage());

        OAuth2ErrorResponse error = OAuth2ErrorResponse.builder()
            .error("invalid_scope")
            .errorDescription(ex.getMessage())
            .scope(ex.getRequestedScope())
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .traceId(UUID.randomUUID().toString())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle insufficient scope exceptions.
     */
    @ExceptionHandler(InsufficientScopeException.class)
    public ResponseEntity<OAuth2ErrorResponse> handleInsufficientScopeException(
            InsufficientScopeException ex, HttpServletRequest request) {

        log.error("Insufficient scope: {}", ex.getMessage());

        OAuth2ErrorResponse error = OAuth2ErrorResponse.builder()
            .error("insufficient_scope")
            .errorDescription(ex.getMessage())
            .scope(ex.getRequiredScope())
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .traceId(UUID.randomUUID().toString())
            .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle rate limit exceeded exceptions.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<OAuth2ErrorResponse> handleRateLimitExceededException(
            RateLimitExceededException ex, HttpServletRequest request) {

        log.warn("Rate limit exceeded for client: {}", ex.getClientId());

        OAuth2ErrorResponse error = OAuth2ErrorResponse.builder()
            .error("rate_limit_exceeded")
            .errorDescription("Too many requests. Please retry after " + ex.getRetryAfterSeconds() + " seconds")
            .retryAfter(ex.getRetryAfterSeconds())
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .traceId(UUID.randomUUID().toString())
            .build();

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
            .body(error);
    }

    /**
     * Handle JWT token exceptions.
     */
    @ExceptionHandler({ExpiredJwtException.class})
    public ResponseEntity<OAuth2ErrorResponse> handleExpiredJwtException(
            ExpiredJwtException ex, HttpServletRequest request) {

        log.error("JWT token expired: {}", ex.getMessage());

        OAuth2ErrorResponse error = OAuth2ErrorResponse.builder()
            .error("token_expired")
            .errorDescription("The access token has expired")
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .traceId(UUID.randomUUID().toString())
            .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler({MalformedJwtException.class, SignatureException.class})
    public ResponseEntity<OAuth2ErrorResponse> handleInvalidJwtException(
            Exception ex, HttpServletRequest request) {

        log.error("Invalid JWT token: {}", ex.getMessage());

        OAuth2ErrorResponse error = OAuth2ErrorResponse.builder()
            .error("invalid_token")
            .errorDescription("The access token is invalid")
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .traceId(UUID.randomUUID().toString())
            .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle authentication exceptions.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<OAuth2ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {

        log.error("Bad credentials: {}", ex.getMessage());

        OAuth2ErrorResponse error = OAuth2ErrorResponse.builder()
            .error("invalid_credentials")
            .errorDescription("Invalid username or password")
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .traceId(UUID.randomUUID().toString())
            .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<OAuth2ErrorResponse> handleUsernameNotFoundException(
            UsernameNotFoundException ex, HttpServletRequest request) {

        log.error("Username not found: {}", ex.getMessage());

        OAuth2ErrorResponse error = OAuth2ErrorResponse.builder()
            .error("user_not_found")
            .errorDescription("User account not found")
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .traceId(UUID.randomUUID().toString())
            .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<OAuth2ErrorResponse> handleDisabledException(
            DisabledException ex, HttpServletRequest request) {

        log.error("Account disabled: {}", ex.getMessage());

        OAuth2ErrorResponse error = OAuth2ErrorResponse.builder()
            .error("account_disabled")
            .errorDescription("User account is disabled")
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .traceId(UUID.randomUUID().toString())
            .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<OAuth2ErrorResponse> handleLockedException(
            LockedException ex, HttpServletRequest request) {

        log.error("Account locked: {}", ex.getMessage());

        OAuth2ErrorResponse error = OAuth2ErrorResponse.builder()
            .error("account_locked")
            .errorDescription("User account is locked due to too many failed attempts")
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .traceId(UUID.randomUUID().toString())
            .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle generic authentication exceptions.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<OAuth2ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {

        log.error("Authentication error: {}", ex.getMessage());

        OAuth2ErrorResponse error = OAuth2ErrorResponse.builder()
            .error("authentication_failed")
            .errorDescription(ex.getMessage())
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .traceId(UUID.randomUUID().toString())
            .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle validation exceptions.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<OAuth2ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.error("Validation error: {}", errors);

        OAuth2ErrorResponse error = OAuth2ErrorResponse.builder()
            .error("validation_failed")
            .errorDescription("Request validation failed")
            .validationErrors(errors)
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .traceId(UUID.randomUUID().toString())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<OAuth2ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.error("Illegal argument: {}", ex.getMessage());

        OAuth2ErrorResponse error = OAuth2ErrorResponse.builder()
            .error("invalid_request")
            .errorDescription(ex.getMessage())
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .traceId(UUID.randomUUID().toString())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle illegal state exceptions.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<OAuth2ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, HttpServletRequest request) {

        log.error("Illegal state: {}", ex.getMessage());

        OAuth2ErrorResponse error = OAuth2ErrorResponse.builder()
            .error("server_error")
            .errorDescription("An internal server error occurred")
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .traceId(UUID.randomUUID().toString())
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handle type mismatch exceptions.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<OAuth2ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String message = String.format("Parameter '%s' should be of type %s",
            ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        log.error("Type mismatch: {}", message);

        OAuth2ErrorResponse error = OAuth2ErrorResponse.builder()
            .error("invalid_request")
            .errorDescription(message)
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .traceId(UUID.randomUUID().toString())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle generic exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<OAuth2ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error: ", ex);

        OAuth2ErrorResponse error = OAuth2ErrorResponse.builder()
            .error("server_error")
            .errorDescription("An unexpected error occurred. Please try again later.")
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .traceId(UUID.randomUUID().toString())
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Map OAuth2 error codes to HTTP status codes.
     */
    private HttpStatus mapOAuth2ErrorToHttpStatus(String errorCode) {
        if (errorCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return switch (errorCode) {
            case "invalid_request", "invalid_grant", "invalid_scope", "unsupported_response_type",
                 "unsupported_grant_type" -> HttpStatus.BAD_REQUEST;
            case "invalid_client", "invalid_token", "unauthorized" -> HttpStatus.UNAUTHORIZED;
            case "insufficient_scope", "access_denied" -> HttpStatus.FORBIDDEN;
            case "server_error", "temporarily_unavailable" -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}