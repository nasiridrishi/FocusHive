package com.focushive.identity.controller;

import com.focushive.identity.annotation.RateLimit;
import com.focushive.identity.dto.*;
import com.focushive.identity.entity.User;
import com.focushive.identity.service.AuthenticationService;
import com.focushive.identity.service.CookieJwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

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
    private final CookieJwtService cookieJwtService;
    
    @Operation(summary = "Register a new user", 
              description = "Creates a new user account with a default persona")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User successfully registered"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "User already exists"),
        @ApiResponse(responseCode = "429", description = "Too many requests - Rate limit exceeded")
    })
    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(value = 2, window = 1, timeUnit = TimeUnit.MINUTES, type = RateLimit.RateLimitType.IP, 
              message = "Too many registration attempts. Please wait before trying again.",
              progressivePenalties = true)
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest request, 
            HttpServletResponse httpResponse) {
        log.info("Processing user registration request");
        AuthenticationResponse response = authenticationService.register(request);
        
        // Set tokens as secure httpOnly cookies
        cookieJwtService.setAccessTokenCookie(httpResponse, response.getAccessToken());
        cookieJwtService.setRefreshTokenCookie(httpResponse, response.getRefreshToken());
        
        // For backward compatibility, still return tokens in response body
        // Frontend can migrate to use cookies instead
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(summary = "Login user", 
              description = "Authenticates user and returns JWT tokens")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "429", description = "Too many requests - Rate limit exceeded")
    })
    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(value = 5, window = 1, timeUnit = TimeUnit.MINUTES, type = RateLimit.RateLimitType.IP,
              message = "Too many login attempts. Please wait before trying again.",
              progressivePenalties = true)
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody LoginRequest request, 
            HttpServletResponse httpResponse) {
        log.info("User login attempt for: {}", request.getUsernameOrEmail());
        AuthenticationResponse response = authenticationService.login(request);
        
        // Set tokens as secure httpOnly cookies
        cookieJwtService.setAccessTokenCookie(httpResponse, response.getAccessToken());
        cookieJwtService.setRefreshTokenCookie(httpResponse, response.getRefreshToken());
        
        // For backward compatibility, still return tokens in response body
        // Frontend can migrate to use cookies instead
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Refresh access token", 
              description = "Returns a new access token using a valid refresh token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token successfully refreshed"),
        @ApiResponse(responseCode = "401", description = "Invalid refresh token"),
        @ApiResponse(responseCode = "429", description = "Too many requests - Rate limit exceeded")
    })
    @PostMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(value = 10, window = 1, timeUnit = TimeUnit.MINUTES, type = RateLimit.RateLimitType.USER,
              message = "Too many token refresh attempts. Please wait before trying again.")
    public ResponseEntity<AuthenticationResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request, 
            HttpServletResponse httpResponse) {
        AuthenticationResponse response = authenticationService.refreshToken(request);
        
        // Set new tokens as secure httpOnly cookies
        cookieJwtService.setAccessTokenCookie(httpResponse, response.getAccessToken());
        cookieJwtService.setRefreshTokenCookie(httpResponse, response.getRefreshToken());
        
        // For backward compatibility, still return tokens in response body
        // Frontend can migrate to use cookies instead
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Validate token", 
              description = "Validates a JWT token and returns user information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token is valid"),
        @ApiResponse(responseCode = "401", description = "Invalid token")
    })
    @PostMapping(value = "/validate", produces = MediaType.APPLICATION_JSON_VALUE)
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
    @PostMapping(value = "/introspect", produces = MediaType.APPLICATION_JSON_VALUE)
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
    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> logout(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody LogoutRequest request,
            @AuthenticationPrincipal User user,
            HttpServletResponse httpResponse) {
        authenticationService.logout(request, user);
        
        // Clear httpOnly cookies
        cookieJwtService.clearAllCookies(httpResponse);
        
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }
    
    @Operation(summary = "Request password reset", 
              description = "Initiates password reset process by sending reset token to email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reset email sent if user exists"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "429", description = "Too many requests - Rate limit exceeded")
    })
    @PostMapping(value = "/password/reset-request", produces = MediaType.APPLICATION_JSON_VALUE)
    // @RateLimit(value = 1, window = 1, timeUnit = TimeUnit.MINUTES, type = RateLimit.RateLimitType.IP,
    //          message = "Too many password reset requests. Please wait before trying again.",
    //          progressivePenalties = true)
    public ResponseEntity<MessageResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        log.info("Received password reset request for email: {}", request.getEmail());
        authenticationService.requestPasswordReset(request.getEmail());
        log.info("Password reset request processed, returning response");
        // Always return success to prevent user enumeration
        return ResponseEntity.ok(new MessageResponse("If an account exists with this email, a reset link has been sent."));
    }
    
    @Operation(summary = "Reset password", 
              description = "Resets user password using a valid reset token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password successfully reset"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired token"),
        @ApiResponse(responseCode = "429", description = "Too many requests - Rate limit exceeded")
    })
    @PostMapping(value = "/password/reset", produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(value = 3, window = 5, timeUnit = TimeUnit.MINUTES, type = RateLimit.RateLimitType.IP,
              message = "Too many password reset attempts. Please wait before trying again.")
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
    @PostMapping(value = "/personas/switch", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated() and @securityService.hasAccessToPersona(#request.personaId)")
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