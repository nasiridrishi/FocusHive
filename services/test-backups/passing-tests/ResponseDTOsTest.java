package com.focushive.buddy.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for response DTO classes.
 *
 * Following TDD approach:
 * 1. RED: These tests will initially FAIL because DTO classes don't exist
 * 2. GREEN: Implement DTO classes to make tests pass
 * 3. REFACTOR: Improve implementation while keeping tests green
 */
@DisplayName("Response DTOs")
class ResponseDTOsTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    @DisplayName("ApiResponse should handle successful response")
    void testApiResponseSuccess() {
        String data = "test data";
        ApiResponse<String> response = ApiResponse.success(data);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getMessage()).isEqualTo("Success");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getErrors()).isNull();
    }

    @Test
    @DisplayName("ApiResponse should handle successful response with custom message")
    void testApiResponseSuccessWithMessage() {
        String data = "test data";
        String message = "Operation completed successfully";
        ApiResponse<String> response = ApiResponse.success(data, message);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getErrors()).isNull();
    }

    @Test
    @DisplayName("ApiResponse should handle error response")
    void testApiResponseError() {
        String message = "Something went wrong";
        ApiResponse<String> response = ApiResponse.error(message);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getErrors()).isNull();
    }

    @Test
    @DisplayName("ApiResponse should handle error response with validation errors")
    void testApiResponseErrorWithValidationErrors() {
        String message = "Validation failed";
        List<String> errors = List.of("Email is required", "Password too short");
        ApiResponse<String> response = ApiResponse.error(message, errors);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getErrors()).containsExactlyElementsOf(errors);
    }

    @Test
    @DisplayName("ApiResponse should be serializable to JSON")
    void testApiResponseJsonSerialization() throws Exception {
        ApiResponse<String> response = ApiResponse.success("test", "Success");

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"success\":true");
        assertThat(json).contains("\"data\":\"test\"");
        assertThat(json).contains("\"message\":\"Success\"");
        assertThat(json).contains("\"timestamp\":");
    }

    @Test
    @DisplayName("ErrorResponse should contain error details")
    void testErrorResponse() {
        String message = "Internal server error";
        String path = "/api/v1/buddy/partnerships";
        int status = 500;

        ErrorResponse errorResponse = ErrorResponse.builder()
            .message(message)
            .path(path)
            .status(status)
            .build();

        assertThat(errorResponse.getMessage()).isEqualTo(message);
        assertThat(errorResponse.getPath()).isEqualTo(path);
        assertThat(errorResponse.getStatus()).isEqualTo(status);
        assertThat(errorResponse.getTimestamp()).isNotNull();
        assertThat(errorResponse.getErrors()).isNotNull();
        assertThat(errorResponse.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("ErrorResponse should support validation errors")
    void testErrorResponseWithValidationErrors() {
        List<ValidationError> validationErrors = List.of(
            ValidationError.builder()
                .field("email")
                .message("Email is required")
                .rejectedValue(null)
                .build(),
            ValidationError.builder()
                .field("password")
                .message("Password must be at least 8 characters")
                .rejectedValue("123")
                .build()
        );

        ErrorResponse errorResponse = ErrorResponse.builder()
            .message("Validation failed")
            .path("/api/v1/buddy/request")
            .status(400)
            .errors(validationErrors)
            .build();

        assertThat(errorResponse.getErrors()).hasSize(2);
        assertThat(errorResponse.getErrors().get(0).getField()).isEqualTo("email");
        assertThat(errorResponse.getErrors().get(0).getMessage()).isEqualTo("Email is required");
        assertThat(errorResponse.getErrors().get(1).getField()).isEqualTo("password");
        assertThat(errorResponse.getErrors().get(1).getRejectedValue()).isEqualTo("123");
    }

    @Test
    @DisplayName("ErrorResponse should be serializable to JSON")
    void testErrorResponseJsonSerialization() throws Exception {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .message("Not found")
            .path("/api/v1/buddy/partnerships/123")
            .status(404)
            .build();

        String json = objectMapper.writeValueAsString(errorResponse);

        assertThat(json).contains("\"message\":\"Not found\"");
        assertThat(json).contains("\"path\":\"/api/v1/buddy/partnerships/123\"");
        assertThat(json).contains("\"status\":404");
        assertThat(json).contains("\"timestamp\":");
        assertThat(json).contains("\"errors\":[]");
    }

    @Test
    @DisplayName("PaginatedResponse should handle paginated data")
    void testPaginatedResponse() {
        List<String> content = List.of("item1", "item2", "item3");
        long totalElements = 100L;
        int totalPages = 10;
        int currentPage = 2;
        int pageSize = 10;
        boolean hasNext = true;
        boolean hasPrevious = true;

        PaginatedResponse<String> response = PaginatedResponse.<String>builder()
            .content(content)
            .totalElements(totalElements)
            .totalPages(totalPages)
            .currentPage(currentPage)
            .pageSize(pageSize)
            .hasNext(hasNext)
            .hasPrevious(hasPrevious)
            .build();

        assertThat(response.getContent()).containsExactlyElementsOf(content);
        assertThat(response.getTotalElements()).isEqualTo(totalElements);
        assertThat(response.getTotalPages()).isEqualTo(totalPages);
        assertThat(response.getCurrentPage()).isEqualTo(currentPage);
        assertThat(response.getPageSize()).isEqualTo(pageSize);
        assertThat(response.isHasNext()).isTrue();
        assertThat(response.isHasPrevious()).isTrue();
        assertThat(response.isEmpty()).isFalse();
        assertThat(response.isFirst()).isFalse();
        assertThat(response.isLast()).isFalse();
    }

    @Test
    @DisplayName("PaginatedResponse should detect first page")
    void testPaginatedResponseFirstPage() {
        List<String> content = List.of("item1", "item2");

        PaginatedResponse<String> response = PaginatedResponse.<String>builder()
            .content(content)
            .totalElements(20L)
            .totalPages(10)
            .currentPage(0)
            .pageSize(2)
            .hasNext(true)
            .hasPrevious(false)
            .build();

        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isFalse();
        assertThat(response.isHasPrevious()).isFalse();
        assertThat(response.isHasNext()).isTrue();
    }

    @Test
    @DisplayName("PaginatedResponse should detect last page")
    void testPaginatedResponseLastPage() {
        List<String> content = List.of("item1", "item2");

        PaginatedResponse<String> response = PaginatedResponse.<String>builder()
            .content(content)
            .totalElements(20L)
            .totalPages(10)
            .currentPage(9)
            .pageSize(2)
            .hasNext(false)
            .hasPrevious(true)
            .build();

        assertThat(response.isFirst()).isFalse();
        assertThat(response.isLast()).isTrue();
        assertThat(response.isHasPrevious()).isTrue();
        assertThat(response.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("PaginatedResponse should detect empty page")
    void testPaginatedResponseEmpty() {
        List<String> content = List.of();

        PaginatedResponse<String> response = PaginatedResponse.<String>builder()
            .content(content)
            .totalElements(0L)
            .totalPages(0)
            .currentPage(0)
            .pageSize(10)
            .hasNext(false)
            .hasPrevious(false)
            .build();

        assertThat(response.isEmpty()).isTrue();
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isTrue();
    }

    @Test
    @DisplayName("ValidationError should contain field-level error details")
    void testValidationError() {
        String field = "compatibilityScore";
        String message = "Must be between 0.0 and 1.0";
        Object rejectedValue = 1.5;
        String code = "INVALID_RANGE";

        ValidationError error = ValidationError.builder()
            .field(field)
            .message(message)
            .rejectedValue(rejectedValue)
            .code(code)
            .build();

        assertThat(error.getField()).isEqualTo(field);
        assertThat(error.getMessage()).isEqualTo(message);
        assertThat(error.getRejectedValue()).isEqualTo(rejectedValue);
        assertThat(error.getCode()).isEqualTo(code);
    }

    @Test
    @DisplayName("ValidationError should handle null rejected value")
    void testValidationErrorWithNullValue() {
        ValidationError error = ValidationError.builder()
            .field("email")
            .message("Email is required")
            .rejectedValue(null)
            .build();

        assertThat(error.getRejectedValue()).isNull();
        assertThat(error.getCode()).isNull();
    }

    @Test
    @DisplayName("ValidationError should be serializable to JSON")
    void testValidationErrorJsonSerialization() throws Exception {
        ValidationError error = ValidationError.builder()
            .field("timezone")
            .message("Invalid timezone")
            .rejectedValue("InvalidTZ")
            .code("INVALID_TIMEZONE")
            .build();

        String json = objectMapper.writeValueAsString(error);

        assertThat(json).contains("\"field\":\"timezone\"");
        assertThat(json).contains("\"message\":\"Invalid timezone\"");
        assertThat(json).contains("\"rejectedValue\":\"InvalidTZ\"");
        assertThat(json).contains("\"code\":\"INVALID_TIMEZONE\"");
    }
}