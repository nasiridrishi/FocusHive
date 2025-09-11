package com.focushive.identity.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for DataExportResponse covering builder patterns,
 * serialization/deserialization, equality, and edge cases.
 */
@DisplayName("DataExportResponse Unit Tests")
class DataExportResponseUnitTest {

    private ObjectMapper objectMapper;
    private UUID testExportId;
    private Instant testInstant;
    private Instant testEstimatedCompletion;
    private Instant testExpiresAt;
    private Instant testRequestedAt;
    private Instant testCompletedAt;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        testExportId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        testInstant = Instant.parse("2023-01-15T10:30:00Z");
        testEstimatedCompletion = testInstant.plus(30, ChronoUnit.MINUTES);
        testExpiresAt = testInstant.plus(7, ChronoUnit.DAYS);
        testRequestedAt = testInstant;
        testCompletedAt = testInstant.plus(25, ChronoUnit.MINUTES);
    }

    @Test
    @DisplayName("Should create DataExportResponse using builder with all fields")
    void shouldCreateDataExportResponseUsingBuilderWithAllFields() {
        // Given & When
        DataExportResponse response = DataExportResponse.builder()
                .exportId(testExportId)
                .status("completed")
                .progress(100)
                .estimatedCompletion(testEstimatedCompletion)
                .downloadUrl("https://identity.focushive.com/api/v1/privacy/data/export/550e8400-e29b-41d4-a716-446655440000/download")
                .fileSizeBytes(1048576L)
                .expiresAt(testExpiresAt)
                .requestedAt(testRequestedAt)
                .completedAt(testCompletedAt)
                .errorMessage(null)
                .build();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getExportId()).isEqualTo(testExportId);
        assertThat(response.getStatus()).isEqualTo("completed");
        assertThat(response.getProgress()).isEqualTo(100);
        assertThat(response.getEstimatedCompletion()).isEqualTo(testEstimatedCompletion);
        assertThat(response.getDownloadUrl()).isEqualTo("https://identity.focushive.com/api/v1/privacy/data/export/550e8400-e29b-41d4-a716-446655440000/download");
        assertThat(response.getFileSizeBytes()).isEqualTo(1048576L);
        assertThat(response.getExpiresAt()).isEqualTo(testExpiresAt);
        assertThat(response.getRequestedAt()).isEqualTo(testRequestedAt);
        assertThat(response.getCompletedAt()).isEqualTo(testCompletedAt);
        assertThat(response.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("Should create DataExportResponse with minimal required fields")
    void shouldCreateDataExportResponseWithMinimalFields() {
        // Given & When
        DataExportResponse response = DataExportResponse.builder()
                .exportId(testExportId)
                .status("requested")
                .progress(0)
                .build();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getExportId()).isEqualTo(testExportId);
        assertThat(response.getStatus()).isEqualTo("requested");
        assertThat(response.getProgress()).isEqualTo(0);
        assertThat(response.getEstimatedCompletion()).isNull();
        assertThat(response.getDownloadUrl()).isNull();
        assertThat(response.getFileSizeBytes()).isNull();
        assertThat(response.getExpiresAt()).isNull();
        assertThat(response.getRequestedAt()).isNull();
        assertThat(response.getCompletedAt()).isNull();
        assertThat(response.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("Should create DataExportResponse using no-args constructor and setters")
    void shouldCreateDataExportResponseUsingNoArgsConstructorAndSetters() {
        // Given
        DataExportResponse response = new DataExportResponse();

        // When
        response.setExportId(testExportId);
        response.setStatus("processing");
        response.setProgress(45);
        response.setEstimatedCompletion(testEstimatedCompletion);
        response.setDownloadUrl("https://identity.focushive.com/api/v1/privacy/data/export/550e8400-e29b-41d4-a716-446655440000/download");
        response.setFileSizeBytes(2097152L);
        response.setExpiresAt(testExpiresAt);
        response.setRequestedAt(testRequestedAt);
        response.setCompletedAt(null);
        response.setErrorMessage(null);

        // Then
        assertThat(response.getExportId()).isEqualTo(testExportId);
        assertThat(response.getStatus()).isEqualTo("processing");
        assertThat(response.getProgress()).isEqualTo(45);
        assertThat(response.getEstimatedCompletion()).isEqualTo(testEstimatedCompletion);
        assertThat(response.getDownloadUrl()).isEqualTo("https://identity.focushive.com/api/v1/privacy/data/export/550e8400-e29b-41d4-a716-446655440000/download");
        assertThat(response.getFileSizeBytes()).isEqualTo(2097152L);
        assertThat(response.getExpiresAt()).isEqualTo(testExpiresAt);
        assertThat(response.getRequestedAt()).isEqualTo(testRequestedAt);
        assertThat(response.getCompletedAt()).isNull();
        assertThat(response.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("Should create DataExportResponse using all-args constructor")
    void shouldCreateDataExportResponseUsingAllArgsConstructor() {
        // Given & When
        DataExportResponse response = new DataExportResponse(
                testExportId,
                "failed",
                50,
                testEstimatedCompletion,
                null,
                null,
                testExpiresAt,
                testRequestedAt,
                null,
                "Processing error occurred"
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getExportId()).isEqualTo(testExportId);
        assertThat(response.getStatus()).isEqualTo("failed");
        assertThat(response.getProgress()).isEqualTo(50);
        assertThat(response.getEstimatedCompletion()).isEqualTo(testEstimatedCompletion);
        assertThat(response.getDownloadUrl()).isNull();
        assertThat(response.getFileSizeBytes()).isNull();
        assertThat(response.getExpiresAt()).isEqualTo(testExpiresAt);
        assertThat(response.getRequestedAt()).isEqualTo(testRequestedAt);
        assertThat(response.getCompletedAt()).isNull();
        assertThat(response.getErrorMessage()).isEqualTo("Processing error occurred");
    }

    @Test
    @DisplayName("Should serialize DataExportResponse to JSON correctly")
    void shouldSerializeDataExportResponseToJsonCorrectly() throws JsonProcessingException {
        // Given
        DataExportResponse response = DataExportResponse.builder()
                .exportId(testExportId)
                .status("completed")
                .progress(100)
                .estimatedCompletion(testEstimatedCompletion)
                .downloadUrl("https://identity.focushive.com/api/v1/privacy/data/export/550e8400-e29b-41d4-a716-446655440000/download")
                .fileSizeBytes(1048576L)
                .expiresAt(testExpiresAt)
                .requestedAt(testRequestedAt)
                .completedAt(testCompletedAt)
                .errorMessage(null)
                .build();

        // When
        String json = objectMapper.writeValueAsString(response);

        // Then
        assertThat(json).isNotNull();
        assertThat(json).contains("\"exportId\":\"550e8400-e29b-41d4-a716-446655440000\"");
        assertThat(json).contains("\"status\":\"completed\"");
        assertThat(json).contains("\"progress\":100");
        assertThat(json).contains("\"estimatedCompletion\":");
        assertThat(json).contains("\"downloadUrl\":\"https://identity.focushive.com/api/v1/privacy/data/export/550e8400-e29b-41d4-a716-446655440000/download\"");
        assertThat(json).contains("\"fileSizeBytes\":1048576");
        assertThat(json).contains("\"expiresAt\":");
        assertThat(json).contains("\"requestedAt\":");
        assertThat(json).contains("\"completedAt\":");
        assertThat(json).contains("\"errorMessage\":null");
    }

    @Test
    @DisplayName("Should deserialize JSON to DataExportResponse correctly")
    void shouldDeserializeJsonToDataExportResponseCorrectly() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "exportId": "550e8400-e29b-41d4-a716-446655440000",
                    "status": "processing",
                    "progress": 75,
                    "estimatedCompletion": "2023-01-15T11:00:00Z",
                    "downloadUrl": null,
                    "fileSizeBytes": null,
                    "expiresAt": "2023-01-22T10:30:00Z",
                    "requestedAt": "2023-01-15T10:30:00Z",
                    "completedAt": null,
                    "errorMessage": null
                }
                """;

        // When
        DataExportResponse response = objectMapper.readValue(json, DataExportResponse.class);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getExportId()).isEqualTo(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        assertThat(response.getStatus()).isEqualTo("processing");
        assertThat(response.getProgress()).isEqualTo(75);
        assertThat(response.getEstimatedCompletion()).isEqualTo(Instant.parse("2023-01-15T11:00:00Z"));
        assertThat(response.getDownloadUrl()).isNull();
        assertThat(response.getFileSizeBytes()).isNull();
        assertThat(response.getExpiresAt()).isEqualTo(Instant.parse("2023-01-22T10:30:00Z"));
        assertThat(response.getRequestedAt()).isEqualTo(Instant.parse("2023-01-15T10:30:00Z"));
        assertThat(response.getCompletedAt()).isNull();
        assertThat(response.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("Should handle null values in serialization")
    void shouldHandleNullValuesInSerialization() throws JsonProcessingException {
        // Given
        DataExportResponse response = DataExportResponse.builder()
                .exportId(testExportId)
                .status("requested")
                .progress(0)
                .estimatedCompletion(null)
                .downloadUrl(null)
                .fileSizeBytes(null)
                .expiresAt(null)
                .requestedAt(null)
                .completedAt(null)
                .errorMessage(null)
                .build();

        // When
        String json = objectMapper.writeValueAsString(response);

        // Then
        assertThat(json).isNotNull();
        assertThat(json).contains("\"estimatedCompletion\":null");
        assertThat(json).contains("\"downloadUrl\":null");
        assertThat(json).contains("\"fileSizeBytes\":null");
        assertThat(json).contains("\"expiresAt\":null");
        assertThat(json).contains("\"requestedAt\":null");
        assertThat(json).contains("\"completedAt\":null");
        assertThat(json).contains("\"errorMessage\":null");
    }

    @Test
    @DisplayName("Should test equality and hashCode with same values")
    void shouldTestEqualityAndHashCodeWithSameValues() {
        // Given
        DataExportResponse response1 = DataExportResponse.builder()
                .exportId(testExportId)
                .status("completed")
                .progress(100)
                .estimatedCompletion(testEstimatedCompletion)
                .downloadUrl("https://identity.focushive.com/api/v1/privacy/data/export/550e8400-e29b-41d4-a716-446655440000/download")
                .fileSizeBytes(1048576L)
                .expiresAt(testExpiresAt)
                .requestedAt(testRequestedAt)
                .completedAt(testCompletedAt)
                .build();

        DataExportResponse response2 = DataExportResponse.builder()
                .exportId(testExportId)
                .status("completed")
                .progress(100)
                .estimatedCompletion(testEstimatedCompletion)
                .downloadUrl("https://identity.focushive.com/api/v1/privacy/data/export/550e8400-e29b-41d4-a716-446655440000/download")
                .fileSizeBytes(1048576L)
                .expiresAt(testExpiresAt)
                .requestedAt(testRequestedAt)
                .completedAt(testCompletedAt)
                .build();

        // Then
        assertThat(response1).isEqualTo(response2);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    @DisplayName("Should test inequality with different values")
    void shouldTestInequalityWithDifferentValues() {
        // Given
        UUID differentId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        
        DataExportResponse response1 = DataExportResponse.builder()
                .exportId(testExportId)
                .status("completed")
                .progress(100)
                .build();

        DataExportResponse response2 = DataExportResponse.builder()
                .exportId(differentId)
                .status("processing")
                .progress(50)
                .build();

        // Then
        assertThat(response1).isNotEqualTo(response2);
        assertThat(response1.hashCode()).isNotEqualTo(response2.hashCode());
    }

    @Test
    @DisplayName("Should test toString method")
    void shouldTestToStringMethod() {
        // Given
        DataExportResponse response = DataExportResponse.builder()
                .exportId(testExportId)
                .status("processing")
                .progress(45)
                .estimatedCompletion(testEstimatedCompletion)
                .downloadUrl("https://example.com/download")
                .fileSizeBytes(1024L)
                .expiresAt(testExpiresAt)
                .requestedAt(testRequestedAt)
                .completedAt(null)
                .errorMessage(null)
                .build();

        // When
        String toString = response.toString();

        // Then
        assertThat(toString).isNotNull();
        assertThat(toString).contains("DataExportResponse");
        assertThat(toString).contains("exportId=550e8400-e29b-41d4-a716-446655440000");
        assertThat(toString).contains("status=processing");
        assertThat(toString).contains("progress=45");
        assertThat(toString).contains("estimatedCompletion=");
        assertThat(toString).contains("downloadUrl=https://example.com/download");
        assertThat(toString).contains("fileSizeBytes=1024");
        assertThat(toString).contains("expiresAt=");
        assertThat(toString).contains("requestedAt=");
        assertThat(toString).contains("completedAt=null");
        assertThat(toString).contains("errorMessage=null");
    }

    @Test
    @DisplayName("Should handle all possible status values")
    void shouldHandleAllPossibleStatusValues() {
        // Test all allowable status values: requested, processing, completed, failed, expired
        
        // Requested status
        DataExportResponse requestedResponse = DataExportResponse.builder()
                .exportId(testExportId)
                .status("requested")
                .progress(0)
                .build();
        assertThat(requestedResponse.getStatus()).isEqualTo("requested");
        
        // Processing status
        DataExportResponse processingResponse = DataExportResponse.builder()
                .exportId(testExportId)
                .status("processing")
                .progress(45)
                .build();
        assertThat(processingResponse.getStatus()).isEqualTo("processing");
        
        // Completed status
        DataExportResponse completedResponse = DataExportResponse.builder()
                .exportId(testExportId)
                .status("completed")
                .progress(100)
                .build();
        assertThat(completedResponse.getStatus()).isEqualTo("completed");
        
        // Failed status
        DataExportResponse failedResponse = DataExportResponse.builder()
                .exportId(testExportId)
                .status("failed")
                .progress(50)
                .errorMessage("Processing failed")
                .build();
        assertThat(failedResponse.getStatus()).isEqualTo("failed");
        
        // Expired status
        DataExportResponse expiredResponse = DataExportResponse.builder()
                .exportId(testExportId)
                .status("expired")
                .progress(100)
                .build();
        assertThat(expiredResponse.getStatus()).isEqualTo("expired");
    }

    @Test
    @DisplayName("Should handle various progress values")
    void shouldHandleVariousProgressValues() {
        // 0% progress (requested)
        DataExportResponse zeroProgress = DataExportResponse.builder()
                .exportId(testExportId)
                .status("requested")
                .progress(0)
                .build();
        assertThat(zeroProgress.getProgress()).isEqualTo(0);
        
        // Partial progress (processing)
        DataExportResponse partialProgress = DataExportResponse.builder()
                .exportId(testExportId)
                .status("processing")
                .progress(50)
                .build();
        assertThat(partialProgress.getProgress()).isEqualTo(50);
        
        // Full progress (completed)
        DataExportResponse fullProgress = DataExportResponse.builder()
                .exportId(testExportId)
                .status("completed")
                .progress(100)
                .build();
        assertThat(fullProgress.getProgress()).isEqualTo(100);
        
        // Null progress
        DataExportResponse nullProgress = DataExportResponse.builder()
                .exportId(testExportId)
                .status("requested")
                .progress(null)
                .build();
        assertThat(nullProgress.getProgress()).isNull();
    }

    @Test
    @DisplayName("Should handle different file sizes")
    void shouldHandleDifferentFileSizes() {
        // Small file (1KB)
        DataExportResponse smallFile = DataExportResponse.builder()
                .exportId(testExportId)
                .status("completed")
                .fileSizeBytes(1024L)
                .build();
        assertThat(smallFile.getFileSizeBytes()).isEqualTo(1024L);
        
        // Medium file (1MB)
        DataExportResponse mediumFile = DataExportResponse.builder()
                .exportId(testExportId)
                .status("completed")
                .fileSizeBytes(1048576L)
                .build();
        assertThat(mediumFile.getFileSizeBytes()).isEqualTo(1048576L);
        
        // Large file (1GB)
        DataExportResponse largeFile = DataExportResponse.builder()
                .exportId(testExportId)
                .status("completed")
                .fileSizeBytes(1073741824L)
                .build();
        assertThat(largeFile.getFileSizeBytes()).isEqualTo(1073741824L);
        
        // Zero bytes
        DataExportResponse zeroBytes = DataExportResponse.builder()
                .exportId(testExportId)
                .status("completed")
                .fileSizeBytes(0L)
                .build();
        assertThat(zeroBytes.getFileSizeBytes()).isEqualTo(0L);
        
        // Null file size (when not completed)
        DataExportResponse nullSize = DataExportResponse.builder()
                .exportId(testExportId)
                .status("processing")
                .fileSizeBytes(null)
                .build();
        assertThat(nullSize.getFileSizeBytes()).isNull();
    }

    @Test
    @DisplayName("Should handle various download URL formats")
    void shouldHandleVariousDownloadUrlFormats() {
        // HTTPS URL
        DataExportResponse httpsUrl = DataExportResponse.builder()
                .exportId(testExportId)
                .status("completed")
                .downloadUrl("https://identity.focushive.com/api/v1/privacy/data/export/550e8400-e29b-41d4-a716-446655440000/download")
                .build();
        assertThat(httpsUrl.getDownloadUrl()).startsWith("https://");
        assertThat(httpsUrl.getDownloadUrl()).contains("identity.focushive.com");
        
        // URL with query parameters
        DataExportResponse urlWithQuery = DataExportResponse.builder()
                .exportId(testExportId)
                .status("completed")
                .downloadUrl("https://identity.focushive.com/api/v1/privacy/data/export/download?token=abc123&expires=1234567890")
                .build();
        assertThat(urlWithQuery.getDownloadUrl()).contains("?token=");
        assertThat(urlWithQuery.getDownloadUrl()).contains("&expires=");
        
        // Null URL (when not completed)
        DataExportResponse nullUrl = DataExportResponse.builder()
                .exportId(testExportId)
                .status("processing")
                .downloadUrl(null)
                .build();
        assertThat(nullUrl.getDownloadUrl()).isNull();
    }

    @Test
    @DisplayName("Should handle time-based edge cases")
    void shouldHandleTimeBasedEdgeCases() {
        // All timestamps the same
        Instant sameTime = Instant.parse("2023-01-15T10:30:00Z");
        DataExportResponse sameTimeResponse = DataExportResponse.builder()
                .exportId(testExportId)
                .status("completed")
                .estimatedCompletion(sameTime)
                .expiresAt(sameTime)
                .requestedAt(sameTime)
                .completedAt(sameTime)
                .build();
        
        assertThat(sameTimeResponse.getEstimatedCompletion()).isEqualTo(sameTime);
        assertThat(sameTimeResponse.getExpiresAt()).isEqualTo(sameTime);
        assertThat(sameTimeResponse.getRequestedAt()).isEqualTo(sameTime);
        assertThat(sameTimeResponse.getCompletedAt()).isEqualTo(sameTime);
        
        // Completed before estimated completion
        Instant requestTime = Instant.parse("2023-01-15T10:00:00Z");
        Instant estimatedTime = Instant.parse("2023-01-15T10:30:00Z");
        Instant completedTime = Instant.parse("2023-01-15T10:20:00Z");
        
        DataExportResponse earlyCompletion = DataExportResponse.builder()
                .exportId(testExportId)
                .status("completed")
                .requestedAt(requestTime)
                .estimatedCompletion(estimatedTime)
                .completedAt(completedTime)
                .build();
        
        assertThat(earlyCompletion.getCompletedAt()).isBefore(earlyCompletion.getEstimatedCompletion());
        
        // Far future expiry
        Instant farFuture = Instant.parse("2030-01-15T10:30:00Z");
        DataExportResponse farFutureExpiry = DataExportResponse.builder()
                .exportId(testExportId)
                .status("completed")
                .requestedAt(testRequestedAt)
                .expiresAt(farFuture)
                .build();
        
        assertThat(farFutureExpiry.getExpiresAt()).isAfter(farFutureExpiry.getRequestedAt());
    }

    @Test
    @DisplayName("Should handle error message scenarios")
    void shouldHandleErrorMessageScenarios() {
        // Success - no error message
        DataExportResponse success = DataExportResponse.builder()
                .exportId(testExportId)
                .status("completed")
                .errorMessage(null)
                .build();
        assertThat(success.getErrorMessage()).isNull();
        
        // Generic error
        DataExportResponse genericError = DataExportResponse.builder()
                .exportId(testExportId)
                .status("failed")
                .errorMessage("An error occurred during processing")
                .build();
        assertThat(genericError.getErrorMessage()).isEqualTo("An error occurred during processing");
        
        // Specific error
        DataExportResponse specificError = DataExportResponse.builder()
                .exportId(testExportId)
                .status("failed")
                .errorMessage("Database connection timeout after 30 seconds")
                .build();
        assertThat(specificError.getErrorMessage()).contains("Database connection timeout");
        
        // Very long error message
        String longError = "A very long error message that contains detailed information about what went wrong ".repeat(10);
        DataExportResponse longErrorResponse = DataExportResponse.builder()
                .exportId(testExportId)
                .status("failed")
                .errorMessage(longError)
                .build();
        assertThat(longErrorResponse.getErrorMessage()).hasSize(longError.length());
        
        // Empty error message
        DataExportResponse emptyError = DataExportResponse.builder()
                .exportId(testExportId)
                .status("failed")
                .errorMessage("")
                .build();
        assertThat(emptyError.getErrorMessage()).isEmpty();
    }

    @Test
    @DisplayName("Should handle special characters and unicode in string fields")
    void shouldHandleSpecialCharactersAndUnicodeInStringFields() {
        // Unicode in status
        DataExportResponse unicodeStatus = DataExportResponse.builder()
                .exportId(testExportId)
                .status("处理中")
                .progress(50)
                .build();
        assertThat(unicodeStatus.getStatus()).isEqualTo("处理中");
        
        // Special characters in download URL
        String urlWithSpecialChars = "https://example.com/download?file=data%20export&user=test@example.com";
        DataExportResponse specialUrl = DataExportResponse.builder()
                .exportId(testExportId)
                .status("completed")
                .downloadUrl(urlWithSpecialChars)
                .build();
        assertThat(specialUrl.getDownloadUrl()).isEqualTo(urlWithSpecialChars);
        
        // Unicode in error message
        String unicodeError = "Erreur de traitement: données indisponibles - テスト エラー";
        DataExportResponse unicodeErrorResponse = DataExportResponse.builder()
                .exportId(testExportId)
                .status("failed")
                .errorMessage(unicodeError)
                .build();
        assertThat(unicodeErrorResponse.getErrorMessage()).isEqualTo(unicodeError);
    }

    @Test
    @DisplayName("Should handle builder chaining correctly")
    void shouldHandleBuilderChainingCorrectly() {
        // Test builder reuse
        DataExportResponse.DataExportResponseBuilder builder = DataExportResponse.builder()
                .exportId(testExportId)
                .status("processing");

        DataExportResponse response1 = builder.progress(25).build();
        DataExportResponse response2 = builder.progress(75).build();

        assertThat(response1.getProgress()).isEqualTo(25);
        assertThat(response2.getProgress()).isEqualTo(75);
        assertThat(response1.getExportId()).isEqualTo(response2.getExportId());
        assertThat(response1.getStatus()).isEqualTo(response2.getStatus());
    }

    @Test
    @DisplayName("Should handle edge cases for UUID")
    void shouldHandleEdgeCasesForUUID() {
        // Test various UUID formats
        UUID nilUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
        UUID maxUuid = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        UUID randomUuid = UUID.randomUUID();
        
        // Nil UUID
        DataExportResponse nilResponse = DataExportResponse.builder()
                .exportId(nilUuid)
                .status("requested")
                .build();
        assertThat(nilResponse.getExportId()).isEqualTo(nilUuid);
        
        // Max UUID
        DataExportResponse maxResponse = DataExportResponse.builder()
                .exportId(maxUuid)
                .status("processing")
                .build();
        assertThat(maxResponse.getExportId()).isEqualTo(maxUuid);
        
        // Random UUID
        DataExportResponse randomResponse = DataExportResponse.builder()
                .exportId(randomUuid)
                .status("completed")
                .build();
        assertThat(randomResponse.getExportId()).isEqualTo(randomUuid);
        
        // Null UUID
        DataExportResponse nullUuidResponse = DataExportResponse.builder()
                .exportId(null)
                .status("failed")
                .build();
        assertThat(nullUuidResponse.getExportId()).isNull();
    }
}