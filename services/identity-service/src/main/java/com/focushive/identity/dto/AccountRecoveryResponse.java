package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for account recovery operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountRecoveryResponse {

    private boolean success;
    private UUID userId;
    private String username;
    private String email;
    private LocalDateTime recoveredAt;
    private String message;
}