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
 * Comprehensive unit tests for OAuth2IntrospectionRequest DTO.
 * Tests token introspection request validation and serialization.
 */
@DisplayName("OAuth2IntrospectionRequest DTO Unit Tests")
class OAuth2IntrospectionRequestUnitTest {

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
        @DisplayName("Should create OAuth2IntrospectionRequest using builder with all fields")
        void shouldCreateOAuth2IntrospectionRequestUsingBuilderWithAllFields() {
            // When
            OAuth2IntrospectionRequest request = OAuth2IntrospectionRequest.builder()
                    .token("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
                    .tokenTypeHint("access_token")
                    .clientId("introspection_client_123")
                    .clientSecret("client_secret_456")
                    .authorizationHeader("Basic Y2xpZW50X2lkOmNsaWVudF9zZWNyZXQ=")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getToken()).startsWith("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"),
                    () -> assertThat(request.getTokenTypeHint()).isEqualTo("access_token"),
                    () -> assertThat(request.getClientId()).isEqualTo("introspection_client_123"),
                    () -> assertThat(request.getClientSecret()).isEqualTo("client_secret_456"),
                    () -> assertThat(request.getAuthorizationHeader()).isEqualTo("Basic Y2xpZW50X2lkOmNsaWVudF9zZWNyZXQ=")
            );
        }

        @Test
        @DisplayName("Should create OAuth2IntrospectionRequest with minimal required fields")
        void shouldCreateOAuth2IntrospectionRequestWithMinimalRequiredFields() {
            // When
            OAuth2IntrospectionRequest request = OAuth2IntrospectionRequest.builder()
                    .token("simple_access_token_xyz")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getToken()).isEqualTo("simple_access_token_xyz"),
                    () -> assertThat(request.getTokenTypeHint()).isNull(),
                    () -> assertThat(request.getClientId()).isNull(),
                    () -> assertThat(request.getClientSecret()).isNull(),
                    () -> assertThat(request.getAuthorizationHeader()).isNull()
            );
        }
    }

    @Nested
    @DisplayName("Token Introspection Scenarios")
    class IntrospectionScenarioTests {

        @Test
        @DisplayName("Should create access token introspection request")
        void shouldCreateAccessTokenIntrospectionRequest() {
            // When
            OAuth2IntrospectionRequest request = OAuth2IntrospectionRequest.builder()
                    .token("at_access_token_abc123xyz")
                    .tokenTypeHint("access_token")
                    .clientId("api_client")
                    .clientSecret("api_secret")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request.getToken()).isEqualTo("at_access_token_abc123xyz"),
                    () -> assertThat(request.getTokenTypeHint()).isEqualTo("access_token"),
                    () -> assertThat(request.getClientId()).isEqualTo("api_client"),
                    () -> assertThat(request.getClientSecret()).isEqualTo("api_secret")
            );
        }

        @Test
        @DisplayName("Should create refresh token introspection request")
        void shouldCreateRefreshTokenIntrospectionRequest() {
            // When
            OAuth2IntrospectionRequest request = OAuth2IntrospectionRequest.builder()
                    .token("rt_refresh_token_xyz789abc")
                    .tokenTypeHint("refresh_token")
                    .authorizationHeader("Bearer client_bearer_token")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request.getToken()).isEqualTo("rt_refresh_token_xyz789abc"),
                    () -> assertThat(request.getTokenTypeHint()).isEqualTo("refresh_token"),
                    () -> assertThat(request.getAuthorizationHeader()).isEqualTo("Bearer client_bearer_token"),
                    () -> assertThat(request.getClientId()).isNull(),
                    () -> assertThat(request.getClientSecret()).isNull()
            );
        }

        @Test
        @DisplayName("Should create JWT token introspection request")
        void shouldCreateJwtTokenIntrospectionRequest() {
            // Given
            String jwtToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJrZXkxIn0.eyJleHAiOjE2NDI2ODQ4NDgsImlhdCI6MTY0MjY4MTI0OCwianRpIjoiYWJjZGVmZ2giLCJpc3MiOiJodHRwczovL2ZvY3VzaGl2ZS5jb20iLCJhdWQiOlsiZm9jdXNoaXZlLWFwaSJdLCJzdWIiOiJ1c2VyMTIzIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiZm9jdXNoaXZlLWNsaWVudCIsInNlc3Npb25fc3RhdGUiOiJzZXNzaW9uMTIzIiwiYWNyIjoiMSIsInNjb3BlIjoib3BlbmlkIHByb2ZpbGUgZW1haWwifQ.signature";
            
            // When
            OAuth2IntrospectionRequest request = OAuth2IntrospectionRequest.builder()
                    .token(jwtToken)
                    .tokenTypeHint("access_token")
                    .clientId("focushive_client")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request.getToken()).isEqualTo(jwtToken),
                    () -> assertThat(request.getToken()).contains("."), // JWT contains dots
                    () -> assertThat(request.getTokenTypeHint()).isEqualTo("access_token"),
                    () -> assertThat(request.getClientId()).isEqualTo("focushive_client")
            );
        }
    }

    @Nested
    @DisplayName("Validation Constraint Tests")
    class ValidationConstraintTests {

        @Test
        @DisplayName("Should pass validation when token is provided")
        void shouldPassValidationWhenTokenIsProvided() {
            // Given
            OAuth2IntrospectionRequest request = OAuth2IntrospectionRequest.builder()
                    .token("valid_token_123")
                    .build();

            // When
            Set<ConstraintViolation<OAuth2IntrospectionRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail validation when token is null")
        void shouldFailValidationWhenTokenIsNull() {
            // Given
            OAuth2IntrospectionRequest request = OAuth2IntrospectionRequest.builder()
                    .token(null)
                    .clientId("client")
                    .build();

            // When
            Set<ConstraintViolation<OAuth2IntrospectionRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).hasSize(1),
                    () -> {
                        ConstraintViolation<OAuth2IntrospectionRequest> violation = violations.iterator().next();
                        assertThat(violation.getMessage()).isEqualTo("Token is required");
                        assertThat(violation.getPropertyPath().toString()).isEqualTo("token");
                    }
            );
        }

        @Test
        @DisplayName("Should fail validation when token is empty")
        void shouldFailValidationWhenTokenIsEmpty() {
            // Given
            OAuth2IntrospectionRequest request = OAuth2IntrospectionRequest.builder()
                    .token("")
                    .build();

            // When
            Set<ConstraintViolation<OAuth2IntrospectionRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).hasSize(1),
                    () -> {
                        ConstraintViolation<OAuth2IntrospectionRequest> violation = violations.iterator().next();
                        assertThat(violation.getMessage()).isEqualTo("Token is required");
                    }
            );
        }

        @Test
        @DisplayName("Should fail validation when token is whitespace only")
        void shouldFailValidationWhenTokenIsWhitespaceOnly() {
            // Given
            OAuth2IntrospectionRequest request = OAuth2IntrospectionRequest.builder()
                    .token("   ")
                    .build();

            // When
            Set<ConstraintViolation<OAuth2IntrospectionRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).hasSize(1),
                    () -> {
                        ConstraintViolation<OAuth2IntrospectionRequest> violation = violations.iterator().next();
                        assertThat(violation.getMessage()).isEqualTo("Token is required");
                    }
            );
        }
    }

    @Nested
    @DisplayName("JSON Serialization Tests")
    class JsonSerializationTests {

        @Test
        @DisplayName("Should serialize OAuth2IntrospectionRequest to JSON correctly")
        void shouldSerializeOAuth2IntrospectionRequestToJsonCorrectly() throws Exception {
            // Given
            OAuth2IntrospectionRequest request = OAuth2IntrospectionRequest.builder()
                    .token("sample_token_for_serialization")
                    .tokenTypeHint("access_token")
                    .clientId("serialization_client")
                    .clientSecret("serialization_secret")
                    .authorizationHeader("Basic c2VyaWFsaXphdGlvbl9jbGllbnQ6c2VyaWFsaXphdGlvbl9zZWNyZXQ=")
                    .build();

            // When
            String json = objectMapper.writeValueAsString(request);

            // Then
            assertAll(
                    () -> assertThat(json).contains("\"token\":\"sample_token_for_serialization\""),
                    () -> assertThat(json).contains("\"tokenTypeHint\":\"access_token\""),
                    () -> assertThat(json).contains("\"clientId\":\"serialization_client\""),
                    () -> assertThat(json).contains("\"clientSecret\":\"serialization_secret\""),
                    () -> assertThat(json).contains("\"authorizationHeader\":\"Basic c2VyaWFsaXphdGlvbl9jbGllbnQ6c2VyaWFsaXphdGlvbl9zZWNyZXQ=\"")
            );
        }

        @Test
        @DisplayName("Should deserialize JSON to OAuth2IntrospectionRequest correctly")
        void shouldDeserializeJsonToOAuth2IntrospectionRequestCorrectly() throws Exception {
            // Given
            String json = """
                {
                    "token": "deserialized_token_xyz",
                    "tokenTypeHint": "refresh_token",
                    "clientId": "deserialized_client",
                    "clientSecret": "deserialized_secret",
                    "authorizationHeader": "Bearer bearer_token_abc"
                }
                """;

            // When
            OAuth2IntrospectionRequest request = objectMapper.readValue(json, OAuth2IntrospectionRequest.class);

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getToken()).isEqualTo("deserialized_token_xyz"),
                    () -> assertThat(request.getTokenTypeHint()).isEqualTo("refresh_token"),
                    () -> assertThat(request.getClientId()).isEqualTo("deserialized_client"),
                    () -> assertThat(request.getClientSecret()).isEqualTo("deserialized_secret"),
                    () -> assertThat(request.getAuthorizationHeader()).isEqualTo("Bearer bearer_token_abc")
            );
        }

        @Test
        @DisplayName("Should handle null optional fields in JSON serialization")
        void shouldHandleNullOptionalFieldsInJsonSerialization() throws Exception {
            // Given
            OAuth2IntrospectionRequest request = OAuth2IntrospectionRequest.builder()
                    .token("minimal_token")
                    .build();

            // When
            String json = objectMapper.writeValueAsString(request);

            // Then
            assertAll(
                    () -> assertThat(json).contains("\"token\":\"minimal_token\""),
                    () -> assertThat(json).contains("\"tokenTypeHint\":null"),
                    () -> assertThat(json).contains("\"clientId\":null"),
                    () -> assertThat(json).contains("\"clientSecret\":null"),
                    () -> assertThat(json).contains("\"authorizationHeader\":null")
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
            OAuth2IntrospectionRequest request1 = OAuth2IntrospectionRequest.builder()
                    .token("same_token")
                    .tokenTypeHint("access_token")
                    .clientId("same_client")
                    .build();
                    
            OAuth2IntrospectionRequest request2 = OAuth2IntrospectionRequest.builder()
                    .token("same_token")
                    .tokenTypeHint("access_token")
                    .clientId("same_client")
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
            OAuth2IntrospectionRequest request1 = OAuth2IntrospectionRequest.builder()
                    .token("token_1")
                    .clientId("same_client")
                    .build();
                    
            OAuth2IntrospectionRequest request2 = OAuth2IntrospectionRequest.builder()
                    .token("token_2")
                    .clientId("same_client")
                    .build();

            // Then
            assertThat(request1).isNotEqualTo(request2);
        }

        @Test
        @DisplayName("Should handle null values in equals comparison")
        void shouldHandleNullValuesInEqualsComparison() {
            // Given
            OAuth2IntrospectionRequest request1 = OAuth2IntrospectionRequest.builder()
                    .token("same_token")
                    .tokenTypeHint(null)
                    .clientId(null)
                    .build();
                    
            OAuth2IntrospectionRequest request2 = OAuth2IntrospectionRequest.builder()
                    .token("same_token")
                    .tokenTypeHint(null)
                    .clientId(null)
                    .build();

            // Then
            assertThat(request1).isEqualTo(request2);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Security Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long tokens")
        void shouldHandleVeryLongTokens() {
            // Given
            String longToken = "token_" + "a".repeat(5000);
            
            // When
            OAuth2IntrospectionRequest request = OAuth2IntrospectionRequest.builder()
                    .token(longToken)
                    .build();

            // Then
            assertThat(request.getToken()).hasSize(5006); // "token_" + 5000 chars
        }

        @Test
        @DisplayName("Should handle JWT tokens with multiple segments")
        void shouldHandleJwtTokensWithMultipleSegments() {
            // Given
            String complexJwt = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJrZXkxIn0." +
                    "eyJleHAiOjE2NDI2ODQ4NDgsImlhdCI6MTY0MjY4MTI0OCwianRpIjoiYWJjZGVmZ2giLCJpc3MiOiJodHRwczovL2ZvY3VzaGl2ZS5jb20iLCJhdWQiOlsiZm9jdXNoaXZlLWFwaSJdLCJzdWIiOiJ1c2VyMTIzIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiZm9jdXNoaXZlLWNsaWVudCIsInNlc3Npb25fc3RhdGUiOiJzZXNzaW9uMTIzIiwiYWNyIjoiMSIsInNjb3BlIjoib3BlbmlkIHByb2ZpbGUgZW1haWwifQ." +
                    "very_long_signature_part_with_many_characters_abcdefghijklmnopqrstuvwxyz";
            
            // When
            OAuth2IntrospectionRequest request = OAuth2IntrospectionRequest.builder()
                    .token(complexJwt)
                    .tokenTypeHint("access_token")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request.getToken()).isEqualTo(complexJwt),
                    () -> assertThat(request.getToken()).contains("."), // JWT format
                    () -> assertThat(request.getToken().split("\\.")).hasSize(3) // Header.Payload.Signature
            );
        }

        @Test
        @DisplayName("Should handle both token type hints")
        void shouldHandleBothTokenTypeHints() {
            // Given
            String[] validTokenTypes = {"access_token", "refresh_token"};

            // When/Then
            for (String tokenType : validTokenTypes) {
                OAuth2IntrospectionRequest request = OAuth2IntrospectionRequest.builder()
                        .token("test_token_" + tokenType)
                        .tokenTypeHint(tokenType)
                        .build();
                        
                assertThat(request.getTokenTypeHint()).isEqualTo(tokenType);
            }
        }

        @Test
        @DisplayName("Should handle various authorization header formats")
        void shouldHandleVariousAuthorizationHeaderFormats() {
            // Given
            String[] authHeaders = {
                    "Basic Y2xpZW50X2lkOmNsaWVudF9zZWNyZXQ=",
                    "Bearer access_token_abc123",
                    "OAuth oauth_token=xyz123",
                    "Digest username=\"client\", realm=\"api\", nonce=\"abc123\""
            };

            // When/Then
            for (String authHeader : authHeaders) {
                OAuth2IntrospectionRequest request = OAuth2IntrospectionRequest.builder()
                        .token("test_token")
                        .authorizationHeader(authHeader)
                        .build();
                        
                assertThat(request.getAuthorizationHeader()).isEqualTo(authHeader);
            }
        }

        @Test
        @DisplayName("Should handle Base64 encoded client credentials")
        void shouldHandleBase64EncodedClientCredentials() {
            // Given
            String clientId = "complex_client!@#$%^&*()_+";
            String clientSecret = "super_secret_password_with_special_chars!@#";
            String credentials = clientId + ":" + clientSecret;
            String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
            String authHeader = "Basic " + encodedCredentials;
            
            // When
            OAuth2IntrospectionRequest request = OAuth2IntrospectionRequest.builder()
                    .token("test_token")
                    .authorizationHeader(authHeader)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request.getAuthorizationHeader()).startsWith("Basic "),
                    () -> assertThat(request.getAuthorizationHeader()).isEqualTo(authHeader),
                    () -> {
                        String decoded = new String(java.util.Base64.getDecoder()
                                .decode(request.getAuthorizationHeader().substring(6)));
                        assertThat(decoded).isEqualTo(credentials);
                    }
            );
        }

        @Test
        @DisplayName("Should handle special characters in tokens and credentials")
        void shouldHandleSpecialCharactersInTokensAndCredentials() {
            // Given
            String tokenWithSpecialChars = "token_with_!@#$%^&*()_+-=[]{}|;':\",./<>?`~";
            String clientIdWithSpecialChars = "client_!@#$%^&*()";
            String secretWithSpecialChars = "secret_<>?{}[]|\\";
            
            // When
            OAuth2IntrospectionRequest request = OAuth2IntrospectionRequest.builder()
                    .token(tokenWithSpecialChars)
                    .clientId(clientIdWithSpecialChars)
                    .clientSecret(secretWithSpecialChars)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request.getToken()).isEqualTo(tokenWithSpecialChars),
                    () -> assertThat(request.getClientId()).isEqualTo(clientIdWithSpecialChars),
                    () -> assertThat(request.getClientSecret()).isEqualTo(secretWithSpecialChars)
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
            OAuth2IntrospectionRequest request = new OAuth2IntrospectionRequest();

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getToken()).isNull(),
                    () -> assertThat(request.getTokenTypeHint()).isNull(),
                    () -> assertThat(request.getClientId()).isNull(),
                    () -> assertThat(request.getClientSecret()).isNull(),
                    () -> assertThat(request.getAuthorizationHeader()).isNull()
            );
        }

        @Test
        @DisplayName("Should allow modification after creation")
        void shouldAllowModificationAfterCreation() {
            // Given
            OAuth2IntrospectionRequest request = new OAuth2IntrospectionRequest();

            // When
            request.setToken("dynamic_token");
            request.setTokenTypeHint("refresh_token");
            request.setClientId("dynamic_client");
            request.setClientSecret("dynamic_secret");
            request.setAuthorizationHeader("Bearer dynamic_auth");

            // Then
            assertAll(
                    () -> assertThat(request.getToken()).isEqualTo("dynamic_token"),
                    () -> assertThat(request.getTokenTypeHint()).isEqualTo("refresh_token"),
                    () -> assertThat(request.getClientId()).isEqualTo("dynamic_client"),
                    () -> assertThat(request.getClientSecret()).isEqualTo("dynamic_secret"),
                    () -> assertThat(request.getAuthorizationHeader()).isEqualTo("Bearer dynamic_auth")
            );
        }
    }
}