package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentityPropagationResponse {
    private boolean success;
    private Map<String, Boolean> serviceResults;
    private List<String> failedServices;
    private String message;
}
