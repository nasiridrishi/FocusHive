package com.focushive.identity.service;

import com.focushive.identity.audit.OAuth2AuditEvent;
import com.focushive.identity.audit.OAuth2AuditEvent.EventType;
import com.focushive.identity.audit.OAuth2AuditEvent.RiskLevel;
import com.focushive.identity.dto.*;
import com.focushive.identity.entity.User;
import com.focushive.identity.repository.OAuth2AuditEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service for OAuth2 security audit logging.
 * Provides asynchronous, comprehensive audit logging for all OAuth2 operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2AuditService {

    private final OAuth2AuditEventRepository auditRepository;

    /**
     * Log authorization request.
     */
    @Async
    @Transactional
    public void logAuthorizationRequest(OAuth2AuthorizeRequest request, String ipAddress) {
        try {
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(EventType.AUTHORIZATION_REQUEST)
                .riskLevel(RiskLevel.LOW)
                .clientId(request.getClientId())
                .scope(request.getScope())
                .redirectUri(request.getRedirectUri())
                .responseType(request.getResponseType())
                .ipAddress(ipAddress)
                .success(true)
                .timestamp(Instant.now())
                .build();
            
            auditRepository.save(event);
            log.debug("Logged authorization request for client: {}", request.getClientId());
        } catch (Exception e) {
            log.error("Failed to log authorization request: {}", e.getMessage());
        }
    }

    /**
     * Log successful authorization.
     */
    @Async
    @Transactional
    public void logAuthorizationSuccess(String clientId, UUID userId, String authCode, 
                                       String scope, long processingTimeMs) {
        try {
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(EventType.AUTHORIZATION_CODE_ISSUED)
                .riskLevel(RiskLevel.LOW)
                .clientId(clientId)
                .userId(userId)
                .authorizationCode(authCode)
                .scope(scope)
                .success(true)
                .processingTimeMs(processingTimeMs)
                .timestamp(Instant.now())
                .build();
            
            auditRepository.save(event);
            log.debug("Logged successful authorization for user: {} client: {}", userId, clientId);
        } catch (Exception e) {
            log.error("Failed to log authorization success: {}", e.getMessage());
        }
    }

    /**
     * Log authorization failure.
     */
    @Async
    @Transactional
    public void logAuthorizationFailure(String clientId, String errorCode, 
                                       String errorDescription, String ipAddress) {
        try {
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(EventType.AUTHORIZATION_FAILURE)
                .riskLevel(RiskLevel.HIGH)
                .clientId(clientId)
                .errorCode(errorCode)
                .errorDescription(errorDescription)
                .ipAddress(ipAddress)
                .success(false)
                .suspiciousActivity(true)
                .timestamp(Instant.now())
                .build();
            
            auditRepository.save(event);
            log.warn("Logged authorization failure for client: {} - {}", clientId, errorCode);
        } catch (Exception e) {
            log.error("Failed to log authorization failure: {}", e.getMessage());
        }
    }

    /**
     * Log token issuance.
     */
    @Async
    @Transactional
    public void logTokenIssuance(String clientId, UUID userId, String grantType, 
                                String scope, String tokenId, long processingTimeMs) {
        try {
            EventType eventType = "refresh_token".equals(grantType) ? 
                EventType.TOKEN_REFRESH : EventType.ACCESS_TOKEN_ISSUED;
            
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(eventType)
                .riskLevel(RiskLevel.LOW)
                .clientId(clientId)
                .userId(userId)
                .grantType(grantType)
                .scope(scope)
                .tokenId(tokenId)
                .success(true)
                .processingTimeMs(processingTimeMs)
                .timestamp(Instant.now())
                .build();
            
            auditRepository.save(event);
            log.debug("Logged token issuance for client: {} grant: {}", clientId, grantType);
        } catch (Exception e) {
            log.error("Failed to log token issuance: {}", e.getMessage());
        }
    }

    /**
     * Log token revocation.
     */
    @Async
    @Transactional
    public void logTokenRevocation(String clientId, String tokenId, String reason) {
        try {
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(EventType.TOKEN_REVOCATION)
                .riskLevel(RiskLevel.MEDIUM)
                .clientId(clientId)
                .tokenId(tokenId)
                .success(true)
                .timestamp(Instant.now())
                .build();
            
            if (reason != null) {
                Map<String, Object> additionalData = new HashMap<>();
                additionalData.put("revocationReason", reason);
                event.setAdditionalData(additionalData);
            }
            
            auditRepository.save(event);
            log.debug("Logged token revocation for client: {} token: {}", clientId, tokenId);
        } catch (Exception e) {
            log.error("Failed to log token revocation: {}", e.getMessage());
        }
    }

    /**
     * Log token introspection.
     */
    @Async
    @Transactional
    public void logTokenIntrospection(String clientId, String tokenId, boolean active) {
        try {
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(EventType.TOKEN_INTROSPECTION)
                .riskLevel(RiskLevel.LOW)
                .clientId(clientId)
                .tokenId(tokenId)
                .success(true)
                .timestamp(Instant.now())
                .build();
            
            Map<String, Object> additionalData = new HashMap<>();
            additionalData.put("tokenActive", active);
            event.setAdditionalData(additionalData);
            
            auditRepository.save(event);
            log.debug("Logged token introspection for client: {} active: {}", clientId, active);
        } catch (Exception e) {
            log.error("Failed to log token introspection: {}", e.getMessage());
        }
    }

    /**
     * Log UserInfo access.
     */
    @Async
    @Transactional
    public void logUserInfoAccess(UUID userId, String clientId, String scope) {
        try {
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(EventType.USER_INFO_ACCESS)
                .riskLevel(RiskLevel.LOW)
                .userId(userId)
                .clientId(clientId)
                .scope(scope)
                .success(true)
                .timestamp(Instant.now())
                .build();
            
            auditRepository.save(event);
            log.debug("Logged UserInfo access for user: {} client: {}", userId, clientId);
        } catch (Exception e) {
            log.error("Failed to log UserInfo access: {}", e.getMessage());
        }
    }

    /**
     * Log security alert.
     */
    @Async
    @Transactional
    public void logSecurityAlert(EventType eventType, String threat, String ipAddress, 
                                String clientId, Map<String, Object> details) {
        try {
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(eventType)
                .riskLevel(RiskLevel.CRITICAL)
                .clientId(clientId)
                .ipAddress(ipAddress)
                .threatIndicators(threat)
                .suspiciousActivity(true)
                .success(false)
                .additionalData(details != null ? details : new HashMap<>())
                .timestamp(Instant.now())
                .build();
            
            auditRepository.save(event);
            log.error("SECURITY ALERT: {} - {} from IP: {}", eventType, threat, ipAddress);
        } catch (Exception e) {
            log.error("Failed to log security alert: {}", e.getMessage());
        }
    }

    /**
     * Log rate limit exceeded.
     */
    @Async
    @Transactional
    public void logRateLimitExceeded(String clientId, String ipAddress, String endpoint) {
        try {
            Map<String, Object> additionalData = new HashMap<>();
            additionalData.put("endpoint", endpoint);
            
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(EventType.RATE_LIMIT_EXCEEDED)
                .riskLevel(RiskLevel.MEDIUM)
                .clientId(clientId)
                .ipAddress(ipAddress)
                .suspiciousActivity(true)
                .success(false)
                .additionalData(additionalData)
                .timestamp(Instant.now())
                .build();
            
            auditRepository.save(event);
            log.warn("Rate limit exceeded for client: {} from IP: {}", clientId, ipAddress);
        } catch (Exception e) {
            log.error("Failed to log rate limit violation: {}", e.getMessage());
        }
    }

    /**
     * Log PKCE validation failure.
     */
    @Async
    @Transactional
    public void logPKCEValidationFailure(String clientId, String ipAddress) {
        try {
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(EventType.PKCE_VALIDATION_FAILURE)
                .riskLevel(RiskLevel.HIGH)
                .clientId(clientId)
                .ipAddress(ipAddress)
                .suspiciousActivity(true)
                .success(false)
                .timestamp(Instant.now())
                .build();
            
            auditRepository.save(event);
            log.error("PKCE validation failed for client: {} from IP: {}", clientId, ipAddress);
        } catch (Exception e) {
            log.error("Failed to log PKCE validation failure: {}", e.getMessage());
        }
    }

    /**
     * Check for suspicious activity patterns.
     */
    @Transactional(readOnly = true)
    public boolean checkSuspiciousActivity(String clientId, String ipAddress) {
        try {
            // Check failed attempts in last 15 minutes
            Instant since = Instant.now().minus(15, ChronoUnit.MINUTES);
            
            // Check failed attempts from this client
            var failedAttempts = auditRepository.findFailedAttemptsForClient(clientId, since);
            if (failedAttempts.size() > 5) {
                logSecurityAlert(
                    EventType.SUSPICIOUS_ACTIVITY,
                    "Multiple failed attempts detected",
                    ipAddress,
                    clientId,
                    Map.of("failedAttempts", failedAttempts.size())
                );
                return true;
            }
            
            // Check rate limit violations
            var rateLimitViolations = auditRepository.findRateLimitViolationsSince(since);
            if (rateLimitViolations.size() > 3) {
                logSecurityAlert(
                    EventType.SUSPICIOUS_ACTIVITY,
                    "Multiple rate limit violations",
                    ipAddress,
                    clientId,
                    Map.of("violations", rateLimitViolations.size())
                );
                return true;
            }
            
            // Check events from this IP
            var ipEvents = auditRepository.findByIpAddressAndTimestampAfterOrderByTimestampDesc(
                ipAddress, since);
            long suspiciousCount = ipEvents.stream()
                .filter(OAuth2AuditEvent::isSuspiciousActivity)
                .count();
            
            if (suspiciousCount > 2) {
                logSecurityAlert(
                    EventType.SUSPICIOUS_ACTIVITY,
                    "Multiple suspicious activities from IP",
                    ipAddress,
                    clientId,
                    Map.of("suspiciousCount", suspiciousCount)
                );
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.error("Failed to check suspicious activity: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get client IP address from request.
     */
    public String getClientIpAddress() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) 
                RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            
            // Check for proxied requests
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }
            
            return request.getRemoteAddr();
        } catch (Exception e) {
            log.error("Failed to get client IP address: {}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * Clean up old audit events (retention policy).
     */
    @Async
    @Transactional
    public void cleanupOldAuditEvents(int retentionDays) {
        try {
            Instant cutoffTime = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
            auditRepository.deleteOldAuditEvents(cutoffTime);
            log.info("Cleaned up audit events older than {} days", retentionDays);
        } catch (Exception e) {
            log.error("Failed to cleanup old audit events: {}", e.getMessage());
        }
    }

    /**
     * Log a token rotation event for audit trail.
     */
    @Async
    @Transactional
    public void logTokenRotation(String clientId, UUID userId, UUID oldTokenId,
                                 UUID newTokenId, String ipAddress) {
        try {
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(EventType.TOKEN_ROTATION)
                .riskLevel(RiskLevel.LOW)
                .clientId(clientId)
                .userId(userId)
                .ipAddress(ipAddress)
                .success(true)
                .additionalData(Map.of(
                    "oldTokenId", oldTokenId.toString(),
                    "newTokenId", newTokenId.toString(),
                    "rotationTime", Instant.now().toString()
                ))
                .timestamp(Instant.now())
                .build();

            auditRepository.save(event);
            log.debug("Token rotation logged for client: {} user: {}", clientId, userId);
        } catch (Exception e) {
            log.error("Failed to log token rotation: {}", e.getMessage());
        }
    }

    public void logSecurityAlert(EventType eventType, String threat, UUID userId,
                                String clientId, Map<String, Object> details) {
        logSecurityAlert(eventType, threat, getClientIpAddress(), clientId, details);
    }

    /**
     * Log consent granted event.
     */
    @Async
    @Transactional
    public void logConsentGranted(UUID userId, String clientId, Set<String> grantedScopes, String ipAddress) {
        try {
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(EventType.USER_CONSENT_GRANTED)
                .riskLevel(RiskLevel.LOW)
                .userId(userId)
                .clientId(clientId)
                .scope(String.join(" ", grantedScopes))
                .ipAddress(ipAddress)
                .success(true)
                .timestamp(Instant.now())
                .build();

            auditRepository.save(event);
            log.debug("Consent granted logged for user: {} client: {}", userId, clientId);
        } catch (Exception e) {
            log.error("Failed to log consent granted: {}", e.getMessage());
        }
    }

    /**
     * Log consent denied event.
     */
    @Async
    @Transactional
    public void logConsentDenied(UUID userId, String clientId, Set<String> requestedScopes, String ipAddress) {
        try {
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(EventType.USER_CONSENT_REVOKED)
                .riskLevel(RiskLevel.MEDIUM)
                .userId(userId)
                .clientId(clientId)
                .scope(String.join(" ", requestedScopes))
                .ipAddress(ipAddress)
                .success(false)
                .timestamp(Instant.now())
                .build();

            auditRepository.save(event);
            log.debug("Consent denied logged for user: {} client: {}", userId, clientId);
        } catch (Exception e) {
            log.error("Failed to log consent denied: {}", e.getMessage());
        }
    }

    /**
     * Log consent revoked event.
     */
    @Async
    @Transactional
    public void logConsentRevoked(UUID userId, String clientId, String reason, String ipAddress) {
        try {
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(EventType.USER_CONSENT_REVOKED)
                .riskLevel(RiskLevel.MEDIUM)
                .userId(userId)
                .clientId(clientId)
                .ipAddress(ipAddress)
                .success(true)
                .additionalData(Map.of("revocationReason", reason))
                .timestamp(Instant.now())
                .build();

            auditRepository.save(event);
            log.debug("Consent revoked logged for user: {} client: {}", userId, clientId);
        } catch (Exception e) {
            log.error("Failed to log consent revoked: {}", e.getMessage());
        }
    }

    /**
     * Log all consents revoked for a user.
     */
    @Async
    @Transactional
    public void logAllConsentsRevoked(UUID userId, String reason, String ipAddress) {
        try {
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(EventType.USER_CONSENT_REVOKED)
                .riskLevel(RiskLevel.HIGH)
                .userId(userId)
                .ipAddress(ipAddress)
                .success(true)
                .additionalData(Map.of(
                    "revocationReason", reason,
                    "scope", "all_consents"
                ))
                .timestamp(Instant.now())
                .build();

            auditRepository.save(event);
            log.debug("All consents revoked logged for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to log all consents revoked: {}", e.getMessage());
        }
    }

    /**
     * Log session created event.
     */
    @Async
    @Transactional
    public void logSessionCreated(UUID userId, String clientId, String sessionId, String ipAddress) {
        try {
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(EventType.SESSION_CREATED)
                .riskLevel(RiskLevel.LOW)
                .userId(userId)
                .clientId(clientId)
                .ipAddress(ipAddress)
                .success(true)
                .additionalData(Map.of("sessionId", sessionId))
                .timestamp(Instant.now())
                .build();

            auditRepository.save(event);
            log.debug("Session created logged for user: {} client: {}", userId, clientId);
        } catch (Exception e) {
            log.error("Failed to log session created: {}", e.getMessage());
        }
    }

    /**
     * Log session refreshed event.
     */
    @Async
    @Transactional
    public void logSessionRefreshed(UUID userId, String clientId, String sessionId, String ipAddress) {
        try {
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(EventType.SESSION_REFRESHED)
                .riskLevel(RiskLevel.LOW)
                .userId(userId)
                .clientId(clientId)
                .ipAddress(ipAddress)
                .success(true)
                .additionalData(Map.of("sessionId", sessionId))
                .timestamp(Instant.now())
                .build();

            auditRepository.save(event);
            log.debug("Session refreshed logged for user: {} client: {}", userId, clientId);
        } catch (Exception e) {
            log.error("Failed to log session refreshed: {}", e.getMessage());
        }
    }

    /**
     * Log session terminated event.
     */
    @Async
    @Transactional
    public void logSessionTerminated(UUID userId, String clientId, String sessionId, String reason, String ipAddress) {
        try {
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(EventType.SESSION_TERMINATED)
                .riskLevel(RiskLevel.MEDIUM)
                .userId(userId)
                .clientId(clientId)
                .ipAddress(ipAddress)
                .success(true)
                .additionalData(Map.of(
                    "sessionId", sessionId,
                    "terminationReason", reason
                ))
                .timestamp(Instant.now())
                .build();

            auditRepository.save(event);
            log.debug("Session terminated logged for user: {} client: {}", userId, clientId);
        } catch (Exception e) {
            log.error("Failed to log session terminated: {}", e.getMessage());
        }
    }

    /**
     * Log session elevated event.
     */
    @Async
    @Transactional
    public void logSessionElevated(UUID userId, String clientId, String sessionId, String authLevel, String ipAddress) {
        try {
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(EventType.SESSION_ELEVATED)
                .riskLevel(RiskLevel.MEDIUM)
                .userId(userId)
                .clientId(clientId)
                .ipAddress(ipAddress)
                .success(true)
                .additionalData(Map.of(
                    "sessionId", sessionId,
                    "authLevel", authLevel
                ))
                .timestamp(Instant.now())
                .build();

            auditRepository.save(event);
            log.debug("Session elevated logged for user: {} client: {}", userId, clientId);
        } catch (Exception e) {
            log.error("Failed to log session elevated: {}", e.getMessage());
        }
    }

    /**
     * Log all sessions terminated event.
     */
    @Async
    @Transactional
    public void logAllSessionsTerminated(UUID userId, String reason, String ipAddress) {
        try {
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(EventType.SESSION_TERMINATED)
                .riskLevel(RiskLevel.HIGH)
                .userId(userId)
                .ipAddress(ipAddress)
                .success(true)
                .additionalData(Map.of(
                    "terminationReason", reason,
                    "scope", "all_sessions"
                ))
                .timestamp(Instant.now())
                .build();

            auditRepository.save(event);
            log.debug("All sessions terminated logged for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to log all sessions terminated: {}", e.getMessage());
        }
    }

    /**
     * Log client registration event.
     */
    @Async
    @Transactional
    public void logClientRegistration(String clientId, String ipAddress) {
        try {
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(EventType.CLIENT_REGISTRATION)
                .riskLevel(RiskLevel.LOW)
                .clientId(clientId)
                .ipAddress(ipAddress)
                .success(true)
                .timestamp(Instant.now())
                .build();

            auditRepository.save(event);
            log.debug("Client registration logged for client: {}", clientId);
        } catch (Exception e) {
            log.error("Failed to log client registration: {}", e.getMessage());
        }
    }

    /**
     * Log client update event.
     */
    @Async
    @Transactional
    public void logClientUpdate(String clientId, String ipAddress) {
        try {
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(EventType.CLIENT_UPDATE)
                .riskLevel(RiskLevel.LOW)
                .clientId(clientId)
                .ipAddress(ipAddress)
                .success(true)
                .timestamp(Instant.now())
                .build();

            auditRepository.save(event);
            log.debug("Client update logged for client: {}", clientId);
        } catch (Exception e) {
            log.error("Failed to log client update: {}", e.getMessage());
        }
    }

    /**
     * Log client deletion event.
     */
    @Async
    @Transactional
    public void logClientDeletion(String clientId, String ipAddress) {
        try {
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(EventType.CLIENT_DELETION)
                .riskLevel(RiskLevel.HIGH)
                .clientId(clientId)
                .ipAddress(ipAddress)
                .success(true)
                .timestamp(Instant.now())
                .build();

            auditRepository.save(event);
            log.debug("Client deletion logged for client: {}", clientId);
        } catch (Exception e) {
            log.error("Failed to log client deletion: {}", e.getMessage());
        }
    }

    /**
     * Log client secret rotation event.
     */
    @Async
    @Transactional
    public void logClientSecretRotation(String clientId, String ipAddress) {
        try {
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(EventType.CLIENT_SECRET_ROTATION)
                .riskLevel(RiskLevel.MEDIUM)
                .clientId(clientId)
                .ipAddress(ipAddress)
                .success(true)
                .timestamp(Instant.now())
                .build();

            auditRepository.save(event);
            log.debug("Client secret rotation logged for client: {}", clientId);
        } catch (Exception e) {
            log.error("Failed to log client secret rotation: {}", e.getMessage());
        }
    }

    /**
     * Log client status change event.
     */
    @Async
    @Transactional
    public void logClientStatusChange(String clientId, String action, String ipAddress) {
        try {
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(EventType.CLIENT_STATUS_CHANGE)
                .riskLevel(RiskLevel.MEDIUM)
                .clientId(clientId)
                .ipAddress(ipAddress)
                .success(true)
                .additionalData(Map.of("action", action))
                .timestamp(Instant.now())
                .build();

            auditRepository.save(event);
            log.debug("Client status change logged for client: {} action: {}", clientId, action);
        } catch (Exception e) {
            log.error("Failed to log client status change: {}", e.getMessage());
        }
    }

    /**
     * Log client tokens revoked event.
     */
    @Async
    @Transactional
    public void logClientTokensRevoked(String clientId, int tokenCount, String reason, String ipAddress) {
        try {
            OAuth2AuditEvent event = OAuth2AuditEvent.builder()
                .eventType(EventType.TOKEN_REVOCATION)
                .riskLevel(RiskLevel.HIGH)
                .clientId(clientId)
                .ipAddress(ipAddress)
                .success(true)
                .additionalData(Map.of(
                    "tokenCount", tokenCount,
                    "reason", reason
                ))
                .timestamp(Instant.now())
                .build();

            auditRepository.save(event);
            log.debug("Client tokens revoked logged for client: {} count: {}", clientId, tokenCount);
        } catch (Exception e) {
            log.error("Failed to log client tokens revoked: {}", e.getMessage());
        }
    }
}