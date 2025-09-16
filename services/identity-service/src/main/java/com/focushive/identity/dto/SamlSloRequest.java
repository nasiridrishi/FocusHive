package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SamlSloRequest {
    private String samlLogoutRequest;
    private String samlLogoutResponse;
    private String relayState;
}
