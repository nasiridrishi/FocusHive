package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to initiate federation authentication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FederationAuthRequest {
    private String providerId;
    private String returnUrl;
    private String state;
}