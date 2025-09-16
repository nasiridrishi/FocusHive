package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for deleting user account.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteAccountRequest {

    @NotBlank(message = "Password confirmation is required for account deletion")
    private String password;

    private String reason;

    private boolean exportData;
}