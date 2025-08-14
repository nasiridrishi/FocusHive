package com.focushive.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OAuth2 authorization request")
public class OAuth2AuthorizeRequest {

    @NotBlank(message = "Client ID is required")
    @Schema(description = "OAuth2 client identifier", example = "focushive_client_123")
    private String clientId;

    @NotBlank(message = "Response type is required")
    @Schema(description = "OAuth2 response type", example = "code", allowableValues = {"code"})
    private String responseType;

    @NotBlank(message = "Redirect URI is required")
    @Schema(description = "Redirect URI for authorization response", example = "https://app.focushive.com/oauth/callback")
    private String redirectUri;

    @Schema(description = "Space-separated list of requested scopes", example = "openid profile email")
    private String scope;

    @Schema(description = "State parameter for CSRF protection", example = "xyz123")
    private String state;

    @Schema(description = "PKCE code challenge")
    private String codeChallenge;

    @Schema(description = "PKCE code challenge method", allowableValues = {"plain", "S256"})
    private String codeChallengeMethod;
}