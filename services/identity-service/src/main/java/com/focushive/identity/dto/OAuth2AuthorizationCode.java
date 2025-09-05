package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2AuthorizationCode {
    
    private String code;
    private String clientId;
    private String userId;
    private String redirectUri;
    private String scope;
    private String codeChallenge;
    private String codeChallengeMethod;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean used;
}