# FocusHive Backend

[![Java](https://img.shields.io/badge/Java-21-orange?style=flat&logo=openjdk)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-brightgreen?style=flat&logo=spring)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue?style=flat&logo=docker)](https://www.docker.com/)
[![API Docs](https://img.shields.io/badge/API-Swagger-green?style=flat&logo=swagger)](http://localhost:8080/swagger-ui.html)

## 📋 Overview

FocusHive Backend is a **monolithic Spring Boot service** that consolidates multiple modules for digital co-working and co-studying. It provides real-time presence tracking, collaborative hives, focus sessions, and productivity analytics.

**Architecture Note**: Originally designed as separate microservices, Chat, Analytics, and Forum services have been **merged as modules** into this backend to reduce operational complexity while maintaining logical separation.

## 🚀 Features

### Core Modules
- **🏠 Hive Management**: Virtual co-working spaces with member management
- **👥 Real-time Presence**: WebSocket-based user presence and status tracking
- **⏰ Focus Timer**: Pomodoro-style productivity sessions with synchronization
- **💬 Chat Module**: Real-time messaging within hives (merged from chat-service)
- **📊 Analytics Module**: Productivity insights, streaks, and gamification (merged from analytics-service)
- **🏛️ Forum Module**: Community discussions and Q&A (merged from forum-service)

### External Service Integrations
- **🔐 Identity Service** (Port 8081): OAuth2 authentication, JWT validation, multi-persona profiles
- **🔔 Notification Service** (Port 8083): Multi-channel notification delivery
- **🤝 Buddy Service** (Port 8087): Accountability partner matching system

### Technical Features
- **🔐 JWT Authentication**: Token-based security with Identity Service validation
- **🌐 WebSocket Support**: STOMP protocol for real-time features
- **⚡ Two-tier Caching**: Caffeine (L1) + Redis (L2) for performance
- **📈 Observability**: Prometheus metrics, health checks, distributed tracing
- **🗄️ Database**: PostgreSQL with Flyway migrations (H2 for development)
- **🔄 Circuit Breakers**: Resilience4j for external service calls
- **🐳 Docker Ready**: Multi-stage build with health checks
- **📖 API Documentation**: SpringDoc OpenAPI 3.0 with Swagger UI

## 🏗️ Architecture

### Service Communication
```
┌─────────────────┐    ┌─────────────────────┐    ┌──────────────────┐
│   Frontend      │    │  FocusHive Backend  │    │ External Services│
│   React App     │◄──►│    (Port 8080)      │◄──►├──────────────────┤
│                 │    │                     │    │ Identity (8081)  │
└─────────────────┘    │  ┌──────────────┐  │    │ Notification     │
         ▲             │  │ Chat Module  │  │    │   (8083)         │
         │             │  ├──────────────┤  │    │ Buddy (8087)     │
    WebSocket          │  │Analytics Mod │  │    └──────────────────┘
         │             │  ├──────────────┤  │             │
         └─────────────►  │ Forum Module │  │             ▼
                       │  └──────────────┘  │    ┌──────────────────┐
                       └─────────────────────┘    │   Databases      │
                                │                 ├──────────────────┤
                                └─────────────────► PostgreSQL       │
                                                  │ Redis Cache      │
                                                  └──────────────────┘
```

### Module Structure
```
src/main/java/com/focushive/
├── analytics/          # Productivity tracking module
│   ├── controller/     # REST & WebSocket endpoints
│   ├── entity/         # JPA entities
│   ├── repository/     # Data access layer
│   └── service/        # Business logic
├── chat/              # Real-time messaging module
│   ├── controller/     # REST & WebSocket handlers
│   ├── entity/         # Message entities
│   └── service/        # Chat service logic
├── forum/             # Community discussions module
│   ├── controller/     # Forum endpoints
│   ├── domain/         # Forum entities
│   └── service/        # Forum logic
├── hive/              # Core hive management
│   ├── controller/     # Hive & membership APIs
│   ├── entity/         # Hive entities
│   └── service/        # Hive business logic
├── presence/          # Real-time presence tracking
│   ├── controller/     # REST & WebSocket
│   └── service/        # Presence management
├── timer/             # Focus session management
│   ├── controller/     # Timer endpoints
│   ├── dto/            # Timer DTOs
│   └── service/        # Timer logic
├── websocket/         # WebSocket infrastructure
│   ├── config/         # STOMP configuration
│   ├── controller/     # WebSocket controllers
│   └── handler/        # Event handlers
├── api/               # External service clients
│   ├── client/         # Feign clients
│   └── security/       # JWT validation
├── integration/       # Additional integrations
│   ├── client/         # Buddy & Notification clients
│   └── service/        # Integration services
├── config/            # Spring configurations
│   ├── SecurityConfig  # Security setup
│   ├── RedisConfig     # Cache configuration
│   └── WebSocketConfig # WebSocket setup
└── common/            # Shared utilities
```

## 🛠️ Technology Stack

- **Framework**: Spring Boot 3.3.0
- **Language**: Java 21
- **Database**: H2 (development) / PostgreSQL (production)
- **Cache**: Caffeine (L1) / Redis (L2)
- **Build Tool**: Gradle 8.11
- **WebSocket**: Spring WebSocket with STOMP
- **API Docs**: SpringDoc OpenAPI 3.0
- **Security**: Spring Security + JWT
- **HTTP Client**: OpenFeign with Circuit Breakers
- **Monitoring**: Micrometer + Prometheus
- **Container**: Docker with multi-stage builds

## 🚀 Getting Started

### Prerequisites
- Java 21
- Docker & Docker Compose
- Redis (for caching)
- PostgreSQL (for production)

### Running with Docker Compose
```bash
# Start all services (PostgreSQL, Redis, Backend)
docker-compose up -d

# Check service health
curl http://localhost:8080/actuator/health
```

### Running Locally
```bash
# Start infrastructure services
docker-compose up -d postgres redis

# Run the application
./gradlew bootRun

# Or with a specific profile
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Building
```bash
# Build without tests (faster)
./gradlew build -x test

# Full build with tests
./gradlew clean build

# Build Docker image
./gradlew bootBuildImage
# Or
docker build -t focushive-backend:latest .
```

## 🧪 Testing

### Running Tests
```bash
# All tests
./gradlew test

# Unit tests only (excludes integration tests)
./gradlew unitTest

# Specific test class
./gradlew test --tests "FocusTimerServiceTest"

# With test report
./gradlew test jacocoTestReport
# Report: build/reports/tests/test/index.html
```

### Test Categories
- **Unit Tests**: `*Test.java` - Fast, isolated tests
- **Integration Tests**: `*IntegrationTest.java` - Full Spring context

## 📡 API Documentation

### Swagger UI
Access the interactive API documentation at:
- Local: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs

### Key API Endpoints

#### Hive Management
- `POST /api/v1/hives` - Create hive
- `GET /api/v1/hives/{id}` - Get hive details
- `POST /api/v1/hives/{id}/join` - Join hive
- `POST /api/v1/hives/{id}/leave` - Leave hive
- `GET /api/v1/hives/{id}/members` - List members

#### Focus Timer
- `POST /api/v1/timer/sessions/start` - Start focus session
- `POST /api/v1/timer/sessions/{sessionId}/end` - End session
- `POST /api/v1/timer/sessions/{sessionId}/pause` - Pause session
- `GET /api/v1/timer/sessions/current` - Get current session
- `GET /api/v1/timer/stats` - Get user statistics

#### Analytics
- `GET /api/v1/analytics/daily` - Daily productivity stats
- `GET /api/v1/analytics/weekly` - Weekly summary
- `GET /api/v1/analytics/achievements` - User achievements
- `GET /api/v1/analytics/leaderboard` - Hive leaderboard

#### Chat
- `GET /api/v1/chat/messages` - Get chat messages
- `POST /api/v1/chat/messages` - Send message
- `GET /api/v1/chat/history/{hiveId}` - Chat history

#### Presence
- `POST /api/v1/presence/update` - Update presence status
- `GET /api/v1/presence/hive/{hiveId}` - Get hive presence
- `GET /api/v1/presence/user/{userId}` - Get user presence

### WebSocket Endpoints
- **Connection**: `/ws`
- **STOMP Destinations**:
  - `/topic/hive/{hiveId}/presence` - Presence updates
  - `/topic/hive/{hiveId}/chat` - Chat messages
  - `/topic/hive/{hiveId}/timer` - Timer synchronization
  - `/user/queue/notifications` - Personal notifications

## ⚙️ Configuration

### Environment Variables
```bash
# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/focushive
DATABASE_USERNAME=focushive
DATABASE_PASSWORD=secure_password
DATABASE_DRIVER=org.postgresql.Driver

# Redis Cache
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=optional_password

# JWT Security
JWT_SECRET=your-256-bit-secret-key
JWT_EXPIRATION=86400000        # 24 hours
JWT_REFRESH_EXPIRATION=604800000  # 7 days

# External Services
IDENTITY_SERVICE_URL=http://localhost:8081
IDENTITY_SERVICE_API_KEY=optional_api_key
NOTIFICATION_SERVICE_URL=http://localhost:8083
BUDDY_SERVICE_URL=http://localhost:8087

# Server Configuration
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev

# Monitoring
ZIPKIN_ENDPOINT=http://localhost:9411/api/v2/spans
```

### Spring Profiles
- `default` - H2 database, minimal configuration
- `dev` - H2 database, debug logging, DevTools enabled
- `test` - H2 in-memory, mocked external services
- `staging` - PostgreSQL, Redis, external service integration
- `prod` - Full production configuration with monitoring

## 📊 Monitoring & Health

### Health Checks
- Overall: `GET /actuator/health`
- Liveness: `GET /actuator/health/liveness`
- Readiness: `GET /actuator/health/readiness`
- Components:
  - Database connectivity
  - Redis cache status
  - External service circuit breakers
  - WebSocket connections

### Metrics
- Prometheus: `GET /actuator/prometheus`
- Application metrics: `GET /actuator/metrics`
- Custom business metrics for:
  - Active hive sessions
  - Focus session duration
  - Message throughput
  - User engagement

## 🔧 Development

### Database Migrations
```bash
# Run migrations
./gradlew flywayMigrate

# Validate migrations
./gradlew flywayValidate

# Migration info
./gradlew flywayInfo
```

### Code Quality
```bash
# Check code style
./gradlew checkstyleMain checkstyleTest

# Run SpotBugs analysis
./gradlew spotbugsMain

# Dependency report
./gradlew dependencyReport
```

## 🐳 Docker

### Multi-stage Build
The Dockerfile uses a multi-stage build for optimal image size:
1. **Build stage**: Compiles with Gradle
2. **Production stage**: Runs with JRE only

### Running in Docker
```bash
# Build image
docker build -t focushive-backend:latest .

# Run container
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/focushive \
  -e REDIS_HOST=host.docker.internal \
  focushive-backend:latest
```

## 🚨 Known Issues & Production Notes

### Current State
- ✅ Core functionality operational
- ✅ WebSocket real-time features working
- ✅ External service integration functional
- ⚠️ Flyway migrations currently disabled (`flyway.enabled=false`)
- ⚠️ Some tests need updating after module consolidation

### Production Considerations
1. **Database**: Switch from H2 to PostgreSQL
2. **Caching**: Configure Redis for distributed caching
3. **Security**: Update JWT secret and secure all endpoints
4. **Monitoring**: Enable Prometheus and Grafana dashboards
5. **Scaling**: Configure connection pools and thread pools

## 📚 Related Services

- [Identity Service](../identity-service/README.md) - Authentication & user management
- [Notification Service](../notification-service/README.md) - Multi-channel notifications
- [Buddy Service](../buddy-service/README.md) - Accountability partners
- [Frontend](../../frontend/README.md) - React TypeScript application

## 📝 License

This is an academic project for the University of London BSc Computer Science program.