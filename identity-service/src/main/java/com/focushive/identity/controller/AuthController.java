package com.focushive.identity.controller;

import com.focushive.identity.dto.*;
import com.focushive.identity.entity.User;
import com.focushive.identity.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller for the Identity Service.
 * Handles user registration, login, token management, and persona switching.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User authentication and identity management endpoints")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthenticationService authenticationService;
    
    @Operation(summary = "Register a new user", 
              description = "Creates a new user account with a default persona")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User successfully registered"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "User already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());
        AuthenticationResponse response = authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(summary = "Login user", 
              description = "Authenticates user and returns JWT tokens")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("User login attempt for: {}", request.getUsernameOrEmail());
        AuthenticationResponse response = authenticationService.login(request);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Refresh access token", 
              description = "Returns a new access token using a valid refresh token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token successfully refreshed"),
        @ApiResponse(responseCode = "401", description = "Invalid refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthenticationResponse response = authenticationService.refreshToken(request);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Validate token", 
              description = "Validates a JWT token and returns user information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token is valid"),
        @ApiResponse(responseCode = "401", description = "Invalid token")
    })
    @PostMapping("/validate")
    public ResponseEntity<ValidateTokenResponse> validateToken(
            @Valid @RequestBody ValidateTokenRequest request) {
        ValidateTokenResponse response = authenticationService.validateToken(request);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Introspect token", 
              description = "Returns detailed information about a token (OAuth2 introspection)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token information returned"),
        @ApiResponse(responseCode = "401", description = "Invalid token")
    })
    @PostMapping("/introspect")
    public ResponseEntity<IntrospectTokenResponse> introspectToken(
            @Valid @RequestBody IntrospectTokenRequest request) {
        IntrospectTokenResponse response = authenticationService.introspectToken(request);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Logout user", 
              description = "Invalidates the user's current session and tokens")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully logged out"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody LogoutRequest request,
            @AuthenticationPrincipal User user) {
        authenticationService.logout(request, user);
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }
    
    @Operation(summary = "Request password reset", 
              description = "Initiates password reset process by sending reset token to email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reset email sent if user exists"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/password/reset-request")
    public ResponseEntity<MessageResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        authenticationService.requestPasswordReset(request.getEmail());
        // Always return success to prevent user enumeration
        return ResponseEntity.ok(new MessageResponse("If an account exists with this email, a reset link has been sent."));
    }
    
    @Operation(summary = "Reset password", 
              description = "Resets user password using a valid reset token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password successfully reset"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @PostMapping("/password/reset")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        authenticationService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Password has been successfully reset."));
    }
    
    @Operation(summary = "Switch active persona", 
              description = "Switches the user's active persona/profile")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Persona switched successfully"),
        @ApiResponse(responseCode = "404", description = "Persona not found"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/personas/switch")
    public ResponseEntity<AuthenticationResponse> switchPersona(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody SwitchPersonaRequest request,
            @AuthenticationPrincipal User user) {
        AuthenticationResponse response = authenticationService.switchPersona(request, user);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Extract JWT token from Authorization header.
     */
    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid authorization header");
    }
}