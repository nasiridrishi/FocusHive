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

```bash
# Start all services with Docker Compose
docker-compose up -d

# Backend will be available at: http://localhost:8080
# Frontend will be available at: http://localhost:5173

# Stop all services
docker-compose down
```

### Development Commands

#### Backend (Spring Boot)
```bash
cd backend
./gradlew bootRun     # Run backend
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
