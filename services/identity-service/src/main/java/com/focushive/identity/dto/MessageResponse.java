package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Simple response DTO for messages.
 */
@Data
@AllArgsConstructor
public class MessageResponse {
    private String message;
}