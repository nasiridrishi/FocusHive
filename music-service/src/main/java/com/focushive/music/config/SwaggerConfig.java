package com.focushive.music.config;

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
 * OpenAPI (Swagger) configuration for the Music Service.
 * 
 * Provides comprehensive API documentation with security schemes,
 * endpoints documentation, and interactive testing capabilities.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Configuration
public class SwaggerConfig {

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${server.port:8084}")
    private String serverPort;

    /**
     * Configures the OpenAPI specification for the Music Service.
     * 
     * @return Configured OpenAPI instance
     */
    @Bean
    public OpenAPI musicServiceOpenAPI() {
        return new OpenAPI()
            .info(apiInfo())
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Development server"),
                new Server()
                    .url("https://api.focushive.com")
                    .description("Production server")
            ))
            .addSecurityItem(new SecurityRequirement()
                .addList("JWT Authentication"))
            .components(new Components()
                .addSecuritySchemes("JWT Authentication", createJWTScheme())
            );
    }

    /**
     * Creates API information metadata.
     * 
     * @return API info configuration
     */
    private Info apiInfo() {
        return new Info()
            .title("FocusHive Music Service API")
            .description("""
                **FocusHive Music Service** provides comprehensive music management capabilities for the FocusHive platform.
                
                ## Features
                - **Music Recommendations**: Personalized music suggestions based on user preferences and hive context
                - **Spotify Integration**: Seamless integration with Spotify for streaming and playlist management  
                - **Collaborative Playlists**: Multi-user playlist creation and management for hives
                - **Music Sessions**: Real-time music session tracking and synchronization
                - **Analytics**: Music listening insights and productivity correlations
                
                ## Authentication
                All endpoints (except public ones) require JWT authentication obtained from the Identity Service.
                
                ## Rate Limiting
                API endpoints are rate-limited to ensure fair usage and system stability.
                
                ## Support
                For technical support or questions about the API, contact the development team.
                """)
            .version(appVersion)
            .contact(new Contact()
                .name("FocusHive Development Team")
                .url("https://github.com/focushive/music-service")
                .email("dev@focushive.com"))
            .license(new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT"));
    }

    /**
     * Creates JWT security scheme for API authentication.
     * 
     * @return JWT SecurityScheme configuration
     */
    private SecurityScheme createJWTScheme() {
        return new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("""
                JWT token obtained from the FocusHive Identity Service.
                
                **Format**: `Bearer <your-jwt-token>`
                
                **Example**: `Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...`
                
                To obtain a token:
                1. Authenticate with the Identity Service
                2. Use the returned JWT token in the Authorization header
                3. Token expires after a configured period and must be refreshed
                """);
    }
}