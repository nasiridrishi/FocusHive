package com.focushive.api.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Response;
import feign.codec.Decoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * Comprehensive logging interceptor for Feign client requests and responses.
 * Provides detailed logging for debugging inter-service communication.
 */
@Slf4j
@Configuration
@Profile("!test")
public class FeignLoggingInterceptor {
    
    /**
     * Request logging interceptor.
     */
    @Bean
    public RequestInterceptor feignRequestLoggingInterceptor() {
        return new RequestLoggingInterceptor();
    }
    
    /**
     * Response logging decoder wrapper.
     */
    @Bean
    public Decoder feignResponseLoggingDecoder() {
        return new ResponseLoggingDecoder();
    }
    
    /**
     * Logs all outbound requests to Identity Service.
     */
    @Slf4j
    static class RequestLoggingInterceptor implements RequestInterceptor {
        
        @Override
        public void apply(RequestTemplate template) {
            String correlationId = extractCorrelationId(template);
            
            log.info("[{}] Outbound Request: {} {}", 
                    correlationId, template.method(), template.url());
            
            if (log.isDebugEnabled()) {
                // Log headers (excluding sensitive ones)
                template.headers().forEach((name, values) -> {
                    if (!isSensitiveHeader(name)) {
                        log.debug("[{}] Request Header: {} = {}", 
                                correlationId, name, values);
                    } else {
                        log.debug("[{}] Request Header: {} = [HIDDEN]", 
                                correlationId, name);
                    }
                });
                
                // Log request body if present
                if (template.body() != null) {
                    String body = new String(template.body());
                    if (body.length() > 1000) {
                        log.debug("[{}] Request Body: {}... (truncated)", 
                                correlationId, body.substring(0, 1000));
                    } else {
                        log.debug("[{}] Request Body: {}", correlationId, body);
                    }
                }
            }
        }
        
        private String extractCorrelationId(RequestTemplate template) {
            Collection<String> correlationIds = template.headers().get("X-Correlation-ID");
            return correlationIds != null && !correlationIds.isEmpty() 
                    ? correlationIds.iterator().next() 
                    : "unknown";
        }
        
        private boolean isSensitiveHeader(String headerName) {
            String lowerCaseName = headerName.toLowerCase();
            return lowerCaseName.contains("authorization") || 
                   lowerCaseName.contains("cookie") || 
                   lowerCaseName.contains("token") ||
                   lowerCaseName.contains("api-key");
        }
    }
    
    /**
     * Logs all inbound responses from Identity Service.
     */
    @Slf4j
    @RequiredArgsConstructor
    static class ResponseLoggingDecoder implements Decoder {
        
        private final Decoder delegate = new feign.codec.Decoder.Default();
        
        @Override
        public Object decode(Response response, Type type) throws IOException {
            String correlationId = extractCorrelationId(response);
            long startTime = extractStartTime(response);
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("[{}] Inbound Response: {} {} - Status: {} - Duration: {}ms",
                    correlationId,
                    response.request().httpMethod(),
                    response.request().url(),
                    response.status(),
                    duration);
            
            if (log.isDebugEnabled()) {
                // Log response headers
                response.headers().forEach((name, values) -> {
                    if (!isSensitiveHeader(name)) {
                        log.debug("[{}] Response Header: {} = {}", 
                                correlationId, name, values);
                    } else {
                        log.debug("[{}] Response Header: {} = [HIDDEN]", 
                                correlationId, name);
                    }
                });
            }
            
            // Log performance metrics
            if (duration > 3000) {
                log.warn("[{}] Slow response detected: {}ms", correlationId, duration);
            }
            
            return delegate.decode(response, type);
        }
        
        private String extractCorrelationId(Response response) {
            Collection<String> correlationIds = response.headers().get("X-Correlation-ID");
            if (correlationIds != null && !correlationIds.isEmpty()) {
                return correlationIds.iterator().next();
            }
            
            // Fallback to request headers
            correlationIds = response.request().headers().get("X-Correlation-ID");
            return correlationIds != null && !correlationIds.isEmpty() 
                    ? correlationIds.iterator().next() 
                    : "unknown";
        }
        
        private long extractStartTime(Response response) {
            Collection<String> timestamps = response.request().headers().get("X-Request-Timestamp");
            if (timestamps != null && !timestamps.isEmpty()) {
                try {
                    return Long.parseLong(timestamps.iterator().next());
                } catch (NumberFormatException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Invalid timestamp format in request header");
                    }
                }
            }
            return System.currentTimeMillis();
        }
        
        private boolean isSensitiveHeader(String headerName) {
            String lowerCaseName = headerName.toLowerCase();
            return lowerCaseName.contains("authorization") || 
                   lowerCaseName.contains("cookie") || 
                   lowerCaseName.contains("token") ||
                   lowerCaseName.contains("api-key");
        }
    }
}