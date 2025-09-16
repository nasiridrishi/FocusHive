package com.focushive.notification.config;

import com.focushive.notification.annotation.ApiVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

/**
 * Configuration for API versioning strategy.
 * Supports both URI-based versioning (e.g., /api/v1/notifications) and header-based versioning.
 */
@Slf4j
@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {

    private static final String API_VERSION_PREFIX = "/api/v";
    private static final String DEFAULT_API_VERSION = "1";
    private static final String API_VERSION_HEADER = "X-API-Version";
    private static final String ACCEPT_VERSION_HEADER = "Accept-Version";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Configure path matching to support API versioning
        configurer.setPatternParser(null); // Use Ant-style path patterns for flexibility
    }

    @Bean
    public WebMvcRegistrations webMvcRegistrations() {
        return new WebMvcRegistrations() {
            @Override
            public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
                return new VersionedRequestMappingHandlerMapping();
            }
        };
    }

    /**
     * Custom request mapping handler that supports API versioning.
     */
    private static class VersionedRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

        @Override
        protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
            RequestMappingInfo info = super.getMappingForMethod(method, handlerType);

            if (info == null) {
                return null;
            }

            // Check for ApiVersion annotation
            ApiVersion methodVersion = AnnotationUtils.findAnnotation(method, ApiVersion.class);
            ApiVersion typeVersion = AnnotationUtils.findAnnotation(handlerType, ApiVersion.class);

            // Get the most specific version
            ApiVersion apiVersion = methodVersion != null ? methodVersion : typeVersion;

            if (apiVersion != null) {
                // Prepend version to the path
                String[] versions = apiVersion.value();
                PatternsRequestCondition patternsCondition = info.getPatternsCondition();

                if (patternsCondition != null) {
                    // Create versioned patterns
                    String[] patterns = patternsCondition.getPatterns().toArray(new String[0]);
                    String[] versionedPatterns = createVersionedPatterns(patterns, versions);

                    // Create new patterns condition with versioned paths
                    PatternsRequestCondition versionedPatternsCondition =
                        new PatternsRequestCondition(versionedPatterns);

                    // Create new request mapping info with versioned patterns
                    info = RequestMappingInfo
                        .paths(versionedPatterns)
                        .methods(info.getMethodsCondition().getMethods().toArray(new org.springframework.web.bind.annotation.RequestMethod[0]))
                        .params(info.getParamsCondition().getExpressions().stream()
                            .map(Object::toString)
                            .toArray(String[]::new))
                        .headers(info.getHeadersCondition().getExpressions().stream()
                            .map(Object::toString)
                            .toArray(String[]::new))
                        .consumes(info.getConsumesCondition().getExpressions().stream()
                            .map(Object::toString)
                            .toArray(String[]::new))
                        .produces(info.getProducesCondition().getExpressions().stream()
                            .map(Object::toString)
                            .toArray(String[]::new))
                        .build();
                }
            }

            return info;
        }

        /**
         * Create versioned patterns for the given paths and versions.
         */
        private String[] createVersionedPatterns(String[] patterns, String[] versions) {
            if (patterns == null || patterns.length == 0) {
                return patterns;
            }

            String[] versionedPatterns = new String[patterns.length * versions.length];
            int index = 0;

            for (String version : versions) {
                for (String pattern : patterns) {
                    // Check if pattern already starts with /api
                    if (pattern.startsWith("/api/")) {
                        // Replace /api/ with /api/vX/
                        versionedPatterns[index++] = pattern.replaceFirst("/api/", "/api/v" + version + "/");
                    } else {
                        // Prepend /api/vX to the pattern
                        versionedPatterns[index++] = "/api/v" + version + pattern;
                    }
                }
            }

            return versionedPatterns;
        }
    }
}