package com.focushive.identity.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for DataExportRequest covering builder patterns,
 * validation, serialization/deserialization, equality, and edge cases.
 */
@DisplayName("DataExportRequest Unit Tests")
class DataExportRequestUnitTest {

    private ObjectMapper objectMapper;
    private Validator validator;
    private Set<String> testDataCategories;
    private Set<String> testPersonaIds;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        testDataCategories = new HashSet<>();
        testDataCategories.add("profile");
        testDataCategories.add("personas");
        testDataCategories.add("preferences");
        testDataCategories.add("activity");
        testDataCategories.add("connections");
        
        testPersonaIds = new HashSet<>();
        testPersonaIds.add("persona-1");
        testPersonaIds.add("persona-2");
        testPersonaIds.add("persona-3");
    }

    @Test
    @DisplayName("Should create DataExportRequest using builder with all fields")
    void shouldCreateDataExportRequestUsingBuilderWithAllFields() {
        // Given & When
        DataExportRequest request = DataExportRequest.builder()
                .dataCategories(testDataCategories)
                .format("json")
                .includeDeleted(true)
                .dateFrom("2023-01-01T00:00:00Z")
                .dateTo("2023-12-31T23:59:59Z")
                .personaIds(testPersonaIds)
                .build();

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getDataCategories()).isEqualTo(testDataCategories);
        assertThat(request.getFormat()).isEqualTo("json");
        assertThat(request.getIncludeDeleted()).isTrue();
        assertThat(request.getDateFrom()).isEqualTo("2023-01-01T00:00:00Z");
        assertThat(request.getDateTo()).isEqualTo("2023-12-31T23:59:59Z");
        assertThat(request.getPersonaIds()).isEqualTo(testPersonaIds);
    }

    @Test
    @DisplayName("Should create DataExportRequest with default values")
    void shouldCreateDataExportRequestWithDefaultValues() {
        // Given & When
        DataExportRequest request = DataExportRequest.builder()
                .dataCategories(testDataCategories)
                .build();

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getDataCategories()).isEqualTo(testDataCategories);
        assertThat(request.getFormat()).isEqualTo("json"); // Default value
        assertThat(request.getIncludeDeleted()).isFalse(); // Default value
        assertThat(request.getDateFrom()).isNull();
        assertThat(request.getDateTo()).isNull();
        assertThat(request.getPersonaIds()).isNull();
    }

    @Test
    @DisplayName("Should create DataExportRequest with minimal required fields")
    void shouldCreateDataExportRequestWithMinimalFields() {
        // Given
        Set<String> minimalCategories = Set.of("profile");

        // When
        DataExportRequest request = DataExportRequest.builder()
                .dataCategories(minimalCategories)
                .build();

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getDataCategories()).hasSize(1);
        assertThat(request.getDataCategories()).contains("profile");
        assertThat(request.getFormat()).isEqualTo("json");
        assertThat(request.getIncludeDeleted()).isFalse();
    }

    @Test
    @DisplayName("Should create DataExportRequest using no-args constructor and setters")
    void shouldCreateDataExportRequestUsingNoArgsConstructorAndSetters() {
        // Given
        DataExportRequest request = new DataExportRequest();

        // When
        request.setDataCategories(testDataCategories);
        request.setFormat("xml");
        request.setIncludeDeleted(true);
        request.setDateFrom("2023-06-01T00:00:00Z");
        request.setDateTo("2023-06-30T23:59:59Z");
        request.setPersonaIds(testPersonaIds);

        // Then
        assertThat(request.getDataCategories()).isEqualTo(testDataCategories);
        assertThat(request.getFormat()).isEqualTo("xml");
        assertThat(request.getIncludeDeleted()).isTrue();
        assertThat(request.getDateFrom()).isEqualTo("2023-06-01T00:00:00Z");
        assertThat(request.getDateTo()).isEqualTo("2023-06-30T23:59:59Z");
        assertThat(request.getPersonaIds()).isEqualTo(testPersonaIds);
    }

    @Test
    @DisplayName("Should create DataExportRequest using all-args constructor")
    void shouldCreateDataExportRequestUsingAllArgsConstructor() {
        // Given & When
        DataExportRequest request = new DataExportRequest(
                testDataCategories,
                "csv",
                true,
                "2023-03-01T00:00:00Z",
                "2023-03-31T23:59:59Z",
                testPersonaIds
        );

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getDataCategories()).isEqualTo(testDataCategories);
        assertThat(request.getFormat()).isEqualTo("csv");
        assertThat(request.getIncludeDeleted()).isTrue();
        assertThat(request.getDateFrom()).isEqualTo("2023-03-01T00:00:00Z");
        assertThat(request.getDateTo()).isEqualTo("2023-03-31T23:59:59Z");
        assertThat(request.getPersonaIds()).isEqualTo(testPersonaIds);
    }

    @Test
    @DisplayName("Should validate successfully with valid data categories")
    void shouldValidateSuccessfullyWithValidDataCategories() {
        // Given
        DataExportRequest request = DataExportRequest.builder()
                .dataCategories(testDataCategories)
                .format("json")
                .build();

        // When
        Set<ConstraintViolation<DataExportRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation with empty data categories")
    void shouldFailValidationWithEmptyDataCategories() {
        // Given
        DataExportRequest request = DataExportRequest.builder()
                .dataCategories(new HashSet<>())
                .format("json")
                .build();

        // When
        Set<ConstraintViolation<DataExportRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<DataExportRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("At least one data category must be selected");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("dataCategories");
    }

    @Test
    @DisplayName("Should fail validation with null data categories")
    void shouldFailValidationWithNullDataCategories() {
        // Given
        DataExportRequest request = DataExportRequest.builder()
                .dataCategories(null)
                .format("json")
                .build();

        // When
        Set<ConstraintViolation<DataExportRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<DataExportRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("At least one data category must be selected");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("dataCategories");
    }

    @Test
    @DisplayName("Should serialize DataExportRequest to JSON correctly")
    void shouldSerializeDataExportRequestToJsonCorrectly() throws JsonProcessingException {
        // Given
        DataExportRequest request = DataExportRequest.builder()
                .dataCategories(testDataCategories)
                .format("json")
                .includeDeleted(false)
                .dateFrom("2023-01-01T00:00:00Z")
                .dateTo("2023-12-31T23:59:59Z")
                .personaIds(testPersonaIds)
                .build();

        // When
        String json = objectMapper.writeValueAsString(request);

        // Then
        assertThat(json).isNotNull();
        assertThat(json).contains("\"dataCategories\":[");
        assertThat(json).contains("\"profile\"");
        assertThat(json).contains("\"personas\"");
        assertThat(json).contains("\"preferences\"");
        assertThat(json).contains("\"activity\"");
        assertThat(json).contains("\"connections\"");
        assertThat(json).contains("\"format\":\"json\"");
        assertThat(json).contains("\"includeDeleted\":false");
        assertThat(json).contains("\"dateFrom\":\"2023-01-01T00:00:00Z\"");
        assertThat(json).contains("\"dateTo\":\"2023-12-31T23:59:59Z\"");
        assertThat(json).contains("\"personaIds\":[");
        assertThat(json).contains("\"persona-1\"");
        assertThat(json).contains("\"persona-2\"");
        assertThat(json).contains("\"persona-3\"");
    }

    @Test
    @DisplayName("Should deserialize JSON to DataExportRequest correctly")
    void shouldDeserializeJsonToDataExportRequestCorrectly() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "dataCategories": ["profile", "activity", "connections"],
                    "format": "xml",
                    "includeDeleted": true,
                    "dateFrom": "2023-07-01T00:00:00Z",
                    "dateTo": "2023-07-31T23:59:59Z",
                    "personaIds": ["persona-a", "persona-b"]
                }
                """;

        // When
        DataExportRequest request = objectMapper.readValue(json, DataExportRequest.class);

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getDataCategories()).hasSize(3);
        assertThat(request.getDataCategories()).contains("profile", "activity", "connections");
        assertThat(request.getFormat()).isEqualTo("xml");
        assertThat(request.getIncludeDeleted()).isTrue();
        assertThat(request.getDateFrom()).isEqualTo("2023-07-01T00:00:00Z");
        assertThat(request.getDateTo()).isEqualTo("2023-07-31T23:59:59Z");
        assertThat(request.getPersonaIds()).hasSize(2);
        assertThat(request.getPersonaIds()).contains("persona-a", "persona-b");
    }

    @Test
    @DisplayName("Should deserialize JSON with default values correctly")
    void shouldDeserializeJsonWithDefaultValuesCorrectly() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "dataCategories": ["profile", "preferences"]
                }
                """;

        // When
        DataExportRequest request = objectMapper.readValue(json, DataExportRequest.class);

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getDataCategories()).hasSize(2);
        assertThat(request.getDataCategories()).contains("profile", "preferences");
        assertThat(request.getFormat()).isEqualTo("json"); // Default value
        assertThat(request.getIncludeDeleted()).isFalse(); // Default value
        assertThat(request.getDateFrom()).isNull();
        assertThat(request.getDateTo()).isNull();
        assertThat(request.getPersonaIds()).isNull();
    }

    @Test
    @DisplayName("Should handle null values in serialization")
    void shouldHandleNullValuesInSerialization() throws JsonProcessingException {
        // Given
        DataExportRequest request = DataExportRequest.builder()
                .dataCategories(testDataCategories)
                .format("json")
                .includeDeleted(false)
                .dateFrom(null)
                .dateTo(null)
                .personaIds(null)
                .build();

        // When
        String json = objectMapper.writeValueAsString(request);

        // Then
        assertThat(json).isNotNull();
        assertThat(json).contains("\"dateFrom\":null");
        assertThat(json).contains("\"dateTo\":null");
        assertThat(json).contains("\"personaIds\":null");
    }

    @Test
    @DisplayName("Should test equality and hashCode with same values")
    void shouldTestEqualityAndHashCodeWithSameValues() {
        // Given
        DataExportRequest request1 = DataExportRequest.builder()
                .dataCategories(testDataCategories)
                .format("json")
                .includeDeleted(true)
                .dateFrom("2023-01-01T00:00:00Z")
                .dateTo("2023-12-31T23:59:59Z")
                .personaIds(testPersonaIds)
                .build();

        DataExportRequest request2 = DataExportRequest.builder()
                .dataCategories(testDataCategories)
                .format("json")
                .includeDeleted(true)
                .dateFrom("2023-01-01T00:00:00Z")
                .dateTo("2023-12-31T23:59:59Z")
                .personaIds(testPersonaIds)
                .build();

        // Then
        assertThat(request1).isEqualTo(request2);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    @DisplayName("Should test inequality with different values")
    void shouldTestInequalityWithDifferentValues() {
        // Given
        DataExportRequest request1 = DataExportRequest.builder()
                .dataCategories(Set.of("profile"))
                .format("json")
                .includeDeleted(false)
                .build();

        DataExportRequest request2 = DataExportRequest.builder()
                .dataCategories(Set.of("activity"))
                .format("xml")
                .includeDeleted(true)
                .build();

        // Then
        assertThat(request1).isNotEqualTo(request2);
        assertThat(request1.hashCode()).isNotEqualTo(request2.hashCode());
    }

    @Test
    @DisplayName("Should test toString method")
    void shouldTestToStringMethod() {
        // Given
        DataExportRequest request = DataExportRequest.builder()
                .dataCategories(Set.of("profile", "activity"))
                .format("json")
                .includeDeleted(true)
                .dateFrom("2023-01-01T00:00:00Z")
                .dateTo("2023-12-31T23:59:59Z")
                .personaIds(Set.of("persona-1"))
                .build();

        // When
        String toString = request.toString();

        // Then
        assertThat(toString).isNotNull();
        assertThat(toString).contains("DataExportRequest");
        assertThat(toString).contains("dataCategories=");
        assertThat(toString).contains("format=json");
        assertThat(toString).contains("includeDeleted=true");
        assertThat(toString).contains("dateFrom=2023-01-01T00:00:00Z");
        assertThat(toString).contains("dateTo=2023-12-31T23:59:59Z");
        assertThat(toString).contains("personaIds=");
    }

    @Test
    @DisplayName("Should handle different export formats")
    void shouldHandleDifferentExportFormats() {
        // Test all allowable formats: json, xml, csv
        
        // JSON format
        DataExportRequest jsonRequest = DataExportRequest.builder()
                .dataCategories(testDataCategories)
                .format("json")
                .build();
        assertThat(jsonRequest.getFormat()).isEqualTo("json");
        
        // XML format
        DataExportRequest xmlRequest = DataExportRequest.builder()
                .dataCategories(testDataCategories)
                .format("xml")
                .build();
        assertThat(xmlRequest.getFormat()).isEqualTo("xml");
        
        // CSV format
        DataExportRequest csvRequest = DataExportRequest.builder()
                .dataCategories(testDataCategories)
                .format("csv")
                .build();
        assertThat(csvRequest.getFormat()).isEqualTo("csv");
    }

    @Test
    @DisplayName("Should handle various data categories combinations")
    void shouldHandleVariousDataCategoriesCombinations() {
        // Single category
        Set<String> singleCategory = Set.of("profile");
        DataExportRequest singleRequest = DataExportRequest.builder()
                .dataCategories(singleCategory)
                .build();
        assertThat(singleRequest.getDataCategories()).hasSize(1);
        assertThat(singleRequest.getDataCategories()).contains("profile");
        
        // Multiple categories
        Set<String> multipleCategories = Set.of("profile", "personas", "activity");
        DataExportRequest multipleRequest = DataExportRequest.builder()
                .dataCategories(multipleCategories)
                .build();
        assertThat(multipleRequest.getDataCategories()).hasSize(3);
        assertThat(multipleRequest.getDataCategories()).containsAll(multipleCategories);
        
        // All possible categories
        Set<String> allCategories = Set.of("profile", "personas", "preferences", "activity", "connections");
        DataExportRequest allRequest = DataExportRequest.builder()
                .dataCategories(allCategories)
                .build();
        assertThat(allRequest.getDataCategories()).hasSize(5);
        assertThat(allRequest.getDataCategories()).containsAll(allCategories);
    }

    @Test
    @DisplayName("Should handle edge cases for date ranges")
    void shouldHandleEdgeCasesForDateRanges() {
        // Both dates provided
        DataExportRequest bothDatesRequest = DataExportRequest.builder()
                .dataCategories(testDataCategories)
                .dateFrom("2023-01-01T00:00:00Z")
                .dateTo("2023-12-31T23:59:59Z")
                .build();
        assertThat(bothDatesRequest.getDateFrom()).isEqualTo("2023-01-01T00:00:00Z");
        assertThat(bothDatesRequest.getDateTo()).isEqualTo("2023-12-31T23:59:59Z");
        
        // Only from date
        DataExportRequest fromOnlyRequest = DataExportRequest.builder()
                .dataCategories(testDataCategories)
                .dateFrom("2023-01-01T00:00:00Z")
                .build();
        assertThat(fromOnlyRequest.getDateFrom()).isEqualTo("2023-01-01T00:00:00Z");
        assertThat(fromOnlyRequest.getDateTo()).isNull();
        
        // Only to date
        DataExportRequest toOnlyRequest = DataExportRequest.builder()
                .dataCategories(testDataCategories)
                .dateTo("2023-12-31T23:59:59Z")
                .build();
        assertThat(toOnlyRequest.getDateFrom()).isNull();
        assertThat(toOnlyRequest.getDateTo()).isEqualTo("2023-12-31T23:59:59Z");
        
        // No dates (should be null)
        DataExportRequest noDatesRequest = DataExportRequest.builder()
                .dataCategories(testDataCategories)
                .build();
        assertThat(noDatesRequest.getDateFrom()).isNull();
        assertThat(noDatesRequest.getDateTo()).isNull();
    }

    @Test
    @DisplayName("Should handle empty and null persona IDs")
    void shouldHandleEmptyAndNullPersonaIds() {
        // Empty persona IDs
        DataExportRequest emptyPersonasRequest = DataExportRequest.builder()
                .dataCategories(testDataCategories)
                .personaIds(new HashSet<>())
                .build();
        assertThat(emptyPersonasRequest.getPersonaIds()).isNotNull();
        assertThat(emptyPersonasRequest.getPersonaIds()).isEmpty();
        
        // Null persona IDs
        DataExportRequest nullPersonasRequest = DataExportRequest.builder()
                .dataCategories(testDataCategories)
                .personaIds(null)
                .build();
        assertThat(nullPersonasRequest.getPersonaIds()).isNull();
        
        // Single persona ID
        DataExportRequest singlePersonaRequest = DataExportRequest.builder()
                .dataCategories(testDataCategories)
                .personaIds(Set.of("persona-single"))
                .build();
        assertThat(singlePersonaRequest.getPersonaIds()).hasSize(1);
        assertThat(singlePersonaRequest.getPersonaIds()).contains("persona-single");
    }

    @Test
    @DisplayName("Should handle collection references correctly")
    void shouldHandleCollectionReferencesCorrectly() {
        // Given
        Set<String> originalDataCategories = new HashSet<>(testDataCategories);
        Set<String> originalPersonaIds = new HashSet<>(testPersonaIds);

        DataExportRequest request = DataExportRequest.builder()
                .dataCategories(originalDataCategories)
                .personaIds(originalPersonaIds)
                .build();

        // When & Then - verify collections are properly set
        assertThat(request.getDataCategories()).hasSize(testDataCategories.size());
        assertThat(request.getPersonaIds()).hasSize(testPersonaIds.size());
        assertThat(request.getDataCategories()).containsAll(testDataCategories);
        assertThat(request.getPersonaIds()).containsAll(testPersonaIds);
    }

    @Test
    @DisplayName("Should handle special characters and unicode in string fields")
    void shouldHandleSpecialCharactersAndUnicodeInStringFields() {
        // Given
        Set<String> specialCategories = Set.of("profile-ñ", "activity_测试", "preferences@special");
        Set<String> unicodePersonas = Set.of("persona-üñïçødé", "persona-テスト", "persona-العربية");

        // When
        DataExportRequest request = DataExportRequest.builder()
                .dataCategories(specialCategories)
                .format("json")
                .dateFrom("2023-01-01T00:00:00Z-测试")
                .dateTo("2023-12-31T23:59:59Z-ñoël")
                .personaIds(unicodePersonas)
                .build();

        // Then
        assertThat(request.getDataCategories()).containsAll(specialCategories);
        assertThat(request.getDateFrom()).contains("测试");
        assertThat(request.getDateTo()).contains("ñoël");
        assertThat(request.getPersonaIds()).containsAll(unicodePersonas);
    }

    @Test
    @DisplayName("Should handle builder chaining correctly")
    void shouldHandleBuilderChainingCorrectly() {
        // Given & When
        DataExportRequest request = DataExportRequest.builder()
                .dataCategories(Set.of("profile"))
                .format("json")
                .includeDeleted(false)
                .dateFrom("2023-01-01T00:00:00Z")
                .dateTo("2023-12-31T23:59:59Z")
                .personaIds(Set.of("persona-1"))
                .build();

        // Then - should be able to build multiple times from same builder
        DataExportRequest.DataExportRequestBuilder builder = DataExportRequest.builder()
                .dataCategories(testDataCategories)
                .format("xml");

        DataExportRequest request1 = builder.includeDeleted(true).build();
        DataExportRequest request2 = builder.includeDeleted(false).build();

        assertThat(request1.getIncludeDeleted()).isTrue();
        assertThat(request2.getIncludeDeleted()).isFalse();
        assertThat(request1.getDataCategories()).isEqualTo(request2.getDataCategories());
        assertThat(request1.getFormat()).isEqualTo(request2.getFormat());
    }
}