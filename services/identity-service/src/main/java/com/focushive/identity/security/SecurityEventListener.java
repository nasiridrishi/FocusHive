package com.focushive.identity.security;

import com.focushive.identity.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;

/**
 * Security Event Listener for OWASP A09: Security Logging and Monitoring.
 * Logs all authentication attempts with timestamp, username, IP, and result.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityEventListener {

    private final AuditService auditService;

    /**
     * A09: Log successful authentication events.
     */
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        String username = auth.getName();
        String ipAddress = getClientIpAddress();

        // Structured logging for security events
        log.info("SECURITY_EVENT: event=AUTHENTICATION_SUCCESS, " +
                "username={}, ipAddress={}, timestamp={}, " +
                "userAgent={}, sessionId={}",
                username, ipAddress, Instant.now(),
                getUserAgent(), getSessionId());

        // Also log to audit service
        auditService.logSecurityEvent(
            null, // userId will be populated by audit service
            "AUTHENTICATION_SUCCESS",
            String.format("User '%s' successfully authenticated", username),
            ipAddress
        );
    }

    /**
     * A09: Log failed authentication events.
     */
    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        Authentication auth = event.getAuthentication();
        String username = auth != null ? auth.getName() : "unknown";
        String ipAddress = getClientIpAddress();
        Exception exception = event.getException();

        // Structured logging for security events
        log.warn("SECURITY_EVENT: event=AUTHENTICATION_FAILURE, " +
                "username={}, ipAddress={}, timestamp={}, " +
                "reason={}, userAgent={}, sessionId={}",
                username, ipAddress, Instant.now(),
                exception.getMessage(), getUserAgent(), getSessionId());

        // Also log to audit service
        auditService.logSecurityEvent(
            "AUTHENTICATION_FAILURE",
            String.format("Failed authentication attempt for user '%s': %s",
                username, exception.getMessage()),
            ipAddress
        );
    }

    /**
     * A09: Log all other authentication events.
     */
    @EventListener
    public void onAbstractAuthenticationEvent(AbstractAuthenticationEvent event) {
        // Skip events we've already handled specifically
        if (event instanceof AuthenticationSuccessEvent ||
            event instanceof AuthenticationFailureBadCredentialsEvent) {
            return;
        }

        Authentication auth = event.getAuthentication();
        String username = auth != null ? auth.getName() : "unknown";
        String ipAddress = getClientIpAddress();
        String eventType = event.getClass().getSimpleName();

        // Structured logging for security events
        log.info("SECURITY_EVENT: event={}, " +
                "username={}, ipAddress={}, timestamp={}, " +
                "userAgent={}, sessionId={}",
                eventType, username, ipAddress, Instant.now(),
                getUserAgent(), getSessionId());

        // Also log to audit service
        auditService.logSecurityEvent(
            eventType,
            String.format("Authentication event '%s' for user '%s'", eventType, username),
            ipAddress
        );
    }

    /**
     * Extract client IP address from request, considering proxy headers.
     */
    private String getClientIpAddress() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return "unknown";
        }

        HttpServletRequest request = attributes.getRequest();

        // Check for IP address from proxy headers (in order of preference)
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "X-Original-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        // Fallback to remote address
        return request.getRemoteAddr();
    }

    /**
     * Extract User-Agent header from request.
     */
    private String getUserAgent() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return "unknown";
        }

        HttpServletRequest request = attributes.getRequest();
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "unknown";
    }

    /**
     * Extract session ID from request.
     */
    private String getSessionId() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return "unknown";
        }

        HttpServletRequest request = attributes.getRequest();
        String sessionId = request.getSession(false) != null ?
            request.getSession(false).getId() : "no-session";
        return sessionId;
    }
}