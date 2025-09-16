package com.focushive.buddy.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for matching preferences, used for API requests/responses.
 * Maps to BuddyPreferences entity.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MatchingPreferencesDto {

    @NotNull
    private String userId;

    @Size(max = 50)
    private String preferredTimezone;

    private Map<String, Object> preferredWorkHours;

    private List<String> focusAreas;

    private List<String> goals;

    private List<String> interests;

    @Size(max = 50)
    private String communicationStyle;

    @Builder.Default
    private Boolean matchingEnabled = true;

    @Min(0)
    @Max(12)
    @Builder.Default
    private Integer timezoneFlexibility = 2;

    @Min(1)
    @Max(168)
    @Builder.Default
    private Integer minCommitmentHours = 10;

    @Min(1)
    @Max(10)
    @Builder.Default
    private Integer maxPartners = 3;

    @Size(max = 10)
    @Pattern(regexp = "^[a-z]{2,3}(-[A-Z]{2})?$", message = "Language must be in ISO format")
    @Builder.Default
    private String language = "en";

    private List<String> languages;

    @Size(max = 50)
    private String personalityType;

    @Size(max = 20)
    private String experienceLevel;

    private List<String> preferredFocusTimes;

    private LocalDateTime updatedAt;
}