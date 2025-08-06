package com.focushive.api.config;

import brave.sampler.Sampler;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for distributed tracing using Micrometer Tracing and Brave.
 * Provides tracing for inter-service communication and internal operations.
 */
@Slf4j
@Configuration
@Profile("!test")
@AutoConfigureAfter(ObservationAutoConfiguration.class)
public class TracingConfiguration {
    
    @Value("${management.tracing.sampling.probability:1.0}")
    private Float samplingProbability;
    
    @Value("${spring.application.name}")
    private String serviceName;
    
    /**
     * Configure sampling strategy for traces.
     * In production, you might want to sample less frequently to reduce overhead.
     */
    @Bean
    public Sampler alwaysSampler() {
        log.info("Configuring tracing with sampling probability: {}", samplingProbability);
        return Sampler.create(samplingProbability);
    }
    
    /**
     * Enable @Observed annotation support for custom tracing.
     */
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        log.info("Enabling @Observed annotation support for service: {}", serviceName);
        return new ObservedAspect(observationRegistry);
    }
    
    /**
     * Custom observation registry configuration.
     */
    @Bean
    public ObservationRegistry observationRegistry(MeterRegistry meterRegistry) {
        ObservationRegistry registry = ObservationRegistry.create();
        
        // Add custom observation handlers
        registry.observationConfig()
            .observationHandler(new CustomObservationHandler())
            // Timer metrics
            .observationHandler(context -> {
                if (context.getName().startsWith("identity.service")) {
                    log.debug("Identity Service operation: {} - Duration: {}ms", 
                            context.getName(), 
                            context.getHighCardinalityKeyValue("duration"));
                }
                return true;
            });
        
        return registry;
    }
    
    /**
     * Custom observation handler for specific business metrics.
     */
    private static class CustomObservationHandler implements io.micrometer.observation.ObservationHandler<io.micrometer.observation.Observation.Context> {
        
        @Override
        public void onStart(io.micrometer.observation.Observation.Context context) {
            log.debug("Starting observation: {}", context.getName());
        }
        
        @Override
        public void onError(io.micrometer.observation.Observation.Context context) {
            log.warn("Observation error: {} - {}", 
                    context.getName(), 
                    context.getError().getMessage());
        }
        
        @Override
        public void onStop(io.micrometer.observation.Observation.Context context) {
            log.debug("Completed observation: {}", context.getName());
        }
        
        @Override
        public boolean supportsContext(io.micrometer.observation.Observation.Context context) {
            return context.getName().startsWith("identity.service") || 
                   context.getName().startsWith("focushive");
        }
    }
}