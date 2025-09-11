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
 * Comprehensive unit tests for OAuth2AuthorizeRequest DTO.
 * Tests OAuth2 authorization code flow, validation constraints, and serialization.
 */
@DisplayName("OAuth2AuthorizeRequest DTO Unit Tests")
class OAuth2AuthorizeRequestUnitTest {

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
        @DisplayName("Should create OAuth2AuthorizeRequest using builder with all fields")
        void shouldCreateOAuth2AuthorizeRequestUsingBuilderWithAllFields() {
            // When
            OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.builder()
                    .clientId("focushive_web_client")
                    .responseType("code")
                    .redirectUri("https://app.focushive.com/oauth/callback")
                    .scope("openid profile email read write")
                    .state("csrf_protection_xyz123")
                    .codeChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")
                    .codeChallengeMethod("S256")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getClientId()).isEqualTo("focushive_web_client"),
                    () -> assertThat(request.getResponseType()).isEqualTo("code"),
                    () -> assertThat(request.getRedirectUri()).isEqualTo("https://app.focushive.com/oauth/callback"),
                    () -> assertThat(request.getScope()).isEqualTo("openid profile email read write"),
                    () -> assertThat(request.getState()).isEqualTo("csrf_protection_xyz123"),
                    () -> assertThat(request.getCodeChallenge()).isEqualTo("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
                    () -> assertThat(request.getCodeChallengeMethod()).isEqualTo("S256")
            );
        }

        @Test
        @DisplayName("Should create OAuth2AuthorizeRequest with minimal required fields")
        void shouldCreateOAuth2AuthorizeRequestWithMinimalRequiredFields() {
            // When
            OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.builder()
                    .clientId("minimal_client")
                    .responseType("code")
                    .redirectUri("https://minimal.example.com/callback")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getClientId()).isEqualTo("minimal_client"),
                    () -> assertThat(request.getResponseType()).isEqualTo("code"),
                    () -> assertThat(request.getRedirectUri()).isEqualTo("https://minimal.example.com/callback"),
                    () -> assertThat(request.getScope()).isNull(),
                    () -> assertThat(request.getState()).isNull(),
                    () -> assertThat(request.getCodeChallenge()).isNull(),
                    () -> assertThat(request.getCodeChallengeMethod()).isNull()
            );
        }
    }

    @Nested
    @DisplayName("OAuth2 Authorization Scenarios")
    class AuthorizationScenarioTests {

        @Test
        @DisplayName("Should create valid authorization request with PKCE S256")
        void shouldCreateValidAuthorizationRequestWithPkceS256() {
            // When
            OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.builder()
                    .clientId("mobile_app_client")
                    .responseType("code")
                    .redirectUri("com.focushive.mobile://oauth/callback")
                    .scope("openid profile")
                    .state("mobile_state_abc123")
                    .codeChallenge("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM")
                    .codeChallengeMethod("S256")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request.getClientId()).isEqualTo("mobile_app_client"),
                    () -> assertThat(request.getResponseType()).isEqualTo("code"),
                    () -> assertThat(request.getRedirectUri()).startsWith("com.focushive.mobile://"),
                    () -> assertThat(request.getScope()).contains("openid"),
                    () -> assertThat(request.getCodeChallengeMethod()).isEqualTo("S256"),
                    () -> assertThat(request.getCodeChallenge()).hasSize(43) // Base64 URL encoded SHA256
            );
        }

        @Test
        @DisplayName("Should create valid authorization request with PKCE plain")
        void shouldCreateValidAuthorizationRequestWithPkcePlain() {
            // When
            OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.builder()
                    .clientId("dev_client")
                    .responseType("code")
                    .redirectUri("http://localhost:3000/auth/callback")
                    .scope("read write")
                    .state("dev_state_xyz")
                    .codeChallenge("plain_challenge_verifier_string")
                    .codeChallengeMethod("plain")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request.getClientId()).isEqualTo("dev_client"),
                    () -> assertThat(request.getResponseType()).isEqualTo("code"),
                    () -> assertThat(request.getRedirectUri()).startsWith("http://localhost:3000"),
                    () -> assertThat(request.getScope()).isEqualTo("read write"),
                    () -> assertThat(request.getCodeChallengeMethod()).isEqualTo("plain"),
                    () -> assertThat(request.getCodeChallenge()).isEqualTo("plain_challenge_verifier_string")
            );
        }

        @Test
        @DisplayName("Should create authorization request without PKCE")
        void shouldCreateAuthorizationRequestWithoutPkce() {
            // When
            OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.builder()
                    .clientId("web_client_no_pkce")
                    .responseType("code")
                    .redirectUri("https://web.example.com/auth")
                    .scope("openid profile email")
                    .state("web_csrf_token_456")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request.getClientId()).isEqualTo("web_client_no_pkce"),
                    () -> assertThat(request.getResponseType()).isEqualTo("code"),
                    () -> assertThat(request.getRedirectUri()).isEqualTo("https://web.example.com/auth"),
                    () -> assertThat(request.getScope()).contains("openid"),
                    () -> assertThat(request.getState()).isEqualTo("web_csrf_token_456"),
                    () -> assertThat(request.getCodeChallenge()).isNull(),
                    () -> assertThat(request.getCodeChallengeMethod()).isNull()
            );
        }
    }

    @Nested
    @DisplayName("Validation Constraint Tests")
    class ValidationConstraintTests {

        @Test
        @DisplayName("Should pass validation with valid required fields")
        void shouldPassValidationWithValidRequiredFields() {
            // Given
            OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.builder()
                    .clientId("valid_client")
                    .responseType("code")
                    .redirectUri("https://valid.example.com/callback")
                    .build();

            // When
            Set<ConstraintViolation<OAuth2AuthorizeRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail validation when clientId is null")
        void shouldFailValidationWhenClientIdIsNull() {
            // Given
            OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.builder()
                    .clientId(null)
                    .responseType("code")
                    .redirectUri("https://example.com/callback")
                    .build();

            // When
            Set<ConstraintViolation<OAuth2AuthorizeRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).hasSize(1),
                    () -> {
                        ConstraintViolation<OAuth2AuthorizeRequest> violation = violations.iterator().next();
                        assertThat(violation.getMessage()).isEqualTo("Client ID is required");
                        assertThat(violation.getPropertyPath().toString()).isEqualTo("clientId");
                    }
            );
        }

        @Test
        @DisplayName("Should fail validation when clientId is empty")
        void shouldFailValidationWhenClientIdIsEmpty() {
            // Given
            OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.builder()
                    .clientId("")
                    .responseType("code")
                    .redirectUri("https://example.com/callback")
                    .build();

            // When
            Set<ConstraintViolation<OAuth2AuthorizeRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).hasSize(1),
                    () -> {
                        ConstraintViolation<OAuth2AuthorizeRequest> violation = violations.iterator().next();
                        assertThat(violation.getMessage()).isEqualTo("Client ID is required");
                    }
            );
        }

        @Test
        @DisplayName("Should fail validation when responseType is null")
        void shouldFailValidationWhenResponseTypeIsNull() {
            // Given
            OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.builder()
                    .clientId("valid_client")
                    .responseType(null)
                    .redirectUri("https://example.com/callback")
                    .build();

            // When
            Set<ConstraintViolation<OAuth2AuthorizeRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).hasSize(1),
                    () -> {
                        ConstraintViolation<OAuth2AuthorizeRequest> violation = violations.iterator().next();
                        assertThat(violation.getMessage()).isEqualTo("Response type is required");
                        assertThat(violation.getPropertyPath().toString()).isEqualTo("responseType");
                    }
            );
        }

        @Test
        @DisplayName("Should fail validation when redirectUri is blank")
        void shouldFailValidationWhenRedirectUriIsBlank() {
            // Given
            OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.builder()
                    .clientId("valid_client")
                    .responseType("code")
                    .redirectUri("   ")
                    .build();

            // When
            Set<ConstraintViolation<OAuth2AuthorizeRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).hasSize(1),
                    () -> {
                        ConstraintViolation<OAuth2AuthorizeRequest> violation = violations.iterator().next();
                        assertThat(violation.getMessage()).isEqualTo("Redirect URI is required");
                        assertThat(violation.getPropertyPath().toString()).isEqualTo("redirectUri");
                    }
            );
        }

        @Test
        @DisplayName("Should fail validation when multiple required fields are missing")
        void shouldFailValidationWhenMultipleRequiredFieldsAreMissing() {
            // Given
            OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.builder()
                    .clientId("")
                    .responseType("")
                    .redirectUri("")
                    .build();

            // When
            Set<ConstraintViolation<OAuth2AuthorizeRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).hasSize(3),
                    () -> {
                        Set<String> messages = violations.stream()
                                .map(ConstraintViolation::getMessage)
                                .collect(java.util.stream.Collectors.toSet());
                        assertThat(messages).containsExactlyInAnyOrder(
                                "Client ID is required",
                                "Response type is required",
                                "Redirect URI is required"
                        );
                    }
            );
        }
    }

    @Nested
    @DisplayName("JSON Serialization Tests")
    class JsonSerializationTests {

        @Test
        @DisplayName("Should serialize OAuth2AuthorizeRequest to JSON correctly")
        void shouldSerializeOAuth2AuthorizeRequestToJsonCorrectly() throws Exception {
            // Given
            OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.builder()
                    .clientId("web_client")
                    .responseType("code")
                    .redirectUri("https://app.example.com/callback")
                    .scope("openid profile email")
                    .state("csrf_state_token")
                    .codeChallenge("challenge_string")
                    .codeChallengeMethod("S256")
                    .build();

            // When
            String json = objectMapper.writeValueAsString(request);

            // Then
            assertAll(
                    () -> assertThat(json).contains("\"clientId\":\"web_client\""),
                    () -> assertThat(json).contains("\"responseType\":\"code\""),
                    () -> assertThat(json).contains("\"redirectUri\":\"https://app.example.com/callback\""),
                    () -> assertThat(json).contains("\"scope\":\"openid profile email\""),
                    () -> assertThat(json).contains("\"state\":\"csrf_state_token\""),
                    () -> assertThat(json).contains("\"codeChallenge\":\"challenge_string\""),
                    () -> assertThat(json).contains("\"codeChallengeMethod\":\"S256\"")
            );
        }

        @Test
        @DisplayName("Should deserialize JSON to OAuth2AuthorizeRequest correctly")
        void shouldDeserializeJsonToOAuth2AuthorizeRequestCorrectly() throws Exception {
            // Given
            String json = """
                {
                    "clientId": "mobile_client",
                    "responseType": "code",
                    "redirectUri": "com.example.app://callback",
                    "scope": "read write profile",
                    "state": "mobile_state_abc",
                    "codeChallenge": "mobile_challenge_xyz",
                    "codeChallengeMethod": "S256"
                }
                """;

            // When
            OAuth2AuthorizeRequest request = objectMapper.readValue(json, OAuth2AuthorizeRequest.class);

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getClientId()).isEqualTo("mobile_client"),
                    () -> assertThat(request.getResponseType()).isEqualTo("code"),
                    () -> assertThat(request.getRedirectUri()).isEqualTo("com.example.app://callback"),
                    () -> assertThat(request.getScope()).isEqualTo("read write profile"),
                    () -> assertThat(request.getState()).isEqualTo("mobile_state_abc"),
                    () -> assertThat(request.getCodeChallenge()).isEqualTo("mobile_challenge_xyz"),
                    () -> assertThat(request.getCodeChallengeMethod()).isEqualTo("S256")
            );
        }

        @Test
        @DisplayName("Should handle null optional fields in JSON serialization")
        void shouldHandleNullOptionalFieldsInJsonSerialization() throws Exception {
            // Given
            OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.builder()
                    .clientId("minimal_client")
                    .responseType("code")
                    .redirectUri("https://minimal.com/callback")
                    .build();

            // When
            String json = objectMapper.writeValueAsString(request);

            // Then
            assertAll(
                    () -> assertThat(json).contains("\"clientId\":\"minimal_client\""),
                    () -> assertThat(json).contains("\"responseType\":\"code\""),
                    () -> assertThat(json).contains("\"redirectUri\":\"https://minimal.com/callback\""),
                    () -> assertThat(json).contains("\"scope\":null"),
                    () -> assertThat(json).contains("\"state\":null"),
                    () -> assertThat(json).contains("\"codeChallenge\":null"),
                    () -> assertThat(json).contains("\"codeChallengeMethod\":null")
            );
        }

        @Test
        @DisplayName("Should handle special characters in serialization")
        void shouldHandleSpecialCharactersInSerialization() throws Exception {
            // Given
            String specialState = "state_with_special_chars_!@#$%^&*()";
            String specialScope = "read:special write:admin user:profile";
            
            OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.builder()
                    .clientId("special_client")
                    .responseType("code")
                    .redirectUri("https://example.com/callback?param=value&other=test")
                    .scope(specialScope)
                    .state(specialState)
                    .build();

            // When
            String json = objectMapper.writeValueAsString(request);
            OAuth2AuthorizeRequest deserialized = objectMapper.readValue(json, OAuth2AuthorizeRequest.class);

            // Then
            assertAll(
                    () -> assertThat(deserialized.getState()).isEqualTo(specialState),
                    () -> assertThat(deserialized.getScope()).isEqualTo(specialScope),
                    () -> assertThat(deserialized.getRedirectUri()).contains("param=value&other=test")
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
            OAuth2AuthorizeRequest request1 = OAuth2AuthorizeRequest.builder()
                    .clientId("same_client")
                    .responseType("code")
                    .redirectUri("https://same.uri.com")
                    .scope("same scope")
                    .state("same_state")
                    .build();
                    
            OAuth2AuthorizeRequest request2 = OAuth2AuthorizeRequest.builder()
                    .clientId("same_client")
                    .responseType("code")
                    .redirectUri("https://same.uri.com")
                    .scope("same scope")
                    .state("same_state")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request1).isEqualTo(request2),
                    () -> assertThat(request1.hashCode()).isEqualTo(request2.hashCode())
            );
        }

        @Test
        @DisplayName("Should not be equal when client IDs differ")
        void shouldNotBeEqualWhenClientIdsDiffer() {
            // Given
            OAuth2AuthorizeRequest request1 = OAuth2AuthorizeRequest.builder()
                    .clientId("client_1")
                    .responseType("code")
                    .redirectUri("https://same.uri.com")
                    .build();
                    
            OAuth2AuthorizeRequest request2 = OAuth2AuthorizeRequest.builder()
                    .clientId("client_2")
                    .responseType("code")
                    .redirectUri("https://same.uri.com")
                    .build();

            // Then
            assertThat(request1).isNotEqualTo(request2);
        }

        @Test
        @DisplayName("Should handle null values in equals comparison")
        void shouldHandleNullValuesInEqualsComparison() {
            // Given
            OAuth2AuthorizeRequest request1 = OAuth2AuthorizeRequest.builder()
                    .clientId("same_client")
                    .responseType("code")
                    .redirectUri("https://example.com")
                    .scope(null)
                    .state(null)
                    .build();
                    
            OAuth2AuthorizeRequest request2 = OAuth2AuthorizeRequest.builder()
                    .clientId("same_client")
                    .responseType("code")
                    .redirectUri("https://example.com")
                    .scope(null)
                    .state(null)
                    .build();

            // Then
            assertThat(request1).isEqualTo(request2);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Security Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long state parameter")
        void shouldHandleVeryLongStateParameter() {
            // Given
            String longState = "a".repeat(1000);
            
            // When
            OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.builder()
                    .clientId("client")
                    .responseType("code")
                    .redirectUri("https://example.com/callback")
                    .state(longState)
                    .build();

            // Then
            assertThat(request.getState()).hasSize(1000);
        }

        @Test
        @DisplayName("Should handle multiple URI schemes in redirectUri")
        void shouldHandleMultipleUriSchemesInRedirectUri() {
            // Given
            String[] uriSchemes = {
                    "https://app.focushive.com/callback",
                    "http://localhost:3000/auth/callback",
                    "focushive://oauth/callback",
                    "com.focushive.mobile://auth",
                    "urn:ietf:wg:oauth:2.0:oob"
            };

            // When/Then
            for (String uri : uriSchemes) {
                OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.builder()
                        .clientId("test_client")
                        .responseType("code")
                        .redirectUri(uri)
                        .build();
                        
                assertThat(request.getRedirectUri()).isEqualTo(uri);
            }
        }

        @Test
        @DisplayName("Should handle complex scopes with special characters")
        void shouldHandleComplexScopesWithSpecialCharacters() {
            // Given/When
            OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.builder()
                    .clientId("scoped_client")
                    .responseType("code")
                    .redirectUri("https://example.com/callback")
                    .scope("openid profile email user:read user:write admin:all api:v1 api:v2")
                    .build();

            // Then
            assertThat(request.getScope())
                    .contains("openid")
                    .contains("user:read")
                    .contains("admin:all")
                    .contains("api:v1");
        }

        @Test
        @DisplayName("Should handle Base64 URL safe PKCE challenges")
        void shouldHandleBase64UrlSafePkceChallenges() {
            // Given
            String base64Challenge = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
            
            // When
            OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.builder()
                    .clientId("pkce_client")
                    .responseType("code")
                    .redirectUri("https://example.com/callback")
                    .codeChallenge(base64Challenge)
                    .codeChallengeMethod("S256")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request.getCodeChallenge()).isEqualTo(base64Challenge),
                    () -> assertThat(request.getCodeChallenge()).hasSize(43),
                    () -> assertThat(request.getCodeChallenge()).matches("[A-Za-z0-9\\-_]+"),
                    () -> assertThat(request.getCodeChallengeMethod()).isEqualTo("S256")
            );
        }

        @Test
        @DisplayName("Should handle both PKCE challenge methods")
        void shouldHandleBothPkceChallengeMethod() {
            // Given
            String[] validMethods = {"plain", "S256"};

            // When/Then
            for (String method : validMethods) {
                OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.builder()
                        .clientId("method_client")
                        .responseType("code")
                        .redirectUri("https://example.com/callback")
                        .codeChallenge("test_challenge")
                        .codeChallengeMethod(method)
                        .build();
                        
                assertThat(request.getCodeChallengeMethod()).isEqualTo(method);
            }
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create instance using no-args constructor")
        void shouldCreateInstanceUsingNoArgsConstructor() {
            // When
            OAuth2AuthorizeRequest request = new OAuth2AuthorizeRequest();

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getClientId()).isNull(),
                    () -> assertThat(request.getResponseType()).isNull(),
                    () -> assertThat(request.getRedirectUri()).isNull(),
                    () -> assertThat(request.getScope()).isNull(),
                    () -> assertThat(request.getState()).isNull(),
                    () -> assertThat(request.getCodeChallenge()).isNull(),
                    () -> assertThat(request.getCodeChallengeMethod()).isNull()
            );
        }

        @Test
        @DisplayName("Should allow modification after creation")
        void shouldAllowModificationAfterCreation() {
            // Given
            OAuth2AuthorizeRequest request = new OAuth2AuthorizeRequest();

            // When
            request.setClientId("dynamic_client");
            request.setResponseType("code");
            request.setRedirectUri("https://dynamic.com/callback");
            request.setScope("dynamic scope");
            request.setState("dynamic_state");

            // Then
            assertAll(
                    () -> assertThat(request.getClientId()).isEqualTo("dynamic_client"),
                    () -> assertThat(request.getResponseType()).isEqualTo("code"),
                    () -> assertThat(request.getRedirectUri()).isEqualTo("https://dynamic.com/callback"),
                    () -> assertThat(request.getScope()).isEqualTo("dynamic scope"),
                    () -> assertThat(request.getState()).isEqualTo("dynamic_state")
            );
        }
    }
}