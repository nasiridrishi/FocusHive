package com.focushive.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Comprehensive observability configuration for FocusHive Backend.
 * 
 * This configuration provides:
 * - Micrometer metrics with Prometheus export
 * - Distributed tracing with OpenTelemetry/Zipkin
 * - JVM and system metrics
 * - Custom business metrics
 * - Observation aspect for @Observed annotations
 * - Performance monitoring
 */
@Slf4j
@Configuration
@Profile("!test")
@AutoConfigureAfter(ObservationAutoConfiguration.class)
public class ObservabilityConfiguration {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${app.version:1.0.0-SNAPSHOT}")
    private String applicationVersion;

    @Value("${management.tracing.sampling.probability:1.0}")
    private Float tracingSamplingProbability;

    /**
     * Configure common tags for all metrics.
     * These tags will be automatically applied to all metrics.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> meterRegistryCustomizer() {
        return registry -> {
            registry.config().commonTags(
                "application", applicationName,
                "version", applicationVersion,
                "environment", System.getProperty("spring.profiles.active", "default")
            );
            
            log.info("Configured MeterRegistry with common tags: application={}, version={}", 
                    applicationName, applicationVersion);
        };
    }

    /**
     * Enable @Observed annotation support for custom observations.
     */
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        log.info("Enabling @Observed annotation support for application: {}", applicationName);
        return new ObservedAspect(observationRegistry);
    }

    /**
     * Configure JVM metrics collection.
     * Provides insights into memory usage, garbage collection, threads, and class loading.
     */
    @Bean
    public ClassLoaderMetrics classLoaderMetrics(MeterRegistry meterRegistry) {
        ClassLoaderMetrics metrics = new ClassLoaderMetrics();
        metrics.bindTo(meterRegistry);
        log.debug("Registered ClassLoaderMetrics");
        return metrics;
    }

    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics(MeterRegistry meterRegistry) {
        JvmMemoryMetrics metrics = new JvmMemoryMetrics();
        metrics.bindTo(meterRegistry);
        log.debug("Registered JvmMemoryMetrics");
        return metrics;
    }

    @Bean
    public JvmGcMetrics jvmGcMetrics(MeterRegistry meterRegistry) {
        JvmGcMetrics metrics = new JvmGcMetrics();
        metrics.bindTo(meterRegistry);
        log.debug("Registered JvmGcMetrics");
        return metrics;
    }

    @Bean
    public JvmThreadMetrics jvmThreadMetrics(MeterRegistry meterRegistry) {
        JvmThreadMetrics metrics = new JvmThreadMetrics();
        metrics.bindTo(meterRegistry);
        log.debug("Registered JvmThreadMetrics");
        return metrics;
    }

    @Bean
    public ProcessorMetrics processorMetrics(MeterRegistry meterRegistry) {
        ProcessorMetrics metrics = new ProcessorMetrics();
        metrics.bindTo(meterRegistry);
        log.debug("Registered ProcessorMetrics");
        return metrics;
    }

    /**
     * Configure observation registry with custom handlers and filters.
     */
    @Bean
    @Primary
    public ObservationRegistry configureObservationRegistry(ObservationRegistry observationRegistry) {
        // Add custom observation handlers
        observationRegistry.observationConfig()
            .observationHandler(new CustomObservationHandler());

        // Add observation filters for context enhancement
        observationRegistry.observationConfig()
            .observationFilter((context) -> {
                // Add application context to all observations
                context.addLowCardinalityKeyValue(io.micrometer.common.KeyValue.of("application", applicationName));
                context.addLowCardinalityKeyValue(io.micrometer.common.KeyValue.of("version", applicationVersion));
                return context;
            });

        log.info("Configured ObservationRegistry with custom handlers and filters");
        return observationRegistry;
    }

    /**
     * Custom observation handler for FocusHive-specific business logic.
     */
    private static class CustomObservationHandler 
            implements io.micrometer.observation.ObservationHandler<io.micrometer.observation.Observation.Context> {
        
        @Override
        public void onStart(io.micrometer.observation.Observation.Context context) {
            if (log.isDebugEnabled()) {
                log.debug("Starting observation: {}", context.getName());
            }
        }
        
        @Override
        public void onError(io.micrometer.observation.Observation.Context context) {
            log.warn("Observation error: {} - {}", 
                    context.getName(), 
                    context.getError() != null ? context.getError().getMessage() : "Unknown error");
        }
        
        @Override
        public void onStop(io.micrometer.observation.Observation.Context context) {
            if (log.isDebugEnabled()) {
                log.debug("Completed observation: {}", context.getName());
            }
        }
        
        @Override
        public boolean supportsContext(io.micrometer.observation.Observation.Context context) {
            String name = context.getName();
            return name != null && (
                name.startsWith("focushive") || 
                name.startsWith("identity.service") ||
                name.startsWith("http.server.requests")
            );
        }
    }
}