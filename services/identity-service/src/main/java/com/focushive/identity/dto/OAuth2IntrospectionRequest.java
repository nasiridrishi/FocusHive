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
@Schema(description = "OAuth2 token introspection request")
public class OAuth2IntrospectionRequest {

    @NotBlank(message = "Token is required")
    @Schema(description = "Token to introspect", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "Token type hint", example = "access_token", allowableValues = {"access_token", "refresh_token"})
    private String tokenTypeHint;

    @Schema(description = "Client ID (alternative to Authorization header)")
    private String clientId;

    @Schema(description = "Client secret (alternative to Authorization header)")
    private String clientSecret;

    @Schema(description = "Authorization header containing client credentials")
    private String authorizationHeader;
}