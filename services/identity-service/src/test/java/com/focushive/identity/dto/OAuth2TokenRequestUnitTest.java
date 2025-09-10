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
 * Comprehensive unit tests for OAuth2TokenRequest DTO.
 * Tests OAuth2 grant type flows, validation constraints, and serialization.
 */
@DisplayName("OAuth2TokenRequest DTO Unit Tests")
class OAuth2TokenRequestUnitTest {

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
        @DisplayName("Should create OAuth2TokenRequest using builder with all fields")
        void shouldCreateOAuth2TokenRequestUsingBuilderWithAllFields() {
            // When
            OAuth2TokenRequest request = OAuth2TokenRequest.builder()
                    .grantType("authorization_code")
                    .code("auth_code_123")
                    .redirectUri("https://app.focushive.com/oauth/callback")
                    .codeVerifier("code_verifier_xyz")
                    .refreshToken("refresh_token_456")
                    .scope("read write admin")
                    .clientId("client_123")
                    .clientSecret("client_secret_789")
                    .authorizationHeader("Basic Y2xpZW50X2lkOmNsaWVudF9zZWNyZXQ=")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getGrantType()).isEqualTo("authorization_code"),
                    () -> assertThat(request.getCode()).isEqualTo("auth_code_123"),
                    () -> assertThat(request.getRedirectUri()).isEqualTo("https://app.focushive.com/oauth/callback"),
                    () -> assertThat(request.getCodeVerifier()).isEqualTo("code_verifier_xyz"),
                    () -> assertThat(request.getRefreshToken()).isEqualTo("refresh_token_456"),
                    () -> assertThat(request.getScope()).isEqualTo("read write admin"),
                    () -> assertThat(request.getClientId()).isEqualTo("client_123"),
                    () -> assertThat(request.getClientSecret()).isEqualTo("client_secret_789"),
                    () -> assertThat(request.getAuthorizationHeader()).isEqualTo("Basic Y2xpZW50X2lkOmNsaWVudF9zZWNyZXQ=")
            );
        }

        @Test
        @DisplayName("Should create OAuth2TokenRequest with minimal required fields")
        void shouldCreateOAuth2TokenRequestWithMinimalRequiredFields() {
            // When
            OAuth2TokenRequest request = OAuth2TokenRequest.builder()
                    .grantType("client_credentials")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getGrantType()).isEqualTo("client_credentials"),
                    () -> assertThat(request.getCode()).isNull(),
                    () -> assertThat(request.getRedirectUri()).isNull(),
                    () -> assertThat(request.getCodeVerifier()).isNull(),
                    () -> assertThat(request.getRefreshToken()).isNull(),
                    () -> assertThat(request.getScope()).isNull(),
                    () -> assertThat(request.getClientId()).isNull(),
                    () -> assertThat(request.getClientSecret()).isNull(),
                    () -> assertThat(request.getAuthorizationHeader()).isNull()
            );
        }
    }

    @Nested
    @DisplayName("OAuth2 Grant Type Scenarios")
    class GrantTypeScenarioTests {

        @Test
        @DisplayName("Should create valid authorization_code grant request")
        void shouldCreateValidAuthorizationCodeGrantRequest() {
            // When
            OAuth2TokenRequest request = OAuth2TokenRequest.builder()
                    .grantType("authorization_code")
                    .code("authorization_code_xyz")
                    .redirectUri("https://app.focushive.com/callback")
                    .codeVerifier("pkce_verifier_123")
                    .clientId("focushive_web_client")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request.getGrantType()).isEqualTo("authorization_code"),
                    () -> assertThat(request.getCode()).isEqualTo("authorization_code_xyz"),
                    () -> assertThat(request.getRedirectUri()).isEqualTo("https://app.focushive.com/callback"),
                    () -> assertThat(request.getCodeVerifier()).isEqualTo("pkce_verifier_123"),
                    () -> assertThat(request.getClientId()).isEqualTo("focushive_web_client")
            );
        }

        @Test
        @DisplayName("Should create valid refresh_token grant request")
        void shouldCreateValidRefreshTokenGrantRequest() {
            // When
            OAuth2TokenRequest request = OAuth2TokenRequest.builder()
                    .grantType("refresh_token")
                    .refreshToken("valid_refresh_token_abc")
                    .scope("read write")
                    .clientId("mobile_client")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request.getGrantType()).isEqualTo("refresh_token"),
                    () -> assertThat(request.getRefreshToken()).isEqualTo("valid_refresh_token_abc"),
                    () -> assertThat(request.getScope()).isEqualTo("read write"),
                    () -> assertThat(request.getClientId()).isEqualTo("mobile_client"),
                    () -> assertThat(request.getCode()).isNull(),
                    () -> assertThat(request.getRedirectUri()).isNull()
            );
        }

        @Test
        @DisplayName("Should create valid client_credentials grant request")
        void shouldCreateValidClientCredentialsGrantRequest() {
            // When
            OAuth2TokenRequest request = OAuth2TokenRequest.builder()
                    .grantType("client_credentials")
                    .scope("api:read api:write")
                    .clientId("service_client")
                    .clientSecret("super_secret_key")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request.getGrantType()).isEqualTo("client_credentials"),
                    () -> assertThat(request.getScope()).isEqualTo("api:read api:write"),
                    () -> assertThat(request.getClientId()).isEqualTo("service_client"),
                    () -> assertThat(request.getClientSecret()).isEqualTo("super_secret_key"),
                    () -> assertThat(request.getCode()).isNull(),
                    () -> assertThat(request.getRefreshToken()).isNull(),
                    () -> assertThat(request.getRedirectUri()).isNull()
            );
        }
    }

    @Nested
    @DisplayName("Validation Constraint Tests")
    class ValidationConstraintTests {

        @Test
        @DisplayName("Should pass validation when grantType is provided")
        void shouldPassValidationWhenGrantTypeIsProvided() {
            // Given
            OAuth2TokenRequest request = OAuth2TokenRequest.builder()
                    .grantType("authorization_code")
                    .build();

            // When
            Set<ConstraintViolation<OAuth2TokenRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail validation when grantType is null")
        void shouldFailValidationWhenGrantTypeIsNull() {
            // Given
            OAuth2TokenRequest request = OAuth2TokenRequest.builder()
                    .grantType(null)
                    .code("some_code")
                    .build();

            // When
            Set<ConstraintViolation<OAuth2TokenRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).hasSize(1),
                    () -> {
                        ConstraintViolation<OAuth2TokenRequest> violation = violations.iterator().next();
                        assertThat(violation.getMessage()).isEqualTo("Grant type is required");
                        assertThat(violation.getPropertyPath().toString()).isEqualTo("grantType");
                    }
            );
        }

        @Test
        @DisplayName("Should fail validation when grantType is empty")
        void shouldFailValidationWhenGrantTypeIsEmpty() {
            // Given
            OAuth2TokenRequest request = OAuth2TokenRequest.builder()
                    .grantType("")
                    .build();

            // When
            Set<ConstraintViolation<OAuth2TokenRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).hasSize(1),
                    () -> {
                        ConstraintViolation<OAuth2TokenRequest> violation = violations.iterator().next();
                        assertThat(violation.getMessage()).isEqualTo("Grant type is required");
                        assertThat(violation.getPropertyPath().toString()).isEqualTo("grantType");
                    }
            );
        }

        @Test
        @DisplayName("Should fail validation when grantType is whitespace only")
        void shouldFailValidationWhenGrantTypeIsWhitespaceOnly() {
            // Given
            OAuth2TokenRequest request = OAuth2TokenRequest.builder()
                    .grantType("   ")
                    .build();

            // When
            Set<ConstraintViolation<OAuth2TokenRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).hasSize(1),
                    () -> {
                        ConstraintViolation<OAuth2TokenRequest> violation = violations.iterator().next();
                        assertThat(violation.getMessage()).isEqualTo("Grant type is required");
                    }
            );
        }

        @Test
        @DisplayName("Should accept all valid grant types")
        void shouldAcceptAllValidGrantTypes() {
            // Given
            String[] validGrantTypes = {"authorization_code", "refresh_token", "client_credentials"};

            // When/Then
            for (String grantType : validGrantTypes) {
                OAuth2TokenRequest request = OAuth2TokenRequest.builder()
                        .grantType(grantType)
                        .build();
                
                Set<ConstraintViolation<OAuth2TokenRequest>> violations = validator.validate(request);
                assertThat(violations).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("JSON Serialization Tests")
    class JsonSerializationTests {

        @Test
        @DisplayName("Should serialize OAuth2TokenRequest to JSON correctly")
        void shouldSerializeOAuth2TokenRequestToJsonCorrectly() throws Exception {
            // Given
            OAuth2TokenRequest request = OAuth2TokenRequest.builder()
                    .grantType("authorization_code")
                    .code("auth_code_123")
                    .redirectUri("https://app.focushive.com/callback")
                    .codeVerifier("pkce_verifier")
                    .clientId("web_client")
                    .build();

            // When
            String json = objectMapper.writeValueAsString(request);

            // Then
            assertAll(
                    () -> assertThat(json).contains("\"grantType\":\"authorization_code\""),
                    () -> assertThat(json).contains("\"code\":\"auth_code_123\""),
                    () -> assertThat(json).contains("\"redirectUri\":\"https://app.focushive.com/callback\""),
                    () -> assertThat(json).contains("\"codeVerifier\":\"pkce_verifier\""),
                    () -> assertThat(json).contains("\"clientId\":\"web_client\"")
            );
        }

        @Test
        @DisplayName("Should deserialize JSON to OAuth2TokenRequest correctly")
        void shouldDeserializeJsonToOAuth2TokenRequestCorrectly() throws Exception {
            // Given
            String json = """
                {
                    "grantType": "refresh_token",
                    "refreshToken": "refresh_token_xyz",
                    "scope": "read write",
                    "clientId": "mobile_app",
                    "authorizationHeader": "Bearer token123"
                }
                """;

            // When
            OAuth2TokenRequest request = objectMapper.readValue(json, OAuth2TokenRequest.class);

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getGrantType()).isEqualTo("refresh_token"),
                    () -> assertThat(request.getRefreshToken()).isEqualTo("refresh_token_xyz"),
                    () -> assertThat(request.getScope()).isEqualTo("read write"),
                    () -> assertThat(request.getClientId()).isEqualTo("mobile_app"),
                    () -> assertThat(request.getAuthorizationHeader()).isEqualTo("Bearer token123"),
                    () -> assertThat(request.getCode()).isNull(),
                    () -> assertThat(request.getRedirectUri()).isNull()
            );
        }

        @Test
        @DisplayName("Should handle null fields in JSON serialization")
        void shouldHandleNullFieldsInJsonSerialization() throws Exception {
            // Given
            OAuth2TokenRequest request = OAuth2TokenRequest.builder()
                    .grantType("client_credentials")
                    .scope("api:read")
                    // other fields are null
                    .build();

            // When
            String json = objectMapper.writeValueAsString(request);

            // Then
            assertAll(
                    () -> assertThat(json).contains("\"grantType\":\"client_credentials\""),
                    () -> assertThat(json).contains("\"scope\":\"api:read\""),
                    () -> assertThat(json).contains("\"code\":null"),
                    () -> assertThat(json).contains("\"redirectUri\":null"),
                    () -> assertThat(json).contains("\"refreshToken\":null")
            );
        }

        @Test
        @DisplayName("Should handle complex authorization header in serialization")
        void shouldHandleComplexAuthorizationHeaderInSerialization() throws Exception {
            // Given
            String complexAuthHeader = "Basic " + java.util.Base64.getEncoder()
                    .encodeToString("complex_client_id:complex!@#$%^&*()_+secret".getBytes());
            
            OAuth2TokenRequest request = OAuth2TokenRequest.builder()
                    .grantType("authorization_code")
                    .authorizationHeader(complexAuthHeader)
                    .build();

            // When
            String json = objectMapper.writeValueAsString(request);
            OAuth2TokenRequest deserialized = objectMapper.readValue(json, OAuth2TokenRequest.class);

            // Then
            assertThat(deserialized.getAuthorizationHeader()).isEqualTo(complexAuthHeader);
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when all fields are the same")
        void shouldBeEqualWhenAllFieldsAreTheSame() {
            // Given
            OAuth2TokenRequest request1 = OAuth2TokenRequest.builder()
                    .grantType("authorization_code")
                    .code("same_code")
                    .redirectUri("https://same.uri.com")
                    .clientId("same_client")
                    .build();
                    
            OAuth2TokenRequest request2 = OAuth2TokenRequest.builder()
                    .grantType("authorization_code")
                    .code("same_code")
                    .redirectUri("https://same.uri.com")
                    .clientId("same_client")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request1).isEqualTo(request2),
                    () -> assertThat(request1.hashCode()).isEqualTo(request2.hashCode())
            );
        }

        @Test
        @DisplayName("Should not be equal when grant types differ")
        void shouldNotBeEqualWhenGrantTypesDiffer() {
            // Given
            OAuth2TokenRequest request1 = OAuth2TokenRequest.builder()
                    .grantType("authorization_code")
                    .clientId("same_client")
                    .build();
                    
            OAuth2TokenRequest request2 = OAuth2TokenRequest.builder()
                    .grantType("refresh_token")
                    .clientId("same_client")
                    .build();

            // Then
            assertThat(request1).isNotEqualTo(request2);
        }

        @Test
        @DisplayName("Should handle null values in equals comparison")
        void shouldHandleNullValuesInEqualsComparison() {
            // Given
            OAuth2TokenRequest request1 = OAuth2TokenRequest.builder()
                    .grantType("client_credentials")
                    .code(null)
                    .refreshToken(null)
                    .build();
                    
            OAuth2TokenRequest request2 = OAuth2TokenRequest.builder()
                    .grantType("client_credentials")
                    .code(null)
                    .refreshToken(null)
                    .build();

            // Then
            assertThat(request1).isEqualTo(request2);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Conditions")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long tokens and codes")
        void shouldHandleVeryLongTokensAndCodes() {
            // Given
            String longCode = "a".repeat(1000);
            String longToken = "b".repeat(2000);
            
            // When
            OAuth2TokenRequest request = OAuth2TokenRequest.builder()
                    .grantType("authorization_code")
                    .code(longCode)
                    .refreshToken(longToken)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request.getCode()).hasSize(1000),
                    () -> assertThat(request.getRefreshToken()).hasSize(2000)
            );
        }

        @Test
        @DisplayName("Should handle special characters in all fields")
        void shouldHandleSpecialCharactersInAllFields() {
            // Given
            String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~";
            
            // When
            OAuth2TokenRequest request = OAuth2TokenRequest.builder()
                    .grantType("authorization_code")
                    .code("code_" + specialChars)
                    .redirectUri("https://example.com/callback?param=" + java.net.URLEncoder.encode(specialChars, java.nio.charset.StandardCharsets.UTF_8))
                    .scope("read:special write:special")
                    .clientId("client_" + specialChars.substring(0, 10))
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request.getCode()).contains(specialChars),
                    () -> assertThat(request.getRedirectUri()).contains("callback?param="),
                    () -> assertThat(request.getScope()).isEqualTo("read:special write:special"),
                    () -> assertThat(request.getClientId()).contains("client_")
            );
        }

        @Test
        @DisplayName("Should handle multiple scopes in scope field")
        void shouldHandleMultipleScopesInScopeField() {
            // Given/When
            OAuth2TokenRequest request = OAuth2TokenRequest.builder()
                    .grantType("client_credentials")
                    .scope("read write admin delete user:profile user:settings api:v1 api:v2")
                    .build();

            // Then
            assertThat(request.getScope()).isEqualTo("read write admin delete user:profile user:settings api:v1 api:v2");
        }

        @Test
        @DisplayName("Should handle various URI schemes in redirectUri")
        void shouldHandleVariousUriSchemesInRedirectUri() {
            // Given
            String[] uriSchemes = {
                    "https://app.focushive.com/callback",
                    "http://localhost:3000/auth/callback",
                    "focushive://oauth/callback",
                    "com.focushive.mobile://auth"
            };

            // When/Then
            for (String uri : uriSchemes) {
                OAuth2TokenRequest request = OAuth2TokenRequest.builder()
                        .grantType("authorization_code")
                        .redirectUri(uri)
                        .build();
                        
                assertThat(request.getRedirectUri()).isEqualTo(uri);
            }
        }

        @Test
        @DisplayName("Should handle Base64 encoded authorization headers")
        void shouldHandleBase64EncodedAuthorizationHeaders() {
            // Given
            String credentials = "client_id:client_secret";
            String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
            String authHeader = "Basic " + encodedCredentials;

            // When
            OAuth2TokenRequest request = OAuth2TokenRequest.builder()
                    .grantType("authorization_code")
                    .authorizationHeader(authHeader)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request.getAuthorizationHeader()).startsWith("Basic "),
                    () -> assertThat(request.getAuthorizationHeader()).isEqualTo(authHeader),
                    () -> {
                        String decoded = new String(java.util.Base64.getDecoder()
                                .decode(request.getAuthorizationHeader().substring(6)));
                        assertThat(decoded).isEqualTo("client_id:client_secret");
                    }
            );
        }
    }

    @Nested
    @DisplayName("NoArgsConstructor and AllArgsConstructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create instance using no-args constructor")
        void shouldCreateInstanceUsingNoArgsConstructor() {
            // When
            OAuth2TokenRequest request = new OAuth2TokenRequest();

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getGrantType()).isNull(),
                    () -> assertThat(request.getCode()).isNull(),
                    () -> assertThat(request.getRedirectUri()).isNull(),
                    () -> assertThat(request.getCodeVerifier()).isNull(),
                    () -> assertThat(request.getRefreshToken()).isNull(),
                    () -> assertThat(request.getScope()).isNull(),
                    () -> assertThat(request.getClientId()).isNull(),
                    () -> assertThat(request.getClientSecret()).isNull(),
                    () -> assertThat(request.getAuthorizationHeader()).isNull()
            );
        }

        @Test
        @DisplayName("Should allow modification after creation")
        void shouldAllowModificationAfterCreation() {
            // Given
            OAuth2TokenRequest request = new OAuth2TokenRequest();

            // When
            request.setGrantType("client_credentials");
            request.setScope("api:read");
            request.setClientId("dynamic_client");

            // Then
            assertAll(
                    () -> assertThat(request.getGrantType()).isEqualTo("client_credentials"),
                    () -> assertThat(request.getScope()).isEqualTo("api:read"),
                    () -> assertThat(request.getClientId()).isEqualTo("dynamic_client")
            );
        }
    }
}