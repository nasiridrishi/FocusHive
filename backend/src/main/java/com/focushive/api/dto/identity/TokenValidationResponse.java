package com.focushive.api.dto.identity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidationResponse {
    private boolean valid;
    private UUID userId;
    private String username;
    private String email;
    private List<String> authorities;
    private PersonaDto activePersona;
    private String errorMessage;
}