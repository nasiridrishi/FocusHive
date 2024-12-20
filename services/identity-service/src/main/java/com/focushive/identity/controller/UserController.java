package com.focushive.identity.controller;

import com.focushive.identity.dto.*;
import com.focushive.identity.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for user profile management operations.
 * Provides endpoints for user profile retrieval, update, password change, and account management.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "User profile and account management endpoints")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
public class UserController {

    private final UserManagementService userManagementService;

    @Operation(
            summary = "Get user profile",
            description = "Retrieve the current user's profile information"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        UserProfileResponse profile = userManagementService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @Operation(
            summary = "Update user profile",
            description = "Update the current user's profile information"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateUserProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        UUID userId = getUserIdFromAuthentication(authentication);
        UserProfileResponse updatedProfile = userManagementService.updateUserProfile(userId, request);
        return ResponseEntity.ok(updatedProfile);
    }

    @Operation(
            summary = "Change password",
            description = "Change the current user's password"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid password or request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized or incorrect current password"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = getUserIdFromAuthentication(authentication);
        userManagementService.changePassword(userId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Delete account",
            description = "Initiate account deletion with grace period for recovery"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account deletion initiated"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized or incorrect password"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/account")
    public ResponseEntity<AccountDeletionResponse> deleteAccount(
            Authentication authentication,
            @Valid @RequestBody DeleteAccountRequest request) {
        UUID userId = getUserIdFromAuthentication(authentication);
        AccountDeletionResponse response = userManagementService.deleteAccount(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Recover deleted account",
            description = "Recover a deleted account within the grace period using recovery token"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account recovered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired recovery token"),
            @ApiResponse(responseCode = "404", description = "Account not found or recovery period expired")
    })
    @PostMapping("/recover-account")
    public ResponseEntity<AccountRecoveryResponse> recoverAccount(
            @RequestParam String recoveryToken) {
        AccountRecoveryResponse response = userManagementService.recoverAccount(recoveryToken);
        return ResponseEntity.ok(response);
    }

    /**
     * Extract user ID from authentication context.
     * @param authentication Spring Security authentication object
     * @return UUID of the authenticated user
     */
    private UUID getUserIdFromAuthentication(Authentication authentication) {
        // This extracts the user ID from the authentication principal
        // The JWT token sets the user ID as the principal name
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format in authentication: {}", authentication.getName());
            throw new IllegalStateException("Invalid user ID in authentication context", e);
        }
    }
}