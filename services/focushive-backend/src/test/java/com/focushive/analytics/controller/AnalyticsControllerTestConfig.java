package com.focushive.analytics.controller;

import com.focushive.analytics.service.AnalyticsService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class AnalyticsControllerTestConfig {

    @Bean
    @Primary
    public AnalyticsService analyticsService() {
        return org.mockito.Mockito.mock(AnalyticsService.class);
    }
}