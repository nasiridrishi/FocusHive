package com.focushive.notification.service;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for template statistics information.
 */
@Data
@Builder
public class TemplateStatistics {
    private long languageCount;
    private long templateCount;
}