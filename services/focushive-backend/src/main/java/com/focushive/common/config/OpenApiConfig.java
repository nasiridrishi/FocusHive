package com.focushive.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI focusHiveOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FocusHive API")
                        .description("""
                            # FocusHive API Documentation

                            Welcome to the FocusHive API - a comprehensive digital co-working and co-studying platform.

                            ## Features
                            - **Hive Management**: Create and manage collaborative workspaces
                            - **Real-time Presence**: Track user presence and focus sessions
                            - **Focus Timer**: Pomodoro-style productivity tracking
                            - **Chat System**: Real-time messaging within hives
                            - **Analytics**: Productivity insights and gamification
                            - **Forum**: Community discussions and knowledge sharing
                            - **Buddy System**: Accountability partners and peer support
                            - **Notifications**: Multi-channel notification delivery

                            ## Authentication
                            Most endpoints require JWT authentication. Obtain a token from the identity service
                            and include it in the Authorization header: `Authorization: Bearer <your-token>`

                            ## WebSocket Support
                            Real-time features use WebSocket connections at `/ws` endpoint.

                            ## Rate Limiting
                            API requests are rate-limited to ensure fair usage and system stability.
                            """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("FocusHive Development Team")
                                .email("support@focushive.com")
                                .url("https://github.com/focushive"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server"),
                        new Server()
                                .url("http://localhost:8081")
                                .description("Local development server (alternative port)"),
                        new Server()
                                .url("https://api.focushive.com")
                                .description("Production server"),
                        new Server()
                                .url("https://staging-api.focushive.com")
                                .description("Staging server")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT authentication token required for most endpoints. " +
                                           "Example: Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")));
    }
}