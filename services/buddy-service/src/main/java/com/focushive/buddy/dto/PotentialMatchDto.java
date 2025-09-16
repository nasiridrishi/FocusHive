package com.focushive.buddy.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing a potential buddy match for a user.
 * Contains user information and compatibility score.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PotentialMatchDto {

    @NotNull
    private String userId;

    @NotNull
    private String displayName;

    private String timezone;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double compatibilityScore;

    private List<String> commonInterests;

    private List<String> focusAreas;

    private String experienceLevel;

    private String communicationStyle;

    private String personalityType;

    private Integer timezoneOffsetHours;

    private String reasonForMatch;
}