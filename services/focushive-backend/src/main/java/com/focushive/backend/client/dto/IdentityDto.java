package com.focushive.backend.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentityDto {
    private UUID id;
    private String primaryEmail;
    private boolean emailVerified;
    private String username;
    private List<PersonaDto> personas;
    private UUID activePersonaId;
    private Map<String, Object> globalSettings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}