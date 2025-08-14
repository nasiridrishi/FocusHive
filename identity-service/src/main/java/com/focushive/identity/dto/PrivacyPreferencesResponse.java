package com.focushive.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User privacy preferences response")
public class PrivacyPreferencesResponse {

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

    @Schema(description = "Account visibility settings")
    private String accountVisibility;

    @Schema(description = "Profile visibility settings")
    private String profileVisibility;

    @Schema(description = "Activity visibility settings")
    private String activityVisibility;

    @Schema(description = "Data retention period preference in days")
    private Integer dataRetentionDays;

    @Schema(description = "Automatic data deletion enabled")
    private Boolean autoDataDeletion;

    @Schema(description = "Last updated timestamp")
    private Instant updatedAt;
}