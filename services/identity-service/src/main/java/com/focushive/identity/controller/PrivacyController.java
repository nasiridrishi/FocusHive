package com.focushive.identity.controller;

import com.focushive.identity.dto.*;
import com.focushive.identity.service.PrivacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Privacy and Data Management controller for GDPR compliance.
 * Provides endpoints for data access, portability, deletion, and consent management.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/privacy")
@Tag(name = "Privacy & Data Management", description = "GDPR-compliant privacy controls and data management endpoints")
@SecurityRequirement(name = "JWT")
@RequiredArgsConstructor
public class PrivacyController {

    private final PrivacyService privacyService;

    @Operation(
            summary = "Get user's privacy preferences",
            description = "Retrieve the user's current privacy preferences and consent status"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Privacy preferences retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/preferences")
    public ResponseEntity<PrivacyPreferencesResponse> getPrivacyPreferences(Authentication authentication) {
        log.debug("Privacy preferences request");
        
        UUID userId = getUserIdFromAuthentication(authentication);
        PrivacyPreferencesResponse response = privacyService.getPrivacyPreferences(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update privacy preferences",
            description = "Update the user's privacy preferences and consent settings"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Privacy preferences updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid preferences data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/preferences")
    public ResponseEntity<PrivacyPreferencesResponse> updatePrivacyPreferences(
            @Valid @RequestBody UpdatePrivacyPreferencesRequest request,
            Authentication authentication) {
        
        log.info("Privacy preferences update request");
        
        UUID userId = getUserIdFromAuthentication(authentication);
        PrivacyPreferencesResponse response = privacyService.updatePrivacyPreferences(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Export user data",
            description = "Export all user data in a portable format (GDPR Article 20 - Right to Data Portability)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Data export ready for download"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/data/export")
    public ResponseEntity<DataExportResponse> requestDataExport(
            @Valid @RequestBody DataExportRequest request,
            Authentication authentication) {
        
        log.info("Data export request");
        
        UUID userId = getUserIdFromAuthentication(authentication);
        DataExportResponse response = privacyService.initiateDataExport(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Download exported data",
            description = "Download the exported user data file"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Data file downloaded successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Export not found or expired")
    })
    @GetMapping("/data/export/{exportId}/download")
    public void downloadDataExport(
            @Parameter(description = "Export ID") @PathVariable UUID exportId,
            Authentication authentication,
            HttpServletResponse response) throws IOException {
        
        log.info("Data export download request for export: {}", exportId);
        
        UUID userId = getUserIdFromAuthentication(authentication);
        privacyService.downloadDataExport(userId, exportId, response);
    }

    @Operation(
            summary = "Get data export history",
            description = "Get the history of data exports for the user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Export history retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/data/exports")
    public ResponseEntity<DataExportHistoryResponse> getDataExportHistory(Authentication authentication) {
        log.debug("Data export history request");
        
        UUID userId = getUserIdFromAuthentication(authentication);
        DataExportHistoryResponse response = privacyService.getDataExportHistory(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Request account deletion",
            description = "Request permanent deletion of user account and all associated data (GDPR Article 17 - Right to Erasure)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deletion request submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid deletion request"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "409", description = "Deletion request already pending")
    })
    @PostMapping("/data/delete")
    public ResponseEntity<DataDeletionResponse> requestAccountDeletion(
            @Valid @RequestBody DataDeletionRequest request,
            Authentication authentication) {
        
        log.info("Account deletion request");
        
        UUID userId = getUserIdFromAuthentication(authentication);
        DataDeletionResponse response = privacyService.requestAccountDeletion(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Cancel account deletion",
            description = "Cancel a pending account deletion request"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deletion request cancelled successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "No pending deletion request found")
    })
    @PostMapping("/data/delete/cancel")
    public ResponseEntity<MessageResponse> cancelAccountDeletion(Authentication authentication) {
        log.info("Account deletion cancellation request");
        
        UUID userId = getUserIdFromAuthentication(authentication);
        privacyService.cancelAccountDeletion(userId);
        return ResponseEntity.ok(new MessageResponse("Account deletion request cancelled successfully"));
    }

    @Operation(
            summary = "Get data access log",
            description = "Get the log of who accessed the user's data (GDPR Article 15 - Right of Access)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Data access log retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/data/access-log")
    public ResponseEntity<DataAccessLogResponse> getDataAccessLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        
        log.debug("Data access log request");
        
        UUID userId = getUserIdFromAuthentication(authentication);
        DataAccessLogResponse response = privacyService.getDataAccessLog(userId, page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Grant data access consent",
            description = "Grant consent for specific data access or processing activities"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consent granted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid consent request"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/consent/grant")
    public ResponseEntity<ConsentResponse> grantConsent(
            @Valid @RequestBody GrantConsentRequest request,
            Authentication authentication) {
        
        log.info("Grant consent request");
        
        UUID userId = getUserIdFromAuthentication(authentication);
        ConsentResponse response = privacyService.grantConsent(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Revoke data access consent",
            description = "Revoke previously granted consent for data access or processing"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consent revoked successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Consent not found")
    })
    @PostMapping("/consent/revoke")
    public ResponseEntity<ConsentResponse> revokeConsent(
            @Valid @RequestBody RevokeConsentRequest request,
            Authentication authentication) {
        
        log.info("Revoke consent request");
        
        UUID userId = getUserIdFromAuthentication(authentication);
        ConsentResponse response = privacyService.revokeConsent(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get consent history",
            description = "Get the history of consent grants and revocations for the user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consent history retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/consent/history")
    public ResponseEntity<ConsentHistoryResponse> getConsentHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        
        log.debug("Consent history request");
        
        UUID userId = getUserIdFromAuthentication(authentication);
        ConsentHistoryResponse response = privacyService.getConsentHistory(userId, page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Request data rectification",
            description = "Request correction of inaccurate personal data (GDPR Article 16 - Right to Rectification)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rectification request submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid rectification request"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/data/rectify")
    public ResponseEntity<DataRectificationResponse> requestDataRectification(
            @Valid @RequestBody DataRectificationRequest request,
            Authentication authentication) {
        
        log.info("Data rectification request");
        
        UUID userId = getUserIdFromAuthentication(authentication);
        DataRectificationResponse response = privacyService.requestDataRectification(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get data processing activities",
            description = "Get information about how the user's data is being processed (GDPR Article 13-14 - Information to be provided)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Processing activities retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/data/processing")
    public ResponseEntity<DataProcessingResponse> getDataProcessingActivities(Authentication authentication) {
        log.debug("Data processing activities request");
        
        UUID userId = getUserIdFromAuthentication(authentication);
        DataProcessingResponse response = privacyService.getDataProcessingActivities(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Object to data processing",
            description = "Object to certain processing activities (GDPR Article 21 - Right to Object)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Objection recorded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid objection request"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/data/object")
    public ResponseEntity<DataProcessingObjectionResponse> objectToDataProcessing(
            @Valid @RequestBody DataProcessingObjectionRequest request,
            Authentication authentication) {
        
        log.info("Data processing objection request");
        
        UUID userId = getUserIdFromAuthentication(authentication);
        DataProcessingObjectionResponse response = privacyService.objectToDataProcessing(userId, request);
        return ResponseEntity.ok(response);
    }

    private UUID getUserIdFromAuthentication(Authentication authentication) {
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid user authentication", e);
        }
    }
}