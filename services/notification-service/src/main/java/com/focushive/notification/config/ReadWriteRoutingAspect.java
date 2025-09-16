package com.focushive.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

/**
 * Aspect for automatic read/write routing based on method patterns.
 * Routes read queries to read replicas and write operations to master.
 */
@Slf4j
@Aspect
@Component
@Order(0) // Execute before @Transactional
@ConditionalOnProperty(name = "notification.datasource.read-write-splitting.enabled", havingValue = "true")
public class ReadWriteRoutingAspect {

    /**
     * Route repository read methods to read data source.
     */
    @Around("@within(org.springframework.stereotype.Repository)")
    public Object routeDataSource(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        boolean isReadOnly = isReadMethod(methodName);

        // Check if method has @Transactional(readOnly = false)
        Method method = getMethod(joinPoint);
        if (method != null) {
            Transactional transactional = method.getAnnotation(Transactional.class);
            if (transactional != null && !transactional.readOnly()) {
                isReadOnly = false;
            }
        }

        ReadWriteDataSourceConfig.DataSourceType previousType =
            ReadWriteDataSourceConfig.DataSourceContextHolder.getDataSourceType();

        try {
            if (isReadOnly) {
                log.trace("Routing to READ data source for method: {}", methodName);
                ReadWriteDataSourceConfig.DataSourceContextHolder.setDataSourceType(
                    ReadWriteDataSourceConfig.DataSourceType.READ);
            } else {
                log.trace("Routing to WRITE data source for method: {}", methodName);
                ReadWriteDataSourceConfig.DataSourceContextHolder.setDataSourceType(
                    ReadWriteDataSourceConfig.DataSourceType.WRITE);
            }

            return joinPoint.proceed();

        } finally {
            // Restore previous context
            if (previousType != null) {
                ReadWriteDataSourceConfig.DataSourceContextHolder.setDataSourceType(previousType);
            } else {
                ReadWriteDataSourceConfig.DataSourceContextHolder.clearDataSourceType();
            }
        }
    }

    /**
     * Determine if a method is a read operation based on naming convention.
     */
    private boolean isReadMethod(String methodName) {
        return methodName.startsWith("find") ||
               methodName.startsWith("get") ||
               methodName.startsWith("query") ||
               methodName.startsWith("select") ||
               methodName.startsWith("count") ||
               methodName.startsWith("exists") ||
               methodName.startsWith("search") ||
               methodName.startsWith("load") ||
               methodName.startsWith("fetch");
    }

    /**
     * Get method from join point.
     */
    private Method getMethod(ProceedingJoinPoint joinPoint) {
        try {
            String methodName = joinPoint.getSignature().getName();
            Class<?>[] paramTypes = new Class[joinPoint.getArgs().length];
            for (int i = 0; i < joinPoint.getArgs().length; i++) {
                Object arg = joinPoint.getArgs()[i];
                paramTypes[i] = arg != null ? arg.getClass() : Object.class;
            }
            return joinPoint.getTarget().getClass().getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}