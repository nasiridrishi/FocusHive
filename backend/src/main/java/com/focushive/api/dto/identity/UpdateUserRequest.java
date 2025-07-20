package com.focushive.api.dto.identity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    private String displayName;
    private String bio;
    private String avatarUrl;
    private String timezone;
    private String locale;
}