# FocusHive Project Index

> **Last Updated**: September 4, 2025  
> **Version**: 1.0.0  
> **Status**: Active Development

## 📚 Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Services Directory](#services-directory)
4. [Frontend Structure](#frontend-structure)
5. [API Documentation](#api-documentation)
6. [Development Resources](#development-resources)
7. [Testing Infrastructure](#testing-infrastructure)
8. [Deployment & Operations](#deployment--operations)
9. [Academic Requirements](#academic-requirements)
10. [Quick Links](#quick-links)

---

## 🎯 Project Overview

FocusHive is a digital co-working and co-studying platform creating virtual "hives" where users work on individual tasks while being visibly present and accountable to others.

### Key Features
- 🏠 **Virtual Hives**: Dedicated online workspaces for collaborative focus
- ⏱️ **Synchronized Timers**: Shared Pomodoro sessions across hive members
- 👥 **Real-time Presence**: Live activity tracking and status updates
- 📊 **Productivity Analytics**: Comprehensive tracking and insights
- 🎮 **Gamification**: Points, achievements, and leaderboards
- 🤝 **Buddy System**: Accountability partner matching
- 💬 **Integrated Chat**: Real-time messaging within hives
- 🎵 **Music Integration**: Spotify-powered collaborative playlists

### Technology Stack
| Layer | Technology | Version |
|-------|------------|---------|
| Frontend | React + TypeScript | 18.3.1 |
| UI Framework | Material UI | 5.16.x |
| Build Tool | Vite | 5.4.10 |
| Backend | Spring Boot | 3.3.0 |
| Language | Java | 21 |
| Database | PostgreSQL | 16 |
| Cache | Redis | 7 |
| Real-time | WebSocket + STOMP | - |
| Container | Docker | 24.x |

---

## 🏗️ Architecture

### Microservices Architecture
```
┌─────────────────────────────────────────────────────────┐
│                    Frontend (React)                      │
│                     Port: 3000                          │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────┴────────────────────────────────┐
│                   API Gateway (NGINX)                    │
│                     Port: 8080                          │
└────────────────────────┬────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
┌───────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐
│   FocusHive  │ │  Identity   │ │    Music    │
│   Backend    │ │   Service   │ │   Service   │
│   Port: 8080 │ │  Port: 8081 │ │  Port: 8082 │
└──────────────┘ └─────────────┘ └─────────────┘
        │                │                │
┌───────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐
│ Notification │ │     Chat    │ │  Analytics  │
│   Service    │ │   Service   │ │   Service   │
│  Port: 8083  │ │  Port: 8084 │ │  Port: 8085 │
└──────────────┘ └─────────────┘ └─────────────┘
        │                │                │
┌───────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐
│    Forum     │ │    Buddy    │ │  PostgreSQL │
│   Service    │ │   Service   │ │    Redis    │
│  Port: 8086  │ │  Port: 8087 │ │             │
└──────────────┘ └─────────────┘ └─────────────┘
```

### Service Communication
- **REST APIs**: Service-to-service via OpenFeign
- **WebSocket**: Real-time updates via STOMP
- **Events**: Redis pub/sub for async communication
- **Database**: Service-specific PostgreSQL schemas

---

## 📁 Services Directory

### Core Services

#### 1. FocusHive Backend (Primary Service)
- **Location**: `/services/focushive-backend/`
- **Port**: 8080
- **Responsibilities**:
  - User management
  - Hive creation and management
  - Real-time presence tracking
  - Timer synchronization
  - WebSocket coordination
- **Key Endpoints**:
  - `/api/hives` - Hive management
  - `/api/presence` - Real-time presence
  - `/api/timer` - Timer operations
  - `/ws` - WebSocket connection

#### 2. Identity Service
- **Location**: `/services/identity-service/`
- **Port**: 8081
- **Responsibilities**:
  - OAuth2 authorization server
  - JWT token management
  - Multi-persona profiles
  - Privacy controls
  - User authentication
- **Key Endpoints**:
  - `/oauth2/authorize` - OAuth2 authorization
  - `/oauth2/token` - Token operations
  - `/api/personas` - Persona management
  - `/api/users` - User profiles

#### 3. Music Service
- **Location**: `/services/music-service/`
- **Port**: 8082
- **Responsibilities**:
  - Spotify integration
  - Collaborative playlists
  - Music recommendations
  - Mood-based selections
- **Key Endpoints**:
  - `/api/spotify/auth` - Spotify OAuth
  - `/api/playlists` - Playlist management
  - `/api/recommendations` - Music suggestions

#### 4. Analytics Service
- **Location**: `/services/analytics-service/`
- **Port**: 8085
- **Responsibilities**:
  - Productivity tracking
  - Performance metrics
  - Achievement calculations
  - Report generation
- **Key Endpoints**:
  - `/api/analytics/sessions` - Session analytics
  - `/api/analytics/productivity` - Productivity metrics
  - `/api/analytics/achievements` - Achievement tracking

### Supporting Services

| Service | Port | Purpose |
|---------|------|---------|
| Notification Service | 8083 | Multi-channel notifications |
| Chat Service | 8084 | Real-time messaging |
| Forum Service | 8086 | Community discussions |
| Buddy Service | 8087 | Accountability partners |

---

## 💻 Frontend Structure

### Directory Organization
```
frontend/src/
├── app/                    # Application shell
├── features/              # Feature modules
│   ├── analytics/         # Analytics dashboard
│   ├── auth/             # Authentication
│   ├── buddy/            # Buddy system
│   ├── chat/             # Chat interface
│   ├── forum/            # Forum features
│   ├── gamification/     # Points & achievements
│   ├── hive/             # Hive management
│   ├── music/            # Music integration
│   ├── presence/         # Presence system
│   └── timer/            # Focus timer
├── shared/               # Shared resources
│   ├── components/       # Reusable components
│   ├── contexts/         # React contexts
│   ├── hooks/           # Custom hooks
│   ├── theme/           # MUI theme
│   ├── types/           # TypeScript definitions
│   └── ui/              # UI components
├── services/            # API services
│   ├── api/            # API clients
│   ├── config/         # Configuration
│   └── websocket/      # WebSocket service
└── test-utils/         # Testing utilities
```

### Key Components

#### Real-time Features
- **WebSocketContext**: Global WebSocket state management
- **PresenceContext**: Real-time presence tracking
- **ChatContext**: Chat state and message handling
- **TimerContext**: Synchronized timer state

#### UI Components
- **ResponsiveLayout**: Adaptive layout system
- **AdaptiveNavigation**: Mobile/desktop navigation
- **SmartModal**: Intelligent modal system
- **AccessibleButton**: WCAG-compliant buttons

#### PWA Features
- **PWAProvider**: Service worker management
- **PWAUpdateNotification**: Update prompts
- **Offline indicators**: Connection status

---

## 📡 API Documentation

### Authentication
All API endpoints (except public) require JWT authentication:
```
Authorization: Bearer <jwt-token>
```

### Core API Endpoints

#### Hive Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/hives` | List all hives |
| POST | `/api/hives` | Create new hive |
| GET | `/api/hives/{id}` | Get hive details |
| PUT | `/api/hives/{id}` | Update hive |
| DELETE | `/api/hives/{id}` | Delete hive |
| POST | `/api/hives/{id}/join` | Join hive |
| POST | `/api/hives/{id}/leave` | Leave hive |

#### Presence & Real-time
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/presence/hive/{id}` | Get hive presence |
| POST | `/api/presence/update` | Update presence |
| WS | `/ws` | WebSocket connection |
| STOMP | `/app/presence` | Presence updates |

#### Timer Operations
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/timer/start` | Start focus session |
| POST | `/api/timer/pause` | Pause session |
| POST | `/api/timer/stop` | Stop session |
| GET | `/api/timer/status` | Get timer status |

### WebSocket Topics
```javascript
// Subscription topics
/topic/hive/{hiveId}/presence  - Presence updates
/topic/hive/{hiveId}/chat      - Chat messages
/topic/hive/{hiveId}/timer     - Timer updates
/topic/user/{userId}/notifications - Personal notifications

// Send destinations
/app/presence/update    - Update presence
/app/chat/send         - Send message
/app/timer/sync        - Sync timer
```

---

## 🛠️ Development Resources

### Configuration Files

#### Environment Variables
- **Frontend**: `/frontend/.env.example`
- **Backend**: `/services/*/application.yml`
- **Docker**: `/.env.example`

#### Build Configuration
- **Frontend**: `/frontend/vite.config.ts`
- **Backend**: `/services/*/build.gradle.kts`
- **Docker**: `/docker-compose.yml`

### Development Commands

#### Frontend Development
```bash
cd frontend
npm install        # Install dependencies
npm run dev       # Start dev server
npm run build     # Production build
npm run test      # Run tests
npm run lint      # Lint code
```

#### Backend Development
```bash
cd services/focushive-backend
./gradlew bootRun     # Start service
./gradlew test        # Run tests
./gradlew build       # Build JAR
./gradlew clean       # Clean build
```

#### Docker Operations
```bash
docker-compose up -d           # Start all services
docker-compose down            # Stop all services
docker-compose logs -f [service]  # View logs
docker-compose ps              # List services
```

### Database Management

#### Migrations
- **Tool**: Flyway
- **Location**: `/services/*/src/main/resources/db/migration/`
- **Naming**: `V{version}__{description}.sql`

#### Schema Structure
```sql
-- Each service has its own schema
focushive_main    -- Core application data
identity_service  -- User and auth data
analytics_service -- Analytics data
chat_service      -- Chat messages
forum_service     -- Forum posts
buddy_service     -- Buddy relationships
music_service     -- Music preferences
```

---

## 🧪 Testing Infrastructure

### Test Coverage
| Component | Coverage | Tests |
|-----------|----------|-------|
| Backend Services | 92% | 640+ |
| Frontend Components | 80% | 250+ |
| E2E Tests | - | 50+ |

### Testing Strategy

#### Unit Tests
- **Backend**: JUnit 5 + Mockito
- **Frontend**: Vitest + React Testing Library
- **Location**: `*.test.{ts,tsx,java}`

#### Integration Tests
- **Backend**: TestContainers + MockMvc
- **Frontend**: MSW (Mock Service Worker)
- **Database**: H2 for tests

#### E2E Tests
- **Tool**: Playwright
- **Location**: `/e2e-tests/`
- **Coverage**: Critical user journeys

### Running Tests
```bash
# Frontend tests
npm run test          # Run once
npm run test:watch    # Watch mode
npm run test:coverage # With coverage

# Backend tests
./gradlew test              # All tests
./gradlew test --tests *Service*  # Specific pattern
./gradlew jacocoTestReport  # Coverage report
```

---

## 🚀 Deployment & Operations

### Docker Deployment

#### Multi-Environment Setup
```yaml
docker-compose.yml         # Base configuration
docker-compose.dev.yml     # Development overrides
docker-compose.prod.yml    # Production settings
docker-compose.test.yml    # Testing environment
```

#### Service Health Checks
All services expose health endpoints:
- `/actuator/health` - Basic health
- `/actuator/health/liveness` - Liveness probe
- `/actuator/health/readiness` - Readiness probe

### Monitoring Stack

#### Metrics Collection
- **Prometheus**: Metrics aggregation
- **Grafana**: Visualization dashboards
- **Zipkin**: Distributed tracing
- **ELK Stack**: Log aggregation

#### Key Metrics
- Response times (p50, p95, p99)
- Error rates by service
- Database query performance
- WebSocket connection count
- Cache hit rates

### CI/CD Pipeline

#### GitHub Actions Workflows
- `.github/workflows/ci.yml` - Continuous integration
- `.github/workflows/deploy.yml` - Deployment pipeline
- `.github/workflows/e2e.yml` - E2E test suite

#### Pipeline Stages
1. **Lint & Format**: Code quality checks
2. **Unit Tests**: Component testing
3. **Integration Tests**: Service testing
4. **Build**: Docker image creation
5. **E2E Tests**: Full system testing
6. **Deploy**: Environment deployment

---

## 🎓 Academic Requirements

### University of London Templates

#### Primary Template (70%)
**CM3055 Interaction Design**
- Emotion-aware adaptive UI
- Real-time presence for mood detection
- Stress reduction features
- Adaptive themes and responsive design

#### Secondary Template (25%)
**CM3035 Advanced Web Design**
- Identity management API
- OAuth2 provider implementation
- Multi-persona profiles
- Privacy controls and data portability

#### Supporting Template (5%)
**CM3065 Intelligent Signal Processing**
- Gamification system
- Activity pattern analysis
- Performance tracking algorithms
- Adaptive challenge generation

### Project Timeline
- **Start Date**: May 22, 2025
- **End Date**: September 22, 2025
- **Current Phase**: Development & Testing
- **Submission**: Final report and code

### Academic Documentation
| Document | Location | Purpose |
|----------|----------|---------|
| Development Specification | `/docs/FocusHive_Development_Specification.md` | Technical requirements |
| Project Design | `/docs/project_design.md` | Architecture decisions |
| Task List | `/docs/project_todo_list.md` | Sprint planning |
| Report Drafts | `/docs/reports/` | Academic submissions |

---

## 🔗 Quick Links

### Documentation
- [README](./README.md) - Project introduction
- [Contributing Guide](./CONTRIBUTING.md) - Contribution guidelines
- [API Specs](./shared/openapi/) - OpenAPI specifications

### Service Documentation
- [Backend README](./services/focushive-backend/README.md)
- [Identity Service](./services/identity-service/README.md)
- [Frontend Guide](./frontend/README.md)

### Development Tools
- [Linear Board](https://linear.app/focushive) - Task tracking
- [GitHub Repository](https://github.com/yourusername/focushive) - Source code
- [Docker Hub](https://hub.docker.com/u/focushive) - Container images

### External Resources
- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [React Documentation](https://react.dev)
- [Material UI](https://mui.com)
- [PostgreSQL](https://www.postgresql.org/docs/)

---

## 📊 Project Statistics

| Metric | Count |
|--------|-------|
| Total Services | 8 |
| Lines of Code | 50,000+ |
| Test Cases | 890+ |
| API Endpoints | 120+ |
| React Components | 150+ |
| Database Tables | 45+ |
| Docker Images | 10 |
| Dependencies | 200+ |

---

## 🤝 Contact & Support

For questions or support regarding this project:
- **Academic Supervisor**: [Supervisor Name]
- **Project Lead**: Nasir
- **University**: University of London
- **Program**: BSc Computer Science
- **Module**: Final Year Project

---

*This index is automatically generated and maintained. Last update: September 4, 2025*