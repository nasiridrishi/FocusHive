package com.focushive.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OAuth2 client list response")
public class OAuth2ClientListResponse {

    @Schema(description = "List of OAuth2 clients")
    private List<OAuth2ClientResponse> clients;

    @Schema(description = "Total number of clients", example = "5")
    private Integer totalCount;
}