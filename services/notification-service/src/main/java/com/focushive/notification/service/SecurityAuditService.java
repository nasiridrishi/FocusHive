package com.focushive.notification.service;

import com.focushive.notification.entity.SecurityAuditLog;
import com.focushive.notification.repository.SecurityAuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service for comprehensive security audit logging.
 * Provides structured logging for sensitive operations and security events.
 */
@Service
@Slf4j
public class SecurityAuditService {

    private final UserContextService userContextService;
    private final Logger auditLogger;
    private final SecurityAuditLogRepository auditLogRepository;
    
    @Autowired
    public SecurityAuditService(UserContextService userContextService, SecurityAuditLogRepository auditLogRepository) {
        this.userContextService = userContextService;
        this.auditLogRepository = auditLogRepository;
        // Dedicated audit logger for security events
        this.auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");
    }
    
    // Constructor for testing with injectable logger
    public SecurityAuditService(UserContextService userContextService, Logger auditLogger, SecurityAuditLogRepository auditLogRepository) {
        this.userContextService = userContextService;
        this.auditLogger = auditLogger;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Logs successful authentication events.
     *
     * @param ipAddress the IP address of the client
     */
    public void logAuthenticationSuccess(String ipAddress) {
        UserContextService.UserContext userContext = userContextService.getCurrentUserContext();
        String userId = userContext != null ? userContext.userId() : "ANONYMOUS";
        
        auditLogger.info("[AUDIT] AUTHENTICATION_SUCCESS - user={} ip={} timestamp={}",
                userId, ipAddress, Instant.now());
    }

    /**
     * Logs successful authentication events with correlation ID.
     *
     * @param ipAddress the IP address of the client
     * @param correlationId correlation ID for request tracking
     */
    public void logAuthenticationSuccessWithCorrelation(String ipAddress, String correlationId) {
        UserContextService.UserContext userContext = userContextService.getCurrentUserContext();
        String userId = userContext != null ? userContext.userId() : "ANONYMOUS";
        
        auditLogger.info("[AUDIT] AUTHENTICATION_SUCCESS - user={} ip={} correlationId={} timestamp={}",
                userId, ipAddress, correlationId, Instant.now());
    }

    /**
     * Logs failed authentication attempts.
     *
     * @param token the attempted token (masked for security)
     * @param ipAddress the IP address of the client
     * @param reason the failure reason
     */
    public void logAuthenticationFailure(String token, String ipAddress, String reason) {
        String maskedToken = maskToken(token);
        
        auditLogger.warn("[AUDIT] AUTHENTICATION_FAILURE - token={} ip={} reason={} timestamp={}",
                maskedToken, ipAddress, reason, Instant.now());
    }

    /**
     * Logs authorization failures.
     *
     * @param resource the resource that was attempted to be accessed
     * @param requiredRole the role that was required
     */
    public void logAuthorizationFailure(String resource, String requiredRole) {
        UserContextService.UserContext userContext = userContextService.getCurrentUserContext();
        String userId = userContext != null ? userContext.userId() : "ANONYMOUS";
        
        auditLogger.warn("[AUDIT] AUTHORIZATION_FAILURE - user={} resource={} requiredRole={} timestamp={}",
                userId, resource, requiredRole, Instant.now());
    }

    /**
     * Logs rate limit exceeded events.
     *
     * @param endpoint the endpoint that was rate limited
     * @param operationType the type of operation (READ, WRITE, etc.)
     * @param currentCount the current request count
     */
    public void logRateLimitExceeded(String endpoint, String operationType, int currentCount) {
        UserContextService.UserContext userContext = userContextService.getCurrentUserContext();
        String userId = userContext != null ? userContext.userId() : "ANONYMOUS";
        
        auditLogger.warn("[AUDIT] RATE_LIMIT_EXCEEDED - user={} endpoint={} operation={} count={} timestamp={}",
                userId, endpoint, operationType, currentCount, Instant.now());
    }

    /**
     * Logs preference changes.
     *
     * @param preferenceId the ID of the preference that was changed
     * @param notificationType the type of notification preference
     * @param changes map of field changes (old -> new values)
     */
    public void logPreferenceChange(String preferenceId, String notificationType, Map<String, Object> changes) {
        UserContextService.UserContext userContext = userContextService.getCurrentUserContext();
        String userId = userContext != null ? userContext.userId() : "ANONYMOUS";
        
        auditLogger.info("[AUDIT] PREFERENCE_CHANGE - user={} preferenceId={} notificationType={} changes={} timestamp={}",
                userId, preferenceId, notificationType, formatChanges(changes), Instant.now());
    }

    /**
     * Logs notification sending events.
     *
     * @param notificationId the ID of the notification sent
     * @param targetUserId the ID of the user receiving the notification
     * @param notificationType the type of notification
     * @param channel the delivery channel (email, sms, etc.)
     */
    public void logNotificationSent(String notificationId, String targetUserId, String notificationType, String channel) {
        UserContextService.UserContext userContext = userContextService.getCurrentUserContext();
        String triggeredByUserId = userContext != null ? userContext.userId() : "SYSTEM";
        
        auditLogger.info("[AUDIT] NOTIFICATION_SENT - triggeredBy={} notificationId={} targetUser={} type={} channel={} timestamp={}",
                triggeredByUserId, notificationId, targetUserId, notificationType, channel, Instant.now());
    }

    /**
     * Logs admin actions.
     *
     * @param action the action performed
     * @param resource the resource affected
     * @param parameters additional parameters for the action
     */
    public void logAdminAction(String action, String resource, Map<String, Object> parameters) {
        UserContextService.UserContext userContext = userContextService.getCurrentUserContext();
        String userId = userContext != null ? userContext.userId() : "UNKNOWN";
        
        auditLogger.info("[AUDIT] ADMIN_ACTION - admin={} action={} resource={} parameters={} timestamp={}",
                userId, action, resource, formatChanges(parameters), Instant.now());
    }

    /**
     * Logs template creation events.
     *
     * @param templateId the ID of the created template
     * @param notificationType the type of notification template
     * @param language the language of the template
     */
    public void logTemplateCreation(String templateId, String notificationType, String language) {
        UserContextService.UserContext userContext = userContextService.getCurrentUserContext();
        String userId = userContext != null ? userContext.userId() : "SYSTEM";
        
        auditLogger.info("[AUDIT] TEMPLATE_CREATION - user={} templateId={} type={} language={} timestamp={}",
                userId, templateId, notificationType, language, Instant.now());
    }

    /**
     * Logs template deletion events.
     *
     * @param templateId the ID of the deleted template
     * @param notificationType the type of notification template
     * @param language the language of the template
     */
    public void logTemplateDeletion(String templateId, String notificationType, String language) {
        UserContextService.UserContext userContext = userContextService.getCurrentUserContext();
        String userId = userContext != null ? userContext.userId() : "SYSTEM";
        
        auditLogger.warn("[AUDIT] TEMPLATE_DELETION - user={} templateId={} type={} language={} timestamp={}",
                userId, templateId, notificationType, language, Instant.now());
    }

    /**
     * Logs suspicious activity events.
     *
     * @param activityType the type of suspicious activity
     * @param ipAddress the IP address involved
     * @param metadata additional metadata about the activity
     */
    public void logSuspiciousActivity(String activityType, String ipAddress, Map<String, Object> metadata) {
        UserContextService.UserContext userContext = userContextService.getCurrentUserContext();
        String userId = userContext != null ? userContext.userId() : "UNKNOWN";
        
        auditLogger.error("[AUDIT] SUSPICIOUS_ACTIVITY - user={} activityType={} ip={} metadata={} timestamp={}",
                userId, activityType, ipAddress, formatChanges(metadata), Instant.now());
    }

    /**
     * Logs security configuration changes.
     *
     * @param configType the type of configuration changed
     * @param changes map of configuration changes
     */
    public void logSecurityConfigurationChange(String configType, Map<String, Object> changes) {
        UserContextService.UserContext userContext = userContextService.getCurrentUserContext();
        String userId = userContext != null ? userContext.userId() : "SYSTEM";
        
        auditLogger.warn("[AUDIT] SECURITY_CONFIGURATION_CHANGE - user={} configType={} changes={} timestamp={}",
                userId, configType, formatChanges(changes), Instant.now());
    }

    /**
     * Masks sensitive token information for logging.
     *
     * @param token the token to mask
     * @return masked token string
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }

    /**
     * Formats changes map for consistent logging output.
     *
     * @param changes the changes to format
     * @return formatted string representation
     */
    private String formatChanges(Map<String, Object> changes) {
        if (changes == null || changes.isEmpty()) {
            return "{}";
        }
        
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : changes.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        sb.append("}");
        
        return sb.toString();
    }

    /**
     * General method to log security events to both logger and database.
     *
     * @param action the action performed
     * @param username the username (can be null)
     * @param details detailed description of the event
     * @param success whether the action was successful
     * @param ipAddress the IP address (can be null)
     */
    public void logSecurityEvent(String action, String username, String details, boolean success, String ipAddress) {
        // Log to application logger
        if (success) {
            auditLogger.info("[AUDIT] {} - user={} details={} ip={} timestamp={}",
                    action, username, details, ipAddress, Instant.now());
        } else {
            auditLogger.warn("[AUDIT] {} - user={} details={} ip={} timestamp={}",
                    action, username, details, ipAddress, Instant.now());
        }

        // Log to database if repository is available
        if (auditLogRepository != null) {
            try {
                SecurityAuditLog auditLog = new SecurityAuditLog();
                auditLog.setAction(action);
                auditLog.setUsername(username != null ? username : "UNKNOWN");
                auditLog.setDetails(details);
                auditLog.setSuccess(success);
                auditLog.setIpAddress(ipAddress);
                auditLog.setTimestamp(LocalDateTime.now());
                
                auditLogRepository.save(auditLog);
            } catch (Exception e) {
                // Don't let audit logging failures break the main flow
                log.error("Failed to save audit log to database", e);
            }
        }
    }

    /**
     * Logs logout events for audit tracking.
     *
     * @param username The user logging out
     * @param reason The reason for logout (e.g., "user_logout", "session_expired")
     * @param tokenId The token ID being invalidated
     */
    public void logLogoutEvent(String username, String reason, String tokenId) {
        String message = String.format(
                "LOGOUT - User: %s, Reason: %s, TokenId: %s",
                username, reason, tokenId
        );
        auditLogger.info(message);

        // Log to database
        logSecurityEvent("LOGOUT", username, "Reason: " + reason + ", Token: " + tokenId,
                        true, "unknown");
    }

    /**
     * Logs security events with basic information.
     *
     * @param eventType The type of security event
     * @param username The user involved
     * @param details Additional details about the event
     */
    public void logSecurityEvent(String eventType, String username, String details) {
        String message = String.format(
                "SECURITY_EVENT - Type: %s, User: %s, Details: %s",
                eventType, username, details
        );
        auditLogger.info(message);

        // Log to database
        logSecurityEvent(eventType, username, details, true, "unknown");
    }
}
