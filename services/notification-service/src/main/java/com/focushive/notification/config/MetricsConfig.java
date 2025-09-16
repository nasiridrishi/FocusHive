package com.focushive.notification.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.db.DatabaseTableMetrics;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Collections;

/**
 * Configuration for Micrometer metrics.
 * Sets up custom metrics, JVM metrics, and database metrics.
 */
@Slf4j
@Configuration
@EnableAspectJAutoProxy
public class MetricsConfig {

    /**
     * Customize the meter registry with application-specific tags.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            registry.config()
                .commonTags(
                    "application", "notification-service",
                    "environment", System.getProperty("spring.profiles.active", "default"),
                    "region", System.getenv().getOrDefault("AWS_REGION", "us-east-1")
                );

            // Add meter filters
            registry.config()
                .meterFilter(MeterFilter.deny(id -> {
                    String uri = id.getTag("uri");
                    return uri != null && (
                        uri.startsWith("/actuator") ||
                        uri.startsWith("/health") ||
                        uri.equals("/")
                    );
                }))
                .meterFilter(MeterFilter.maximumAllowableMetrics(10000));

            log.info("Configured meter registry with common tags and filters");
        };
    }

    /**
     * Enable @Timed annotation support for method-level metrics.
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        log.info("Enabling @Timed annotation support");
        return new TimedAspect(registry);
    }

    /**
     * JVM metrics configuration.
     */
    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics() {
        return new JvmMemoryMetrics();
    }

    @Bean
    public JvmGcMetrics jvmGcMetrics() {
        return new JvmGcMetrics();
    }

    @Bean
    public JvmThreadMetrics jvmThreadMetrics() {
        return new JvmThreadMetrics();
    }

    @Bean
    public ClassLoaderMetrics classLoaderMetrics() {
        return new ClassLoaderMetrics();
    }

    /**
     * System metrics configuration.
     */
    @Bean
    public ProcessorMetrics processorMetrics() {
        return new ProcessorMetrics();
    }

    /**
     * Database metrics configuration.
     */
    @Bean
    public DatabaseTableMetrics databaseTableMetrics(DataSource dataSource, MeterRegistry registry) {
        Collection<String> tables = Collections.singletonList("notifications");

        // Create metrics for each table
        tables.forEach(table -> {
            DatabaseTableMetrics tableMetric = new DatabaseTableMetrics(
                dataSource,
                "notification_service",
                table,
                Collections.emptyList()
            );
            tableMetric.bindTo(registry);
        });

        log.info("Configured database table metrics for tables: {}", tables);

        // Return one of the metrics for bean registration
        return new DatabaseTableMetrics(
            dataSource,
            "notification_service",
            "notifications",
            Collections.emptyList()
        );
    }

    /**
     * Custom metric filters for performance optimization.
     */
    @Bean
    public MeterFilter customMeterFilter() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if (id.getName().startsWith("http.server.requests")) {
                    return DistributionStatisticConfig.builder()
                        .percentilesHistogram(true)
                        .percentiles(0.5, 0.75, 0.95, 0.99)
                        .serviceLevelObjectives(
                            toNanos(50),   // 50ms
                            toNanos(100),  // 100ms
                            toNanos(250),  // 250ms
                            toNanos(500),  // 500ms
                            toNanos(1000), // 1s
                            toNanos(2000)  // 2s
                        )
                        .minimumExpectedValue(toNanos(1))
                        .maximumExpectedValue(toNanos(30000))
                        .build()
                        .merge(config);
                }

                if (id.getName().startsWith("notification.delivery.time")) {
                    return DistributionStatisticConfig.builder()
                        .percentilesHistogram(true)
                        .percentiles(0.5, 0.75, 0.95, 0.99)
                        .serviceLevelObjectives(
                            toNanos(100),  // 100ms
                            toNanos(500),  // 500ms
                            toNanos(1000), // 1s
                            toNanos(5000)  // 5s
                        )
                        .minimumExpectedValue(toNanos(10))
                        .maximumExpectedValue(toNanos(60000))
                        .build()
                        .merge(config);
                }

                return config;
            }

            private long toNanos(long millis) {
                return millis * 1_000_000L;
            }
        };
    }

    /**
     * Custom health indicator based on metrics.
     */
    @Bean
    public MetricsHealthIndicator metricsHealthIndicator(MeterRegistry registry) {
        return new MetricsHealthIndicator(registry);
    }

    /**
     * Health indicator that uses metrics to determine health status.
     */
    public static class MetricsHealthIndicator implements org.springframework.boot.actuate.health.HealthIndicator {

        private final MeterRegistry meterRegistry;

        public MetricsHealthIndicator(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
        }

        @Override
        public org.springframework.boot.actuate.health.Health health() {
            // Check error rate
            double errorRate = calculateErrorRate();
            if (errorRate > 0.5) { // More than 50% errors
                return org.springframework.boot.actuate.health.Health.down()
                    .withDetail("errorRate", String.format("%.2f%%", errorRate * 100))
                    .withDetail("message", "High error rate detected")
                    .build();
            }

            // Check queue depth
            double queueDepth = getQueueDepth();
            if (queueDepth > 1000) {
                return org.springframework.boot.actuate.health.Health.outOfService()
                    .withDetail("queueDepth", queueDepth)
                    .withDetail("message", "Email queue is backing up")
                    .build();
            }

            return org.springframework.boot.actuate.health.Health.up()
                .withDetail("errorRate", String.format("%.2f%%", errorRate * 100))
                .withDetail("queueDepth", queueDepth)
                .build();
        }

        private double calculateErrorRate() {
            try {
                double sent = meterRegistry.counter("notification.sent.total").count();
                double failed = meterRegistry.counter("notification.failed.total").count();
                double total = sent + failed;
                return total > 0 ? failed / total : 0.0;
            } catch (Exception e) {
                return 0.0;
            }
        }

        private double getQueueDepth() {
            try {
                return meterRegistry.gauge("email.queue.depth", 0.0);
            } catch (Exception e) {
                return 0.0;
            }
        }
    }
}