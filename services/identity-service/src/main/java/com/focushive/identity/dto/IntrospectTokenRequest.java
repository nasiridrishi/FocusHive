package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Request for OAuth2 token introspection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntrospectTokenRequest {
    
    @NotBlank(message = "Token is required")
    private String token;
    
    private String tokenTypeHint; // access_token or refresh_token
}