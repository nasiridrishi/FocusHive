package com.focushive.hive.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for sending hive invitations
 */
public class InvitationRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @Size(max = 500, message = "Message must not exceed 500 characters")
    private String message;

    // Default constructor
    public InvitationRequest() {}

    // Constructor
    public InvitationRequest(String email, String message) {
        this.email = email;
        this.message = message;
    }

    // Getters and setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}