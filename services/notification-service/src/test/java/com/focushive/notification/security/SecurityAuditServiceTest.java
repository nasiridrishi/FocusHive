package com.focushive.notification.security;

import com.focushive.notification.service.SecurityAuditService;
import com.focushive.notification.service.UserContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Test class for SecurityAuditService following TDD approach.
 * Tests comprehensive audit logging for sensitive operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityAuditService Tests")
class SecurityAuditServiceTest {

    @Mock
    private UserContextService userContextService;

    @Mock
    private Logger auditLogger;

    private SecurityAuditService securityAuditService;

    private UserContextService.UserContext testUserContext;

    @BeforeEach
    void setUp() {
        testUserContext = new UserContextService.UserContext(
                "user123",
                "user@example.com", 
                "Test User",
                java.util.List.of("ROLE_USER")
        );
        securityAuditService = new SecurityAuditService(userContextService, auditLogger, null);
    }

    @Test
    @DisplayName("Should log authentication success event")
    void shouldLogAuthenticationSuccessEvent() {
        // Given
        given(userContextService.getCurrentUserContext()).willReturn(testUserContext);

        // When
        securityAuditService.logAuthenticationSuccess("127.0.0.1");

        // Then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(auditLogger).info(messageCaptor.capture(), argsCaptor.capture());
        
        String logMessage = messageCaptor.getValue();
        Object[] args = argsCaptor.getValue();
        
        assertThat(logMessage).isEqualTo("[AUDIT] AUTHENTICATION_SUCCESS - user={} ip={} timestamp={}");
        assertThat(args).hasSize(3);
        assertThat(args[0]).isEqualTo("user123");
        assertThat(args[1]).isEqualTo("127.0.0.1");
        assertThat(args[2]).isNotNull(); // timestamp
    }

    @Test
    @DisplayName("Should log authentication failure event")
    void shouldLogAuthenticationFailureEvent() {
        // When
        securityAuditService.logAuthenticationFailure("invalid-token", "127.0.0.1", "Invalid JWT token");

        // Then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(auditLogger).warn(messageCaptor.capture(), argsCaptor.capture());
        
        String logMessage = messageCaptor.getValue();
        Object[] args = argsCaptor.getValue();
        
        assertThat(logMessage).isEqualTo("[AUDIT] AUTHENTICATION_FAILURE - token={} ip={} reason={} timestamp={}");
        assertThat(args).hasSize(4);
        assertThat(args[0].toString()).contains("***"); // masked token
        assertThat(args[1]).isEqualTo("127.0.0.1");
        assertThat(args[2]).isEqualTo("Invalid JWT token");
        assertThat(args[3]).isNotNull(); // timestamp
    }

    @Test
    @DisplayName("Should log authorization failure event")
    void shouldLogAuthorizationFailureEvent() {
        // Given
        given(userContextService.getCurrentUserContext()).willReturn(testUserContext);

        // When
        securityAuditService.logAuthorizationFailure("/api/admin/stats", "ROLE_ADMIN");

        // Then
        verify(auditLogger).warn(eq("[AUDIT] AUTHORIZATION_FAILURE - user={} resource={} requiredRole={} timestamp={}"), 
                eq("user123"), eq("/api/admin/stats"), eq("ROLE_ADMIN"), any());
    }

    @Test
    @DisplayName("Should log rate limit exceeded event")
    void shouldLogRateLimitExceededEvent() {
        // Given
        given(userContextService.getCurrentUserContext()).willReturn(testUserContext);

        // When
        securityAuditService.logRateLimitExceeded("/api/templates", "READ", 100);

        // Then
        verify(auditLogger).warn(eq("[AUDIT] RATE_LIMIT_EXCEEDED - user={} endpoint={} operation={} count={} timestamp={}"),
                eq("user123"), eq("/api/templates"), eq("READ"), eq(100), any());
    }

    @Test
    @DisplayName("Should log preference change event")
    void shouldLogPreferenceChangeEvent() {
        // Given
        given(userContextService.getCurrentUserContext()).willReturn(testUserContext);
        Map<String, Object> changes = Map.of(
                "emailEnabled", true,
                "frequency", "DAILY"
        );

        // When
        securityAuditService.logPreferenceChange("pref-123", "WELCOME", changes);

        // Then
        verify(auditLogger).info(eq("[AUDIT] PREFERENCE_CHANGE - user={} preferenceId={} notificationType={} changes={} timestamp={}"),
                eq("user123"), eq("pref-123"), eq("WELCOME"), any(String.class), any());
    }

    @Test
    @DisplayName("Should log notification sent event")
    void shouldLogNotificationSentEvent() {
        // Given
        given(userContextService.getCurrentUserContext()).willReturn(testUserContext);

        // When
        securityAuditService.logNotificationSent("notif-456", "user789", "EMAIL_VERIFICATION", "email");

        // Then
        verify(auditLogger).info(eq("[AUDIT] NOTIFICATION_SENT - triggeredBy={} notificationId={} targetUser={} type={} channel={} timestamp={}"),
                eq("user123"), eq("notif-456"), eq("user789"), eq("EMAIL_VERIFICATION"), eq("email"), any());
    }

    @Test
    @DisplayName("Should log admin action event")
    void shouldLogAdminActionEvent() {
        // Given
        UserContextService.UserContext adminContext = new UserContextService.UserContext(
                "admin123",
                "admin@example.com",
                "Admin User", 
                java.util.List.of("ROLE_ADMIN")
        );
        given(userContextService.getCurrentUserContext()).willReturn(adminContext);

        // When
        securityAuditService.logAdminAction("VIEW_SYSTEM_STATS", "/api/admin/stats", Map.of("statsType", "overview"));

        // Then
        verify(auditLogger).info(eq("[AUDIT] ADMIN_ACTION - admin={} action={} resource={} parameters={} timestamp={}"),
                eq("admin123"), eq("VIEW_SYSTEM_STATS"), eq("/api/admin/stats"), any(String.class), any());
    }

    @Test
    @DisplayName("Should log template creation event")
    void shouldLogTemplateCreationEvent() {
        // Given
        given(userContextService.getCurrentUserContext()).willReturn(testUserContext);

        // When
        securityAuditService.logTemplateCreation("template-789", "WELCOME", "en");

        // Then
        verify(auditLogger).info(eq("[AUDIT] TEMPLATE_CREATION - user={} templateId={} type={} language={} timestamp={}"),
                eq("user123"), eq("template-789"), eq("WELCOME"), eq("en"), any());
    }

    @Test
    @DisplayName("Should log template deletion event")
    void shouldLogTemplateDeletionEvent() {
        // Given
        given(userContextService.getCurrentUserContext()).willReturn(testUserContext);

        // When
        securityAuditService.logTemplateDeletion("template-789", "WELCOME", "en");

        // Then
        verify(auditLogger).warn(eq("[AUDIT] TEMPLATE_DELETION - user={} templateId={} type={} language={} timestamp={}"),
                eq("user123"), eq("template-789"), eq("WELCOME"), eq("en"), any());
    }

    @Test
    @DisplayName("Should log suspicious activity event")
    void shouldLogSuspiciousActivityEvent() {
        // Given
        given(userContextService.getCurrentUserContext()).willReturn(testUserContext);

        // When
        securityAuditService.logSuspiciousActivity("MULTIPLE_FAILED_ATTEMPTS", "127.0.0.1", 
                Map.of("attemptCount", 5, "timeWindow", "60s"));

        // Then
        verify(auditLogger).error(eq("[AUDIT] SUSPICIOUS_ACTIVITY - user={} activityType={} ip={} metadata={} timestamp={}"),
                eq("user123"), eq("MULTIPLE_FAILED_ATTEMPTS"), eq("127.0.0.1"), any(String.class), any());
    }

    @Test
    @DisplayName("Should log security configuration change event")
    void shouldLogSecurityConfigurationChangeEvent() {
        // Given
        UserContextService.UserContext adminContext = new UserContextService.UserContext(
                "admin123",
                "admin@example.com",
                "Admin User",
                java.util.List.of("ROLE_ADMIN")
        );
        given(userContextService.getCurrentUserContext()).willReturn(adminContext);

        // When
        securityAuditService.logSecurityConfigurationChange("RATE_LIMIT_UPDATE", 
                Map.of("oldLimit", 100, "newLimit", 150, "operationType", "READ"));

        // Then
        verify(auditLogger).warn(eq("[AUDIT] SECURITY_CONFIGURATION_CHANGE - user={} configType={} changes={} timestamp={}"),
                eq("admin123"), eq("RATE_LIMIT_UPDATE"), any(String.class), any());
    }

    @Test
    @DisplayName("Should handle null user context gracefully")
    void shouldHandleNullUserContextGracefully() {
        // Given
        given(userContextService.getCurrentUserContext()).willReturn(null);

        // When
        securityAuditService.logAuthenticationSuccess("127.0.0.1");

        // Then
        verify(auditLogger).info(eq("[AUDIT] AUTHENTICATION_SUCCESS - user={} ip={} timestamp={}"),
                eq("ANONYMOUS"), eq("127.0.0.1"), any());
    }

    @Test
    @DisplayName("Should format audit log entries consistently")
    void shouldFormatAuditLogEntriesConsistently() {
        // Given
        given(userContextService.getCurrentUserContext()).willReturn(testUserContext);

        // When
        securityAuditService.logAuthenticationSuccess("127.0.0.1");

        // Then
        verify(auditLogger).info(eq("[AUDIT] AUTHENTICATION_SUCCESS - user={} ip={} timestamp={}"),
                eq("user123"), eq("127.0.0.1"), any());
    }

    @Test
    @DisplayName("Should include correlation ID in audit logs when available")
    void shouldIncludeCorrelationIdInAuditLogsWhenAvailable() {
        // Given
        given(userContextService.getCurrentUserContext()).willReturn(testUserContext);
        String correlationId = "corr-123-456";

        // When
        securityAuditService.logAuthenticationSuccessWithCorrelation("127.0.0.1", correlationId);

        // Then
        verify(auditLogger).info(eq("[AUDIT] AUTHENTICATION_SUCCESS - user={} ip={} correlationId={} timestamp={}"),
                eq("user123"), eq("127.0.0.1"), eq("corr-123-456"), any());
    }
}