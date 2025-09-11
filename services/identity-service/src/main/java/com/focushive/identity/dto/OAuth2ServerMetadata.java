package com.focushive.identity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "OAuth2 Authorization Server Metadata (RFC 8414)")
public class OAuth2ServerMetadata {

    @JsonProperty("issuer")
    @Schema(description = "Authorization server issuer identifier")
    private String issuer;

    @JsonProperty("authorization_endpoint")
    @Schema(description = "Authorization endpoint URL")
    private String authorizationEndpoint;

    @JsonProperty("token_endpoint")
    @Schema(description = "Token endpoint URL")
    private String tokenEndpoint;

    @JsonProperty("device_authorization_endpoint")
    @Schema(description = "Device authorization endpoint URL")
    private String deviceAuthorizationEndpoint;

    @JsonProperty("device_verification_endpoint")
    @Schema(description = "Device verification endpoint URL")
    private String deviceVerificationEndpoint;

    @JsonProperty("jwks_uri")
    @Schema(description = "JWK Set endpoint URL")
    private String jwksUri;

    @JsonProperty("userinfo_endpoint")
    @Schema(description = "UserInfo endpoint URL")
    private String userinfoEndpoint;

    @JsonProperty("registration_endpoint")
    @Schema(description = "Dynamic client registration endpoint URL")
    private String registrationEndpoint;

    @JsonProperty("introspection_endpoint")
    @Schema(description = "Token introspection endpoint URL")
    private String introspectionEndpoint;

    @JsonProperty("revocation_endpoint")
    @Schema(description = "Token revocation endpoint URL")
    private String revocationEndpoint;

    @JsonProperty("scopes_supported")
    @Schema(description = "Supported scopes")
    private List<String> scopesSupported;

    @JsonProperty("response_types_supported")
    @Schema(description = "Supported response types")
    private List<String> responseTypesSupported;

    @JsonProperty("grant_types_supported")
    @Schema(description = "Supported grant types")
    private List<String> grantTypesSupported;

    @JsonProperty("token_endpoint_auth_methods_supported")
    @Schema(description = "Supported client authentication methods")
    private List<String> tokenEndpointAuthMethodsSupported;

    @JsonProperty("revocation_endpoint_auth_methods_supported")
    @Schema(description = "Supported token revocation endpoint authentication methods")
    private List<String> revocationEndpointAuthMethodsSupported;

    @JsonProperty("code_challenge_methods_supported")
    @Schema(description = "Supported PKCE code challenge methods")
    private List<String> codeChallengeMethodsSupported;

    @JsonProperty("id_token_signing_alg_values_supported")
    @Schema(description = "Supported ID token signing algorithms")
    private List<String> idTokenSigningAlgValuesSupported;
}