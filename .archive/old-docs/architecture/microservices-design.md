# FocusHive Microservices Architecture

## Overview

FocusHive is designed as a modular monolith with clear service boundaries, allowing for future extraction into separate microservices if needed. Each service is organized as a separate package within the Spring Boot application with well-defined interfaces and responsibilities.

## Architecture Principles

1. **Domain-Driven Design**: Each service represents a bounded context
2. **API-First**: All services expose RESTful APIs with OpenAPI documentation
3. **Event-Driven**: Services communicate via events for loose coupling
4. **Database per Service**: Each service owns its data and schemas
5. **Security by Design**: JWT-based authentication across all services

## Core Services

### 1. User Service (`com.focushive.user`)
**Responsibility**: User management, authentication, and profiles

**Key Components**:
- User registration and login
- Profile management
- JWT token generation and validation
- OAuth2 integration (Google, GitHub)
- Password reset functionality

**Database Tables**:
- `users`
- `user_profiles`
- `user_preferences`
- `oauth_connections`

**API Endpoints**:
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/users/{id}`
- `PUT /api/v1/users/{id}`
- `GET /api/v1/users/me`

### 2. Hive Service (`com.focushive.hive`)
**Responsibility**: Virtual co-working space management

**Key Components**:
- Hive creation and configuration
- Member management
- Access control and permissions
- Hive settings and preferences

**Database Tables**:
- `hives`
- `hive_members`
- `hive_settings`
- `hive_invitations`

**API Endpoints**:
- `POST /api/v1/hives`
- `GET /api/v1/hives/{id}`
- `PUT /api/v1/hives/{id}`
- `DELETE /api/v1/hives/{id}`
- `POST /api/v1/hives/{id}/join`
- `POST /api/v1/hives/{id}/leave`
- `GET /api/v1/hives/{id}/members`

### 3. Presence Service (`com.focushive.presence`)
**Responsibility**: Real-time user status and activity tracking

**Key Components**:
- WebSocket connections management
- User status updates (online/away/busy/offline)
- Activity tracking
- Presence broadcasting
- Redis integration for real-time data

**Database Tables**:
- `user_sessions`
- `activity_logs`

**Redis Keys**:
- `presence:{userId}` - Current user status
- `hive:{hiveId}:members` - Active members in a hive

**WebSocket Endpoints**:
- `/ws/presence` - Main WebSocket endpoint
- STOMP destinations:
  - `/topic/hive/{hiveId}/presence` - Hive presence updates
  - `/user/queue/status` - Personal status updates

### 4. Analytics Service (`com.focushive.analytics`)
**Responsibility**: Productivity tracking and insights

**Key Components**:
- Session tracking
- Productivity metrics calculation
- Focus time analytics
- Daily/weekly/monthly reports
- Achievement tracking

**Database Tables**:
- `focus_sessions`
- `productivity_metrics`
- `user_achievements`
- `daily_summaries`

**API Endpoints**:
- `POST /api/v1/analytics/sessions/start`
- `POST /api/v1/analytics/sessions/end`
- `GET /api/v1/analytics/users/{id}/summary`
- `GET /api/v1/analytics/users/{id}/achievements`
- `GET /api/v1/analytics/hives/{id}/leaderboard`

## Shared Components

### Common Package (`com.focushive.common`)
- Exception handlers
- Security configuration
- Base entities (audit fields)
- Utility classes
- Common DTOs

### Event System (`com.focushive.events`)
- Event publishers
- Event listeners
- Event store (for event sourcing)

## Communication Patterns

### Synchronous Communication
- RESTful APIs for client-server communication
- Service-to-service calls via interfaces (within monolith)

### Asynchronous Communication
- Spring Application Events for internal communication
- WebSockets for real-time updates
- Redis Pub/Sub for distributed events

## Technology Stack

- **Framework**: Spring Boot 3.x
- **Language**: Java 21
- **Build Tool**: Gradle
- **API Documentation**: SpringDoc OpenAPI
- **Database**: PostgreSQL
- **Cache/Real-time**: Redis
- **WebSockets**: Spring WebSocket with STOMP
- **Security**: Spring Security + JWT

## Deployment Architecture

```
┌─────────────────┐     ┌─────────────────┐
│   React Web     │     │  React Mobile   │
│   Application   │     │  Application    │
└────────┬────────┘     └────────┬────────┘
         │                       │
         └───────────┬───────────┘
                     │
              ┌──────▼──────┐
              │   Nginx     │
              │  (Reverse   │
              │   Proxy)    │
              └──────┬──────┘
                     │
        ┌────────────▼────────────┐
        │   Spring Boot App       │
        │  ┌─────────────────┐    │
        │  │  User Service   │    │
        │  ├─────────────────┤    │
        │  │  Hive Service   │    │
        │  ├─────────────────┤    │
        │  │Presence Service │    │
        │  ├─────────────────┤    │
        │  │Analytics Service│    │
        │  └─────────────────┘    │
        └────────┬───────┬────────┘
                 │       │
         ┌───────▼───┐ ┌─▼────┐
         │PostgreSQL │ │Redis │
         └───────────┘ └──────┘
```

## Future Considerations

1. **Service Extraction**: The modular design allows extracting services into separate deployments
2. **API Gateway**: Can add Spring Cloud Gateway when moving to microservices
3. **Service Discovery**: Can integrate Eureka or Consul for service discovery
4. **Message Queue**: Can add RabbitMQ/Kafka for async communication between services
5. **Distributed Tracing**: Can add Spring Cloud Sleuth + Zipkin for monitoring