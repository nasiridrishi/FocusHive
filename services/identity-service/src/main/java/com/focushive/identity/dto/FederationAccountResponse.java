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
public class FederationAccountResponse {
    private String providerId;
    private String externalUserId;
    private boolean linked;
    private Instant linkedAt;
}
