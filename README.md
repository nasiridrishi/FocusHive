# FocusHive

Digital co-working and co-studying platform that creates virtual "hives" - dedicated online spaces where users can work on individual tasks while being visibly present and accountable to others.

## Project Status

This project is currently in the development phase for the University of London BSc Computer Science final project (CM3070).

- **Project Timeline**: May 22, 2025 - September 15, 2025
- **Current Phase**: Phase 1 - Foundation Setup
- **Architecture Update**: Migrated to Java/Spring Boot backend (July 8, 2025)

## Project Structure

FocusHive follows a microservices architecture with separate backend services and frontend:

```
focushive/
├── backend/             # Spring Boot 3.x Java backend (Main FocusHive service)
│   ├── src/            # Java source code
│   ├── build.gradle.kts # Gradle build configuration
│   └── Dockerfile      # Backend container
├── music-service/       # Spring Boot 3.x Music Recommendation microservice
│   ├── src/            # Java source code
│   ├── build.gradle.kts # Gradle build configuration
│   └── Dockerfile      # Music service container
├── identity-service/    # Spring Boot 3.x Identity Management microservice
│   ├── src/            # Java source code
│   ├── build.gradle.kts # Gradle build configuration
│   └── Dockerfile      # Identity service container
├── frontend/           # React TypeScript web application
│   ├── src/            # React source code with music features
│   └── Dockerfile      # Frontend container
├── shared/
│   └── openapi/        # OpenAPI specifications
├── docs/               # Documentation
├── docker-compose.yml  # Docker development environment
└── .github/            # GitHub Actions CI/CD
```

## Development

### Prerequisites

- Java 21
- Node.js 20+
- npm 10+
- Docker & Docker Compose
- Git

### Getting Started

#### Quick Start (Development)

```bash
# Clone the repository
git clone <repository-url>
cd focushive

# Copy environment variables
cp .env.example .env

# Start all services with Docker Compose (includes Identity Service)
docker-compose up -d

# Services will be available at:
# - Frontend: http://localhost:5173
# - Main Backend: http://localhost:8080  
# - Identity Service: http://localhost:8081
# - NGINX API Gateway: http://localhost:80
# - PostgreSQL (Main): localhost:5432
# - PostgreSQL (Identity): localhost:5433
# - Redis (Main): localhost:6379
# - Redis (Identity): localhost:6380

# Development tools (auto-included in development):
# - Adminer (Database UI): http://localhost:8082
# - Redis Commander: http://localhost:8083
# - Traefik Dashboard: http://localhost:8084

# Stop all services
docker-compose down
```

#### Environment-Specific Deployments

**Development Environment** (default):
```bash
# Uses docker-compose.override.yml automatically
docker-compose up -d

# Or explicitly:
docker-compose -f docker-compose.yml -f docker-compose.override.yml up -d
```

**Production Environment**:
```bash
# Production deployment with monitoring
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Additional monitoring services:
# - Prometheus: http://localhost:9090
# - Grafana: http://localhost:3000
```

**Testing Environment**:
```bash
# Testing environment with isolated databases
docker-compose -f docker-compose.yml -f docker-compose.test.yml up -d

# Run integration tests
docker-compose -f docker-compose.yml -f docker-compose.test.yml --profile integration-tests up --abort-on-container-exit

# Run E2E tests
docker-compose -f docker-compose.yml -f docker-compose.test.yml --profile e2e-tests up --abort-on-container-exit
```

#### Service Access and Routing

All API requests are routed through NGINX for consistent access:

**Frontend API Calls**:
- Main Backend APIs: `http://localhost/api/*` → `backend:8080/*`
- Identity Service APIs: `http://localhost/api/identity/*` → `identity-service:8081/api/v1/*`
- WebSocket connections: `http://localhost/ws` → `backend:8080/ws`

**Direct Service Access** (for debugging):
- Main Backend: `http://localhost:8080`
- Identity Service: `http://localhost:8081`
- Frontend: `http://localhost:5173`

### Development Commands

#### Backend (Spring Boot)
```bash
cd backend
./gradlew bootRun     # Run backend
./gradlew test        # Run tests
./gradlew build       # Build JAR
```

#### Identity Service (Spring Boot)
```bash
cd identity-service
./gradlew bootRun     # Run identity service
./gradlew test        # Run tests
./gradlew build       # Build JAR
```

#### Frontend (React)
```bash
cd frontend
npm install           # Install dependencies
npm run dev           # Run development server
npm test              # Run tests
npm run build         # Build production
```

### Identity Service Configuration

The Identity Service is a separate microservice that provides:

- **OAuth2 Authorization Server**: Full OAuth2.1 compliant authorization server
- **Multiple User Personas**: Users can have different profiles for work/study/personal contexts
- **Advanced Privacy Controls**: Granular privacy settings and data portability
- **Inter-service Authentication**: Secure JWT-based communication between services

#### Environment Variables

Key Identity Service environment variables (see `.env.example` for complete list):

```bash
# Identity Service Database
IDENTITY_DB_NAME=identity_db
IDENTITY_DB_USER=identity_user
IDENTITY_DB_PASSWORD=identity_pass
IDENTITY_DB_EXTERNAL_PORT=5433

# Identity Service Redis
IDENTITY_REDIS_PASSWORD=identity_redis_pass
IDENTITY_REDIS_EXTERNAL_PORT=6380

# Security Configuration
KEY_STORE_PASSWORD=changeme
PRIVATE_KEY_PASSWORD=changeme
FOCUSHIVE_CLIENT_SECRET=secret

# Service Configuration
IDENTITY_SERVICE_PORT=8081
```

#### Health Checks and Monitoring

All services include comprehensive health checks:

- **Identity Service**: `http://localhost:8081/api/v1/health`
- **Main Backend**: `http://localhost:8080/actuator/health`
- **Databases**: PostgreSQL `pg_isready` checks
- **Redis**: Redis `ping` checks
- **NGINX**: HTTP health endpoint

#### Database Initialization

Database initialization scripts are automatically run when containers start:

- `scripts/init-identity-db.sh`: Production identity database setup
- `scripts/dev-init-identity-db.sh`: Development identity database with debug tools
- `scripts/init-db.sh`: Production main database setup
- `scripts/dev-init-db.sh`: Development main database with debug tools

#### Troubleshooting

**Common Issues:**

1. **Port Conflicts**: Ensure ports 5432, 5433, 6379, 6380, 8080, 8081 are available
2. **Database Connection**: Check that PostgreSQL containers are healthy before services start
3. **Redis Connection**: Verify Redis containers are accessible with correct passwords
4. **NGINX Routing**: Check NGINX logs for routing issues: `docker-compose logs nginx`

**Debug Commands:**
```bash
# Check service status
docker-compose ps

# View logs for specific service
docker-compose logs identity-service
docker-compose logs backend
docker-compose logs nginx

# Check database connectivity
docker-compose exec identity-db psql -U identity_user -d identity_db -c "SELECT 1;"
docker-compose exec identity-redis redis-cli -a identity_redis_pass ping

# Access database management tools
# Adminer: http://localhost:8082
# Redis Commander: http://localhost:8083
```

**Development Database Access:**
```bash
# Connect to main database
docker-compose exec db psql -U focushive_user -d focushive

# Connect to identity database  
docker-compose exec identity-db psql -U identity_user -d identity_db

# Access Redis
docker-compose exec redis redis-cli -a focushive_pass
docker-compose exec identity-redis redis-cli -a identity_redis_pass
```

## Architecture

### Microservices Architecture
- **Main Backend**: Spring Boot 3.x with Java 21
  - RESTful APIs with Spring MVC
  - WebSocket support with STOMP
  - PostgreSQL with Spring Data JPA
  - Redis for caching and pub/sub
  - JWT authentication with Spring Security
- **Music Service**: Spring Boot 3.x microservice
  - Advanced music recommendation engine
  - Spotify OAuth2 integration with token encryption
  - Collaborative playlist management
  - Real-time music synchronization via WebSockets
  - Redis caching for performance optimization
- **Identity Service**: Spring Boot 3.x microservice
  - Multiple user personas/profiles
  - OAuth2 authorization server
  - Advanced privacy controls
  - Inter-service authentication
- **Frontend**: React with TypeScript and Vite
  - Material UI (MUI) component library
  - Real-time music player integration
  - Spotify Web SDK integration
  - Real-time updates via WebSockets

### Infrastructure
- **Databases**: PostgreSQL 16 for all microservices
- **Caching**: Redis 7 for caching and real-time features
- **Containerization**: Docker with multi-service orchestration
- **Authentication**: JWT-based with inter-service communication

## Documentation

### Service Documentation
- [Music Service](music-service/README.md) - Music recommendation engine and playlist management
- [Identity Service](identity-service/README.md) - User identity and profile management
- [Main Backend Service](backend/README.md) - Core FocusHive functionality

### Architecture & Integration
- [Music Service Integration Guide](docs/music-service-integration.md)
- [Inter-service Communication](backend/docs/INTER_SERVICE_COMMUNICATION.md)
- [Development Specification](archive/docs/FocusHive_Development_Specification.md)
- [Project Design](archive/docs/FocusHive_Project_Description.md)

## Testing

The project follows Test-Driven Development (TDD) practices:

1. Tests are written before implementation
2. All packages have their own test suites
3. Monorepo structure is tested with `monorepo.test.js` and `packages.test.js`

## License

This project is part of an academic submission for the University of London.
