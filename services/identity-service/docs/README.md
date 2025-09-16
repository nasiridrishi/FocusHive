# FocusHive Identity Service

> A production-ready Spring Boot 3.x microservice implementing OAuth2 authorization server, JWT-based authentication, and advanced identity management for the FocusHive platform.

## 🚀 Quick Start

### Prerequisites
- Java 21+
- PostgreSQL 15+
- Redis 7+
- Docker (optional, for containerized dependencies)

### Running the Service

```bash
# Clone the repository
git clone https://github.com/nasiridrishi/focushive-identity-service.git
cd focushive-identity-service

# Run with local profile (uses OrbStack domains for inter-service communication)
./gradlew bootRun --args='--spring.profiles.active=local'

# Or build and run JAR
./gradlew bootJar
java -jar build/libs/identity-service.jar
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run unit tests only
./gradlew unitTest

# Run with coverage report
./gradlew jacocoTestReport
# Report: build/reports/jacoco/test/html/index.html
```

## 🏗️ Architecture Overview

### Service Details
- **Port**: 8081
- **Context Path**: `/identity`
- **API Version**: v1
- **Documentation**: http://localhost:8081/swagger-ui.html

### Technology Stack
- **Framework**: Spring Boot 3.3.1 with Spring Security 6
- **Authorization**: Spring Authorization Server 1.3.1 (OAuth2 Provider)
- **Database**: PostgreSQL 15 with Flyway migrations
- **Caching**: Redis with Jedis client
- **Message Queue**: RabbitMQ (for async operations)
- **Rate Limiting**: Bucket4j with Redis backend
- **Monitoring**: Micrometer + Prometheus + Zipkin
- **Testing**: JUnit 5, TestContainers, Mockito

### Key Components

```
identity-service/
├── OAuth2 Authorization Server (RFC 6749, 6750, 7636)
├── JWT Token Management (RS256/HS256)
├── Multi-Persona Profile System
├── Privacy Controls & GDPR Compliance
├── Rate Limiting & DDoS Protection
├── Notification Service Integration
└── Comprehensive Security Features
```

## 🔐 Core Features

### Authentication & Authorization
- **OAuth2 Flows**: Authorization Code (with PKCE), Client Credentials, Refresh Token
- **JWT Tokens**: Access tokens (1hr), Refresh tokens (30d) with rotation
- **OpenID Connect**: Full OIDC provider with discovery endpoint
- **Two-Factor Authentication**: TOTP-based 2FA support
- **Account Security**: Brute force protection, account lockout, IP geolocation

### Identity Management
- **Multi-Persona Profiles**: Work, Study, Personal contexts with instant switching
- **Privacy Controls**: Granular consent management, data portability (GDPR)
- **User Management**: Registration, email verification, password reset
- **Session Management**: Concurrent session control, device tracking

### Integration Features
- **Microservice Communication**: OpenFeign with circuit breakers
- **Notification Service**: Integrated email/SMS/push notifications
- **Real-time Updates**: WebSocket support for instant notifications
- **API Rate Limiting**: Per-user and per-endpoint limits

## 📡 API Endpoints

### Public Endpoints
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/refresh` - Refresh access token
- `POST /api/auth/forgot-password` - Request password reset

### OAuth2 Endpoints
- `GET /oauth2/authorize` - Authorization endpoint
- `POST /oauth2/token` - Token endpoint
- `POST /oauth2/revoke` - Token revocation
- `GET /.well-known/openid-configuration` - OIDC discovery

### Protected Endpoints
- `GET /api/users/profile` - Get user profile
- `GET /api/users/personas` - Manage personas
- `GET /api/privacy/settings` - Privacy controls

[Full API documentation →](./TECHNICAL_DOCUMENTATION.md#api-reference)

## 🧪 Testing

### Test Coverage
- **Overall Coverage**: 80%+ (Target: 85%)
- **Unit Tests**: 232 test files
- **Integration Tests**: Full OAuth2 flows, API endpoints
- **Performance Tests**: Load testing, stress testing
- **Security Tests**: Penetration testing, vulnerability scanning

### Test Categories
- **Unit Tests**: Service layer, utilities, validators
- **Integration Tests**: API endpoints, OAuth2 flows, database
- **Performance Tests**: Throughput, latency, scalability
- **Resilience Tests**: Circuit breakers, fallbacks, recovery
- **Security Tests**: Authentication, authorization, input validation

[Full test documentation →](./TEST_COVERAGE.md)

## 🔄 Microservice Integration

### Notification Service Integration
The identity service integrates with the notification service for all email/SMS operations:

```java
// Uses OpenFeign with circuit breaker pattern
@FeignClient(name = "notification-service",
             url = "http://focushive-notification-service-app.orb.local:8083")
```

Features:
- Graceful degradation with fallback
- Service-to-service authentication
- Distributed tracing with correlation IDs
- Circuit breaker protection

### OrbStack Networking
For local development with OrbStack, services are accessible via `.orb.local` domains:
- Identity Service: `localhost:8081`
- Notification Service: `focushive-notification-service-app.orb.local:8083`

## 🚢 Deployment

### Docker Support
```bash
# Build Docker image
docker build -t focushive/identity-service .

# Run with Docker Compose
docker-compose up identity-service
```

### Environment Variables
```bash
# Required
DATABASE_URL=jdbc:postgresql://localhost:5432/identity_db
DATABASE_USERNAME=identity_user
DATABASE_PASSWORD=secure_password
JWT_SECRET=your-256-bit-secret
REDIS_PASSWORD=redis_password

# Optional
SPRING_PROFILES_ACTIVE=production
SERVER_PORT=8081
```

## 📊 Monitoring & Observability

- **Health Check**: `/actuator/health`
- **Metrics**: `/actuator/prometheus`
- **API Docs**: `/swagger-ui.html`
- **Distributed Tracing**: Zipkin integration

## 🛡️ Security Features

- **Password Security**: Argon2 hashing, complexity requirements
- **Rate Limiting**: Per-user and per-endpoint limits
- **CORS**: Configurable allowed origins
- **CSRF Protection**: For state-changing operations
- **Security Headers**: XSS, clickjacking, content-type protection
- **Audit Logging**: All security events logged

## 📚 Documentation

- [Technical Documentation](./TECHNICAL_DOCUMENTATION.md) - Architecture, API, implementation details
- [Test Coverage Report](./TEST_COVERAGE.md) - Comprehensive test documentation
- [CLAUDE.md](../CLAUDE.md) - AI assistant development guide

## 🤝 Contributing

This is a University of London final year project. For questions or collaboration:
- **Author**: Nasir Idrishi
- **Email**: nasiridrishi@outlook.com
- **Project**: CM3035 Advanced Web Design Template

## 📄 License

This project is part of an academic submission for the University of London Computer Science program.