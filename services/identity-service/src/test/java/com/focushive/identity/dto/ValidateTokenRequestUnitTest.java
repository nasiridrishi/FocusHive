package com.focushive.identity.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Comprehensive unit tests for ValidateTokenRequest DTO.
 */
@DisplayName("ValidateTokenRequest DTO Unit Tests")
class ValidateTokenRequestUnitTest {

    private Validator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Nested
    @DisplayName("Builder Pattern Tests")
    class BuilderPatternTests {

        @Test
        @DisplayName("Should create ValidateTokenRequest using builder")
        void shouldCreateValidateTokenRequestUsingBuilder() {
            // When
            ValidateTokenRequest request = ValidateTokenRequest.builder()
                    .token("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getToken()).startsWith("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9")
            );
        }

        @Test
        @DisplayName("Should create ValidateTokenRequest with simple token")
        void shouldCreateValidateTokenRequestWithSimpleToken() {
            // When
            ValidateTokenRequest request = ValidateTokenRequest.builder()
                    .token("simple_access_token")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getToken()).isEqualTo("simple_access_token")
            );
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation when token is provided")
        void shouldPassValidationWhenTokenIsProvided() {
            // Given
            ValidateTokenRequest request = ValidateTokenRequest.builder()
                    .token("valid_token")
                    .build();

            // When
            Set<ConstraintViolation<ValidateTokenRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail validation when token is null")
        void shouldFailValidationWhenTokenIsNull() {
            // Given
            ValidateTokenRequest request = ValidateTokenRequest.builder()
                    .token(null)
                    .build();

            // When
            Set<ConstraintViolation<ValidateTokenRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).hasSize(1),
                    () -> {
                        ConstraintViolation<ValidateTokenRequest> violation = violations.iterator().next();
                        assertThat(violation.getMessage()).isEqualTo("Token is required");
                        assertThat(violation.getPropertyPath().toString()).isEqualTo("token");
                    }
            );
        }

        @Test
        @DisplayName("Should fail validation when token is blank")
        void shouldFailValidationWhenTokenIsBlank() {
            // Given
            ValidateTokenRequest request = ValidateTokenRequest.builder()
                    .token("   ")
                    .build();

            // When
            Set<ConstraintViolation<ValidateTokenRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).hasSize(1),
                    () -> {
                        ConstraintViolation<ValidateTokenRequest> violation = violations.iterator().next();
                        assertThat(violation.getMessage()).isEqualTo("Token is required");
                    }
            );
        }
    }

    @Nested
    @DisplayName("JSON Serialization Tests")
    class JsonSerializationTests {

        @Test
        @DisplayName("Should serialize to JSON correctly")
        void shouldSerializeToJsonCorrectly() throws Exception {
            // Given
            ValidateTokenRequest request = ValidateTokenRequest.builder()
                    .token("test_token_123")
                    .build();

            // When
            String json = objectMapper.writeValueAsString(request);

            // Then
            assertThat(json).contains("\"token\":\"test_token_123\"");
        }

        @Test
        @DisplayName("Should deserialize from JSON correctly")
        void shouldDeserializeFromJsonCorrectly() throws Exception {
            // Given
            String json = """
                {
                    "token": "deserialized_token_456"
                }
                """;

            // When
            ValidateTokenRequest request = objectMapper.readValue(json, ValidateTokenRequest.class);

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getToken()).isEqualTo("deserialized_token_456")
            );
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when tokens are the same")
        void shouldBeEqualWhenTokensAreTheSame() {
            // Given
            ValidateTokenRequest request1 = ValidateTokenRequest.builder()
                    .token("same_token")
                    .build();
            ValidateTokenRequest request2 = ValidateTokenRequest.builder()
                    .token("same_token")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request1).isEqualTo(request2),
                    () -> assertThat(request1.hashCode()).isEqualTo(request2.hashCode())
            );
        }

        @Test
        @DisplayName("Should not be equal when tokens differ")
        void shouldNotBeEqualWhenTokensDiffer() {
            // Given
            ValidateTokenRequest request1 = ValidateTokenRequest.builder()
                    .token("token_1")
                    .build();
            ValidateTokenRequest request2 = ValidateTokenRequest.builder()
                    .token("token_2")
                    .build();

            // Then
            assertThat(request1).isNotEqualTo(request2);
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create instance using no-args constructor")
        void shouldCreateInstanceUsingNoArgsConstructor() {
            // When
            ValidateTokenRequest request = new ValidateTokenRequest();

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getToken()).isNull()
            );
        }

        @Test
        @DisplayName("Should create instance using all-args constructor")
        void shouldCreateInstanceUsingAllArgsConstructor() {
            // When
            ValidateTokenRequest request = new ValidateTokenRequest("constructor_token");

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getToken()).isEqualTo("constructor_token")
            );
        }

        @Test
        @DisplayName("Should allow modification after creation")
        void shouldAllowModificationAfterCreation() {
            // Given
            ValidateTokenRequest request = new ValidateTokenRequest();

            // When
            request.setToken("modified_token");

            // Then
            assertThat(request.getToken()).isEqualTo("modified_token");
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long tokens")
        void shouldHandleVeryLongTokens() {
            // Given
            String longToken = "token_" + "a".repeat(5000);

            // When
            ValidateTokenRequest request = ValidateTokenRequest.builder()
                    .token(longToken)
                    .build();

            // Then
            assertThat(request.getToken()).hasSize(5006);
        }

        @Test
        @DisplayName("Should handle JWT tokens")
        void shouldHandleJwtTokens() {
            // Given
            String jwtToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJrZXkxIn0.eyJleHAiOjE2NDI2ODQ4NDgsImlhdCI6MTY0MjY4MTI0OCwianRpIjoiYWJjZGVmZ2giLCJpc3MiOiJodHRwczovL2ZvY3VzaGl2ZS5jb20iLCJhdWQiOlsiZm9jdXNoaXZlLWFwaSJdLCJzdWIiOiJ1c2VyMTIzIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiZm9jdXNoaXZlLWNsaWVudCIsInNlc3Npb25fc3RhdGUiOiJzZXNzaW9uMTIzIiwiYWNyIjoiMSIsInNjb3BlIjoib3BlbmlkIHByb2ZpbGUgZW1haWwifQ.signature";

            // When
            ValidateTokenRequest request = ValidateTokenRequest.builder()
                    .token(jwtToken)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request.getToken()).isEqualTo(jwtToken),
                    () -> assertThat(request.getToken().split("\\.")).hasSize(3)
            );
        }

        @Test
        @DisplayName("Should handle tokens with special characters")
        void shouldHandleTokensWithSpecialCharacters() {
            // Given
            String specialToken = "token_with_!@#$%^&*()_+-=[]{}|;':\",./<>?`~";

            // When
            ValidateTokenRequest request = ValidateTokenRequest.builder()
                    .token(specialToken)
                    .build();

            // Then
            assertThat(request.getToken()).isEqualTo(specialToken);
        }
    }
}