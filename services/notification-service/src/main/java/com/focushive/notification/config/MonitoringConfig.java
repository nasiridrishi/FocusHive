package com.focushive.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuration class for monitoring and observability components.
 * Enables AOP for performance monitoring and configures additional monitoring beans.
 */
@Configuration
@EnableAspectJAutoProxy
public class MonitoringConfig {
    // TimedAspect bean is defined in MetricsConfig to avoid duplication
}