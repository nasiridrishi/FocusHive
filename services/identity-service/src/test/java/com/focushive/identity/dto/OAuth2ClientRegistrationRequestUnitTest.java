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
 * Comprehensive unit tests for OAuth2ClientRegistrationRequest DTO.
 * Tests client registration, validation constraints, and serialization.
 */
@DisplayName("OAuth2ClientRegistrationRequest DTO Unit Tests")
class OAuth2ClientRegistrationRequestUnitTest {

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
        @DisplayName("Should create OAuth2ClientRegistrationRequest using builder with all fields")
        void shouldCreateOAuth2ClientRegistrationRequestUsingBuilderWithAllFields() {
            // When
            OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
                    .clientName("FocusHive Web Application")
                    .description("Official FocusHive web client for browser-based access")
                    .redirectUris(Set.of("https://app.focushive.com/oauth/callback", "https://app.focushive.com/auth/callback"))
                    .grantTypes(Set.of("authorization_code", "refresh_token"))
                    .scopes(Set.of("openid", "profile", "email", "read", "write"))
                    .accessTokenValiditySeconds(7200)
                    .refreshTokenValiditySeconds(7776000)
                    .autoApprove(true)
                    .clientType("confidential")
                    .applicationType("web")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getClientName()).isEqualTo("FocusHive Web Application"),
                    () -> assertThat(request.getDescription()).isEqualTo("Official FocusHive web client for browser-based access"),
                    () -> assertThat(request.getRedirectUris()).containsExactlyInAnyOrder(
                            "https://app.focushive.com/oauth/callback",
                            "https://app.focushive.com/auth/callback"
                    ),
                    () -> assertThat(request.getGrantTypes()).containsExactlyInAnyOrder("authorization_code", "refresh_token"),
                    () -> assertThat(request.getScopes()).containsExactlyInAnyOrder("openid", "profile", "email", "read", "write"),
                    () -> assertThat(request.getAccessTokenValiditySeconds()).isEqualTo(7200),
                    () -> assertThat(request.getRefreshTokenValiditySeconds()).isEqualTo(7776000),
                    () -> assertThat(request.getAutoApprove()).isTrue(),
                    () -> assertThat(request.getClientType()).isEqualTo("confidential"),
                    () -> assertThat(request.getApplicationType()).isEqualTo("web")
            );
        }

        @Test
        @DisplayName("Should create OAuth2ClientRegistrationRequest with minimal required fields and defaults")
        void shouldCreateOAuth2ClientRegistrationRequestWithMinimalRequiredFieldsAndDefaults() {
            // When
            OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
                    .clientName("Minimal Client")
                    .redirectUris(Set.of("https://minimal.example.com/callback"))
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getClientName()).isEqualTo("Minimal Client"),
                    () -> assertThat(request.getDescription()).isNull(),
                    () -> assertThat(request.getRedirectUris()).containsExactly("https://minimal.example.com/callback"),
                    () -> assertThat(request.getGrantTypes()).containsExactly("authorization_code"),
                    () -> assertThat(request.getScopes()).containsExactlyInAnyOrder("openid", "profile"),
                    () -> assertThat(request.getAccessTokenValiditySeconds()).isEqualTo(3600),
                    () -> assertThat(request.getRefreshTokenValiditySeconds()).isEqualTo(2592000),
                    () -> assertThat(request.getAutoApprove()).isFalse(),
                    () -> assertThat(request.getClientType()).isEqualTo("confidential"),
                    () -> assertThat(request.getApplicationType()).isEqualTo("web")
            );
        }
    }

    @Nested
    @DisplayName("Client Registration Scenarios")
    class ClientRegistrationScenarioTests {

        @Test
        @DisplayName("Should create valid web client registration")
        void shouldCreateValidWebClientRegistration() {
            // When
            OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
                    .clientName("FocusHive Web Client")
                    .description("Web-based client for FocusHive productivity platform")
                    .redirectUris(Set.of("https://app.focushive.com/oauth/callback"))
                    .grantTypes(Set.of("authorization_code", "refresh_token"))
                    .scopes(Set.of("openid", "profile", "email", "read", "write"))
                    .accessTokenValiditySeconds(3600)
                    .refreshTokenValiditySeconds(2592000)
                    .autoApprove(false)
                    .clientType("confidential")
                    .applicationType("web")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request.getClientName()).isEqualTo("FocusHive Web Client"),
                    () -> assertThat(request.getClientType()).isEqualTo("confidential"),
                    () -> assertThat(request.getApplicationType()).isEqualTo("web"),
                    () -> assertThat(request.getRedirectUris()).containsExactly("https://app.focushive.com/oauth/callback"),
                    () -> assertThat(request.getGrantTypes()).contains("authorization_code", "refresh_token"),
                    () -> assertThat(request.getScopes()).contains("openid", "profile")
            );
        }

        @Test
        @DisplayName("Should create valid mobile client registration")
        void shouldCreateValidMobileClientRegistration() {
            // When
            OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
                    .clientName("FocusHive Mobile App")
                    .description("Native mobile application for iOS and Android")
                    .redirectUris(Set.of("com.focushive.mobile://oauth/callback", "focushive://auth"))
                    .grantTypes(Set.of("authorization_code", "refresh_token"))
                    .scopes(Set.of("openid", "profile", "offline_access"))
                    .accessTokenValiditySeconds(1800)
                    .refreshTokenValiditySeconds(7776000)
                    .autoApprove(false)
                    .clientType("public")
                    .applicationType("native")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request.getClientName()).isEqualTo("FocusHive Mobile App"),
                    () -> assertThat(request.getClientType()).isEqualTo("public"),
                    () -> assertThat(request.getApplicationType()).isEqualTo("native"),
                    () -> assertThat(request.getRedirectUris()).containsExactlyInAnyOrder(
                            "com.focushive.mobile://oauth/callback", "focushive://auth"
                    ),
                    () -> assertThat(request.getScopes()).contains("offline_access"),
                    () -> assertThat(request.getAccessTokenValiditySeconds()).isEqualTo(1800) // Shorter for mobile
            );
        }

        @Test
        @DisplayName("Should create valid service client registration")
        void shouldCreateValidServiceClientRegistration() {
            // When
            OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
                    .clientName("FocusHive Analytics Service")
                    .description("Backend service for processing analytics data")
                    .redirectUris(Set.of("https://api.focushive.com/oauth/callback"))
                    .grantTypes(Set.of("client_credentials", "authorization_code"))
                    .scopes(Set.of("api:read", "api:write", "analytics:process"))
                    .accessTokenValiditySeconds(7200)
                    .refreshTokenValiditySeconds(0) // No refresh tokens for client_credentials
                    .autoApprove(true)
                    .clientType("confidential")
                    .applicationType("web")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request.getClientName()).isEqualTo("FocusHive Analytics Service"),
                    () -> assertThat(request.getGrantTypes()).contains("client_credentials"),
                    () -> assertThat(request.getScopes()).containsExactlyInAnyOrder("api:read", "api:write", "analytics:process"),
                    () -> assertThat(request.getAutoApprove()).isTrue(),
                    () -> assertThat(request.getRefreshTokenValiditySeconds()).isEqualTo(0)
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
            OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
                    .clientName("Valid Client")
                    .redirectUris(Set.of("https://valid.example.com/callback"))
                    .build();

            // When
            Set<ConstraintViolation<OAuth2ClientRegistrationRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail validation when clientName is null")
        void shouldFailValidationWhenClientNameIsNull() {
            // Given
            OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
                    .clientName(null)
                    .redirectUris(Set.of("https://example.com/callback"))
                    .build();

            // When
            Set<ConstraintViolation<OAuth2ClientRegistrationRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).hasSize(1),
                    () -> {
                        ConstraintViolation<OAuth2ClientRegistrationRequest> violation = violations.iterator().next();
                        assertThat(violation.getMessage()).isEqualTo("Client name is required");
                        assertThat(violation.getPropertyPath().toString()).isEqualTo("clientName");
                    }
            );
        }

        @Test
        @DisplayName("Should fail validation when clientName is blank")
        void shouldFailValidationWhenClientNameIsBlank() {
            // Given
            OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
                    .clientName("   ")
                    .redirectUris(Set.of("https://example.com/callback"))
                    .build();

            // When
            Set<ConstraintViolation<OAuth2ClientRegistrationRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).hasSize(1),
                    () -> {
                        ConstraintViolation<OAuth2ClientRegistrationRequest> violation = violations.iterator().next();
                        assertThat(violation.getMessage()).isEqualTo("Client name is required");
                    }
            );
        }

        @Test
        @DisplayName("Should fail validation when redirectUris is empty")
        void shouldFailValidationWhenRedirectUrisIsEmpty() {
            // Given
            OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
                    .clientName("Valid Client")
                    .redirectUris(Set.of())
                    .build();

            // When
            Set<ConstraintViolation<OAuth2ClientRegistrationRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).hasSize(1),
                    () -> {
                        ConstraintViolation<OAuth2ClientRegistrationRequest> violation = violations.iterator().next();
                        assertThat(violation.getMessage()).isEqualTo("At least one redirect URI is required");
                        assertThat(violation.getPropertyPath().toString()).isEqualTo("redirectUris");
                    }
            );
        }

        @Test
        @DisplayName("Should fail validation when redirectUris is null")
        void shouldFailValidationWhenRedirectUrisIsNull() {
            // Given
            OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
                    .clientName("Valid Client")
                    .redirectUris(null)
                    .build();

            // When
            Set<ConstraintViolation<OAuth2ClientRegistrationRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).hasSize(1),
                    () -> {
                        ConstraintViolation<OAuth2ClientRegistrationRequest> violation = violations.iterator().next();
                        assertThat(violation.getMessage()).isEqualTo("At least one redirect URI is required");
                    }
            );
        }

        @Test
        @DisplayName("Should fail validation when multiple required fields are missing")
        void shouldFailValidationWhenMultipleRequiredFieldsAreMissing() {
            // Given
            OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
                    .clientName("")
                    .redirectUris(Set.of())
                    .build();

            // When
            Set<ConstraintViolation<OAuth2ClientRegistrationRequest>> violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertThat(violations).hasSize(2),
                    () -> {
                        Set<String> messages = violations.stream()
                                .map(ConstraintViolation::getMessage)
                                .collect(java.util.stream.Collectors.toSet());
                        assertThat(messages).containsExactlyInAnyOrder(
                                "Client name is required",
                                "At least one redirect URI is required"
                        );
                    }
            );
        }
    }

    @Nested
    @DisplayName("JSON Serialization Tests")
    class JsonSerializationTests {

        @Test
        @DisplayName("Should serialize OAuth2ClientRegistrationRequest to JSON correctly")
        void shouldSerializeOAuth2ClientRegistrationRequestToJsonCorrectly() throws Exception {
            // Given
            OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
                    .clientName("Test Client")
                    .description("Test client description")
                    .redirectUris(Set.of("https://test.example.com/callback"))
                    .grantTypes(Set.of("authorization_code", "refresh_token"))
                    .scopes(Set.of("openid", "profile", "email"))
                    .accessTokenValiditySeconds(3600)
                    .refreshTokenValiditySeconds(2592000)
                    .autoApprove(false)
                    .clientType("confidential")
                    .applicationType("web")
                    .build();

            // When
            String json = objectMapper.writeValueAsString(request);

            // Then
            assertAll(
                    () -> assertThat(json).contains("\"clientName\":\"Test Client\""),
                    () -> assertThat(json).contains("\"description\":\"Test client description\""),
                    () -> assertThat(json).contains("\"redirectUris\":[\"https://test.example.com/callback\"]"),
                    () -> assertThat(json).contains("\"grantTypes\""),
                    () -> assertThat(json).contains("\"scopes\""),
                    () -> assertThat(json).contains("\"accessTokenValiditySeconds\":3600"),
                    () -> assertThat(json).contains("\"refreshTokenValiditySeconds\":2592000"),
                    () -> assertThat(json).contains("\"autoApprove\":false"),
                    () -> assertThat(json).contains("\"clientType\":\"confidential\""),
                    () -> assertThat(json).contains("\"applicationType\":\"web\"")
            );
        }

        @Test
        @DisplayName("Should deserialize JSON to OAuth2ClientRegistrationRequest correctly")
        void shouldDeserializeJsonToOAuth2ClientRegistrationRequestCorrectly() throws Exception {
            // Given
            String json = """
                {
                    "clientName": "Mobile App Client",
                    "description": "Native mobile application",
                    "redirectUris": ["com.example.app://callback", "example://oauth"],
                    "grantTypes": ["authorization_code", "refresh_token"],
                    "scopes": ["openid", "profile", "offline_access"],
                    "accessTokenValiditySeconds": 1800,
                    "refreshTokenValiditySeconds": 7776000,
                    "autoApprove": true,
                    "clientType": "public",
                    "applicationType": "native"
                }
                """;

            // When
            OAuth2ClientRegistrationRequest request = objectMapper.readValue(json, OAuth2ClientRegistrationRequest.class);

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getClientName()).isEqualTo("Mobile App Client"),
                    () -> assertThat(request.getDescription()).isEqualTo("Native mobile application"),
                    () -> assertThat(request.getRedirectUris()).containsExactlyInAnyOrder(
                            "com.example.app://callback", "example://oauth"
                    ),
                    () -> assertThat(request.getGrantTypes()).containsExactlyInAnyOrder("authorization_code", "refresh_token"),
                    () -> assertThat(request.getScopes()).containsExactlyInAnyOrder("openid", "profile", "offline_access"),
                    () -> assertThat(request.getAccessTokenValiditySeconds()).isEqualTo(1800),
                    () -> assertThat(request.getRefreshTokenValiditySeconds()).isEqualTo(7776000),
                    () -> assertThat(request.getAutoApprove()).isTrue(),
                    () -> assertThat(request.getClientType()).isEqualTo("public"),
                    () -> assertThat(request.getApplicationType()).isEqualTo("native")
            );
        }

        @Test
        @DisplayName("Should handle default values in JSON serialization")
        void shouldHandleDefaultValuesInJsonSerialization() throws Exception {
            // Given
            OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
                    .clientName("Default Values Client")
                    .redirectUris(Set.of("https://example.com/callback"))
                    .build();

            // When
            String json = objectMapper.writeValueAsString(request);
            OAuth2ClientRegistrationRequest deserialized = objectMapper.readValue(json, OAuth2ClientRegistrationRequest.class);

            // Then
            assertAll(
                    () -> assertThat(deserialized.getGrantTypes()).containsExactly("authorization_code"),
                    () -> assertThat(deserialized.getScopes()).containsExactlyInAnyOrder("openid", "profile"),
                    () -> assertThat(deserialized.getAccessTokenValiditySeconds()).isEqualTo(3600),
                    () -> assertThat(deserialized.getRefreshTokenValiditySeconds()).isEqualTo(2592000),
                    () -> assertThat(deserialized.getAutoApprove()).isFalse(),
                    () -> assertThat(deserialized.getClientType()).isEqualTo("confidential"),
                    () -> assertThat(deserialized.getApplicationType()).isEqualTo("web")
            );
        }

        @Test
        @DisplayName("Should handle complex redirect URIs in serialization")
        void shouldHandleComplexRedirectUrisInSerialization() throws Exception {
            // Given
            Set<String> complexUris = Set.of(
                    "https://app.example.com/oauth/callback?state=xyz",
                    "com.example.mobile://oauth/callback",
                    "http://localhost:3000/auth/callback",
                    "urn:ietf:wg:oauth:2.0:oob"
            );
            
            OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
                    .clientName("Complex URI Client")
                    .redirectUris(complexUris)
                    .build();

            // When
            String json = objectMapper.writeValueAsString(request);
            OAuth2ClientRegistrationRequest deserialized = objectMapper.readValue(json, OAuth2ClientRegistrationRequest.class);

            // Then
            assertThat(deserialized.getRedirectUris()).containsExactlyInAnyOrderElementsOf(complexUris);
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when all fields are the same")
        void shouldBeEqualWhenAllFieldsAreTheSame() {
            // Given
            OAuth2ClientRegistrationRequest request1 = OAuth2ClientRegistrationRequest.builder()
                    .clientName("Same Client")
                    .description("Same description")
                    .redirectUris(Set.of("https://same.uri.com"))
                    .grantTypes(Set.of("authorization_code"))
                    .scopes(Set.of("openid", "profile"))
                    .build();
                    
            OAuth2ClientRegistrationRequest request2 = OAuth2ClientRegistrationRequest.builder()
                    .clientName("Same Client")
                    .description("Same description")
                    .redirectUris(Set.of("https://same.uri.com"))
                    .grantTypes(Set.of("authorization_code"))
                    .scopes(Set.of("openid", "profile"))
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request1).isEqualTo(request2),
                    () -> assertThat(request1.hashCode()).isEqualTo(request2.hashCode())
            );
        }

        @Test
        @DisplayName("Should not be equal when client names differ")
        void shouldNotBeEqualWhenClientNamesDiffer() {
            // Given
            OAuth2ClientRegistrationRequest request1 = OAuth2ClientRegistrationRequest.builder()
                    .clientName("Client One")
                    .redirectUris(Set.of("https://same.uri.com"))
                    .build();
                    
            OAuth2ClientRegistrationRequest request2 = OAuth2ClientRegistrationRequest.builder()
                    .clientName("Client Two")
                    .redirectUris(Set.of("https://same.uri.com"))
                    .build();

            // Then
            assertThat(request1).isNotEqualTo(request2);
        }

        @Test
        @DisplayName("Should not be equal when redirect URIs differ")
        void shouldNotBeEqualWhenRedirectUrisDiffer() {
            // Given
            OAuth2ClientRegistrationRequest request1 = OAuth2ClientRegistrationRequest.builder()
                    .clientName("Same Client")
                    .redirectUris(Set.of("https://one.uri.com"))
                    .build();
                    
            OAuth2ClientRegistrationRequest request2 = OAuth2ClientRegistrationRequest.builder()
                    .clientName("Same Client")
                    .redirectUris(Set.of("https://two.uri.com"))
                    .build();

            // Then
            assertThat(request1).isNotEqualTo(request2);
        }

        @Test
        @DisplayName("Should handle null values in equals comparison")
        void shouldHandleNullValuesInEqualsComparison() {
            // Given
            OAuth2ClientRegistrationRequest request1 = OAuth2ClientRegistrationRequest.builder()
                    .clientName("Same Client")
                    .description(null)
                    .redirectUris(Set.of("https://same.uri.com"))
                    .build();
                    
            OAuth2ClientRegistrationRequest request2 = OAuth2ClientRegistrationRequest.builder()
                    .clientName("Same Client")
                    .description(null)
                    .redirectUris(Set.of("https://same.uri.com"))
                    .build();

            // Then
            assertThat(request1).isEqualTo(request2);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Business Logic Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long client names and descriptions")
        void shouldHandleVeryLongClientNamesAndDescriptions() {
            // Given
            String longName = "A".repeat(1000);
            String longDescription = "B".repeat(2000);
            
            // When
            OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
                    .clientName(longName)
                    .description(longDescription)
                    .redirectUris(Set.of("https://example.com/callback"))
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request.getClientName()).hasSize(1000),
                    () -> assertThat(request.getDescription()).hasSize(2000)
            );
        }

        @Test
        @DisplayName("Should handle multiple redirect URIs with various schemes")
        void shouldHandleMultipleRedirectUrisWithVariousSchemes() {
            // Given
            Set<String> diverseUris = Set.of(
                    "https://web.focushive.com/callback",
                    "http://localhost:8080/auth/callback",
                    "focushive://oauth/callback",
                    "com.focushive.mobile://auth/callback",
                    "urn:ietf:wg:oauth:2.0:oob",
                    "https://staging.focushive.com/oauth/callback?env=staging"
            );

            // When
            OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
                    .clientName("Multi-URI Client")
                    .redirectUris(diverseUris)
                    .build();

            // Then
            assertThat(request.getRedirectUris()).containsExactlyInAnyOrderElementsOf(diverseUris);
        }

        @Test
        @DisplayName("Should handle all supported grant types")
        void shouldHandleAllSupportedGrantTypes() {
            // Given
            Set<String> allGrantTypes = Set.of(
                    "authorization_code",
                    "refresh_token", 
                    "client_credentials",
                    "password",
                    "implicit"
            );

            // When
            OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
                    .clientName("All Grant Types Client")
                    .redirectUris(Set.of("https://example.com/callback"))
                    .grantTypes(allGrantTypes)
                    .build();

            // Then
            assertThat(request.getGrantTypes()).containsExactlyInAnyOrderElementsOf(allGrantTypes);
        }

        @Test
        @DisplayName("Should handle extensive scope combinations")
        void shouldHandleExtensiveScopeCombinations() {
            // Given
            Set<String> extensiveScopes = Set.of(
                    "openid", "profile", "email", "address", "phone",
                    "read", "write", "admin", "delete",
                    "user:profile", "user:settings", "user:data",
                    "api:v1", "api:v2", "api:admin",
                    "offline_access"
            );

            // When
            OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
                    .clientName("Extensive Scopes Client")
                    .redirectUris(Set.of("https://example.com/callback"))
                    .scopes(extensiveScopes)
                    .build();

            // Then
            assertThat(request.getScopes()).containsExactlyInAnyOrderElementsOf(extensiveScopes);
        }

        @Test
        @DisplayName("Should handle boundary values for token validity")
        void shouldHandleBoundaryValuesForTokenValidity() {
            // Given/When
            OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
                    .clientName("Boundary Values Client")
                    .redirectUris(Set.of("https://example.com/callback"))
                    .accessTokenValiditySeconds(1) // Very short
                    .refreshTokenValiditySeconds(Integer.MAX_VALUE) // Very long
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request.getAccessTokenValiditySeconds()).isEqualTo(1),
                    () -> assertThat(request.getRefreshTokenValiditySeconds()).isEqualTo(Integer.MAX_VALUE)
            );
        }

        @Test
        @DisplayName("Should handle all client types and application types")
        void shouldHandleAllClientTypesAndApplicationTypes() {
            // Given
            String[] clientTypes = {"public", "confidential"};
            String[] applicationTypes = {"web", "native"};

            // When/Then
            for (String clientType : clientTypes) {
                for (String applicationType : applicationTypes) {
                    OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
                            .clientName("Type Test Client")
                            .redirectUris(Set.of("https://example.com/callback"))
                            .clientType(clientType)
                            .applicationType(applicationType)
                            .build();
                            
                    assertAll(
                            () -> assertThat(request.getClientType()).isEqualTo(clientType),
                            () -> assertThat(request.getApplicationType()).isEqualTo(applicationType)
                    );
                }
            }
        }

        @Test
        @DisplayName("Should handle special characters in client names and descriptions")
        void shouldHandleSpecialCharactersInClientNamesAndDescriptions() {
            // Given
            String specialCharsName = "Client with Special !@#$%^&*()_+-={}[]|\\:;\"'<>,.?/~ Characters";
            String unicodeName = "Клиент с русскими символами 中文字符 🚀 📱 💻";
            
            // When
            OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
                    .clientName(specialCharsName)
                    .description(unicodeName)
                    .redirectUris(Set.of("https://example.com/callback"))
                    .build();

            // Then
            assertAll(
                    () -> assertThat(request.getClientName()).isEqualTo(specialCharsName),
                    () -> assertThat(request.getDescription()).isEqualTo(unicodeName)
            );
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create instance using no-args constructor with default values")
        void shouldCreateInstanceUsingNoArgsConstructorWithDefaultValues() {
            // When
            OAuth2ClientRegistrationRequest request = new OAuth2ClientRegistrationRequest();

            // Then
            assertAll(
                    () -> assertThat(request).isNotNull(),
                    () -> assertThat(request.getClientName()).isNull(),
                    () -> assertThat(request.getDescription()).isNull(),
                    () -> assertThat(request.getRedirectUris()).isNull(),
                    () -> assertThat(request.getGrantTypes()).containsExactly("authorization_code"),
                    () -> assertThat(request.getScopes()).containsExactlyInAnyOrder("openid", "profile"),
                    () -> assertThat(request.getAccessTokenValiditySeconds()).isEqualTo(3600),
                    () -> assertThat(request.getRefreshTokenValiditySeconds()).isEqualTo(2592000),
                    () -> assertThat(request.getAutoApprove()).isFalse(),
                    () -> assertThat(request.getClientType()).isEqualTo("confidential"),
                    () -> assertThat(request.getApplicationType()).isEqualTo("web")
            );
        }

        @Test
        @DisplayName("Should allow modification after creation")
        void shouldAllowModificationAfterCreation() {
            // Given
            OAuth2ClientRegistrationRequest request = new OAuth2ClientRegistrationRequest();

            // When
            request.setClientName("Dynamic Client");
            request.setDescription("Dynamically configured client");
            request.setRedirectUris(Set.of("https://dynamic.com/callback"));
            request.setGrantTypes(Set.of("client_credentials"));
            request.setScopes(Set.of("api:read", "api:write"));
            request.setAutoApprove(true);
            request.setClientType("public");

            // Then
            assertAll(
                    () -> assertThat(request.getClientName()).isEqualTo("Dynamic Client"),
                    () -> assertThat(request.getDescription()).isEqualTo("Dynamically configured client"),
                    () -> assertThat(request.getRedirectUris()).containsExactly("https://dynamic.com/callback"),
                    () -> assertThat(request.getGrantTypes()).containsExactly("client_credentials"),
                    () -> assertThat(request.getScopes()).containsExactlyInAnyOrder("api:read", "api:write"),
                    () -> assertThat(request.getAutoApprove()).isTrue(),
                    () -> assertThat(request.getClientType()).isEqualTo("public")
            );
        }
    }
}