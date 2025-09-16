package com.focushive.buddy.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.UUID;

/**
 * DTO for partnership request operations.
 * Used when creating or managing partnership requests between users.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PartnershipRequestDto {

    @NotNull(message = "Requester ID is required")
    private UUID requesterId;

    @NotNull(message = "Recipient ID is required")
    private UUID recipientId;

    @Size(max = 500, message = "Message must not exceed 500 characters")
    private String message;

    @Min(value = 7, message = "Duration must be at least 7 days")
    @Max(value = 365, message = "Duration must not exceed 365 days")
    @Builder.Default
    private Integer durationDays = 30;

    @Size(max = 1000, message = "Agreement text must not exceed 1000 characters")
    private String agreementText;

    /**
     * Indicates if this is a mutual request (both users requesting each other)
     */
    @Builder.Default
    private Boolean isMutual = false;

    /**
     * Priority level of the request (1=low, 5=high)
     */
    @Min(value = 1, message = "Priority must be between 1 and 5")
    @Max(value = 5, message = "Priority must be between 1 and 5")
    @Builder.Default
    private Integer priority = 3;
}