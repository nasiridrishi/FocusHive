package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkedAccountsResponse {
    private List<LinkedAccount> accounts;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkedAccount {
        private String providerId;
        private String providerName;
        private String externalUserId;
        private String linkedAt;
    }
}
