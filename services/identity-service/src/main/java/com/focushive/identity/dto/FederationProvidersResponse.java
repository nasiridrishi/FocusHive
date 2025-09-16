package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response containing available federation providers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FederationProvidersResponse {
    
    private List<FederationProvider> providers;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FederationProvider {
        private String providerId;
        private String displayName;
        private String type; // oauth2, saml, oidc
        private String logoUrl;
        private boolean enabled;
    }
}