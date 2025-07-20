package com.focushive.api.dto.identity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenIntrospectionRequest {
    private String token;
    private String tokenTypeHint; // access_token or refresh_token
}