package com.focushive.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data export request")
public class DataExportRequest {

    @NotEmpty(message = "At least one data category must be selected")
    @Schema(description = "Data categories to export", 
            example = "[\"profile\", \"personas\", \"preferences\", \"activity\", \"connections\"]")
    private Set<String> dataCategories;

    @Schema(description = "Export format", example = "json", allowableValues = {"json", "xml", "csv"})
    @Builder.Default
    private String format = "json";

    @Schema(description = "Include deleted data if available", example = "false")
    @Builder.Default
    private Boolean includeDeleted = false;

    @Schema(description = "Date range start for activity data (ISO 8601)", example = "2023-01-01T00:00:00Z")
    private String dateFrom;

    @Schema(description = "Date range end for activity data (ISO 8601)", example = "2023-12-31T23:59:59Z")
    private String dateTo;

    @Schema(description = "Specific persona IDs to export (if empty, exports all)")
    private Set<String> personaIds;
}