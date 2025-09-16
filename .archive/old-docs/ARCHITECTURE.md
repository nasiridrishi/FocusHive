# FocusHive System Architecture

## Overview
FocusHive is a microservices-based digital co-working platform built with Spring Boot (backend) and React (frontend), designed for scalability, reliability, and real-time collaboration.

## System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Frontend (React)                      │
│                     Port: 3000                          │
│              Material UI • TypeScript • Vite            │
└────────────────────────┬────────────────────────────────┘
                         │ HTTPS/WSS
                         │
┌────────────────────────┴────────────────────────────────┐
│                   Load Balancer                         │
│                    (nginx/ALB)                          │
└────────────────────────┬────────────────────────────────┘
                         │
     ┌───────────────────┼───────────────────┐
     │                   │                   │
┌────▼──────────────────────────────────┐ ┌──▼──────────┐
│   FocusHive Backend (Monolith)        │ │  Identity   │
│           Port: 8080                  │ │   Service   │
│                                       │ │  Port: 8081 │
│  Core Services:                       │ │             │
│  • Hive Management                    │ │ • OAuth2    │
│  • Timer Synchronization              │ │ • JWT Auth  │
│  • Real-time Presence                 │ │ • Personas  │
│  • WebSocket Hub                      │ │             │
│                                       │ └─────────────┘
│  Integrated Modules:                  │         │
│  • Chat System                        │ ┌───────▼──────┐
│  • Analytics Engine                   │ │ Notification │
│  • Forum Platform                     │ │   Service    │
│                                       │ │  Port: 8083  │
└───────────────┬───────────────────────┘ │             │
                │                         │ • Email      │
                │                         │ • SMS        │
                │                         │ • Push       │
                │                         └──────────────┘
                │                                 │
                │                         ┌───────▼──────┐
                │                         │    Buddy     │
                │                         │   Service    │
                │                         │  Port: 8087  │
                │                         │             │
                │                         │ • Matching   │
                │                         │ • Goals      │
                │                         │ • Check-ins  │
                │                         └──────────────┘
                │
    ┌───────────┴───────────────────────────────┐
    │                                           │
┌───▼────────────────────┐      ┌──────────────▼──────┐
│     PostgreSQL 16      │      │      Redis 7        │
│                        │      │                     │
│ • Users & Auth         │      │ • Session Cache     │
│ • Hives & Activities   │      │ • Real-time Data    │
│ • Chat Messages        │      │ • Rate Limiting     │
│ • Analytics Data       │      │ • Pub/Sub Events    │
│ • Forum Content        │      │                     │
└────────────────────────┘      └─────────────────────┘
```

## Service Architecture

### 1. FocusHive Backend (Monolith)
**Technology**: Spring Boot 3.3.0, Java 21
**Responsibilities**:
- Core business logic and orchestration
- WebSocket management for real-time features
- RESTful API endpoints
- Event coordination between services

**Modules**:
- **Hive Module**: Virtual workspace management
- **Timer Module**: Pomodoro synchronization
- **Presence Module**: Real-time activity tracking
- **Chat Module**: Integrated messaging system
- **Analytics Module**: Productivity metrics and insights
- **Forum Module**: Community discussions

### 2. Identity Service
**Technology**: Spring Boot 3.3.0, Java 21
**Responsibilities**:
- OAuth2 authorization server
- JWT token generation and validation
- User authentication and authorization
- Multi-persona management
- Privacy controls and data protection

### 3. Notification Service
**Technology**: Spring Boot 3.3.0, Java 21
**Responsibilities**:
- Multi-channel notification delivery
- Template management
- Notification preferences
- Delivery tracking and retry logic
- Rate limiting per channel

### 4. Buddy Service
**Technology**: Spring Boot 3.2.0, Java 21
**Responsibilities**:
- Accountability partner matching
- Goal sharing and tracking
- Check-in reminders
- Partnership health monitoring
- Matching algorithm optimization

## Database Architecture

### PostgreSQL Schema Design

```sql
-- Core Schemas
focushive_core     -- Main application data
identity_service   -- Authentication & users
notification_db    -- Notification logs
buddy_service      -- Partnership data

-- Key Tables
users              -- User accounts
hives              -- Virtual workspaces
hive_members       -- Membership tracking
activities         -- User activities
timers             -- Synchronized timers
chat_messages      -- Real-time messages
analytics_events   -- Tracking events
```

### Database Optimizations
- **Indexes**: 45+ optimized indexes for query performance
- **Partitioning**: Time-based partitioning for analytics
- **Connection Pooling**: HikariCP with optimized settings
- **Read Replicas**: For analytics queries (planned)

### Redis Architecture

```yaml
Databases:
  0: Session storage
  1: Cache layer
  2: Rate limiting
  3: WebSocket sessions
  4: Real-time presence
  5: Pub/Sub messaging
  6: Temporary data
  7: Buddy service cache
```

## Communication Patterns

### Synchronous Communication
- **REST APIs** via OpenFeign clients
- **Request/Response** with circuit breakers
- **Service discovery** via DNS
- **Load balancing** with retry logic

### Asynchronous Communication
- **WebSockets** for real-time updates
- **Redis Pub/Sub** for event broadcasting
- **Message queues** for background tasks
- **Event sourcing** for audit trails

### API Gateway Pattern (Planned)
```
Client → API Gateway → Service Discovery → Microservice
         ↓
     Rate Limiting
     Authentication
     Request Routing
     Response Caching
```

## Technology Stack

### Backend Technologies
| Component | Technology | Version |
|-----------|------------|---------|
| Framework | Spring Boot | 3.3.0 |
| Language | Java | 21 |
| Build Tool | Gradle | 8.11 |
| Database | PostgreSQL | 16 |
| Cache | Redis | 7 |
| ORM | Hibernate/JPA | 6.x |
| Migration | Flyway | 10.x |
| Testing | JUnit 5 | 5.10 |
| API Docs | OpenAPI 3 | 3.0 |

### Frontend Technologies
| Component | Technology | Version |
|-----------|------------|---------|
| Framework | React | 18.3.1 |
| Language | TypeScript | 5.6 |
| UI Library | Material UI | 5.16 |
| Build Tool | Vite | 5.4 |
| State Mgmt | Zustand | 4.5 |
| HTTP Client | Axios | 1.7 |
| WebSocket | Socket.io | 4.7 |
| Testing | Jest | 29.7 |

## Deployment Architecture

### Container Strategy
```dockerfile
# Base image optimization
FROM eclipse-temurin:21-jre-alpine

# Multi-stage builds for size reduction
# Non-root user execution
# Health checks included
# Graceful shutdown support
```

### Docker Compose Services
```yaml
services:
  focushive_backend
  identity_service
  notification_service
  buddy_service
  postgres
  redis
  nginx (reverse proxy)
```

## Scalability Design

### Horizontal Scaling
- **Stateless services** for easy scaling
- **Session affinity** via Redis
- **Database connection pooling**
- **Caching strategy** at multiple levels

### Performance Targets
- **API Response**: < 200ms (p95)
- **WebSocket Latency**: < 50ms
- **Database Queries**: < 100ms
- **Page Load**: < 3 seconds
- **Concurrent Users**: 10,000+

## Security Architecture

### Defense in Depth
1. **Network Layer**: Firewall, DDoS protection
2. **Application Layer**: WAF, rate limiting
3. **Service Layer**: JWT authentication, RBAC
4. **Data Layer**: Encryption at rest/transit

### Security Controls
- OAuth2/JWT authentication
- Role-based authorization
- API rate limiting
- Input validation
- SQL injection prevention
- XSS protection
- CORS configuration

## Monitoring & Observability

### Metrics Collection
- **Application Metrics**: Micrometer + Prometheus
- **Infrastructure Metrics**: Node exporter
- **Custom Metrics**: Business KPIs

### Logging Strategy
- **Centralized Logging**: ELK stack (planned)
- **Structured Logging**: JSON format
- **Log Levels**: Environment-specific
- **Correlation IDs**: Request tracing

### Health Checks
```
/actuator/health         - Overall health
/actuator/health/liveness  - Kubernetes liveness
/actuator/health/readiness - Kubernetes readiness
/actuator/metrics        - Prometheus metrics
```

## Development Patterns

### Design Patterns Used
- **Repository Pattern**: Data access abstraction
- **Service Layer**: Business logic separation
- **DTO Pattern**: Data transfer objects
- **Builder Pattern**: Complex object creation
- **Strategy Pattern**: Algorithm selection
- **Observer Pattern**: Event handling

### Code Organization
```
src/
├── main/java/com/focushive/
│   ├── controller/    # REST endpoints
│   ├── service/       # Business logic
│   ├── repository/    # Data access
│   ├── entity/        # Domain models
│   ├── dto/          # Transfer objects
│   ├── config/       # Configuration
│   ├── security/     # Security layer
│   └── websocket/    # Real-time
```

## Future Architecture Plans

### Phase 2 Enhancements
- API Gateway implementation
- Service mesh (Istio)
- Event streaming (Kafka)
- CQRS pattern for analytics
- GraphQL federation

### Scaling Improvements
- Kubernetes deployment
- Auto-scaling policies
- Global CDN
- Multi-region support
- Database sharding