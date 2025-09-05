package com.focushive.identity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for switching active persona.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwitchPersonaRequest {
    
    @NotNull(message = "Persona ID is required")
    private UUID personaId;
}