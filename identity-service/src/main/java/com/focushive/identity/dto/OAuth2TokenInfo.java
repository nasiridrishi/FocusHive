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
    private Set<String> scopes;
    private String tokenType;
    private Instant issuedAt;
    private Instant expiresAt;
    private boolean active;
    private String personaId;
}