package com.focushive.notification.controller;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO for template validation results.
 */
@Data
@Builder
public class ValidationResult {
    private boolean valid;
    private List<String> errors;
}