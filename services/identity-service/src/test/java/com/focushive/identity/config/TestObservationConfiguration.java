package com.focushive.identity.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import io.micrometer.observation.ObservationRegistry;

@TestConfiguration
public class TestObservationConfiguration {
    
    @Bean
    @Primary
    public ObservationRegistry observationRegistry() {
        return ObservationRegistry.NOOP;
    }
}