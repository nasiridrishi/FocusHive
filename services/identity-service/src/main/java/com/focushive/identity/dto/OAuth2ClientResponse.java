package com.focushive.identity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "OAuth2 client response")
public class OAuth2ClientResponse {

    @JsonProperty("client_id")
    @Schema(description = "Client identifier", example = "focushive_client_123")
    private String clientId;

    @JsonProperty("client_secret")
    @Schema(description = "Client secret (only returned on creation)", example = "secret_abc123")
    private String clientSecret;

    @JsonProperty("client_name")
    @Schema(description = "Human-readable client name", example = "FocusHive Mobile App")
    private String clientName;

    @JsonProperty("description")
    @Schema(description = "Client description", example = "Official FocusHive mobile application")
    private String description;

    @JsonProperty("redirect_uris")
    @Schema(description = "Authorized redirect URIs")
    private Set<String> redirectUris;

    @JsonProperty("grant_types")
    @Schema(description = "Authorized grant types")
    private Set<String> grantTypes;

    @JsonProperty("scopes")
    @Schema(description = "Authorized scopes")
    private Set<String> scopes;

    @JsonProperty("access_token_validity_seconds")
    @Schema(description = "Access token validity in seconds", example = "3600")
    private Integer accessTokenValiditySeconds;

    @JsonProperty("refresh_token_validity_seconds")
    @Schema(description = "Refresh token validity in seconds", example = "2592000")
    private Integer refreshTokenValiditySeconds;

    @JsonProperty("auto_approve")
    @Schema(description = "Whether requests are auto-approved", example = "false")
    private Boolean autoApprove;

    @JsonProperty("client_type")
    @Schema(description = "Client type", example = "confidential")
    private String clientType;

    @JsonProperty("application_type")
    @Schema(description = "Application type", example = "web")
    private String applicationType;

    @JsonProperty("enabled")
    @Schema(description = "Whether the client is enabled", example = "true")
    private Boolean enabled;

    @JsonProperty("created_at")
    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @JsonProperty("last_used_at")
    @Schema(description = "Last usage timestamp")
    private Instant lastUsedAt;
}