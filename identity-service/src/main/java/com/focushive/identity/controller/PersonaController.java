package com.focushive.identity.controller;

import com.focushive.identity.dto.PersonaDto;
import com.focushive.identity.entity.Persona;
import com.focushive.identity.service.PersonaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for persona management.
 * Provides endpoints for CRUD operations, context switching, and persona templates.
 */
@RestController
@RequestMapping("/api/v1/personas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Persona Management", description = "APIs for managing user personas and context switching")
@SecurityRequirement(name = "JWT")
public class PersonaController {

    private final PersonaService personaService;

    @Operation(
            summary = "Create a new persona",
            description = "Create a new persona for the authenticated user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Persona created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid persona data or duplicate name"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping
    public ResponseEntity<PersonaDto> createPersona(
            @Valid @RequestBody PersonaDto personaDto,
            Authentication authentication) {
        
        log.debug("Creating persona for user: {}", authentication.getName());
        UUID userId = getUserIdFromAuthentication(authentication);
        
        PersonaDto created = personaService.createPersona(userId, personaDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "Get all personas for user",
            description = "Retrieve all personas for the authenticated user, ordered by priority (active, default, creation time)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Personas retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping
    public ResponseEntity<List<PersonaDto>> getUserPersonas(Authentication authentication) {
        log.debug("Getting personas for user: {}", authentication.getName());
        UUID userId = getUserIdFromAuthentication(authentication);
        
        List<PersonaDto> personas = personaService.getUserPersonas(userId);
        return ResponseEntity.ok(personas);
    }

    @Operation(
            summary = "Get a specific persona",
            description = "Retrieve a specific persona by ID for the authenticated user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Persona retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Persona or user not found")
    })
    @GetMapping("/{personaId}")
    public ResponseEntity<PersonaDto> getPersona(
            @Parameter(description = "Persona ID") @PathVariable UUID personaId,
            Authentication authentication) {
        
        log.debug("Getting persona {} for user: {}", personaId, authentication.getName());
        UUID userId = getUserIdFromAuthentication(authentication);
        
        PersonaDto persona = personaService.getPersona(userId, personaId);
        return ResponseEntity.ok(persona);
    }

    @Operation(
            summary = "Update a persona",
            description = "Update an existing persona for the authenticated user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Persona updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid persona data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Persona or user not found")
    })
    @PutMapping("/{personaId}")
    public ResponseEntity<PersonaDto> updatePersona(
            @Parameter(description = "Persona ID") @PathVariable UUID personaId,
            @Valid @RequestBody PersonaDto personaDto,
            Authentication authentication) {
        
        log.debug("Updating persona {} for user: {}", personaId, authentication.getName());
        UUID userId = getUserIdFromAuthentication(authentication);
        
        PersonaDto updated = personaService.updatePersona(userId, personaId, personaDto);
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "Delete a persona",
            description = "Delete a persona for the authenticated user. Cannot delete the default persona."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Persona deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot delete default persona"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Persona or user not found")
    })
    @DeleteMapping("/{personaId}")
    public ResponseEntity<Void> deletePersona(
            @Parameter(description = "Persona ID") @PathVariable UUID personaId,
            Authentication authentication) {
        
        log.debug("Deleting persona {} for user: {}", personaId, authentication.getName());
        UUID userId = getUserIdFromAuthentication(authentication);
        
        personaService.deletePersona(userId, personaId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Switch persona context",
            description = "Switch to a different persona for the authenticated user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Persona switched successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Persona or user not found")
    })
    @PostMapping("/{personaId}/switch")
    public ResponseEntity<PersonaDto> switchPersona(
            @Parameter(description = "Persona ID") @PathVariable UUID personaId,
            Authentication authentication) {
        
        log.debug("Switching to persona {} for user: {}", personaId, authentication.getName());
        UUID userId = getUserIdFromAuthentication(authentication);
        
        PersonaDto switched = personaService.switchPersona(userId, personaId);
        return ResponseEntity.ok(switched);
    }

    @Operation(
            summary = "Get active persona",
            description = "Get the currently active persona for the authenticated user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active persona retrieved successfully"),
            @ApiResponse(responseCode = "204", description = "No active persona found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/active")
    public ResponseEntity<PersonaDto> getActivePersona(Authentication authentication) {
        log.debug("Getting active persona for user: {}", authentication.getName());
        UUID userId = getUserIdFromAuthentication(authentication);
        
        Optional<PersonaDto> activePersona = personaService.getActivePersona(userId);
        return activePersona
                .map(persona -> ResponseEntity.ok(persona))
                .orElse(ResponseEntity.noContent().build());
    }

    @Operation(
            summary = "Set default persona",
            description = "Set a persona as the default persona for the authenticated user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Default persona set successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Persona or user not found")
    })
    @PostMapping("/{personaId}/default")
    public ResponseEntity<PersonaDto> setDefaultPersona(
            @Parameter(description = "Persona ID") @PathVariable UUID personaId,
            Authentication authentication) {
        
        log.debug("Setting persona {} as default for user: {}", personaId, authentication.getName());
        UUID userId = getUserIdFromAuthentication(authentication);
        
        PersonaDto defaultPersona = personaService.setDefaultPersona(userId, personaId);
        return ResponseEntity.ok(defaultPersona);
    }

    @Operation(
            summary = "Create persona from template",
            description = "Create a new persona from a predefined template"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Persona created from template successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid template type or duplicate name"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/templates/{type}")
    public ResponseEntity<PersonaDto> createPersonaFromTemplate(
            @Parameter(description = "Template type") @PathVariable Persona.PersonaType type,
            Authentication authentication) {
        
        log.debug("Creating persona from template {} for user: {}", type, authentication.getName());
        UUID userId = getUserIdFromAuthentication(authentication);
        
        PersonaDto created = personaService.createPersonaFromTemplate(userId, type);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "Get persona templates",
            description = "Get available persona templates"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Templates retrieved successfully")
    })
    @GetMapping("/templates")
    public ResponseEntity<List<PersonaDto>> getPersonaTemplates() {
        log.debug("Getting persona templates");
        
        List<PersonaDto> templates = personaService.getPersonaTemplates();
        return ResponseEntity.ok(templates);
    }

    // Helper method to extract user ID from authentication
    private UUID getUserIdFromAuthentication(Authentication authentication) {
        // Assuming the authentication principal contains the user ID
        // This might need to be adjusted based on your security implementation
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            // If authentication.getName() is not a UUID, you might need to look up the user
            // This depends on your authentication setup
            throw new IllegalStateException("Invalid user authentication", e);
        }
    }
}