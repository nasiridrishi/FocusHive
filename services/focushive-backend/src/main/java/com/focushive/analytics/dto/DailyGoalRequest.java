package com.focushive.analytics.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for creating or updating daily goals.
 * Contains goal target information and metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyGoalRequest {

    @NotNull(message = "Target minutes is required")
    @Min(value = 1, message = "Target minutes must be at least 1")
    @Max(value = 1440, message = "Target minutes cannot exceed 24 hours (1440 minutes)")
    private Integer targetMinutes;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date = LocalDate.now(); // Default to today

    @Size(max = 200, message = "Description cannot exceed 200 characters")
    private String description;

    @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "Priority must be LOW, MEDIUM, or HIGH")
    private String priority = "MEDIUM";

    @AssertTrue(message = "Date cannot be in the past")
    public boolean isDateValid() {
        return date == null || !date.isBefore(LocalDate.now());
    }
}