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
@Schema(description = "OAuth2 token introspection response")
public class OAuth2IntrospectionResponse {

    @JsonProperty("active")
    @Schema(description = "Whether the token is active", example = "true")
    private Boolean active;
    
    /**
     * Convenience method for checking if token is active
     */
    public boolean isActive() {
        return active != null && active;
    }

    @JsonProperty("scope")
    @Schema(description = "Token scopes", example = "openid profile email")
    private String scope;

    @JsonProperty("client_id")
    @Schema(description = "Client ID that the token was issued to", example = "focushive_client_123")
    private String clientId;

    @JsonProperty("username")
    @Schema(description = "Username of the token owner", example = "john.doe")
    private String username;

    @JsonProperty("token_type")
    @Schema(description = "Type of token", example = "Bearer")
    private String tokenType;

    @JsonProperty("exp")
    @Schema(description = "Expiration timestamp", example = "1620000000")
    private Long exp;

    @JsonProperty("iat")
    @Schema(description = "Issued at timestamp", example = "1620000000")
    private Long iat;

    @JsonProperty("nbf")
    @Schema(description = "Not before timestamp", example = "1620000000")
    private Long nbf;

    @JsonProperty("sub")
    @Schema(description = "Subject (user ID)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String sub;

    @JsonProperty("aud")
    @Schema(description = "Audience", example = "[\"focushive-api\"]")
    private Object aud; // Can be either String or String[] depending on Spring Authorization Server version

    @JsonProperty("iss")
    @Schema(description = "Issuer", example = "https://identity.focushive.com")
    private String iss;

    @JsonProperty("jti")
    @Schema(description = "JWT ID", example = "abc-123-def-456")
    private String jti;
    
    /**
     * Get audience as string for backward compatibility
     */
    public String getAudString() {
        if (aud instanceof String) {
            return (String) aud;
        } else if (aud instanceof String[]) {
            String[] audArray = (String[]) aud;
            return audArray.length > 0 ? audArray[0] : null;
        }
        return null;
    }
}