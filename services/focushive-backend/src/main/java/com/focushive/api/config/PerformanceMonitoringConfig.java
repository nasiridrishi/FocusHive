package com.focushive.api.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Slf4j
@Profile("!test") // Don't load in test profile
public class PerformanceMonitoringConfig {

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            registry.config().commonTags(
                "application", "focushive-backend",
                "version", "1.0.0",
                "environment", "development"
            );

            // Add JVM metrics
            new JvmMemoryMetrics().bindTo(registry);
            new JvmGcMetrics().bindTo(registry);
            new JvmThreadMetrics().bindTo(registry);
            new ClassLoaderMetrics().bindTo(registry);

            // Add system metrics
            new ProcessorMetrics().bindTo(registry);
            new UptimeMetrics().bindTo(registry);

            log.info("Configured performance monitoring with common tags and JVM metrics");
        };
    }

    @Bean
    public Timer.Builder customTimerBuilder() {
        return Timer.builder("focushive.custom.timer")
            .description("Custom timer for FocusHive operations")
            .minimumExpectedValue(java.time.Duration.ofMillis(1))
            .maximumExpectedValue(java.time.Duration.ofSeconds(10));
    }
}