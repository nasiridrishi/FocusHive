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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for RegisterRequest covering validation constraints,
 * serialization/deserialization, and edge cases.
 */
@DisplayName("RegisterRequest Unit Tests")
class RegisterRequestUnitTest {

    private ObjectMapper objectMapper;
    private Validator validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should create valid RegisterRequest with all required fields")
    void shouldCreateValidRegisterRequestWithAllRequiredFields() {
        // Given & When
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setFirstName("John");
        request.setLastName("Doe");

        // Then
        assertThat(request.getEmail()).isEqualTo("test@example.com");
        assertThat(request.getUsername()).isEqualTo("testuser");
        assertThat(request.getPassword()).isEqualTo("password123");
        assertThat(request.getFirstName()).isEqualTo("John");
        assertThat(request.getLastName()).isEqualTo("Doe");
        assertThat(request.getPersonaType()).isEqualTo("PERSONAL"); // Default value
        assertThat(request.getPersonaName()).isNull();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should create valid RegisterRequest with all fields including optional ones")
    void shouldCreateValidRegisterRequestWithAllFields() {
        // Given & When
        RegisterRequest request = new RegisterRequest();
        request.setEmail("complete@example.com");
        request.setUsername("completeuser");
        request.setPassword("securePassword123");
        request.setFirstName("Jane");
        request.setLastName("Smith");
        request.setPersonaType("PROFESSIONAL");
        request.setPersonaName("Work Profile");

        // Then
        assertThat(request.getEmail()).isEqualTo("complete@example.com");
        assertThat(request.getUsername()).isEqualTo("completeuser");
        assertThat(request.getPassword()).isEqualTo("securePassword123");
        assertThat(request.getFirstName()).isEqualTo("Jane");
        assertThat(request.getLastName()).isEqualTo("Smith");
        assertThat(request.getPersonaType()).isEqualTo("PROFESSIONAL");
        assertThat(request.getPersonaName()).isEqualTo("Work Profile");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    // Email validation tests
    @Test
    @DisplayName("Should fail validation when email is null")
    void shouldFailValidationWhenEmailIsNull() {
        // Given
        RegisterRequest request = createValidRequest();
        request.setEmail(null);

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<RegisterRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Email is required");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("email");
    }

    @Test
    @DisplayName("Should fail validation when email is empty")
    void shouldFailValidationWhenEmailIsEmpty() {
        // Given
        RegisterRequest request = createValidRequest();
        request.setEmail("");

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<RegisterRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Email is required");
    }

    @Test
    @DisplayName("Should fail validation when email is invalid format")
    void shouldFailValidationWhenEmailIsInvalidFormat() {
        // Given
        RegisterRequest request = createValidRequest();
        request.setEmail("invalid-email");

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<RegisterRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Email must be valid");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("email");
    }

    @Test
    @DisplayName("Should accept various valid email formats")
    void shouldAcceptVariousValidEmailFormats() {
        String[] validEmails = {
            "test@example.com",
            "user.name@domain.org",
            "user+tag@domain.co.uk",
            "123@domain.net",
            "test-email@sub.domain.com"
        };

        for (String email : validEmails) {
            RegisterRequest request = createValidRequest();
            request.setEmail(email);

            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
            assertThat(violations)
                .as("Email %s should be valid", email)
                .isEmpty();
        }
    }

    // Username validation tests
    @Test
    @DisplayName("Should fail validation when username is null")
    void shouldFailValidationWhenUsernameIsNull() {
        // Given
        RegisterRequest request = createValidRequest();
        request.setUsername(null);

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<RegisterRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Username is required");
    }

    @Test
    @DisplayName("Should fail validation when username is too short")
    void shouldFailValidationWhenUsernameIsTooShort() {
        // Given
        RegisterRequest request = createValidRequest();
        request.setUsername("ab"); // Only 2 characters

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<RegisterRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Username must be between 3 and 50 characters");
    }

    @Test
    @DisplayName("Should fail validation when username is too long")
    void shouldFailValidationWhenUsernameIsTooLong() {
        // Given
        RegisterRequest request = createValidRequest();
        request.setUsername("a".repeat(51)); // 51 characters

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<RegisterRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Username must be between 3 and 50 characters");
    }

    @Test
    @DisplayName("Should accept username at boundary lengths")
    void shouldAcceptUsernameAtBoundaryLengths() {
        // Test minimum length (3 characters)
        RegisterRequest request1 = createValidRequest();
        request1.setUsername("abc");
        assertThat(validator.validate(request1)).isEmpty();

        // Test maximum length (50 characters)
        RegisterRequest request2 = createValidRequest();
        request2.setUsername("a".repeat(50));
        assertThat(validator.validate(request2)).isEmpty();
    }

    // Password validation tests
    @Test
    @DisplayName("Should fail validation when password is null")
    void shouldFailValidationWhenPasswordIsNull() {
        // Given
        RegisterRequest request = createValidRequest();
        request.setPassword(null);

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<RegisterRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Password is required");
    }

    @Test
    @DisplayName("Should fail validation when password is too short")
    void shouldFailValidationWhenPasswordIsTooShort() {
        // Given
        RegisterRequest request = createValidRequest();
        request.setPassword("1234567"); // Only 7 characters

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<RegisterRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Password must be at least 8 characters");
    }

    @Test
    @DisplayName("Should accept password at minimum length")
    void shouldAcceptPasswordAtMinimumLength() {
        // Given
        RegisterRequest request = createValidRequest();
        request.setPassword("12345678"); // Exactly 8 characters

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    // First name validation tests
    @Test
    @DisplayName("Should fail validation when first name is null")
    void shouldFailValidationWhenFirstNameIsNull() {
        // Given
        RegisterRequest request = createValidRequest();
        request.setFirstName(null);

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<RegisterRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("First name is required");
    }

    @Test
    @DisplayName("Should fail validation when first name is too long")
    void shouldFailValidationWhenFirstNameIsTooLong() {
        // Given
        RegisterRequest request = createValidRequest();
        request.setFirstName("a".repeat(51)); // 51 characters

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<RegisterRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("First name must not exceed 50 characters");
    }

    // Last name validation tests
    @Test
    @DisplayName("Should fail validation when last name is null")
    void shouldFailValidationWhenLastNameIsNull() {
        // Given
        RegisterRequest request = createValidRequest();
        request.setLastName(null);

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<RegisterRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Last name is required");
    }

    @Test
    @DisplayName("Should fail validation when last name is too long")
    void shouldFailValidationWhenLastNameIsTooLong() {
        // Given
        RegisterRequest request = createValidRequest();
        request.setLastName("a".repeat(51)); // 51 characters

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<RegisterRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Last name must not exceed 50 characters");
    }

    @Test
    @DisplayName("Should fail validation with multiple constraint violations")
    void shouldFailValidationWithMultipleConstraintViolations() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("invalid-email");
        request.setUsername("ab"); // Too short
        request.setPassword("short"); // Too short
        request.setFirstName("a".repeat(51)); // Too long
        request.setLastName(""); // Empty

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(5);
        Set<String> violationMessages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertThat(violationMessages).contains(
                "Email must be valid",
                "Username must be between 3 and 50 characters",
                "Password must be at least 8 characters",
                "First name must not exceed 50 characters",
                "Last name is required"
        );
    }

    @Test
    @DisplayName("Should serialize RegisterRequest to JSON correctly")
    void shouldSerializeRegisterRequestToJsonCorrectly() throws JsonProcessingException {
        // Given
        RegisterRequest request = createValidRequest();
        request.setPersonaType("PROFESSIONAL");
        request.setPersonaName("Work Profile");

        // When
        String json = objectMapper.writeValueAsString(request);

        // Then
        assertThat(json).isNotNull();
        assertThat(json).contains("\"email\":\"test@example.com\"");
        assertThat(json).contains("\"username\":\"testuser\"");
        assertThat(json).contains("\"password\":\"password123\"");
        assertThat(json).contains("\"firstName\":\"John\"");
        assertThat(json).contains("\"lastName\":\"Doe\"");
        assertThat(json).contains("\"personaType\":\"PROFESSIONAL\"");
        assertThat(json).contains("\"personaName\":\"Work Profile\"");
    }

    @Test
    @DisplayName("Should deserialize JSON to RegisterRequest correctly")
    void shouldDeserializeJsonToRegisterRequestCorrectly() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "email": "deserialize@example.com",
                    "username": "deserializeuser",
                    "password": "deserializePassword123",
                    "firstName": "Jane",
                    "lastName": "Smith",
                    "personaType": "ACADEMIC",
                    "personaName": "Study Profile"
                }
                """;

        // When
        RegisterRequest request = objectMapper.readValue(json, RegisterRequest.class);

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getEmail()).isEqualTo("deserialize@example.com");
        assertThat(request.getUsername()).isEqualTo("deserializeuser");
        assertThat(request.getPassword()).isEqualTo("deserializePassword123");
        assertThat(request.getFirstName()).isEqualTo("Jane");
        assertThat(request.getLastName()).isEqualTo("Smith");
        assertThat(request.getPersonaType()).isEqualTo("ACADEMIC");
        assertThat(request.getPersonaName()).isEqualTo("Study Profile");
    }

    @Test
    @DisplayName("Should handle default values in deserialization")
    void shouldHandleDefaultValuesInDeserialization() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "email": "minimal@example.com",
                    "username": "minimaluser",
                    "password": "minimalPassword123",
                    "firstName": "Min",
                    "lastName": "User"
                }
                """;

        // When
        RegisterRequest request = objectMapper.readValue(json, RegisterRequest.class);

        // Then
        assertThat(request.getPersonaType()).isEqualTo("PERSONAL"); // Default value
        assertThat(request.getPersonaName()).isNull();
    }

    @Test
    @DisplayName("Should test equality and hashCode with same values")
    void shouldTestEqualityAndHashCodeWithSameValues() {
        // Given
        RegisterRequest request1 = createValidRequest();
        RegisterRequest request2 = createValidRequest();

        // Then
        assertThat(request1).isEqualTo(request2);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    @DisplayName("Should test inequality with different values")
    void shouldTestInequalityWithDifferentValues() {
        // Given
        RegisterRequest request1 = createValidRequest();
        RegisterRequest request2 = createValidRequest();
        request2.setEmail("different@example.com");

        // Then
        assertThat(request1).isNotEqualTo(request2);
        assertThat(request1.hashCode()).isNotEqualTo(request2.hashCode());
    }

    @Test
    @DisplayName("Should test toString method")
    void shouldTestToStringMethod() {
        // Given
        RegisterRequest request = createValidRequest();
        request.setPersonaType("PROFESSIONAL");
        request.setPersonaName("Work Profile");

        // When
        String toString = request.toString();

        // Then
        assertThat(toString).isNotNull();
        assertThat(toString).contains("RegisterRequest");
        assertThat(toString).contains("email=test@example.com");
        assertThat(toString).contains("username=testuser");
        // Password should be in toString (though in production you might want to mask it)
        assertThat(toString).contains("password=password123");
        assertThat(toString).contains("firstName=John");
        assertThat(toString).contains("lastName=Doe");
        assertThat(toString).contains("personaType=PROFESSIONAL");
        assertThat(toString).contains("personaName=Work Profile");
    }

    @Test
    @DisplayName("Should handle special characters in all fields")
    void shouldHandleSpecialCharactersInAllFields() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("special+test@example.com");
        request.setUsername("user_123");
        request.setPassword("pass!@#$%^&*()123");
        request.setFirstName("Jean-Pierre");
        request.setLastName("O'Connor");
        request.setPersonaType("SPECIAL_TYPE");
        request.setPersonaName("Special Profile!");

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should handle unicode characters")
    void shouldHandleUnicodeCharacters() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("unicode@测试.com");
        request.setUsername("üser123");
        request.setPassword("pässwörd123");
        request.setFirstName("François");
        request.setLastName("José");
        request.setPersonaType("ACADÉMIQUE");
        request.setPersonaName("Profil académique");

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
        assertThat(request.getFirstName()).isEqualTo("François");
        assertThat(request.getLastName()).isEqualTo("José");
    }

    @Test
    @DisplayName("Should handle null optional fields gracefully")
    void shouldHandleNullOptionalFieldsGracefully() {
        // Given
        RegisterRequest request = createValidRequest();
        request.setPersonaName(null); // Optional field

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
        assertThat(request.getPersonaName()).isNull();
        assertThat(request.getPersonaType()).isEqualTo("PERSONAL"); // Default should remain
    }

    @Test
    @DisplayName("Should validate persona type can be changed from default")
    void shouldValidatePersonaTypeCanBeChangedFromDefault() {
        // Given
        RegisterRequest request = createValidRequest();
        request.setPersonaType("CUSTOM_TYPE");

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
        assertThat(request.getPersonaType()).isEqualTo("CUSTOM_TYPE");
    }

    @Test
    @DisplayName("Should handle empty string for optional fields")
    void shouldHandleEmptyStringForOptionalFields() {
        // Given
        RegisterRequest request = createValidRequest();
        request.setPersonaName(""); // Empty string for optional field

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
        assertThat(request.getPersonaName()).isEqualTo("");
    }

    @Test
    @DisplayName("Should serialize and deserialize maintaining all data integrity")
    void shouldSerializeAndDeserializeMaintainingAllDataIntegrity() throws JsonProcessingException {
        // Given
        RegisterRequest originalRequest = createValidRequest();
        originalRequest.setPersonaType("PROFESSIONAL");
        originalRequest.setPersonaName("Work Profile");

        // When
        String json = objectMapper.writeValueAsString(originalRequest);
        RegisterRequest deserializedRequest = objectMapper.readValue(json, RegisterRequest.class);

        // Then
        assertThat(deserializedRequest).isEqualTo(originalRequest);
        assertThat(deserializedRequest.getEmail()).isEqualTo(originalRequest.getEmail());
        assertThat(deserializedRequest.getUsername()).isEqualTo(originalRequest.getUsername());
        assertThat(deserializedRequest.getPassword()).isEqualTo(originalRequest.getPassword());
        assertThat(deserializedRequest.getFirstName()).isEqualTo(originalRequest.getFirstName());
        assertThat(deserializedRequest.getLastName()).isEqualTo(originalRequest.getLastName());
        assertThat(deserializedRequest.getPersonaType()).isEqualTo(originalRequest.getPersonaType());
        assertThat(deserializedRequest.getPersonaName()).isEqualTo(originalRequest.getPersonaName());
    }

    private RegisterRequest createValidRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setFirstName("John");
        request.setLastName("Doe");
        return request;
    }
}