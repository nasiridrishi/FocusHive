package com.focushive.notification.monitoring;

import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Aspect for monitoring performance of critical methods.
 * Automatically tracks execution times and integrates with metrics.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class PerformanceMonitoringAspect {

    private final NotificationMetricsService metricsService;
    private final CorrelationIdService correlationIdService;

    @Around("execution(* com.focushive.notification.service.NotificationServiceImpl.createNotification(..))")
    public Object monitorNotificationCreation(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = metricsService.startNotificationProcessingTimer();
        String correlationId = correlationIdService.getCorrelationId();
        
        log.debug("Starting notification creation monitoring [{}]", correlationId);
        
        try {
            Object result = joinPoint.proceed();
            metricsService.recordNotificationProcessingTime(sample);
            log.debug("Notification creation completed successfully [{}]", correlationId);
            return result;
        } catch (Exception e) {
            metricsService.recordNotificationProcessingTime(sample);
            log.error("Notification creation failed [{}]: {}", correlationId, e.getMessage());
            throw e;
        }
    }
    
    @Around("execution(* com.focushive.notification.service.EmailNotificationService.sendEmail(..))")
    public Object monitorEmailDelivery(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = metricsService.startEmailDeliveryTimer();
        String correlationId = correlationIdService.getCorrelationId();
        
        log.debug("Starting email delivery monitoring [{}]", correlationId);
        
        try {
            Object result = joinPoint.proceed();
            metricsService.recordEmailDeliveryTime(sample);
            metricsService.incrementEmailsSent();
            log.debug("Email delivery completed successfully [{}]", correlationId);
            return result;
        } catch (Exception e) {
            metricsService.recordEmailDeliveryTime(sample);
            metricsService.incrementEmailsFailed();
            log.error("Email delivery failed [{}]: {}", correlationId, e.getMessage());
            throw e;
        }
    }
    
    @Around("execution(* com.focushive.notification.service.NotificationTemplateService.processTemplate(..))")
    public Object monitorTemplateRendering(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = metricsService.startTemplateRenderingTimer();
        String correlationId = correlationIdService.getCorrelationId();
        
        log.debug("Starting template rendering monitoring [{}]", correlationId);
        
        try {
            Object result = joinPoint.proceed();
            metricsService.recordTemplateRenderingTime(sample);
            metricsService.incrementTemplatesRendered();
            log.debug("Template rendering completed successfully [{}]", correlationId);
            return result;
        } catch (Exception e) {
            metricsService.recordTemplateRenderingTime(sample);
            metricsService.incrementTemplatesRenderedFailed();
            log.error("Template rendering failed [{}]: {}", correlationId, e.getMessage());
            throw e;
        }
    }
    
    @Around("execution(* com.focushive.notification.service.NotificationDigestService.processDigestForUser(..))")
    public Object monitorDigestProcessing(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = metricsService.startDigestProcessingTimer();
        String correlationId = correlationIdService.getCorrelationId();
        String userId = null;
        
        // Extract userId from method arguments
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof String) {
            userId = (String) args[0];
            correlationIdService.setUserId(userId);
        }
        
        log.info("Starting digest processing monitoring for user {} [{}]", userId, correlationId);
        
        try {
            Object result = joinPoint.proceed();
            metricsService.recordDigestProcessingTime(sample);
            metricsService.incrementDigestsProcessed();
            log.info("Digest processing completed successfully for user {} [{}]", userId, correlationId);
            return result;
        } catch (Exception e) {
            metricsService.recordDigestProcessingTime(sample);
            log.error("Digest processing failed for user {} [{}]: {}", userId, correlationId, e.getMessage());
            throw e;
        }
    }
    
    @Around("execution(* com.focushive.notification.service.NotificationPreferenceService.updatePreference(..))")
    public Object monitorPreferenceUpdates(ProceedingJoinPoint joinPoint) throws Throwable {
        String correlationId = correlationIdService.getCorrelationId();
        
        log.debug("Starting preference update monitoring [{}]", correlationId);
        
        try {
            Object result = joinPoint.proceed();
            metricsService.incrementPreferencesUpdated();
            log.debug("Preference update completed successfully [{}]", correlationId);
            return result;
        } catch (Exception e) {
            log.error("Preference update failed [{}]: {}", correlationId, e.getMessage());
            throw e;
        }
    }
    
    @Around("@annotation(MonitorPerformance)")
    public Object monitorAnnotatedMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String correlationId = correlationIdService.getCorrelationId();
        
        long startTime = System.currentTimeMillis();
        log.debug("Starting performance monitoring for {}.{} [{}]", className, methodName, correlationId);
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Method {}.{} completed in {}ms [{}]", className, methodName, executionTime, correlationId);
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Method {}.{} failed after {}ms [{}]: {}", className, methodName, executionTime, correlationId, e.getMessage());
            throw e;
        }
    }
}