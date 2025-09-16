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
public class FederatedServiceResponse {
    private String serviceId;
    private String serviceName;
    private String serviceUrl;
    private boolean active;
    private Instant registeredAt;
}
