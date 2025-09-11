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
 * Comprehensive unit tests for TokenRefreshRequest covering default constructor,
 * validation, serialization/deserialization, equality, and edge cases.
 */
@DisplayName("TokenRefreshRequest Unit Tests")
class TokenRefreshRequestUnitTest {

    private ObjectMapper objectMapper;
    private Validator validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should create TokenRefreshRequest using default constructor")
    void shouldCreateTokenRefreshRequestUsingDefaultConstructor() {
        // Given & When
        TokenRefreshRequest request = new TokenRefreshRequest();

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getRefreshToken()).isNull();
    }

    @Test
    @DisplayName("Should create TokenRefreshRequest and set refresh token")
    void shouldCreateTokenRefreshRequestAndSetRefreshToken() {
        // Given
        TokenRefreshRequest request = new TokenRefreshRequest();
        String refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        // When
        request.setRefreshToken(refreshToken);

        // Then
        assertThat(request.getRefreshToken()).isEqualTo(refreshToken);
    }

    @Test
    @DisplayName("Should validate successfully with valid refresh token")
    void shouldValidateSuccessfullyWithValidRefreshToken() {
        // Given
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken("valid.jwt.token");

        // When
        Set<ConstraintViolation<TokenRefreshRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation with null refresh token")
    void shouldFailValidationWithNullRefreshToken() {
        // Given
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken(null);

        // When
        Set<ConstraintViolation<TokenRefreshRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<TokenRefreshRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Refresh token is required");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("refreshToken");
    }

    @Test
    @DisplayName("Should fail validation with empty refresh token")
    void shouldFailValidationWithEmptyRefreshToken() {
        // Given
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken("");

        // When
        Set<ConstraintViolation<TokenRefreshRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<TokenRefreshRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Refresh token is required");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("refreshToken");
    }

    @Test
    @DisplayName("Should fail validation with blank refresh token")
    void shouldFailValidationWithBlankRefreshToken() {
        // Given
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken("   \n\t   ");

        // When
        Set<ConstraintViolation<TokenRefreshRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        ConstraintViolation<TokenRefreshRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo("Refresh token is required");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("refreshToken");
    }

    @Test
    @DisplayName("Should serialize TokenRefreshRequest to JSON correctly")
    void shouldSerializeTokenRefreshRequestToJsonCorrectly() throws JsonProcessingException {
        // Given
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token");

        // When
        String json = objectMapper.writeValueAsString(request);

        // Then
        assertThat(json).isNotNull();
        assertThat(json).contains("\"refreshToken\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token\"");
    }

    @Test
    @DisplayName("Should serialize TokenRefreshRequest with null token to JSON correctly")
    void shouldSerializeTokenRefreshRequestWithNullTokenToJsonCorrectly() throws JsonProcessingException {
        // Given
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken(null);

        // When
        String json = objectMapper.writeValueAsString(request);

        // Then
        assertThat(json).isNotNull();
        assertThat(json).contains("\"refreshToken\":null");
    }

    @Test
    @DisplayName("Should deserialize JSON to TokenRefreshRequest correctly")
    void shouldDeserializeJsonToTokenRefreshRequestCorrectly() throws JsonProcessingException {
        // Given
        String json = "{\"refreshToken\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.example.refresh.token\"}";

        // When
        TokenRefreshRequest request = objectMapper.readValue(json, TokenRefreshRequest.class);

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getRefreshToken()).isEqualTo("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.example.refresh.token");
    }

    @Test
    @DisplayName("Should deserialize JSON with null refresh token correctly")
    void shouldDeserializeJsonWithNullRefreshTokenCorrectly() throws JsonProcessingException {
        // Given
        String json = "{\"refreshToken\":null}";

        // When
        TokenRefreshRequest request = objectMapper.readValue(json, TokenRefreshRequest.class);

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getRefreshToken()).isNull();
    }

    @Test
    @DisplayName("Should deserialize JSON with missing refresh token field correctly")
    void shouldDeserializeJsonWithMissingRefreshTokenFieldCorrectly() throws JsonProcessingException {
        // Given
        String json = "{}";

        // When
        TokenRefreshRequest request = objectMapper.readValue(json, TokenRefreshRequest.class);

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getRefreshToken()).isNull();
    }

    @Test
    @DisplayName("Should test equality and hashCode with same refresh token values")
    void shouldTestEqualityAndHashCodeWithSameRefreshTokenValues() {
        // Given
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
        TokenRefreshRequest request1 = new TokenRefreshRequest();
        request1.setRefreshToken(token);
        
        TokenRefreshRequest request2 = new TokenRefreshRequest();
        request2.setRefreshToken(token);

        // Then
        assertThat(request1).isEqualTo(request2);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    @DisplayName("Should test equality with both null refresh token values")
    void shouldTestEqualityWithBothNullRefreshTokenValues() {
        // Given
        TokenRefreshRequest request1 = new TokenRefreshRequest();
        request1.setRefreshToken(null);
        
        TokenRefreshRequest request2 = new TokenRefreshRequest();
        request2.setRefreshToken(null);

        // Then
        assertThat(request1).isEqualTo(request2);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    @DisplayName("Should test inequality with different refresh token values")
    void shouldTestInequalityWithDifferentRefreshTokenValues() {
        // Given
        TokenRefreshRequest request1 = new TokenRefreshRequest();
        request1.setRefreshToken("token1");
        
        TokenRefreshRequest request2 = new TokenRefreshRequest();
        request2.setRefreshToken("token2");

        // Then
        assertThat(request1).isNotEqualTo(request2);
        assertThat(request1.hashCode()).isNotEqualTo(request2.hashCode());
    }

    @Test
    @DisplayName("Should test inequality with null and non-null refresh token values")
    void shouldTestInequalityWithNullAndNonNullRefreshTokenValues() {
        // Given
        TokenRefreshRequest request1 = new TokenRefreshRequest();
        request1.setRefreshToken(null);
        
        TokenRefreshRequest request2 = new TokenRefreshRequest();
        request2.setRefreshToken("some.token");

        // Then
        assertThat(request1).isNotEqualTo(request2);
        assertThat(request1.hashCode()).isNotEqualTo(request2.hashCode());
    }

    @Test
    @DisplayName("Should test toString method with refresh token")
    void shouldTestToStringMethodWithRefreshToken() {
        // Given
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token");

        // When
        String toString = request.toString();

        // Then
        assertThat(toString).isNotNull();
        assertThat(toString).contains("TokenRefreshRequest");
        assertThat(toString).contains("refreshToken=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token");
    }

    @Test
    @DisplayName("Should test toString method with null refresh token")
    void shouldTestToStringMethodWithNullRefreshToken() {
        // Given
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken(null);

        // When
        String toString = request.toString();

        // Then
        assertThat(toString).isNotNull();
        assertThat(toString).contains("TokenRefreshRequest");
        assertThat(toString).contains("refreshToken=null");
    }

    @Test
    @DisplayName("Should handle very long refresh token")
    void shouldHandleVeryLongRefreshToken() {
        // Given
        String longToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." + "a".repeat(1000) + ".signature";
        TokenRefreshRequest request = new TokenRefreshRequest();
        
        // When
        request.setRefreshToken(longToken);

        // Then
        assertThat(request.getRefreshToken()).hasSize(longToken.length());
        assertThat(request.getRefreshToken()).isEqualTo(longToken);
    }

    @Test
    @DisplayName("Should handle refresh token with special characters")
    void shouldHandleRefreshTokenWithSpecialCharacters() {
        // Given - JWT tokens can contain special URL-safe characters
        String tokenWithSpecialChars = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWItdGVzdCI6IjEyMzQ1Njc4OTAiLCJuYW1lIjoi--Sm9obiBEb2UiLCJpYXQiOjE1MTYyMzkwMjJ9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        TokenRefreshRequest request = new TokenRefreshRequest();

        // When
        request.setRefreshToken(tokenWithSpecialChars);

        // Then
        assertThat(request.getRefreshToken()).isEqualTo(tokenWithSpecialChars);
    }

    @Test
    @DisplayName("Should handle various JWT token formats")
    void shouldHandleVariousJwtTokenFormats() {
        TokenRefreshRequest request = new TokenRefreshRequest();
        
        // Standard JWT format
        String standardJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        request.setRefreshToken(standardJwt);
        assertThat(request.getRefreshToken()).isEqualTo(standardJwt);
        
        // Shorter JWT
        String shortJwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.signature";
        request.setRefreshToken(shortJwt);
        assertThat(request.getRefreshToken()).isEqualTo(shortJwt);
        
        // Custom token format (not necessarily JWT)
        String customToken = "refresh_token_12345abcdef";
        request.setRefreshToken(customToken);
        assertThat(request.getRefreshToken()).isEqualTo(customToken);
        
        // UUID-like token
        String uuidToken = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
        request.setRefreshToken(uuidToken);
        assertThat(request.getRefreshToken()).isEqualTo(uuidToken);
        
        // Base64-encoded token
        String base64Token = "dG9rZW5fZXhhbXBsZV8xMjM0NTY3ODkw";
        request.setRefreshToken(base64Token);
        assertThat(request.getRefreshToken()).isEqualTo(base64Token);
    }

    @Test
    @DisplayName("Should serialize and deserialize complex refresh token correctly")
    void shouldSerializeAndDeserializeComplexRefreshTokenCorrectly() throws JsonProcessingException {
        // Given - Complex JWT token with special characters
        String complexToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjEyMyJ9.eyJzdWIiOiJ1c2VyXzEyMyIsImVtYWlsIjoidXNlckBleGFtcGxlLmNvbSIsInJvbGVzIjpbImFkbWluIiwidXNlciJdLCJpYXQiOjE2MjM5MzAyMjIsImV4cCI6MTYyMzk0NDYyMiwiaXNzIjoiaHR0cHM6Ly9pZGVudGl0eS5leGFtcGxlLmNvbSJ9.signature_with_special_chars-123";
        TokenRefreshRequest originalRequest = new TokenRefreshRequest();
        originalRequest.setRefreshToken(complexToken);

        // When
        String json = objectMapper.writeValueAsString(originalRequest);
        TokenRefreshRequest deserializedRequest = objectMapper.readValue(json, TokenRefreshRequest.class);

        // Then
        assertThat(deserializedRequest).isEqualTo(originalRequest);
        assertThat(deserializedRequest.getRefreshToken()).isEqualTo(complexToken);
    }

    @Test
    @DisplayName("Should handle token setter with different values")
    void shouldHandleTokenSetterWithDifferentValues() {
        // Given
        TokenRefreshRequest request = new TokenRefreshRequest();

        // When & Then - Test setting different types of values
        String token1 = "initial.token.value";
        request.setRefreshToken(token1);
        assertThat(request.getRefreshToken()).isEqualTo(token1);

        request.setRefreshToken(null);
        assertThat(request.getRefreshToken()).isNull();

        String token2 = "";
        request.setRefreshToken(token2);
        assertThat(request.getRefreshToken()).isEmpty();

        String token3 = "   whitespace_token   ";
        request.setRefreshToken(token3);
        assertThat(request.getRefreshToken()).isEqualTo(token3);

        String token4 = "final.refresh.token.123";
        request.setRefreshToken(token4);
        assertThat(request.getRefreshToken()).isEqualTo(token4);
    }

    @Test
    @DisplayName("Should handle JSON with extra fields gracefully")
    void shouldHandleJsonWithExtraFieldsGracefully() throws JsonProcessingException {
        // Given - Configure ObjectMapper to ignore unknown properties for this specific test
        ObjectMapper lenientMapper = new ObjectMapper();
        lenientMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        String jsonWithExtraFields = """
                {
                    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token",
                    "extraField1": "should be ignored",
                    "extraField2": 12345,
                    "extraField3": true,
                    "nestedObject": {
                        "nested": "value"
                    }
                }
                """;

        // When
        TokenRefreshRequest request = lenientMapper.readValue(jsonWithExtraFields, TokenRefreshRequest.class);

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getRefreshToken()).isEqualTo("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token");
        // Extra fields should be ignored during deserialization
    }

    @Test
    @DisplayName("Should maintain consistency across multiple operations")
    void shouldMaintainConsistencyAcrossMultipleOperations() {
        // Given
        String originalToken = "original.refresh.token";
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken(originalToken);

        // When & Then - Test multiple operations maintain consistency
        assertThat(request.getRefreshToken()).isEqualTo(originalToken);
        
        // Test getter consistency
        String retrievedToken1 = request.getRefreshToken();
        String retrievedToken2 = request.getRefreshToken();
        assertThat(retrievedToken1).isEqualTo(retrievedToken2);
        
        // Test setter and getter consistency
        String newToken = "new.consistent.token";
        request.setRefreshToken(newToken);
        assertThat(request.getRefreshToken()).isEqualTo(newToken);
        assertThat(request.getRefreshToken()).isEqualTo(newToken); // Second call should be identical
        
        // Test toString consistency
        String toString1 = request.toString();
        String toString2 = request.toString();
        assertThat(toString1).isEqualTo(toString2);
    }

    @Test
    @DisplayName("Should handle token boundaries and edge cases")
    void shouldHandleTokenBoundariesAndEdgeCases() {
        TokenRefreshRequest request = new TokenRefreshRequest();
        
        // Test single character token
        request.setRefreshToken("A");
        assertThat(request.getRefreshToken()).hasSize(1);
        assertThat(request.getRefreshToken()).isEqualTo("A");
        
        // Test single dot (invalid JWT but valid string)
        request.setRefreshToken(".");
        assertThat(request.getRefreshToken()).isEqualTo(".");
        
        // Test two dots (JWT structure without content)
        request.setRefreshToken("..");
        assertThat(request.getRefreshToken()).isEqualTo("..");
        
        // Test only numbers
        request.setRefreshToken("1234567890");
        assertThat(request.getRefreshToken()).isEqualTo("1234567890");
        
        // Test only special characters
        request.setRefreshToken("-_.");
        assertThat(request.getRefreshToken()).isEqualTo("-_.");
    }

    @Test
    @DisplayName("Should validate different token length scenarios")
    void shouldValidateDifferentTokenLengthScenarios() {
        // Very short valid token
        TokenRefreshRequest shortTokenRequest = new TokenRefreshRequest();
        shortTokenRequest.setRefreshToken("abc");
        Set<ConstraintViolation<TokenRefreshRequest>> shortViolations = validator.validate(shortTokenRequest);
        assertThat(shortViolations).isEmpty(); // Should be valid as it's not blank
        
        // Medium length token
        TokenRefreshRequest mediumTokenRequest = new TokenRefreshRequest();
        mediumTokenRequest.setRefreshToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");
        Set<ConstraintViolation<TokenRefreshRequest>> mediumViolations = validator.validate(mediumTokenRequest);
        assertThat(mediumViolations).isEmpty();
        
        // Very long token
        String veryLongToken = "a".repeat(5000);
        TokenRefreshRequest longTokenRequest = new TokenRefreshRequest();
        longTokenRequest.setRefreshToken(veryLongToken);
        Set<ConstraintViolation<TokenRefreshRequest>> longViolations = validator.validate(longTokenRequest);
        assertThat(longViolations).isEmpty(); // Should be valid as it's not blank
    }

    @Test
    @DisplayName("Should handle validation edge cases correctly")
    void shouldHandleValidationEdgeCasesCorrectly() {
        // Test with just a space
        TokenRefreshRequest spaceRequest = new TokenRefreshRequest();
        spaceRequest.setRefreshToken(" ");
        Set<ConstraintViolation<TokenRefreshRequest>> spaceViolations = validator.validate(spaceRequest);
        assertThat(spaceViolations).hasSize(1); // Should fail as it's considered blank
        
        // Test with mixed whitespace
        TokenRefreshRequest mixedWhitespaceRequest = new TokenRefreshRequest();
        mixedWhitespaceRequest.setRefreshToken(" \t\n\r ");
        Set<ConstraintViolation<TokenRefreshRequest>> mixedViolations = validator.validate(mixedWhitespaceRequest);
        assertThat(mixedViolations).hasSize(1); // Should fail as it's considered blank
        
        // Test with whitespace around valid token (should be valid)
        TokenRefreshRequest paddedRequest = new TokenRefreshRequest();
        paddedRequest.setRefreshToken(" validtoken ");
        Set<ConstraintViolation<TokenRefreshRequest>> paddedViolations = validator.validate(paddedRequest);
        assertThat(paddedViolations).isEmpty(); // Should be valid as it contains non-blank content
    }
}