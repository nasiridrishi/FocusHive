# FocusHive

Digital co-working and co-studying platform that creates virtual "hives" - dedicated online spaces where users can work on individual tasks while being visibly present and accountable to others.

## Project Status

This project is currently in the development phase for the University of London BSc Computer Science final project (CM3070).

- **Project Timeline**: May 22, 2025 - September 15, 2025
- **Current Phase**: Phase 1 - Foundation Setup
- **Architecture Update**: Migrated to Java/Spring Boot backend (July 8, 2025)

## Project Structure

FocusHive follows a modular architecture with separate backend and frontend:

```
focushive/
├── backend/             # Spring Boot 3.x Java backend
│   ├── src/            # Java source code
│   ├── build.gradle.kts # Gradle build configuration
│   └── Dockerfile      # Backend container
├── frontend/
│   └── web/            # React TypeScript web application
│       ├── src/        # React source code
│       └── Dockerfile  # Frontend container
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
cd frontend/web
npm install           # Install dependencies
npm run dev           # Run development server
npm test              # Run tests
npm run build         # Build production
```

## Architecture

- **Backend**: Spring Boot 3.x with Java 21
  - RESTful APIs with Spring MVC
  - WebSocket support with STOMP
  - PostgreSQL with Spring Data JPA
  - Redis for caching and pub/sub
  - JWT authentication with Spring Security
- **Frontend**: React with TypeScript and Vite
  - Tailwind CSS v4 for styling
  - Real-time updates via WebSockets
- **Infrastructure**: Docker containerization
  - PostgreSQL 16 for relational data
  - Redis 7 for caching and real-time features

## Documentation

- [Monorepo Setup Guide](docs/monorepo-setup.md)
- Development Specification (see project docs)
- Project Design (see project docs)
- Todo List (see project docs)

## Testing

The project follows Test-Driven Development (TDD) practices:

1. Tests are written before implementation
2. All packages have their own test suites
3. Monorepo structure is tested with `monorepo.test.js` and `packages.test.js`

## License

This project is part of an academic submission for the University of London.
