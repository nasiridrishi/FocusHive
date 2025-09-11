package com.focushive.identity.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive unit tests for AuditLog entity covering all business logic,
 * builder patterns, metadata management, and utility methods.
 */
@DisplayName("AuditLog Entity Tests")
class AuditLogUnitTest {

    @Nested
    @DisplayName("Builder Pattern Tests")
    class BuilderPatternTests {

        @Test
        @DisplayName("Should build AuditLog with all fields")
        void shouldBuildAuditLogWithAllFields() {
            // Given
            User user = User.builder().username("testuser").build();
            OAuthClient client = OAuthClient.builder().clientName("test-client").build();
            Map<String, String> metadata = new HashMap<>();
            metadata.put("key1", "value1");

            // When
            AuditLog auditLog = AuditLog.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .client(client)
                    .eventType("USER_LOGIN")
                    .eventCategory("AUTHENTICATION")
                    .description("User login attempt")
                    .resource("/api/login")
                    .action("LOGIN")
                    .outcome("SUCCESS")
                    .severity("INFO")
                    .ipAddress("192.168.1.1")
                    .userAgent("Mozilla/5.0")
                    .sessionId("session123")
                    .requestId("req456")
                    .metadata(metadata)
                    .errorCode(null)
                    .errorMessage(null)
                    .durationMs(150L)
                    .geographicLocation("US-CA")
                    .riskScore(25)
                    .automatedActionTriggered(true)
                    .automatedActionDetails("Rate limit applied")
                    .build();

            // Then
            assertThat(auditLog.getUser()).isEqualTo(user);
            assertThat(auditLog.getClient()).isEqualTo(client);
            assertThat(auditLog.getEventType()).isEqualTo("USER_LOGIN");
            assertThat(auditLog.getEventCategory()).isEqualTo("AUTHENTICATION");
            assertThat(auditLog.getDescription()).isEqualTo("User login attempt");
            assertThat(auditLog.getResource()).isEqualTo("/api/login");
            assertThat(auditLog.getAction()).isEqualTo("LOGIN");
            assertThat(auditLog.getOutcome()).isEqualTo("SUCCESS");
            assertThat(auditLog.getSeverity()).isEqualTo("INFO");
            assertThat(auditLog.getIpAddress()).isEqualTo("192.168.1.1");
            assertThat(auditLog.getUserAgent()).isEqualTo("Mozilla/5.0");
            assertThat(auditLog.getSessionId()).isEqualTo("session123");
            assertThat(auditLog.getRequestId()).isEqualTo("req456");
            assertThat(auditLog.getMetadata()).containsEntry("key1", "value1");
            assertThat(auditLog.getDurationMs()).isEqualTo(150L);
            assertThat(auditLog.getGeographicLocation()).isEqualTo("US-CA");
            assertThat(auditLog.getRiskScore()).isEqualTo(25);
            assertThat(auditLog.isAutomatedActionTriggered()).isTrue();
            assertThat(auditLog.getAutomatedActionDetails()).isEqualTo("Rate limit applied");
        }

        @Test
        @DisplayName("Should build AuditLog with default values")
        void shouldBuildAuditLogWithDefaults() {
            // When
            AuditLog auditLog = AuditLog.builder()
                    .eventType("SYSTEM_EVENT")
                    .eventCategory("SYSTEM")
                    .description("System maintenance")
                    .action("MAINTENANCE")
                    .outcome("SUCCESS")
                    .build();

            // Then
            assertThat(auditLog.getSeverity()).isEqualTo("INFO");
            assertThat(auditLog.getMetadata()).isEmpty();
            assertThat(auditLog.isAutomatedActionTriggered()).isFalse();
        }

        @Test
        @DisplayName("Should build AuditLog with minimal required fields")
        void shouldBuildAuditLogWithMinimalFields() {
            // When
            AuditLog auditLog = AuditLog.builder()
                    .eventType("TEST_EVENT")
                    .eventCategory("TEST")
                    .description("Test description")
                    .action("TEST")
                    .outcome("SUCCESS")
                    .build();

            // Then
            assertThat(auditLog.getEventType()).isEqualTo("TEST_EVENT");
            assertThat(auditLog.getEventCategory()).isEqualTo("TEST");
            assertThat(auditLog.getDescription()).isEqualTo("Test description");
            assertThat(auditLog.getAction()).isEqualTo("TEST");
            assertThat(auditLog.getOutcome()).isEqualTo("SUCCESS");
            assertThat(auditLog.getSeverity()).isEqualTo("INFO");
        }
    }

    @Nested
    @DisplayName("Metadata Management Tests")
    class MetadataManagementTests {

        @Test
        @DisplayName("Should add metadata to existing map")
        void shouldAddMetadataToExistingMap() {
            // Given
            AuditLog auditLog = AuditLog.builder()
                    .eventType("TEST")
                    .eventCategory("TEST")
                    .description("test")
                    .action("TEST")
                    .outcome("SUCCESS")
                    .build();

            // When
            auditLog.addMetadata("key1", "value1");
            auditLog.addMetadata("key2", "value2");

            // Then
            assertThat(auditLog.getMetadata("key1")).isEqualTo("value1");
            assertThat(auditLog.getMetadata("key2")).isEqualTo("value2");
            assertThat(auditLog.getMetadata()).hasSize(2);
        }

        @Test
        @DisplayName("Should add metadata when map is null")
        void shouldAddMetadataWhenMapIsNull() {
            // Given
            AuditLog auditLog = new AuditLog();
            auditLog.setMetadata(null);

            // When
            auditLog.addMetadata("key", "value");

            // Then
            assertThat(auditLog.getMetadata("key")).isEqualTo("value");
            assertThat(auditLog.getMetadata()).isNotNull();
        }

        @Test
        @DisplayName("Should get metadata value")
        void shouldGetMetadataValue() {
            // Given
            Map<String, String> metadata = new HashMap<>();
            metadata.put("test_key", "test_value");
            
            AuditLog auditLog = AuditLog.builder()
                    .eventType("TEST")
                    .eventCategory("TEST")
                    .description("test")
                    .action("TEST")
                    .outcome("SUCCESS")
                    .metadata(metadata)
                    .build();

            // When & Then
            assertThat(auditLog.getMetadata("test_key")).isEqualTo("test_value");
            assertThat(auditLog.getMetadata("nonexistent")).isNull();
        }

        @Test
        @DisplayName("Should return null when metadata map is null")
        void shouldReturnNullWhenMetadataMapIsNull() {
            // Given
            AuditLog auditLog = new AuditLog();
            auditLog.setMetadata(null);

            // When & Then
            assertThat(auditLog.getMetadata("any_key")).isNull();
        }
    }

    @Nested
    @DisplayName("Event Classification Tests")
    class EventClassificationTests {

        @Test
        @DisplayName("Should identify security events")
        void shouldIdentifySecurityEvents() {
            // Given & When & Then
            assertThat(createAuditLogWithCategory("SECURITY").isSecurityEvent()).isTrue();
            assertThat(createAuditLogWithCategory("AUTHENTICATION").isSecurityEvent()).isTrue();
            assertThat(createAuditLogWithCategory("AUTHORIZATION").isSecurityEvent()).isTrue();
            assertThat(createAuditLogWithCategory("SYSTEM").isSecurityEvent()).isFalse();
        }

        @Test
        @DisplayName("Should identify data privacy events")
        void shouldIdentifyDataPrivacyEvents() {
            // Given & When & Then
            assertThat(createAuditLogWithCategory("DATA_PRIVACY").isDataPrivacyEvent()).isTrue();
            assertThat(createAuditLogWithEventType("DATA_ACCESS_USER").isDataPrivacyEvent()).isTrue();
            assertThat(createAuditLogWithEventType("DATA_EXPORT_REQUEST").isDataPrivacyEvent()).isTrue();
            assertThat(createAuditLogWithEventType("GDPR_REQUEST").isDataPrivacyEvent()).isTrue();
            assertThat(createAuditLogWithEventType("USER_LOGIN").isDataPrivacyEvent()).isFalse();
        }

        @Test
        @DisplayName("Should identify high risk events")
        void shouldIdentifyHighRiskEvents() {
            // Given
            AuditLog highRisk = createAuditLogWithRiskScore(85);
            AuditLog mediumRisk = createAuditLogWithRiskScore(50);
            AuditLog lowRisk = createAuditLogWithRiskScore(25);
            AuditLog nullRisk = createAuditLogWithRiskScore(null);

            // When & Then
            assertThat(highRisk.isHighRisk()).isTrue();
            assertThat(mediumRisk.isHighRisk()).isFalse();
            assertThat(lowRisk.isHighRisk()).isFalse();
            assertThat(nullRisk.isHighRisk()).isFalse();
        }

        @Test
        @DisplayName("Should identify system events")
        void shouldIdentifySystemEvents() {
            // Given
            AuditLog userEvent = AuditLog.builder()
                    .user(User.builder().username("test").build())
                    .eventType("TEST")
                    .eventCategory("TEST")
                    .description("test")
                    .action("TEST")
                    .outcome("SUCCESS")
                    .build();

            AuditLog systemEvent = AuditLog.builder()
                    .eventType("SYSTEM_MAINTENANCE")
                    .eventCategory("SYSTEM")
                    .description("System maintenance")
                    .action("MAINTENANCE")
                    .outcome("SUCCESS")
                    .build();

            // When & Then
            assertThat(userEvent.isSystemEvent()).isFalse();
            assertThat(systemEvent.isSystemEvent()).isTrue();
        }

        @Test
        @DisplayName("Should identify failed events")
        void shouldIdentifyFailedEvents() {
            // Given
            AuditLog successEvent = createAuditLogWithOutcome("SUCCESS");
            AuditLog failureEvent = createAuditLogWithOutcome("FAILURE");
            AuditLog partialEvent = createAuditLogWithOutcome("PARTIAL");

            // When & Then
            assertThat(successEvent.isFailed()).isFalse();
            assertThat(failureEvent.isFailed()).isTrue();
            assertThat(partialEvent.isFailed()).isFalse();
        }

        @Test
        @DisplayName("Should identify critical events")
        void shouldIdentifyCriticalEvents() {
            // Given
            AuditLog criticalEvent = createAuditLogWithSeverity("CRITICAL");
            AuditLog errorEvent = createAuditLogWithSeverity("ERROR");
            AuditLog infoEvent = createAuditLogWithSeverity("INFO");

            // When & Then
            assertThat(criticalEvent.isCritical()).isTrue();
            assertThat(errorEvent.isCritical()).isFalse();
            assertThat(infoEvent.isCritical()).isFalse();
        }

        private AuditLog createAuditLogWithCategory(String category) {
            return AuditLog.builder()
                    .eventType("TEST")
                    .eventCategory(category)
                    .description("test")
                    .action("TEST")
                    .outcome("SUCCESS")
                    .build();
        }

        private AuditLog createAuditLogWithEventType(String eventType) {
            return AuditLog.builder()
                    .eventType(eventType)
                    .eventCategory("TEST")
                    .description("test")
                    .action("TEST")
                    .outcome("SUCCESS")
                    .build();
        }

        private AuditLog createAuditLogWithRiskScore(Integer riskScore) {
            return AuditLog.builder()
                    .eventType("TEST")
                    .eventCategory("TEST")
                    .description("test")
                    .action("TEST")
                    .outcome("SUCCESS")
                    .riskScore(riskScore)
                    .build();
        }

        private AuditLog createAuditLogWithOutcome(String outcome) {
            return AuditLog.builder()
                    .eventType("TEST")
                    .eventCategory("TEST")
                    .description("test")
                    .action("TEST")
                    .outcome(outcome)
                    .build();
        }

        private AuditLog createAuditLogWithSeverity(String severity) {
            return AuditLog.builder()
                    .eventType("TEST")
                    .eventCategory("TEST")
                    .description("test")
                    .action("TEST")
                    .outcome("SUCCESS")
                    .severity(severity)
                    .build();
        }
    }

    @Nested
    @DisplayName("Formatted Log Message Tests")
    class FormattedLogMessageTests {

        @Test
        @DisplayName("Should format complete log message with all fields")
        void shouldFormatCompleteLogMessage() {
            // Given
            User user = User.builder().username("testuser").build();
            OAuthClient client = OAuthClient.builder().clientName("test-client").build();
            
            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .client(client)
                    .eventType("USER_LOGIN")
                    .eventCategory("AUTHENTICATION")
                    .description("User successfully logged in")
                    .resource("/api/login")
                    .action("LOGIN")
                    .outcome("SUCCESS")
                    .severity("INFO")
                    .ipAddress("192.168.1.1")
                    .build();

            // When
            String formatted = auditLog.getFormattedLogMessage();

            // Then
            assertThat(formatted).contains("[INFO]");
            assertThat(formatted).contains("AUTHENTICATION.USER_LOGIN");
            assertThat(formatted).contains("User: testuser");
            assertThat(formatted).contains("Client: test-client");
            assertThat(formatted).contains("LOGIN SUCCESS");
            assertThat(formatted).contains("Resource: /api/login");
            assertThat(formatted).contains("IP: 192.168.1.1");
            assertThat(formatted).contains("User successfully logged in");
        }

        @Test
        @DisplayName("Should format log message without user")
        void shouldFormatLogMessageWithoutUser() {
            // Given
            AuditLog auditLog = AuditLog.builder()
                    .eventType("SYSTEM_MAINTENANCE")
                    .eventCategory("SYSTEM")
                    .description("Scheduled maintenance completed")
                    .action("MAINTENANCE")
                    .outcome("SUCCESS")
                    .severity("INFO")
                    .build();

            // When
            String formatted = auditLog.getFormattedLogMessage();

            // Then
            assertThat(formatted).contains("[INFO]");
            assertThat(formatted).contains("SYSTEM.SYSTEM_MAINTENANCE");
            assertThat(formatted).contains("MAINTENANCE SUCCESS");
            assertThat(formatted).contains("Scheduled maintenance completed");
            assertThat(formatted).doesNotContain("User:");
            assertThat(formatted).doesNotContain("Client:");
        }

        @Test
        @DisplayName("Should format log message without optional fields")
        void shouldFormatLogMessageWithoutOptionalFields() {
            // Given
            AuditLog auditLog = AuditLog.builder()
                    .eventType("DATA_PROCESSING")
                    .eventCategory("DATA")
                    .description("Data processing completed")
                    .action("PROCESS")
                    .outcome("SUCCESS")
                    .severity("INFO")
                    .build();

            // When
            String formatted = auditLog.getFormattedLogMessage();

            // Then
            assertThat(formatted).contains("[INFO]");
            assertThat(formatted).contains("DATA.DATA_PROCESSING");
            assertThat(formatted).contains("PROCESS SUCCESS");
            assertThat(formatted).contains("Data processing completed");
            assertThat(formatted).doesNotContain("Resource:");
            assertThat(formatted).doesNotContain("IP:");
        }

        @Test
        @DisplayName("Should format critical failure log message")
        void shouldFormatCriticalFailureLogMessage() {
            // Given
            User user = User.builder().username("admin").build();
            
            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .eventType("SECURITY_BREACH")
                    .eventCategory("SECURITY")
                    .description("Unauthorized access detected")
                    .resource("/admin/users")
                    .action("ACCESS")
                    .outcome("FAILURE")
                    .severity("CRITICAL")
                    .ipAddress("10.0.0.1")
                    .build();

            // When
            String formatted = auditLog.getFormattedLogMessage();

            // Then
            assertThat(formatted).contains("[CRITICAL]");
            assertThat(formatted).contains("SECURITY.SECURITY_BREACH");
            assertThat(formatted).contains("User: admin");
            assertThat(formatted).contains("ACCESS FAILURE");
            assertThat(formatted).contains("Resource: /admin/users");
            assertThat(formatted).contains("IP: 10.0.0.1");
            assertThat(formatted).contains("Unauthorized access detected");
        }
    }

    @Nested
    @DisplayName("Follow-up Audit Log Tests")
    class FollowUpAuditLogTests {

        @Test
        @DisplayName("Should create follow-up audit log with inherited fields")
        void shouldCreateFollowUpAuditLogWithInheritedFields() {
            // Given
            User user = User.builder().username("testuser").build();
            OAuthClient client = OAuthClient.builder().clientName("test-client").build();
            
            AuditLog originalLog = AuditLog.builder()
                    .user(user)
                    .client(client)
                    .eventType("USER_LOGIN")
                    .eventCategory("AUTHENTICATION")
                    .description("User login attempt")
                    .resource("/api/login")
                    .action("LOGIN")
                    .outcome("SUCCESS")
                    .severity("INFO")
                    .ipAddress("192.168.1.1")
                    .userAgent("Mozilla/5.0")
                    .sessionId("session123")
                    .requestId("req456")
                    .geographicLocation("US-CA")
                    .build();

            // When
            AuditLog followUpLog = originalLog.createFollowUp(
                    "PERMISSION_GRANTED",
                    "User granted additional permissions",
                    "SUCCESS"
            );

            // Then
            assertThat(followUpLog.getUser()).isEqualTo(user);
            assertThat(followUpLog.getClient()).isEqualTo(client);
            assertThat(followUpLog.getEventType()).isEqualTo("PERMISSION_GRANTED");
            assertThat(followUpLog.getEventCategory()).isEqualTo("AUTHENTICATION");
            assertThat(followUpLog.getDescription()).isEqualTo("User granted additional permissions");
            assertThat(followUpLog.getResource()).isEqualTo("/api/login");
            assertThat(followUpLog.getAction()).isEqualTo("FOLLOW_UP");
            assertThat(followUpLog.getOutcome()).isEqualTo("SUCCESS");
            assertThat(followUpLog.getSeverity()).isEqualTo("INFO");
            assertThat(followUpLog.getIpAddress()).isEqualTo("192.168.1.1");
            assertThat(followUpLog.getUserAgent()).isEqualTo("Mozilla/5.0");
            assertThat(followUpLog.getSessionId()).isEqualTo("session123");
            assertThat(followUpLog.getRequestId()).isEqualTo("req456");
            assertThat(followUpLog.getGeographicLocation()).isEqualTo("US-CA");
        }

        @Test
        @DisplayName("Should create follow-up audit log for system event")
        void shouldCreateFollowUpAuditLogForSystemEvent() {
            // Given
            AuditLog originalLog = AuditLog.builder()
                    .eventType("SYSTEM_BACKUP")
                    .eventCategory("SYSTEM")
                    .description("System backup started")
                    .resource("/backup")
                    .action("BACKUP")
                    .outcome("SUCCESS")
                    .severity("INFO")
                    .build();

            // When
            AuditLog followUpLog = originalLog.createFollowUp(
                    "BACKUP_COMPLETED",
                    "System backup completed successfully",
                    "SUCCESS"
            );

            // Then
            assertThat(followUpLog.getUser()).isNull();
            assertThat(followUpLog.getClient()).isNull();
            assertThat(followUpLog.getEventType()).isEqualTo("BACKUP_COMPLETED");
            assertThat(followUpLog.getEventCategory()).isEqualTo("SYSTEM");
            assertThat(followUpLog.getDescription()).isEqualTo("System backup completed successfully");
            assertThat(followUpLog.getAction()).isEqualTo("FOLLOW_UP");
            assertThat(followUpLog.getOutcome()).isEqualTo("SUCCESS");
        }
    }

    @Nested
    @DisplayName("Builder Helper Methods Tests")
    class BuilderHelperMethodsTests {

        @Test
        @DisplayName("Should create login success audit log")
        void shouldCreateLoginSuccessAuditLog() {
            // Given
            User user = User.builder().username("testuser").build();
            String ipAddress = "192.168.1.1";

            // When
            AuditLog auditLog = AuditLog.builder()
                    .loginSuccess(user, ipAddress)
                    .build();

            // Then
            assertThat(auditLog.getUser()).isEqualTo(user);
            assertThat(auditLog.getEventType()).isEqualTo("USER_LOGIN");
            assertThat(auditLog.getEventCategory()).isEqualTo("AUTHENTICATION");
            assertThat(auditLog.getDescription()).isEqualTo("User successfully logged in");
            assertThat(auditLog.getAction()).isEqualTo("LOGIN");
            assertThat(auditLog.getOutcome()).isEqualTo("SUCCESS");
            assertThat(auditLog.getSeverity()).isEqualTo("INFO");
            assertThat(auditLog.getIpAddress()).isEqualTo(ipAddress);
        }

        @Test
        @DisplayName("Should create login failure audit log")
        void shouldCreateLoginFailureAuditLog() {
            // Given
            String username = "testuser";
            String ipAddress = "192.168.1.1";
            String reason = "Invalid password";

            // When
            AuditLog auditLog = AuditLog.builder()
                    .loginFailure(username, ipAddress, reason)
                    .build();

            // Then
            assertThat(auditLog.getEventType()).isEqualTo("USER_LOGIN_FAILED");
            assertThat(auditLog.getEventCategory()).isEqualTo("SECURITY");
            assertThat(auditLog.getDescription()).contains("Failed login attempt for user: testuser");
            assertThat(auditLog.getDescription()).contains("Reason: Invalid password");
            assertThat(auditLog.getAction()).isEqualTo("LOGIN");
            assertThat(auditLog.getOutcome()).isEqualTo("FAILURE");
            assertThat(auditLog.getSeverity()).isEqualTo("WARNING");
            assertThat(auditLog.getIpAddress()).isEqualTo(ipAddress);
        }

        @Test
        @DisplayName("Should create data access audit log")
        void shouldCreateDataAccessAuditLog() {
            // Given
            User user = User.builder().username("testuser").build();
            OAuthClient client = OAuthClient.builder().clientName("test-app").build();
            String dataType = "profile";
            String permissions = "read";

            // When
            AuditLog auditLog = AuditLog.builder()
                    .dataAccess(user, client, dataType, permissions)
                    .build();

            // Then
            assertThat(auditLog.getUser()).isEqualTo(user);
            assertThat(auditLog.getClient()).isEqualTo(client);
            assertThat(auditLog.getEventType()).isEqualTo("DATA_ACCESS");
            assertThat(auditLog.getEventCategory()).isEqualTo("DATA_PRIVACY");
            assertThat(auditLog.getDescription()).isEqualTo("Client accessed user data: profile");
            assertThat(auditLog.getResource()).isEqualTo("data/profile");
            assertThat(auditLog.getAction()).isEqualTo("read");
            assertThat(auditLog.getOutcome()).isEqualTo("SUCCESS");
            assertThat(auditLog.getSeverity()).isEqualTo("INFO");
            assertThat(auditLog.getMetadata("data_type")).isEqualTo("profile");
            assertThat(auditLog.getMetadata("permissions")).isEqualTo("read");
        }

        @Test
        @DisplayName("Should create consent given audit log")
        void shouldCreateConsentGivenAuditLog() {
            // Given
            User user = User.builder().username("testuser").build();
            String consentType = "data_processing";
            String version = "v2.1";

            // When
            AuditLog auditLog = AuditLog.builder()
                    .consentGiven(user, consentType, version)
                    .build();

            // Then
            assertThat(auditLog.getUser()).isEqualTo(user);
            assertThat(auditLog.getEventType()).isEqualTo("CONSENT_GIVEN");
            assertThat(auditLog.getEventCategory()).isEqualTo("DATA_PRIVACY");
            assertThat(auditLog.getDescription()).isEqualTo("User gave consent for: data_processing");
            assertThat(auditLog.getAction()).isEqualTo("GRANT");
            assertThat(auditLog.getOutcome()).isEqualTo("SUCCESS");
            assertThat(auditLog.getSeverity()).isEqualTo("INFO");
            assertThat(auditLog.getMetadata("consent_type")).isEqualTo("data_processing");
            assertThat(auditLog.getMetadata("consent_version")).isEqualTo("v2.1");
        }
    }

    @Nested
    @DisplayName("Entity Lifecycle Tests")
    class EntityLifecycleTests {

        @Test
        @DisplayName("Should set creation timestamp automatically")
        void shouldSetCreationTimestampAutomatically() {
            // Given
            AuditLog auditLog = AuditLog.builder()
                    .eventType("TEST")
                    .eventCategory("TEST")
                    .description("test")
                    .action("TEST")
                    .outcome("SUCCESS")
                    .build();

            // When
            Instant beforeSave = Instant.now();
            auditLog.setCreatedAt(Instant.now()); // Simulate @CreationTimestamp
            Instant afterSave = Instant.now();

            // Then
            assertThat(auditLog.getCreatedAt()).isBetween(beforeSave, afterSave);
        }

        @Test
        @DisplayName("Should maintain immutable creation timestamp")
        void shouldMaintainImmutableCreationTimestamp() {
            // Given
            AuditLog auditLog = AuditLog.builder()
                    .eventType("TEST")
                    .eventCategory("TEST")
                    .description("test")
                    .action("TEST")
                    .outcome("SUCCESS")
                    .build();

            Instant originalTimestamp = Instant.now();
            auditLog.setCreatedAt(originalTimestamp);

            // When
            Instant laterTimestamp = Instant.now().plusSeconds(60);
            auditLog.setCreatedAt(laterTimestamp);

            // Then
            assertThat(auditLog.getCreatedAt()).isEqualTo(laterTimestamp);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesAndErrorHandlingTests {

        @Test
        @DisplayName("Should handle null values gracefully")
        void shouldHandleNullValuesGracefully() {
            // Given
            AuditLog auditLog = AuditLog.builder()
                    .eventType("TEST")
                    .eventCategory("TEST")
                    .description("test")
                    .action("TEST")
                    .outcome("SUCCESS")
                    .build();

            // When & Then - Should not throw exceptions
            assertThat(auditLog.getMetadata("nonexistent")).isNull();
            assertThat(auditLog.isHighRisk()).isFalse(); // riskScore is null
            assertThat(auditLog.isSystemEvent()).isTrue(); // user is null
        }

        @Test
        @DisplayName("Should handle empty metadata map")
        void shouldHandleEmptyMetadataMap() {
            // Given
            AuditLog auditLog = AuditLog.builder()
                    .eventType("TEST")
                    .eventCategory("TEST")
                    .description("test")
                    .action("TEST")
                    .outcome("SUCCESS")
                    .metadata(new HashMap<>())
                    .build();

            // When & Then
            assertThat(auditLog.getMetadata("any_key")).isNull();
            assertThat(auditLog.getMetadata()).isEmpty();
        }

        @Test
        @DisplayName("Should handle very long descriptions")
        void shouldHandleVeryLongDescriptions() {
            // Given
            StringBuilder longDescription = new StringBuilder();
            for (int i = 0; i < 200; i++) {
                longDescription.append("Very long description part ").append(i).append(". ");
            }

            // When
            AuditLog auditLog = AuditLog.builder()
                    .eventType("TEST")
                    .eventCategory("TEST")
                    .description(longDescription.toString())
                    .action("TEST")
                    .outcome("SUCCESS")
                    .build();

            // Then
            assertThat(auditLog.getDescription()).isEqualTo(longDescription.toString());
            assertThat(auditLog.getFormattedLogMessage()).contains(longDescription.toString());
        }

        @Test
        @DisplayName("Should handle special characters in strings")
        void shouldHandleSpecialCharactersInStrings() {
            // Given
            String specialChars = "Test with special chars: àáâãäåæçèéêëìíîïñòóôõöùúûüý@#$%^&*()";

            // When
            AuditLog auditLog = AuditLog.builder()
                    .eventType("SPECIAL_CHARS_TEST")
                    .eventCategory("TEST")
                    .description(specialChars)
                    .action("TEST")
                    .outcome("SUCCESS")
                    .build();

            // Then
            assertThat(auditLog.getDescription()).isEqualTo(specialChars);
            assertThat(auditLog.getFormattedLogMessage()).contains(specialChars);
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should exclude user and client from equals and hashCode")
        void shouldExcludeUserAndClientFromEqualsAndHashCode() {
            // Given
            User user1 = User.builder().username("user1").build();
            User user2 = User.builder().username("user2").build();
            OAuthClient client1 = OAuthClient.builder().clientName("client1").build();
            OAuthClient client2 = OAuthClient.builder().clientName("client2").build();

            AuditLog log1 = AuditLog.builder()
                    .id(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
                    .user(user1)
                    .client(client1)
                    .eventType("TEST")
                    .eventCategory("TEST")
                    .description("test")
                    .action("TEST")
                    .outcome("SUCCESS")
                    .build();

            AuditLog log2 = AuditLog.builder()
                    .id(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
                    .user(user2)
                    .client(client2)
                    .eventType("TEST")
                    .eventCategory("TEST")
                    .description("test")
                    .action("TEST")
                    .outcome("SUCCESS")
                    .build();

            // When & Then
            assertThat(log1).isEqualTo(log2);
            assertThat(log1.hashCode()).isEqualTo(log2.hashCode());
        }
    }
}