# FocusHive 🎯

> Virtual co-working platform for enhanced productivity and accountability

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.1-brightgreen)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3.1-61dafb)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.5.4-blue)](https://www.typescriptlang.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

FocusHive transforms remote work by creating virtual co-working spaces where individuals can work alongside others in real-time, fostering accountability, reducing isolation, and enhancing productivity through community-driven focus sessions.

## 🌟 Features

### Core Functionality
- **🏠 Virtual Hives** - Join or create focused work spaces with like-minded individuals
- **⏱️ Smart Timers** - Pomodoro, Deep Work, and custom timer modes with synchronized sessions
- **👥 Buddy System** - AI-powered accountability partner matching based on goals and timezone
- **📊 Analytics Dashboard** - Track productivity patterns and focus metrics
- **💬 Real-time Chat** - Communicate within hives with thread support
- **🎭 Multiple Personas** - Separate work, study, and personal profiles for privacy

### Advanced Features
- **🧠 Emotion-Aware UI** - Adaptive interface that responds to user mood
- **🔔 Multi-channel Notifications** - Email, push, and in-app notifications
- **🏆 Gamification** - Achievements, streaks, and leaderboards
- **📱 Responsive Design** - Works seamlessly across devices
- **🔒 Privacy-First** - Field-level encryption for sensitive data

## 🚀 Quick Start

### Prerequisites

- Java 21 or higher
- Node.js 18+ and npm 9+
- Docker and Docker Compose
- PostgreSQL 16 (via Docker)
- Redis 7 (via Docker)

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/nasiridrishi/FocusHive.git
cd FocusHive
```

2. **Set up environment variables**
```bash
# Copy example environment files
cp .env.example .env
# Update with your configuration
```

3. **Start infrastructure services**
```bash
# Start PostgreSQL, Redis, RabbitMQ
docker-compose up -d
```

4. **Start backend services**
```bash
# Start all microservices
.scripts/start-all-services.sh

# Or start individually:
cd services/identity-service && ./mvnw spring-boot:run
cd services/focushive-backend && ./gradlew bootRun
cd services/buddy-service && ./gradlew bootRun
cd services/notification-service && ./gradlew bootRun
```

5. **Start frontend application**
```bash
cd frontend
npm install
npm run dev
```

6. **Access the application**
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- Identity Service: http://localhost:8081
- API Documentation: http://localhost:8080/swagger-ui.html

## 🏗️ Architecture

### Microservices Architecture

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   Frontend  │────▶│  Load Balancer   │────▶│   API Gateway   │
│  (React 18) │     │   (Cloudflare)   │     │   (Spring)      │
└─────────────┘     └──────────────────┘     └─────────────────┘
                                                       │
                    ┌──────────────────────────────────┼──────────────────────────────────┐
                    │                                  │                                  │
            ┌───────▼────────┐            ┌───────────▼────────┐           ┌─────────────▼──────┐
            │   Identity     │            │  FocusHive Backend │           │  Notification      │
            │   Service      │◀───────────│     (Main API)     │──────────▶│    Service         │
            │   (OAuth2)     │            │    (Port 8080)     │           │   (Port 8083)      │
            └────────────────┘            └────────────────────┘           └────────────────────┘
                    │                                  │                                  │
                    │                      ┌───────────▼────────┐                        │
                    └──────────────────────│   Buddy Service   │────────────────────────┘
                                          │   (Port 8087)     │
                                          └──────────────────┘
```

### Tech Stack

#### Frontend
- **React 18.3.1** - UI framework
- **TypeScript 5.5.4** - Type safety (strict mode)
- **Tailwind CSS** - Styling
- **Zustand** - State management
- **TanStack Query** - Server state management
- **Socket.io** - Real-time communication
- **Vite** - Build tool

#### Backend
- **Java 21** - Primary language
- **Spring Boot 3.3.1** - Framework
- **Spring Security** - Authentication & authorization
- **PostgreSQL 16** - Primary database
- **Redis 7** - Caching & sessions
- **RabbitMQ** - Message queue
- **WebSocket/STOMP** - Real-time updates

### Service Ports

| Service | Port | Description |
|---------|------|-------------|
| Frontend | 3000 | React application |
| FocusHive Backend | 8080 | Main API service |
| Identity Service | 8081 | OAuth2 authentication |
| Notification Service | 8083 | Multi-channel notifications |
| Buddy Service | 8087 | Accountability matching |

## 📂 Project Structure

```
FocusHive/
├── frontend/                 # React TypeScript frontend
│   ├── src/
│   │   ├── components/      # Reusable UI components
│   │   ├── pages/          # Route-based pages
│   │   ├── features/       # Feature modules
│   │   ├── services/       # API clients
│   │   ├── hooks/          # Custom React hooks
│   │   └── store/          # State management
│   └── tests/              # Frontend tests
├── services/
│   ├── focushive-backend/  # Main backend service
│   ├── identity-service/   # Authentication service
│   ├── notification-service/ # Notification handling
│   └── buddy-service/      # Buddy matching system
├── .scripts/               # Development scripts
├── .docs/                  # Documentation
└── docker-compose.yml      # Docker configuration
```

## 🛠️ Development

### Running Tests

```bash
# Run all tests
.scripts/run-all-tests.sh

# Frontend tests
cd frontend && npm test

# Backend tests
cd services/focushive-backend && ./gradlew test

# E2E tests
cd frontend && npm run test:e2e
```

### Code Quality

```bash
# Frontend linting
cd frontend && npm run lint

# Java formatting
cd services/focushive-backend && ./gradlew spotlessApply

# Type checking
cd frontend && npm run typecheck
```

### Building for Production

```bash
# Build all services
.scripts/build-all.sh

# Build frontend
cd frontend && npm run build

# Build backend services
cd services/focushive-backend && ./gradlew build
```

## 📚 API Documentation

Each service provides OpenAPI documentation:

- **FocusHive Backend**: http://localhost:8080/swagger-ui.html
- **Identity Service**: http://localhost:8081/swagger-ui.html
- **Buddy Service**: http://localhost:8087/swagger-ui.html
- **Notification Service**: http://localhost:8083/swagger-ui.html

## 🔐 Security

- **OAuth2** authentication with JWT tokens
- **Field-level encryption** for PII
- **Rate limiting** on all endpoints
- **OWASP** security best practices
- **TLS 1.3** for all communications

## 🚦 Performance

### Current Metrics
- **Bundle Size**: <1MB (target, currently optimizing from 6MB+)
- **API Response**: <200ms p95
- **WebSocket Latency**: <100ms
- **Lighthouse Score**: >90 (target)

### Optimization Strategies
- Code splitting for all routes
- Lazy loading of components
- Image optimization with WebP
- Two-tier caching (Caffeine + Redis)
- Database query optimization

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Follow coding standards:
   - TypeScript: Strict mode, no `any` types
   - Java: SLF4J logging only
   - All functions need return type annotations
   - No console.log in production code
4. Commit your changes (`git commit -m 'feat: add amazing feature'`)
5. Push to the branch (`git push origin feature/amazing-feature`)
6. Open a Pull Request

### Commit Convention

We follow [Conventional Commits](https://www.conventionalcommits.org/):
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation
- `style:` Formatting
- `refactor:` Code restructuring
- `test:` Tests
- `chore:` Maintenance

## 📋 Environment Variables

### Required Variables

```env
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/focushive
DATABASE_USERNAME=focushive_user
DATABASE_PASSWORD=secure_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=your_secret_key
JWT_EXPIRATION=3600000

# Service URLs
IDENTITY_SERVICE_URL=http://localhost:8081
NOTIFICATION_SERVICE_URL=http://localhost:8083
BUDDY_SERVICE_URL=http://localhost:8087
```

## 🐛 Troubleshooting

### Common Issues

1. **Port already in use**
   ```bash
   lsof -i :8080  # Find process using port
   kill -9 <PID>  # Kill the process
   ```

2. **Database connection failed**
   ```bash
   docker-compose restart postgres
   ```

3. **Frontend build errors**
   ```bash
   rm -rf node_modules package-lock.json
   npm install
   ```

## 📈 Monitoring

- **Health Checks**: `/actuator/health` on all services
- **Metrics**: Prometheus format at `/actuator/prometheus`
- **Logging**: Structured JSON logs with correlation IDs
- **Tracing**: Distributed tracing with Zipkin

## 🗺️ Roadmap

### Phase 1: MVP ✅ (Completed Q1 2025)
- Core hive functionality
- Basic timer system
- User authentication
- Simple presence tracking

### Phase 2: Beta ✅ (Completed Q2 2025)
- Buddy matching system
- Analytics dashboard
- Emotion-aware features
- Forum integration

### Phase 3: Launch 🚧 (Current - Q3 2025)
- Performance optimization
- Security hardening
- Load testing
- Marketing website

### Phase 4: Growth 📅 (Q4 2025 - Q2 2026)
- Mobile applications
- Advanced analytics
- Enterprise features
- API marketplace

