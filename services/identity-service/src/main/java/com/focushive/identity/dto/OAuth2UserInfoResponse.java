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
@Schema(description = "OAuth2 user info response")
public class OAuth2UserInfoResponse {

    @JsonProperty("sub")
    @Schema(description = "Subject (user ID)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String sub;

    @JsonProperty("name")
    @Schema(description = "Full name", example = "John Doe")
    private String name;

    @JsonProperty("given_name")
    @Schema(description = "Given name", example = "John")
    private String givenName;

    @JsonProperty("family_name")
    @Schema(description = "Family name", example = "Doe")
    private String familyName;

    @JsonProperty("preferred_username")
    @Schema(description = "Preferred username", example = "john.doe")
    private String preferredUsername;

    @JsonProperty("email")
    @Schema(description = "Email address", example = "john.doe@example.com")
    private String email;

    @JsonProperty("email_verified")
    @Schema(description = "Email verification status", example = "true")
    private Boolean emailVerified;

    @JsonProperty("picture")
    @Schema(description = "Profile picture URL", example = "https://example.com/avatar.jpg")
    private String picture;

    @JsonProperty("locale")
    @Schema(description = "User locale", example = "en-US")
    private String locale;

    @JsonProperty("zoneinfo")
    @Schema(description = "User timezone", example = "America/New_York")
    private String zoneinfo;

    @JsonProperty("updated_at")
    @Schema(description = "Last update timestamp", example = "1620000000")
    private Long updatedAt;

    @JsonProperty("persona_id")
    @Schema(description = "Active persona ID", example = "persona-123-456")
    private String personaId;

    @JsonProperty("persona_name")
    @Schema(description = "Active persona name", example = "Work Profile")
    private String personaName;
}