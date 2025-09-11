package com.focushive.identity.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Comprehensive unit tests for OAuth2ClientResponse DTO.
 * Tests OAuth2 client response serialization and data handling.
 */
@DisplayName("OAuth2ClientResponse DTO Unit Tests")
class OAuth2ClientResponseUnitTest {

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
        @DisplayName("Should create OAuth2ClientResponse using builder with all fields")
        void shouldCreateOAuth2ClientResponseUsingBuilderWithAllFields() {
            // Given
            Instant now = Instant.now();
            Instant lastUsed = now.minusSeconds(3600);
            
            // When
            OAuth2ClientResponse response = OAuth2ClientResponse.builder()
                    .clientId("focushive_web_client_123")
                    .clientSecret("super_secret_key_abc456")
                    .clientName("FocusHive Web Application")
                    .description("Official FocusHive web client for productivity management")
                    .redirectUris(Set.of("https://app.focushive.com/oauth/callback", "https://app.focushive.com/auth/callback"))
                    .grantTypes(Set.of("authorization_code", "refresh_token"))
                    .scopes(Set.of("openid", "profile", "email", "read", "write"))
                    .accessTokenValiditySeconds(7200)
                    .refreshTokenValiditySeconds(7776000)
                    .autoApprove(true)
                    .clientType("confidential")
                    .applicationType("web")
                    .enabled(true)
                    .createdAt(now)
                    .lastUsedAt(lastUsed)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getClientId()).isEqualTo("focushive_web_client_123"),
                    () -> assertThat(response.getClientSecret()).isEqualTo("super_secret_key_abc456"),
                    () -> assertThat(response.getClientName()).isEqualTo("FocusHive Web Application"),
                    () -> assertThat(response.getDescription()).isEqualTo("Official FocusHive web client for productivity management"),
                    () -> assertThat(response.getRedirectUris()).containsExactlyInAnyOrder(
                            "https://app.focushive.com/oauth/callback",
                            "https://app.focushive.com/auth/callback"
                    ),
                    () -> assertThat(response.getGrantTypes()).containsExactlyInAnyOrder("authorization_code", "refresh_token"),
                    () -> assertThat(response.getScopes()).containsExactlyInAnyOrder("openid", "profile", "email", "read", "write"),
                    () -> assertThat(response.getAccessTokenValiditySeconds()).isEqualTo(7200),
                    () -> assertThat(response.getRefreshTokenValiditySeconds()).isEqualTo(7776000),
                    () -> assertThat(response.getAutoApprove()).isTrue(),
                    () -> assertThat(response.getClientType()).isEqualTo("confidential"),
                    () -> assertThat(response.getApplicationType()).isEqualTo("web"),
                    () -> assertThat(response.getEnabled()).isTrue(),
                    () -> assertThat(response.getCreatedAt()).isEqualTo(now),
                    () -> assertThat(response.getLastUsedAt()).isEqualTo(lastUsed)
            );
        }

        @Test
        @DisplayName("Should create OAuth2ClientResponse with minimal fields")
        void shouldCreateOAuth2ClientResponseWithMinimalFields() {
            // When
            OAuth2ClientResponse response = OAuth2ClientResponse.builder()
                    .clientId("minimal_client_456")
                    .clientName("Minimal Client")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getClientId()).isEqualTo("minimal_client_456"),
                    () -> assertThat(response.getClientName()).isEqualTo("Minimal Client"),
                    () -> assertThat(response.getClientSecret()).isNull(),
                    () -> assertThat(response.getDescription()).isNull(),
                    () -> assertThat(response.getRedirectUris()).isNull(),
                    () -> assertThat(response.getGrantTypes()).isNull(),
                    () -> assertThat(response.getScopes()).isNull(),
                    () -> assertThat(response.getAccessTokenValiditySeconds()).isNull(),
                    () -> assertThat(response.getRefreshTokenValiditySeconds()).isNull(),
                    () -> assertThat(response.getAutoApprove()).isNull(),
                    () -> assertThat(response.getClientType()).isNull(),
                    () -> assertThat(response.getApplicationType()).isNull(),
                    () -> assertThat(response.getEnabled()).isNull(),
                    () -> assertThat(response.getCreatedAt()).isNull(),
                    () -> assertThat(response.getLastUsedAt()).isNull()
            );
        }
    }

    @Nested
    @DisplayName("Client Response Scenarios")
    class ClientResponseScenarioTests {

        @Test
        @DisplayName("Should create web client response")
        void shouldCreateWebClientResponse() {
            // Given
            Instant createdAt = Instant.parse("2024-01-15T10:30:00Z");
            
            // When
            OAuth2ClientResponse response = OAuth2ClientResponse.builder()
                    .clientId("web_client_789")
                    .clientName("FocusHive Web Portal")
                    .description("Main web application for FocusHive platform")
                    .redirectUris(Set.of("https://web.focushive.com/oauth/callback"))
                    .grantTypes(Set.of("authorization_code", "refresh_token"))
                    .scopes(Set.of("openid", "profile", "email", "read", "write"))
                    .accessTokenValiditySeconds(3600)
                    .refreshTokenValiditySeconds(2592000)
                    .autoApprove(false)
                    .clientType("confidential")
                    .applicationType("web")
                    .enabled(true)
                    .createdAt(createdAt)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getClientId()).isEqualTo("web_client_789"),
                    () -> assertThat(response.getClientType()).isEqualTo("confidential"),
                    () -> assertThat(response.getApplicationType()).isEqualTo("web"),
                    () -> assertThat(response.getRedirectUris()).containsExactly("https://web.focushive.com/oauth/callback"),
                    () -> assertThat(response.getGrantTypes()).contains("authorization_code", "refresh_token"),
                    () -> assertThat(response.getScopes()).contains("openid", "profile"),
                    () -> assertThat(response.getEnabled()).isTrue(),
                    () -> assertThat(response.getCreatedAt()).isEqualTo(createdAt)
            );
        }

        @Test
        @DisplayName("Should create mobile client response")
        void shouldCreateMobileClientResponse() {
            // Given
            Instant createdAt = Instant.parse("2024-02-01T14:22:30Z");
            Instant lastUsedAt = Instant.parse("2024-02-15T09:45:12Z");
            
            // When
            OAuth2ClientResponse response = OAuth2ClientResponse.builder()
                    .clientId("mobile_client_456")
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
                    .enabled(true)
                    .createdAt(createdAt)
                    .lastUsedAt(lastUsedAt)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getClientId()).isEqualTo("mobile_client_456"),
                    () -> assertThat(response.getClientType()).isEqualTo("public"),
                    () -> assertThat(response.getApplicationType()).isEqualTo("native"),
                    () -> assertThat(response.getRedirectUris()).containsExactlyInAnyOrder(
                            "com.focushive.mobile://oauth/callback", "focushive://auth"
                    ),
                    () -> assertThat(response.getScopes()).contains("offline_access"),
                    () -> assertThat(response.getAccessTokenValiditySeconds()).isEqualTo(1800),
                    () -> assertThat(response.getLastUsedAt()).isEqualTo(lastUsedAt)
            );
        }

        @Test
        @DisplayName("Should create service client response without client secret")
        void shouldCreateServiceClientResponseWithoutClientSecret() {
            // Given
            Instant createdAt = Instant.parse("2024-01-10T08:15:00Z");
            
            // When
            OAuth2ClientResponse response = OAuth2ClientResponse.builder()
                    .clientId("service_client_321")
                    .clientName("Analytics Service")
                    .description("Internal service for analytics processing")
                    .redirectUris(Set.of("https://api.focushive.com/oauth/callback"))
                    .grantTypes(Set.of("client_credentials"))
                    .scopes(Set.of("api:read", "api:write", "analytics:process"))
                    .accessTokenValiditySeconds(7200)
                    .refreshTokenValiditySeconds(0)
                    .autoApprove(true)
                    .clientType("confidential")
                    .applicationType("web")
                    .enabled(true)
                    .createdAt(createdAt)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getClientId()).isEqualTo("service_client_321"),
                    () -> assertThat(response.getClientSecret()).isNull(), // Not returned for existing clients
                    () -> assertThat(response.getGrantTypes()).containsExactly("client_credentials"),
                    () -> assertThat(response.getScopes()).containsExactlyInAnyOrder("api:read", "api:write", "analytics:process"),
                    () -> assertThat(response.getAutoApprove()).isTrue(),
                    () -> assertThat(response.getRefreshTokenValiditySeconds()).isEqualTo(0)
            );
        }

        @Test
        @DisplayName("Should create disabled client response")
        void shouldCreateDisabledClientResponse() {
            // Given
            Instant createdAt = Instant.parse("2024-01-05T12:00:00Z");
            Instant lastUsedAt = Instant.parse("2024-01-20T16:30:45Z");
            
            // When
            OAuth2ClientResponse response = OAuth2ClientResponse.builder()
                    .clientId("disabled_client_999")
                    .clientName("Disabled Test Client")
                    .description("Client that has been disabled")
                    .redirectUris(Set.of("https://disabled.example.com/callback"))
                    .grantTypes(Set.of("authorization_code"))
                    .scopes(Set.of("openid"))
                    .accessTokenValiditySeconds(3600)
                    .refreshTokenValiditySeconds(2592000)
                    .autoApprove(false)
                    .clientType("confidential")
                    .applicationType("web")
                    .enabled(false)
                    .createdAt(createdAt)
                    .lastUsedAt(lastUsedAt)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getClientId()).isEqualTo("disabled_client_999"),
                    () -> assertThat(response.getEnabled()).isFalse(),
                    () -> assertThat(response.getClientName()).isEqualTo("Disabled Test Client"),
                    () -> assertThat(response.getLastUsedAt()).isEqualTo(lastUsedAt)
            );
        }
    }

    @Nested
    @DisplayName("JSON Serialization Tests")
    class JsonSerializationTests {

        @Test
        @DisplayName("Should serialize OAuth2ClientResponse to JSON with snake_case properties")
        void shouldSerializeOAuth2ClientResponseToJsonWithSnakeCaseProperties() throws Exception {
            // Given
            Instant createdAt = Instant.parse("2024-01-15T10:30:00Z");
            Instant lastUsedAt = Instant.parse("2024-01-16T14:45:30Z");
            
            OAuth2ClientResponse response = OAuth2ClientResponse.builder()
                    .clientId("test_client_123")
                    .clientSecret("test_secret_456")
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
                    .enabled(true)
                    .createdAt(createdAt)
                    .lastUsedAt(lastUsedAt)
                    .build();

            // When
            String json = objectMapper.writeValueAsString(response);

            // Then
            assertAll(
                    () -> assertThat(json).contains("\"client_id\":\"test_client_123\""),
                    () -> assertThat(json).contains("\"client_secret\":\"test_secret_456\""),
                    () -> assertThat(json).contains("\"client_name\":\"Test Client\""),
                    () -> assertThat(json).contains("\"description\":\"Test client description\""),
                    () -> assertThat(json).contains("\"redirect_uris\""),
                    () -> assertThat(json).contains("\"grant_types\""),
                    () -> assertThat(json).contains("\"scopes\""),
                    () -> assertThat(json).contains("\"access_token_validity_seconds\":3600"),
                    () -> assertThat(json).contains("\"refresh_token_validity_seconds\":2592000"),
                    () -> assertThat(json).contains("\"auto_approve\":false"),
                    () -> assertThat(json).contains("\"client_type\":\"confidential\""),
                    () -> assertThat(json).contains("\"application_type\":\"web\""),
                    () -> assertThat(json).contains("\"enabled\":true"),
                    () -> assertThat(json).contains("\"created_at\":\"2024-01-15T10:30:00Z\""),
                    () -> assertThat(json).contains("\"last_used_at\":\"2024-01-16T14:45:30Z\"")
            );
        }

        @Test
        @DisplayName("Should deserialize JSON to OAuth2ClientResponse correctly")
        void shouldDeserializeJsonToOAuth2ClientResponseCorrectly() throws Exception {
            // Given
            String json = """
                {
                    "client_id": "mobile_client_789",
                    "client_name": "Mobile App Client",
                    "description": "Native mobile application",
                    "redirect_uris": ["com.example.app://callback", "example://oauth"],
                    "grant_types": ["authorization_code", "refresh_token"],
                    "scopes": ["openid", "profile", "offline_access"],
                    "access_token_validity_seconds": 1800,
                    "refresh_token_validity_seconds": 7776000,
                    "auto_approve": true,
                    "client_type": "public",
                    "application_type": "native",
                    "enabled": true,
                    "created_at": "2024-02-01T14:22:30Z",
                    "last_used_at": "2024-02-15T09:45:12Z"
                }
                """;

            // When
            OAuth2ClientResponse response = objectMapper.readValue(json, OAuth2ClientResponse.class);

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getClientId()).isEqualTo("mobile_client_789"),
                    () -> assertThat(response.getClientName()).isEqualTo("Mobile App Client"),
                    () -> assertThat(response.getDescription()).isEqualTo("Native mobile application"),
                    () -> assertThat(response.getRedirectUris()).containsExactlyInAnyOrder(
                            "com.example.app://callback", "example://oauth"
                    ),
                    () -> assertThat(response.getGrantTypes()).containsExactlyInAnyOrder("authorization_code", "refresh_token"),
                    () -> assertThat(response.getScopes()).containsExactlyInAnyOrder("openid", "profile", "offline_access"),
                    () -> assertThat(response.getAccessTokenValiditySeconds()).isEqualTo(1800),
                    () -> assertThat(response.getRefreshTokenValiditySeconds()).isEqualTo(7776000),
                    () -> assertThat(response.getAutoApprove()).isTrue(),
                    () -> assertThat(response.getClientType()).isEqualTo("public"),
                    () -> assertThat(response.getApplicationType()).isEqualTo("native"),
                    () -> assertThat(response.getEnabled()).isTrue(),
                    () -> assertThat(response.getCreatedAt()).isEqualTo(Instant.parse("2024-02-01T14:22:30Z")),
                    () -> assertThat(response.getLastUsedAt()).isEqualTo(Instant.parse("2024-02-15T09:45:12Z"))
            );
        }

        @Test
        @DisplayName("Should handle null fields in JSON serialization (JsonInclude.NON_NULL)")
        void shouldHandleNullFieldsInJsonSerialization() throws Exception {
            // Given
            OAuth2ClientResponse response = OAuth2ClientResponse.builder()
                    .clientId("minimal_client")
                    .clientName("Minimal Client")
                    .enabled(true)
                    .build();

            // When
            String json = objectMapper.writeValueAsString(response);

            // Then
            assertAll(
                    () -> assertThat(json).contains("\"client_id\":\"minimal_client\""),
                    () -> assertThat(json).contains("\"client_name\":\"Minimal Client\""),
                    () -> assertThat(json).contains("\"enabled\":true"),
                    () -> assertThat(json).doesNotContain("\"client_secret\""),
                    () -> assertThat(json).doesNotContain("\"description\""),
                    () -> assertThat(json).doesNotContain("\"redirect_uris\""),
                    () -> assertThat(json).doesNotContain("\"created_at\""),
                    () -> assertThat(json).doesNotContain("\"last_used_at\"")
            );
        }

        @Test
        @DisplayName("Should handle instant serialization and deserialization")
        void shouldHandleInstantSerializationAndDeserialization() throws Exception {
            // Given
            Instant now = Instant.parse("2024-03-10T12:34:56.789Z");
            
            OAuth2ClientResponse response = OAuth2ClientResponse.builder()
                    .clientId("time_client")
                    .clientName("Time Test Client")
                    .createdAt(now)
                    .lastUsedAt(now.minusSeconds(3600))
                    .build();

            // When
            String json = objectMapper.writeValueAsString(response);
            OAuth2ClientResponse deserialized = objectMapper.readValue(json, OAuth2ClientResponse.class);

            // Then
            assertAll(
                    () -> assertThat(deserialized.getCreatedAt()).isEqualTo(now),
                    () -> assertThat(deserialized.getLastUsedAt()).isEqualTo(now.minusSeconds(3600))
            );
        }

        @Test
        @DisplayName("Should handle complex redirect URIs and scopes in serialization")
        void shouldHandleComplexRedirectUrisAndScopesInSerialization() throws Exception {
            // Given
            Set<String> complexUris = Set.of(
                    "https://app.example.com/oauth/callback?env=prod",
                    "com.example.mobile://auth/callback",
                    "http://localhost:3000/auth/callback",
                    "urn:ietf:wg:oauth:2.0:oob"
            );
            
            Set<String> complexScopes = Set.of(
                    "openid", "profile", "email",
                    "user:read", "user:write", "admin:all",
                    "api:v1", "api:v2"
            );
            
            OAuth2ClientResponse response = OAuth2ClientResponse.builder()
                    .clientId("complex_client")
                    .clientName("Complex Client")
                    .redirectUris(complexUris)
                    .scopes(complexScopes)
                    .build();

            // When
            String json = objectMapper.writeValueAsString(response);
            OAuth2ClientResponse deserialized = objectMapper.readValue(json, OAuth2ClientResponse.class);

            // Then
            assertAll(
                    () -> assertThat(deserialized.getRedirectUris()).containsExactlyInAnyOrderElementsOf(complexUris),
                    () -> assertThat(deserialized.getScopes()).containsExactlyInAnyOrderElementsOf(complexScopes)
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
            Instant createdAt = Instant.parse("2024-01-15T10:30:00Z");
            
            OAuth2ClientResponse response1 = OAuth2ClientResponse.builder()
                    .clientId("same_client")
                    .clientName("Same Name")
                    .description("Same description")
                    .redirectUris(Set.of("https://same.uri.com"))
                    .grantTypes(Set.of("authorization_code"))
                    .scopes(Set.of("openid", "profile"))
                    .enabled(true)
                    .createdAt(createdAt)
                    .build();
                    
            OAuth2ClientResponse response2 = OAuth2ClientResponse.builder()
                    .clientId("same_client")
                    .clientName("Same Name")
                    .description("Same description")
                    .redirectUris(Set.of("https://same.uri.com"))
                    .grantTypes(Set.of("authorization_code"))
                    .scopes(Set.of("openid", "profile"))
                    .enabled(true)
                    .createdAt(createdAt)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response1).isEqualTo(response2),
                    () -> assertThat(response1.hashCode()).isEqualTo(response2.hashCode())
            );
        }

        @Test
        @DisplayName("Should not be equal when client IDs differ")
        void shouldNotBeEqualWhenClientIdsDiffer() {
            // Given
            OAuth2ClientResponse response1 = OAuth2ClientResponse.builder()
                    .clientId("client_1")
                    .clientName("Same Name")
                    .enabled(true)
                    .build();
                    
            OAuth2ClientResponse response2 = OAuth2ClientResponse.builder()
                    .clientId("client_2")
                    .clientName("Same Name")
                    .enabled(true)
                    .build();

            // Then
            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("Should not be equal when enabled status differs")
        void shouldNotBeEqualWhenEnabledStatusDiffers() {
            // Given
            OAuth2ClientResponse response1 = OAuth2ClientResponse.builder()
                    .clientId("same_client")
                    .clientName("Same Name")
                    .enabled(true)
                    .build();
                    
            OAuth2ClientResponse response2 = OAuth2ClientResponse.builder()
                    .clientId("same_client")
                    .clientName("Same Name")
                    .enabled(false)
                    .build();

            // Then
            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("Should handle null values in equals comparison")
        void shouldHandleNullValuesInEqualsComparison() {
            // Given
            OAuth2ClientResponse response1 = OAuth2ClientResponse.builder()
                    .clientId("same_client")
                    .clientName("Same Name")
                    .description(null)
                    .clientSecret(null)
                    .lastUsedAt(null)
                    .build();
                    
            OAuth2ClientResponse response2 = OAuth2ClientResponse.builder()
                    .clientId("same_client")
                    .clientName("Same Name")
                    .description(null)
                    .clientSecret(null)
                    .lastUsedAt(null)
                    .build();

            // Then
            assertThat(response1).isEqualTo(response2);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Business Logic Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long client identifiers and names")
        void shouldHandleVeryLongClientIdentifiersAndNames() {
            // Given
            String longClientId = "client_" + "a".repeat(1000);
            String longClientName = "Client " + "B".repeat(2000);
            
            // When
            OAuth2ClientResponse response = OAuth2ClientResponse.builder()
                    .clientId(longClientId)
                    .clientName(longClientName)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getClientId()).hasSize(1007), // "client_" + 1000 chars
                    () -> assertThat(response.getClientName()).hasSize(2007) // "Client " + 2000 chars
            );
        }

        @Test
        @DisplayName("Should handle boundary values for token validity")
        void shouldHandleBoundaryValuesForTokenValidity() {
            // When
            OAuth2ClientResponse response = OAuth2ClientResponse.builder()
                    .clientId("boundary_client")
                    .accessTokenValiditySeconds(1) // Very short
                    .refreshTokenValiditySeconds(Integer.MAX_VALUE) // Very long
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getAccessTokenValiditySeconds()).isEqualTo(1),
                    () -> assertThat(response.getRefreshTokenValiditySeconds()).isEqualTo(Integer.MAX_VALUE)
            );
        }

        @Test
        @DisplayName("Should handle zero and negative token validity values")
        void shouldHandleZeroAndNegativeTokenValidityValues() {
            // When
            OAuth2ClientResponse response = OAuth2ClientResponse.builder()
                    .clientId("zero_validity_client")
                    .accessTokenValiditySeconds(0)
                    .refreshTokenValiditySeconds(-1)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getAccessTokenValiditySeconds()).isEqualTo(0),
                    () -> assertThat(response.getRefreshTokenValiditySeconds()).isEqualTo(-1)
            );
        }

        @Test
        @DisplayName("Should handle extensive redirect URIs with various schemes")
        void shouldHandleExtensiveRedirectUrisWithVariousSchemes() {
            // Given
            Set<String> diverseUris = Set.of(
                    "https://prod.focushive.com/oauth/callback",
                    "https://staging.focushive.com/oauth/callback",
                    "http://localhost:8080/auth/callback",
                    "focushive://oauth/callback",
                    "com.focushive.mobile://auth/callback",
                    "com.focushive.desktop://callback",
                    "urn:ietf:wg:oauth:2.0:oob",
                    "https://api.focushive.com/oauth/callback?version=v2",
                    "postman://app"
            );

            // When
            OAuth2ClientResponse response = OAuth2ClientResponse.builder()
                    .clientId("diverse_uris_client")
                    .redirectUris(diverseUris)
                    .build();

            // Then
            assertThat(response.getRedirectUris()).containsExactlyInAnyOrderElementsOf(diverseUris);
        }

        @Test
        @DisplayName("Should handle all standard OAuth2 grant types")
        void shouldHandleAllStandardOAuth2GrantTypes() {
            // Given
            Set<String> allGrantTypes = Set.of(
                    "authorization_code",
                    "refresh_token",
                    "client_credentials",
                    "password",
                    "implicit",
                    "urn:ietf:params:oauth:grant-type:device_code"
            );

            // When
            OAuth2ClientResponse response = OAuth2ClientResponse.builder()
                    .clientId("all_grants_client")
                    .grantTypes(allGrantTypes)
                    .build();

            // Then
            assertThat(response.getGrantTypes()).containsExactlyInAnyOrderElementsOf(allGrantTypes);
        }

        @Test
        @DisplayName("Should handle complex scope combinations")
        void shouldHandleComplexScopeCombinations() {
            // Given
            Set<String> complexScopes = Set.of(
                    // OpenID Connect scopes
                    "openid", "profile", "email", "address", "phone",
                    // Custom API scopes
                    "api:read", "api:write", "api:admin", "api:delete",
                    // Resource-specific scopes
                    "user:profile", "user:settings", "user:data", "user:delete",
                    "admin:users", "admin:clients", "admin:audit",
                    // Versioned API scopes
                    "api:v1", "api:v2", "api:beta",
                    // Special scopes
                    "offline_access", "device_code"
            );

            // When
            OAuth2ClientResponse response = OAuth2ClientResponse.builder()
                    .clientId("complex_scopes_client")
                    .scopes(complexScopes)
                    .build();

            // Then
            assertThat(response.getScopes()).containsExactlyInAnyOrderElementsOf(complexScopes);
        }

        @Test
        @DisplayName("Should handle special characters in client names and descriptions")
        void shouldHandleSpecialCharactersInClientNamesAndDescriptions() {
            // Given
            String specialCharsName = "Client with Special !@#$%^&*()_+-={}[]|\\:;\"'<>,.?/~ Characters";
            String unicodeName = "Клиент с русскими символами 中文字符 🚀 📱 💻";
            String htmlContent = "<script>alert('xss')</script> & encoded entities &amp; &lt;";
            
            // When
            OAuth2ClientResponse response = OAuth2ClientResponse.builder()
                    .clientId("special_chars_client")
                    .clientName(specialCharsName)
                    .description(unicodeName + " - " + htmlContent)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getClientName()).isEqualTo(specialCharsName),
                    () -> assertThat(response.getDescription()).isEqualTo(unicodeName + " - " + htmlContent)
            );
        }

        @Test
        @DisplayName("Should handle precise instant values")
        void shouldHandlePreciseInstantValues() {
            // Given
            Instant preciseCreatedAt = Instant.parse("2024-03-15T14:30:45.123456789Z");
            Instant preciseLastUsedAt = Instant.parse("2024-03-15T16:22:33.987654321Z");
            
            // When
            OAuth2ClientResponse response = OAuth2ClientResponse.builder()
                    .clientId("precise_time_client")
                    .createdAt(preciseCreatedAt)
                    .lastUsedAt(preciseLastUsedAt)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getCreatedAt()).isEqualTo(preciseCreatedAt),
                    () -> assertThat(response.getLastUsedAt()).isEqualTo(preciseLastUsedAt)
            );
        }

        @Test
        @DisplayName("Should handle client secret presence and absence")
        void shouldHandleClientSecretPresenceAndAbsence() {
            // Given
            OAuth2ClientResponse withSecret = OAuth2ClientResponse.builder()
                    .clientId("client_with_secret")
                    .clientSecret("very_secret_value_123")
                    .build();
                    
            OAuth2ClientResponse withoutSecret = OAuth2ClientResponse.builder()
                    .clientId("client_without_secret")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(withSecret.getClientSecret()).isEqualTo("very_secret_value_123"),
                    () -> assertThat(withoutSecret.getClientSecret()).isNull()
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
            OAuth2ClientResponse response = new OAuth2ClientResponse();

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getClientId()).isNull(),
                    () -> assertThat(response.getClientSecret()).isNull(),
                    () -> assertThat(response.getClientName()).isNull(),
                    () -> assertThat(response.getDescription()).isNull(),
                    () -> assertThat(response.getRedirectUris()).isNull(),
                    () -> assertThat(response.getGrantTypes()).isNull(),
                    () -> assertThat(response.getScopes()).isNull(),
                    () -> assertThat(response.getAccessTokenValiditySeconds()).isNull(),
                    () -> assertThat(response.getRefreshTokenValiditySeconds()).isNull(),
                    () -> assertThat(response.getAutoApprove()).isNull(),
                    () -> assertThat(response.getClientType()).isNull(),
                    () -> assertThat(response.getApplicationType()).isNull(),
                    () -> assertThat(response.getEnabled()).isNull(),
                    () -> assertThat(response.getCreatedAt()).isNull(),
                    () -> assertThat(response.getLastUsedAt()).isNull()
            );
        }

        @Test
        @DisplayName("Should allow modification after creation")
        void shouldAllowModificationAfterCreation() {
            // Given
            OAuth2ClientResponse response = new OAuth2ClientResponse();
            Instant now = Instant.now();

            // When
            response.setClientId("dynamic_client");
            response.setClientName("Dynamic Client");
            response.setDescription("Dynamically configured client");
            response.setRedirectUris(Set.of("https://dynamic.com/callback"));
            response.setGrantTypes(Set.of("authorization_code"));
            response.setScopes(Set.of("openid", "profile"));
            response.setEnabled(true);
            response.setCreatedAt(now);

            // Then
            assertAll(
                    () -> assertThat(response.getClientId()).isEqualTo("dynamic_client"),
                    () -> assertThat(response.getClientName()).isEqualTo("Dynamic Client"),
                    () -> assertThat(response.getDescription()).isEqualTo("Dynamically configured client"),
                    () -> assertThat(response.getRedirectUris()).containsExactly("https://dynamic.com/callback"),
                    () -> assertThat(response.getGrantTypes()).containsExactly("authorization_code"),
                    () -> assertThat(response.getScopes()).containsExactlyInAnyOrder("openid", "profile"),
                    () -> assertThat(response.getEnabled()).isTrue(),
                    () -> assertThat(response.getCreatedAt()).isEqualTo(now)
            );
        }
    }
}