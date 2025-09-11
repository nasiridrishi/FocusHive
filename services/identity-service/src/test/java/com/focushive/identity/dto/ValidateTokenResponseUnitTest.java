package com.focushive.identity.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Comprehensive unit tests for ValidateTokenResponse DTO.
 */
@DisplayName("ValidateTokenResponse DTO Unit Tests")
class ValidateTokenResponseUnitTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Nested
    @DisplayName("Builder Pattern Tests")
    class BuilderPatternTests {

        @Test
        @DisplayName("Should create ValidateTokenResponse using builder")
        void shouldCreateValidateTokenResponseUsingBuilder() {
            // When
            ValidateTokenResponse response = ValidateTokenResponse.builder()
                    .valid(true)
                    .userId("user123")
                    .personaId("persona456")
                    .error(null)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.isValid()).isTrue(),
                    () -> assertThat(response.getUserId()).isEqualTo("user123"),
                    () -> assertThat(response.getPersonaId()).isEqualTo("persona456"),
                    () -> assertThat(response.getError()).isNull()
            );
        }

        @Test
        @DisplayName("Should create error response using builder")
        void shouldCreateErrorResponseUsingBuilder() {
            // When
            ValidateTokenResponse response = ValidateTokenResponse.builder()
                    .valid(false)
                    .userId(null)
                    .personaId(null)
                    .error("Token is expired")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.isValid()).isFalse(),
                    () -> assertThat(response.getUserId()).isNull(),
                    () -> assertThat(response.getPersonaId()).isNull(),
                    () -> assertThat(response.getError()).isEqualTo("Token is expired")
            );
        }

        @Test
        @DisplayName("Should create response with minimal data")
        void shouldCreateResponseWithMinimalData() {
            // When
            ValidateTokenResponse response = ValidateTokenResponse.builder()
                    .valid(true)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.isValid()).isTrue(),
                    () -> assertThat(response.getUserId()).isNull(),
                    () -> assertThat(response.getPersonaId()).isNull(),
                    () -> assertThat(response.getError()).isNull()
            );
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create instance using no-args constructor")
        void shouldCreateInstanceUsingNoArgsConstructor() {
            // When
            ValidateTokenResponse response = new ValidateTokenResponse();

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.isValid()).isFalse(),
                    () -> assertThat(response.getUserId()).isNull(),
                    () -> assertThat(response.getPersonaId()).isNull(),
                    () -> assertThat(response.getError()).isNull()
            );
        }

        @Test
        @DisplayName("Should create instance using all-args constructor")
        void shouldCreateInstanceUsingAllArgsConstructor() {
            // When
            ValidateTokenResponse response = new ValidateTokenResponse(true, "user789", "persona101", null);

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.isValid()).isTrue(),
                    () -> assertThat(response.getUserId()).isEqualTo("user789"),
                    () -> assertThat(response.getPersonaId()).isEqualTo("persona101"),
                    () -> assertThat(response.getError()).isNull()
            );
        }

        @Test
        @DisplayName("Should create error instance using all-args constructor")
        void shouldCreateErrorInstanceUsingAllArgsConstructor() {
            // When
            ValidateTokenResponse response = new ValidateTokenResponse(false, null, null, "Invalid token format");

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.isValid()).isFalse(),
                    () -> assertThat(response.getUserId()).isNull(),
                    () -> assertThat(response.getPersonaId()).isNull(),
                    () -> assertThat(response.getError()).isEqualTo("Invalid token format")
            );
        }

        @Test
        @DisplayName("Should allow modification after creation")
        void shouldAllowModificationAfterCreation() {
            // Given
            ValidateTokenResponse response = new ValidateTokenResponse();

            // When
            response.setValid(true);
            response.setUserId("modified-user");
            response.setPersonaId("modified-persona");
            response.setError("modified-error");

            // Then
            assertAll(
                    () -> assertThat(response.isValid()).isTrue(),
                    () -> assertThat(response.getUserId()).isEqualTo("modified-user"),
                    () -> assertThat(response.getPersonaId()).isEqualTo("modified-persona"),
                    () -> assertThat(response.getError()).isEqualTo("modified-error")
            );
        }
    }

    @Nested
    @DisplayName("Success Response Tests")
    class SuccessResponseTests {

        @Test
        @DisplayName("Should create successful response with user and persona")
        void shouldCreateSuccessfulResponseWithUserAndPersona() {
            // When
            ValidateTokenResponse response = ValidateTokenResponse.builder()
                    .valid(true)
                    .userId("user-12345")
                    .personaId("work-persona-67890")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.isValid()).isTrue(),
                    () -> assertThat(response.getUserId()).isEqualTo("user-12345"),
                    () -> assertThat(response.getPersonaId()).isEqualTo("work-persona-67890"),
                    () -> assertThat(response.getError()).isNull()
            );
        }

        @Test
        @DisplayName("Should create successful response with only user")
        void shouldCreateSuccessfulResponseWithOnlyUser() {
            // When
            ValidateTokenResponse response = ValidateTokenResponse.builder()
                    .valid(true)
                    .userId("user-only-123")
                    .personaId(null)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.isValid()).isTrue(),
                    () -> assertThat(response.getUserId()).isEqualTo("user-only-123"),
                    () -> assertThat(response.getPersonaId()).isNull(),
                    () -> assertThat(response.getError()).isNull()
            );
        }

        @Test
        @DisplayName("Should handle UUID-style identifiers")
        void shouldHandleUuidStyleIdentifiers() {
            // When
            ValidateTokenResponse response = ValidateTokenResponse.builder()
                    .valid(true)
                    .userId("550e8400-e29b-41d4-a716-446655440000")
                    .personaId("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.isValid()).isTrue(),
                    () -> assertThat(response.getUserId()).matches("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$"),
                    () -> assertThat(response.getPersonaId()).matches("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$"),
                    () -> assertThat(response.getError()).isNull()
            );
        }
    }

    @Nested
    @DisplayName("Error Response Tests")
    class ErrorResponseTests {

        @Test
        @DisplayName("Should create error response for expired token")
        void shouldCreateErrorResponseForExpiredToken() {
            // When
            ValidateTokenResponse response = ValidateTokenResponse.builder()
                    .valid(false)
                    .error("Token has expired")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.isValid()).isFalse(),
                    () -> assertThat(response.getUserId()).isNull(),
                    () -> assertThat(response.getPersonaId()).isNull(),
                    () -> assertThat(response.getError()).isEqualTo("Token has expired")
            );
        }

        @Test
        @DisplayName("Should create error response for invalid signature")
        void shouldCreateErrorResponseForInvalidSignature() {
            // When
            ValidateTokenResponse response = ValidateTokenResponse.builder()
                    .valid(false)
                    .error("Invalid token signature")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.isValid()).isFalse(),
                    () -> assertThat(response.getUserId()).isNull(),
                    () -> assertThat(response.getPersonaId()).isNull(),
                    () -> assertThat(response.getError()).isEqualTo("Invalid token signature")
            );
        }

        @Test
        @DisplayName("Should create error response for malformed token")
        void shouldCreateErrorResponseForMalformedToken() {
            // When
            ValidateTokenResponse response = ValidateTokenResponse.builder()
                    .valid(false)
                    .error("Token format is invalid")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.isValid()).isFalse(),
                    () -> assertThat(response.getError()).isEqualTo("Token format is invalid")
            );
        }

        @Test
        @DisplayName("Should handle various error messages")
        void shouldHandleVariousErrorMessages() {
            String[] errorMessages = {
                    "Token has expired",
                    "Invalid token signature",
                    "Token format is invalid",
                    "User not found",
                    "Token has been revoked",
                    "Insufficient permissions",
                    "Token issuer is invalid"
            };

            for (String errorMessage : errorMessages) {
                // When
                ValidateTokenResponse response = ValidateTokenResponse.builder()
                        .valid(false)
                        .error(errorMessage)
                        .build();

                // Then
                assertAll(
                        () -> assertThat(response.isValid()).isFalse(),
                        () -> assertThat(response.getError()).isEqualTo(errorMessage),
                        () -> assertThat(response.getUserId()).isNull(),
                        () -> assertThat(response.getPersonaId()).isNull()
                );
            }
        }
    }

    @Nested
    @DisplayName("JSON Serialization Tests")
    class JsonSerializationTests {

        @Test
        @DisplayName("Should serialize successful response to JSON correctly")
        void shouldSerializeSuccessfulResponseToJsonCorrectly() throws Exception {
            // Given
            ValidateTokenResponse response = ValidateTokenResponse.builder()
                    .valid(true)
                    .userId("test-user-123")
                    .personaId("test-persona-456")
                    .error(null)
                    .build();

            // When
            String json = objectMapper.writeValueAsString(response);

            // Then
            assertAll(
                    () -> assertThat(json).contains("\"valid\":true"),
                    () -> assertThat(json).contains("\"userId\":\"test-user-123\""),
                    () -> assertThat(json).contains("\"personaId\":\"test-persona-456\""),
                    () -> assertThat(json).contains("\"error\":null")
            );
        }

        @Test
        @DisplayName("Should serialize error response to JSON correctly")
        void shouldSerializeErrorResponseToJsonCorrectly() throws Exception {
            // Given
            ValidateTokenResponse response = ValidateTokenResponse.builder()
                    .valid(false)
                    .userId(null)
                    .personaId(null)
                    .error("Token validation failed")
                    .build();

            // When
            String json = objectMapper.writeValueAsString(response);

            // Then
            assertAll(
                    () -> assertThat(json).contains("\"valid\":false"),
                    () -> assertThat(json).contains("\"userId\":null"),
                    () -> assertThat(json).contains("\"personaId\":null"),
                    () -> assertThat(json).contains("\"error\":\"Token validation failed\"")
            );
        }

        @Test
        @DisplayName("Should deserialize successful response from JSON correctly")
        void shouldDeserializeSuccessfulResponseFromJsonCorrectly() throws Exception {
            // Given
            String json = """
                {
                    "valid": true,
                    "userId": "json-user-789",
                    "personaId": "json-persona-101",
                    "error": null
                }
                """;

            // When
            ValidateTokenResponse response = objectMapper.readValue(json, ValidateTokenResponse.class);

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.isValid()).isTrue(),
                    () -> assertThat(response.getUserId()).isEqualTo("json-user-789"),
                    () -> assertThat(response.getPersonaId()).isEqualTo("json-persona-101"),
                    () -> assertThat(response.getError()).isNull()
            );
        }

        @Test
        @DisplayName("Should deserialize error response from JSON correctly")
        void shouldDeserializeErrorResponseFromJsonCorrectly() throws Exception {
            // Given
            String json = """
                {
                    "valid": false,
                    "userId": null,
                    "personaId": null,
                    "error": "Deserialized error message"
                }
                """;

            // When
            ValidateTokenResponse response = objectMapper.readValue(json, ValidateTokenResponse.class);

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.isValid()).isFalse(),
                    () -> assertThat(response.getUserId()).isNull(),
                    () -> assertThat(response.getPersonaId()).isNull(),
                    () -> assertThat(response.getError()).isEqualTo("Deserialized error message")
            );
        }

        @Test
        @DisplayName("Should handle missing fields in JSON")
        void shouldHandleMissingFieldsInJson() throws Exception {
            // Given
            String json = """
                {
                    "valid": true
                }
                """;

            // When
            ValidateTokenResponse response = objectMapper.readValue(json, ValidateTokenResponse.class);

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.isValid()).isTrue(),
                    () -> assertThat(response.getUserId()).isNull(),
                    () -> assertThat(response.getPersonaId()).isNull(),
                    () -> assertThat(response.getError()).isNull()
            );
        }

        @Test
        @DisplayName("Should handle empty JSON object")
        void shouldHandleEmptyJsonObject() throws Exception {
            // Given
            String json = "{}";

            // When
            ValidateTokenResponse response = objectMapper.readValue(json, ValidateTokenResponse.class);

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.isValid()).isFalse(), // boolean defaults to false
                    () -> assertThat(response.getUserId()).isNull(),
                    () -> assertThat(response.getPersonaId()).isNull(),
                    () -> assertThat(response.getError()).isNull()
            );
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when all fields are the same")
        void shouldBeEqualWhenAllFieldsAreTheSame() {
            // Given
            ValidateTokenResponse response1 = ValidateTokenResponse.builder()
                    .valid(true)
                    .userId("same-user")
                    .personaId("same-persona")
                    .error(null)
                    .build();

            ValidateTokenResponse response2 = ValidateTokenResponse.builder()
                    .valid(true)
                    .userId("same-user")
                    .personaId("same-persona")
                    .error(null)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response1).isEqualTo(response2),
                    () -> assertThat(response1.hashCode()).isEqualTo(response2.hashCode())
            );
        }

        @Test
        @DisplayName("Should not be equal when valid flags differ")
        void shouldNotBeEqualWhenValidFlagsDiffer() {
            // Given
            ValidateTokenResponse response1 = ValidateTokenResponse.builder()
                    .valid(true)
                    .userId("user")
                    .build();

            ValidateTokenResponse response2 = ValidateTokenResponse.builder()
                    .valid(false)
                    .userId("user")
                    .build();

            // Then
            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("Should not be equal when user IDs differ")
        void shouldNotBeEqualWhenUserIdsDiffer() {
            // Given
            ValidateTokenResponse response1 = ValidateTokenResponse.builder()
                    .valid(true)
                    .userId("user1")
                    .build();

            ValidateTokenResponse response2 = ValidateTokenResponse.builder()
                    .valid(true)
                    .userId("user2")
                    .build();

            // Then
            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("Should not be equal when persona IDs differ")
        void shouldNotBeEqualWhenPersonaIdsDiffer() {
            // Given
            ValidateTokenResponse response1 = ValidateTokenResponse.builder()
                    .valid(true)
                    .personaId("persona1")
                    .build();

            ValidateTokenResponse response2 = ValidateTokenResponse.builder()
                    .valid(true)
                    .personaId("persona2")
                    .build();

            // Then
            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("Should not be equal when error messages differ")
        void shouldNotBeEqualWhenErrorMessagesDiffer() {
            // Given
            ValidateTokenResponse response1 = ValidateTokenResponse.builder()
                    .valid(false)
                    .error("Error 1")
                    .build();

            ValidateTokenResponse response2 = ValidateTokenResponse.builder()
                    .valid(false)
                    .error("Error 2")
                    .build();

            // Then
            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("Should handle null values in equality")
        void shouldHandleNullValuesInEquality() {
            // Given
            ValidateTokenResponse response1 = new ValidateTokenResponse();
            ValidateTokenResponse response2 = new ValidateTokenResponse();

            // Then
            assertAll(
                    () -> assertThat(response1).isEqualTo(response2),
                    () -> assertThat(response1.hashCode()).isEqualTo(response2.hashCode())
            );
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should provide meaningful string representation for success")
        void shouldProvideMeaningfulStringRepresentationForSuccess() {
            // Given
            ValidateTokenResponse response = ValidateTokenResponse.builder()
                    .valid(true)
                    .userId("toString-user")
                    .personaId("toString-persona")
                    .error(null)
                    .build();

            // When
            String toString = response.toString();

            // Then
            assertAll(
                    () -> assertThat(toString).contains("ValidateTokenResponse"),
                    () -> assertThat(toString).contains("valid=true"),
                    () -> assertThat(toString).contains("userId=toString-user"),
                    () -> assertThat(toString).contains("personaId=toString-persona"),
                    () -> assertThat(toString).contains("error=null")
            );
        }

        @Test
        @DisplayName("Should provide meaningful string representation for error")
        void shouldProvideMeaningfulStringRepresentationForError() {
            // Given
            ValidateTokenResponse response = ValidateTokenResponse.builder()
                    .valid(false)
                    .error("toString error message")
                    .build();

            // When
            String toString = response.toString();

            // Then
            assertAll(
                    () -> assertThat(toString).contains("ValidateTokenResponse"),
                    () -> assertThat(toString).contains("valid=false"),
                    () -> assertThat(toString).contains("error=toString error message")
            );
        }

        @Test
        @DisplayName("Should handle null values in toString")
        void shouldHandleNullValuesInToString() {
            // Given
            ValidateTokenResponse response = new ValidateTokenResponse();

            // When
            String toString = response.toString();

            // Then
            assertAll(
                    () -> assertThat(toString).contains("ValidateTokenResponse"),
                    () -> assertThat(toString).contains("valid=false"),
                    () -> assertThat(toString).contains("userId=null"),
                    () -> assertThat(toString).contains("personaId=null"),
                    () -> assertThat(toString).contains("error=null")
            );
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long user and persona IDs")
        void shouldHandleVeryLongUserAndPersonaIds() {
            // Given
            String longUserId = "user-" + "x".repeat(1000);
            String longPersonaId = "persona-" + "y".repeat(1000);

            ValidateTokenResponse response = ValidateTokenResponse.builder()
                    .valid(true)
                    .userId(longUserId)
                    .personaId(longPersonaId)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getUserId()).hasSize(1005),
                    () -> assertThat(response.getPersonaId()).hasSize(1008),
                    () -> assertThat(response.getUserId()).isEqualTo(longUserId),
                    () -> assertThat(response.getPersonaId()).isEqualTo(longPersonaId)
            );
        }

        @Test
        @DisplayName("Should handle very long error messages")
        void shouldHandleVeryLongErrorMessages() {
            // Given
            String longError = "This is a very long error message: " + "error ".repeat(200);

            ValidateTokenResponse response = ValidateTokenResponse.builder()
                    .valid(false)
                    .error(longError)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getError()).contains("This is a very long error message"),
                    () -> assertThat(response.getError()).isEqualTo(longError),
                    () -> assertThat(response.getError().length()).isGreaterThan(1000)
            );
        }

        @Test
        @DisplayName("Should handle special characters in IDs and messages")
        void shouldHandleSpecialCharactersInIdsAndMessages() {
            // Given
            String specialUserId = "user!@#$%^&*()_+-=[]{}|;':\",./<>?`~";
            String specialPersonaId = "persona©®™€£¥§¶•ªº–—''\"\"…";
            String specialError = "Error with unicode: αβγδε 中文 日本語 한국어 العربية עברית";

            ValidateTokenResponse response = ValidateTokenResponse.builder()
                    .valid(false)
                    .userId(specialUserId)
                    .personaId(specialPersonaId)
                    .error(specialError)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getUserId()).isEqualTo(specialUserId),
                    () -> assertThat(response.getPersonaId()).isEqualTo(specialPersonaId),
                    () -> assertThat(response.getError()).isEqualTo(specialError)
            );
        }

        @Test
        @DisplayName("Should handle mixed success and error scenario")
        void shouldHandleMixedSuccessAndErrorScenario() {
            // Given - This might represent a partial success scenario
            ValidateTokenResponse response = ValidateTokenResponse.builder()
                    .valid(true)
                    .userId("partial-user")
                    .personaId(null) // Maybe persona couldn't be determined
                    .error("Warning: Persona could not be determined")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.isValid()).isTrue(),
                    () -> assertThat(response.getUserId()).isEqualTo("partial-user"),
                    () -> assertThat(response.getPersonaId()).isNull(),
                    () -> assertThat(response.getError()).isEqualTo("Warning: Persona could not be determined")
            );
        }

        @Test
        @DisplayName("Should handle empty string values")
        void shouldHandleEmptyStringValues() {
            // Given
            ValidateTokenResponse response = ValidateTokenResponse.builder()
                    .valid(true)
                    .userId("")
                    .personaId("")
                    .error("")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.isValid()).isTrue(),
                    () -> assertThat(response.getUserId()).isEmpty(),
                    () -> assertThat(response.getPersonaId()).isEmpty(),
                    () -> assertThat(response.getError()).isEmpty()
            );
        }

        @Test
        @DisplayName("Should handle whitespace-only values")
        void shouldHandleWhitespaceOnlyValues() {
            // Given
            ValidateTokenResponse response = ValidateTokenResponse.builder()
                    .valid(false)
                    .userId("   ")
                    .personaId("\t\n\r")
                    .error("  \t  ")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.isValid()).isFalse(),
                    () -> assertThat(response.getUserId()).isEqualTo("   "),
                    () -> assertThat(response.getPersonaId()).isEqualTo("\t\n\r"),
                    () -> assertThat(response.getError()).isEqualTo("  \t  ")
            );
        }
    }
}