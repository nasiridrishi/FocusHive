package com.focushive.api.dto.identity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenIntrospectionResponse {
    private boolean active;
    private String scope;
    private String clientId;
    private String username;
    private String tokenType;
    private Long exp;
    private Long iat;
    private String sub;
    private List<String> authorities;
    private Map<String, Object> additionalInfo;
}