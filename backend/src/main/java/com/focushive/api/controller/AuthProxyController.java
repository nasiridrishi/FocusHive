package com.focushive.api.controller;

import com.focushive.api.client.IdentityServiceClient;
import com.focushive.api.dto.identity.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

/**
 * Proxy controller that forwards authentication requests to the Identity Service.
 * This allows FocusHive to appear as a single entry point while delegating
 * identity management to the specialized service.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User authentication proxy endpoints")
@RequiredArgsConstructor
@Profile("!test") // Don't load this controller in test profile
public class AuthProxyController {
    
    private final RestTemplate restTemplate;
    
    @Value("${identity.service.url}")
    private String identityServiceUrl;
    
    @Operation(summary = "Register a new user", 
              description = "Creates a new user account in the Identity Service")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User successfully registered"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "User already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            String url = identityServiceUrl + "/api/v1/auth/register";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<RegisterRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Object> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Object.class
            );
            
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            log.error("Failed to register user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Registration failed: " + e.getMessage());
        }
    }
    
    @Operation(summary = "Login user", 
              description = "Authenticates user with the Identity Service and returns JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            String url = identityServiceUrl + "/api/v1/auth/login";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Object> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Object.class
            );
            
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            log.error("Failed to login user", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Login failed: " + e.getMessage());
        }
    }
    
    @Operation(summary = "Refresh token", 
              description = "Refreshes JWT token using refresh token from Identity Service")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token successfully refreshed"),
        @ApiResponse(responseCode = "401", description = "Invalid refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            String url = identityServiceUrl + "/api/v1/auth/refresh";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<RefreshTokenRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Object> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Object.class
            );
            
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token refresh failed: " + e.getMessage());
        }
    }
    
    @Operation(summary = "Logout user", 
              description = "Invalidates the user's current session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully logged out"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {
        try {
            String url = identityServiceUrl + "/api/v1/auth/logout";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Object> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Object.class
            );
            
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            log.error("Failed to logout user", e);
            return ResponseEntity.ok("Logged out locally");
        }
    }
    
    // Request DTOs
    
    @Data
    public static class RegisterRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;
        
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        private String username;
        
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;
        
        @NotBlank(message = "Display name is required")
        private String displayName;
    }
    
    @Data
    public static class LoginRequest {
        @NotBlank(message = "Username or email is required")
        private String usernameOrEmail;
        
        @NotBlank(message = "Password is required")
        private String password;
    }
    
    @Data
    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }
}