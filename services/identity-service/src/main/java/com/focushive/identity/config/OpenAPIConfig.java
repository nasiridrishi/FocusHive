package com.focushive.identity.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * OpenAPI/Swagger documentation configuration for Identity Service.
 * Provides comprehensive API documentation for OAuth2/OIDC endpoints.
 */
@Configuration
public class OpenAPIConfig {

    @Value("${server.port:8081}")
    private String serverPort;

    @Value("${spring.application.name:identity-service}")
    private String applicationName;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(apiInfo())
            .servers(getServers())
            .tags(getTags())
            .components(new Components()
                .addSecuritySchemes("bearer-jwt", bearerJwtScheme())
                .addSecuritySchemes("oauth2", oauth2Scheme())
                .addSecuritySchemes("basic", basicAuthScheme())
            )
            .security(Arrays.asList(
                new SecurityRequirement().addList("bearer-jwt"),
                new SecurityRequirement().addList("oauth2", Arrays.asList("openid", "profile", "email"))
            ));
    }

    private Info apiInfo() {
        return new Info()
            .title("FocusHive Identity Service API")
            .version("1.0.0")
            .description("""
                # OAuth2/OpenID Connect Identity Service

                This service provides comprehensive identity management for the FocusHive platform:

                ## Features
                - **OAuth2 Authorization Server** with PKCE support
                - **OpenID Connect Provider** for SSO
                - **JWT Token Management** with RSA signatures
                - **Multi-Persona Support** for context switching
                - **Privacy Controls** with GDPR compliance
                - **Security Monitoring** with audit logging
                - **Rate Limiting** per client

                ## Authentication Methods
                1. **Bearer JWT**: Include JWT token in Authorization header
                2. **OAuth2**: Use authorization code flow with PKCE
                3. **Basic Auth**: For client credentials (OAuth2 clients only)

                ## Common Flows

                ### Authorization Code Flow with PKCE
                1. Generate code_verifier and code_challenge
                2. Redirect to `/oauth2/authorize` with code_challenge
                3. User authenticates and authorizes
                4. Receive authorization code
                5. Exchange code for tokens at `/oauth2/token` with code_verifier

                ### Client Credentials Flow
                1. Authenticate with client_id and client_secret
                2. Request token from `/oauth2/token` with grant_type=client_credentials
                3. Receive access token (no refresh token)

                ### Token Refresh Flow
                1. Use refresh_token at `/oauth2/token` with grant_type=refresh_token
                2. Receive new access and refresh tokens

                ## Rate Limits
                - **Authorization**: 10 requests per minute per client
                - **Token**: 60 requests per minute per client
                - **Introspection**: 100 requests per minute per client
                - **UserInfo**: 60 requests per minute per user

                ## Security Headers
                All responses include security headers for XSS, CSRF, and clickjacking protection.
                """)
            .contact(new Contact()
                .name("FocusHive Team")
                .email("support@focushive.com")
                .url("https://focushive.com"))
            .license(new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT"));
    }

    private List<Server> getServers() {
        return Arrays.asList(
            new Server()
                .url("http://localhost:" + serverPort)
                .description("Local Development Server"),
            new Server()
                .url("https://identity.focushive.com")
                .description("Production Identity Server"),
            new Server()
                .url("https://identity-staging.focushive.com")
                .description("Staging Identity Server")
        );
    }

    private List<Tag> getTags() {
        return Arrays.asList(
            new Tag()
                .name("OAuth2 Authorization")
                .description("OAuth2/OIDC authorization endpoints for authentication and token management"),
            new Tag()
                .name("Authentication")
                .description("User authentication endpoints including login, registration, and password management"),
            new Tag()
                .name("User Management")
                .description("User profile and account management endpoints"),
            new Tag()
                .name("Persona Management")
                .description("Multi-persona profile management for context switching"),
            new Tag()
                .name("Privacy Controls")
                .description("Privacy settings and data management endpoints for GDPR compliance"),
            new Tag()
                .name("OAuth2 Client Management")
                .description("OAuth2 client application registration and management"),
            new Tag()
                .name("Token Management")
                .description("JWT token validation, introspection, and revocation"),
            new Tag()
                .name("Security Monitoring")
                .description("Security audit logs and threat detection"),
            new Tag()
                .name("Admin")
                .description("Administrative endpoints for system management"),
            new Tag()
                .name("Metrics")
                .description("Prometheus metrics and health monitoring")
        );
    }

    private SecurityScheme bearerJwtScheme() {
        return new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("""
                JWT Bearer token authentication.

                Include the JWT token in the Authorization header:
                ```
                Authorization: Bearer <your-jwt-token>
                ```

                Tokens can be obtained through:
                - Login endpoint: POST /api/auth/login
                - OAuth2 token endpoint: POST /oauth2/token
                """);
    }

    private SecurityScheme oauth2Scheme() {
        return new SecurityScheme()
            .type(SecurityScheme.Type.OAUTH2)
            .description("OAuth2 with OpenID Connect")
            .flows(new OAuthFlows()
                .authorizationCode(new OAuthFlow()
                    .authorizationUrl("/oauth2/authorize")
                    .tokenUrl("/oauth2/token")
                    .refreshUrl("/oauth2/token")
                    .scopes(new Scopes()
                        .addString("openid", "OpenID Connect - basic user info")
                        .addString("profile", "User profile information")
                        .addString("email", "User email address")
                        .addString("phone", "User phone number")
                        .addString("address", "User address")
                        .addString("offline_access", "Refresh token for offline access")
                        .addString("api.read", "Read access to API resources")
                        .addString("api.write", "Write access to API resources")
                        .addString("admin", "Administrative access")
                    ))
                .clientCredentials(new OAuthFlow()
                    .tokenUrl("/oauth2/token")
                    .scopes(new Scopes()
                        .addString("api.read", "Read access to API resources")
                        .addString("api.write", "Write access to API resources")
                        .addString("admin", "Administrative access")
                    ))
                .password(new OAuthFlow()
                    .tokenUrl("/oauth2/token")
                    .refreshUrl("/oauth2/token")
                    .scopes(new Scopes()
                        .addString("openid", "OpenID Connect - basic user info")
                        .addString("profile", "User profile information")
                        .addString("email", "User email address")
                        .addString("offline_access", "Refresh token for offline access")
                        .addString("api.read", "Read access to API resources")
                        .addString("api.write", "Write access to API resources")
                    ))
            );
    }

    private SecurityScheme basicAuthScheme() {
        return new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("basic")
            .description("""
                HTTP Basic authentication for OAuth2 client credentials.

                Used for authenticating OAuth2 clients when requesting tokens.
                Encode client_id:client_secret in Base64:
                ```
                Authorization: Basic <base64(client_id:client_secret)>
                ```
                """);
    }
}