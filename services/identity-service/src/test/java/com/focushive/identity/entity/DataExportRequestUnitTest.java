package com.focushive.identity.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for DataExportRequest entity covering all business logic,
 * builder patterns, status transitions, and edge cases for GDPR compliance.
 */
@DisplayName("DataExportRequest Entity Unit Tests")
class DataExportRequestUnitTest {

    private User testUser;
    private Set<String> testDataCategories;
    private Map<String, String> testMetadata;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .build();

        testDataCategories = new HashSet<>(Arrays.asList("profile", "preferences", "activities"));
        
        testMetadata = new HashMap<>();
        testMetadata.put("purpose", "gdpr_compliance");
        testMetadata.put("requested_by", "user_portal");
    }

    @Nested
    @DisplayName("Builder Pattern Tests")
    class BuilderPatternTests {

        @Test
        @DisplayName("Should create DataExportRequest using builder with all fields")
        void shouldCreateDataExportRequestUsingBuilderWithAllFields() {
            // Given
            UUID requestId = UUID.randomUUID();
            Instant now = Instant.now();
            Instant expiry = now.plus(30, ChronoUnit.DAYS);

            // When
            DataExportRequest request = DataExportRequest.builder()
                    .id(requestId)
                    .user(testUser)
                    .requestType("full_export")
                    .format("JSON")
                    .dataCategories(testDataCategories)
                    .status("PENDING")
                    .reason("GDPR data portability request under Article 20")
                    .verificationMethod("email_verification")
                    .processingStartedAt(now)
                    .completedAt(now.plus(1, ChronoUnit.HOURS))
                    .failedAt(null)
                    .exportFilePath("/exports/user-data-123.json")
                    .fileSizeBytes(1024L * 1024L) // 1MB
                    .recordCount(1500)
                    .expiresAt(expiry)
                    .errorMessage(null)
                    .errorCode(null)
                    .downloadedAt(null)
                    .downloadCount(0)
                    .lastDownloadIp(null)
                    .metadata(testMetadata)
                    .retentionDays(30)
                    .fileChecksum("sha256:abc123...")
                    .encrypted(true)
                    .encryptionKeyId("key-123")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            // Then
            assertThat(request.getId()).isEqualTo(requestId);
            assertThat(request.getUser()).isEqualTo(testUser);
            assertThat(request.getRequestType()).isEqualTo("full_export");
            assertThat(request.getFormat()).isEqualTo("JSON");
            assertThat(request.getDataCategories()).isEqualTo(testDataCategories);
            assertThat(request.getStatus()).isEqualTo("PENDING");
            assertThat(request.getReason()).contains("GDPR data portability");
            assertThat(request.getVerificationMethod()).isEqualTo("email_verification");
            assertThat(request.getFileSizeBytes()).isEqualTo(1024L * 1024L);
            assertThat(request.getRecordCount()).isEqualTo(1500);
            assertThat(request.getRetentionDays()).isEqualTo(30);
            assertThat(request.isEncrypted()).isTrue();
            assertThat(request.getEncryptionKeyId()).isEqualTo("key-123");
        }

        @Test
        @DisplayName("Should create DataExportRequest with default values")
        void shouldCreateDataExportRequestWithDefaultValues() {
            // When
            DataExportRequest request = DataExportRequest.builder()
                    .user(testUser)
                    .requestType("partial_export")
                    .build();

            // Then
            assertThat(request.getFormat()).isEqualTo("JSON");
            assertThat(request.getDataCategories()).isNotNull().isEmpty();
            assertThat(request.getStatus()).isEqualTo("PENDING");
            assertThat(request.getDownloadCount()).isEqualTo(0);
            assertThat(request.getMetadata()).isNotNull().isEmpty();
            assertThat(request.getRetentionDays()).isEqualTo(30);
            assertThat(request.isEncrypted()).isTrue();
        }

        @Test
        @DisplayName("Should create DataExportRequest using no-args constructor")
        void shouldCreateDataExportRequestUsingNoArgsConstructor() {
            // When
            DataExportRequest request = new DataExportRequest();

            // Then
            assertThat(request.getId()).isNull();
            assertThat(request.getUser()).isNull();
            assertThat(request.getRequestType()).isNull();
            assertThat(request.getDataCategories()).isNotNull();
            assertThat(request.getMetadata()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Status Management Tests")
    class StatusManagementTests {

        @Test
        @DisplayName("Should mark export as started correctly")
        void shouldMarkExportAsStartedCorrectly() {
            // Given
            DataExportRequest request = createTestRequest();
            Instant beforeStart = Instant.now().minusSeconds(1);

            // When
            request.markAsStarted();

            // Then
            assertThat(request.getStatus()).isEqualTo("PROCESSING");
            assertThat(request.getProcessingStartedAt()).isNotNull();
            assertThat(request.getProcessingStartedAt()).isAfter(beforeStart);
        }

        @Test
        @DisplayName("Should mark export as completed correctly")
        void shouldMarkExportAsCompletedCorrectly() {
            // Given
            DataExportRequest request = createTestRequest();
            request.markAsStarted();
            Instant beforeCompletion = Instant.now().minusSeconds(1);

            // When
            request.markAsCompleted("/exports/user-data.json", 2048L, 500);

            // Then
            assertThat(request.getStatus()).isEqualTo("COMPLETED");
            assertThat(request.getCompletedAt()).isNotNull();
            assertThat(request.getCompletedAt()).isAfter(beforeCompletion);
            assertThat(request.getExportFilePath()).isEqualTo("/exports/user-data.json");
            assertThat(request.getFileSizeBytes()).isEqualTo(2048L);
            assertThat(request.getRecordCount()).isEqualTo(500);
            assertThat(request.getExpiresAt()).isNotNull();
        }

        @Test
        @DisplayName("Should set expiration when marking as completed with retention policy")
        void shouldSetExpirationWhenMarkingAsCompletedWithRetentionPolicy() {
            // Given
            DataExportRequest request = createTestRequest();
            request.setRetentionDays(7);
            Instant beforeCompletion = Instant.now();

            // When
            request.markAsCompleted("/exports/test.json", 1024L, 100);

            // Then
            assertThat(request.getExpiresAt()).isNotNull();
            assertThat(request.getExpiresAt()).isAfter(beforeCompletion.plus(6, ChronoUnit.DAYS));
            assertThat(request.getExpiresAt()).isBefore(beforeCompletion.plus(8, ChronoUnit.DAYS));
        }

        @Test
        @DisplayName("Should not override existing expiration when marking as completed")
        void shouldNotOverrideExistingExpirationWhenMarkingAsCompleted() {
            // Given
            DataExportRequest request = createTestRequest();
            Instant customExpiry = Instant.now().plus(60, ChronoUnit.DAYS);
            request.setExpiresAt(customExpiry);

            // When
            request.markAsCompleted("/exports/test.json", 1024L, 100);

            // Then
            assertThat(request.getExpiresAt()).isEqualTo(customExpiry);
        }

        @Test
        @DisplayName("Should mark export as failed correctly")
        void shouldMarkExportAsFailedCorrectly() {
            // Given
            DataExportRequest request = createTestRequest();
            request.markAsStarted();
            Instant beforeFailure = Instant.now().minusSeconds(1);

            // When
            request.markAsFailed("DATA_ERROR", "Failed to access user data");

            // Then
            assertThat(request.getStatus()).isEqualTo("FAILED");
            assertThat(request.getFailedAt()).isNotNull();
            assertThat(request.getFailedAt()).isAfter(beforeFailure);
            assertThat(request.getErrorCode()).isEqualTo("DATA_ERROR");
            assertThat(request.getErrorMessage()).isEqualTo("Failed to access user data");
        }
    }

    @Nested
    @DisplayName("Download Tracking Tests")
    class DownloadTrackingTests {

        @Test
        @DisplayName("Should record download correctly")
        void shouldRecordDownloadCorrectly() {
            // Given
            DataExportRequest request = createTestRequest();
            request.markAsCompleted("/exports/test.json", 1024L, 100);
            Instant beforeDownload = Instant.now().minusSeconds(1);
            String ipAddress = "192.168.1.100";

            // When
            request.recordDownload(ipAddress);

            // Then
            assertThat(request.getDownloadedAt()).isNotNull();
            assertThat(request.getDownloadedAt()).isAfter(beforeDownload);
            assertThat(request.getDownloadCount()).isEqualTo(1);
            assertThat(request.getLastDownloadIp()).isEqualTo(ipAddress);
        }

        @Test
        @DisplayName("Should increment download count on multiple downloads")
        void shouldIncrementDownloadCountOnMultipleDownloads() {
            // Given
            DataExportRequest request = createTestRequest();
            request.markAsCompleted("/exports/test.json", 1024L, 100);

            // When
            request.recordDownload("192.168.1.100");
            request.recordDownload("192.168.1.101");
            request.recordDownload("192.168.1.102");

            // Then
            assertThat(request.getDownloadCount()).isEqualTo(3);
            assertThat(request.getLastDownloadIp()).isEqualTo("192.168.1.102");
        }

        @Test
        @DisplayName("Should handle null download count correctly")
        void shouldHandleNullDownloadCountCorrectly() {
            // Given
            DataExportRequest request = createTestRequest();
            request.setDownloadCount(null);

            // When
            request.recordDownload("192.168.1.100");

            // Then
            assertThat(request.getDownloadCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should check if export has been downloaded")
        void shouldCheckIfExportHasBeenDownloaded() {
            // Given
            DataExportRequest request = createTestRequest();

            // Initially not downloaded
            assertThat(request.hasBeenDownloaded()).isFalse();

            // When
            request.recordDownload("192.168.1.100");

            // Then
            assertThat(request.hasBeenDownloaded()).isTrue();
        }
    }

    @Nested
    @DisplayName("Metadata Management Tests")
    class MetadataManagementTests {

        @Test
        @DisplayName("Should add metadata correctly")
        void shouldAddMetadataCorrectly() {
            // Given
            DataExportRequest request = createTestRequest();

            // When
            request.addMetadata("purpose", "gdpr_compliance");
            request.addMetadata("priority", "high");

            // Then
            assertThat(request.getMetadata("purpose")).isEqualTo("gdpr_compliance");
            assertThat(request.getMetadata("priority")).isEqualTo("high");
        }

        @Test
        @DisplayName("Should handle null metadata map when adding")
        void shouldHandleNullMetadataMapWhenAdding() {
            // Given
            DataExportRequest request = createTestRequest();
            request.setMetadata(null);

            // When
            request.addMetadata("test", "value");

            // Then
            assertThat(request.getMetadata()).isNotNull();
            assertThat(request.getMetadata("test")).isEqualTo("value");
        }

        @Test
        @DisplayName("Should return null for non-existent metadata key")
        void shouldReturnNullForNonExistentMetadataKey() {
            // Given
            DataExportRequest request = createTestRequest();

            // When & Then
            assertThat(request.getMetadata("non_existent_key")).isNull();
        }

        @Test
        @DisplayName("Should handle null metadata map when getting value")
        void shouldHandleNullMetadataMapWhenGettingValue() {
            // Given
            DataExportRequest request = createTestRequest();
            request.setMetadata(null);

            // When & Then
            assertThat(request.getMetadata("any_key")).isNull();
        }
    }

    @Nested
    @DisplayName("Processing Duration Tests")
    class ProcessingDurationTests {

        @Test
        @DisplayName("Should calculate processing duration for completed export")
        void shouldCalculateProcessingDurationForCompletedExport() {
            // Given
            DataExportRequest request = createTestRequest();
            Instant startTime = Instant.now().minus(30, ChronoUnit.MINUTES);
            Instant endTime = startTime.plus(25, ChronoUnit.MINUTES);
            
            request.setProcessingStartedAt(startTime);
            request.setCompletedAt(endTime);

            // When
            Long duration = request.getProcessingDurationMinutes();

            // Then
            assertThat(duration).isEqualTo(25L);
        }

        @Test
        @DisplayName("Should calculate processing duration for failed export")
        void shouldCalculateProcessingDurationForFailedExport() {
            // Given
            DataExportRequest request = createTestRequest();
            Instant startTime = Instant.now().minus(15, ChronoUnit.MINUTES);
            Instant failTime = startTime.plus(10, ChronoUnit.MINUTES);
            
            request.setProcessingStartedAt(startTime);
            request.setFailedAt(failTime);

            // When
            Long duration = request.getProcessingDurationMinutes();

            // Then
            assertThat(duration).isEqualTo(10L);
        }

        @Test
        @DisplayName("Should calculate processing duration for ongoing export")
        void shouldCalculateProcessingDurationForOngoingExport() {
            // Given
            DataExportRequest request = createTestRequest();
            Instant startTime = Instant.now().minus(5, ChronoUnit.MINUTES);
            request.setProcessingStartedAt(startTime);

            // When
            Long duration = request.getProcessingDurationMinutes();

            // Then
            assertThat(duration).isGreaterThanOrEqualTo(4L);
            assertThat(duration).isLessThanOrEqualTo(6L);
        }

        @Test
        @DisplayName("Should return null when processing not started")
        void shouldReturnNullWhenProcessingNotStarted() {
            // Given
            DataExportRequest request = createTestRequest();

            // When
            Long duration = request.getProcessingDurationMinutes();

            // Then
            assertThat(duration).isNull();
        }
    }

    @Nested
    @DisplayName("Expiration and Retention Tests")
    class ExpirationAndRetentionTests {

        @Test
        @DisplayName("Should check if export is expired")
        void shouldCheckIfExportIsExpired() {
            // Given
            DataExportRequest request = createTestRequest();

            // Not expired when no expiry set
            assertThat(request.isExpired()).isFalse();

            // Not expired when expiry is in future
            request.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
            assertThat(request.isExpired()).isFalse();

            // Expired when expiry is in past
            request.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
            assertThat(request.isExpired()).isTrue();
        }

        @Test
        @DisplayName("Should check if export is ready for download")
        void shouldCheckIfExportIsReadyForDownload() {
            // Given
            DataExportRequest request = createTestRequest();
            request.setStatus("COMPLETED");
            request.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));

            // When & Then
            assertThat(request.isReadyForDownload()).isTrue();

            // When expired
            request.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
            assertThat(request.isReadyForDownload()).isFalse();

            // When not completed
            request.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
            request.setStatus("PROCESSING");
            assertThat(request.isReadyForDownload()).isFalse();
        }

        @Test
        @DisplayName("Should calculate days until deletion")
        void shouldCalculateDaysUntilDeletion() {
            // Given
            DataExportRequest request = createTestRequest();

            // No expiry set
            assertThat(request.getDaysUntilDeletion()).isNull();

            // 5 days until deletion
            request.setExpiresAt(Instant.now().plus(5, ChronoUnit.DAYS));
            assertThat(request.getDaysUntilDeletion()).isEqualTo(5L);

            // Already expired (should return 0)
            request.setExpiresAt(Instant.now().minus(2, ChronoUnit.DAYS));
            assertThat(request.getDaysUntilDeletion()).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should check retention policy")
        void shouldCheckRetentionPolicy() {
            // Given
            DataExportRequest request = createTestRequest();
            request.setCreatedAt(Instant.now().minus(20, ChronoUnit.DAYS));
            request.setRetentionDays(30);

            // Should be retained (created 20 days ago, retention is 30 days)
            assertThat(request.shouldBeRetained()).isTrue();

            // Should not be retained (created 40 days ago)
            request.setCreatedAt(Instant.now().minus(40, ChronoUnit.DAYS));
            assertThat(request.shouldBeRetained()).isFalse();

            // No retention policy (should be retained)
            request.setRetentionDays(null);
            assertThat(request.shouldBeRetained()).isTrue();
        }
    }

    @Nested
    @DisplayName("File Size and Formatting Tests")
    class FileSizeAndFormattingTests {

        @Test
        @DisplayName("Should format file size in bytes")
        void shouldFormatFileSizeInBytes() {
            // Given
            DataExportRequest request = createTestRequest();
            request.setFileSizeBytes(512L);

            // When & Then
            assertThat(request.getFormattedFileSize()).isEqualTo("512 B");
        }

        @Test
        @DisplayName("Should format file size in KB")
        void shouldFormatFileSizeInKB() {
            // Given
            DataExportRequest request = createTestRequest();
            request.setFileSizeBytes(1536L); // 1.5 KB

            // When & Then
            assertThat(request.getFormattedFileSize()).isEqualTo("1.5 KB");
        }

        @Test
        @DisplayName("Should format file size in MB")
        void shouldFormatFileSizeInMB() {
            // Given
            DataExportRequest request = createTestRequest();
            request.setFileSizeBytes(2621440L); // 2.5 MB

            // When & Then
            assertThat(request.getFormattedFileSize()).isEqualTo("2.5 MB");
        }

        @Test
        @DisplayName("Should format file size in GB")
        void shouldFormatFileSizeInGB() {
            // Given
            DataExportRequest request = createTestRequest();
            request.setFileSizeBytes(3221225472L); // 3.0 GB

            // When & Then
            assertThat(request.getFormattedFileSize()).isEqualTo("3.0 GB");
        }

        @Test
        @DisplayName("Should handle unknown file size")
        void shouldHandleUnknownFileSize() {
            // Given
            DataExportRequest request = createTestRequest();
            request.setFileSizeBytes(null);

            // When & Then
            assertThat(request.getFormattedFileSize()).isEqualTo("Unknown");
        }
    }

    @Nested
    @DisplayName("GDPR Compliance Tests")
    class GDPRComplianceTests {

        @Test
        @DisplayName("Should identify GDPR requests by keyword")
        void shouldIdentifyGDPRRequestsByKeyword() {
            // Given
            DataExportRequest request1 = createTestRequest();
            request1.setReason("GDPR data portability request");

            DataExportRequest request2 = createTestRequest();
            request2.setReason("Need my data under Article 20");

            DataExportRequest request3 = createTestRequest();
            request3.setReason("Just want to see my data");

            // When & Then
            assertThat(request1.isGDPRRequest()).isTrue();
            assertThat(request2.isGDPRRequest()).isTrue();
            assertThat(request3.isGDPRRequest()).isFalse();
        }

        @Test
        @DisplayName("Should handle null reason for GDPR check")
        void shouldHandleNullReasonForGDPRCheck() {
            // Given
            DataExportRequest request = createTestRequest();
            request.setReason(null);

            // When & Then
            assertThat(request.isGDPRRequest()).isFalse();
        }
    }

    @Nested
    @DisplayName("Data Category Validation Tests")
    class DataCategoryValidationTests {

        @Test
        @DisplayName("Should validate supported data categories")
        void shouldValidateSupportedDataCategories() {
            // Given
            DataExportRequest request = createTestRequest();
            request.setDataCategories(Set.of("profile", "preferences", "activities"));

            // When & Then
            assertThat(request.hasValidDataCategories()).isTrue();
        }

        @Test
        @DisplayName("Should reject unsupported data categories")
        void shouldRejectUnsupportedDataCategories() {
            // Given
            DataExportRequest request = createTestRequest();
            request.setDataCategories(Set.of("profile", "invalid_category", "preferences"));

            // When & Then
            assertThat(request.hasValidDataCategories()).isFalse();
        }

        @Test
        @DisplayName("Should handle empty data categories")
        void shouldHandleEmptyDataCategories() {
            // Given
            DataExportRequest request = createTestRequest();
            request.setDataCategories(new HashSet<>());

            // When & Then
            assertThat(request.hasValidDataCategories()).isFalse();
        }

        @Test
        @DisplayName("Should handle null data categories")
        void shouldHandleNullDataCategories() {
            // Given
            DataExportRequest request = createTestRequest();
            request.setDataCategories(null);

            // When & Then
            assertThat(request.hasValidDataCategories()).isFalse();
        }
    }

    @Nested
    @DisplayName("Processing Time Estimation Tests")
    class ProcessingTimeEstimationTests {

        @Test
        @DisplayName("Should estimate processing time for full export")
        void shouldEstimateProcessingTimeForFullExport() {
            // Given
            DataExportRequest request = createTestRequest();
            request.setDataCategories(Set.of("all"));

            // When & Then
            assertThat(request.getEstimatedProcessingMinutes()).isEqualTo(60L);
        }

        @Test
        @DisplayName("Should estimate processing time for specific categories")
        void shouldEstimateProcessingTimeForSpecificCategories() {
            // Given
            DataExportRequest request = createTestRequest();
            request.setDataCategories(Set.of("profile", "preferences")); // 2 + 2 = 4 minutes

            // When & Then
            assertThat(request.getEstimatedProcessingMinutes()).isEqualTo(4L);
        }

        @Test
        @DisplayName("Should estimate processing time for heavy categories")
        void shouldEstimateProcessingTimeForHeavyCategories() {
            // Given
            DataExportRequest request = createTestRequest();
            request.setDataCategories(Set.of("audit_logs", "activities")); // 15 + 15 = 30 minutes

            // When & Then
            assertThat(request.getEstimatedProcessingMinutes()).isEqualTo(30L);
        }

        @Test
        @DisplayName("Should have minimum processing time estimate")
        void shouldHaveMinimumProcessingTimeEstimate() {
            // Given
            DataExportRequest request = createTestRequest();
            request.setDataCategories(Set.of("profile")); // 2 minutes, but minimum is 5

            // When & Then
            assertThat(request.getEstimatedProcessingMinutes()).isEqualTo(5L);
        }

        @Test
        @DisplayName("Should handle empty data categories for estimation")
        void shouldHandleEmptyDataCategoriesForEstimation() {
            // Given
            DataExportRequest request = createTestRequest();
            request.setDataCategories(new HashSet<>());

            // When & Then
            assertThat(request.getEstimatedProcessingMinutes()).isEqualTo(5L);
        }

        @Test
        @DisplayName("Should handle null data categories for estimation")
        void shouldHandleNullDataCategoriesForEstimation() {
            // Given
            DataExportRequest request = createTestRequest();
            request.setDataCategories(null);

            // When & Then
            assertThat(request.getEstimatedProcessingMinutes()).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should test equality based on ID")
        void shouldTestEqualityBasedOnId() {
            // Given
            UUID sameId = UUID.randomUUID();
            DataExportRequest request1 = DataExportRequest.builder()
                    .id(sameId)
                    .user(testUser)
                    .requestType("full_export")
                    .build();

            DataExportRequest request2 = DataExportRequest.builder()
                    .id(sameId)
                    .user(testUser)
                    .requestType("partial_export") // Different type
                    .build();

            // Then
            assertThat(request1).isEqualTo(request2);
            assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
        }

        @Test
        @DisplayName("Should test inequality with different IDs")
        void shouldTestInequalityWithDifferentIds() {
            // Given
            DataExportRequest request1 = DataExportRequest.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .requestType("full_export")
                    .build();

            DataExportRequest request2 = DataExportRequest.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .requestType("full_export")
                    .build();

            // Then
            assertThat(request1).isNotEqualTo(request2);
            assertThat(request1.hashCode()).isNotEqualTo(request2.hashCode());
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should exclude user from toString")
        void shouldExcludeUserFromToString() {
            // Given
            DataExportRequest request = createTestRequest();

            // When
            String toString = request.toString();

            // Then
            assertThat(toString).isNotNull();
            assertThat(toString).contains("requestType");
            assertThat(toString).contains("status");
            assertThat(toString).doesNotContain("user=");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Null Handling Tests")
    class EdgeCasesAndNullHandlingTests {

        @Test
        @DisplayName("Should handle null values gracefully in business methods")
        void shouldHandleNullValuesGracefullyInBusinessMethods() {
            // Given
            DataExportRequest request = new DataExportRequest();

            // When & Then - should not throw exceptions
            assertThatCode(() -> request.isExpired()).doesNotThrowAnyException();
            assertThatCode(() -> request.isReadyForDownload()).doesNotThrowAnyException();
            assertThatCode(() -> request.getProcessingDurationMinutes()).doesNotThrowAnyException();
            assertThatCode(() -> request.shouldBeRetained()).doesNotThrowAnyException();
            assertThatCode(() -> request.getDaysUntilDeletion()).doesNotThrowAnyException();
            assertThatCode(() -> request.getFormattedFileSize()).doesNotThrowAnyException();
            assertThatCode(() -> request.isGDPRRequest()).doesNotThrowAnyException();
            assertThatCode(() -> request.hasValidDataCategories()).doesNotThrowAnyException();
            assertThatCode(() -> request.getEstimatedProcessingMinutes()).doesNotThrowAnyException();
            assertThatCode(() -> request.hasBeenDownloaded()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle special characters in string fields")
        void shouldHandleSpecialCharactersInStringFields() {
            // Given
            String specialChars = "Special!@#$%^&*()";
            String unicodeChars = "üñïçødé 测试";

            // When
            DataExportRequest request = DataExportRequest.builder()
                    .user(testUser)
                    .requestType(specialChars)
                    .reason(unicodeChars)
                    .verificationMethod("email" + specialChars)
                    .errorMessage(unicodeChars + " error")
                    .lastDownloadIp("192.168.1.100")
                    .build();

            // Then
            assertThat(request.getRequestType()).contains(specialChars);
            assertThat(request.getReason()).isEqualTo(unicodeChars);
            assertThat(request.getVerificationMethod()).contains(specialChars);
            assertThat(request.getErrorMessage()).contains(unicodeChars);
        }

        @Test
        @DisplayName("Should handle very large file sizes")
        void shouldHandleVeryLargeFileSizes() {
            // Given
            DataExportRequest request = createTestRequest();
            request.setFileSizeBytes(5_368_709_120L); // 5 GB

            // When & Then
            assertThat(request.getFormattedFileSize()).isEqualTo("5.0 GB");
        }

        @Test
        @DisplayName("Should handle extreme retention periods")
        void shouldHandleExtremeRetentionPeriods() {
            // Given
            DataExportRequest request = createTestRequest();
            request.setCreatedAt(Instant.now().minus(365, ChronoUnit.DAYS));

            // Very long retention (should be retained)
            request.setRetentionDays(1000);
            assertThat(request.shouldBeRetained()).isTrue();

            // Very short retention (should not be retained)
            request.setRetentionDays(1);
            assertThat(request.shouldBeRetained()).isFalse();
        }
    }

    private DataExportRequest createTestRequest() {
        return DataExportRequest.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .requestType("partial_export")
                .format("JSON")
                .dataCategories(testDataCategories)
                .reason("User requested data export")
                .build();
    }
}