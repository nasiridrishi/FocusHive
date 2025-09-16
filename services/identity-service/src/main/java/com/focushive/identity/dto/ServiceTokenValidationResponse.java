package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTokenValidationResponse {
    private boolean valid;
    private UUID userId;
    private String serviceId;
    private List<String> scopes;
    private Instant expiresAt;
    private String errorMessage;
}
