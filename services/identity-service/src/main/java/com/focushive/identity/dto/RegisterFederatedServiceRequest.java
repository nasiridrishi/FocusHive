package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterFederatedServiceRequest {
    private String serviceName;
    private String serviceUrl;
    private String description;
    private String publicKey;
}
