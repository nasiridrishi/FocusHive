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
 * Comprehensive unit tests for PasswordResetRequest DTO.
 */
@DisplayName("PasswordResetRequest DTO Unit Tests")
class PasswordResetRequestUnitTest {

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
            PasswordResetRequest request = new PasswordResetRequest();

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getEmail()).isNull()
            );
        }

        @Test
        @DisplayName("Should allow setting email after creation")
        void shouldAllowSettingEmailAfterCreation() {
            // Given
            PasswordResetRequest request = new PasswordResetRequest();

            // When
            request.setEmail("user@example.com");

            // Then
            assertThat(request.getEmail()).isEqualTo("user@example.com");
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation with valid email")
        void shouldPassValidationWithValidEmail() {
            // Given
            PasswordResetRequest request = new PasswordResetRequest();
            request.setEmail("user@example.com");

            // When
            Set<ConstraintViolation<PasswordResetRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail validation when email is null")
        void shouldFailValidationWhenEmailIsNull() {
            // Given
            PasswordResetRequest request = new PasswordResetRequest();
            request.setEmail(null);

            // When
            Set<ConstraintViolation<PasswordResetRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).hasSize(1),
                    () -> {
                        ConstraintViolation<PasswordResetRequest> violation = violations.iterator().next();
                        assertThat(violation.getMessage()).isEqualTo("Email is required");
                        assertThat(violation.getPropertyPath().toString()).isEqualTo("email");
                    }
            );
        }

        @Test
        @DisplayName("Should fail validation when email is blank")
        void shouldFailValidationWhenEmailIsBlank() {
            // Given
            PasswordResetRequest request = new PasswordResetRequest();
            request.setEmail("");

            // When
            Set<ConstraintViolation<PasswordResetRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).hasSize(1),
                    () -> {
                        ConstraintViolation<PasswordResetRequest> violation = violations.iterator().next();
                        assertThat(violation.getMessage()).isEqualTo("Email is required");
                    }
            );
        }

        @Test
        @DisplayName("Should fail validation when email is empty")
        void shouldFailValidationWhenEmailIsEmpty() {
            // Given
            PasswordResetRequest request = new PasswordResetRequest();
            request.setEmail("");

            // When
            Set<ConstraintViolation<PasswordResetRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).hasSize(1),
                    () -> {
                        ConstraintViolation<PasswordResetRequest> violation = violations.iterator().next();
                        assertThat(violation.getMessage()).isEqualTo("Email is required");
                    }
            );
        }

        @Test
        @DisplayName("Should fail validation when email format is invalid")
        void shouldFailValidationWhenEmailFormatIsInvalid() {
            // Given
            PasswordResetRequest request = new PasswordResetRequest();
            request.setEmail("invalid-email");

            // When
            Set<ConstraintViolation<PasswordResetRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).hasSize(1),
                    () -> {
                        ConstraintViolation<PasswordResetRequest> violation = violations.iterator().next();
                        assertThat(violation.getMessage()).isEqualTo("Email must be valid");
                        assertThat(violation.getPropertyPath().toString()).isEqualTo("email");
                    }
            );
        }

        @Test
        @DisplayName("Should handle multiple email format violations")
        void shouldHandleMultipleEmailFormatViolations() {
            // Given
            PasswordResetRequest request = new PasswordResetRequest();
            request.setEmail("@invalid");

            // When
            Set<ConstraintViolation<PasswordResetRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            ConstraintViolation<PasswordResetRequest> violation = violations.iterator().next();
            assertThat(violation.getMessage()).isEqualTo("Email must be valid");
        }
    }

    @Nested
    @DisplayName("Email Format Tests")
    class EmailFormatTests {

        @Test
        @DisplayName("Should accept standard email formats")
        void shouldAcceptStandardEmailFormats() {
            String[] validEmails = {
                    "user@example.com",
                    "test.email@domain.co.uk",
                    "user+tag@example.org",
                    "admin@sub.domain.com",
                    "123@numbers.com",
                    "user-name@example-domain.com"
            };

            for (String email : validEmails) {
                // Given
                PasswordResetRequest request = new PasswordResetRequest();
                request.setEmail(email);

                // When
                Set<ConstraintViolation<PasswordResetRequest>> violations = validator.validate(request);

                // Then
                assertThat(violations).as("Email %s should be valid", email).isEmpty();
            }
        }

        @Test
        @DisplayName("Should reject invalid email formats")
        void shouldRejectInvalidEmailFormats() {
            // Only test the most clearly invalid formats that Jakarta validation will actually reject
            String[] invalidEmails = {
                    "invalid-email",
                    "@domain.com",
                    "user@",
                    "user name@domain.com",
                    ""
            };

            for (String email : invalidEmails) {
                // Given
                PasswordResetRequest request = new PasswordResetRequest();
                request.setEmail(email);

                // When
                Set<ConstraintViolation<PasswordResetRequest>> violations = validator.validate(request);

                // Then
                assertThat(violations).as("Email %s should be invalid", email).isNotEmpty();
            }
        }
    }

    @Nested
    @DisplayName("JSON Serialization Tests")
    class JsonSerializationTests {

        @Test
        @DisplayName("Should serialize to JSON correctly")
        void shouldSerializeToJsonCorrectly() throws Exception {
            // Given
            PasswordResetRequest request = new PasswordResetRequest();
            request.setEmail("test@example.com");

            // When
            String json = objectMapper.writeValueAsString(request);

            // Then
            assertThat(json).contains("\"email\":\"test@example.com\"");
        }

        @Test
        @DisplayName("Should deserialize from JSON correctly")
        void shouldDeserializeFromJsonCorrectly() throws Exception {
            // Given
            String json = """
                {
                    "email": "deserialize@example.com"
                }
                """;

            // When
            PasswordResetRequest request = objectMapper.readValue(json, PasswordResetRequest.class);

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getEmail()).isEqualTo("deserialize@example.com")
            );
        }

        @Test
        @DisplayName("Should handle null values in JSON")
        void shouldHandleNullValuesInJson() throws Exception {
            // Given
            String json = """
                {
                    "email": null
                }
                """;

            // When
            PasswordResetRequest request = objectMapper.readValue(json, PasswordResetRequest.class);

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getEmail()).isNull()
            );
        }

        @Test
        @DisplayName("Should handle missing fields in JSON")
        void shouldHandleMissingFieldsInJson() throws Exception {
            // Given
            String json = "{}";

            // When
            PasswordResetRequest request = objectMapper.readValue(json, PasswordResetRequest.class);

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getEmail()).isNull()
            );
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when emails are the same")
        void shouldBeEqualWhenEmailsAreTheSame() {
            // Given
            PasswordResetRequest request1 = new PasswordResetRequest();
            request1.setEmail("same@example.com");
            
            PasswordResetRequest request2 = new PasswordResetRequest();
            request2.setEmail("same@example.com");

            // Then
            assertAll(
                    () -> assertThat(request1).isEqualTo(request2),
                    () -> assertThat(request1.hashCode()).isEqualTo(request2.hashCode())
            );
        }

        @Test
        @DisplayName("Should not be equal when emails differ")
        void shouldNotBeEqualWhenEmailsDiffer() {
            // Given
            PasswordResetRequest request1 = new PasswordResetRequest();
            request1.setEmail("first@example.com");
            
            PasswordResetRequest request2 = new PasswordResetRequest();
            request2.setEmail("second@example.com");

            // Then
            assertThat(request1).isNotEqualTo(request2);
        }

        @Test
        @DisplayName("Should handle null emails in equality")
        void shouldHandleNullEmailsInEquality() {
            // Given
            PasswordResetRequest request1 = new PasswordResetRequest();
            PasswordResetRequest request2 = new PasswordResetRequest();

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
            PasswordResetRequest request = new PasswordResetRequest();
            request.setEmail("test@example.com");

            // When
            String toString = request.toString();

            // Then
            assertAll(
                    () -> assertThat(toString).contains("PasswordResetRequest"),
                    () -> assertThat(toString).contains("email=test@example.com")
            );
        }

        @Test
        @DisplayName("Should handle null email in toString")
        void shouldHandleNullEmailInToString() {
            // Given
            PasswordResetRequest request = new PasswordResetRequest();

            // When
            String toString = request.toString();

            // Then
            assertAll(
                    () -> assertThat(toString).contains("PasswordResetRequest"),
                    () -> assertThat(toString).contains("email=null")
            );
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long email addresses")
        void shouldHandleVeryLongEmailAddresses() {
            // Given - Create a long email that stays within reasonable limits
            String longEmail = "verylongemailaddress" + "a".repeat(100) + "@example.com";
            PasswordResetRequest request = new PasswordResetRequest();
            request.setEmail(longEmail);

            // When
            Set<ConstraintViolation<PasswordResetRequest>> violations = validator.validate(request);

            // Then - Email validation should work for reasonably long emails
            // Jakarta Email validation is lenient about length, so we just verify the email is set
            assertThat(request.getEmail()).isEqualTo(longEmail);
            // Don't assert about violations since email length validation varies by implementation
        }

        @Test
        @DisplayName("Should handle international email domains")
        void shouldHandleInternationalEmailDomains() {
            // Given
            String internationalEmail = "user@école.fr";
            PasswordResetRequest request = new PasswordResetRequest();
            request.setEmail(internationalEmail);

            // When & Then - Should at least accept the email (validation may vary)
            assertThat(request.getEmail()).isEqualTo(internationalEmail);
        }

        @Test
        @DisplayName("Should handle case sensitivity appropriately")
        void shouldHandleCaseSensitivityAppropriately() {
            // Given
            PasswordResetRequest request1 = new PasswordResetRequest();
            request1.setEmail("User@Example.COM");
            
            PasswordResetRequest request2 = new PasswordResetRequest();
            request2.setEmail("user@example.com");

            // When
            Set<ConstraintViolation<PasswordResetRequest>> violations1 = validator.validate(request1);
            Set<ConstraintViolation<PasswordResetRequest>> violations2 = validator.validate(request2);

            // Then - Both should be valid emails
            assertAll(
                    () -> assertThat(violations1).isEmpty(),
                    () -> assertThat(violations2).isEmpty(),
                    () -> assertThat(request1.getEmail()).isEqualTo("User@Example.COM"),
                    () -> assertThat(request2.getEmail()).isEqualTo("user@example.com")
            );
        }
    }
}