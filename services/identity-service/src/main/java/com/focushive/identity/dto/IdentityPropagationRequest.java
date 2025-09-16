package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentityPropagationRequest {
    private String updateType; // profile_update, persona_switch, permission_change
    private Map<String, Object> changes;
    private boolean notifyServices;
}
