package com.focushive.notification.controller;

import com.focushive.notification.service.TokenBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication controller for handling auth-related operations.
 * Provides endpoints for logout and token management.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and token management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class AuthController {

    private final TokenBlacklistService tokenBlacklistService;

    /**
     * Logout endpoint to invalidate the current JWT token.
     * The token is blacklisted and cannot be used for future requests.
     *
     * @param authentication The current authentication object containing the JWT
     * @param request The HTTP request for logging purposes
     * @return Response indicating successful logout
     */
    @PostMapping("/logout")
    @Operation(
        summary = "Logout current user",
        description = "Invalidates the current JWT token by adding it to the blacklist",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully logged out"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    public ResponseEntity<Map<String, Object>> logout(
            Authentication authentication,
            HttpServletRequest request) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", "Not authenticated",
                            "message", "No valid authentication found"
                    ));
        }

        try {
            // Extract JWT from authentication
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String username = jwt.getSubject();

            // Blacklist the token
            tokenBlacklistService.blacklistToken(jwt, "user_logout");

            log.info("User {} logged out successfully from IP: {}",
                    username, getClientIp(request));

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Successfully logged out");
            response.put("timestamp", Instant.now());
            response.put("user", username);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Logout failed",
                            "message", "An error occurred during logout. Please try again."
                    ));
        }
    }

    /**
     * Logout all sessions for the current user.
     * Invalidates all tokens for the authenticated user across all devices/sessions.
     *
     * @param authentication The current authentication object
     * @return Response indicating successful logout of all sessions
     */
    @PostMapping("/logout/all")
    @Operation(
        summary = "Logout from all sessions",
        description = "Invalidates all JWT tokens for the current user across all sessions",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully logged out from all sessions"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    public ResponseEntity<Map<String, Object>> logoutAll(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", "Not authenticated",
                            "message", "No valid authentication found"
                    ));
        }

        try {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String username = jwt.getSubject();

            // Blacklist all tokens for the user
            tokenBlacklistService.blacklistAllUserTokens(username, "user_logout_all");

            log.info("User {} logged out from all sessions", username);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Successfully logged out from all sessions");
            response.put("timestamp", Instant.now());
            response.put("user", username);
            response.put("allSessions", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Logout all sessions failed: {}", e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Logout failed",
                            "message", "An error occurred during logout. Please try again."
                    ));
        }
    }

    /**
     * Check if the current token is valid (not blacklisted).
     * Useful for client applications to verify token status.
     *
     * @param authentication The current authentication object
     * @return Response indicating token validity status
     */
    @PostMapping("/token/validate")
    @Operation(
        summary = "Validate current token",
        description = "Checks if the current JWT token is valid and not blacklisted",
        responses = {
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "401", description = "Token is invalid or blacklisted")
        }
    )
    public ResponseEntity<Map<String, Object>> validateToken(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "valid", false,
                            "message", "Token is invalid or expired"
                    ));
        }

        try {
            Jwt jwt = (Jwt) authentication.getPrincipal();

            // Check if token is blacklisted
            boolean isBlacklisted = tokenBlacklistService.isBlacklisted(jwt);

            if (isBlacklisted) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "valid", false,
                                "message", "Token has been blacklisted"
                        ));
            }

            // Check if user is globally blacklisted
            boolean isUserBlacklisted = tokenBlacklistService.isUserBlacklisted(jwt.getSubject());

            if (isUserBlacklisted) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "valid", false,
                                "message", "User has been globally blacklisted"
                        ));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("message", "Token is valid");
            response.put("subject", jwt.getSubject());
            response.put("expiresAt", jwt.getExpiresAt());
            response.put("issuedAt", jwt.getIssuedAt());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "valid", false,
                            "message", "Token validation failed"
                    ));
        }
    }

    /**
     * Public endpoint to validate a token passed in request body.
     * This endpoint does not require authentication.
     *
     * @param request The request body containing the token
     * @return Response indicating token validity status
     */
    @PostMapping("/token/validate/public")
    @Operation(
        summary = "Validate a token",
        description = "Validates a JWT token passed in the request body",
        responses = {
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "400", description = "Invalid request or token format"),
            @ApiResponse(responseCode = "401", description = "Token is invalid or expired")
        }
    )
    public ResponseEntity<Map<String, Object>> validateTokenPublic(@RequestBody(required = false) Map<String, String> request) {
        if (request == null || !request.containsKey("token")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "valid", false,
                            "message", "Token is required in request body"
                    ));
        }

        String token = request.get("token");
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "valid", false,
                            "message", "Token cannot be empty"
                    ));
        }

        // For invalid format tokens, return 400
        if (!token.contains(".") || token.split("\\.").length != 3) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "valid", false,
                            "message", "Invalid token format"
                    ));
        }

        // For this example, we'll return 401 for any other validation failure
        // In production, you would actually validate the JWT here
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "valid", false,
                        "message", "Token validation not implemented"
                ));
    }

    /**
     * Extract client IP address from request.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}