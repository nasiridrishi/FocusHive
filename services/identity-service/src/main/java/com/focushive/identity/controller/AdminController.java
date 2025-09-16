package com.focushive.identity.controller;

import com.focushive.identity.entity.User;
import com.focushive.identity.repository.UserRepository;
import com.focushive.identity.service.AccountLockoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin controller for account management operations.
 * Includes account lockout management functionality.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administrative operations")
@SecurityRequirement(name = "bearer-jwt")
public class AdminController {

    private final UserRepository userRepository;
    private final AccountLockoutService accountLockoutService;

    /**
     * Unlock a user account (admin operation).
     */
    @PostMapping("/users/{userId}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unlock user account", description = "Manually unlock a user account that was locked due to failed login attempts")
    public ResponseEntity<String> unlockAccount(
            @Parameter(description = "User ID to unlock") @PathVariable UUID userId,
            @Parameter(description = "Reason for unlocking") @RequestParam(defaultValue = "ADMIN_UNLOCK") String reason) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        accountLockoutService.unlockAccount(user, reason);

        log.info("Admin unlocked account for user: {} - Reason: {}", user.getUsername(), reason);

        return ResponseEntity.ok("Account unlocked successfully");
    }

    /**
     * Get account lockout status for a user.
     */
    @GetMapping("/users/{userId}/lockout-status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get account lockout status", description = "Get detailed lockout information for a user account")
    public ResponseEntity<AccountLockoutService.AccountLockoutStatus> getAccountLockoutStatus(
            @Parameter(description = "User ID to check") @PathVariable UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        AccountLockoutService.AccountLockoutStatus status = accountLockoutService.getAccountLockoutStatus(user);

        return ResponseEntity.ok(status);
    }
}