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
public class ServiceTokenResponse {
    private String token;
    private String tokenType;
    private Instant expiresAt;
    private String serviceId;
}
