package com.focushive.identity.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Comprehensive unit tests for OAuth2TokenResponse DTO.
 * Tests OAuth2 token response serialization, JSON property mapping,
 * and OpenID Connect features.
 */
@DisplayName("OAuth2TokenResponse DTO Unit Tests")
class OAuth2TokenResponseUnitTest {

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
        @DisplayName("Should create OAuth2TokenResponse using builder with all fields")
        void shouldCreateOAuth2TokenResponseUsingBuilderWithAllFields() {
            // When
            OAuth2TokenResponse response = OAuth2TokenResponse.builder()
                    .accessToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.access.token")
                    .tokenType("Bearer")
                    .expiresIn(3600)
                    .refreshToken("def456.refresh.token")
                    .scope("openid profile email read write")
                    .idToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.id.token")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getAccessToken()).isEqualTo("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.access.token"),
                    () -> assertThat(response.getTokenType()).isEqualTo("Bearer"),
                    () -> assertThat(response.getExpiresIn()).isEqualTo(3600),
                    () -> assertThat(response.getRefreshToken()).isEqualTo("def456.refresh.token"),
                    () -> assertThat(response.getScope()).isEqualTo("openid profile email read write"),
                    () -> assertThat(response.getIdToken()).isEqualTo("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.id.token")
            );
        }

        @Test
        @DisplayName("Should create OAuth2TokenResponse with minimal required fields")
        void shouldCreateOAuth2TokenResponseWithMinimalRequiredFields() {
            // When
            OAuth2TokenResponse response = OAuth2TokenResponse.builder()
                    .accessToken("minimal.access.token")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getAccessToken()).isEqualTo("minimal.access.token"),
                    () -> assertThat(response.getTokenType()).isEqualTo("Bearer"), // Default value
                    () -> assertThat(response.getExpiresIn()).isNull(),
                    () -> assertThat(response.getRefreshToken()).isNull(),
                    () -> assertThat(response.getScope()).isNull(),
                    () -> assertThat(response.getIdToken()).isNull()
            );
        }

        @Test
        @DisplayName("Should have Bearer as default token type")
        void shouldHaveBearerAsDefaultTokenType() {
            // When
            OAuth2TokenResponse response = OAuth2TokenResponse.builder()
                    .accessToken("test.token")
                    .build();

            // Then
            assertThat(response.getTokenType()).isEqualTo("Bearer");
        }
    }

    @Nested
    @DisplayName("Token Type Scenarios")
    class TokenTypeScenarioTests {

        @Test
        @DisplayName("Should create standard Bearer token response")
        void shouldCreateStandardBearerTokenResponse() {
            // When
            OAuth2TokenResponse response = OAuth2TokenResponse.builder()
                    .accessToken("bearer.access.token")
                    .tokenType("Bearer")
                    .expiresIn(7200)
                    .scope("read write")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getTokenType()).isEqualTo("Bearer"),
                    () -> assertThat(response.getExpiresIn()).isEqualTo(7200),
                    () -> assertThat(response.getScope()).isEqualTo("read write")
            );
        }

        @Test
        @DisplayName("Should create OpenID Connect token response with id_token")
        void shouldCreateOpenIDConnectTokenResponseWithIdToken() {
            // When
            OAuth2TokenResponse response = OAuth2TokenResponse.builder()
                    .accessToken("oidc.access.token")
                    .tokenType("Bearer")
                    .expiresIn(3600)
                    .refreshToken("oidc.refresh.token")
                    .scope("openid profile email")
                    .idToken("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.signature")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getAccessToken()).isEqualTo("oidc.access.token"),
                    () -> assertThat(response.getRefreshToken()).isEqualTo("oidc.refresh.token"),
                    () -> assertThat(response.getScope()).contains("openid"),
                    () -> assertThat(response.getIdToken()).startsWith("eyJhbGciOiJSUzI1NiIs"),
                    () -> assertThat(response.getIdToken()).contains("signature")
            );
        }

        @Test
        @DisplayName("Should create client_credentials grant response without refresh token")
        void shouldCreateClientCredentialsGrantResponseWithoutRefreshToken() {
            // When
            OAuth2TokenResponse response = OAuth2TokenResponse.builder()
                    .accessToken("client.credentials.token")
                    .tokenType("Bearer")
                    .expiresIn(1800)
                    .scope("api:read api:write")
                    // No refresh token for client_credentials
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getAccessToken()).isEqualTo("client.credentials.token"),
                    () -> assertThat(response.getExpiresIn()).isEqualTo(1800),
                    () -> assertThat(response.getScope()).isEqualTo("api:read api:write"),
                    () -> assertThat(response.getRefreshToken()).isNull(),
                    () -> assertThat(response.getIdToken()).isNull()
            );
        }
    }

    @Nested
    @DisplayName("JSON Serialization Tests")
    class JsonSerializationTests {

        @Test
        @DisplayName("Should serialize OAuth2TokenResponse to JSON with snake_case properties")
        void shouldSerializeOAuth2TokenResponseToJsonWithSnakeCaseProperties() throws Exception {
            // Given
            OAuth2TokenResponse response = OAuth2TokenResponse.builder()
                    .accessToken("test.access.token")
                    .tokenType("Bearer")
                    .expiresIn(3600)
                    .refreshToken("test.refresh.token")
                    .scope("openid profile")
                    .idToken("test.id.token")
                    .build();

            // When
            String json = objectMapper.writeValueAsString(response);

            // Then
            assertAll(
                    () -> assertThat(json).contains("\"access_token\":\"test.access.token\""),
                    () -> assertThat(json).contains("\"token_type\":\"Bearer\""),
                    () -> assertThat(json).contains("\"expires_in\":3600"),
                    () -> assertThat(json).contains("\"refresh_token\":\"test.refresh.token\""),
                    () -> assertThat(json).contains("\"scope\":\"openid profile\""),
                    () -> assertThat(json).contains("\"id_token\":\"test.id.token\"")
            );
        }

        @Test
        @DisplayName("Should deserialize JSON with snake_case properties to OAuth2TokenResponse")
        void shouldDeserializeJsonWithSnakeCasePropertiesToOAuth2TokenResponse() throws Exception {
            // Given
            String json = """
                {
                    "access_token": "deserialized.access.token",
                    "token_type": "Bearer",
                    "expires_in": 7200,
                    "refresh_token": "deserialized.refresh.token",
                    "scope": "read write admin",
                    "id_token": "deserialized.id.token"
                }
                """;

            // When
            OAuth2TokenResponse response = objectMapper.readValue(json, OAuth2TokenResponse.class);

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getAccessToken()).isEqualTo("deserialized.access.token"),
                    () -> assertThat(response.getTokenType()).isEqualTo("Bearer"),
                    () -> assertThat(response.getExpiresIn()).isEqualTo(7200),
                    () -> assertThat(response.getRefreshToken()).isEqualTo("deserialized.refresh.token"),
                    () -> assertThat(response.getScope()).isEqualTo("read write admin"),
                    () -> assertThat(response.getIdToken()).isEqualTo("deserialized.id.token")
            );
        }

        @Test
        @DisplayName("Should exclude null fields from JSON serialization")
        void shouldExcludeNullFieldsFromJsonSerialization() throws Exception {
            // Given
            OAuth2TokenResponse response = OAuth2TokenResponse.builder()
                    .accessToken("only.access.token")
                    .expiresIn(3600)
                    // refreshToken, scope, idToken are null
                    .build();

            // When
            String json = objectMapper.writeValueAsString(response);

            // Then
            assertAll(
                    () -> assertThat(json).contains("\"access_token\":\"only.access.token\""),
                    () -> assertThat(json).contains("\"token_type\":\"Bearer\""), // Default value
                    () -> assertThat(json).contains("\"expires_in\":3600"),
                    () -> assertThat(json).doesNotContain("refresh_token"),
                    () -> assertThat(json).doesNotContain("scope"),
                    () -> assertThat(json).doesNotContain("id_token")
            );
        }

        @Test
        @DisplayName("Should handle JSON with missing optional fields")
        void shouldHandleJsonWithMissingOptionalFields() throws Exception {
            // Given
            String minimalJson = """
                {
                    "access_token": "minimal.token"
                }
                """;

            // When
            OAuth2TokenResponse response = objectMapper.readValue(minimalJson, OAuth2TokenResponse.class);

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getAccessToken()).isEqualTo("minimal.token"),
                    () -> assertThat(response.getTokenType()).isEqualTo("Bearer"), // Default value from @Builder.Default
                    () -> assertThat(response.getExpiresIn()).isNull(),
                    () -> assertThat(response.getRefreshToken()).isNull(),
                    () -> assertThat(response.getScope()).isNull(),
                    () -> assertThat(response.getIdToken()).isNull()
            );
        }

        @Test
        @DisplayName("Should handle very long JWT tokens in serialization")
        void shouldHandleVeryLongJwtTokensInSerialization() throws Exception {
            // Given
            String longAccessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                    "a".repeat(1000) + "." +
                    "b".repeat(500);
            String longIdToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9." +
                    "c".repeat(800) + "." +
                    "d".repeat(300);
                    
            OAuth2TokenResponse response = OAuth2TokenResponse.builder()
                    .accessToken(longAccessToken)
                    .idToken(longIdToken)
                    .build();

            // When
            String json = objectMapper.writeValueAsString(response);
            OAuth2TokenResponse deserialized = objectMapper.readValue(json, OAuth2TokenResponse.class);

            // Then
            assertAll(
                    () -> assertThat(deserialized.getAccessToken()).hasSize(longAccessToken.length()),
                    () -> assertThat(deserialized.getIdToken()).hasSize(longIdToken.length()),
                    () -> assertThat(deserialized.getAccessToken()).isEqualTo(longAccessToken),
                    () -> assertThat(deserialized.getIdToken()).isEqualTo(longIdToken)
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
            OAuth2TokenResponse response1 = OAuth2TokenResponse.builder()
                    .accessToken("same.token")
                    .tokenType("Bearer")
                    .expiresIn(3600)
                    .refreshToken("same.refresh")
                    .scope("same scope")
                    .build();
                    
            OAuth2TokenResponse response2 = OAuth2TokenResponse.builder()
                    .accessToken("same.token")
                    .tokenType("Bearer")
                    .expiresIn(3600)
                    .refreshToken("same.refresh")
                    .scope("same scope")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response1).isEqualTo(response2),
                    () -> assertThat(response1.hashCode()).isEqualTo(response2.hashCode())
            );
        }

        @Test
        @DisplayName("Should not be equal when access tokens differ")
        void shouldNotBeEqualWhenAccessTokensDiffer() {
            // Given
            OAuth2TokenResponse response1 = OAuth2TokenResponse.builder()
                    .accessToken("token.one")
                    .tokenType("Bearer")
                    .build();
                    
            OAuth2TokenResponse response2 = OAuth2TokenResponse.builder()
                    .accessToken("token.two")
                    .tokenType("Bearer")
                    .build();

            // Then
            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("Should not be equal when expiration times differ")
        void shouldNotBeEqualWhenExpirationTimesDiffer() {
            // Given
            OAuth2TokenResponse response1 = OAuth2TokenResponse.builder()
                    .accessToken("same.token")
                    .expiresIn(3600)
                    .build();
                    
            OAuth2TokenResponse response2 = OAuth2TokenResponse.builder()
                    .accessToken("same.token")
                    .expiresIn(7200)
                    .build();

            // Then
            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("Should handle null values in equals comparison")
        void shouldHandleNullValuesInEqualsComparison() {
            // Given
            OAuth2TokenResponse response1 = OAuth2TokenResponse.builder()
                    .accessToken("token")
                    .refreshToken(null)
                    .idToken(null)
                    .build();
                    
            OAuth2TokenResponse response2 = OAuth2TokenResponse.builder()
                    .accessToken("token")
                    .refreshToken(null)
                    .idToken(null)
                    .build();

            // Then
            assertThat(response1).isEqualTo(response2);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Conditions")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle zero and negative expires_in values")
        void shouldHandleZeroAndNegativeExpiresInValues() {
            // When/Then
            OAuth2TokenResponse zeroExpiry = OAuth2TokenResponse.builder()
                    .accessToken("token")
                    .expiresIn(0)
                    .build();
            assertThat(zeroExpiry.getExpiresIn()).isZero();
            
            OAuth2TokenResponse negativeExpiry = OAuth2TokenResponse.builder()
                    .accessToken("token")
                    .expiresIn(-100)
                    .build();
            assertThat(negativeExpiry.getExpiresIn()).isEqualTo(-100);
        }

        @Test
        @DisplayName("Should handle empty strings in all fields")
        void shouldHandleEmptyStringsInAllFields() {
            // When
            OAuth2TokenResponse response = OAuth2TokenResponse.builder()
                    .accessToken("")
                    .tokenType("")
                    .refreshToken("")
                    .scope("")
                    .idToken("")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getAccessToken()).isEmpty(),
                    () -> assertThat(response.getTokenType()).isEmpty(),
                    () -> assertThat(response.getRefreshToken()).isEmpty(),
                    () -> assertThat(response.getScope()).isEmpty(),
                    () -> assertThat(response.getIdToken()).isEmpty()
            );
        }

        @Test
        @DisplayName("Should handle complex scopes with special characters")
        void shouldHandleComplexScopesWithSpecialCharacters() {
            // Given
            String complexScope = "openid profile:read profile:write email user:admin " +
                    "api:v1 api:v2 custom:scope-with-dashes custom:scope_with_underscores";

            // When
            OAuth2TokenResponse response = OAuth2TokenResponse.builder()
                    .accessToken("token")
                    .scope(complexScope)
                    .build();

            // Then
            assertThat(response.getScope()).isEqualTo(complexScope);
        }

        @Test
        @DisplayName("Should handle various token types")
        void shouldHandleVariousTokenTypes() {
            // Given/When/Then
            String[] tokenTypes = {"Bearer", "bearer", "MAC", "Basic", "Custom"};
            
            for (String tokenType : tokenTypes) {
                OAuth2TokenResponse response = OAuth2TokenResponse.builder()
                        .accessToken("token")
                        .tokenType(tokenType)
                        .build();
                        
                assertThat(response.getTokenType()).isEqualTo(tokenType);
            }
        }

        @Test
        @DisplayName("Should handle malformed JWT tokens")
        void shouldHandleMalformedJwtTokens() {
            // Given
            String[] malformedTokens = {
                    "not.a.jwt",
                    "single.part",
                    "too.many.parts.here",
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9", // Missing parts
                    "invalid-base64!@#$%",
                    ""
            };

            // When/Then - Should accept any string value
            for (String malformedToken : malformedTokens) {
                OAuth2TokenResponse response = OAuth2TokenResponse.builder()
                        .accessToken(malformedToken)
                        .idToken(malformedToken)
                        .build();
                        
                assertAll(
                        () -> assertThat(response.getAccessToken()).isEqualTo(malformedToken),
                        () -> assertThat(response.getIdToken()).isEqualTo(malformedToken)
                );
            }
        }

        @Test
        @DisplayName("Should handle very large expiration values")
        void shouldHandleVeryLargeExpirationValues() {
            // Given
            Integer maxIntValue = Integer.MAX_VALUE;
            Integer largeValue = 999999999;

            // When
            OAuth2TokenResponse maxResponse = OAuth2TokenResponse.builder()
                    .accessToken("token")
                    .expiresIn(maxIntValue)
                    .build();
                    
            OAuth2TokenResponse largeResponse = OAuth2TokenResponse.builder()
                    .accessToken("token")
                    .expiresIn(largeValue)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(maxResponse.getExpiresIn()).isEqualTo(maxIntValue),
                    () -> assertThat(largeResponse.getExpiresIn()).isEqualTo(largeValue)
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
            OAuth2TokenResponse response = new OAuth2TokenResponse();

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getAccessToken()).isNull(),
                    () -> assertThat(response.getTokenType()).isEqualTo("Bearer"), // Default value from @Builder.Default
                    () -> assertThat(response.getExpiresIn()).isNull(),
                    () -> assertThat(response.getRefreshToken()).isNull(),
                    () -> assertThat(response.getScope()).isNull(),
                    () -> assertThat(response.getIdToken()).isNull()
            );
        }

        @Test
        @DisplayName("Should allow modification after creation")
        void shouldAllowModificationAfterCreation() {
            // Given
            OAuth2TokenResponse response = new OAuth2TokenResponse();

            // When
            response.setAccessToken("modified.token");
            response.setTokenType("Modified");
            response.setExpiresIn(1800);
            response.setScope("modified scope");

            // Then
            assertAll(
                    () -> assertThat(response.getAccessToken()).isEqualTo("modified.token"),
                    () -> assertThat(response.getTokenType()).isEqualTo("Modified"),
                    () -> assertThat(response.getExpiresIn()).isEqualTo(1800),
                    () -> assertThat(response.getScope()).isEqualTo("modified scope")
            );
        }
    }

    @Nested
    @DisplayName("OAuth2 Specification Compliance Tests")
    class OAuth2ComplianceTests {

        @Test
        @DisplayName("Should create RFC 6749 compliant token response")
        void shouldCreateRfc6749CompliantTokenResponse() {
            // Given - Based on OAuth 2.0 RFC 6749 Section 5.1
            OAuth2TokenResponse response = OAuth2TokenResponse.builder()
                    .accessToken("2YotnFZFEjr1zCsicMWpAA")
                    .tokenType("Bearer")
                    .expiresIn(3600)
                    .refreshToken("tGzv3JOkF0XG5Qx2TlKWIA")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getAccessToken()).isNotNull(),
                    () -> assertThat(response.getTokenType()).isEqualTo("Bearer"),
                    () -> assertThat(response.getExpiresIn()).isPositive(),
                    () -> assertThat(response.getRefreshToken()).isNotNull()
            );
        }

        @Test
        @DisplayName("Should create OpenID Connect compliant response")
        void shouldCreateOpenIDConnectCompliantResponse() {
            // Given - Based on OpenID Connect Core 1.0 Section 3.1.3.3
            OAuth2TokenResponse response = OAuth2TokenResponse.builder()
                    .accessToken("SlAV32hkKG")
                    .tokenType("Bearer")
                    .expiresIn(3600)
                    .refreshToken("8xLOxBtZp8")
                    .scope("openid profile email")
                    .idToken("eyJhbGciOiJSUzI1NiIsImtpZCI6IjFlOWdkazcifQ.ewogImlzcyI6ICJodHRwOi8vc2VydmVyLmV4YW1wbGUuY29tIiwKICJzdWIiOiAiMjQ4Mjg5NzYxMDAxIiwKICJhdWQiOiAiczZCaGRSa3F0MyIsCiAibm9uY2UiOiAibi0wUzZfV3pBMk1qIiwKICJleHAiOiAxMzExMjgxOTcwLAogImlhdCI6IDEzMTEyODA5NzAKfQ.ggW8hZ1EuVLuxNuuIJKX_V8a_OMXzR0EHR9R6jgdqrOOF4daGU96Sr_P6qJp6IcmD3HP99Obi1PRs-cwh3LO-p146waJ8IhehcwL7F09JdijmBqkvPeB2T9CJNqeGpe-gccMg4vfKjkM8FcGvnzZUN4_KSP0aAp1tOJ1zZwgjxqGByKHiOtX7TpdQyHE5lcMiKPXfEIQILVq0pc_E2DzL7emopWoaoZTF_m0_N0YzFC6g6EJbOEoRoSK5hoDalrcvRYLSrQAZZKflyuVCyixEoV9GfNQC3_osjzw2PAithfubEEBLuVVk4XUVrWOLrLl0nx7RkKU8NXNHq-rvKMzqg")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getScope()).contains("openid"),
                    () -> assertThat(response.getIdToken()).isNotNull(),
                    () -> assertThat(response.getIdToken()).startsWith("eyJ") // JWT format
            );
        }
    }
}