package com.focushive.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OAuth2 client update request")
public class OAuth2ClientUpdateRequest {

    @NotBlank(message = "Client name is required")
    @Schema(description = "Human-readable client name", example = "FocusHive Mobile App v2")
    private String clientName;

    @Schema(description = "Client description", example = "Updated FocusHive mobile application")
    private String description;

    @Schema(description = "Authorized redirect URIs")
    private Set<String> redirectUris;

    @Schema(description = "Grant types")
    private Set<String> grantTypes;

    @Schema(description = "Scopes")
    private Set<String> scopes;

    @Schema(description = "Access token validity in seconds", example = "7200")
    private Integer accessTokenValiditySeconds;

    @Schema(description = "Refresh token validity in seconds", example = "1209600")
    private Integer refreshTokenValiditySeconds;

    @Schema(description = "Whether to auto-approve requests", example = "true")
    private Boolean autoApprove;

    @Schema(description = "Whether the client is enabled", example = "true")
    private Boolean enabled;
}