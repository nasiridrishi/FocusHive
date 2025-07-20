# FocusHive Identity Service

A Spring Boot 3.x microservice implementing advanced identity and profile management based on the CM3035 Advanced Web Design template requirements.

## Features

- **Multiple Personas per User**: Support for work, personal, gaming, and custom personas
- **OAuth2 Authorization Server**: Full OAuth2 provider capabilities using Spring Authorization Server
- **Context-Aware Identity Switching**: Automatic and manual persona switching
- **Advanced Privacy Controls**: Granular visibility and data sharing permissions
- **Data Portability**: Export and import user data per persona
- **JWT-based Authentication**: Secure token-based authentication with refresh tokens
- **OpenID Connect Support**: Standard OIDC compliance for third-party integrations

## Technology Stack

- **Java 21**: Latest LTS version
- **Spring Boot 3.3.1**: Core framework
- **Spring Authorization Server**: OAuth2/OIDC provider
- **PostgreSQL**: Primary database
- **Redis**: Caching and session management
- **Flyway**: Database migrations
- **Docker**: Containerization

## Getting Started

### Prerequisites

- Java 21 or higher
- Docker and Docker Compose
- PostgreSQL 15+ (or use Docker)
- Redis 7+ (or use Docker)

### Running Locally

1. **Build the project**:
   ```bash
   ./gradlew build
   ```

2. **Run tests**:
   ```bash
   ./gradlew test
   ```

3. **Run the application**:
   ```bash
   ./gradlew bootRun
   ```

### Running with Docker

From the project root directory:

```bash
docker-compose up identity-service
```

This will start the Identity Service along with its PostgreSQL and Redis dependencies.

## API Documentation

Once the service is running, you can access:

- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8081/api-docs
- **Health Check**: http://localhost:8081/actuator/health

## OAuth2 Endpoints

- **Authorization**: `/oauth2/authorize`
- **Token**: `/oauth2/token`
- **Token Introspection**: `/oauth2/introspect`
- **Token Revocation**: `/oauth2/revoke`
- **JWKS**: `/oauth2/jwks`
- **OpenID Discovery**: `/.well-known/openid-configuration`

## Default Configuration

### OAuth2 Client (FocusHive Backend)
- **Client ID**: `focushive-backend`
- **Client Secret**: `secret` (change in production!)
- **Redirect URI**: `http://localhost:8080/login/oauth2/code/identity`
- **Scopes**: `openid`, `profile`, `email`, `personas`, `identity.read`, `identity.write`

### Database
- **Host**: `localhost` (or `identity-db` in Docker)
- **Port**: `5433`
- **Database**: `identity_db`
- **Username**: `identity_user`
- **Password**: `identity_pass`

### Redis
- **Host**: `localhost` (or `identity-redis` in Docker)
- **Port**: `6380`
- **Password**: `identity_redis_pass`

## Development

### Project Structure

```
identity-service/
├── src/main/java/com/focushive/identity/
│   ├── config/         # Configuration classes
│   ├── controller/     # REST controllers
│   ├── dto/           # Data transfer objects
│   ├── entity/        # JPA entities
│   ├── repository/    # Data repositories
│   ├── service/       # Business logic
│   ├── security/      # Security components
│   └── exception/     # Custom exceptions
├── src/main/resources/
│   ├── application.yml # Main configuration
│   └── db/migration/  # Flyway migrations
└── src/test/         # Test files
```

### Adding New Features

1. Create database migrations in `src/main/resources/db/migration/`
2. Define entities in the `entity` package
3. Create repositories in the `repository` package
4. Implement business logic in the `service` package
5. Expose APIs through controllers in the `controller` package
6. Write tests for all new functionality

## Security Considerations

- All endpoints except health and auth endpoints require authentication
- JWT tokens are signed with RSA keys (auto-generated in development)
- CORS is configured for local development origins
- Use proper key management in production
- Enable HTTPS in production deployments

## Monitoring

- **Health Check**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Prometheus**: `/actuator/prometheus`

## License

This project is part of the FocusHive platform developed for the University of London BSc Computer Science final project.