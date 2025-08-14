package com.focushive.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update privacy preferences request")
public class UpdatePrivacyPreferencesRequest {

    @Schema(description = "Data processing consent status")
    private Map<String, Boolean> consentStatus;

    @Schema(description = "Data sharing preferences")
    private Map<String, String> dataSharingPreferences;

    @Schema(description = "Marketing communication consent")
    private Boolean marketingCommunicationConsent;

    @Schema(description = "Analytics and tracking consent")
    private Boolean analyticsConsent;

    @Schema(description = "Third-party data sharing consent")
    private Boolean thirdPartyDataSharingConsent;

    @Schema(description = "Account visibility settings", allowableValues = {"public", "private", "friends"})
    private String accountVisibility;

    @Schema(description = "Profile visibility settings", allowableValues = {"public", "private", "friends"})
    private String profileVisibility;

    @Schema(description = "Activity visibility settings", allowableValues = {"public", "private", "friends"})
    private String activityVisibility;

    @Min(value = 30, message = "Data retention must be at least 30 days")
    @Max(value = 2555, message = "Data retention cannot exceed 7 years")
    @Schema(description = "Data retention period preference in days", example = "730")
    private Integer dataRetentionDays;

    @Schema(description = "Automatic data deletion enabled")
    private Boolean autoDataDeletion;
}