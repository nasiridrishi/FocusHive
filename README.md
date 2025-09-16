# FocusHive

Virtual co-working platform for collaborative focus and accountability.

## Quick Start

```bash
# Clone and start all services
git clone https://github.com/focushive/focushive.git
cd focushive
docker-compose up -d

# Access application
open http://localhost:3000
```

## Architecture

**4 Active Microservices**:
- **FocusHive Backend** (8080): Core platform with integrated modules
  - Hive management and real-time presence
  - Chat module for team communication
  - Analytics module for productivity tracking
  - Forum module for community discussions
- **Identity Service** (8081): OAuth2 provider with multi-persona support
- **Notification Service** (8083): Email, push, and in-app notifications
- **Buddy Service** (8087): Accountability partner matching

**Technology Stack**:
- Backend: Spring Boot 3.3, Java 21
- Frontend: React 18.3, TypeScript, Material UI
- Database: PostgreSQL 16, Redis 7
- Real-time: WebSockets with STOMP protocol
- Testing: JUnit 5, Jest, Playwright

## Documentation

- [Project Overview](PROJECT_INDEX.md) - Complete navigation guide
- [API Reference](API_REFERENCE.md) - All endpoints documented
- [Testing](TESTING.md) - 86% overall coverage
- [Security](SECURITY.md) - Implementation details
- [Deployment](DEPLOYMENT.md) - Production setup

## Development

### Prerequisites
- Java 21
- Node.js 20+
- Docker & Docker Compose

### Commands

```bash
# Backend
cd services/focushive-backend
./gradlew bootRun

# Frontend
cd frontend
npm install
npm run dev

# Run tests
./gradlew test
npm test
```

## Project Status

University of London BSc Computer Science Final Year Project (2025)
- **Timeline**: May 22, 2025 - September 22, 2025
- **Current Phase**: Implementation & Testing
- **Test Coverage**: ~80% overall

## License

Academic project - University of London