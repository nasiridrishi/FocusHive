package com.focushive.buddy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Configuration to properly handle actuator endpoints and prevent them
 * from being treated as static resources when accessed on the main application port.
 *
 * This fixes the issue where /actuator/health returns NoResourceFoundException
 * when accessed on port 8087 instead of the management port 8088.
 */
@Configuration
public class ActuatorMvcConfig implements WebMvcConfigurer {

    /**
     * Configure resource handling to explicitly exclude actuator paths.
     * This prevents Spring from treating /actuator/* as static resources.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Don't add a catch-all resource handler that might interfere with controllers
        // Only add specific resource handlers for known static resource paths
        registry.addResourceHandler("/static/**")
            .addResourceLocations("classpath:/static/");

        registry.addResourceHandler("/public/**")
            .addResourceLocations("classpath:/public/");

        // Swagger UI resources
        registry.addResourceHandler("/swagger-ui/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/springdoc-openapi-ui/");
    }


    /**
     * Configure path matching to ensure actuator paths are not treated as suffixed patterns.
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Ensure exact path matching for actuator endpoints
        configurer.setUseTrailingSlashMatch(false);
        configurer.setUseSuffixPatternMatch(false);
    }
}