package com.focushive.identity.dto;

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
public class OAuth2TokenInfo {
    
    private String tokenId;
    private String userId;
    private String clientId;
    private String username;
    private String scope; // As space-separated string
    private Set<String> scopes; // As set
    private String tokenType;
    private Instant iat; // Issued at
    private Instant exp; // Expires at
    private Instant issuedAt;
    private Instant expiresAt;
    private boolean active;
    private String personaId;
    private String sub; // Subject
    private String aud; // Audience
    private String iss; // Issuer
}