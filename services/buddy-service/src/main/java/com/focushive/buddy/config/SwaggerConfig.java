package com.focushive.buddy.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI configuration for Buddy Service.
 * Provides interactive API documentation and testing interface.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FocusHive Buddy Service API")
                        .version("1.0.0")
                        .description("API documentation for FocusHive Buddy Service - Accountability partner management system")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                .servers(List.of(
                        new Server().url("http://localhost:8087").description("Local Development Server"),
                        new Server().url("http://focushive_buddy_service_app:8087").description("Docker Container")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT authentication token. Use format: Bearer {token}")
                        ));
    }
}