package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopyPreferencesRequest {
    private UUID sourcePersonaId;
    private UUID targetPersonaId;
    private List<String> categories;
    private boolean overwrite;
}
