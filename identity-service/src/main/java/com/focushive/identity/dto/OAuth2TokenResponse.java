package com.focushive.identity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "OAuth2 token response")
public class OAuth2TokenResponse {

    @JsonProperty("access_token")
    @Schema(description = "Access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @JsonProperty("token_type")
    @Schema(description = "Token type", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @JsonProperty("expires_in")
    @Schema(description = "Token expiration time in seconds", example = "3600")
    private Integer expiresIn;

    @JsonProperty("refresh_token")
    @Schema(description = "Refresh token", example = "def456...")
    private String refreshToken;

    @JsonProperty("scope")
    @Schema(description = "Granted scopes", example = "openid profile email")
    private String scope;

    @JsonProperty("id_token")
    @Schema(description = "OpenID Connect ID token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String idToken;
}