package com.focushive.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OAuth2 client registration request")
public class OAuth2ClientRegistrationRequest {

    @NotBlank(message = "Client name is required")
    @Schema(description = "Human-readable client name", example = "FocusHive Mobile App")
    private String clientName;

    @Schema(description = "Client description", example = "Official FocusHive mobile application")
    private String description;

    @NotEmpty(message = "At least one redirect URI is required")
    @Schema(description = "Authorized redirect URIs", example = "[\"https://app.focushive.com/oauth/callback\", \"focushive://oauth/callback\"]")
    private Set<String> redirectUris;

    @Schema(description = "Grant types", example = "[\"authorization_code\", \"refresh_token\"]")
    @Builder.Default
    private Set<String> grantTypes = Set.of("authorization_code");

    @Schema(description = "Scopes", example = "[\"openid\", \"profile\", \"email\"]")
    @Builder.Default
    private Set<String> scopes = Set.of("openid", "profile");

    @Schema(description = "Access token validity in seconds", example = "3600")
    @Builder.Default
    private Integer accessTokenValiditySeconds = 3600;

    @Schema(description = "Refresh token validity in seconds", example = "2592000")
    @Builder.Default
    private Integer refreshTokenValiditySeconds = 2592000;

    @Schema(description = "Whether to auto-approve requests", example = "false")
    @Builder.Default
    private Boolean autoApprove = false;

    @Schema(description = "Client type", example = "confidential", allowableValues = {"public", "confidential"})
    @Builder.Default
    private String clientType = "confidential";

    @Schema(description = "Application type", example = "web", allowableValues = {"web", "native"})
    @Builder.Default
    private String applicationType = "web";
}