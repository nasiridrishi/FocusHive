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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Comprehensive unit tests for PasswordResetConfirmRequest DTO.
 */
@DisplayName("PasswordResetConfirmRequest DTO Unit Tests")
class PasswordResetConfirmRequestUnitTest {

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
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("Should create instance using no-args constructor")
        void shouldCreateInstanceUsingNoArgsConstructor() {
            // When
            PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getToken()).isNull(),
                    () -> assertThat(request.getNewPassword()).isNull(),
                    () -> assertThat(request.getConfirmPassword()).isNull()
            );
        }

        @Test
        @DisplayName("Should allow setting all fields after creation")
        void shouldAllowSettingAllFieldsAfterCreation() {
            // Given
            PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();

            // When
            request.setToken("reset-token-123");
            request.setNewPassword("newSecurePassword123");
            request.setConfirmPassword("newSecurePassword123");

            // Then
            assertAll(
                    () -> assertThat(request.getToken()).isEqualTo("reset-token-123"),
                    () -> assertThat(request.getNewPassword()).isEqualTo("newSecurePassword123"),
                    () -> assertThat(request.getConfirmPassword()).isEqualTo("newSecurePassword123")
            );
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should pass validation with valid token")
        void shouldPassValidationWithValidToken() {
            // Given
            PasswordResetConfirmRequest request = createValidRequest();

            // When
            Set<ConstraintViolation<PasswordResetConfirmRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail validation when token is null")
        void shouldFailValidationWhenTokenIsNull() {
            // Given
            PasswordResetConfirmRequest request = createValidRequest();
            request.setToken(null);

            // When
            Set<ConstraintViolation<PasswordResetConfirmRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            ConstraintViolation<PasswordResetConfirmRequest> violation = violations.iterator().next();
            assertAll(
                    () -> assertThat(violation.getMessage()).isEqualTo("Reset token is required"),
                    () -> assertThat(violation.getPropertyPath().toString()).isEqualTo("token")
            );
        }

        @Test
        @DisplayName("Should fail validation when token is blank")
        void shouldFailValidationWhenTokenIsBlank() {
            // Given
            PasswordResetConfirmRequest request = createValidRequest();
            request.setToken("   ");

            // When
            Set<ConstraintViolation<PasswordResetConfirmRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            ConstraintViolation<PasswordResetConfirmRequest> violation = violations.iterator().next();
            assertThat(violation.getMessage()).isEqualTo("Reset token is required");
        }

        @Test
        @DisplayName("Should fail validation when token is empty")
        void shouldFailValidationWhenTokenIsEmpty() {
            // Given
            PasswordResetConfirmRequest request = createValidRequest();
            request.setToken("");

            // When
            Set<ConstraintViolation<PasswordResetConfirmRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            ConstraintViolation<PasswordResetConfirmRequest> violation = violations.iterator().next();
            assertThat(violation.getMessage()).isEqualTo("Reset token is required");
        }
    }

    @Nested
    @DisplayName("Password Validation Tests")
    class PasswordValidationTests {

        @Test
        @DisplayName("Should pass validation with valid password")
        void shouldPassValidationWithValidPassword() {
            // Given
            PasswordResetConfirmRequest request = createValidRequest();
            request.setNewPassword("validPassword123");
            request.setConfirmPassword("validPassword123");

            // When
            Set<ConstraintViolation<PasswordResetConfirmRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail validation when new password is null")
        void shouldFailValidationWhenNewPasswordIsNull() {
            // Given
            PasswordResetConfirmRequest request = createValidRequest();
            request.setNewPassword(null);

            // When
            Set<ConstraintViolation<PasswordResetConfirmRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            ConstraintViolation<PasswordResetConfirmRequest> violation = violations.iterator().next();
            assertAll(
                    () -> assertThat(violation.getMessage()).isEqualTo("New password is required"),
                    () -> assertThat(violation.getPropertyPath().toString()).isEqualTo("newPassword")
            );
        }

        @Test
        @DisplayName("Should fail validation when new password is blank")
        void shouldFailValidationWhenNewPasswordIsBlank() {
            // Given
            PasswordResetConfirmRequest request = createValidRequest();
            request.setNewPassword("");

            // When
            Set<ConstraintViolation<PasswordResetConfirmRequest>> violations = validator.validate(request);

            // Then - Empty password triggers both @NotBlank and @Size validations
            assertThat(violations).hasSize(2);
            Set<String> messages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
            assertThat(messages).containsAnyOf("New password is required", "Password must be at least 8 characters");
        }

        @Test
        @DisplayName("Should fail validation when new password is too short")
        void shouldFailValidationWhenNewPasswordIsTooShort() {
            // Given
            PasswordResetConfirmRequest request = createValidRequest();
            request.setNewPassword("short");
            request.setConfirmPassword("short");

            // When
            Set<ConstraintViolation<PasswordResetConfirmRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            ConstraintViolation<PasswordResetConfirmRequest> violation = violations.iterator().next();
            assertAll(
                    () -> assertThat(violation.getMessage()).isEqualTo("Password must be at least 8 characters"),
                    () -> assertThat(violation.getPropertyPath().toString()).isEqualTo("newPassword")
            );
        }

        @Test
        @DisplayName("Should pass validation with exactly 8 character password")
        void shouldPassValidationWithExactly8CharacterPassword() {
            // Given
            PasswordResetConfirmRequest request = createValidRequest();
            request.setNewPassword("8charPwd");
            request.setConfirmPassword("8charPwd");

            // When
            Set<ConstraintViolation<PasswordResetConfirmRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should handle various password lengths")
        void shouldHandleVariousPasswordLengths() {
            String[] passwords = {
                    "8charPwd",           // Minimum length
                    "mediumLengthPassword123",  // Medium length
                    "veryLongPasswordWithManyCharacters12345678901234567890"  // Long password
            };

            for (String password : passwords) {
                // Given
                PasswordResetConfirmRequest request = createValidRequest();
                request.setNewPassword(password);
                request.setConfirmPassword(password);

                // When
                Set<ConstraintViolation<PasswordResetConfirmRequest>> violations = validator.validate(request);

                // Then
                assertThat(violations).as("Password '%s' should be valid", password).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Confirm Password Validation Tests")
    class ConfirmPasswordValidationTests {

        @Test
        @DisplayName("Should fail validation when confirm password is null")
        void shouldFailValidationWhenConfirmPasswordIsNull() {
            // Given
            PasswordResetConfirmRequest request = createValidRequest();
            request.setConfirmPassword(null);

            // When
            Set<ConstraintViolation<PasswordResetConfirmRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            ConstraintViolation<PasswordResetConfirmRequest> violation = violations.iterator().next();
            assertAll(
                    () -> assertThat(violation.getMessage()).isEqualTo("Password confirmation is required"),
                    () -> assertThat(violation.getPropertyPath().toString()).isEqualTo("confirmPassword")
            );
        }

        @Test
        @DisplayName("Should fail validation when confirm password is blank")
        void shouldFailValidationWhenConfirmPasswordIsBlank() {
            // Given
            PasswordResetConfirmRequest request = createValidRequest();
            request.setConfirmPassword("   ");

            // When
            Set<ConstraintViolation<PasswordResetConfirmRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            ConstraintViolation<PasswordResetConfirmRequest> violation = violations.iterator().next();
            assertThat(violation.getMessage()).isEqualTo("Password confirmation is required");
        }

        @Test
        @DisplayName("Should fail validation when confirm password is empty")
        void shouldFailValidationWhenConfirmPasswordIsEmpty() {
            // Given
            PasswordResetConfirmRequest request = createValidRequest();
            request.setConfirmPassword("");

            // When
            Set<ConstraintViolation<PasswordResetConfirmRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            ConstraintViolation<PasswordResetConfirmRequest> violation = violations.iterator().next();
            assertThat(violation.getMessage()).isEqualTo("Password confirmation is required");
        }
    }

    @Nested
    @DisplayName("Multiple Field Validation Tests")
    class MultipleFieldValidationTests {

        @Test
        @DisplayName("Should handle multiple validation errors")
        void shouldHandleMultipleValidationErrors() {
            // Given
            PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
            request.setToken(null);
            request.setNewPassword(null);
            request.setConfirmPassword(null);

            // When
            Set<ConstraintViolation<PasswordResetConfirmRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(3);
            
            Set<String> messages = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(java.util.stream.Collectors.toSet());
            
            assertThat(messages).containsExactlyInAnyOrder(
                    "Reset token is required",
                    "New password is required",
                    "Password confirmation is required"
            );
        }

        @Test
        @DisplayName("Should handle password too short with other valid fields")
        void shouldHandlePasswordTooShortWithOtherValidFields() {
            // Given
            PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
            request.setToken("valid-token");
            request.setNewPassword("short");
            request.setConfirmPassword("different-short");

            // When
            Set<ConstraintViolation<PasswordResetConfirmRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            ConstraintViolation<PasswordResetConfirmRequest> violation = violations.iterator().next();
            assertThat(violation.getMessage()).isEqualTo("Password must be at least 8 characters");
        }
    }

    @Nested
    @DisplayName("JSON Serialization Tests")
    class JsonSerializationTests {

        @Test
        @DisplayName("Should serialize to JSON correctly")
        void shouldSerializeToJsonCorrectly() throws Exception {
            // Given
            PasswordResetConfirmRequest request = createValidRequest();

            // When
            String json = objectMapper.writeValueAsString(request);

            // Then
            assertAll(
                    () -> assertThat(json).contains("\"token\":\"reset-token-123\""),
                    () -> assertThat(json).contains("\"newPassword\":\"newPassword123\""),
                    () -> assertThat(json).contains("\"confirmPassword\":\"newPassword123\"")
            );
        }

        @Test
        @DisplayName("Should deserialize from JSON correctly")
        void shouldDeserializeFromJsonCorrectly() throws Exception {
            // Given
            String json = """
                {
                    "token": "json-token-456",
                    "newPassword": "jsonPassword789",
                    "confirmPassword": "jsonPassword789"
                }
                """;

            // When
            PasswordResetConfirmRequest request = objectMapper.readValue(json, PasswordResetConfirmRequest.class);

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getToken()).isEqualTo("json-token-456"),
                    () -> assertThat(request.getNewPassword()).isEqualTo("jsonPassword789"),
                    () -> assertThat(request.getConfirmPassword()).isEqualTo("jsonPassword789")
            );
        }

        @Test
        @DisplayName("Should handle null values in JSON")
        void shouldHandleNullValuesInJson() throws Exception {
            // Given
            String json = """
                {
                    "token": null,
                    "newPassword": null,
                    "confirmPassword": null
                }
                """;

            // When
            PasswordResetConfirmRequest request = objectMapper.readValue(json, PasswordResetConfirmRequest.class);

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getToken()).isNull(),
                    () -> assertThat(request.getNewPassword()).isNull(),
                    () -> assertThat(request.getConfirmPassword()).isNull()
            );
        }

        @Test
        @DisplayName("Should handle missing fields in JSON")
        void shouldHandleMissingFieldsInJson() throws Exception {
            // Given
            String json = "{}";

            // When
            PasswordResetConfirmRequest request = objectMapper.readValue(json, PasswordResetConfirmRequest.class);

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getToken()).isNull(),
                    () -> assertThat(request.getNewPassword()).isNull(),
                    () -> assertThat(request.getConfirmPassword()).isNull()
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
            PasswordResetConfirmRequest request1 = createValidRequest();
            PasswordResetConfirmRequest request2 = createValidRequest();

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
            PasswordResetConfirmRequest request1 = createValidRequest();
            PasswordResetConfirmRequest request2 = createValidRequest();
            request2.setToken("different-token");

            // Then
            assertThat(request1).isNotEqualTo(request2);
        }

        @Test
        @DisplayName("Should not be equal when passwords differ")
        void shouldNotBeEqualWhenPasswordsDiffer() {
            // Given
            PasswordResetConfirmRequest request1 = createValidRequest();
            PasswordResetConfirmRequest request2 = createValidRequest();
            request2.setNewPassword("differentPassword123");

            // Then
            assertThat(request1).isNotEqualTo(request2);
        }

        @Test
        @DisplayName("Should handle null values in equality")
        void shouldHandleNullValuesInEquality() {
            // Given
            PasswordResetConfirmRequest request1 = new PasswordResetConfirmRequest();
            PasswordResetConfirmRequest request2 = new PasswordResetConfirmRequest();

            // Then
            assertAll(
                    () -> assertThat(request1).isEqualTo(request2),
                    () -> assertThat(request1.hashCode()).isEqualTo(request2.hashCode())
            );
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should provide meaningful string representation")
        void shouldProvideMeaningfulStringRepresentation() {
            // Given
            PasswordResetConfirmRequest request = createValidRequest();

            // When
            String toString = request.toString();

            // Then
            assertAll(
                    () -> assertThat(toString).contains("PasswordResetConfirmRequest"),
                    () -> assertThat(toString).contains("token=reset-token-123"),
                    // Note: Password fields should be included but may be masked for security
                    () -> assertThat(toString).containsAnyOf("newPassword", "confirmPassword")
            );
        }

        @Test
        @DisplayName("Should handle null values in toString")
        void shouldHandleNullValuesInToString() {
            // Given
            PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();

            // When
            String toString = request.toString();

            // Then
            assertAll(
                    () -> assertThat(toString).contains("PasswordResetConfirmRequest"),
                    () -> assertThat(toString).contains("token=null"),
                    () -> assertThat(toString).contains("newPassword=null"),
                    () -> assertThat(toString).contains("confirmPassword=null")
            );
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle password mismatch scenarios")
        void shouldHandlePasswordMismatchScenarios() {
            // Given
            PasswordResetConfirmRequest request = createValidRequest();
            request.setNewPassword("password123");
            request.setConfirmPassword("differentPassword456");

            // When - Note: This DTO only validates individual fields, not password matching
            Set<ConstraintViolation<PasswordResetConfirmRequest>> violations = validator.validate(request);

            // Then - Field-level validation should pass (password matching would be business logic)
            assertThat(violations).isEmpty();
            assertAll(
                    () -> assertThat(request.getNewPassword()).isEqualTo("password123"),
                    () -> assertThat(request.getConfirmPassword()).isEqualTo("differentPassword456")
            );
        }

        @Test
        @DisplayName("Should handle very long tokens")
        void shouldHandleVeryLongTokens() {
            // Given
            String longToken = "very-long-reset-token-" + "x".repeat(1000);
            PasswordResetConfirmRequest request = createValidRequest();
            request.setToken(longToken);

            // When
            Set<ConstraintViolation<PasswordResetConfirmRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).isEmpty(),
                    () -> assertThat(request.getToken()).isEqualTo(longToken)
            );
        }

        @Test
        @DisplayName("Should handle special characters in passwords")
        void shouldHandleSpecialCharactersInPasswords() {
            // Given
            String specialPassword = "P@ssw0rd!#$%^&*()_+-=[]{}|;':\",./<>?`~";
            PasswordResetConfirmRequest request = createValidRequest();
            request.setNewPassword(specialPassword);
            request.setConfirmPassword(specialPassword);

            // When
            Set<ConstraintViolation<PasswordResetConfirmRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).isEmpty(),
                    () -> assertThat(request.getNewPassword()).isEqualTo(specialPassword),
                    () -> assertThat(request.getConfirmPassword()).isEqualTo(specialPassword)
            );
        }

        @Test
        @DisplayName("Should handle Unicode characters in passwords")
        void shouldHandleUnicodeCharactersInPasswords() {
            // Given
            String unicodePassword = "pässwörd123αβγδε中文日本語";
            PasswordResetConfirmRequest request = createValidRequest();
            request.setNewPassword(unicodePassword);
            request.setConfirmPassword(unicodePassword);

            // When
            Set<ConstraintViolation<PasswordResetConfirmRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).isEmpty(),
                    () -> assertThat(request.getNewPassword()).isEqualTo(unicodePassword),
                    () -> assertThat(request.getConfirmPassword()).isEqualTo(unicodePassword)
            );
        }

        @Test
        @DisplayName("Should handle boundary length passwords")
        void shouldHandleBoundaryLengthPasswords() {
            // Given - 7 characters (just under minimum)
            PasswordResetConfirmRequest request1 = createValidRequest();
            request1.setNewPassword("1234567");
            request1.setConfirmPassword("1234567");

            // Given - 8 characters (exactly minimum)
            PasswordResetConfirmRequest request2 = createValidRequest();
            request2.setNewPassword("12345678");
            request2.setConfirmPassword("12345678");

            // When
            Set<ConstraintViolation<PasswordResetConfirmRequest>> violations1 = validator.validate(request1);
            Set<ConstraintViolation<PasswordResetConfirmRequest>> violations2 = validator.validate(request2);

            // Then
            assertAll(
                    () -> assertThat(violations1).hasSize(1),
                    () -> assertThat(violations1.iterator().next().getMessage()).isEqualTo("Password must be at least 8 characters"),
                    () -> assertThat(violations2).isEmpty()
            );
        }
    }

    /**
     * Helper method to create a valid PasswordResetConfirmRequest for testing.
     */
    private PasswordResetConfirmRequest createValidRequest() {
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken("reset-token-123");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");
        return request;
    }
}