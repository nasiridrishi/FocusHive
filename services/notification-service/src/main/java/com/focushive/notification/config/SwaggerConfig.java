package com.focushive.notification.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for the Notification Service.
 * Provides interactive API documentation at /swagger-ui.html
 */
@Configuration
public class SwaggerConfig {

    @Value("${spring.application.name:notification-service}")
    private String applicationName;

    @Value("${server.port:8083}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(getServers())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(
                        new Components()
                                .addSecuritySchemes("bearerAuth",
                                        new SecurityScheme()
                                                .name("bearerAuth")
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description("JWT Bearer token for authentication")
                                )
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("FocusHive Notification Service API")
                .description("""
                        The Notification Service provides a centralized system for managing all notifications 
                        within the FocusHive platform. It handles email, push, and in-app notifications with 
                        support for user preferences, templates, and multi-language content.
                        
                        ## Key Features
                        - Event-driven notification creation via RabbitMQ
                        - REST API for notification management
                        - User preference management
                        - Email and push notification delivery
                        - Template-based notifications
                        - Multi-language support
                        
                        ## Integration
                        Services can send notifications by:
                        1. Publishing events to RabbitMQ (recommended)
                        2. Using REST API endpoints (requires JWT authentication)
                        
                        ## Authentication
                        All REST endpoints except health checks require JWT Bearer token authentication.
                        Include the token in the Authorization header: `Bearer <token>`
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("FocusHive Platform Team")
                        .email("platform@focushive.com")
                        .url("https://focushive.com"))
                .license(new License()
                        .name("Proprietary")
                        .url("https://focushive.com/license"));
    }

    private List<Server> getServers() {
        return List.of(
                new Server()
                        .url("http://localhost:" + serverPort)
                        .description("Local Development Server"),
                new Server()
                        .url("http://notification-service:8083")
                        .description("Docker Internal Network"),
                new Server()
                        .url("https://api.focushive.com/notifications")
                        .description("Production API Gateway")
        );
    }
}