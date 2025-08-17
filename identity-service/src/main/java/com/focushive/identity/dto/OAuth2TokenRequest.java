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
@Schema(description = "OAuth2 token request")
public class OAuth2TokenRequest {

    @NotBlank(message = "Grant type is required")
    @Schema(description = "OAuth2 grant type", example = "authorization_code", 
            allowableValues = {"authorization_code", "refresh_token", "client_credentials"})
    private String grantType;

    @Schema(description = "Authorization code (required for authorization_code grant)", example = "abc123")
    private String code;

    @Schema(description = "Redirect URI (required for authorization_code grant)", example = "https://app.focushive.com/oauth/callback")
    private String redirectUri;

    @Schema(description = "PKCE code verifier")
    private String codeVerifier;

    @Schema(description = "Refresh token (required for refresh_token grant)")
    private String refreshToken;

    @Schema(description = "Requested scope (for client_credentials grant)", example = "read write")
    private String scope;

    @Schema(description = "Client ID (alternative to Authorization header)")
    private String clientId;

    @Schema(description = "Client secret (alternative to Authorization header)")
    private String clientSecret;

    @Schema(description = "Authorization header containing client credentials")
    private String authorizationHeader;
}