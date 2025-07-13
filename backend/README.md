# FocusHive Backend

## Overview

FocusHive backend is built as a modular monolith using Spring Boot 3.x and Java 21. The architecture follows Domain-Driven Design principles with clear service boundaries, allowing for future extraction into microservices if needed.

## Architecture

### Services

1. **User Service** (`com.focushive.user`)
   - Authentication and authorization
   - User profile management
   - JWT token handling
   - OAuth2 integration

2. **Hive Service** (`com.focushive.hive`)
   - Virtual co-working space management
   - Member management
   - Access control

3. **Presence Service** (`com.focushive.presence`)
   - Real-time status tracking
   - WebSocket connections
   - Activity monitoring

4. **Analytics Service** (`com.focushive.analytics`)
   - Productivity tracking
   - Session management
   - Insights and reporting

### Technology Stack

- **Framework**: Spring Boot 3.x
- **Language**: Java 21
- **Database**: PostgreSQL 16
- **Cache**: Redis 7
- **WebSockets**: Spring WebSocket with STOMP
- **API Documentation**: SpringDoc OpenAPI
- **Build Tool**: Gradle

## Getting Started

### Prerequisites

- Java 21
- Docker and Docker Compose
- Gradle 8.x

### Running Locally

1. Start the infrastructure services:
```bash
docker-compose up -d
```

2. Run the application:
```bash
./gradlew bootRun
```

3. Access the API documentation:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs

### Running Tests

```bash
./gradlew test
```

## API Endpoints

### Authentication
- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/login` - Login user
- `POST /api/v1/auth/refresh` - Refresh JWT token

### Hives
- `POST /api/v1/hives` - Create new hive
- `GET /api/v1/hives/{id}` - Get hive details
- `GET /api/v1/hives` - List hives
- `POST /api/v1/hives/{id}/join` - Join hive
- `POST /api/v1/hives/{id}/leave` - Leave hive
- `GET /api/v1/hives/{id}/members` - Get hive members

### Analytics
- `POST /api/v1/analytics/sessions/start` - Start focus session
- `POST /api/v1/analytics/sessions/{id}/end` - End focus session
- `GET /api/v1/analytics/users/{id}/stats` - Get user statistics
- `GET /api/v1/analytics/hives/{id}/leaderboard` - Get hive leaderboard

### WebSocket Endpoints
- `/ws` - WebSocket connection endpoint
- STOMP destinations:
  - `/app/presence/update` - Update user presence
  - `/topic/hive/{id}/presence` - Subscribe to hive presence updates

## Development

### Project Structure

```
src/main/java/com/focushive/
├── common/          # Shared components
├── user/           # User service
├── hive/           # Hive service
├── presence/       # Presence service
├── analytics/      # Analytics service
└── events/         # Event system
```

### Adding New Features

1. Create entities in the appropriate service package
2. Define DTOs for request/response
3. Implement repository interfaces
4. Create service classes with business logic
5. Add controllers with OpenAPI documentation
6. Write tests for all layers

## Configuration

Configuration is managed through `application.yml` with environment variable overrides:

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` - PostgreSQL settings
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` - Redis settings
- `JWT_SECRET`, `JWT_EXPIRATION` - JWT configuration
- `SERVER_PORT` - Application port

## Monitoring

- Health check: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Info: `GET /actuator/info`