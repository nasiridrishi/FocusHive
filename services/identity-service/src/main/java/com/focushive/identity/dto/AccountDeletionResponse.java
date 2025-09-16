package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for account deletion operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDeletionResponse {

    private boolean success;
    private String recoveryToken;
    private LocalDateTime deletionScheduledFor;
    private LocalDateTime recoveryDeadline;
    private String dataExportUrl;
    private String message;
}