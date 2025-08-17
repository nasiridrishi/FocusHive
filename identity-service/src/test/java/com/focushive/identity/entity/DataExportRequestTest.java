package com.focushive.identity.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for DataExportRequest entity (GDPR Data Portability)
 */
@ExtendWith(SpringJUnitExtension.class)
@DataJpaTest
class DataExportRequestTest {

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;

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
    }

    @Test
    void shouldCreateDataExportRequest() {
        // Given
        DataExportRequest exportRequest = DataExportRequest.builder()
                .user(testUser)
                .requestType("full_export")
                .format("JSON")
                .dataCategories(Set.of("profile", "activities", "preferences", "audit_logs"))
                .reason("Data portability request under GDPR Article 20")
                .verificationMethod("email")
                .build();

        // When
        DataExportRequest savedRequest = entityManager.persistAndFlush(exportRequest);

        // Then
        assertThat(savedRequest.getId()).isNotNull();
        assertThat(savedRequest.getUser()).isEqualTo(testUser);
        assertThat(savedRequest.getRequestType()).isEqualTo("full_export");
        assertThat(savedRequest.getFormat()).isEqualTo("JSON");
        assertThat(savedRequest.getDataCategories()).containsExactlyInAnyOrder("profile", "activities", "preferences", "audit_logs");
        assertThat(savedRequest.getReason()).isEqualTo("Data portability request under GDPR Article 20");
        assertThat(savedRequest.getVerificationMethod()).isEqualTo("email");
        assertThat(savedRequest.getStatus()).isEqualTo("PENDING"); // Default status
        assertThat(savedRequest.getCreatedAt()).isNotNull();
        assertThat(savedRequest.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldSetDefaultValues() {
        // Given
        DataExportRequest exportRequest = DataExportRequest.builder()
                .user(testUser)
                .requestType("partial_export")
                .dataCategories(Set.of("profile"))
                .build();

        // When
        DataExportRequest savedRequest = entityManager.persistAndFlush(exportRequest);

        // Then
        assertThat(savedRequest.getStatus()).isEqualTo("PENDING");
        assertThat(savedRequest.getFormat()).isEqualTo("JSON"); // Default format
        assertThat(savedRequest.getCreatedAt()).isNotNull();
        assertThat(savedRequest.getUpdatedAt()).isNotNull();
        assertThat(savedRequest.getReason()).isNull(); // Optional
        assertThat(savedRequest.getVerificationMethod()).isNull(); // Optional
    }

    @Test
    void shouldTrackRequestProcessing() {
        // Given
        DataExportRequest exportRequest = DataExportRequest.builder()
                .user(testUser)
                .requestType("account_data")
                .dataCategories(Set.of("profile", "settings"))
                .format("CSV")
                .build();
        DataExportRequest savedRequest = entityManager.persistAndFlush(exportRequest);

        // When - Processing starts
        savedRequest.setStatus("PROCESSING");
        savedRequest.setProcessingStartedAt(Instant.now());
        DataExportRequest updatedRequest = entityManager.persistAndFlush(savedRequest);

        // Then
        assertThat(updatedRequest.getStatus()).isEqualTo("PROCESSING");
        assertThat(updatedRequest.getProcessingStartedAt()).isNotNull();
    }

    @Test
    void shouldCompleteExportRequest() {
        // Given
        DataExportRequest exportRequest = DataExportRequest.builder()
                .user(testUser)
                .requestType("full_export")
                .dataCategories(Set.of("all"))
                .format("JSON")
                .build();
        DataExportRequest savedRequest = entityManager.persistAndFlush(exportRequest);

        // When - Export completes
        savedRequest.setStatus("COMPLETED");
        savedRequest.setProcessingStartedAt(Instant.now().minus(2, ChronoUnit.HOURS));
        savedRequest.setCompletedAt(Instant.now());
        savedRequest.setExportFilePath("/exports/user_" + testUser.getId() + "_export.json");
        savedRequest.setFileSizeBytes(1024000L); // 1MB
        savedRequest.setRecordCount(15000);
        DataExportRequest completedRequest = entityManager.persistAndFlush(savedRequest);

        // Then
        assertThat(completedRequest.getStatus()).isEqualTo("COMPLETED");
        assertThat(completedRequest.getCompletedAt()).isNotNull();
        assertThat(completedRequest.getExportFilePath()).isEqualTo("/exports/user_" + testUser.getId() + "_export.json");
        assertThat(completedRequest.getFileSizeBytes()).isEqualTo(1024000L);
        assertThat(completedRequest.getRecordCount()).isEqualTo(15000);
    }

    @Test
    void shouldHandleFailedExportRequest() {
        // Given
        DataExportRequest exportRequest = DataExportRequest.builder()
                .user(testUser)
                .requestType("large_export")
                .dataCategories(Set.of("activities", "audit_logs"))
                .format("XML")
                .build();
        DataExportRequest savedRequest = entityManager.persistAndFlush(exportRequest);

        // When - Export fails
        savedRequest.setStatus("FAILED");
        savedRequest.setProcessingStartedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        savedRequest.setFailedAt(Instant.now());
        savedRequest.setErrorMessage("Export size exceeded maximum limit");
        savedRequest.setErrorCode("EXPORT_SIZE_LIMIT_EXCEEDED");
        DataExportRequest failedRequest = entityManager.persistAndFlush(savedRequest);

        // Then
        assertThat(failedRequest.getStatus()).isEqualTo("FAILED");
        assertThat(failedRequest.getFailedAt()).isNotNull();
        assertThat(failedRequest.getErrorMessage()).isEqualTo("Export size exceeded maximum limit");
        assertThat(failedRequest.getErrorCode()).isEqualTo("EXPORT_SIZE_LIMIT_EXCEEDED");
    }

    @Test
    void shouldSetExpirationDate() {
        // Given
        DataExportRequest exportRequest = DataExportRequest.builder()
                .user(testUser)
                .requestType("temporary_export")
                .dataCategories(Set.of("profile"))
                .format("JSON")
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        // When
        DataExportRequest savedRequest = entityManager.persistAndFlush(exportRequest);

        // Then
        assertThat(savedRequest.getExpiresAt()).isAfter(Instant.now());
        assertThat(savedRequest.isExpired()).isFalse();
    }

    @Test
    void shouldDetectExpiredExport() {
        // Given
        DataExportRequest exportRequest = DataExportRequest.builder()
                .user(testUser)
                .requestType("expired_export")
                .dataCategories(Set.of("profile"))
                .format("JSON")
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();

        // When
        DataExportRequest savedRequest = entityManager.persistAndFlush(exportRequest);

        // Then
        assertThat(savedRequest.isExpired()).isTrue();
    }

    @Test
    void shouldStoreRequestMetadata() {
        // Given
        DataExportRequest exportRequest = DataExportRequest.builder()
                .user(testUser)
                .requestType("filtered_export")
                .dataCategories(Set.of("activities"))
                .format("JSON")
                .metadata(Map.of(
                    "date_range_start", "2024-01-01",
                    "date_range_end", "2024-12-31",
                    "include_deleted", "false",
                    "anonymize_ip", "true",
                    "compression", "gzip"
                ))
                .build();

        // When
        DataExportRequest savedRequest = entityManager.persistAndFlush(exportRequest);

        // Then
        assertThat(savedRequest.getMetadata()).containsEntry("date_range_start", "2024-01-01");
        assertThat(savedRequest.getMetadata()).containsEntry("date_range_end", "2024-12-31");
        assertThat(savedRequest.getMetadata()).containsEntry("include_deleted", "false");
        assertThat(savedRequest.getMetadata()).containsEntry("anonymize_ip", "true");
        assertThat(savedRequest.getMetadata()).containsEntry("compression", "gzip");
    }

    @Test
    void shouldTrackDownloadActivity() {
        // Given
        DataExportRequest exportRequest = DataExportRequest.builder()
                .user(testUser)
                .requestType("profile_export")
                .dataCategories(Set.of("profile", "preferences"))
                .format("CSV")
                .status("COMPLETED")
                .completedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();
        DataExportRequest savedRequest = entityManager.persistAndFlush(exportRequest);

        // When - User downloads the export
        savedRequest.setDownloadedAt(Instant.now());
        savedRequest.setDownloadCount(1);
        savedRequest.setLastDownloadIp("192.168.1.100");
        DataExportRequest downloadedRequest = entityManager.persistAndFlush(savedRequest);

        // Then
        assertThat(downloadedRequest.getDownloadedAt()).isNotNull();
        assertThat(downloadedRequest.getDownloadCount()).isEqualTo(1);
        assertThat(downloadedRequest.getLastDownloadIp()).isEqualTo("192.168.1.100");
    }

    @Test
    void shouldSupportMultipleFormats() {
        // Given - JSON export
        DataExportRequest jsonRequest = DataExportRequest.builder()
                .user(testUser)
                .requestType("json_export")
                .dataCategories(Set.of("profile"))
                .format("JSON")
                .build();

        // And - CSV export
        DataExportRequest csvRequest = DataExportRequest.builder()
                .user(testUser)
                .requestType("csv_export")
                .dataCategories(Set.of("activities"))
                .format("CSV")
                .build();

        // And - XML export
        DataExportRequest xmlRequest = DataExportRequest.builder()
                .user(testUser)
                .requestType("xml_export")
                .dataCategories(Set.of("audit_logs"))
                .format("XML")
                .build();

        // When
        DataExportRequest savedJson = entityManager.persistAndFlush(jsonRequest);
        DataExportRequest savedCsv = entityManager.persistAndFlush(csvRequest);
        DataExportRequest savedXml = entityManager.persistAndFlush(xmlRequest);

        // Then
        assertThat(savedJson.getFormat()).isEqualTo("JSON");
        assertThat(savedCsv.getFormat()).isEqualTo("CSV");
        assertThat(savedXml.getFormat()).isEqualTo("XML");
    }

    @Test
    void shouldCalculateProcessingDuration() {
        // Given
        DataExportRequest exportRequest = DataExportRequest.builder()
                .user(testUser)
                .requestType("timed_export")
                .dataCategories(Set.of("all"))
                .format("JSON")
                .build();
        DataExportRequest savedRequest = entityManager.persistAndFlush(exportRequest);

        // When - Set processing times
        Instant startTime = Instant.now().minus(30, ChronoUnit.MINUTES);
        Instant endTime = Instant.now();
        savedRequest.setProcessingStartedAt(startTime);
        savedRequest.setCompletedAt(endTime);
        savedRequest.setStatus("COMPLETED");
        DataExportRequest completedRequest = entityManager.persistAndFlush(savedRequest);

        // Then
        assertThat(completedRequest.getProcessingDurationMinutes()).isEqualTo(30);
    }

    @Test
    void shouldValidateRetentionPeriod() {
        // Given - Request with 30-day retention
        DataExportRequest exportRequest = DataExportRequest.builder()
                .user(testUser)
                .requestType("retention_test")
                .dataCategories(Set.of("profile"))
                .format("JSON")
                .retentionDays(30)
                .build();
        DataExportRequest savedRequest = entityManager.persistAndFlush(exportRequest);

        // When - Check if should be retained
        savedRequest.setCreatedAt(Instant.now().minus(35, ChronoUnit.DAYS)); // 35 days old
        DataExportRequest oldRequest = entityManager.persistAndFlush(savedRequest);

        // Then
        assertThat(oldRequest.shouldBeRetained()).isFalse();
        assertThat(oldRequest.getRetentionDays()).isEqualTo(30);
    }
}