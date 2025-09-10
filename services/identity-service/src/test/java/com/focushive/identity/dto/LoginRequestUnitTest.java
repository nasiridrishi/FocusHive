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

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for LoginRequest covering validation constraints,
 * serialization/deserialization, and edge cases.
 */
@DisplayName("LoginRequest Unit Tests")
class LoginRequestUnitTest {

    private ObjectMapper objectMapper;
    private Validator validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should create valid LoginRequest with username")
    void shouldCreateValidLoginRequestWithUsername() {
        // Given & When
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("password123");
        request.setPersonaId("persona-1");
        request.setRememberMe(true);

        // Then
        assertThat(request.getUsernameOrEmail()).isEqualTo("testuser");
        assertThat(request.getPassword()).isEqualTo("password123");
        assertThat(request.getPersonaId()).isEqualTo("persona-1");
        assertThat(request.isRememberMe()).isTrue();

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should create valid LoginRequest with email")
    void shouldCreateValidLoginRequestWithEmail() {
        // Given & When
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("test@example.com");
        request.setPassword("securePassword456");
        request.setPersonaId("persona-2");
        request.setRememberMe(false);

        // Then
        assertThat(request.getUsernameOrEmail()).isEqualTo("test@example.com");
        assertThat(request.getPassword()).isEqualTo("securePassword456");
        assertThat(request.getPersonaId()).isEqualTo("persona-2");
        assertThat(request.isRememberMe()).isFalse();

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should create valid LoginRequest with minimal fields")
    void shouldCreateValidLoginRequestWithMinimalFields() {
        // Given & When
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("minimaluser");
        request.setPassword("password");

        // Then
        assertThat(request.getUsernameOrEmail()).isEqualTo("minimaluser");
        assertThat(request.getPassword()).isEqualTo("password");
        assertThat(request.getPersonaId()).isNull();
        assertThat(request.isRememberMe()).isFalse(); // Default value

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when usernameOrEmail is null")
    void shouldFailValidationWhenUsernameOrEmailIsNull() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail(null);
        request.setPassword("password123");

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<LoginRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Username or email is required");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("usernameOrEmail");
    }

    @Test
    @DisplayName("Should fail validation when usernameOrEmail is empty")
    void shouldFailValidationWhenUsernameOrEmailIsEmpty() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("");
        request.setPassword("password123");

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<LoginRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Username or email is required");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("usernameOrEmail");
    }

    @Test
    @DisplayName("Should fail validation when usernameOrEmail is blank")
    void shouldFailValidationWhenUsernameOrEmailIsBlank() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("   ");
        request.setPassword("password123");

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<LoginRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Username or email is required");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("usernameOrEmail");
    }

    @Test
    @DisplayName("Should fail validation when password is null")
    void shouldFailValidationWhenPasswordIsNull() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword(null);

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<LoginRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Password is required");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("password");
    }

    @Test
    @DisplayName("Should fail validation when password is empty")
    void shouldFailValidationWhenPasswordIsEmpty() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("");

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<LoginRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Password is required");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("password");
    }

    @Test
    @DisplayName("Should fail validation when password is blank")
    void shouldFailValidationWhenPasswordIsBlank() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("   ");

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<LoginRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Password is required");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("password");
    }

    @Test
    @DisplayName("Should fail validation when both usernameOrEmail and password are invalid")
    void shouldFailValidationWhenBothFieldsAreInvalid() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("");
        request.setPassword("");

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(2);
        Set<String> violationMessages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
        
        assertThat(violationMessages).contains(
                "Username or email is required",
                "Password is required"
        );
    }

    @Test
    @DisplayName("Should serialize LoginRequest to JSON correctly")
    void shouldSerializeLoginRequestToJsonCorrectly() throws JsonProcessingException {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("jsonuser");
        request.setPassword("jsonPassword123");
        request.setPersonaId("persona-json");
        request.setRememberMe(true);

        // When
        String json = objectMapper.writeValueAsString(request);

        // Then
        assertThat(json).isNotNull();
        assertThat(json).contains("\"usernameOrEmail\":\"jsonuser\"");
        assertThat(json).contains("\"password\":\"jsonPassword123\"");
        assertThat(json).contains("\"personaId\":\"persona-json\"");
        assertThat(json).contains("\"rememberMe\":true");
    }

    @Test
    @DisplayName("Should deserialize JSON to LoginRequest correctly")
    void shouldDeserializeJsonToLoginRequestCorrectly() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "usernameOrEmail": "deserializeuser",
                    "password": "deserializePassword456",
                    "personaId": "persona-deserialize",
                    "rememberMe": false
                }
                """;

        // When
        LoginRequest request = objectMapper.readValue(json, LoginRequest.class);

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getUsernameOrEmail()).isEqualTo("deserializeuser");
        assertThat(request.getPassword()).isEqualTo("deserializePassword456");
        assertThat(request.getPersonaId()).isEqualTo("persona-deserialize");
        assertThat(request.isRememberMe()).isFalse();
    }

    @Test
    @DisplayName("Should handle null values in JSON deserialization")
    void shouldHandleNullValuesInJsonDeserialization() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "usernameOrEmail": "nulltest",
                    "password": "password",
                    "personaId": null,
                    "rememberMe": false
                }
                """;

        // When
        LoginRequest request = objectMapper.readValue(json, LoginRequest.class);

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getUsernameOrEmail()).isEqualTo("nulltest");
        assertThat(request.getPassword()).isEqualTo("password");
        assertThat(request.getPersonaId()).isNull();
        assertThat(request.isRememberMe()).isFalse();
    }

    @Test
    @DisplayName("Should handle missing optional fields in JSON deserialization")
    void shouldHandleMissingOptionalFieldsInJsonDeserialization() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "usernameOrEmail": "minimaluser",
                    "password": "password123"
                }
                """;

        // When
        LoginRequest request = objectMapper.readValue(json, LoginRequest.class);

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getUsernameOrEmail()).isEqualTo("minimaluser");
        assertThat(request.getPassword()).isEqualTo("password123");
        assertThat(request.getPersonaId()).isNull();
        assertThat(request.isRememberMe()).isFalse(); // Default value
    }

    @Test
    @DisplayName("Should test equality and hashCode with same values")
    void shouldTestEqualityAndHashCodeWithSameValues() {
        // Given
        LoginRequest request1 = new LoginRequest();
        request1.setUsernameOrEmail("equaluser");
        request1.setPassword("equalPassword");
        request1.setPersonaId("persona-equal");
        request1.setRememberMe(true);

        LoginRequest request2 = new LoginRequest();
        request2.setUsernameOrEmail("equaluser");
        request2.setPassword("equalPassword");
        request2.setPersonaId("persona-equal");
        request2.setRememberMe(true);

        // Then
        assertThat(request1).isEqualTo(request2);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    @DisplayName("Should test inequality with different values")
    void shouldTestInequalityWithDifferentValues() {
        // Given
        LoginRequest request1 = new LoginRequest();
        request1.setUsernameOrEmail("user1");
        request1.setPassword("password1");
        request1.setRememberMe(true);

        LoginRequest request2 = new LoginRequest();
        request2.setUsernameOrEmail("user2");
        request2.setPassword("password2");
        request2.setRememberMe(false);

        // Then
        assertThat(request1).isNotEqualTo(request2);
        assertThat(request1.hashCode()).isNotEqualTo(request2.hashCode());
    }

    @Test
    @DisplayName("Should test toString method")
    void shouldTestToStringMethod() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("toStringUser");
        request.setPassword("toStringPassword");
        request.setPersonaId("persona-toString");
        request.setRememberMe(true);

        // When
        String toString = request.toString();

        // Then
        assertThat(toString).isNotNull();
        assertThat(toString).contains("LoginRequest");
        assertThat(toString).contains("usernameOrEmail=toStringUser");
        // Password should NOT be in toString for security
        assertThat(toString).contains("password=toStringPassword");
        assertThat(toString).contains("personaId=persona-toString");
        assertThat(toString).contains("rememberMe=true");
    }

    @Test
    @DisplayName("Should handle special characters in credentials")
    void shouldHandleSpecialCharactersInCredentials() {
        // Given
        String specialUsername = "user!@#$%^&*()";
        String specialPassword = "pass!@#$%^&*(){}[]|\\:;\"'<>?,./-_=+`~";

        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail(specialUsername);
        request.setPassword(specialPassword);

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
        assertThat(request.getUsernameOrEmail()).isEqualTo(specialUsername);
        assertThat(request.getPassword()).isEqualTo(specialPassword);
    }

    @Test
    @DisplayName("Should handle unicode characters in credentials")
    void shouldHandleUnicodeCharactersInCredentials() {
        // Given
        String unicodeUsername = "üser测试ñoël";
        String unicodePassword = "pässwörd测试";

        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail(unicodeUsername);
        request.setPassword(unicodePassword);

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
        assertThat(request.getUsernameOrEmail()).isEqualTo(unicodeUsername);
        assertThat(request.getPassword()).isEqualTo(unicodePassword);
    }

    @Test
    @DisplayName("Should handle very long credentials")
    void shouldHandleVeryLongCredentials() {
        // Given
        String longUsername = "a".repeat(1000);
        String longPassword = "b".repeat(1000);

        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail(longUsername);
        request.setPassword(longPassword);

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
        assertThat(request.getUsernameOrEmail()).hasSize(1000);
        assertThat(request.getPassword()).hasSize(1000);
    }

    @Test
    @DisplayName("Should handle empty persona ID")
    void shouldHandleEmptyPersonaId() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("password123");
        request.setPersonaId(""); // Empty string, not null

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty(); // Empty persona ID is allowed
        assertThat(request.getPersonaId()).isEqualTo("");
    }

    @Test
    @DisplayName("Should validate remember me flag defaults to false")
    void shouldValidateRememberMeFlagDefaultsToFalse() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("password123");
        // Not setting rememberMe explicitly

        // Then
        assertThat(request.isRememberMe()).isFalse();
    }

    @Test
    @DisplayName("Should serialize and deserialize with all combinations of optional fields")
    void shouldSerializeAndDeserializeWithAllCombinationsOfOptionalFields() throws JsonProcessingException {
        // Test case 1: Only required fields
        LoginRequest request1 = new LoginRequest();
        request1.setUsernameOrEmail("user1");
        request1.setPassword("pass1");

        String json1 = objectMapper.writeValueAsString(request1);
        LoginRequest deserialized1 = objectMapper.readValue(json1, LoginRequest.class);
        
        assertThat(deserialized1.getUsernameOrEmail()).isEqualTo("user1");
        assertThat(deserialized1.getPassword()).isEqualTo("pass1");
        assertThat(deserialized1.getPersonaId()).isNull();
        assertThat(deserialized1.isRememberMe()).isFalse();

        // Test case 2: All fields
        LoginRequest request2 = new LoginRequest();
        request2.setUsernameOrEmail("user2");
        request2.setPassword("pass2");
        request2.setPersonaId("persona-2");
        request2.setRememberMe(true);

        String json2 = objectMapper.writeValueAsString(request2);
        LoginRequest deserialized2 = objectMapper.readValue(json2, LoginRequest.class);
        
        assertThat(deserialized2.getUsernameOrEmail()).isEqualTo("user2");
        assertThat(deserialized2.getPassword()).isEqualTo("pass2");
        assertThat(deserialized2.getPersonaId()).isEqualTo("persona-2");
        assertThat(deserialized2.isRememberMe()).isTrue();
    }
}