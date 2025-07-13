package com.focushive.user.controller;

import com.focushive.user.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthController {
    
    @Operation(summary = "Register a new user", description = "Creates a new user account with email and password")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User successfully registered",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "User already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // TODO: Implement registration logic
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse("token", "user-id"));
    }
    
    @Operation(summary = "Login user", description = "Authenticates user and returns JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully authenticated",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // TODO: Implement login logic
        return ResponseEntity.ok(new AuthResponse("token", "user-id"));
    }
    
    @Operation(summary = "Refresh token", description = "Refreshes JWT token using refresh token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token successfully refreshed",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest request) {
        // TODO: Implement refresh logic
        return ResponseEntity.ok(new AuthResponse("new-token", "user-id"));
    }
    
    // Inner classes for DTOs
    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
        
        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    public static class RefreshRequest {
        @NotBlank
        private String refreshToken;
        
        // Getters and setters
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }
    
    public record AuthResponse(String accessToken, String userId) {}
}