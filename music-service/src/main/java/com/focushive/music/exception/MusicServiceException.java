package com.focushive.music.exception;

/**
 * Base exception for Music Service specific errors.
 * 
 * All custom exceptions in the music service should extend this base class
 * to ensure consistent error handling and messaging.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
public abstract class MusicServiceException extends RuntimeException {
    
    private final String errorCode;
    
    /**
     * Creates a new MusicServiceException with the specified message and error code.
     * 
     * @param message Error message
     * @param errorCode Unique error code for this exception type
     */
    protected MusicServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * Creates a new MusicServiceException with the specified message, cause, and error code.
     * 
     * @param message Error message
     * @param cause Root cause exception
     * @param errorCode Unique error code for this exception type
     */
    protected MusicServiceException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * Gets the error code for this exception.
     * 
     * @return Error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Exception thrown when a requested resource is not found.
     */
    public static class ResourceNotFoundException extends MusicServiceException {
        public ResourceNotFoundException(String resourceType, String identifier) {
            super(String.format("%s with identifier '%s' not found", resourceType, identifier), "RESOURCE_NOT_FOUND");
        }
        
        public ResourceNotFoundException(String message) {
            super(message, "RESOURCE_NOT_FOUND");
        }
    }

    /**
     * Exception thrown when a user is not authorized to perform an operation.
     */
    public static class UnauthorizedOperationException extends MusicServiceException {
        public UnauthorizedOperationException(String operation) {
            super(String.format("User is not authorized to perform operation: %s", operation), "UNAUTHORIZED_OPERATION");
        }
        
        public UnauthorizedOperationException(String message, String errorCode) {
            super(message, errorCode);
        }
    }

    /**
     * Exception thrown when a streaming service operation fails.
     */
    public static class StreamingServiceException extends MusicServiceException {
        public StreamingServiceException(String serviceName, String operation, Throwable cause) {
            super(String.format("Streaming service '%s' failed during operation '%s': %s", 
                  serviceName, operation, cause.getMessage()), cause, "STREAMING_SERVICE_ERROR");
        }
        
        public StreamingServiceException(String serviceName, String operation) {
            super(String.format("Streaming service '%s' failed during operation '%s'", serviceName, operation), "STREAMING_SERVICE_ERROR");
        }
        
        public StreamingServiceException(String message) {
            super(message, "STREAMING_SERVICE_ERROR");
        }
    }

    /**
     * Exception thrown when playlist operations fail.
     */
    public static class PlaylistOperationException extends MusicServiceException {
        public PlaylistOperationException(String operation, String reason) {
            super(String.format("Playlist operation '%s' failed: %s", operation, reason), "PLAYLIST_OPERATION_FAILED");
        }
        
        public PlaylistOperationException(String operation, Throwable cause) {
            super(String.format("Playlist operation '%s' failed", operation), cause, "PLAYLIST_OPERATION_FAILED");
        }
    }

    /**
     * Exception thrown when collaborative features encounter errors.
     */
    public static class CollaborativeFeatureException extends MusicServiceException {
        public CollaborativeFeatureException(String feature, String reason) {
            super(String.format("Collaborative feature '%s' failed: %s", feature, reason), "COLLABORATIVE_FEATURE_ERROR");
        }
        
        public CollaborativeFeatureException(String message, Throwable cause) {
            super(message, cause, "COLLABORATIVE_FEATURE_ERROR");
        }
    }

    /**
     * Exception thrown when music recommendation generation fails.
     */
    public static class RecommendationException extends MusicServiceException {
        public RecommendationException(String reason) {
            super(String.format("Music recommendation failed: %s", reason), "RECOMMENDATION_FAILED");
        }
        
        public RecommendationException(String reason, Throwable cause) {
            super(String.format("Music recommendation failed: %s", reason), cause, "RECOMMENDATION_FAILED");
        }
    }

    /**
     * Exception thrown when external service calls fail.
     */
    public static class ExternalServiceException extends MusicServiceException {
        public ExternalServiceException(String serviceName, String operation, Throwable cause) {
            super(String.format("External service '%s' call failed for operation '%s'", serviceName, operation), 
                  cause, "EXTERNAL_SERVICE_ERROR");
        }
        
        public ExternalServiceException(String serviceName, String message) {
            super(String.format("External service '%s' error: %s", serviceName, message), "EXTERNAL_SERVICE_ERROR");
        }
    }

    /**
     * Exception thrown when validation errors occur.
     */
    public static class ValidationException extends MusicServiceException {
        public ValidationException(String field, String reason) {
            super(String.format("Validation failed for field '%s': %s", field, reason), "VALIDATION_ERROR");
        }
        
        public ValidationException(String message) {
            super(message, "VALIDATION_ERROR");
        }
    }

    /**
     * Exception thrown when business rule violations occur.
     */
    public static class BusinessRuleViolationException extends MusicServiceException {
        public BusinessRuleViolationException(String rule, String context) {
            super(String.format("Business rule violation: %s in context: %s", rule, context), "BUSINESS_RULE_VIOLATION");
        }
        
        public BusinessRuleViolationException(String message) {
            super(message, "BUSINESS_RULE_VIOLATION");
        }
    }

    /**
     * Exception thrown when rate limits are exceeded.
     */
    public static class RateLimitException extends MusicServiceException {
        public RateLimitException(String operation, long retryAfter) {
            super(String.format("Rate limit exceeded for operation '%s'. Retry after %d seconds", operation, retryAfter), 
                  "RATE_LIMIT_EXCEEDED");
        }
        
        public RateLimitException(String message) {
            super(message, "RATE_LIMIT_EXCEEDED");
        }
    }

    /**
     * Exception thrown when concurrent modification conflicts occur.
     */
    public static class ConcurrentModificationException extends MusicServiceException {
        public ConcurrentModificationException(String resource, String resourceId) {
            super(String.format("Concurrent modification detected for %s with ID: %s", resource, resourceId), 
                  "CONCURRENT_MODIFICATION");
        }
        
        public ConcurrentModificationException(String message) {
            super(message, "CONCURRENT_MODIFICATION");
        }
    }

    /**
     * Exception thrown when security-related operations fail.
     */
    public static class SecurityException extends MusicServiceException {
        public SecurityException(String message, Throwable cause) {
            super(message, cause, "SECURITY_ERROR");
        }
        
        public SecurityException(String message) {
            super(message, "SECURITY_ERROR");
        }
    }
}