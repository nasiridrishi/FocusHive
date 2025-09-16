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
public class FederatedServicesResponse {
    private List<FederatedServiceInfo> services;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FederatedServiceInfo {
        private String serviceId;
        private String serviceName;
        private String serviceUrl;
        private boolean active;
    }
}
