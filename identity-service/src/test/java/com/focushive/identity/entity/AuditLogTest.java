package com.focushive.identity.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for AuditLog entity
 */
@ExtendWith(SpringJUnitExtension.class)
@DataJpaTest
class AuditLogTest {

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private OAuthClient testClient;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("hashedpassword")
                .displayName("Test User")
                .build();
        entityManager.persistAndFlush(testUser);

        // Create test OAuth client
        testClient = OAuthClient.builder()
                .clientId("test-client-id")
                .clientSecret("secret")
                .clientName("Test Client")
                .user(testUser)
                .build();
        entityManager.persistAndFlush(testClient);
    }

    @Test
    void shouldCreateAuditLogEntry() {
        // Given
        AuditLog auditLog = AuditLog.builder()
                .user(testUser)
                .eventType("USER_LOGIN")
                .eventCategory("AUTHENTICATION")
                .description("User successfully logged in")
                .ipAddress("192.168.1.100")
                .userAgent("Mozilla/5.0 (Test Browser)")
                .resource("auth/login")
                .action("LOGIN")
                .outcome("SUCCESS")
                .build();

        // When
        AuditLog savedLog = entityManager.persistAndFlush(auditLog);

        // Then
        assertThat(savedLog.getId()).isNotNull();
        assertThat(savedLog.getUser()).isEqualTo(testUser);
        assertThat(savedLog.getEventType()).isEqualTo("USER_LOGIN");
        assertThat(savedLog.getEventCategory()).isEqualTo("AUTHENTICATION");
        assertThat(savedLog.getDescription()).isEqualTo("User successfully logged in");
        assertThat(savedLog.getIpAddress()).isEqualTo("192.168.1.100");
        assertThat(savedLog.getUserAgent()).isEqualTo("Mozilla/5.0 (Test Browser)");
        assertThat(savedLog.getResource()).isEqualTo("auth/login");
        assertThat(savedLog.getAction()).isEqualTo("LOGIN");
        assertThat(savedLog.getOutcome()).isEqualTo("SUCCESS");
        assertThat(savedLog.getCreatedAt()).isNotNull();
        assertThat(savedLog.getSeverity()).isEqualTo("INFO"); // Default value
    }

    @Test
    void shouldCreateAuditLogWithClient() {
        // Given
        AuditLog auditLog = AuditLog.builder()
                .user(testUser)
                .client(testClient)
                .eventType("OAUTH_TOKEN_ISSUED")
                .eventCategory("AUTHORIZATION")
                .description("OAuth access token issued to client")
                .resource("oauth/token")
                .action("CREATE")
                .outcome("SUCCESS")
                .severity("INFO")
                .build();

        // When
        AuditLog savedLog = entityManager.persistAndFlush(auditLog);

        // Then
        assertThat(savedLog.getClient()).isEqualTo(testClient);
        assertThat(savedLog.getEventType()).isEqualTo("OAUTH_TOKEN_ISSUED");
        assertThat(savedLog.getEventCategory()).isEqualTo("AUTHORIZATION");
    }

    @Test
    void shouldSetDefaultValues() {
        // Given
        AuditLog auditLog = AuditLog.builder()
                .eventType("SYSTEM_EVENT")
                .eventCategory("SYSTEM")
                .description("System maintenance performed")
                .action("MAINTENANCE")
                .outcome("SUCCESS")
                .build();

        // When
        AuditLog savedLog = entityManager.persistAndFlush(auditLog);

        // Then
        assertThat(savedLog.getSeverity()).isEqualTo("INFO"); // Default
        assertThat(savedLog.getCreatedAt()).isNotNull();
        assertThat(savedLog.getUser()).isNull(); // Optional for system events
        assertThat(savedLog.getClient()).isNull(); // Optional
    }

    @Test
    void shouldHandleSecurityEvents() {
        // Given
        AuditLog securityLog = AuditLog.builder()
                .user(testUser)
                .eventType("FAILED_LOGIN_ATTEMPT")
                .eventCategory("SECURITY")
                .description("Multiple failed login attempts detected")
                .ipAddress("10.0.0.1")
                .resource("auth/login")
                .action("LOGIN")
                .outcome("FAILURE")
                .severity("WARNING")
                .metadata(Map.of(
                    "attempt_count", "5",
                    "time_window", "300",
                    "blocked", "true"
                ))
                .build();

        // When
        AuditLog savedLog = entityManager.persistAndFlush(securityLog);

        // Then
        assertThat(savedLog.getEventCategory()).isEqualTo("SECURITY");
        assertThat(savedLog.getSeverity()).isEqualTo("WARNING");
        assertThat(savedLog.getOutcome()).isEqualTo("FAILURE");
        assertThat(savedLog.getMetadata()).containsEntry("attempt_count", "5");
        assertThat(savedLog.getMetadata()).containsEntry("blocked", "true");
    }

    @Test
    void shouldTrackDataAccessEvents() {
        // Given
        AuditLog dataAccessLog = AuditLog.builder()
                .user(testUser)
                .client(testClient)
                .eventType("DATA_ACCESS")
                .eventCategory("DATA_PRIVACY")
                .description("Client accessed user profile data")
                .resource("api/users/profile")
                .action("READ")
                .outcome("SUCCESS")
                .severity("INFO")
                .metadata(Map.of(
                    "data_type", "profile",
                    "fields_accessed", "name,email,preferences",
                    "retention_policy", "365_days"
                ))
                .build();

        // When
        AuditLog savedLog = entityManager.persistAndFlush(dataAccessLog);

        // Then
        assertThat(savedLog.getEventCategory()).isEqualTo("DATA_PRIVACY");
        assertThat(savedLog.getAction()).isEqualTo("READ");
        assertThat(savedLog.getMetadata()).containsEntry("data_type", "profile");
        assertThat(savedLog.getMetadata()).containsEntry("fields_accessed", "name,email,preferences");
    }

    @Test
    void shouldTrackPermissionChanges() {
        // Given
        AuditLog permissionLog = AuditLog.builder()
                .user(testUser)
                .eventType("PERMISSION_GRANTED")
                .eventCategory("AUTHORIZATION")
                .description("User granted data access permission to client")
                .resource("permissions/data")
                .action("GRANT")
                .outcome("SUCCESS")
                .severity("INFO")
                .metadata(Map.of(
                    "permission_type", "data_access",
                    "client_id", testClient.getClientId(),
                    "scope", "read,write",
                    "duration", "permanent"
                ))
                .build();

        // When
        AuditLog savedLog = entityManager.persistAndFlush(permissionLog);

        // Then
        assertThat(savedLog.getEventType()).isEqualTo("PERMISSION_GRANTED");
        assertThat(savedLog.getAction()).isEqualTo("GRANT");
        assertThat(savedLog.getMetadata()).containsEntry("permission_type", "data_access");
        assertThat(savedLog.getMetadata()).containsEntry("scope", "read,write");
    }

    @Test
    void shouldHandleCriticalSecurityEvents() {
        // Given
        AuditLog criticalLog = AuditLog.builder()
                .user(testUser)
                .eventType("ACCOUNT_COMPROMISE_DETECTED")
                .eventCategory("SECURITY")
                .description("Suspicious activity detected - account temporarily locked")
                .ipAddress("192.168.1.999") // Suspicious IP
                .userAgent("Automated Tool")
                .resource("auth/login")
                .action("BLOCK")
                .outcome("SUCCESS")
                .severity("CRITICAL")
                .metadata(Map.of(
                    "detection_method", "anomaly_detection",
                    "risk_score", "95",
                    "automatic_action", "account_lock",
                    "admin_notified", "true"
                ))
                .build();

        // When
        AuditLog savedLog = entityManager.persistAndFlush(criticalLog);

        // Then
        assertThat(savedLog.getSeverity()).isEqualTo("CRITICAL");
        assertThat(savedLog.getEventType()).isEqualTo("ACCOUNT_COMPROMISE_DETECTED");
        assertThat(savedLog.getAction()).isEqualTo("BLOCK");
        assertThat(savedLog.getMetadata()).containsEntry("risk_score", "95");
        assertThat(savedLog.getMetadata()).containsEntry("admin_notified", "true");
    }

    @Test
    void shouldTrackSystemEvents() {
        // Given
        AuditLog systemLog = AuditLog.builder()
                .eventType("SYSTEM_BACKUP")
                .eventCategory("SYSTEM")
                .description("Automated system backup completed")
                .resource("system/backup")
                .action("BACKUP")
                .outcome("SUCCESS")
                .severity("INFO")
                .metadata(Map.of(
                    "backup_type", "incremental",
                    "data_size", "1.2GB",
                    "duration", "45_seconds",
                    "storage_location", "encrypted_backup_server"
                ))
                .build();

        // When
        AuditLog savedLog = entityManager.persistAndFlush(systemLog);

        // Then
        assertThat(savedLog.getUser()).isNull(); // System event
        assertThat(savedLog.getClient()).isNull();
        assertThat(savedLog.getEventCategory()).isEqualTo("SYSTEM");
        assertThat(savedLog.getMetadata()).containsEntry("backup_type", "incremental");
    }

    @Test
    void shouldHandleCompliantDataDeletion() {
        // Given
        AuditLog deletionLog = AuditLog.builder()
                .user(testUser)
                .eventType("GDPR_DATA_DELETION")
                .eventCategory("DATA_PRIVACY")
                .description("User data deleted in compliance with GDPR request")
                .resource("user/data")
                .action("DELETE")
                .outcome("SUCCESS")
                .severity("INFO")
                .metadata(Map.of(
                    "request_type", "right_to_be_forgotten",
                    "data_categories", "profile,activities,preferences",
                    "retention_override", "legal_requirement",
                    "verification_method", "email_confirmation"
                ))
                .build();

        // When
        AuditLog savedLog = entityManager.persistAndFlush(deletionLog);

        // Then
        assertThat(savedLog.getEventType()).isEqualTo("GDPR_DATA_DELETION");
        assertThat(savedLog.getEventCategory()).isEqualTo("DATA_PRIVACY");
        assertThat(savedLog.getAction()).isEqualTo("DELETE");
        assertThat(savedLog.getMetadata()).containsEntry("request_type", "right_to_be_forgotten");
    }
}