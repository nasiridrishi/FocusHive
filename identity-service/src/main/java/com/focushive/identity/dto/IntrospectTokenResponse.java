package com.focushive.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for OAuth2 token introspection (RFC 7662).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntrospectTokenResponse {
    
    private boolean active;
    
    private String scope;
    
    @JsonProperty("client_id")
    private String clientId;
    
    private String username;
    
    @JsonProperty("token_type")
    private String tokenType;
    
    private Long exp; // Expiration time (seconds since epoch)
    
    private Long iat; // Issued at time (seconds since epoch)
    
    private Long nbf; // Not before time (seconds since epoch)
    
    private String sub; // Subject (user ID)
    
    private String aud; // Audience
    
    private String iss; // Issuer
    
    private String jti; // JWT ID
    
    // Custom claims
    private String email;
    
    @JsonProperty("email_verified")
    private Boolean emailVerified;
    
    @JsonProperty("persona_id")
    private String personaId;
    
    @JsonProperty("persona_type")
    private String personaType;
}