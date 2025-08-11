package com.focushive.buddy.dto;

import com.focushive.buddy.entity.BuddyPreferences.CommunicationStyle;
import com.focushive.buddy.entity.BuddyPreferences.WorkHours;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuddyPreferencesDTO {
    private Long id;
    private String userId;
    private String preferredTimezone;
    private Map<String, WorkHours> preferredWorkHours;
    private List<String> focusAreas;
    private CommunicationStyle communicationStyle;
    
    @NotNull
    private Boolean matchingEnabled;
}